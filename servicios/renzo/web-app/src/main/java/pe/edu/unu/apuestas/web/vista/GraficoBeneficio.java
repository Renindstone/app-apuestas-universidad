package pe.edu.unu.apuestas.web.vista;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import pe.edu.unu.apuestas.web.dto.PickHistorialDTO;

/**
 * Datos del gráfico SVG de beneficio acumulado del historial (stake de 1 unidad por pick).
 */
public record GraficoBeneficio(int ancho, int alto, String linea, String area, double yCero, List<Punto> puntos) {

    /** Un pick en el gráfico: posición, si ganó y el texto de ayuda. */
    public record Punto(double x, double y, boolean gana, String titulo) {
    }

    private static final int MARGEN = 12;

    // Arma el gráfico a partir de los picks, que llegan del más reciente al más antiguo. Sin picks devuelve null.
    public static GraficoBeneficio desde(List<PickHistorialDTO> recientePrimero, Formatos fmt, int ancho, int alto) {
        if (recientePrimero == null || recientePrimero.isEmpty()) {
            return null;
        }
        List<PickHistorialDTO> picks = new ArrayList<>(recientePrimero).reversed();
        double[] acumulado = new double[picks.size() + 1];
        for (int i = 0; i < picks.size(); i++) {
            acumulado[i + 1] = acumulado[i] + picks.get(i).unidades();
        }
        double max = 0, min = 0;
        for (double v : acumulado) {
            max = Math.max(max, v);
            min = Math.min(min, v);
        }
        double rango = max - min == 0 ? 1 : max - min;
        double paso = (ancho - 2.0 * MARGEN) / picks.size();

        StringBuilder linea = new StringBuilder();
        List<Punto> puntos = new ArrayList<>();
        for (int i = 0; i < acumulado.length; i++) {
            double x = MARGEN + i * paso;
            double y = MARGEN + (max - acumulado[i]) * (alto - 2.0 * MARGEN) / rango;
            linea.append(i == 0 ? "M" : " L").append(n(x)).append(',').append(n(y));
            if (i > 0) {
                PickHistorialDTO p = picks.get(i - 1);
                puntos.add(new Punto(x, y, p.ganado(), fmt.fechaCorta(p.fecha()) + ": " + fmt.conSigno(acumulado[i], 2) + " u"));
            }
        }
        double yCero = MARGEN + max * (alto - 2.0 * MARGEN) / rango;
        String area = linea + " L" + n(MARGEN + picks.size() * paso) + "," + n(yCero) + " L" + MARGEN + "," + n(yCero) + " Z";
        return new GraficoBeneficio(ancho, alto, linea.toString(), area, yCero, puntos);
    }

    private static String n(double valor) {
        return String.format(Locale.ROOT, "%.1f", valor);
    }
}
