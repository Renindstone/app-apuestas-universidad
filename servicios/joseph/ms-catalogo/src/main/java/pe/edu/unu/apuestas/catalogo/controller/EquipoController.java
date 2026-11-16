package pe.edu.unu.apuestas.catalogo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.unu.apuestas.catalogo.dto.EquipoDTO;
import pe.edu.unu.apuestas.catalogo.dto.PartidoDTO;
import pe.edu.unu.apuestas.catalogo.entity.EstadoPartido;
import pe.edu.unu.apuestas.catalogo.service.CatalogoService;

/**
 * GET /api/catalogo/equipos y /api/catalogo/equipos/{id}/partidos (públicos).
 */
@RestController
@RequestMapping("/equipos")
public class EquipoController {

    private final CatalogoService catalogo;

    public EquipoController(CatalogoService catalogo) {
        this.catalogo = catalogo;
    }

    // ?ligaId=1 filtra por liga.
    @GetMapping
    public List<EquipoDTO> equipos(@RequestParam(required = false) Long ligaId) {
        return catalogo.equipos(ligaId);
    }

    // ?estado=FINALIZADO&limite=10: los últimos partidos del equipo, del más reciente al más antiguo.
    @GetMapping("/{id}/partidos")
    public List<PartidoDTO> partidos(@PathVariable Long id, @RequestParam(required = false) EstadoPartido estado,
            @RequestParam(defaultValue = "10") int limite) {
        return catalogo.partidosDeEquipo(id, estado, limite);
    }
}
