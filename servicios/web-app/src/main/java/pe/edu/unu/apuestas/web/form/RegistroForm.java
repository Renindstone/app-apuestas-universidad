package pe.edu.unu.apuestas.web.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Formulario de registro. Las reglas son las mismas que valida ms-usuarios (RN-USU-01 y RN-USU-02). */
public class RegistroForm {

    @NotBlank(message = "Escribe un nombre de usuario.")
    @Size(max = 30, message = "El usuario puede tener hasta 30 caracteres.")
    private String nombreUsuario;

    @NotBlank(message = "Escribe tu correo.")
    @Email(message = "Escribe un correo válido.")
    private String email;

    @Size(min = 6, max = 72, message = "La contraseña debe tener entre 6 y 72 caracteres.")
    private String clave = "";

    private String claveConfirmacion = "";

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public String getClaveConfirmacion() {
        return claveConfirmacion;
    }

    public void setClaveConfirmacion(String claveConfirmacion) {
        this.claveConfirmacion = claveConfirmacion;
    }
}
