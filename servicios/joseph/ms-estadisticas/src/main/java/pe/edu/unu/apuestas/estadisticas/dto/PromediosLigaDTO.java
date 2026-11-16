package pe.edu.unu.apuestas.estadisticas.dto;

/** GET /ligas/{id}/promedios: media por equipo y partido, como local y como visitante. temporada en null = todas las cargadas. */
public record PromediosLigaDTO(Long ligaId, String temporada, long partidos, PromediosDTO local, PromediosDTO visitante) {
}
