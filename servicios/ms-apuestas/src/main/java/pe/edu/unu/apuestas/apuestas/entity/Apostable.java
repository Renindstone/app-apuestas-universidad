package pe.edu.unu.apuestas.apuestas.entity;

import java.math.BigDecimal;

/**
 * Lo que tienen en común un pick y una apuesta del usuario: el partido y la selección. Con esto se liquidan igual.
 */
public interface Apostable {

    Long getPartidoId();

    String getMercado();

    String getSeleccion();

    BigDecimal getLinea();
}
