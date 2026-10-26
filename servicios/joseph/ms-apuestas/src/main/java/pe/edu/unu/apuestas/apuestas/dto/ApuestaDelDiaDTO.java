package pe.edu.unu.apuestas.apuestas.dto;

import java.time.LocalDate;

/** GET /apuesta-del-dia. Si no hay candidatas, pick es null y mensaje lo explica (RN-16). */
public record ApuestaDelDiaDTO(LocalDate fecha, PickDTO pick, int otrasCandidatas, String mensaje) {
}
