package pe.edu.unu.apuestas.apuestas.dto;

import java.math.BigDecimal;

/** El rendimiento de un mercado dentro de GET /mis-apuestas/rendimiento. Solo apuestas liquidadas. */
public record RendimientoMercadoDTO(String mercado, int apuestas, BigDecimal beneficio, double roi) {
}
