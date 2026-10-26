package pe.edu.unu.apuestas.apuestas.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import pe.edu.unu.apuestas.apuestas.dto.PrediccionDTO;

/**
 * Feign hacia ms-predicciones.
 */
@FeignClient(name = "ms-predicciones")
public interface PrediccionesClient {

    // Probabilidad de todos los mercados del partido. 404 si no existe y 409 si la liga aún no tiene datos.
    @GetMapping("/partidos/{id}")
    PrediccionDTO prediccion(@PathVariable Long id);
}
