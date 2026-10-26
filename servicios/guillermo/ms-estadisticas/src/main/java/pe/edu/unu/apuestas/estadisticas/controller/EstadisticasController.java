package pe.edu.unu.apuestas.estadisticas.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.unu.apuestas.estadisticas.dto.EstadisticasPartidoDTO;
import pe.edu.unu.apuestas.estadisticas.dto.PerfilArbitroDTO;
import pe.edu.unu.apuestas.estadisticas.dto.PromediosEquipoDTO;
import pe.edu.unu.apuestas.estadisticas.dto.PromediosLigaDTO;
import pe.edu.unu.apuestas.estadisticas.entity.Condicion;
import pe.edu.unu.apuestas.estadisticas.service.EstadisticasService;

/**
 * Consultas públicas (vía gateway: /api/estadisticas + ruta). Las usan ms-predicciones y ms-apuestas.
 */
@RestController
public class EstadisticasController {

    private final EstadisticasService estadisticas;

    public EstadisticasController(EstadisticasService estadisticas) {
        this.estadisticas = estadisticas;
    }

    // ?condicion=LOCAL, VISITANTE o TODOS (por defecto) y ?ultimos=10 (por defecto).
    @GetMapping("/equipos/{equipoId}/promedios")
    public PromediosEquipoDTO promediosDeEquipo(@PathVariable Long equipoId,
            @RequestParam(defaultValue = "TODOS") Condicion condicion, @RequestParam(defaultValue = "10") int ultimos) {
        return estadisticas.promediosDeEquipo(equipoId, condicion, ultimos);
    }

    // ?temporada=2627 es opcional: sin ella se usan las 2 temporadas cargadas.
    @GetMapping("/ligas/{ligaId}/promedios")
    public PromediosLigaDTO promediosDeLiga(@PathVariable Long ligaId, @RequestParam(required = false) String temporada) {
        return estadisticas.promediosDeLiga(ligaId, temporada);
    }

    @GetMapping("/arbitros/{arbitroId}/perfil")
    public PerfilArbitroDTO perfilDeArbitro(@PathVariable Long arbitroId) {
        return estadisticas.perfilDeArbitro(arbitroId);
    }

    @GetMapping("/partidos/{partidoId}/estadisticas")
    public EstadisticasPartidoDTO estadisticasDePartido(@PathVariable Long partidoId) {
        return estadisticas.estadisticasDePartido(partidoId);
    }
}
