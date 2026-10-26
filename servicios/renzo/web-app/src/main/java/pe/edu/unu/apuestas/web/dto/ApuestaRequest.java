package pe.edu.unu.apuestas.web.dto;

import java.math.BigDecimal;

/** POST /api/apuestas/mis-apuestas */
public record ApuestaRequest(Long partidoId, String mercado, String seleccion, Double linea, String casa, BigDecimal cuota,
        BigDecimal monto) {
}
