package pe.edu.unu.apuestas.web.dto;

/** POST /api/usuarios/auth/registro */
public record RegistroRequest(String nombreUsuario, String email, String clave, String claveConfirmacion) {
}
