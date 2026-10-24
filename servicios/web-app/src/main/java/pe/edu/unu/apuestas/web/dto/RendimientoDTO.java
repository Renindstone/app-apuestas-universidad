package pe.edu.unu.apuestas.web.dto;

import java.math.BigDecimal;
import java.util.List;

/** GET /api/apuestas/mis-apuestas/rendimiento. Solo cuenta las apuestas liquidadas (RN-APU-07). */
public record RendimientoDTO(int apuestas, int pendientes, int ganadas, int perdidas, BigDecimal montoApostado,
        BigDecimal beneficio, double roi, List<RendimientoMercadoDTO> porMercado) {
}
