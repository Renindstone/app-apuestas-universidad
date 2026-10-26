package pe.edu.unu.apuestas.usuarios.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/** PUT /me/banca. La banca debe ser mayor o igual a 0 (RN-USU-06) y cabe en NUMERIC(12,2). */
public record BancaRequest(
        @NotNull(message = "Escribe el monto de la banca.")
        @DecimalMin(value = "0", message = "La banca debe ser mayor o igual a 0.")
        @Digits(integer = 10, fraction = 2, message = "La banca admite hasta 10 cifras enteras y 2 decimales.")
        BigDecimal banca) {
}
