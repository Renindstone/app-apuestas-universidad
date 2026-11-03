package pe.edu.unu.apuestas.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import pe.edu.unu.apuestas.web.dto.CuotaDTO;
import pe.edu.unu.apuestas.web.service.AnalisisService.VistaAnalisis;

/**
 * La tabla «Comparar con las casas» solo muestra las casas que tienen cuotas para el partido, en el orden de
 * AnalisisService.CASAS, para no llenar la pantalla de columnas con rayas.
 */
class VistaAnalisisTest {

    private static CuotaDTO cuota(String casa) {
        return new CuotaDTO(casa, "1X2", "LOCAL", null, 2.0, "APERTURA", Instant.parse("2026-11-01T10:00:00Z"));
    }

    private static VistaAnalisis vista(List<CuotaDTO> cuotas) {
        return new VistaAnalisis(null, null, cuotas, List.of(), null);
    }

    @Test
    void muestraSoloLasCasasConCuotasEnElOrdenDeSiempre() {
        assertEquals(List.of("bet365", "betano"), vista(List.of(cuota("betano"), cuota("bet365"), cuota("bet365"))).casasConCuotas());
    }

    @Test
    void sinCuotasNoHayCasas() {
        assertEquals(List.of(), vista(List.of()).casasConCuotas());
        assertEquals(List.of(), vista(null).casasConCuotas());
    }
}
