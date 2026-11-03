package pe.edu.unu.apuestas.ingesta.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import pe.edu.unu.apuestas.ingesta.config.IngestaProperties;
import pe.edu.unu.apuestas.ingesta.dto.EjecucionDTO;
import pe.edu.unu.apuestas.ingesta.dto.EjecucionIniciadaDTO;
import pe.edu.unu.apuestas.ingesta.entity.Ejecucion;
import pe.edu.unu.apuestas.ingesta.entity.EstadoEjecucion;
import pe.edu.unu.apuestas.ingesta.entity.TipoIngesta;
import pe.edu.unu.apuestas.ingesta.exception.ApiException;
import pe.edu.unu.apuestas.ingesta.repository.EjecucionRepository;

/**
 * Lanza la ingesta (a mano, al arrancar o a la hora programada) y lista el historial de ejecuciones.
 * Lazy(false): con spring.main.lazy-initialization (docker-compose.dev.yml) sus @Scheduled no se registrarían
 * hasta que alguien usara el controlador.
 */
@Service
@Lazy(false)
public class IngestaService {

    private static final Logger log = LoggerFactory.getLogger(IngestaService.class);
    private static final List<String> LIGAS = List.of("E0", "SP1", "D1");
    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    /** Un archivo por descargar y la temporada de sus partidos. */
    private record Fuente(String url, String temporada) {
    }

    private final EjecucionRepository ejecuciones;
    private final ProcesadorIngesta procesador;
    private final IngestaProperties propiedades;

    public IngestaService(EjecucionRepository ejecuciones, ProcesadorIngesta procesador, IngestaProperties propiedades) {
        this.ejecuciones = ejecuciones;
        this.procesador = procesador;
        this.propiedades = propiedades;
    }

    /**
     * Registra una ejecución EN_CURSO por archivo y los procesa en segundo plano. Devuelve la primera.
     * HISTORICO: un CSV por liga y temporada (la anterior y la actual, o solo la actual) y new/BRA.csv para Brasil.
     * PROXIMOS: fixtures.csv (ligas europeas) y new_league_fixtures.csv (Brasil).
     */
    public EjecucionIniciadaDTO lanzar(TipoIngesta tipo, boolean soloTemporadaActual) {
        return lanzarVarias(List.of(tipo), soloTemporadaActual);
    }

    /**
     * Varias ingestas en una sola tanda, una detrás de otra (por ejemplo HISTORICO y luego PROXIMOS al arrancar).
     * Se registran todas las ejecuciones antes de empezar, así nadie puede lanzar otra en medio.
     */
    public synchronized EjecucionIniciadaDTO lanzarVarias(List<TipoIngesta> tipos, boolean soloTemporadaActual) {
        // Dos ingestas a la vez descargarían todo dos veces y chocarían al crear los mismos equipos.
        if (ejecuciones.existsByEstado(EstadoEjecucion.EN_CURSO)) {
            throw new ApiException(HttpStatus.CONFLICT, "INGESTA_EN_CURSO", "Ya hay una ingesta en curso. Espera a que termine.");
        }
        List<ProcesadorIngesta.Archivo> archivos = new ArrayList<>();
        for (TipoIngesta tipo : tipos) {
            for (Fuente f : fuentes(tipo, soloTemporadaActual)) {
                archivos.add(new ProcesadorIngesta.Archivo(ejecuciones.save(new Ejecucion(tipo, f.url())).getId(), f.url(), f.temporada()));
            }
        }
        procesador.procesar(archivos);
        return new EjecucionIniciadaDTO(archivos.get(0).ejecucionId(), EstadoEjecucion.EN_CURSO);
    }

