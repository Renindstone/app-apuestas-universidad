package pe.edu.unu.apuestas.estadisticas.repository;

/**
 * Sumas de tarjetas y faltas de los dos equipos en un conjunto de partidos (para el perfil del árbitro).
 */
public record Totales(Long partidos, Long tarjetas, Long faltas) {

    // Tarjetas por partido sumando ambos equipos (RN-EST-05), o null si no hay partidos.
    public Double tarjetasPorPartido() {
        return porPartido(tarjetas);
    }

    // Faltas por partido sumando ambos equipos, o null si no hay partidos.
    public Double faltasPorPartido() {
        return porPartido(faltas);
    }

    private Double porPartido(Long total) {
        return partidos == 0 ? null : Agregado.redondear(total / (double) partidos);
    }
}
