package pe.edu.unu.apuestas.usuarios.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.edu.unu.apuestas.usuarios.dto.BancaRequest;
import pe.edu.unu.apuestas.usuarios.dto.UsuarioDTO;
import pe.edu.unu.apuestas.usuarios.service.UsuarioService;

/**
 * Perfil y banca del usuario autenticado. El servicio no valida el JWT: lee X-Usuario-Id, que pone el gateway.
 */
@RestController
@RequestMapping("/me")
public class UsuarioController {

    private final UsuarioService usuarios;

    public UsuarioController(UsuarioService usuarios) {
        this.usuarios = usuarios;
    }

    // GET /api/usuarios/me
    @GetMapping
    public UsuarioDTO perfil(@RequestHeader("X-Usuario-Id") Long usuarioId) {
        return usuarios.perfil(usuarioId);
    }

    // PUT /api/usuarios/me/banca con {"banca": 500.00}
    @PutMapping("/banca")
    public UsuarioDTO actualizarBanca(@RequestHeader("X-Usuario-Id") Long usuarioId, @Valid @RequestBody BancaRequest datos) {
        return usuarios.actualizarBanca(usuarioId, datos.banca());
    }
}
