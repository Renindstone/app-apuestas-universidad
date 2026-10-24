package pe.edu.unu.apuestas.web.dto;

import java.math.BigDecimal;

/** GET /api/usuarios/me */
public record UsuarioDTO(Long id, String nombreUsuario, String email, String rol, BigDecimal banca) {
}
