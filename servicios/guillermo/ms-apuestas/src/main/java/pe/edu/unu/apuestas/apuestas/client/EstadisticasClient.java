package pe.edu.unu.apuestas.apuestas.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import pe.edu.unu.apuestas.apuestas.dto.EstadisticasPartidoDTO;

/**
 * Feign hacia ms-estadisticas.
 */
@FeignClient(name = "ms-estadisticas")
public interface EstadisticasClient {

    // Lo que hizo cada equipo en el partido. 404 si aún no hay estadísticas.
    @GetMapping("/partidos/{id}/estadisticas")
    EstadisticasPartidoDTO estadisticas(@PathVariable Long id);
}
