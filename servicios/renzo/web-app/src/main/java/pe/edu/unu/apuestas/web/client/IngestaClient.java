package pe.edu.unu.apuestas.web.client;

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import pe.edu.unu.apuestas.web.dto.EjecucionDTO;

/**
 * Llamadas a ms-ingesta a través del gateway (/api/ingesta). El gateway exige el rol ADMIN.
 */
@Component
public class IngestaClient {

    private final RestClient gateway;

    public IngestaClient(RestClient gateway) {
        this.gateway = gateway;
    }

    // Lanza la ingesta en segundo plano. tipo: HISTORICO o PROXIMOS. Responde 202 EN_CURSO.
    public void ejecutar(String tipo) {
        gateway.post().uri("/api/ingesta/ejecutar?tipo={tipo}", tipo).retrieve().toBodilessEntity();
    }

    // Las últimas ejecuciones, de la más reciente a la más antigua.
    public List<EjecucionDTO> ejecuciones(int limite) {
        return gateway.get().uri("/api/ingesta/ejecuciones?limite={limite}", limite)
                .retrieve().body(new ParameterizedTypeReference<List<EjecucionDTO>>() { });
    }
}
