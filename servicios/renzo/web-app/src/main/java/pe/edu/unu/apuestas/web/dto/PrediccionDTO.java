package pe.edu.unu.apuestas.web.dto;

import java.time.Instant;
import java.util.List;

/** GET /api/predicciones/partidos/{id} */
public record PrediccionDTO(Long partidoId, String partido, Instant fechaHora, EsperadosDTO esperados, double factorArbitro,
        MuestraDTO muestra, List<MercadoDTO> mercados, List<String> explicacion) {

    // Busca la probabilidad de una selección por su clave ("1X2|LOCAL|"). Devuelve null si no está.
    public MercadoDTO mercado(String clave) {
        return mercados.stream().filter(m -> m.clave().equals(clave)).findFirst().orElse(null);
    }
}
