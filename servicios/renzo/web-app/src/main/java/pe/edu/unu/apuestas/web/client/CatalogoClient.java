package pe.edu.unu.apuestas.web.client;

import java.time.LocalDate;
import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import pe.edu.unu.apuestas.web.dto.LigaDTO;
import pe.edu.unu.apuestas.web.dto.PartidoDTO;

/**
 * Llamadas a ms-catalogo a través del gateway (/api/catalogo).
 */
@Component
public class CatalogoClient {

    private final RestClient gateway;

    public CatalogoClient(RestClient gateway) {
        this.gateway = gateway;
    }

    // Las 3 ligas (E0, SP1 y D1).
    public List<LigaDTO> ligas() {
        return gateway.get().uri("/api/catalogo/ligas").retrieve().body(new ParameterizedTypeReference<List<LigaDTO>>() { });
    }

    // Partidos de un día de Lima, ordenados por hora.
    public List<PartidoDTO> partidosDelDia(LocalDate fecha) {
        return gateway.get().uri("/api/catalogo/partidos?fecha={fecha}", fecha)
                .retrieve().body(new ParameterizedTypeReference<List<PartidoDTO>>() { });
    }

    // Un partido por su ID. Si no existe, el gateway responde 404 (ApiException).
    public PartidoDTO partido(long id) {
        return gateway.get().uri("/api/catalogo/partidos/{id}", id).retrieve().body(PartidoDTO.class);
    }
}
