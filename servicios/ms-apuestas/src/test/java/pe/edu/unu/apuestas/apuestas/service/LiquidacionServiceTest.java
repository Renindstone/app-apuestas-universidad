package pe.edu.unu.apuestas.apuestas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import pe.edu.unu.apuestas.apuestas.entity.Resultado;
import pe.edu.unu.apuestas.apuestas.service.LiquidacionService.Final;

/**
 * Pruebas de la tabla de liquidación del contrato (RN-07 y RN-APU-06), sin Spring.
 */
class LiquidacionServiceTest {

    // Un 2-1 con 6 tarjetas y 10 córners.
    private static final Final DOS_A_UNO = new Final(2, 1, 6, 10);

    private static Resultado liquidar(String mercado, String seleccion, String linea, Final partido) {
        return LiquidacionService.liquidar(mercado, seleccion, linea == null ? null : new BigDecimal(linea), partido);
    }

    // CB-07: con 2-1, MAS 2.5 gana y deja monto × (cuota − 1).
    @Test
    void masDeDosYMedioConDosAUnoGana() {
        assertEquals(Resultado.GANADA, liquidar("GOLES", "MAS", "2.5", DOS_A_UNO));
        assertEquals(new BigDecimal("22.00"), LiquidacionService.ganancia(Resultado.GANADA, new BigDecimal("20.00"), new BigDecimal("2.10")));
    }

    // RN-07: una apuesta perdida resta el monto.
    @Test
    void unaApuestaPerdidaRestaElMonto() {
        assertEquals(Resultado.PERDIDA, liquidar("GOLES", "MENOS", "2.5", DOS_A_UNO));
        assertEquals(new BigDecimal("-20.00"), LiquidacionService.ganancia(Resultado.PERDIDA, new BigDecimal("20.00"), new BigDecimal("2.10")));
    }

    // La tabla del contrato, mercado por mercado.
    @Test
    void cadaMercadoSegunLaTabla() {
        assertEquals(Resultado.GANADA, liquidar("1X2", "LOCAL", null, DOS_A_UNO));
        assertEquals(Resultado.PERDIDA, liquidar("1X2", "EMPATE", null, DOS_A_UNO));
        assertEquals(Resultado.PERDIDA, liquidar("1X2", "VISITA", null, DOS_A_UNO));
        assertEquals(Resultado.GANADA, liquidar("1X2", "EMPATE", null, new Final(1, 1, 0, 0)));
        assertEquals(Resultado.GANADA, liquidar("AMBOS_MARCAN", "SI", null, DOS_A_UNO));
        assertEquals(Resultado.GANADA, liquidar("AMBOS_MARCAN", "NO", null, new Final(2, 0, 0, 0)));
        assertEquals(Resultado.GANADA, liquidar("TARJETAS", "MAS", "5.5", DOS_A_UNO));
        assertEquals(Resultado.PERDIDA, liquidar("CORNERS", "MENOS", "9.5", DOS_A_UNO));
        assertEquals(Resultado.GANADA, liquidar("CORNERS", "MENOS", "10.5", DOS_A_UNO));
    }

    // RN-APU-06: sin las estadísticas finales, tarjetas y córners siguen pendientes; los goles no las necesitan.
    @Test
    void sinEstadisticasTarjetasYCornersEsperan() {
        Final sinEstadisticas = new Final(2, 1, null, null);
        assertNull(liquidar("TARJETAS", "MAS", "4.5", sinEstadisticas));
        assertNull(liquidar("CORNERS", "MAS", "9.5", sinEstadisticas));
        assertEquals(Resultado.GANADA, liquidar("GOLES", "MAS", "1.5", sinEstadisticas));
    }
}
