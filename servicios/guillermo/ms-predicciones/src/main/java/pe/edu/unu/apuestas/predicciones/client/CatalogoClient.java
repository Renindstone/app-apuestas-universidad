package pe.edu.unu.apuestas.predicciones.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import pe.edu.unu.apuestas.predicciones.dto.PartidoDTO;

/**
 * Feign hacia ms-catalogo.
 */
@FeignClient(name = "ms-catalogo")
public interface CatalogoClient {

    // Liga, equipos y árbitro del partido. Si no existe, ms-catalogo responde 404 y Feign lanza FeignException.NotFound.
    @GetMapping("/partidos/{id}")
    PartidoDTO partido(@PathVariable Long id);
}
