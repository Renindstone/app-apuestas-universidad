package pe.edu.unu.apuestas.predicciones.dto;

import java.time.Instant;

/** Lo que responde ms-catalogo en GET /partidos/{id}. Solo están los campos que usa el modelo. arbitro en null si no se conoce. */
public record PartidoDTO(Long id, Referencia liga, Instant fechaHora, Referencia local, Referencia visitante, Referencia arbitro) {

    /** {"id": 11, "nombre": "Arsenal"}. */
    public record Referencia(Long id, String nombre) {
    }
}
