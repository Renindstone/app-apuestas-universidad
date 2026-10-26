package pe.edu.unu.apuestas.web.dto;

import java.time.LocalDate;

/** GET /api/apuestas/apuesta-del-dia. Si no hay candidatas, pick es null y mensaje lo explica. */
public record ApuestaDelDiaDTO(LocalDate fecha, PickDTO pick, int otrasCandidatas, String mensaje) {
}
