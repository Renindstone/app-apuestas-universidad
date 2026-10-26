package pe.edu.unu.apuestas.predicciones.dto;

/** ms-estadisticas GET /arbitros/{id}/perfil. Con 0 partidos, las cifras llegan en null. */
public record PerfilArbitroDTO(Long arbitroId, long partidos, Double tarjetasPorPartido, Double faltasPorPartido,
        Double mediaLigaTarjetas) {
}
