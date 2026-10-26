package pe.edu.unu.apuestas.ingesta.service;

import java.util.List;

import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import pe.edu.unu.apuestas.ingesta.client.CatalogoClient;
import pe.edu.unu.apuestas.ingesta.client.CuotasClient;
import pe.edu.unu.apuestas.ingesta.client.EstadisticasClient;
import pe.edu.unu.apuestas.ingesta.csv.CsvFootballDataParser;
import pe.edu.unu.apuestas.ingesta.csv.EstadisticasCsv;
import pe.edu.unu.apuestas.ingesta.csv.PartidoCsv;
import pe.edu.unu.apuestas.ingesta.dto.CuotasRequest;
import pe.edu.unu.apuestas.ingesta.dto.EstadisticasRequest;
import pe.edu.unu.apuestas.ingesta.dto.PartidoDTO;
import pe.edu.unu.apuestas.ingesta.dto.PartidoUpsertRequest;
import pe.edu.unu.apuestas.ingesta.entity.Ejecucion;
import pe.edu.unu.apuestas.ingesta.repository.EjecucionRepository;

/**
 * Orquestador: descarga cada archivo, lo lee fila por fila y reparte los datos entre los tres servicios.
 * Está separado de IngestaService porque @Async solo funciona cuando el método se llama desde otro bean.
 */
@Service
public class ProcesadorIngesta {

    private static final Logger log = LoggerFactory.getLogger(ProcesadorIngesta.class);
    // Las cuotas de football-data que se usan son las previas al partido (las de cierre, B365CH y demás, no se leen).
    private static final String TIPO_CUOTA = "APERTURA";

    /** Un archivo por procesar: el ID de su ejecución, ya registrada EN_CURSO, la URL y la temporada de sus partidos. */
    public record Archivo(Long ejecucionId, String url, String temporada) {
    }

    private final EjecucionRepository ejecuciones;
    private final DescargadorCsv descargador;
    private final CsvFootballDataParser parser;
    private final CatalogoClient catalogo;
    private final EstadisticasClient estadisticas;
    private final CuotasClient cuotas;

    public ProcesadorIngesta(EjecucionRepository ejecuciones, DescargadorCsv descargador, CsvFootballDataParser parser,
            CatalogoClient catalogo, EstadisticasClient estadisticas, CuotasClient cuotas) {
        this.ejecuciones = ejecuciones;
        this.descargador = descargador;
        this.parser = parser;
        this.catalogo = catalogo;
        this.estadisticas = estadisticas;
        this.cuotas = cuotas;
    }

    // Procesa los archivos uno tras otro, en segundo plano. POST /ejecutar ya respondió 202.
    @Async
    public void procesar(List<Archivo> archivos) {
        archivos.forEach(this::procesarArchivo);
    }

    // Un archivo: si no se puede descargar o leer, su ejecución termina en ERROR y se reintenta en la próxima hora programada (RN-ING-05).
    void procesarArchivo(Archivo archivo) {
        Ejecucion ejecucion = ejecuciones.findById(archivo.ejecucionId()).orElseThrow();
        ejecucion.iniciar();
        ejecucion = ejecuciones.save(ejecucion);
        try {
            List<CSVRecord> filas = parser.leer(descargador.descargar(archivo.url()));
            int procesadas = 0;
            for (CSVRecord fila : filas) {
                // Una fila con error se anota en el log y se sigue con la siguiente (RN-ING-05).
                try {
                    procesarFila(parser.parsear(fila), archivo.temporada());
                    procesadas++;
                } catch (Exception e) {
                    log.warn("Fila {} de {}: {}", fila.getRecordNumber(), archivo.url(), mensaje(e));
                }
            }
            int fallidas = filas.size() - procesadas;
            ejecucion.terminar(procesadas, fallidas == 0 ? null
                    : fallidas + " de " + filas.size() + " filas fallaron. El detalle está en el log de ms-ingesta.");
            log.info("{}: {} filas procesadas, {} con error.", archivo.url(), procesadas, fallidas);
        } catch (Exception e) {
            log.error("No se pudo procesar {}: {}", archivo.url(), mensaje(e));
            ejecucion.fallar("No se pudo descargar o leer el archivo: " + mensaje(e));
        }
        ejecuciones.save(ejecucion);
    }

    // Una fila, en el orden de RN-ING-04: el catálogo (que devuelve los IDs), las estadísticas si ya se jugó y las cuotas si las trae.
    void procesarFila(PartidoCsv fila, String temporada) {
        PartidoDTO partido = catalogo.upsert(new PartidoUpsertRequest(fila.liga(), temporada, fila.fechaHora(), fila.local(),
                fila.visitante(), fila.arbitro(), fila.golesLocal(), fila.golesVisitante()));
        if (fila.tieneEstadisticas()) {
            Long arbitroId = partido.arbitro() == null ? null : partido.arbitro().id();
            estadisticas.guardar(new EstadisticasRequest(partido.id(), partido.liga().id(), partido.temporada(), partido.fechaHora(),
                    arbitroId,
                    lado(partido.local().id(), fila.golesLocal(), fila.estadisticasLocal()),
                    lado(partido.visitante().id(), fila.golesVisitante(), fila.estadisticasVisitante())));
        }
        fila.cuotas().forEach((casa, lista) -> cuotas.guardar(new CuotasRequest(partido.id(), casa, TIPO_CUOTA, lista)));
    }

    private static EstadisticasRequest.Lado lado(Long equipoId, int goles, EstadisticasCsv e) {
        return new EstadisticasRequest.Lado(equipoId, goles, e.tiros(), e.tirosPuerta(), e.corners(), e.faltas(), e.amarillas(),
                e.rojas());
    }

    // Algunas excepciones (por ejemplo, una conexión rechazada) no traen mensaje: entonces se usa su nombre.
    private static String mensaje(Exception e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }
}
