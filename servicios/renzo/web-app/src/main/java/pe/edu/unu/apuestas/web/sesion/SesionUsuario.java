package pe.edu.unu.apuestas.web.sesion;

import java.io.Serializable;
import java.math.BigDecimal;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

/**
 * Datos del usuario que inició sesión. Viven en la HttpSession del servidor, nunca en el navegador (RN-WEB-02).
 */
@Component
@SessionScope
public class SesionUsuario implements Serializable {

    private String token;
    private String nombreUsuario;
    private String rol;
    private BigDecimal banca;

    // Guarda el JWT y los datos que devolvió el login.
    public void iniciar(String token, String nombreUsuario, String rol) {
        this.token = token;
        this.nombreUsuario = nombreUsuario;
        this.rol = rol;
    }

    // Borra todo: se usa al salir y cuando el gateway responde 401.
    public void cerrar() {
        token = null;
        nombreUsuario = null;
        rol = null;
        banca = null;
    }

    public boolean estaAutenticado() {
        return token != null;
    }

    public boolean esAdmin() {
        return "ADMIN".equals(rol);
    }

    public String getToken() {
        return token;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public String getRol() {
        return rol;
    }

    public BigDecimal getBanca() {
        return banca;
    }

    public void setBanca(BigDecimal banca) {
        this.banca = banca;
    }
}
