package pe.edu.unu.apuestas.predicciones.dto;

/** Valores esperados del partido. corners, tarjetas y tiros son el total de los dos equipos; tarjetas ya incluye el factor árbitro. */
public record EsperadosDTO(double golesLocal, double golesVisitante, double corners, double tarjetas, double tiros) {
}
