package pe.edu.unu.apuestas.usuarios.dto;

import java.math.BigDecimal;

import pe.edu.unu.apuestas.usuarios.entity.Usuario;

/** Perfil que devuelven GET /me y PUT /me/banca. Nunca incluye el hash de la contraseña. */
public record UsuarioDTO(Long id, String nombreUsuario, String email, String rol, BigDecimal banca) {

    public static UsuarioDTO de(Usuario u) {
        return new UsuarioDTO(u.getId(), u.getNombreUsuario(), u.getEmail(), u.getRol().name(), u.getBanca());
    }
}
