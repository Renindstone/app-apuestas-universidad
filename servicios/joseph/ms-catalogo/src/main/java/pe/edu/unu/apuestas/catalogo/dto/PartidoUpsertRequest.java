package pe.edu.unu.apuestas.catalogo.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * POST /interno/partidos: una fila de football-data ya convertida por ms-ingesta (la hora ya viene en UTC).
 * Los goles son null si el partido todavía no se jugó; el árbitro, si no se conoce.
 */
public record PartidoUpsertRequest(
        @NotBlank(message = "Falta ligaCodigo.") String ligaCodigo,
        @NotBlank(message = "Falta temporada.") @Pattern(regexp = "\\d{4}", message = "La temporada tiene 4 dígitos, por ejemplo 2627.") String temporada,
        @NotNull(message = "Falta fechaHora.") Instant fechaHora,
        @NotBlank(message = "Falta el equipo local.") @Size(max = 60, message = "El nombre del local tiene hasta 60 caracteres.") String local,
        @NotBlank(message = "Falta el equipo visitante.") @Size(max = 60, message = "El nombre del visitante tiene hasta 60 caracteres.") String visitante,
        @Size(max = 60, message = "El nombre del árbitro tiene hasta 60 caracteres.") String arbitro,
        @PositiveOrZero(message = "Los goles no pueden ser negativos.") Integer golesLocal,
        @PositiveOrZero(message = "Los goles no pueden ser negativos.") Integer golesVisitante) {
}
