package pe.edu.unu.apuestas.apuestas.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.http.HttpStatus;

import pe.edu.unu.apuestas.apuestas.exception.ApiException;

/**
 * Mercados, selecciones y líneas válidos (RN-09, tabla del README raíz) y las 3 casas (RN-10).
 */
public final class Mercados {

    public static final Set<String> CASAS = Set.of("bet365", "pinnacle", "betano");

    // Las selecciones de un mercado, en el orden en que se muestran, y sus líneas (vacío si el mercado no lleva línea).
    private record Definicion(List<String> selecciones, List<BigDecimal> lineas) {
    }

    private static final Map<String, Definicion> MERCADOS = Map.of(
            "1X2", new Definicion(List.of("LOCAL", "EMPATE", "VISITA"), List.of()),
            "GOLES", new Definicion(List.of("MAS", "MENOS"), lineas("1.5", "2.5", "3.5")),
            "AMBOS_MARCAN", new Definicion(List.of("SI", "NO"), List.of()),
            "TARJETAS", new Definicion(List.of("MAS", "MENOS"), lineas("3.5", "4.5", "5.5")),
            "CORNERS", new Definicion(List.of("MAS", "MENOS"), lineas("8.5", "9.5", "10.5")));

    private Mercados() {
    }

    // Las selecciones del mercado. Lanza 400 si el mercado no existe o si la línea no es una de las suyas.
    public static List<String> selecciones(String mercado, BigDecimal linea) {
        Definicion definicion = MERCADOS.get(mercado);
        if (definicion == null) {
            throw invalido("El mercado " + mercado + " no existe. Son 1X2, GOLES, AMBOS_MARCAN, TARJETAS y CORNERS.");
        }
        if (definicion.lineas().isEmpty()) {
            if (linea != null) {
                throw invalido("El mercado " + mercado + " no lleva línea.");
            }
        } else if (linea == null || definicion.lineas().stream().noneMatch(l -> l.compareTo(linea) == 0)) {
            throw invalido("La línea de " + mercado + " debe ser " + texto(definicion.lineas()) + ".");
        }
        return definicion.selecciones();
    }

    // Lanza 400 si el mercado, la línea o la selección no son válidos (RN-09).
    public static void validar(String mercado, String seleccion, BigDecimal linea) {
        List<String> selecciones = selecciones(mercado, linea);
        if (!selecciones.contains(seleccion)) {
            throw invalido("La selección " + seleccion + " no existe en " + mercado + ". Son " + texto(selecciones) + ".");
        }
    }

    // Lanza 400 si la casa no es bet365, pinnacle ni betano.
    public static void validarCasa(String casa) {
        if (!CASAS.contains(casa)) {
            throw invalido("La casa " + casa + " no existe. Son bet365, pinnacle y betano, en minúsculas.");
        }
    }

    private static List<BigDecimal> lineas(String... valores) {
        return Stream.of(valores).map(BigDecimal::new).toList();
    }

    // [1.5, 2.5, 3.5] → "1.5, 2.5 o 3.5"
    private static String texto(List<?> valores) {
        String todos = valores.stream().map(String::valueOf).collect(Collectors.joining(", "));
        int ultima = todos.lastIndexOf(", ");
        return ultima < 0 ? todos : todos.substring(0, ultima) + " o " + todos.substring(ultima + 2);
    }

    private static ApiException invalido(String detalle) {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALIDACION", detalle);
    }
}
