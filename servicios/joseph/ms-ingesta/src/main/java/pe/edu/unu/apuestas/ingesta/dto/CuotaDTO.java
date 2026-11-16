package pe.edu.unu.apuestas.ingesta.dto;

import java.math.BigDecimal;

/** Una cuota tal como la recibe ms-cuotas: {"mercado": "GOLES", "seleccion": "MAS", "linea": 2.5, "valor": 1.90}. */
public record CuotaDTO(String mercado, String seleccion, BigDecimal linea, BigDecimal valor) {
}
