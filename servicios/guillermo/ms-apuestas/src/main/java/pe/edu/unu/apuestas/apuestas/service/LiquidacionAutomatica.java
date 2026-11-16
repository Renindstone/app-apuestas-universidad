package pe.edu.unu.apuestas.apuestas.service;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import feign.FeignException;
import pe.edu.unu.apuestas.apuestas.client.CatalogoClient;
import pe.edu.unu.apuestas.apuestas.client.IngestaClient;
import pe.edu.unu.apuestas.apuestas.dto.LiquidacionDTO;
import pe.edu.unu.apuestas.apuestas.dto.PartidoDTO;
import pe.edu.unu.apuestas.apuestas.entity.EstadoPick;
import pe.edu.unu.apuestas.apuestas.repository.ApuestaUsuarioRepository;
import pe.edu.unu.apuestas.apuestas.repository.PickRepository;

/**
 * Liquida sola las apuestas cuando termina su partido, sin esperar a la liquidación de las 06:00.
 * Cada 10 minutos: liquida lo que ya tiene resultado y, si queda alguna apuesta de un partido que ya debió terminar,
 * le pide a ms-ingesta que vuelva a leer los resultados. En la siguiente vuelta se liquida con el resultado nuevo.
 * El resultado sale de football-data: si la fuente aún no lo publicó, la apuesta sigue pendiente hasta que lo haga.
 */
@Service
@Lazy(false)
public class LiquidacionAutomatica {

    private static final Logger log = LoggerFactory.getLogger(LiquidacionAutomatica.class);
    // Un partido dura unas 2 horas con el descanso y el tiempo añadido.
    private static final Duration DURACION_PARTIDO = Duration.ofHours(2);
    // No se pide la ingesta más de una vez cada 30 minutos: cada pedido descarga todos los CSV de la temporada.
    private static final Duration ENTRE_ACTUALIZACIONES = Duration.ofMinutes(30);
    // Partidos más viejos que esto ya no disparan la ingesta: si la fuente no los tiene, no los va a tener.
    private static final Duration MAXIMO_ESPERA = Duration.ofDays(7);

    private final LiquidacionService liquidacion;
    private final PickRepository picks;
    private final ApuestaUsuarioRepository apuestas;
    private final CatalogoClient catalogo;
    private final IngestaClient ingesta;

    private Instant ultimaActualizacion;

    public LiquidacionAutomatica(LiquidacionService liquidacion, PickRepository picks, ApuestaUsuarioRepository apuestas,
            CatalogoClient catalogo, IngestaClient ingesta) {
        this.liquidacion = liquidacion;
        this.picks = picks;
        this.apuestas = apuestas;
        this.catalogo = catalogo;
        this.ingesta = ingesta;
    }

    @Scheduled(initialDelayString = "${apuestas.liquidacion.retraso-inicial:PT3M}", fixedDelayString = "${apuestas.liquidacion.cada:PT10M}")
    public void revisar() {
        LiquidacionDTO r = liquidacion.liquidarPendientes();
        if (r.picks() + r.apuestas() > 0) {
            log.info("Liquidación automática: {} picks y {} apuestas liquidados, {} pendientes.", r.picks(), r.apuestas(), r.pendientes());
        }
        if (r.pendientes() > 0 && hayPartidosTerminadosSinResultado()) {
            pedirResultados();
        }
    }

    // ¿Alguna apuesta pendiente es de un partido que empezó hace más de 2 horas y todavía no figura como FINALIZADO?
    private boolean hayPartidosTerminadosSinResultado() {
        Set<Long> partidos = new LinkedHashSet<>();
        Stream.concat(picks.findByEstado(EstadoPick.ABIERTO).stream(), apuestas.findByResultadoIsNull().stream())
                .forEach(a -> partidos.add(a.getPartidoId()));
        Instant ahora = Instant.now();
        for (Long id : partidos) {
            try {
                PartidoDTO p = catalogo.partido(id);
                if (p.fechaHora() != null && !"FINALIZADO".equals(p.estado())
                        && p.fechaHora().plus(DURACION_PARTIDO).isBefore(ahora)
                        && p.fechaHora().isAfter(ahora.minus(MAXIMO_ESPERA))) {
                    return true;
                }
            } catch (FeignException e) {
                log.warn("No se pudo consultar el partido {} en ms-catalogo (HTTP {}).", id, e.status());
            }
        }
        return false;
    }

    // Le pide a ms-ingesta los resultados nuevos, como mucho una vez cada 30 minutos.
    private void pedirResultados() {
        Instant ahora = Instant.now();
        if (ultimaActualizacion != null && ultimaActualizacion.plus(ENTRE_ACTUALIZACIONES).isAfter(ahora)) {
            return;
        }
        ultimaActualizacion = ahora;
        try {
            ingesta.actualizarResultados();
            log.info("Hay apuestas de partidos ya terminados: se pidió a ms-ingesta que actualice los resultados.");
        } catch (FeignException.Conflict e) {
            log.info("ms-ingesta ya tiene una ingesta en curso: se liquidará con sus resultados.");
        } catch (FeignException e) {
            log.warn("No se pudo pedir la actualización a ms-ingesta (HTTP {}). Se reintenta más tarde.", e.status());
        }
    }
}
