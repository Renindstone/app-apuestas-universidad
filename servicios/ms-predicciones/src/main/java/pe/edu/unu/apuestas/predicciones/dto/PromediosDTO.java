package pe.edu.unu.apuestas.predicciones.dto;

/** Promedio por partido de cada estadística, como lo devuelve ms-estadisticas. tarjetas = amarillas + rojas. */
public record PromediosDTO(Double goles, Double tiros, Double tirosPuerta, Double corners, Double faltas, Double tarjetas) {
}
