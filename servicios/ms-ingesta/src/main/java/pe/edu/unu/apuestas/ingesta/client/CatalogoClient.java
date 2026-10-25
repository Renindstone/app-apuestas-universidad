package pe.edu.unu.apuestas.ingesta.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import pe.edu.unu.apuestas.ingesta.dto.PartidoDTO;
import pe.edu.unu.apuestas.ingesta.dto.PartidoUpsertRequest;

/**
 * Feign hacia ms-catalogo. name es el nombre en Eureka: el balanceador busca ahí la dirección del servicio.
 */
@FeignClient(name = "ms-catalogo")
public interface CatalogoClient {

    // Crea o actualiza el partido y devuelve el PartidoDTO con todos los IDs.
    @PostMapping("/interno/partidos")
    PartidoDTO upsert(@RequestBody PartidoUpsertRequest fila);
}
