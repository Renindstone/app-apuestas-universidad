package pe.edu.unu.apuestas.cuotas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import pe.edu.unu.apuestas.cuotas.exception.CuotaInvalidaException;

/**
 * Pruebas del cálculo del margen, sin base de datos ni Spring.
 */
class CuotaServiceTest {

    // CB-04: una cuota que no es mayor que 1 no es válida.
    @Test
    void unaCuotaNoValidaLanzaCuotaInvalidaException() {
        assertThrows(CuotaInvalidaException.class, () -> CuotaService.quitarMargen(List.of(2.0, 0.9, 3.5)));
    }

    // RN-01 y RN-02 con el ejemplo del README: 2.10 / 3.40 / 3.60 dan un margen de 4.8 %.
    @Test
    void elMargenYLasProbabilidadesSinMargen() {
        CuotaService.SinMargen resultado = CuotaService.quitarMargen(List.of(2.10, 3.40, 3.60));

        assertEquals(0.048, resultado.margen(), 0.001);
        assertEquals(0.454, resultado.probabilidades().get(0), 0.001);
        assertEquals(0.281, resultado.probabilidades().get(1), 0.001);
        assertEquals(0.265, resultado.probabilidades().get(2), 0.001);
        // Sin el margen, las probabilidades vuelven a sumar 1.
        assertEquals(1.0, resultado.probabilidades().stream().mapToDouble(Double::doubleValue).sum(), 1e-9);
    }
}
