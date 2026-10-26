package pe.edu.unu.apuestas.apuestas.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import pe.edu.unu.apuestas.apuestas.dto.PartidoDTO;

/**
 * Feign hacia ms-catalogo.
 */
@FeignClient(name = "ms-catalogo")
public interface CatalogoClient {

    // Un partido. Si no existe, Feign lanza FeignException.NotFound.
    @GetMapping("/partidos/{id}")
    PartidoDTO partido(@PathVariable Long id);

    // Los partidos de un día de Lima en un estado. La fecha va como texto ISO (2026-10-24).
    @GetMapping("/partidos")
    List<PartidoDTO> partidos(@RequestParam String fecha, @RequestParam String estado);
}
