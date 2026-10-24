package pe.edu.unu.apuestas.usuarios.dto;

import jakarta.validation.constraints.NotBlank;

/** POST /auth/login */
public record LoginRequest(
        @NotBlank(message = "Escribe tu usuario.") String nombreUsuario,
        @NotBlank(message = "Escribe tu contraseña.") String clave) {
}
