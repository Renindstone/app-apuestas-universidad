package pe.edu.unu.apuestas.apuestas.dto;

import java.math.BigDecimal;
import java.time.Instant;

import pe.edu.unu.apuestas.apuestas.entity.ApuestaUsuario;
import pe.edu.unu.apuestas.apuestas.entity.Resultado;

/** Una apuesta del usuario. resultado y ganancia son null mientras está pendiente. */
public record ApuestaDTO(Long id, Long partidoId, String partido, String mercado, String seleccion, BigDecimal linea, String casa,
        BigDecimal cuota, BigDecimal monto, Instant fecha, Resultado resultado, BigDecimal ganancia) {

    public static ApuestaDTO de(ApuestaUsuario a) {
        return new ApuestaDTO(a.getId(), a.getPartidoId(), a.getDescripcion(), a.getMercado(), a.getSeleccion(), a.getLinea(),
                a.getCasa(), a.getCuota(), a.getMonto(), a.getFecha(), a.getResultado(), a.getGanancia());
    }
}