    // Los archivos de cada tipo de ingesta, en orden.
    private List<Fuente> fuentes(TipoIngesta tipo, boolean soloTemporadaActual) {
        String base = propiedades.urlBase();
        String actual = propiedades.temporadaActual();
        // Brasil juega en año calendario: su temporada actual es el año de hoy en Lima ("2026").
        int anio = LocalDate.now(LIMA).getYear();
        String brasilActual = String.valueOf(anio);
        List<Fuente> fuentes = new ArrayList<>();
        if (tipo == TipoIngesta.PROXIMOS) {
            // fixtures.csv no dice la temporada: todos sus partidos son de la actual.
            fuentes.add(new Fuente(base + "/fixtures.csv", actual));
            // Los próximos partidos de las ligas "nuevas" de football-data, entre ellas la Serie A de Brasil.
            fuentes.add(new Fuente(base + "/new_league_fixtures.csv", brasilActual));
        } else {
            List<String> temporadas = soloTemporadaActual ? List.of(actual) : List.of(propiedades.temporadaAnterior(), actual);
            for (String temporada : temporadas) {
                for (String liga : LIGAS) {
                    fuentes.add(new Fuente(base + "/mmz4281/" + temporada + "/" + liga + ".csv", temporada));
                }
            }
            // new/BRA.csv trae todas las temporadas de Brasil juntas: el procesador se queda solo con la pedida.
            List<String> brasil = soloTemporadaActual ? List.of(brasilActual) : List.of(String.valueOf(anio - 1), brasilActual);
            for (String temporada : brasil) {
                fuentes.add(new Fuente(base + "/new/BRA.csv", temporada));
            }
        }
        return fuentes;
    }

    // Las últimas ejecuciones, de la más reciente a la más antigua.
    public List<EjecucionDTO> ejecuciones(int limite) {
        if (limite < 1 || limite > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDACION", "El límite debe estar entre 1 y 100.");
        }
        return ejecuciones.findAll(PageRequest.of(0, limite, Sort.by("id").descending())).stream().map(EjecucionDTO::de).toList();
    }

    // 05:00 de Lima: histórico, solo de la temporada actual (RN-ING-02).
    @Scheduled(cron = "0 0 5 * * *", zone = "America/Lima")
    public void historicoProgramado() {
        programada(TipoIngesta.HISTORICO);
    }

    // Cada 3 horas (desde las 08:00 de Lima): próximos partidos, para tener los de toda la semana que publique la fuente (RN-ING-02).
    @Scheduled(cron = "0 0 2,5,8,11,14,17,20,23 * * *", zone = "America/Lima")
    public void proximosProgramado() {
        programada(TipoIngesta.PROXIMOS);
    }

    /**
     * Al arrancar, sin esperar a ningún horario: resultados y próximos partidos, para que al entrar ya estén los de la
     * semana. Espera 2 minutos para que ms-catalogo, ms-estadisticas y ms-cuotas ya estén en Eureka.
     * Si la base está vacía, carga también la temporada anterior (la necesita el modelo).
     */
    @Scheduled(initialDelayString = "${ingesta.retraso-inicial:PT2M}", fixedDelayString = "P365D")
    public void alArrancar() {
        boolean primeraVez = ejecuciones.count() == 0;
        try {
            lanzarVarias(List.of(TipoIngesta.HISTORICO, TipoIngesta.PROXIMOS), !primeraVez);
            log.info("Ingesta al arrancar lanzada ({}).", primeraVez ? "las 2 temporadas" : "solo la temporada actual");
        } catch (ApiException e) {
            log.warn("Se saltó la ingesta al arrancar: {}", e.getMessage());
        }
    }

    /**
     * POST /interno/resultados: lo llama ms-apuestas cuando hay apuestas de partidos que ya debieron terminar.
     * Vuelve a leer los resultados de la temporada actual. 409 si ya hay una ingesta en curso.
     */
    public EjecucionIniciadaDTO actualizarResultados() {
        return lanzar(TipoIngesta.HISTORICO, true);
    }

    private void programada(TipoIngesta tipo) {
        try {
            lanzar(tipo, true);
        } catch (ApiException e) {
            log.warn("Se saltó la ingesta {} programada: {}", tipo, e.getMessage());
        }
    }

    // Al arrancar, las ejecuciones que siguen EN_CURSO se cortaron con un reinicio: se cierran en ERROR para no bloquear las siguientes.
    @EventListener(ApplicationReadyEvent.class)
    public void cerrarInterrumpidas() {
        List<Ejecucion> cortadas = ejecuciones.findByEstado(EstadoEjecucion.EN_CURSO);
        cortadas.forEach(e -> e.fallar("Se interrumpió porque ms-ingesta se reinició."));
        ejecuciones.saveAll(cortadas);
    }
}
