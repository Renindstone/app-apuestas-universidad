package pe.edu.unu.apuestas.estadisticas.dto;

/** Promedio por partido de cada estadística, con 2 decimales. tarjetas = amarillas + rojas (RN-09). */
public record PromediosDTO(Double goles, Double tiros, Double tirosPuerta, Double corners, Double faltas, Double tarjetas) {
}
