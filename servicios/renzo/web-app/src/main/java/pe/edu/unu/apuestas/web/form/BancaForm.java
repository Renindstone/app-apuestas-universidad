package pe.edu.unu.apuestas.web.form;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/** «Mi banca es S/ [monto]» (RN-USU-06: mayor o igual a 0). */
public class BancaForm {

    @NotNull(message = "Escribe el monto de tu banca.")
    @DecimalMin(value = "0", message = "La banca debe ser mayor o igual a 0.")
    private BigDecimal banca;

    public BigDecimal getBanca() {
        return banca;
    }

    public void setBanca(BigDecimal banca) {
        this.banca = banca;
    }
}
