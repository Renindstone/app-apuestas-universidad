package pe.edu.unu.apuestas.web.dto;

/** Respuesta del login: el JWT y los datos para la sesión. */
public record LoginResponse(String token, String tipo, Long expiraEn, String rol, String nombreUsuario) {
}
