package pe.edu.unu.apuestas.ingesta.csv;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import pe.edu.unu.apuestas.ingesta.dto.CuotaDTO;

/**
 * Una fila de football-data ya traducida: la hora en UTC, los goles y el árbitro en null si no vienen,
 * las estadísticas en null si falta alguna columna y las cuotas agrupadas por casa (bet365, pinnacle).
 */
public record PartidoCsv(String liga, Instant fechaHora, String local, String visitante, String arbitro, Integer golesLocal,
        Integer golesVisitante, EstadisticasCsv estadisticasLocal, EstadisticasCsv estadisticasVisitante,
        Map<String, List<CuotaDTO>> cuotas) {

    // Ya se jugó si trae los dos goles. Sin goles, ms-catalogo lo deja PROGRAMADO.
    public boolean jugado() {
        return golesLocal != null && golesVisitante != null;
    }

    // Solo se envían estadísticas si el partido se jugó y vienen las de los dos equipos (RN-ING-04).
    public boolean tieneEstadisticas() {
        return jugado() && estadisticasLocal != null && estadisticasVisitante != null;
    }
}
