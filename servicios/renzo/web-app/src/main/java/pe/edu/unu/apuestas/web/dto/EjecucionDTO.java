package pe.edu.unu.apuestas.web.dto;

import java.time.Instant;

/** Una ejecución de la ingesta. estado: EN_CURSO, OK o ERROR. */
public record EjecucionDTO(Long id, String tipo, String fuente, Instant inicio, Instant fin, String estado, Integer filas, String error) {
}
