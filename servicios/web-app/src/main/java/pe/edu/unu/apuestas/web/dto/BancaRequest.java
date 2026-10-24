package pe.edu.unu.apuestas.web.dto;

import java.math.BigDecimal;

/** PUT /api/usuarios/me/banca */
public record BancaRequest(BigDecimal banca) {
}
