package pe.edu.unu.apuestas.web.form;

import jakarta.validation.constraints.NotBlank;

/** Formulario de inicio de sesión. */
public class LoginForm {

    @NotBlank(message = "Escribe tu usuario.")
    private String nombreUsuario;

    @NotBlank(message = "Escribe tu contraseña.")
    private String clave;

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }
}
