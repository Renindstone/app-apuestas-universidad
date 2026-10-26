package pe.edu.unu.apuestas.web.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import pe.edu.unu.apuestas.web.dto.BancaRequest;
import pe.edu.unu.apuestas.web.dto.LoginRequest;
import pe.edu.unu.apuestas.web.dto.LoginResponse;
import pe.edu.unu.apuestas.web.dto.RegistroRequest;
import pe.edu.unu.apuestas.web.dto.UsuarioDTO;

/**
 * Llamadas a ms-usuarios a través del gateway (/api/usuarios).
 */
@Component
public class UsuariosClient {

    private final RestClient gateway;

    public UsuariosClient(RestClient gateway) {
        this.gateway = gateway;
    }

    // Inicia sesión y devuelve el JWT. Credenciales malas: el gateway responde 401.
    public LoginResponse login(LoginRequest datos) {
        return gateway.post().uri("/api/usuarios/auth/login").body(datos).retrieve().body(LoginResponse.class);
    }

    // Crea una cuenta. Usuario o correo repetidos: 409 USUARIO_DUPLICADO.
    public void registrar(RegistroRequest datos) {
        gateway.post().uri("/api/usuarios/auth/registro").body(datos).retrieve().toBodilessEntity();
    }

    // Perfil del usuario del token, con su banca.
    public UsuarioDTO perfil() {
        return gateway.get().uri("/api/usuarios/me").retrieve().body(UsuarioDTO.class);
    }

    // Cambia la banca y devuelve el perfil actualizado.
    public UsuarioDTO actualizarBanca(BancaRequest datos) {
        return gateway.put().uri("/api/usuarios/me/banca").body(datos).retrieve().body(UsuarioDTO.class);
    }
}
