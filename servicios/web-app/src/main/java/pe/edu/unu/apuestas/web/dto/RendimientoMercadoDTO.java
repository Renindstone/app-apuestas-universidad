package pe.edu.unu.apuestas.web.dto;

import java.math.BigDecimal;

/** Rendimiento de un mercado dentro de GET /mis-apuestas/rendimiento. */
public record RendimientoMercadoDTO(String mercado, int apuestas, BigDecimal beneficio, double roi) {
}
