package pe.edu.unu.apuestas.apuestas.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * POST /mis-apuestas: una apuesta que el usuario hizo por su cuenta. El usuario sale del token, no de este JSON (RN-14).
 * RN-APU-05: el monto es mayor que 0 y la cuota, mayor que 1.
 */
public record ApuestaRequest(
        @NotNull(message = "Falta partidoId.") Long partidoId,
        @NotBlank(message = "Falta el mercado.") String mercado,
        @NotBlank(message = "Falta la selección.") String seleccion,
        BigDecimal linea,
        @NotBlank(message = "Falta la casa.") String casa,
        @NotNull(message = "Falta la cuota.") @DecimalMin(value = "1", inclusive = false, message = "La cuota debe ser mayor que 1.")
        @Digits(integer = 4, fraction = 2, message = "La cuota lleva hasta 2 decimales, por ejemplo 2.10.") BigDecimal cuota,
        @NotNull(message = "Falta el monto.") @DecimalMin(value = "0", inclusive = false, message = "El monto debe ser mayor que 0.")
        @Digits(integer = 8, fraction = 2, message = "El monto lleva hasta 2 decimales, por ejemplo 20.00.") BigDecimal monto) {
}
