package pe.edu.unu.apuestas.web.vista;

/**
 * Una cuota de una casa en una tabla. conValor indica que paga más que la cuota justa (EV > 0), y se pinta de verde (RN-WEB-05).
 */
public record CeldaCuota(Double valor, boolean conValor, Double valorEsperado) {

    public static final CeldaCuota VACIA = new CeldaCuota(null, false, null);

    // Arma la celda comparando la cuota de la casa con la probabilidad del modelo (si se conoce).
    public static CeldaCuota de(Double valor, Double probabilidad) {
        if (valor == null) {
            return VACIA;
        }
        if (probabilidad == null) {
            return new CeldaCuota(valor, false, null);
        }
        double ev = probabilidad * valor - 1;
        return new CeldaCuota(valor, ev > 0, ev);
    }
}
