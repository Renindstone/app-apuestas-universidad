package pe.edu.unu.apuestas.web.client;

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import pe.edu.unu.apuestas.web.dto.ApuestaDTO;
import pe.edu.unu.apuestas.web.dto.ApuestaDelDiaDTO;
import pe.edu.unu.apuestas.web.dto.ApuestaRequest;
import pe.edu.unu.apuestas.web.dto.EvaluacionDTO;
import pe.edu.unu.apuestas.web.dto.EvaluacionRequest;
import pe.edu.unu.apuestas.web.dto.HistorialDTO;
import pe.edu.unu.apuestas.web.dto.RendimientoDTO;

/**
 * Llamadas a ms-apuestas a través del gateway (/api/apuestas).
 */
@Component
public class ApuestasClient {

    private final RestClient gateway;

    public ApuestasClient(RestClient gateway) {
        this.gateway = gateway;
    }

    // La apuesta del día de hoy (Lima). Sin cuotas disponibles, ms-apuestas responde 503.
    public ApuestaDelDiaDTO apuestaDelDia() {
        return gateway.get().uri("/api/apuestas/apuesta-del-dia").retrieve().body(ApuestaDelDiaDTO.class);
    }

    // Compara una cuota con la probabilidad del modelo.
    public EvaluacionDTO evaluar(EvaluacionRequest datos) {
        return gateway.post().uri("/api/apuestas/evaluaciones").body(datos).retrieve().body(EvaluacionDTO.class);
    }

    // Apuestas del usuario del token, filtradas por estado (PENDIENTE o LIQUIDADA).
    public List<ApuestaDTO> misApuestas(String estado) {
        return gateway.get().uri("/api/apuestas/mis-apuestas?estado={estado}", estado)
                .retrieve().body(new ParameterizedTypeReference<List<ApuestaDTO>>() { });
    }

    // Registra una apuesta del usuario del token.
    public void registrar(ApuestaRequest datos) {
        gateway.post().uri("/api/apuestas/mis-apuestas").body(datos).retrieve().toBodilessEntity();
    }

    // Ganadas, perdidas, beneficio y ROI del usuario, en total y por mercado.
    public RendimientoDTO rendimiento() {
        return gateway.get().uri("/api/apuestas/mis-apuestas/rendimiento").retrieve().body(RendimientoDTO.class);
    }

    // Historial de la apuesta del día en los últimos días, con stake de 1 unidad.
    public HistorialDTO historial(int dias) {
        return gateway.get().uri("/api/apuestas/picks/historial?dias={dias}", dias).retrieve().body(HistorialDTO.class);
    }
}
