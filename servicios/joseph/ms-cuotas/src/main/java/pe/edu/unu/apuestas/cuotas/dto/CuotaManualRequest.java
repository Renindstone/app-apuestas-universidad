package pe.edu.unu.apuestas.cuotas.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * POST /cuotas/manual: una cuota de Betano cargada por el usuario. linea en null si el mercado no lleva.
 */
public record CuotaManualRequest(
        @NotNull(message = "Falta partidoId.") Long partidoId,
        @NotBlank(message = "Falta la casa.") String casa,
        @NotBlank(message = "Falta el mercado.") String mercado,
        @NotBlank(message = "Falta la selección.") String seleccion,
        BigDecimal linea,
        @NotNull(message = "Falta el valor de la cuota.")
        @Digits(integer = 4, fraction = 2, message = "La cuota lleva hasta 2 decimales, por ejemplo 1.85.") BigDecimal valor) {
}
