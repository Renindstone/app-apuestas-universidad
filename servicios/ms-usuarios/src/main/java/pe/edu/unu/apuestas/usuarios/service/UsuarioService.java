package pe.edu.unu.apuestas.usuarios.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unu.apuestas.usuarios.dto.LoginRequest;
import pe.edu.unu.apuestas.usuarios.dto.LoginResponse;
import pe.edu.unu.apuestas.usuarios.dto.RegistroRequest;
import pe.edu.unu.apuestas.usuarios.dto.RegistroResponse;
import pe.edu.unu.apuestas.usuarios.dto.UsuarioDTO;
import pe.edu.unu.apuestas.usuarios.entity.Rol;
import pe.edu.unu.apuestas.usuarios.entity.Usuario;
import pe.edu.unu.apuestas.usuarios.exception.CredencialesInvalidasException;
import pe.edu.unu.apuestas.usuarios.exception.UsuarioDuplicadoException;
import pe.edu.unu.apuestas.usuarios.exception.UsuarioNoEncontradoException;
import pe.edu.unu.apuestas.usuarios.exception.ValidacionException;
import pe.edu.unu.apuestas.usuarios.repository.UsuarioRepository;

/**
 * Reglas de las cuentas: registro, inicio de sesión, perfil y banca (RN-USU-01 a RN-USU-06).
 */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    // Hash de una clave cualquiera: si el usuario no existe, se compara contra este para tardar lo mismo que con uno real.
    private final String hashFalso;

    public UsuarioService(UsuarioRepository usuarios, PasswordEncoder encoder, JwtService jwt) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwt = jwt;
        this.hashFalso = encoder.encode("clave-que-no-es-de-nadie");
    }

    // Crea la cuenta con rol APOSTADOR, el correo en minúsculas y la clave cifrada con BCrypt.
    @Transactional
    public RegistroResponse registrar(RegistroRequest datos) {
        if (!datos.clave().equals(datos.claveConfirmacion())) {
            throw new ValidacionException("Las contraseñas no coinciden");
        }
        String nombre = datos.nombreUsuario().trim();
        String email = datos.email().trim().toLowerCase(Locale.ROOT);
        if (usuarios.existsByNombreUsuario(nombre)) {
            throw new UsuarioDuplicadoException("El nombre de usuario ya está en uso");
        }
        if (usuarios.existsByEmail(email)) {
            throw new UsuarioDuplicadoException("El correo ya está registrado");
        }
        Usuario nuevo = usuarios.save(new Usuario(nombre, email, encoder.encode(datos.clave()), Rol.APOSTADOR));
        return new RegistroResponse(nuevo.getId(), nuevo.getNombreUsuario(), nuevo.getRol().name());
    }

    // Comprueba usuario y clave y devuelve el JWT. Si algo falla, el mensaje es siempre el mismo (RN-USU-04).
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest datos) {
        Optional<Usuario> usuario = usuarios.findByNombreUsuario(datos.nombreUsuario().trim());
        boolean claveCorrecta = encoder.matches(datos.clave(), usuario.map(Usuario::getClaveHash).orElse(hashFalso));
        if (usuario.isEmpty() || !claveCorrecta) {
            throw new CredencialesInvalidasException();
        }
        Usuario u = usuario.get();
        return new LoginResponse(jwt.generar(u), "Bearer", jwt.segundosDeVida(), u.getRol().name(), u.getNombreUsuario());
    }

    // Perfil del usuario del token (el id llega en la cabecera X-Usuario-Id que pone el gateway).
    @Transactional(readOnly = true)
    public UsuarioDTO perfil(Long id) {
        return UsuarioDTO.de(buscar(id));
    }

    // Cambia la banca del usuario del token. Debe ser mayor o igual a 0 (RN-USU-06).
    @Transactional
    public UsuarioDTO actualizarBanca(Long id, BigDecimal banca) {
        if (banca.signum() < 0) {
            throw new ValidacionException("La banca debe ser mayor o igual a 0.");
        }
        Usuario usuario = buscar(id);
        usuario.setBanca(banca.setScale(2, RoundingMode.HALF_UP));
        return UsuarioDTO.de(usuario);
    }

    private Usuario buscar(Long id) {
        return usuarios.findById(id).orElseThrow(() -> new UsuarioNoEncontradoException(id));
    }
}
