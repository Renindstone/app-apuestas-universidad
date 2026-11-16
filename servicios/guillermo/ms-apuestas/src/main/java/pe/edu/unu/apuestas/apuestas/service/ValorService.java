package pe.edu.unu.apuestas.apuestas.service;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import pe.edu.unu.apuestas.apuestas.client.PrediccionesClient;
import pe.edu.unu.apuestas.apuestas.dto.EvaluacionDTO;
import pe.edu.unu.apuestas.apuestas.dto.EvaluacionRequest;
import pe.edu.unu.apuestas.apuestas.dto.PrediccionDTO;
import pe.edu.unu.apuestas.apuestas.exception.ApiException;

/**
 * Valor esperado, stake sugerido y veredicto de una cuota (RN-03 a RN-05), y POST /evaluaciones.
 */
@Service
public class ValorService {

    // RN-04: desde un EV de 3 % la cuota tiene valor.
    private static final double UMBRAL_VALOR = 0.03;
    // RN-05: un cuarto de Kelly, con un tope de 3 % de la banca.
    private static final double FRACCION_KELLY = 0.25;
    private static final double TOPE_STAKE = 0.03;

    public enum Veredicto {
        CON_VALOR, MARGINAL, SIN_VALOR
    }

    /** Resultado de evaluar: EV y stake con 3 decimales, y el veredicto. */
    public record Valor(double valorEsperado, double stakePct, Veredicto veredicto) {
    }

    private final PrediccionesClient predicciones;

    public ValorService(PrediccionesClient predicciones) {
        this.predicciones = predicciones;
    }

    /**
     * RN-03: EV = p × cuota − 1. RN-05: stake = min(0.25 × EV / (cuota − 1), 0.03), o 0 si el EV no es positivo.
     * RN-04: CON_VALOR si EV ≥ 0.03, MARGINAL si 0 < EV < 0.03 y SIN_VALOR si no. El veredicto usa el EV redondeado,
     * para que coincida con el número que se muestra. La cuota debe ser mayor que 1.
     */
    public static Valor evaluar(double probabilidad, double cuota) {
        double crudo = probabilidad * cuota - 1;
        double valorEsperado = redondear(crudo);
        double stake = valorEsperado > 0 ? Math.min(FRACCION_KELLY * crudo / (cuota - 1), TOPE_STAKE) : 0;
        Veredicto veredicto = valorEsperado >= UMBRAL_VALOR ? Veredicto.CON_VALOR
                : valorEsperado > 0 ? Veredicto.MARGINAL : Veredicto.SIN_VALOR;
        return new Valor(valorEsperado, redondear(stake), veredicto);
    }

    /**
     * POST /evaluaciones: la probabilidad sale siempre de ms-predicciones y el usuario solo manda la cuota (RN-APU-01).
     * Por eso funciona aunque ms-cuotas no responda.
     */
    public EvaluacionDTO evaluacion(EvaluacionRequest datos) {
        Mercados.validar(datos.mercado(), datos.seleccion(), datos.linea());
        Mercados.validarCasa(datos.casa());
        if (datos.cuota().compareTo(BigDecimal.ONE) <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDACION", "La cuota debe ser mayor que 1.");
        }
        PrediccionDTO prediccion = Remoto.llamar("ms-predicciones", datos.partidoId(), () -> predicciones.prediccion(datos.partidoId()));
        // ms-predicciones trae todos los mercados de RN-09: si falta uno, es un error del sistema (500).
        PrediccionDTO.Mercado mercado = buscar(prediccion, datos.mercado(), datos.seleccion(), datos.linea()).orElseThrow();
        Valor valor = evaluar(mercado.probabilidad(), datos.cuota().doubleValue());
        return new EvaluacionDTO(mercado.probabilidad(), mercado.cuotaJusta(), valor.valorEsperado(), valor.stakePct(),
                valor.veredicto().name());
    }

    // La probabilidad del modelo para una selección. Las líneas se comparan como números: 2.5 y 2.50 son la misma.
    static Optional<PrediccionDTO.Mercado> buscar(PrediccionDTO prediccion, String mercado, String seleccion, BigDecimal linea) {
        return prediccion.mercados().stream()
                .filter(m -> m.mercado().equals(mercado) && m.seleccion().equals(seleccion))
                .filter(m -> linea == null ? m.linea() == null : m.linea() != null && BigDecimal.valueOf(m.linea()).compareTo(linea) == 0)
                .findFirst();
    }

    private static double redondear(double valor) {
        return Math.round(valor * 1000) / 1000.0;
    }
}
