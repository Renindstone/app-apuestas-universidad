package pe.edu.unu.apuestas.ingesta.dto;

import java.time.Instant;

/** Lo que responde ms-catalogo al upsert. Solo están los campos que usa la ingesta: los IDs y la fecha. */
public record PartidoDTO(Long id, Referencia liga, String temporada, Instant fechaHora, Referencia local, Referencia visitante,
        Referencia arbitro) {

    /** {"id": 11, "nombre": "Arsenal"}. */
    public record Referencia(Long id, String nombre) {
    }
}
