package pe.edu.unu.apuestas.apuestas.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import pe.edu.unu.apuestas.apuestas.dto.CuotaMercadoDTO;

/**
 * Feign hacia ms-cuotas.
 */
@FeignClient(name = "ms-cuotas")
public interface CuotasClient {

    // La cuota más reciente de cada casa, mercado, selección y línea del partido.
    @GetMapping("/partidos/{id}/cuotas")
    List<CuotaMercadoDTO> cuotas(@PathVariable Long id);
}
