package pe.edu.unu.apuestas.apuestas.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * GET /mis-apuestas/rendimiento. apuestas cuenta todas, incluidas las pendientes; montoApostado, beneficio, roi y
 * porMercado solo cuentan las liquidadas (RN-APU-07).
 */
public record RendimientoDTO(int apuestas, int pendientes, int ganadas, int perdidas, BigDecimal montoApostado, BigDecimal beneficio,
        double roi, List<RendimientoMercadoDTO> porMercado) {
}
