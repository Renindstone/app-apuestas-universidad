package pe.edu.unu.apuestas.web.dto;

/** Un servicio y cuántas instancias suyas están registradas en Eureka (GET /estado del gateway). */
public record EstadoServicioDTO(String servicio, int instancias) {
}
