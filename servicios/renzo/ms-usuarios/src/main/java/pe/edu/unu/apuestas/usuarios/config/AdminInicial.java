package pe.edu.unu.apuestas.usuarios.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import pe.edu.unu.apuestas.usuarios.entity.Rol;
import pe.edu.unu.apuestas.usuarios.entity.Usuario;
import pe.edu.unu.apuestas.usuarios.repository.UsuarioRepository;

/**
 * Al arrancar, crea el usuario admin si no existe (RN-USU-07). Su contraseña sale de ADMIN_PASSWORD.
 */
@Component
public class AdminInicial implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicial.class);
    private static final String NOMBRE = "admin";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final String clave;

    public AdminInicial(UsuarioRepository usuarios, PasswordEncoder encoder, @Value("${admin.clave}") String clave) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.clave = clave;
    }

    @Override
    public void run(String... args) {
        if (usuarios.existsByNombreUsuario(NOMBRE)) {
            return;
        }
        // Sin ADMIN_PASSWORD no se inventa una clave: se avisa en el log y el servicio sigue funcionando.
        if (clave == null || clave.length() < 6) {
            log.warn("No se creó el usuario admin: define la variable ADMIN_PASSWORD (6 caracteres o más).");
            return;
        }
        usuarios.save(new Usuario(NOMBRE, "admin@apuestas.local", encoder.encode(clave), Rol.ADMIN));
        log.info("Usuario admin creado.");
    }
}
