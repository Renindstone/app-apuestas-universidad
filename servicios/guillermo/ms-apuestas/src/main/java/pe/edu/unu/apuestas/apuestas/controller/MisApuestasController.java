package pe.edu.unu.apuestas.apuestas.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.edu.unu.apuestas.apuestas.dto.ApuestaDTO;
import pe.edu.unu.apuestas.apuestas.dto.ApuestaRequest;
import pe.edu.unu.apuestas.apuestas.dto.EstadoApuesta;
import pe.edu.unu.apuestas.apuestas.dto.RendimientoDTO;
import pe.edu.unu.apuestas.apuestas.service.MisApuestasService;

/**
 * Las apuestas del usuario del token. El servicio no valida el JWT: lee X-Usuario-Id, que pone el gateway (RN-14).
 */
@RestController
@RequestMapping("/mis-apuestas")
public class MisApuestasController {

    private final MisApuestasService misApuestas;

    public MisApuestasController(MisApuestasService misApuestas) {
        this.misApuestas = misApuestas;
    }

    // Registra una apuesta que el usuario hizo por su cuenta. Responde 201 con la apuesta guardada.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApuestaDTO registrar(@RequestHeader("X-Usuario-Id") Long usuarioId, @Valid @RequestBody ApuestaRequest datos) {
        return misApuestas.registrar(usuarioId, datos);
    }

    // ?estado=PENDIENTE o LIQUIDADA (opcional).
    @GetMapping
    public List<ApuestaDTO> listar(@RequestHeader("X-Usuario-Id") Long usuarioId, @RequestParam(required = false) EstadoApuesta estado) {
        return misApuestas.listar(usuarioId, estado);
    }

    @GetMapping("/rendimiento")
    public RendimientoDTO rendimiento(@RequestHeader("X-Usuario-Id") Long usuarioId) {
        return misApuestas.rendimiento(usuarioId);
    }
}
