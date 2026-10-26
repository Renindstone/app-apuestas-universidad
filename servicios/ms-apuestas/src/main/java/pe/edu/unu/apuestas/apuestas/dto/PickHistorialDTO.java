package pe.edu.unu.apuestas.apuestas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import pe.edu.unu.apuestas.apuestas.entity.Pick;
import pe.edu.unu.apuestas.apuestas.entity.Resultado;

/** Una apuesta del día ya liquidada, dentro de GET /picks/historial. */
public record PickHistorialDTO(LocalDate fecha, String partido, String mercado, String seleccion, BigDecimal linea, BigDecimal cuota,
        Resultado resultado) {

    public static PickHistorialDTO de(Pick p) {
        return new PickHistorialDTO(p.getFecha(), p.getDescripcion(), p.getMercado(), p.getSeleccion(), p.getLinea(), p.getCuota(),
                p.getResultado());
    }
}
