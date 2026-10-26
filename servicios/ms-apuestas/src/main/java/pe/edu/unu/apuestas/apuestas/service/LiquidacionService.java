package pe.edu.unu.apuestas.apuestas.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import feign.FeignException;
import pe.edu.unu.apuestas.apuestas.client.CatalogoClient;
import pe.edu.unu.apuestas.apuestas.client.EstadisticasClient;
import pe.edu.unu.apuestas.apuestas.dto.EstadisticasPartidoDTO;
import pe.edu.unu.apuestas.apuestas.dto.LiquidacionDTO;
import pe.edu.unu.apuestas.apuestas.dto.PartidoDTO;
import pe.edu.unu.apuestas.apuestas.entity.Apostable;
import pe.edu.unu.apuestas.apuestas.entity.ApuestaUsuario;
import pe.edu.unu.apuestas.apuestas.entity.EstadoPick;
import pe.edu.unu.apuestas.apuestas.entity.Pick;
import pe.edu.unu.apuestas.apuestas.entity.Resultado;
import pe.edu.unu.apuestas.apuestas.repository.ApuestaUsuarioRepository;
import pe.edu.unu.apuestas.apuestas.repository.PickRepository;

/**
 * Liquidación de picks y apuestas cuando termina el partido (RN-07 y RN-APU-06).
 */
@Service
public class LiquidacionService {

    private static final Logger log = LoggerFactory.getLogger(LiquidacionService.class);

    /** Lo que se sabe del partido terminado. tarjetas y corners van en null si ms-estadisticas aún no los tiene. */
    public record Final(int golesLocal, int golesVisitante, Integer tarjetas, Integer corners) {
    }

    private final PickRepository picks;
    private final ApuestaUsuarioRepository apuestas;
    private final CatalogoClient catalogo;
    private final EstadisticasClient estadisticas;

    public LiquidacionService(PickRepository picks, ApuestaUsuarioRepository apuestas, CatalogoClient catalogo,
            EstadisticasClient estadisticas) {
        this.picks = picks;
        this.apuestas = apuestas;
        this.catalogo = catalogo;
        this.estadisticas = estadisticas;
    }

    /**
     * CB-07 y la tabla del contrato: GANADA o PERDIDA según el mercado. Las líneas terminan en .5, así que no hay empate.
     * Devuelve null si el mercado es de tarjetas o córners y sus estadísticas aún no están: la apuesta sigue pendiente.
     */
    public static Resultado liquidar(String mercado, String seleccion, BigDecimal linea, Final partido) {
        int local = partido.golesLocal();
        int visitante = partido.golesVisitante();
        Boolean gana = switch (mercado) {
            case "1X2" -> switch (seleccion) {
                case "LOCAL" -> local > visitante;
                case "EMPATE" -> local == visitante;
                default -> local < visitante;
            };
            case "GOLES" -> masMenos(seleccion, local + visitante, linea);
            case "AMBOS_MARCAN" -> (local >= 1 && visitante >= 1) == "SI".equals(seleccion);
            case "TARJETAS" -> partido.tarjetas() == null ? null : masMenos(seleccion, partido.tarjetas(), linea);
            case "CORNERS" -> partido.corners() == null ? null : masMenos(seleccion, partido.corners(), linea);
            default -> throw new IllegalArgumentException("Mercado desconocido: " + mercado);
        };
        return gana == null ? null : gana ? Resultado.GANADA : Resultado.PERDIDA;
    }

    // MAS gana si el total supera la línea; MENOS, si no la supera.
    private static boolean masMenos(String seleccion, int total, BigDecimal linea) {
        return "MAS".equals(seleccion) == (BigDecimal.valueOf(total).compareTo(linea) > 0);
    }

    // RN-07: monto × (cuota − 1) si ganó y −monto si perdió, con 2 decimales.
    public static BigDecimal ganancia(Resultado resultado, BigDecimal monto, BigDecimal cuota) {
        BigDecimal ganancia = resultado == Resultado.GANADA ? monto.multiply(cuota.subtract(BigDecimal.ONE)) : monto.negate();
        return ganancia.setScale(2, RoundingMode.HALF_UP);
    }

