package pe.edu.unu.apuestas.web.form;

import java.io.Serializable;
import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** «Si [casa] paga [cuota] por [selección]…»: formulario para evaluar una cuota en el análisis. */
public class EvaluacionForm implements Serializable {

    @NotBlank
    private String seleccion = "GOLES|MAS|2.5";

    @NotBlank
    private String casa = "bet365";

    @NotNull(message = "Escribe la cuota.")
    @DecimalMin(value = "1", inclusive = false, message = "La cuota debe ser mayor que 1.")
    private BigDecimal cuota = new BigDecimal("2.00");

    public String getSeleccion() {
        return seleccion;
    }

    public void setSeleccion(String seleccion) {
        this.seleccion = seleccion;
    }

    public String getCasa() {
        return casa;
    }

    public void setCasa(String casa) {
        this.casa = casa;
    }

    public BigDecimal getCuota() {
        return cuota;
    }

    public void setCuota(BigDecimal cuota) {
        this.cuota = cuota;
    }
}
