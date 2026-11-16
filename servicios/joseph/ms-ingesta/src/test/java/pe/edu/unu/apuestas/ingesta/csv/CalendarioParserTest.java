package pe.edu.unu.apuestas.ingesta.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;

import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;

/**
 * Pruebas del calendario de fixturedownload con un CSV escrito aquí mismo (sin internet).
 */
class CalendarioParserTest {

    private static final String CSV = """
            Match Number,Round Number,Date,Location,Home Team,Away Team,Result
            1,1,21/08/2026 19:00,Emirates Stadium,Arsenal,Coventry,3 - 0
            90,9,01/11/2026 16:30,Anfield,Liverpool,Arsenal,1 - 2
            97,10,07/11/2026 12:30,Elland Road,Leeds,Spurs,
            98,10,08/11/2026 16:30,Old Trafford,Man Utd,Aston Villa,
            200,20,30/12/2026 20:00,Anfield,Liverpool,Chelsea,
            """;

    private final CalendarioParser parser = new CalendarioParser();
    private final Instant ahora = Instant.parse("2026-11-03T15:00:00Z");

    // Solo quedan los partidos de hace 3 días a dentro de 10 días.
    @Test
    void soloLeeLosPartidosCercanos() throws Exception {
        List<CSVRecord> filas = parser.leer(CSV, ahora);

        assertEquals(3, filas.size());
    }

    // Partido por jugar: hora en UTC, nombres de football-data y sin goles.
    @Test
    void traduceLosNombresYDejaSinGolesLoQueNoSeJugo() throws Exception {
        PartidoCsv fila = parser.parsear(parser.leer(CSV, ahora).get(1), "E0");

        assertEquals("E0", fila.liga());
        assertEquals(Instant.parse("2026-11-07T12:30:00Z"), fila.fechaHora());
        assertEquals("Leeds", fila.local());
        assertEquals("Tottenham", fila.visitante());
        assertNull(fila.golesLocal());
        assertFalse(fila.jugado());
        assertFalse(fila.tieneEstadisticas());
        assertTrue(fila.cuotas().isEmpty());
    }

    // Partido jugado: trae el marcador, pero no estadísticas (esas llegan por football-data).
    @Test
    void leeElMarcador() throws Exception {
        PartidoCsv fila = parser.parsear(parser.leer(CSV, ahora).get(0), "E0");

        assertEquals(1, fila.golesLocal());
        assertEquals(2, fila.golesVisitante());
        assertTrue(fila.jugado());
        assertFalse(fila.tieneEstadisticas());
    }

    // Un equipo que no está en la tabla no se envía: se crearía un equipo repetido en ms-catalogo.
    @Test
    void rechazaUnEquipoDesconocido() throws Exception {
        String csv = "Match Number,Round Number,Date,Location,Home Team,Away Team,Result\n1,1,05/11/2026 19:00,X,Equipo Raro,Arsenal,\n";

        CSVRecord fila = parser.leer(csv, ahora).get(0);

        assertThrows(IllegalArgumentException.class, () -> parser.parsear(fila, "E0"));
    }
}
