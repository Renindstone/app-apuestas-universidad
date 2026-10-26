package pe.edu.unu.apuestas.apuestas.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * POST /evaluaciones: el usuario solo manda la cuota; la probabilidad la pone ms-predicciones (RN-APU-01).
 * linea en null si el mercado no lleva.
 */
public record EvaluacionRequest(
        @NotNull(message = "Falta partidoId.") Long partidoId,
        @NotBlank(message = "Falta el mercado.") String mercado,
        @NotBlank(message = "Falta la selección.") String seleccion,
        BigDecimal linea,
        @NotBlank(message = "Falta la casa.") String casa,
        @NotNull(message = "Falta la cuota.") BigDecimal cuota) {
}
