package pe.edu.unu.apuestas.ingesta.dto;

import java.time.Instant;

/** Cuerpo de POST ms-catalogo /interno/partidos. Goles y árbitro en null si no se conocen. */
public record PartidoUpsertRequest(String ligaCodigo, String temporada, Instant fechaHora, String local, String visitante,
        String arbitro, Integer golesLocal, Integer golesVisitante) {
}
