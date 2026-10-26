package pe.edu.unu.apuestas.estadisticas.dto;

/** GET /arbitros/{id}/perfil. Las cifras suman a los dos equipos; con 0 partidos van en null. */
public record PerfilArbitroDTO(Long arbitroId, long partidos, Double tarjetasPorPartido, Double faltasPorPartido,
        Double mediaLigaTarjetas) {
}
