package pe.edu.unu.apuestas.cuotas.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign hacia ms-catalogo.
 */
@FeignClient(name = "ms-catalogo")
public interface CatalogoClient {

    // Solo interesa si el partido existe: si no, ms-catalogo responde 404 y Feign lanza FeignException.NotFound.
    @GetMapping("/partidos/{id}")
    void partido(@PathVariable Long id);
}
