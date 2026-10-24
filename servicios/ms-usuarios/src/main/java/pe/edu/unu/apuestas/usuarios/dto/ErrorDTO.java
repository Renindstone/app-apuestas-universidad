package pe.edu.unu.apuestas.usuarios.dto;

/** Formato común de error de todos los servicios: {"error": "CODIGO_CORTO", "detalle": "mensaje para humanos"}. */
public record ErrorDTO(String error, String detalle) {
}
