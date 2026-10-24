package pe.edu.unu.apuestas.web.vista;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import pe.edu.unu.apuestas.web.dto.Seleccion;

/**
 * Mercados, selecciones y líneas válidos (tabla del README raíz, RN-09). Son los mismos códigos en todos los servicios.
 */
public final class Mercados {

    // Nombre para personas de cada mercado, en el orden en que se muestran.
    public static final Map<String, String> NOMBRES = new LinkedHashMap<>();

    static {
        NOMBRES.put("1X2", "Resultado");
        NOMBRES.put("GOLES", "Goles");
        NOMBRES.put("AMBOS_MARCAN", "Ambos marcan");
        NOMBRES.put("TARJETAS", "Tarjetas");
        NOMBRES.put("CORNERS", "Córners");
    }

    /** Una selección concreta, por ejemplo GOLES / MAS / 2.5. */
    public record Opcion(String mercado, String seleccion, Double linea) implements Seleccion {
    }

    private static final List<Opcion> TODAS = armarTodas();

    private Mercados() {
    }

    // Todas las selecciones de todos los mercados, con todas sus líneas.
    public static List<Opcion> todas() {
        return TODAS;
    }

    // Selecciones agrupadas por el nombre de su mercado, para los <optgroup> de los formularios.
    public static Map<String, List<Opcion>> porMercado() {
        Map<String, List<Opcion>> grupos = new LinkedHashMap<>();
        NOMBRES.forEach((mercado, nombre) -> grupos.put(nombre, delMercado(mercado)));
        return grupos;
    }

    // Solo las selecciones de un mercado.
    public static List<Opcion> delMercado(String mercado) {
        return TODAS.stream().filter(o -> o.mercado().equals(mercado)).toList();
    }

    // Clave de texto que identifica una selección: "GOLES|MAS|2.5". Sin línea queda "1X2|LOCAL|".
    public static String clave(String mercado, String seleccion, Double linea) {
        return mercado + "|" + seleccion + "|" + formatoLinea(linea);
    }

    // Convierte una clave en su selección. Si no es válida, lanza IllegalArgumentException.
    public static Opcion desdeClave(String clave) {
        return TODAS.stream().filter(o -> o.clave().equals(clave)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Selección no válida: " + clave));
    }

    // 2.5 → "2.5" y null → "".
    public static String formatoLinea(Double linea) {
        return linea == null ? "" : BigDecimal.valueOf(linea).stripTrailingZeros().toPlainString();
    }

    private static List<Opcion> armarTodas() {
        List<Opcion> lista = new ArrayList<>();
        for (String s : List.of("LOCAL", "EMPATE", "VISITA")) {
            lista.add(new Opcion("1X2", s, null));
        }
        agregarMasMenos(lista, "GOLES", 1.5, 2.5, 3.5);
        lista.add(new Opcion("AMBOS_MARCAN", "SI", null));
        lista.add(new Opcion("AMBOS_MARCAN", "NO", null));
        agregarMasMenos(lista, "TARJETAS", 3.5, 4.5, 5.5);
        agregarMasMenos(lista, "CORNERS", 8.5, 9.5, 10.5);
        return List.copyOf(lista);
    }

    private static void agregarMasMenos(List<Opcion> lista, String mercado, double... lineas) {
        for (double linea : lineas) {
            lista.add(new Opcion(mercado, "MAS", linea));
            lista.add(new Opcion(mercado, "MENOS", linea));
        }
    }
}
