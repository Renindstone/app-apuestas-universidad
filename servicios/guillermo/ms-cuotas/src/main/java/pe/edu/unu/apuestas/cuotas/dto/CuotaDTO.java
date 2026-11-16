package pe.edu.unu.apuestas.cuotas.dto;

import java.math.BigDecimal;
import java.time.Instant;

import pe.edu.unu.apuestas.cuotas.entity.Cuota;
import pe.edu.unu.apuestas.cuotas.entity.TipoCuota;

/** Una cuota tal como la muestran GET /partidos/{id}/cuotas y POST /cuotas/manual. */
public record CuotaDTO(String casa, String mercado, String seleccion, BigDecimal linea, BigDecimal valor, TipoCuota tipo,
        Instant capturadaEn) {

    public static CuotaDTO de(Cuota c) {
        return new CuotaDTO(c.getCasa(), c.getMercado(), c.getSeleccion(), c.getLinea(), c.getValor(), c.getTipo(), c.getCapturadaEn());
    }
}
