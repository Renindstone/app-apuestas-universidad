package pe.edu.unu.apuestas.predicciones.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Pruebas de las cuentas de Poisson con λ conocidos, sin Spring.
 */
class PoissonServiceTest {

    // CB-03: con λL = 1.5 y λV = 1.1, las probabilidades de 1X2 suman 1 (± 0.001).
    @Test
    void elUnoXDosSumaUno() {
        double[] r = PoissonService.unoXDos(PoissonService.matriz(1.5, 1.1));
        assertEquals(1.0, r[0] + r[1] + r[2], 0.001);
        // Con más goles esperados, gana más veces el local.
        assertEquals(true, r[0] > r[2]);
    }

    // λ = 1: P(0) = P(1) = e^−1 = 0.3679 y P(2) = 0.1839.
    @Test
    void laPmfConLambdaUno() {
        assertEquals(0.3679, PoissonService.pmf(0, 1), 0.0001);
        assertEquals(0.3679, PoissonService.pmf(1, 1), 0.0001);
        assertEquals(0.1839, PoissonService.pmf(2, 1), 0.0001);
    }

    // La suma de dos Poisson es Poisson(λL + λV): más de 2.5 goles da lo mismo por la matriz que por el total.
    @Test
    void masDeUnaLineaPorLaMatrizYPorElTotal() {
        double porMatriz = PoissonService.masDe(PoissonService.matriz(1.5, 1.1), 2.5);
        double porTotal = PoissonService.masDeTotal(2.6, 2.5);
        assertEquals(porTotal, porMatriz, 0.0001);
        // 1 − (P0 + P1 + P2) con λ = 2.6.
        assertEquals(1 - Math.exp(-2.6) * (1 + 2.6 + 2.6 * 2.6 / 2), porTotal, 1e-9);
    }

    // Ambos marcan: (1 − e^−1.5) × (1 − e^−1.1).
    @Test
    void ambosMarcan() {
        assertEquals((1 - Math.exp(-1.5)) * (1 - Math.exp(-1.1)), PoissonService.ambosMarcan(1.5, 1.1), 1e-12);
    }
}
