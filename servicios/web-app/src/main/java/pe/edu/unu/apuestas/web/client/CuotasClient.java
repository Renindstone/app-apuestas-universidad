package pe.edu.unu.apuestas.web.client;

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import pe.edu.unu.apuestas.web.dto.CuotaDTO;
import pe.edu.unu.apuestas.web.dto.CuotaManualRequest;

/**
 * Llamadas a ms-cuotas a través del gateway (/api/cuotas).
 */
@Component
public class CuotasClient {

    private final RestClient gateway;

    public CuotasClient(RestClient gateway) {
        this.gateway = gateway;
    }

    // La cuota más reciente de cada casa, mercado, selección y línea.
    public List<CuotaDTO> cuotas(long partidoId) {
        return gateway.get().uri("/api/cuotas/partidos/{id}/cuotas", partidoId)
                .retrieve().body(new ParameterizedTypeReference<List<CuotaDTO>>() { });
    }

    // Guarda una cuota de Betano cargada a mano. Partido inexistente: 404. Mercado o cuota no válidos: 400.
    public void guardarManual(CuotaManualRequest datos) {
        gateway.post().uri("/api/cuotas/cuotas/manual").body(datos).retrieve().toBodilessEntity();
    }
}
