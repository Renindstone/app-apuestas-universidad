package pe.edu.unu.apuestas.web.dto;

import java.math.BigDecimal;

/** POST /api/apuestas/evaluaciones: el usuario solo manda la cuota; la probabilidad la pone el modelo. */
public record EvaluacionRequest(Long partidoId, String mercado, String seleccion, Double linea, String casa, BigDecimal cuota) {
}
