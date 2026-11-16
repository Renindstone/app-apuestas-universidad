package pe.edu.unu.apuestas.estadisticas.dto;

import java.time.Instant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * POST /interno/estadisticas: un partido ya jugado, con los IDs que devolvió ms-catalogo. arbitroId en null si no se conoce.
 */
public record EstadisticasRequest(
        @NotNull(message = "Falta partidoId.") Long partidoId,
        @NotNull(message = "Falta ligaId.") Long ligaId,
        @NotBlank(message = "Falta temporada.") @Pattern(regexp = "\\d{4}", message = "La temporada tiene 4 dígitos, por ejemplo 2627.") String temporada,
        @NotNull(message = "Falta fecha.") Instant fecha,
        Long arbitroId,
        @NotNull(message = "Faltan los datos del local.") @Valid Lado local,
        @NotNull(message = "Faltan los datos del visitante.") @Valid Lado visitante) {

    /** Lo que hizo un equipo en el partido. Con el mismo formato responde GET /partidos/{id}/estadisticas. */
    public record Lado(
            @NotNull(message = "Falta equipoId.") Long equipoId,
            @NotNull(message = "Faltan los goles.") @PositiveOrZero(message = "Las estadísticas no pueden ser negativas.") Integer goles,
            @NotNull(message = "Faltan los tiros.") @PositiveOrZero(message = "Las estadísticas no pueden ser negativas.") Integer tiros,
            @NotNull(message = "Faltan los tiros a puerta.") @PositiveOrZero(message = "Las estadísticas no pueden ser negativas.") Integer tirosPuerta,
            @NotNull(message = "Faltan los córners.") @PositiveOrZero(message = "Las estadísticas no pueden ser negativas.") Integer corners,
            @NotNull(message = "Faltan las faltas.") @PositiveOrZero(message = "Las estadísticas no pueden ser negativas.") Integer faltas,
            @NotNull(message = "Faltan las amarillas.") @PositiveOrZero(message = "Las estadísticas no pueden ser negativas.") Integer amarillas,
            @NotNull(message = "Faltan las rojas.") @PositiveOrZero(message = "Las estadísticas no pueden ser negativas.") Integer rojas) {
    }
}
