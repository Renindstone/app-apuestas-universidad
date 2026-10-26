package pe.edu.unu.apuestas.predicciones.service;

/**
 * Las cuentas del modelo de Poisson (RN-PRE-04). Son funciones puras, sin Spring ni llamadas a otros servicios.
 */
public final class PoissonService {

    // La matriz de marcadores va de 0 a 10 goles por equipo.
    public static final int MAX_GOLES = 10;

    private PoissonService() {
    }

    // P(X = k) con X ~ Poisson(λ) = e^−λ λ^k / k!. Se calcula de forma iterativa para no desbordar con k!.
    public static double pmf(int k, double lambda) {
        double p = Math.exp(-lambda);
        for (int i = 1; i <= k; i++) {
            p = p * lambda / i;
        }
        return p;
    }

    // P(i, j) = Pois(i; λL) × Pois(j; λV): probabilidad de que el partido termine i a j.
    public static double[][] matriz(double lambdaLocal, double lambdaVisitante) {
        double[][] m = new double[MAX_GOLES + 1][MAX_GOLES + 1];
        for (int i = 0; i <= MAX_GOLES; i++) {
            for (int j = 0; j <= MAX_GOLES; j++) {
                m[i][j] = pmf(i, lambdaLocal) * pmf(j, lambdaVisitante);
            }
        }
        return m;
    }

    // 1X2: {LOCAL, EMPATE, VISITA} = suma de P(i, j) con i > j, i = j o i < j.
    public static double[] unoXDos(double[][] m) {
        double[] r = new double[3];
        for (int i = 0; i < m.length; i++) {
            for (int j = 0; j < m[i].length; j++) {
                r[i > j ? 0 : i == j ? 1 : 2] += m[i][j];
            }
        }
        return r;
    }

    // GOLES MAS línea: suma de P(i, j) con i + j > línea.
    public static double masDe(double[][] m, double linea) {
        double p = 0;
        for (int i = 0; i < m.length; i++) {
            for (int j = 0; j < m[i].length; j++) {
                if (i + j > linea) {
                    p += m[i][j];
                }
            }
        }
        return p;
    }

    // AMBOS_MARCAN SI: que cada equipo haga al menos un gol = (1 − e^−λL) × (1 − e^−λV).
    public static double ambosMarcan(double lambdaLocal, double lambdaVisitante) {
        return (1 - Math.exp(-lambdaLocal)) * (1 - Math.exp(-lambdaVisitante));
    }

    // Córners y tarjetas, sobre el total del partido: P(MAS línea) = 1 − Σ Pois(k; λ) para k ≤ línea.
    public static double masDeTotal(double lambda, double linea) {
        double hastaLinea = 0;
        for (int k = 0; k <= linea; k++) {
            hastaLinea += pmf(k, lambda);
        }
        return 1 - hastaLinea;
    }
}
