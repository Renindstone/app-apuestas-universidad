package pe.edu.unu.apuestas.web.vista;

import java.util.Map;

import pe.edu.unu.apuestas.web.dto.Seleccion;

/**
 * Un renglón de la tabla de mercados del análisis: probabilidad del modelo, cuota justa, cuota estimada y la cuota de cada casa.
 */
public record FilaMercado(Seleccion seleccion, String etiqueta, Double probabilidad, Double cuotaJusta, Map<String, CeldaCuota> casas) {

    /**
     * Margen típico que cobra una casa en tarjetas y córners (alrededor de 7 %). Ninguna fuente gratuita publica esas cuotas,
     * así que la columna «Estimada» muestra lo que pagaría una casa con ese margen. Es una referencia, no la cuota de una casa.
     */
    public static final double MARGEN_CASA = 0.07;

    // Cuota estimada = 1 / (p × (1 + margen)), con 2 decimales y nunca menor que 1.01. null si no hay probabilidad.
    public Double cuotaEstimada() {
        if (probabilidad == null || probabilidad <= 0) {
            return null;
        }
        double cuota = 1 / (probabilidad * (1 + MARGEN_CASA));
        return Math.max(1.01, Math.round(cuota * 100) / 100.0);
    }
}
