package pe.edu.unu.apuestas.web.dto;

import java.util.List;

/** GET /api/apuestas/picks/historial?dias=30 */
public record HistorialDTO(int picks, int ganados, int perdidos, double beneficioUnidades, double roi, List<PickHistorialDTO> ultimos) {
}
