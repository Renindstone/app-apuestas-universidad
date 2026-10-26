package pe.edu.unu.apuestas.usuarios.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** POST /auth/registro. El rol no viene aquí: el servidor siempre pone APOSTADOR (RN-USU-03). */
public record RegistroRequest(
        @NotBlank(message = "Escribe un nombre de usuario.")
        @Size(max = 30, message = "El usuario puede tener hasta 30 caracteres.")
        String nombreUsuario,

        @NotBlank(message = "Escribe tu correo.")
        @Email(message = "Escribe un correo válido.")
        @Size(max = 100, message = "El correo puede tener hasta 100 caracteres.")
        String email,

        @NotBlank(message = "Escribe una contraseña.")
        @Size(min = 6, max = 72, message = "La contraseña debe tener entre 6 y 72 caracteres.")
        String clave,

        @NotBlank(message = "Repite la contraseña.")
        String claveConfirmacion) {
}
