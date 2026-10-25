package pe.edu.unu.apuestas.estadisticas.dto;

import pe.edu.unu.apuestas.estadisticas.entity.Condicion;

/** GET /equipos/{id}/promedios. Con 0 partidos, aFavor y enContra van en null (RN-EST-04). */
public record PromediosEquipoDTO(Long equipoId, Condicion condicion, long partidos, PromediosDTO aFavor, PromediosDTO enContra) {
}
