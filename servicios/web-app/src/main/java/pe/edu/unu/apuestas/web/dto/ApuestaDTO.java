package pe.edu.unu.apuestas.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Una apuesta del usuario en GET /api/apuestas/mis-apuestas.
 * El contrato no muestra este JSON completo: los nombres siguen a la tabla apuesta_usuario (sección 6.6).
 * resultado y ganancia son null mientras la apuesta está pendiente.
 */
public record ApuestaDTO(Long id, Long partidoId, String partido, String mercado, String seleccion, Double linea, String casa,
        double cuota, BigDecimal monto, Instant fecha, String resultado, BigDecimal ganancia) implements Seleccion {

    public boolean pendiente() {
        return resultado == null;
    }
}
