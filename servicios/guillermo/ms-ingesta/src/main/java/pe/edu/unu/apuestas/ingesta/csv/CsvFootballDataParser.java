package pe.edu.unu.apuestas.ingesta.csv;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import pe.edu.unu.apuestas.ingesta.dto.CuotaDTO;

/**
 * Convierte los CSV de football-data en PartidoCsv. Es el único lugar que conoce los nombres de las columnas
 * y el formato de las fechas. Las columnas se buscan por nombre, porque los encabezados cambian entre archivos.
 */
@Component
public class CsvFootballDataParser {

    // Las ligas del proyecto (RN-CAT-01). fixtures.csv y new_league_fixtures.csv traen muchas más.
    public static final Set<String> LIGAS = Set.of("E0", "SP1", "D1", "BRA");
    // Código propio de la Serie A de Brasil. Sus archivos (new/BRA.csv y new_league_fixtures.csv) no traen la columna Div,
    // sino Country y League, y llaman Home, Away, HG y AG a lo que en las ligas europeas es HomeTeam, AwayTeam, FTHG y FTAG.
    public static final String BRASIL = "BRA";

    private static final ZoneId LONDRES = ZoneId.of("Europe/London");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    // Algunas temporadas antiguas traen el año con 2 dígitos: 24/10/26.
    private static final DateTimeFormatter FECHA_CORTA = DateTimeFormatter.ofPattern("dd/MM/yy");
    private static final BigDecimal DOS_Y_MEDIO = new BigDecimal("2.5");

    // Columna del CSV → casa, mercado, selección y línea de ms-cuotas (tabla del README).
    private static final List<ColumnaCuota> CUOTAS = List.of(
            new ColumnaCuota("B365H", "bet365", "1X2", "LOCAL", null),
            new ColumnaCuota("B365D", "bet365", "1X2", "EMPATE", null),
            new ColumnaCuota("B365A", "bet365", "1X2", "VISITA", null),
            new ColumnaCuota("B365>2.5", "bet365", "GOLES", "MAS", DOS_Y_MEDIO),
            new ColumnaCuota("B365<2.5", "bet365", "GOLES", "MENOS", DOS_Y_MEDIO),
            new ColumnaCuota("PSH", "pinnacle", "1X2", "LOCAL", null),
            new ColumnaCuota("PSD", "pinnacle", "1X2", "EMPATE", null),
            new ColumnaCuota("PSA", "pinnacle", "1X2", "VISITA", null),
            new ColumnaCuota("P>2.5", "pinnacle", "GOLES", "MAS", DOS_Y_MEDIO),
            new ColumnaCuota("P<2.5", "pinnacle", "GOLES", "MENOS", DOS_Y_MEDIO));

    private record ColumnaCuota(String columna, String casa, String mercado, String seleccion, BigDecimal linea) {
    }

    // Lee el CSV completo y devuelve solo las filas de las ligas del proyecto. Sin quitar el BOM, la primera columna se llamaría "﻿Div".
    public List<CSVRecord> leer(String texto) throws IOException {
        return leer(texto, null);
    }

