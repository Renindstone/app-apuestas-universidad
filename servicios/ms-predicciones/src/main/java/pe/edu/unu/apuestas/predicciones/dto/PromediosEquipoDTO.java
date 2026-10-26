package pe.edu.unu.apuestas.predicciones.dto;

/** ms-estadisticas GET /equipos/{id}/promedios. Con 0 partidos, aFavor y enContra llegan en null. */
public record PromediosEquipoDTO(Long equipoId, String condicion, long partidos, PromediosDTO aFavor, PromediosDTO enContra) {
}
