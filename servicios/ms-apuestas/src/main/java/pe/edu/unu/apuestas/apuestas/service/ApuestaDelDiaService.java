package pe.edu.unu.apuestas.apuestas.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import pe.edu.unu.apuestas.apuestas.client.CatalogoClient;
import pe.edu.unu.apuestas.apuestas.client.CuotasClient;
import pe.edu.unu.apuestas.apuestas.client.PrediccionesClient;
import pe.edu.unu.apuestas.apuestas.dto.ApuestaDelDiaDTO;
import pe.edu.unu.apuestas.apuestas.dto.CuotaMercadoDTO;
import pe.edu.unu.apuestas.apuestas.dto.HistorialDTO;
import pe.edu.unu.apuestas.apuestas.dto.PartidoDTO;
import pe.edu.unu.apuestas.apuestas.dto.PickDTO;
import pe.edu.unu.apuestas.apuestas.dto.PickHistorialDTO;
import pe.edu.unu.apuestas.apuestas.dto.PrediccionDTO;
import pe.edu.unu.apuestas.apuestas.entity.EstadoPick;
import pe.edu.unu.apuestas.apuestas.entity.Pick;
import pe.edu.unu.apuestas.apuestas.entity.Resultado;
import pe.edu.unu.apuestas.apuestas.exception.ApiException;
import pe.edu.unu.apuestas.apuestas.repository.PickRepository;

/**
 * Apuesta del día (RN-06, RN-APU-02 y RN-APU-03) y su historial (RN-APU-08).
 */
@Service
public class ApuestaDelDiaService {

    private static final Logger log = LoggerFactory.getLogger(ApuestaDelDiaService.class);
    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    // RN-06: umbrales de una candidata. RN-APU-03: Pinnacle no se usa, porque el equipo apuesta en bet365 y Betano.
    private static final Set<String> CASAS = Set.of("bet365", "betano");
    private static final BigDecimal CUOTA_MINIMA = new BigDecimal("1.50");
    private static final BigDecimal CUOTA_MAXIMA = new BigDecimal("2.60");
    private static final double PROBABILIDAD_MINIMA = 0.42;
    private static final double VALOR_MINIMO = 0.03;
    private static final int MUESTRA_MINIMA = 5;

    // RN-16: sin candidatas se dice con claridad.
    static final String SIN_APUESTA = "Hoy no hay apuesta del día: ninguna apuesta supera los umbrales. No apostar también es una decisión.";

    private final PickRepository picks;
    private final CatalogoClient catalogo;
    private final PrediccionesClient predicciones;
    private final CuotasClient cuotas;

    public ApuestaDelDiaService(PickRepository picks, CatalogoClient catalogo, PrediccionesClient predicciones, CuotasClient cuotas) {
        this.picks = picks;
        this.catalogo = catalogo;
        this.predicciones = predicciones;
        this.cuotas = cuotas;
    }

    /**
     * GET /apuesta-del-dia. Sin fecha, es hoy en Lima. Si ya hay picks guardados de ese día se devuelven; con
     * recalcular=true se generan de nuevo (RN-APU-02). synchronized: si la tarea de las 09:00 y un usuario piden la misma
     * fecha a la vez, la segunda espera y encuentra la guardada en lugar de generar otra.
     */
    public synchronized ApuestaDelDiaDTO apuestaDelDia(LocalDate fecha, boolean recalcular) {
        LocalDate dia = fecha == null ? LocalDate.now(LIMA) : fecha;
        List<Pick> guardados = picks.findByFecha(dia);
        if (!guardados.isEmpty() && !recalcular) {
            return respuesta(dia, guardados);
        }
        // Recalcular un día ya liquidado borraría su historial y podría dejar dos apuestas del día.
        if (guardados.stream().anyMatch(p -> p.getEstado() == EstadoPick.LIQUIDADO)) {
            throw new ApiException(HttpStatus.CONFLICT, "YA_LIQUIDADA", "La apuesta del día del " + dia + " ya se liquidó: no se puede recalcular.");
        }
        List<Pick> candidatas = candidatas(dia);
        candidatas.stream().max(Comparator.comparingDouble(Pick::getValorEsperado)).ifPresent(Pick::marcarComoApuestaDelDia);
        picks.deleteAll(guardados);
        return respuesta(dia, picks.saveAll(candidatas));
    }

    // RN-APU-02: se genera sola a las 09:00 de Lima. Si falla (por ejemplo, ms-cuotas no responde), se registra en el log.
    @Scheduled(cron = "0 0 9 * * *", zone = "America/Lima")
    public void generarProgramada() {
        try {
            ApuestaDelDiaDTO hoy = apuestaDelDia(null, false);
            log.info("Apuesta del día {}: {}", hoy.fecha(), hoy.pick() == null ? "sin candidatas" : hoy.pick().partido());
        } catch (ApiException e) {
            log.warn("No se pudo generar la apuesta del día: {}", e.getMessage());
        }
    }

