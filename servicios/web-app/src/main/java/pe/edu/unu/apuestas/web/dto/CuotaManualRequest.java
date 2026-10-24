package pe.edu.unu.apuestas.web.dto;

import java.math.BigDecimal;

/** POST /api/cuotas/cuotas/manual (cuota de Betano cargada a mano). */
public record CuotaManualRequest(Long partidoId, String casa, String mercado, String seleccion, Double linea, BigDecimal valor) {
}
