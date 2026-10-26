package pe.edu.unu.apuestas.usuarios;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Cuentas de usuario: registro, inicio de sesión (emite el JWT), perfil y banca.
 * Es el único servicio que conoce las contraseñas, y solo guarda su hash BCrypt.
 */
@SpringBootApplication
public class MsUsuariosApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsUsuariosApplication.class, args);
    }
}
