package pe.edu.unu.apuestas.ingesta.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import pe.edu.unu.apuestas.ingesta.dto.EstadisticasRequest;

/**
 * Feign hacia ms-estadisticas.
 */
@FeignClient(name = "ms-estadisticas")
public interface EstadisticasClient {

    // Guarda las estadísticas de los dos equipos de un partido ya jugado.
    @PostMapping("/interno/estadisticas")
    void guardar(@RequestBody EstadisticasRequest estadisticas);
}