    /**
     * Igual que leer(texto), pero si el archivo trae la columna Season (new/BRA.csv tiene todas las temporadas juntas)
     * solo devuelve las filas de esa temporada. Con temporada en null no filtra.
     */
    public List<CSVRecord> leer(String texto, String temporada) throws IOException {
        String limpio = texto.startsWith("﻿") ? texto.substring(1) : texto;
        CSVFormat formato = CSVFormat.DEFAULT.builder()
                .setHeader().setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true).setTrim(true).setAllowMissingColumnNames(true)
                .get();
        try (CSVParser csv = formato.parse(new StringReader(limpio))) {
            return csv.stream().filter(fila -> {
                String liga = liga(fila);
                String season = valor(fila, "Season");
                return liga != null && LIGAS.contains(liga) && (temporada == null || season == null || season.equals(temporada));
            }).toList();
        }
    }

    /**
     * Convierte una fila en un PartidoCsv. La hora del CSV es la de Londres y se pasa a UTC, así el cambio de horario
     * británico se aplica solo (RN-ING-03). Lanza IllegalArgumentException si falta la liga, la fecha, la hora o un equipo.
     */
    public PartidoCsv parsear(CSVRecord fila) {
        String dia = obligatorio(fila, "Date");
        LocalDate fecha = LocalDate.parse(dia, dia.length() == 8 ? FECHA_CORTA : FECHA);
        LocalTime hora = LocalTime.parse(obligatorio(fila, "Time"));
        Instant fechaHora = LocalDateTime.of(fecha, hora).atZone(LONDRES).toInstant();
        String liga = liga(fila);
        if (liga == null) {
            throw new IllegalArgumentException("Falta la columna Div.");
        }
        // Las ligas europeas toman la temporada del archivo. Brasil juega en año calendario: la de la columna Season
        // o, en los próximos partidos (que no la traen), el año del partido.
        String temporada = BRASIL.equals(liga) ? primero(valor(fila, "Season"), String.valueOf(fecha.getYear())) : null;
        return new PartidoCsv(liga, fechaHora, obligatorio(fila, "HomeTeam", "Home"), obligatorio(fila, "AwayTeam", "Away"),
                valor(fila, "Referee"), primero(entero(fila, "FTHG"), entero(fila, "HG")), primero(entero(fila, "FTAG"), entero(fila, "AG")),
                estadisticas(fila, "H"), estadisticas(fila, "A"), cuotas(fila), temporada);
    }

    /**
     * El código de liga de una fila: la columna Div en los archivos europeos y BRA en los de Brasil
     * (Country = Brazil y League = Serie A). null si la fila es de otra liga sin código.
     */
    static String liga(CSVRecord fila) {
        String div = valor(fila, "Div");
        if (div != null) {
            return div;
        }
        if ("Brazil".equals(valor(fila, "Country")) && "Serie A".equals(valor(fila, "League"))) {
            return BRASIL;
        }
        return null;
    }

    private static <T> T primero(T valor, T otro) {
        return valor != null ? valor : otro;
    }

    // Estadísticas de un lado ("H" local, "A" visitante): HS, HST, HC, HF, HY y HR. null si falta alguna.
    private static EstadisticasCsv estadisticas(CSVRecord fila, String lado) {
        Integer tiros = entero(fila, lado + "S");
        Integer tirosPuerta = entero(fila, lado + "ST");
        Integer corners = entero(fila, lado + "C");
        Integer faltas = entero(fila, lado + "F");
        Integer amarillas = entero(fila, lado + "Y");
        Integer rojas = entero(fila, lado + "R");
        if (tiros == null || tirosPuerta == null || corners == null || faltas == null || amarillas == null || rojas == null) {
            return null;
        }
        return new EstadisticasCsv(tiros, tirosPuerta, corners, faltas, amarillas, rojas);
    }

    // Cuotas agrupadas por casa. Se saltan las columnas que el archivo no trae y los valores que no son mayores que 1 (RN-CUO-01).
    private static Map<String, List<CuotaDTO>> cuotas(CSVRecord fila) {
        Map<String, List<CuotaDTO>> porCasa = new LinkedHashMap<>();
        for (ColumnaCuota c : CUOTAS) {
            String texto = valor(fila, c.columna());
            if (texto == null) {
                continue;
            }
            BigDecimal valor = new BigDecimal(texto);
            if (valor.compareTo(BigDecimal.ONE) > 0) {
                porCasa.computeIfAbsent(c.casa(), casa -> new ArrayList<>()).add(new CuotaDTO(c.mercado(), c.seleccion(), c.linea(), valor));
            }
        }
        return porCasa;
    }

    // El valor de la columna, o null si el archivo no la trae o viene vacía.
    private static String valor(CSVRecord fila, String columna) {
        if (!fila.isSet(columna)) {
            return null;
        }
        String texto = fila.get(columna);
        return texto.isEmpty() ? null : texto;
    }

    private static String obligatorio(CSVRecord fila, String columna) {
        String texto = valor(fila, columna);
        if (texto == null) {
            throw new IllegalArgumentException("Falta la columna " + columna + ".");
        }
        return texto;
    }

    // La primera de las dos columnas que venga con valor (HomeTeam en las ligas europeas, Home en Brasil).
    private static String obligatorio(CSVRecord fila, String columna, String alternativa) {
        String texto = valor(fila, columna);
        return texto != null ? texto : obligatorio(fila, alternativa);
    }

    private static Integer entero(CSVRecord fila, String columna) {
        String texto = valor(fila, columna);
        return texto == null ? null : Integer.valueOf(texto);
    }
}
