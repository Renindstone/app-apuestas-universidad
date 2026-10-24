package pe.edu.unu.apuestas.web.dto;

import pe.edu.unu.apuestas.web.vista.Mercados;

/**
 * Cualquier cosa que apunte a un mercado: una probabilidad, una cuota, un pick o una apuesta.
 */
public interface Seleccion {

    String mercado();

    String seleccion();

    Double linea();

    // Clave única de la selección, por ejemplo "GOLES|MAS|2.5" o "1X2|LOCAL|".
    default String clave() {
        return Mercados.clave(mercado(), seleccion(), linea());
    }
}
