package pe.edu.unu.apuestas.ingesta.dto;

import java.time.Instant;

/** Cuerpo de POST ms-estadisticas /interno/estadisticas. arbitroId en null si no se conoce. */
public record EstadisticasRequest(Long partidoId, Long ligaId, String temporada, Instant fecha, Long arbitroId, Lado local,
        Lado visitante) {

    /** Lo que hizo un equipo en el partido. */
    public record Lado(Long equipoId, int goles, int tiros, int tirosPuerta, int corners, int faltas, int amarillas, int rojas) {
    }
}
