package pe.edu.unu.apuestas.web.dto;

/** Formato común de error de todos los servicios. */
public record ErrorApiDTO(String error, String detalle) {
}