    /**
     * RN-06: por cada partido PROGRAMADO del día, compara las cuotas de bet365 y Betano con la probabilidad del modelo.
     * Son candidatas las de cuota entre 1.50 y 2.60, probabilidad ≥ 0.42, EV ≥ 0.03 y al menos 5 partidos de muestra por
     * equipo. Un partido sin datos para el modelo se salta; si un servicio no responde, 503 (RN-APU-04).
     */
    private List<Pick> candidatas(LocalDate dia) {
        List<Pick> candidatas = new ArrayList<>();
        List<PartidoDTO> partidos = Remoto.llamar("ms-catalogo", null, () -> catalogo.partidos(dia.toString(), "PROGRAMADO"));
        for (PartidoDTO partido : partidos) {
            Optional<PrediccionDTO> prediccion = prediccion(partido.id());
            if (prediccion.isEmpty() || prediccion.get().muestra().partidosLocal() < MUESTRA_MINIMA
                    || prediccion.get().muestra().partidosVisitante() < MUESTRA_MINIMA) {
                continue;
            }
            for (CuotaMercadoDTO cuota : Remoto.llamar("ms-cuotas", partido.id(), () -> cuotas.cuotas(partido.id()))) {
                if (!CASAS.contains(cuota.casa()) || cuota.valor().compareTo(CUOTA_MINIMA) < 0 || cuota.valor().compareTo(CUOTA_MAXIMA) > 0) {
                    continue;
                }
                ValorService.buscar(prediccion.get(), cuota.mercado(), cuota.seleccion(), cuota.linea()).ifPresent(mercado -> {
                    ValorService.Valor valor = ValorService.evaluar(mercado.probabilidad(), cuota.valor().doubleValue());
                    if (mercado.probabilidad() >= PROBABILIDAD_MINIMA && valor.valorEsperado() >= VALOR_MINIMO) {
                        candidatas.add(new Pick(dia, partido.id(), partido.descripcion(), cuota.mercado(), cuota.seleccion(), cuota.linea(),
                                cuota.casa(), cuota.valor(), mercado.probabilidad(), valor.valorEsperado(), valor.stakePct()));
                    }
                });
            }
        }
        return candidatas;
    }

    // La predicción del partido, o vacío si no existe o si el modelo aún no tiene datos. Si ms-predicciones no responde, 503.
    private Optional<PrediccionDTO> prediccion(Long partidoId) {
        try {
            return Optional.of(Remoto.llamar("ms-predicciones", partidoId, () -> predicciones.prediccion(partidoId)));
        } catch (ApiException e) {
            if (e.getEstado() == HttpStatus.SERVICE_UNAVAILABLE) {
                throw e;
            }
            return Optional.empty();
        }
    }

    // La apuesta del día y cuántas candidatas más hubo. Sin ninguna, el mensaje de RN-16.
    private static ApuestaDelDiaDTO respuesta(LocalDate dia, List<Pick> candidatas) {
        return candidatas.stream().filter(Pick::isEsApuestaDelDia).findFirst()
                .map(pick -> new ApuestaDelDiaDTO(dia, PickDTO.de(pick), candidatas.size() - 1, null))
                .orElse(new ApuestaDelDiaDTO(dia, null, 0, SIN_APUESTA));
    }

    /**
     * GET /picks/historial: las apuestas del día liquidadas en los últimos días, con stake plano de 1 unidad (RN-APU-08).
     * Una ganada suma cuota − 1 y una perdida resta 1. ROI = beneficio / picks.
     */
    public HistorialDTO historial(int dias) {
        if (dias < 1 || dias > 365) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDACION", "«dias» debe estar entre 1 y 365.");
        }
        List<Pick> liquidados = picks.findByEsApuestaDelDiaTrueAndEstadoAndFechaGreaterThanEqualOrderByFechaDesc(EstadoPick.LIQUIDADO,
                LocalDate.now(LIMA).minusDays(dias));
        int ganados = (int) liquidados.stream().filter(p -> p.getResultado() == Resultado.GANADA).count();
        double beneficio = liquidados.stream()
                .mapToDouble(p -> p.getResultado() == Resultado.GANADA ? p.getCuota().doubleValue() - 1 : -1).sum();
        double roi = liquidados.isEmpty() ? 0 : Math.round(beneficio / liquidados.size() * 1000) / 1000.0;
        return new HistorialDTO(liquidados.size(), ganados, liquidados.size() - ganados, Math.round(beneficio * 100) / 100.0, roi,
                liquidados.stream().map(PickHistorialDTO::de).toList());
    }
}
