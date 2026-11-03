package pe.edu.unu.apuestas.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import pe.edu.unu.apuestas.web.dto.EquipoDTO;
import pe.edu.unu.apuestas.web.dto.LigaDTO;
import pe.edu.unu.apuestas.web.dto.PartidoDTO;

/**
 * El filtro de la pantalla Hoy: por liga y por franja horaria. La franja se mide en hora de Lima (RN-WEB-03),
 * y los dos extremos cuentan (04:30 y 08:00 entran).
 */
class FiltroPartidosTest {

    private static final LigaDTO PREMIER = new LigaDTO(1L, "E0", "Premier League", "Inglaterra");
    private static final LigaDTO LALIGA = new LigaDTO(2L, "SP1", "LaLiga", "España");

    private static final LocalTime DESDE = LocalTime.of(4, 30);
    private static final LocalTime HASTA = LocalTime.of(8, 0);

    // Lima es UTC−5: 09:30Z son las 04:30 en Lima.
    private static PartidoDTO partido(long id, LigaDTO liga, String utc) {
        return new PartidoDTO(id, liga, "2627", Instant.parse(utc), new EquipoDTO(id * 10, "Local " + id),
                new EquipoDTO(id * 10 + 1, "Visita " + id), null, null, null, "PROGRAMADO");
    }

    private final List<PartidoDTO> partidos = List.of(
            partido(1, PREMIER, "2026-11-07T09:29:00Z"),   // 04:29 Lima: antes de la franja
            partido(2, PREMIER, "2026-11-07T09:30:00Z"),   // 04:30: entra
            partido(3, LALIGA, "2026-11-07T11:30:00Z"),    // 06:30: entra
            partido(4, PREMIER, "2026-11-07T13:00:00Z"),   // 08:00: entra
            partido(5, LALIGA, "2026-11-07T13:01:00Z"));   // 08:01: después de la franja

    private static List<Long> ids(List<PartidoDTO> lista) {
        return lista.stream().map(PartidoDTO::id).toList();
    }

    @Test
    void sinFiltrosMuestraTodo() {
        assertEquals(List.of(1L, 2L, 3L, 4L, 5L), ids(HoyService.filtrar(partidos, null, null, null)));
        assertEquals(List.of(1L, 2L, 3L, 4L, 5L), ids(HoyService.filtrar(partidos, "", null, null)));
    }

    @Test
    void laFranjaIncluyeSusDosExtremos() {
        assertEquals(List.of(2L, 3L, 4L), ids(HoyService.filtrar(partidos, null, DESDE, HASTA)));
    }

    @Test
    void unSoloExtremoTambienFiltra() {
        assertEquals(List.of(3L, 4L, 5L), ids(HoyService.filtrar(partidos, null, LocalTime.of(5, 0), null)));
        assertEquals(List.of(1L, 2L), ids(HoyService.filtrar(partidos, null, null, LocalTime.of(4, 30))));
    }

    @Test
    void sumaLaLigaYLaFranja() {
        assertEquals(List.of(2L, 4L), ids(HoyService.filtrar(partidos, "E0", DESDE, HASTA)));
        assertEquals(List.of(3L), ids(HoyService.filtrar(partidos, "SP1", DESDE, HASTA)));
    }

    @Test
    void sinPartidosEnLaFranjaDevuelveVacio() {
        assertEquals(List.of(), HoyService.filtrar(partidos, "D1", DESDE, HASTA));
    }
}
