package pe.edu.unu.apuestas.predicciones.dto;

/** ms-estadisticas GET /ligas/{id}/promedios. Con 0 partidos, local y visitante llegan en null. */
public record PromediosLigaDTO(Long ligaId, long partidos, PromediosDTO local, PromediosDTO visitante) {
}
