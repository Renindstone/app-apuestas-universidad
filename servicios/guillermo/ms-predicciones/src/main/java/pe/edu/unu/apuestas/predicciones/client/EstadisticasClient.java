package pe.edu.unu.apuestas.predicciones.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import pe.edu.unu.apuestas.predicciones.dto.PerfilArbitroDTO;
import pe.edu.unu.apuestas.predicciones.dto.PromediosEquipoDTO;
import pe.edu.unu.apuestas.predicciones.dto.PromediosLigaDTO;

/**
 * Feign hacia ms-estadisticas.
 */
@FeignClient(name = "ms-estadisticas")
public interface EstadisticasClient {

    // Medias de la liga como local y como visitante, de las 2 temporadas cargadas.
    @GetMapping("/ligas/{ligaId}/promedios")
    PromediosLigaDTO liga(@PathVariable Long ligaId);

    // A favor y en contra del equipo en sus últimos N partidos en esa condición (LOCAL o VISITANTE).
    @GetMapping("/equipos/{equipoId}/promedios")
    PromediosEquipoDTO equipo(@PathVariable Long equipoId, @RequestParam String condicion, @RequestParam int ultimos);

    // Tarjetas por partido del árbitro y la media de su liga.
    @GetMapping("/arbitros/{arbitroId}/perfil")
    PerfilArbitroDTO arbitro(@PathVariable Long arbitroId);
}
