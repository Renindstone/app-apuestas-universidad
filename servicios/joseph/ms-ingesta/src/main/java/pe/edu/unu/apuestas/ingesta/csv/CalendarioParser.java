package pe.edu.unu.apuestas.ingesta.csv;

import java.io.IOException;
import java.io.StringReader;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

/**
 * Lee el calendario de la temporada de fixturedownload.com (un CSV por liga, con la hora en UTC).
 * football-data solo publica los partidos del fin de semana unos días antes; este calendario trae la temporada
 * entera, así la web ya muestra los partidos de los próximos días aunque todavía no tengan cuotas.
 * Columnas: Match Number, Round Number, Date (dd/MM/yyyy HH:mm), Location, Home Team, Away Team, Result ("3 - 0").
 */
@Component
public class CalendarioParser {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    // Solo se envían los partidos de hace 3 días (por sus resultados) hasta dentro de 10 días.
    static final Duration ATRAS = Duration.ofDays(3);
    static final Duration ADELANTE = Duration.ofDays(10);

    /** El nombre del archivo de fixturedownload de cada liga: {@code /download/<nombre>-<año>-UTC.csv}. */
    public static final Map<String, String> ARCHIVOS = Map.of("E0", "epl", "SP1", "la-liga", "D1", "bundesliga");

    /**
     * Nombres de fixturedownload → nombres de football-data. ms-catalogo reconoce un partido por sus equipos y su
     * temporada, así que el nombre tiene que ser idéntico al que ya trajo football-data; si no, se crearía otro equipo.
     */
    static final Map<String, String> EQUIPOS = Map.ofEntries(
            // Premier League
            Map.entry("Arsenal", "Arsenal"), Map.entry("Aston Villa", "Aston Villa"), Map.entry("Bournemouth", "Bournemouth"),
            Map.entry("Brentford", "Brentford"), Map.entry("Brighton", "Brighton"), Map.entry("Burnley", "Burnley"),
            Map.entry("Chelsea", "Chelsea"), Map.entry("Coventry", "Coventry"), Map.entry("Crystal Palace", "Crystal Palace"),
            Map.entry("Everton", "Everton"), Map.entry("Fulham", "Fulham"), Map.entry("Hull", "Hull"), Map.entry("Ipswich", "Ipswich"),
            Map.entry("Leeds", "Leeds"), Map.entry("Leicester", "Leicester"), Map.entry("Liverpool", "Liverpool"),
            Map.entry("Man City", "Man City"), Map.entry("Man Utd", "Man United"), Map.entry("Newcastle", "Newcastle"),
            Map.entry("Nott'm Forest", "Nott'm Forest"), Map.entry("Southampton", "Southampton"), Map.entry("Spurs", "Tottenham"),
            Map.entry("Sunderland", "Sunderland"), Map.entry("West Ham", "West Ham"), Map.entry("Wolves", "Wolves"),
            // LaLiga
            Map.entry("Athletic Club", "Ath Bilbao"), Map.entry("Atlético de Madrid", "Ath Madrid"), Map.entry("CA Osasuna", "Osasuna"),
            Map.entry("Celta", "Celta"), Map.entry("Deportivo Alavés", "Alaves"), Map.entry("Elche CF", "Elche"),
            Map.entry("FC Barcelona", "Barcelona"), Map.entry("Getafe CF", "Getafe"), Map.entry("Girona FC", "Girona"),
            Map.entry("Levante UD", "Levante"), Map.entry("Málaga CF", "Malaga"), Map.entry("R. Racing Club", "Santander"),
            Map.entry("RC Deportivo", "La Coruna"), Map.entry("RCD Espanyol de Barcelona", "Espanol"),
            Map.entry("RCD Mallorca", "Mallorca"), Map.entry("Rayo Vallecano", "Vallecano"), Map.entry("Real Betis", "Betis"),
            Map.entry("Real Madrid", "Real Madrid"), Map.entry("Real Oviedo", "Oviedo"), Map.entry("Real Sociedad", "Sociedad"),
            Map.entry("Sevilla FC", "Sevilla"), Map.entry("Valencia CF", "Valencia"), Map.entry("Villarreal CF", "Villarreal"),
            // Bundesliga
            Map.entry("1. FC Heidenheim 1846", "Heidenheim"), Map.entry("1. FC Köln", "FC Koln"),
            Map.entry("1. FC Union Berlin", "Union Berlin"), Map.entry("1. FSV Mainz 05", "Mainz"),
            Map.entry("Bayer 04 Leverkusen", "Leverkusen"), Map.entry("Borussia Dortmund", "Dortmund"),
            Map.entry("Borussia Mönchengladbach", "M'gladbach"), Map.entry("Eintracht Frankfurt", "Ein Frankfurt"),
            Map.entry("FC Augsburg", "Augsburg"), Map.entry("FC Bayern München", "Bayern Munich"),
            Map.entry("FC St. Pauli", "St Pauli"), Map.entry("FC Schalke 04", "Schalke 04"), Map.entry("Hamburger SV", "Hamburg"),
            Map.entry("RB Leipzig", "RB Leipzig"), Map.entry("SC Paderborn 07", "Paderborn"), Map.entry("SV Elversberg", "Elversberg"),
            Map.entry("SV Werder Bremen", "Werder Bremen"), Map.entry("Sport-Club Freiburg", "Freiburg"),
            Map.entry("TSG Hoffenheim", "Hoffenheim"), Map.entry("VfB Stuttgart", "Stuttgart"),
            Map.entry("VfL Wolfsburg", "Wolfsburg"));

