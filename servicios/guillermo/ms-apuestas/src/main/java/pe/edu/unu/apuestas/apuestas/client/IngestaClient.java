package pe.edu.unu.apuestas.apuestas.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Feign hacia ms-ingesta. Solo para pedirle los resultados nuevos cuando hay apuestas de partidos que ya terminaron.
 */
@FeignClient(name = "ms-ingesta")
public interface IngestaClient {

    // 202 si lanzó la actualización; 409 (FeignException.Conflict) si ya había una ingesta en curso.
    @PostMapping("/interno/resultados")
    void actualizarResultados();
}
