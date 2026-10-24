package pe.edu.unu.apuestas.usuarios.dto;

/** Respuesta del login: el JWT, cuántos segundos dura y los datos que la web guarda en la sesión. */
public record LoginResponse(String token, String tipo, long expiraEn, String rol, String nombreUsuario) {
}
