package pe.edu.unu.apuestas.ingesta.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
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
 * Lanza la ingesta (a mano o a la hora programada) y lista el historial de ejecuciones.
 */
@Service
public class IngestaService {

    private static final Logger log = LoggerFactory.getLogger(IngestaService.class);
    private static final List<String> LIGAS = List.of("E0", "SP1", "D1");

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
     * HISTORICO: un CSV por liga y temporada (la anterior y la actual, o solo la actual). PROXIMOS: fixtures.csv.
     */
    public EjecucionIniciadaDTO lanzar(TipoIngesta tipo, boolean soloTemporadaActual) {
        // Dos ingestas a la vez descargarían todo dos veces y chocarían al crear los mismos equipos.
        if (ejecuciones.existsByEstado(EstadoEjecucion.EN_CURSO)) {
            throw new ApiException(HttpStatus.CONFLICT, "INGESTA_EN_CURSO", "Ya hay una ingesta en curso. Espera a que termine.");
        }
        String base = propiedades.urlBase();
        String actual = propiedades.temporadaActual();
        Map<String, String> fuentes = new LinkedHashMap<>();   // URL → temporada de sus partidos
        if (tipo == TipoIngesta.PROXIMOS) {
            // fixtures.csv no dice la temporada: todos sus partidos son de la actual.
            fuentes.put(base + "/fixtures.csv", actual);
        } else {
            List<String> temporadas = soloTemporadaActual ? List.of(actual) : List.of(propiedades.temporadaAnterior(), actual);
            for (String temporada : temporadas) {
                for (String liga : LIGAS) {
                    fuentes.put(base + "/mmz4281/" + temporada + "/" + liga + ".csv", temporada);
                }
            }
        }
        List<ProcesadorIngesta.Archivo> archivos = new ArrayList<>();
        fuentes.forEach((url, temporada) -> archivos.add(
                new ProcesadorIngesta.Archivo(ejecuciones.save(new Ejecucion(tipo, url)).getId(), url, temporada)));
        procesador.procesar(archivos);
        return new EjecucionIniciadaDTO(archivos.get(0).ejecucionId(), EstadoEjecucion.EN_CURSO);
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

    // 08:00 y 16:00 de Lima: próximos partidos (RN-ING-02).
    @Scheduled(cron = "0 0 8,16 * * *", zone = "America/Lima")
    public void proximosProgramado() {
        programada(TipoIngesta.PROXIMOS);
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
