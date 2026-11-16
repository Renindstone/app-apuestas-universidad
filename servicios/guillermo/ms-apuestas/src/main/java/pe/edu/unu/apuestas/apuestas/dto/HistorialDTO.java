package pe.edu.unu.apuestas.apuestas.dto;

import java.util.List;

/** GET /picks/historial: las apuestas del día liquidadas, con stake plano de 1 unidad (RN-APU-08). */
public record HistorialDTO(int picks, int ganados, int perdidos, double beneficioUnidades, double roi, List<PickHistorialDTO> ultimos) {
}
