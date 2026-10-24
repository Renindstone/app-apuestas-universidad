package pe.edu.unu.apuestas.web.vista;

import java.util.Map;

import pe.edu.unu.apuestas.web.dto.Seleccion;

/**
 * Un renglón de la tabla de mercados del análisis: probabilidad del modelo, cuota justa y la cuota de cada casa.
 */
public record FilaMercado(Seleccion seleccion, String etiqueta, Double probabilidad, Double cuotaJusta, Map<String, CeldaCuota> casas) {
}
