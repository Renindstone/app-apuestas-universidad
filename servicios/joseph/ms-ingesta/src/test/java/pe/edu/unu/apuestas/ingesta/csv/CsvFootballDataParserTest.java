package pe.edu.unu.apuestas.ingesta.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import pe.edu.unu.apuestas.ingesta.dto.CuotaDTO;

/**
 * Pruebas del parser con los CSV de src/test/resources/csv (sin internet).
 */
class CsvFootballDataParserTest {

    private final CsvFootballDataParser parser = new CsvFootballDataParser();

    private List<CSVRecord> leer(String archivo) throws Exception {
        return parser.leer(new ClassPathResource("csv/" + archivo).getContentAsString(StandardCharsets.UTF_8));
    }

    // CB-01: fila completa. El 24/10/2026 Londres sigue en horario de verano (UTC+1): las 15:00 de allá son las 14:00 UTC.
    @Test
    void cb01FilaCompletaConLaHoraDeLondresEnUtc() throws Exception {
        PartidoCsv fila = parser.parsear(leer("historico.csv").get(0));

        assertEquals("E0", fila.liga());
        assertEquals(Instant.parse("2026-10-24T14:00:00Z"), fila.fechaHora());
        assertEquals("Arsenal", fila.local());
        assertEquals("Chelsea", fila.visitante());
        assertEquals("M Oliver", fila.arbitro());
        assertEquals(2, fila.golesLocal());
        assertEquals(1, fila.golesVisitante());
        assertEquals(new EstadisticasCsv(15, 6, 7, 10, 2, 0), fila.estadisticasLocal());
        assertEquals(new EstadisticasCsv(9, 3, 4, 13, 3, 0), fila.estadisticasVisitante());
        assertTrue(fila.tieneEstadisticas());
        assertEquals(List.of(
                new CuotaDTO("1X2", "LOCAL", null, new BigDecimal("2.10")),
                new CuotaDTO("1X2", "EMPATE", null, new BigDecimal("3.40")),
                new CuotaDTO("1X2", "VISITA", null, new BigDecimal("3.60")),
                new CuotaDTO("GOLES", "MAS", new BigDecimal("2.5"), new BigDecimal("1.90")),
                new CuotaDTO("GOLES", "MENOS", new BigDecimal("2.5"), new BigDecimal("1.90"))), fila.cuotas().get("bet365"));
        assertEquals(5, fila.cuotas().get("pinnacle").size());
    }

    // RN-ING-03: el 25/10/2026 termina el horario de verano británico y Londres vuelve a UTC+0.
    // Las columnas de Pinnacle vacías se saltan.
    @Test
    void elCambioDeHorarioBritanicoSeAplicaSolo() throws Exception {
        PartidoCsv fila = parser.parsear(leer("historico.csv").get(1));

        assertEquals(Instant.parse("2026-10-31T15:00:00Z"), fila.fechaHora());
        assertEquals(Set.of("bet365"), fila.cuotas().keySet());
    }

    // Año con 2 dígitos (24/10/26) y sin árbitro, como en la Bundesliga.
    @Test
    void aceptaElAnioConDosDigitosYElArbitroVacio() throws Exception {
        PartidoCsv fila = parser.parsear(leer("historico.csv").get(2));

        assertEquals(Instant.parse("2026-10-24T13:30:00Z"), fila.fechaHora());
        assertNull(fila.arbitro());
    }

    // CB-02: partido sin jugar. fixtures.csv no trae FTHG ni FTAG: goles nulos, sin estadísticas y ms-catalogo lo deja PROGRAMADO.
    // Además se quita el BOM (si no, la columna Div no existiría) y se descartan las ligas que no son E0, SP1 ni D1.
    @Test
    void cb02PartidoSinJugar() throws Exception {
        List<CSVRecord> filas = leer("fixtures.csv");
        assertEquals(2, filas.size());

        PartidoCsv fila = parser.parsear(filas.get(0));
        assertEquals("E0", fila.liga());
        assertEquals(Instant.parse("2026-10-25T14:00:00Z"), fila.fechaHora());
        assertNull(fila.golesLocal());
        assertNull(fila.golesVisitante());
        assertFalse(fila.jugado());
        assertNull(fila.estadisticasLocal());
        assertFalse(fila.tieneEstadisticas());
        assertEquals(5, fila.cuotas().get("bet365").size());
    }
}
