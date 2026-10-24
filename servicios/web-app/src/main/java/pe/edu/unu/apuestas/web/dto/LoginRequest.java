package pe.edu.unu.apuestas.web.dto;

/** POST /api/usuarios/auth/login */
public record LoginRequest(String nombreUsuario, String clave) {
}
