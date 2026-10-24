package pe.edu.unu.apuestas.web.dto;

import java.time.LocalDate;

/** Un pick ya liquidado en el historial de la apuesta del día. */
public record PickHistorialDTO(LocalDate fecha, String partido, String mercado, String seleccion, Double linea, double cuota,
        String resultado) implements Seleccion {

    public boolean ganado() {
        return "GANADA".equals(resultado);
    }

    // Con stake plano de 1 unidad: gana cuota − 1 o pierde 1.
    public double unidades() {
        return ganado() ? cuota - 1 : -1;
    }
}
