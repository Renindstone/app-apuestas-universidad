package pe.edu.unu.apuestas.estadisticas.repository;

import pe.edu.unu.apuestas.estadisticas.dto.PromediosDTO;

/**
 * Resultado de una consulta AVG de EstadisticaRepository: cuántas filas entraron y el promedio de cada estadística.
 * Si no entró ninguna fila, los promedios llegan en null.
 */
public record Agregado(Long filas, Double goles, Double tiros, Double tirosPuerta, Double corners, Double faltas,
        Double tarjetas) {

    // Los promedios con 2 decimales, o null si no hubo filas: no tener datos no es lo mismo que tener cero (RN-EST-04).
    public PromediosDTO promedios() {
        if (filas == 0) {
            return null;
        }
        return new PromediosDTO(redondear(goles), redondear(tiros), redondear(tirosPuerta), redondear(corners), redondear(faltas),
                redondear(tarjetas));
    }

    static Double redondear(double valor) {
        return Math.round(valor * 100) / 100.0;
    }
}
