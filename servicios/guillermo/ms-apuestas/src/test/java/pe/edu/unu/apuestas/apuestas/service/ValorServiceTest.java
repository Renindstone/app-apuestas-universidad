package pe.edu.unu.apuestas.apuestas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import pe.edu.unu.apuestas.apuestas.service.ValorService.Valor;
import pe.edu.unu.apuestas.apuestas.service.ValorService.Veredicto;

/**
 * Pruebas del valor esperado, el stake y el veredicto (RN-03 a RN-05), sin Spring.
 */
class ValorServiceTest {

    // CB-05: p = 0.50 y cuota = 2.20 dan EV = +0.10 y un stake de 0.25 × 0.10 / 1.20 = 2.1 % (≤ 3 %).
    @Test
    void unaCuotaConValor() {
        Valor valor = ValorService.evaluar(0.50, 2.20);
        assertEquals(0.10, valor.valorEsperado(), 1e-9);
        assertEquals(0.021, valor.stakePct(), 1e-9);
        assertTrue(valor.stakePct() <= 0.03);
        assertEquals(Veredicto.CON_VALOR, valor.veredicto());
    }

    // CB-06: p = 0.40 y cuota = 2.20 dan EV = −0.12, stake 0 y SIN_VALOR.
    @Test
    void unaCuotaSinValor() {
        Valor valor = ValorService.evaluar(0.40, 2.20);
        assertEquals(-0.12, valor.valorEsperado(), 1e-9);
        assertEquals(0, valor.stakePct());
        assertEquals(Veredicto.SIN_VALOR, valor.veredicto());
    }

    // RN-04: un EV entre 0 y 3 % es MARGINAL.
    @Test
    void unaCuotaMarginal() {
        Valor valor = ValorService.evaluar(0.47, 2.16);
        assertEquals(0.015, valor.valorEsperado(), 1e-9);
        assertEquals(Veredicto.MARGINAL, valor.veredicto());
    }

    // RN-05: el stake nunca pasa del 3 % de la banca, aunque Kelly pida más.
    @Test
    void elStakeTieneTope() {
        assertEquals(0.03, ValorService.evaluar(0.70, 2.00).stakePct(), 1e-9);
    }
}
