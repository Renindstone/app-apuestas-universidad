package pe.edu.unu.apuestas.web.vista;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/**
 * Pruebas de los formatos que ve el usuario. La más importante: las horas salen en hora de Lima (RN-WEB-03).
 */
class FormatosTest {

    private final Formatos fmt = new Formatos();

    // 14:00 UTC son las 09:00 en Lima (UTC−5, sin horario de verano).
    @Test
    void muestraLaHoraEnLima() {
        assertEquals("09:00", fmt.hora(Instant.parse("2026-10-24T14:00:00Z")));
        assertEquals("24/10 08:00", fmt.fechaHora(Instant.parse("2026-10-24T13:00:00Z")));
    }

    // Un partido a las 02:00 UTC del 25 todavía es del día 24 en Lima.
    @Test
    void laFechaCortaUsaElDiaDeLima() {
        assertEquals("24 oct", fmt.fechaCorta(Instant.parse("2026-10-25T02:00:00Z")));
        assertEquals("sábado 24 de octubre", fmt.fechaLarga(LocalDate.of(2026, 10, 24)));
    }

    @Test
    void formateaPorcentajesCuotasYSoles() {
        assertEquals("52.8 %", fmt.pct(0.528));
        assertEquals("2.00", fmt.cuota(2.0));
        assertEquals("—", fmt.cuota(null));
        assertEquals("S/ 500.00", fmt.soles(new BigDecimal("500")));
        assertEquals("−S/ 4.00", fmt.soles(new BigDecimal("-4")));
        assertEquals("+5.6 %", fmt.signoPct(0.056));
        assertEquals("S/ 7.00", fmt.stakeSoles(0.014, new BigDecimal("500.00")));
    }

    // Etiquetas de las selecciones con los nombres de los equipos.
    @Test
    void escribeLasSeleccionesParaPersonas() {
        assertEquals("Gana Arsenal", fmt.etiqueta(Mercados.desdeClave("1X2|LOCAL|"), "Arsenal vs Chelsea"));
        assertEquals("Más de 2.5 goles", fmt.etiqueta(Mercados.desdeClave("GOLES|MAS|2.5"), "Arsenal", "Chelsea"));
        assertEquals("Menos de 10.5 córners", fmt.etiqueta(Mercados.desdeClave("CORNERS|MENOS|10.5"), "A", "B"));
        assertEquals("No marcan los dos", fmt.etiqueta(Mercados.desdeClave("AMBOS_MARCAN|NO|"), "A", "B"));
    }

    // RN-09: 3 de 1X2, 6 de goles, 2 de ambos marcan, 6 de tarjetas y 6 de córners.
    @Test
    void elCatalogoTieneTodasLasSelecciones() {
        assertEquals(23, Mercados.todas().size());
    }
}
