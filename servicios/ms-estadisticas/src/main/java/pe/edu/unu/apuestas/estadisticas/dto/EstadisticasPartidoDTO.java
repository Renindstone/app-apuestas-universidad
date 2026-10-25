package pe.edu.unu.apuestas.estadisticas.dto;

/** GET /partidos/{id}/estadisticas: lo que hizo cada equipo. Lo usa ms-apuestas para liquidar tarjetas y córners. */
public record EstadisticasPartidoDTO(Long partidoId, EstadisticasRequest.Lado local, EstadisticasRequest.Lado visitante) {
}
