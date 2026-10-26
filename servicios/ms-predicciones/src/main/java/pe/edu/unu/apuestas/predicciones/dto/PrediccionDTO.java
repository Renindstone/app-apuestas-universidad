package pe.edu.unu.apuestas.predicciones.dto;

import java.time.Instant;
import java.util.List;

/** GET /partidos/{id}: lo que espera el modelo, la probabilidad de cada mercado y de dónde sale cada número (RN-PRE-06). */
public record PrediccionDTO(Long partidoId, String partido, Instant fechaHora, EsperadosDTO esperados, double factorArbitro,
        MuestraDTO muestra, List<MercadoDTO> mercados, List<String> explicacion) {
}
