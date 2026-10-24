package pe.edu.unu.apuestas.web.vista;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.stereotype.Component;

import pe.edu.unu.apuestas.web.dto.Seleccion;

/**
 * Formatos que usan las plantillas como ${@fmt.hora(...)}. Todas las horas salen en hora de Lima (RN-WEB-03).
 */
@Component("fmt")
public class Formatos {

    public static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final Locale ESPANOL = Locale.forLanguageTag("es-PE");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm").withZone(LIMA);
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(LIMA);
    private static final DateTimeFormatter FECHA_LARGA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ESPANOL);
    private static final DateTimeFormatter FECHA_CORTA = DateTimeFormatter.ofPattern("d MMM", ESPANOL);

    // ---------- Fechas y horas ----------

    // El día de hoy en Lima: un «día» siempre es un día de Lima (RN-12).
    public LocalDate hoy() {
        return LocalDate.now(LIMA);
    }

    // "09:00"
    public String hora(Instant instante) {
        return instante == null ? "—" : HORA.format(instante);
    }

    // "24/10 08:00"
    public String fechaHora(Instant instante) {
        return instante == null ? "—" : FECHA_HORA.format(instante);
    }

    // "sábado 24 de octubre"
    public String fechaLarga(LocalDate fecha) {
        return FECHA_LARGA.format(fecha);
    }

    // "24 oct"
    public String fechaCorta(LocalDate fecha) {
        return fecha == null ? "—" : FECHA_CORTA.format(fecha).replace(".", "");
    }

    public String fechaCorta(Instant instante) {
        return instante == null ? "—" : fechaCorta(LocalDate.ofInstant(instante, LIMA));
    }

    // ---------- Números ----------

    // 0.528 → "52.8 %"
    public String pct(Number valor) {
        return pct(valor, 1);
    }

    public String pct(Number valor, int decimales) {
        return valor == null ? "—" : numero(valor.doubleValue() * 100, decimales) + " %";
    }

    // Número con punto decimal y la cantidad de decimales pedida.
    public String numero(Number valor, int decimales) {
        return valor == null ? "—" : String.format(Locale.ROOT, "%." + decimales + "f", valor.doubleValue());
    }

    // Cuota con dos decimales, o una raya si no hay cuota.
    public String cuota(Number valor) {
        return numero(valor, 2);
    }

    // "S/ 500.00" o "−S/ 4.00"
    public String soles(Number valor) {
        if (valor == null) {
            return "—";
        }
        BigDecimal monto = new BigDecimal(valor.toString()).setScale(2, RoundingMode.HALF_UP);
        return (monto.signum() < 0 ? "−" : "") + "S/ " + monto.abs().toPlainString();
    }

    // Stake sugerido en soles: porcentaje de la banca.
    public String stakeSoles(double stakePct, BigDecimal banca) {
        return banca == null ? "—" : soles(banca.multiply(BigDecimal.valueOf(stakePct)));
    }

    // "+0.056" o "−1.00": el signo menos es el tipográfico (−), no el guion.
    public String conSigno(Number valor, int decimales) {
        if (valor == null) {
            return "—";
        }
        double x = valor.doubleValue();
        String texto = numero(Math.abs(x), decimales);
        return (x > 0 ? "+" : x < 0 ? "−" : "") + texto;
    }

    // 0.056 → "+5.6 %"
    public String signoPct(Number valor) {
        return valor == null ? "—" : conSigno(valor.doubleValue() * 100, 1) + " %";
    }

    // Clase CSS para pintar ganancias en verde y pérdidas en rojo.
    public String claseSigno(Number valor) {
        if (valor == null) {
            return "";
        }
        double x = valor.doubleValue();
        return x > 0 ? "positivo" : x < 0 ? "negativo" : "";
    }

    // ---------- Mercados ----------

    public String nombreMercado(String mercado) {
        return Mercados.NOMBRES.getOrDefault(mercado, mercado);
    }

    // Texto para personas: "Más de 2.5 goles", "Gana Arsenal", "Ambos marcan".
    public String etiqueta(Seleccion s, String local, String visitante) {
        return switch (s.mercado()) {
            case "1X2" -> switch (s.seleccion()) {
                case "LOCAL" -> "Gana " + local;
                case "VISITA" -> "Gana " + visitante;
                default -> "Empate";
            };
            case "AMBOS_MARCAN" -> "SI".equals(s.seleccion()) ? "Ambos marcan" : "No marcan los dos";
            default -> ("MAS".equals(s.seleccion()) ? "Más" : "Menos") + " de " + Mercados.formatoLinea(s.linea()) + " "
                    + switch (s.mercado()) {
                        case "GOLES" -> "goles";
                        case "TARJETAS" -> "tarjetas";
                        default -> "córners";
                    };
        };
    }

    // Igual que la anterior, a partir de la descripción "Arsenal vs Chelsea".
    public String etiqueta(Seleccion s, String partido) {
        String[] equipos = partido == null ? new String[0] : partido.split(" vs ", 2);
        return equipos.length == 2 ? etiqueta(s, equipos[0], equipos[1]) : etiqueta(s, "el local", "la visita");
    }

    // Sin nombres de equipos (para formularios donde el partido se elige en otro campo).
    public String etiquetaGenerica(Seleccion s) {
        return etiqueta(s, "el local", "la visita");
    }

    // CON_VALOR → "Con valor"
    public String veredicto(String veredicto) {
        return switch (veredicto) {
            case "CON_VALOR" -> "Con valor";
            case "MARGINAL" -> "Marginal";
            default -> "Sin valor";
        };
    }
}
