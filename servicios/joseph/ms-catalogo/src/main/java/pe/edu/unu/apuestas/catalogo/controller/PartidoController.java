package pe.edu.unu.apuestas.catalogo.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.unu.apuestas.catalogo.dto.PartidoDTO;
import pe.edu.unu.apuestas.catalogo.entity.EstadoPartido;
import pe.edu.unu.apuestas.catalogo.service.CatalogoService;

/**
 * GET /api/catalogo/partidos y /api/catalogo/partidos/{id} (públicos).
 */
@RestController
@RequestMapping("/partidos")
public class PartidoController {

    private final CatalogoService catalogo;

    public PartidoController(CatalogoService catalogo) {
        this.catalogo = catalogo;
    }

    // ?fecha=2026-10-24 (un día de Lima), o ?desde=…&hasta=…, más ligaId y estado. Todos opcionales.
    @GetMapping
    public List<PartidoDTO> partidos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long ligaId,
            @RequestParam(required = false) EstadoPartido estado) {
        return catalogo.partidos(fecha, desde, hasta, ligaId, estado);
    }

    // Un partido, o 404 PARTIDO_NO_ENCONTRADO.
    @GetMapping("/{id}")
    public PartidoDTO partido(@PathVariable Long id) {
        return catalogo.partido(id);
    }
}
