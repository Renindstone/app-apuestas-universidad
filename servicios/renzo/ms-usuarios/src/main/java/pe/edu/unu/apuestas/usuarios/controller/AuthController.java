package pe.edu.unu.apuestas.usuarios.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.edu.unu.apuestas.usuarios.dto.LoginRequest;
import pe.edu.unu.apuestas.usuarios.dto.LoginResponse;
import pe.edu.unu.apuestas.usuarios.dto.RegistroRequest;
import pe.edu.unu.apuestas.usuarios.dto.RegistroResponse;
import pe.edu.unu.apuestas.usuarios.service.UsuarioService;

/**
 * Rutas públicas (sin token): /api/usuarios/auth/registro y /api/usuarios/auth/login a través del gateway.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UsuarioService usuarios;

    public AuthController(UsuarioService usuarios) {
        this.usuarios = usuarios;
    }

    // 201 con la cuenta creada; 400 si un dato no es válido o las claves no coinciden; 409 si el usuario o el correo existen.
    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistroResponse registrar(@Valid @RequestBody RegistroRequest datos) {
        return usuarios.registrar(datos);
    }

    // 200 con el JWT; 401 CREDENCIALES_INVALIDAS si el usuario o la clave fallan.
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest datos) {
        return usuarios.login(datos);
    }
}