    // Las filas cuya fecha cae entre hace 3 días y dentro de 10 días. El resto de la temporada no hace falta.
    public List<CSVRecord> leer(String texto, Instant ahora) throws IOException {
        String limpio = texto.startsWith("﻿") ? texto.substring(1) : texto;
        CSVFormat formato = CSVFormat.DEFAULT.builder()
                .setHeader().setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true).setTrim(true).setAllowMissingColumnNames(true)
                .get();
        Instant desde = ahora.minus(ATRAS);
        Instant hasta = ahora.plus(ADELANTE);
        try (CSVParser csv = formato.parse(new StringReader(limpio))) {
            return csv.stream().filter(fila -> {
                Instant cuando = fechaHora(fila);
                return cuando != null && !cuando.isBefore(desde) && !cuando.isAfter(hasta);
            }).toList();
        }
    }

    /**
     * Convierte una fila en un PartidoCsv sin estadísticas ni cuotas (esas llegan después por football-data).
     * Lanza IllegalArgumentException si un equipo no está en la tabla EQUIPOS: así no se crea un equipo repetido.
     */
    public PartidoCsv parsear(CSVRecord fila, String liga) {
        Instant fechaHora = fechaHora(fila);
        if (fechaHora == null) {
            throw new IllegalArgumentException("Falta la fecha.");
        }
        String local = equipo(fila.get("Home Team"));
        String visitante = equipo(fila.get("Away Team"));
        Integer golesLocal = null;
        Integer golesVisitante = null;
        String resultado = fila.isSet("Result") ? fila.get("Result") : null;
        if (resultado != null && resultado.contains("-")) {
            String[] partes = resultado.split("-");
            golesLocal = Integer.valueOf(partes[0].trim());
            golesVisitante = Integer.valueOf(partes[1].trim());
        }
        return new PartidoCsv(liga, fechaHora, local, visitante, null, golesLocal, golesVisitante, null, null, Map.of(), null);
    }

    private static String equipo(String nombre) {
        String traducido = EQUIPOS.get(nombre == null ? "" : nombre.trim());
        if (traducido == null) {
            throw new IllegalArgumentException("El equipo " + nombre + " no está en la tabla de nombres de CalendarioParser.");
        }
        return traducido;
    }

    // La fecha de la fila (en UTC), o null si falta o está mal escrita.
    private static Instant fechaHora(CSVRecord fila) {
        if (!fila.isSet("Date")) {
            return null;
        }
        try {
            return LocalDateTime.parse(fila.get("Date").trim(), FECHA_HORA).toInstant(ZoneOffset.UTC);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
