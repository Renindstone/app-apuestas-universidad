package pe.edu.unu.apuestas.cuotas.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import pe.edu.unu.apuestas.cuotas.entity.TipoCuota;

/**
 * POST /interno/cuotas: las cuotas de una casa para un partido, tal como las manda ms-ingesta.
 * Que el mercado, la línea y el valor sean válidos lo revisa CuotaService.
 */
public record CuotasRequest(
        @NotNull(message = "Falta partidoId.") Long partidoId,
        @NotBlank(message = "Falta la casa.") String casa,
        @NotNull(message = "Falta el tipo.") TipoCuota tipo,
        @NotEmpty(message = "Faltan las cuotas.") List<@Valid Item> cuotas) {

    /** Una cuota: {"mercado": "GOLES", "seleccion": "MAS", "linea": 2.5, "valor": 1.90}. linea en null si el mercado no lleva. */
    public record Item(
            @NotBlank(message = "Falta el mercado.") String mercado,
            @NotBlank(message = "Falta la selección.") String seleccion,
            BigDecimal linea,
            @NotNull(message = "Falta el valor de la cuota.") BigDecimal valor) {
    }
}
