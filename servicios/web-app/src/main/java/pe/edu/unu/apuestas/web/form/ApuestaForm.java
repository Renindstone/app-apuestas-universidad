package pe.edu.unu.apuestas.web.form;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** «Aposté S/ [monto] a [selección] en [partido], con cuota [cuota] en [casa]» (RN-APU-05). */
public class ApuestaForm {

    @NotNull(message = "Elige el partido.")
    private Long partidoId;

    @NotBlank
    private String seleccion = "GOLES|MAS|2.5";

    @NotBlank
    private String casa = "bet365";

    @NotNull(message = "Escribe la cuota.")
    @DecimalMin(value = "1", inclusive = false, message = "La cuota debe ser mayor que 1.")
    private BigDecimal cuota;

    @NotNull(message = "Escribe el monto.")
    @DecimalMin(value = "0", inclusive = false, message = "El monto debe ser mayor que 0.")
    private BigDecimal monto;

    public Long getPartidoId() {
        return partidoId;
    }

    public void setPartidoId(Long partidoId) {
        this.partidoId = partidoId;
    }

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

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }
}