    // RN-APU-06: corre sola a las 06:00 de Lima.
    @Scheduled(cron = "0 0 6 * * *", zone = "America/Lima")
    public void liquidarProgramado() {
        LiquidacionDTO r = liquidarPendientes();
        log.info("Liquidación: {} picks y {} apuestas liquidados, {} pendientes.", r.picks(), r.apuestas(), r.pendientes());
    }

    /**
     * POST /interno/liquidar: recorre los picks abiertos y las apuestas pendientes. Cada partido se consulta una sola vez,
     * y sus estadísticas solo si alguna apuesta es de tarjetas o córners. Lo que no se puede liquidar hoy queda pendiente.
     * Sin @Transactional: no tiene sentido tener una transacción abierta mientras se espera a los otros servicios.
     */
    public synchronized LiquidacionDTO liquidarPendientes() {
        List<Pick> abiertos = picks.findByEstado(EstadoPick.ABIERTO);
        List<ApuestaUsuario> pendientes = apuestas.findByResultadoIsNull();
        Set<Long> partidos = new HashSet<>();
        Set<Long> conEstadisticas = new HashSet<>();
        Stream.concat(abiertos.stream(), pendientes.stream()).forEach(a -> {
            partidos.add(a.getPartidoId());
            if (a.getMercado().equals("TARJETAS") || a.getMercado().equals("CORNERS")) {
                conEstadisticas.add(a.getPartidoId());
            }
        });
        Map<Long, Final> finales = new HashMap<>();
        partidos.forEach(id -> {
            Final f = finalDe(id, conEstadisticas.contains(id));
            if (f != null) {
                finales.put(id, f);
            }
        });

        List<Pick> picksLiquidados = new ArrayList<>();
        for (Pick pick : abiertos) {
            Resultado r = resultado(pick, finales);
            if (r != null) {
                pick.liquidar(r);
                picksLiquidados.add(pick);
            }
        }
        List<ApuestaUsuario> apuestasLiquidadas = new ArrayList<>();
        for (ApuestaUsuario apuesta : pendientes) {
            Resultado r = resultado(apuesta, finales);
            if (r != null) {
                apuesta.liquidar(r, ganancia(r, apuesta.getMonto(), apuesta.getCuota()));
                apuestasLiquidadas.add(apuesta);
            }
        }
        picks.saveAll(picksLiquidados);
        apuestas.saveAll(apuestasLiquidadas);
        int siguenPendientes = abiertos.size() + pendientes.size() - picksLiquidados.size() - apuestasLiquidadas.size();
        return new LiquidacionDTO(picksLiquidados.size(), apuestasLiquidadas.size(), siguenPendientes);
    }

    // El resultado de un pick o una apuesta, o null si su partido aún no terminó o le falta una estadística.
    private static Resultado resultado(Apostable a, Map<Long, Final> finales) {
        Final f = finales.get(a.getPartidoId());
        return f == null ? null : liquidar(a.getMercado(), a.getSeleccion(), a.getLinea(), f);
    }

    // El resultado final del partido, o null si aún no está FINALIZADO o si ms-catalogo no responde (se reintenta mañana).
    private Final finalDe(Long partidoId, boolean conEstadisticas) {
        PartidoDTO partido;
        try {
            partido = catalogo.partido(partidoId);
        } catch (FeignException e) {
            log.warn("No se pudo consultar el partido {} en ms-catalogo (HTTP {}). Se reintenta en la próxima liquidación.", partidoId, e.status());
            return null;
        }
        if (!"FINALIZADO".equals(partido.estado()) || partido.golesLocal() == null || partido.golesVisitante() == null) {
            return null;
        }
        Integer tarjetas = null;
        Integer corners = null;
        if (conEstadisticas) {
            try {
                EstadisticasPartidoDTO e = estadisticas.estadisticas(partidoId);
                tarjetas = e.tarjetas();
                corners = e.corners();
            } catch (FeignException.NotFound e) {
                // RN-APU-06: aún no están; las apuestas de tarjetas y córners esperan a la próxima liquidación.
            } catch (FeignException e) {
                log.warn("No se pudieron consultar las estadísticas del partido {} (HTTP {}).", partidoId, e.status());
            }
        }
        return new Final(partido.golesLocal(), partido.golesVisitante(), tarjetas, corners);
    }
}
