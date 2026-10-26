package pe.edu.unu.apuestas.web.form;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** «Betano paga [cuota] por [selección]»: cuota cargada a mano (RN-CUO-01: mayor que 1.01). */
public class CuotaBetanoForm {

    @NotBlank
    private String seleccion = "TARJETAS|MAS|4.5";

    @NotNull(message = "Escribe la cuota de Betano.")
    @DecimalMin(value = "1.01", inclusive = false, message = "La cuota de Betano debe ser mayor que 1.01.")
    private BigDecimal cuota;

    public String getSeleccion() {
        return seleccion;
    }

    public void setSeleccion(String seleccion) {
        this.seleccion = seleccion;
    }

    public BigDecimal getCuota() {
        return cuota;
    }

    public void setCuota(BigDecimal cuota) {
        this.cuota = cuota;
    }
}
