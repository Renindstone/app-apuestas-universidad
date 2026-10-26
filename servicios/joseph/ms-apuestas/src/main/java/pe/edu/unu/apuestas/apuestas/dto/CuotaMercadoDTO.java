package pe.edu.unu.apuestas.apuestas.dto;

import java.math.BigDecimal;

/** Una cuota de ms-cuotas (GET /partidos/{id}/cuotas). Solo están los campos que usa este servicio. */
public record CuotaMercadoDTO(String casa, String mercado, String seleccion, BigDecimal linea, BigDecimal valor) {
}
