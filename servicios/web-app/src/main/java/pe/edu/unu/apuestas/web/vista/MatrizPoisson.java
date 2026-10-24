package pe.edu.unu.apuestas.web.vista;

/**
 * Matriz de marcadores P(i, j) = Pois(i; λL) × Pois(j; λV) que se dibuja en el análisis.
 * Solo sirve para mostrar de dónde sale el 1X2: las probabilidades oficiales las calcula ms-predicciones.
 */
public record MatrizPoisson(double[][] celdas, double maximo) {

    // Calcula la matriz de 0 a n goles por equipo a partir de los goles esperados.
    public static MatrizPoisson calcular(double lambdaLocal, double lambdaVisitante, int n) {
        double[] local = filaPoisson(lambdaLocal, n);
        double[] visitante = filaPoisson(lambdaVisitante, n);
        double[][] celdas = new double[n + 1][n + 1];
        double maximo = 0;
        for (int i = 0; i <= n; i++) {
            for (int j = 0; j <= n; j++) {
                celdas[i][j] = local[i] * visitante[j];
                maximo = Math.max(maximo, celdas[i][j]);
            }
        }
        return new MatrizPoisson(celdas, maximo);
    }

    // Pois(k; λ) para k = 0..n, de forma iterativa: p0 = e^−λ y p_k = p_{k−1} × λ / k.
    private static double[] filaPoisson(double lambda, int n) {
        double[] fila = new double[n + 1];
        fila[0] = Math.exp(-lambda);
        for (int k = 1; k <= n; k++) {
            fila[k] = fila[k - 1] * lambda / k;
        }
        return fila;
    }

    public int tamanio() {
        return celdas.length;
    }

    public double celda(int i, int j) {
        return celdas[i][j];
    }

    // De 0 a 1: qué tan oscura se pinta la casilla.
    public double intensidad(int i, int j) {
        return celdas[i][j] / maximo;
    }
}
