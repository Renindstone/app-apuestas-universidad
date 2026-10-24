package pe.edu.unu.apuestas.catalogo.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.edu.unu.apuestas.catalogo.dto.PartidoDTO;
import pe.edu.unu.apuestas.catalogo.dto.PartidoUpsertRequest;
import pe.edu.unu.apuestas.catalogo.service.UpsertPartidoService;

/**
 * Rutas internas: solo las usa ms-ingesta dentro de la red de los servicios. El gateway las bloquea desde fuera.
 */
@RestController
@RequestMapping("/interno")
public class InternoController {

    private final UpsertPartidoService upsert;

    public InternoController(UpsertPartidoService upsert) {
        this.upsert = upsert;
    }

    // POST /interno/partidos: crea o actualiza el partido y responde 200 con el PartidoDTO (con todos los IDs).
    @PostMapping("/partidos")
    public PartidoDTO upsert(@Valid @RequestBody PartidoUpsertRequest fila) {
        return upsert.upsert(fila);
    }
}
