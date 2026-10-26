package pe.edu.unu.apuestas.web.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import pe.edu.unu.apuestas.web.dto.PrediccionDTO;

/**
 * Llamadas a ms-predicciones a través del gateway (/api/predicciones).
 */
@Component
public class PrediccionesClient {

    private final RestClient gateway;

    public PrediccionesClient(RestClient gateway) {
        this.gateway = gateway;
    }

    // Probabilidades del modelo de Poisson para todos los mercados del partido.
    public PrediccionDTO prediccion(long partidoId) {
        return gateway.get().uri("/api/predicciones/partidos/{id}", partidoId).retrieve().body(PrediccionDTO.class);
    }
}
