package pe.edu.unu.apuestas.ingesta.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import pe.edu.unu.apuestas.ingesta.dto.CuotasRequest;

/**
 * Feign hacia ms-cuotas.
 */
@FeignClient(name = "ms-cuotas")
public interface CuotasClient {

    // Guarda las cuotas de una casa para un partido. ms-cuotas responde {"guardadas": n}, que aquí no se usa.
    @PostMapping("/interno/cuotas")
    void guardar(@RequestBody CuotasRequest cuotas);
}
