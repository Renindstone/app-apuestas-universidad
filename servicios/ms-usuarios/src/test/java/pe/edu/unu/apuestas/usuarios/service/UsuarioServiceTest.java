package pe.edu.unu.apuestas.usuarios.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import pe.edu.unu.apuestas.usuarios.dto.LoginRequest;
import pe.edu.unu.apuestas.usuarios.dto.LoginResponse;
import pe.edu.unu.apuestas.usuarios.dto.RegistroRequest;
import pe.edu.unu.apuestas.usuarios.entity.Rol;
import pe.edu.unu.apuestas.usuarios.entity.Usuario;
import pe.edu.unu.apuestas.usuarios.exception.CredencialesInvalidasException;
import pe.edu.unu.apuestas.usuarios.exception.UsuarioDuplicadoException;
import pe.edu.unu.apuestas.usuarios.exception.ValidacionException;
import pe.edu.unu.apuestas.usuarios.repository.UsuarioRepository;

/**
 * Pruebas unitarias de UsuarioService, con el repositorio simulado (sin base de datos).
 */
class UsuarioServiceTest {

    private static final String SECRETO = "clave-de-pruebas-de-ms-usuarios-de-32-o-mas";

    private final UsuarioRepository repositorio = mock(UsuarioRepository.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final UsuarioService servicio = new UsuarioService(repositorio, encoder, new JwtService(SECRETO, 8));

    private static RegistroRequest registro(String nombre, String email) {
        return new RegistroRequest(nombre, email, "Secreta123", "Secreta123");
    }

    // Usuario guardado con id (el id lo pone la base de datos; aquí se asigna a mano).
    private Usuario guardado(long id, String nombre, String clave, Rol rol) throws Exception {
        Usuario u = new Usuario(nombre, nombre + "@correo.pe", encoder.encode(clave), rol);
        Field campo = Usuario.class.getDeclaredField("id");
        campo.setAccessible(true);
        campo.set(u, id);
        return u;
    }

    // CB-08: «Renzo@Correo.pe» cuenta como el mismo correo que «renzo@correo.pe»: 409 y no se guarda nada.
    @Test
    void correoDuplicadoConMayusculasResponde409() {
        when(repositorio.existsByEmail("renzo@correo.pe")).thenReturn(true);
        UsuarioDuplicadoException e = assertThrows(UsuarioDuplicadoException.class,
                () -> servicio.registrar(registro("otro", "Renzo@Correo.pe")));
        assertEquals("USUARIO_DUPLICADO", e.getCodigo());
        verify(repositorio, never()).save(any());
    }

    @Test
    void nombreDeUsuarioDuplicadoResponde409() {
        when(repositorio.existsByNombreUsuario("renzo")).thenReturn(true);
        assertThrows(UsuarioDuplicadoException.class, () -> servicio.registrar(registro("renzo", "nuevo@correo.pe")));
        verify(repositorio, never()).save(any());
    }

    // RN-USU-02 y RN-USU-03: se guarda el hash (no la clave), el correo en minúsculas y el rol APOSTADOR.
    @Test
    void registroGuardaHashCorreoEnMinusculasYRolApostador() {
        when(repositorio.save(any())).thenAnswer(i -> i.getArgument(0));
        servicio.registrar(registro("  renzo ", "Renzo@Correo.PE"));
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repositorio).save(captor.capture());
        Usuario u = captor.getValue();
        assertEquals("renzo", u.getNombreUsuario());
        assertEquals("renzo@correo.pe", u.getEmail());
        assertEquals(Rol.APOSTADOR, u.getRol());
        assertNotEquals("Secreta123", u.getClaveHash());
        assertTrue(encoder.matches("Secreta123", u.getClaveHash()));
    }

    @Test
    void clavesQueNoCoincidenResponden400() {
        assertThrows(ValidacionException.class,
                () -> servicio.registrar(new RegistroRequest("renzo", "renzo@correo.pe", "Secreta123", "Otra12345")));
    }

    // Login correcto: el token trae sub = id, rol y usuario, firmado con JWT_SECRET y válido por 8 horas.
    @Test
    void loginCorrectoDevuelveTokenFirmado() throws Exception {
        when(repositorio.findByNombreUsuario("renzo")).thenReturn(Optional.of(guardado(2, "renzo", "Secreta123", Rol.APOSTADOR)));
        LoginResponse respuesta = servicio.login(new LoginRequest("renzo", "Secreta123"));

        assertEquals("Bearer", respuesta.tipo());
        assertEquals(28800, respuesta.expiraEn());
        assertEquals("APOSTADOR", respuesta.rol());
        Claims datos = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8))).build()
                .parseSignedClaims(respuesta.token()).getPayload();
        assertEquals("2", datos.getSubject());
        assertEquals("APOSTADOR", datos.get("rol", String.class));
        assertEquals("renzo", datos.get("usuario", String.class));
        long segundos = (datos.getExpiration().getTime() - datos.getIssuedAt().getTime()) / 1000;
        assertEquals(28800, segundos);
    }

    // RN-USU-04: el mensaje es el mismo si el usuario no existe o si la clave es incorrecta.
    @Test
    void loginFallidoDaElMismoMensaje() throws Exception {
        when(repositorio.findByNombreUsuario("renzo")).thenReturn(Optional.of(guardado(2, "renzo", "Secreta123", Rol.APOSTADOR)));
        when(repositorio.findByNombreUsuario("nadie")).thenReturn(Optional.empty());
        String claveMala = assertThrows(CredencialesInvalidasException.class,
                () -> servicio.login(new LoginRequest("renzo", "otra-clave"))).getMessage();
        String usuarioMalo = assertThrows(CredencialesInvalidasException.class,
                () -> servicio.login(new LoginRequest("nadie", "Secreta123"))).getMessage();
        assertEquals(claveMala, usuarioMalo);
    }

    // RN-USU-06: la banca no puede ser negativa.
    @Test
    void bancaNegativaResponde400() {
        assertThrows(ValidacionException.class, () -> servicio.actualizarBanca(2L, new java.math.BigDecimal("-1")));
    }
}
