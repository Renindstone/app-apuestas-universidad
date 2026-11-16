package pe.edu.unu.apuestas.cuotas.dto;

import java.math.BigDecimal;
import java.util.Map;

/** GET /partidos/{id}/probabilidades-mercado: margen de la casa y probabilidad sin margen de cada selección, con 3 decimales. */
public record ProbabilidadesMercadoDTO(String casa, String mercado, BigDecimal linea, double margen,
        Map<String, Double> probabilidades) {
}
