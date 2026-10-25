package pe.edu.unu.apuestas.ingesta.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

import pe.edu.unu.apuestas.ingesta.client.CatalogoClient;
import pe.edu.unu.apuestas.ingesta.client.CuotasClient;
import pe.edu.unu.apuestas.ingesta.client.EstadisticasClient;
import pe.edu.unu.apuestas.ingesta.csv.CsvFootballDataParser;
import pe.edu.unu.apuestas.ingesta.dto.EstadisticasRequest;
import pe.edu.unu.apuestas.ingesta.dto.PartidoDTO;
import pe.edu.unu.apuestas.ingesta.dto.PartidoDTO.Referencia;
import pe.edu.unu.apuestas.ingesta.dto.PartidoUpsertRequest;
import pe.edu.unu.apuestas.ingesta.entity.Ejecucion;
import pe.edu.unu.apuestas.ingesta.entity.EstadoEjecucion;
import pe.edu.unu.apuestas.ingesta.entity.TipoIngesta;
import pe.edu.unu.apuestas.ingesta.repository.EjecucionRepository;

/**
 * Pruebas del orquestador con los tres servicios simulados (Mockito) y los CSV de src/test/resources/csv.
 */
@ExtendWith(MockitoExtension.class)
class ProcesadorIngestaTest {

    private static final String URL = "https://football-data.co.uk/mmz4281/2627/E0.csv";

    @Mock
    private EjecucionRepository ejecuciones;
    @Mock
    private DescargadorCsv descargador;
    @Mock
    private CatalogoClient catalogo;
    @Mock
    private EstadisticasClient estadisticas;
    @Mock
    private CuotasClient cuotas;

    private ProcesadorIngesta procesador;
    private Ejecucion ejecucion;

    @BeforeEach
    void preparar() {
        procesador = new ProcesadorIngesta(ejecuciones, descargador, new CsvFootballDataParser(), catalogo, estadisticas, cuotas);
        ejecucion = new Ejecucion(TipoIngesta.HISTORICO, URL);
        when(ejecuciones.findById(1L)).thenReturn(Optional.of(ejecucion));
        when(ejecuciones.save(any())).thenAnswer(llamada -> llamada.getArgument(0));
    }

    // Procesa un CSV de prueba como si se hubiera descargado de URL.
    private void procesar(String archivo) throws Exception {
        String csv = new ClassPathResource("csv/" + archivo).getContentAsString(StandardCharsets.UTF_8);
        when(descargador.descargar(URL)).thenReturn(csv);
        procesador.procesarArchivo(new ProcesadorIngesta.Archivo(1L, URL, "2627"));
    }

    // Lo que respondería ms-catalogo: el partido 1532 con sus IDs.
    private static PartidoDTO partido() {
        return new PartidoDTO(1532L, new Referencia(1L, "Premier League"), "2627", Instant.parse("2026-10-24T14:00:00Z"),
                new Referencia(11L, "Arsenal"), new Referencia(17L, "Chelsea"), new Referencia(4L, "M Oliver"));
    }

    // RN-ING-04: primero el catálogo, luego las estadísticas con los IDs que devolvió y al final las cuotas.
    @Test
    void unaFilaJugadaPasaPorLosTresServiciosEnOrden() throws Exception {
        when(catalogo.upsert(any())).thenReturn(partido());

        procesar("historico.csv");

        InOrder orden = inOrder(catalogo, estadisticas, cuotas);
        ArgumentCaptor<PartidoUpsertRequest> fila = ArgumentCaptor.forClass(PartidoUpsertRequest.class);
        ArgumentCaptor<EstadisticasRequest> enviadas = ArgumentCaptor.forClass(EstadisticasRequest.class);
        orden.verify(catalogo).upsert(fila.capture());
        orden.verify(estadisticas).guardar(enviadas.capture());
        // atLeastOnce: la fila 1 manda dos cuotas seguidas (bet365 y Pinnacle) y times(1) las contaría juntas.
        orden.verify(cuotas, atLeastOnce()).guardar(any());

        assertEquals(new PartidoUpsertRequest("E0", "2627", Instant.parse("2026-10-24T14:00:00Z"), "Arsenal", "Chelsea", "M Oliver",
                2, 1), fila.getValue());
        assertEquals(new EstadisticasRequest(1532L, 1L, "2627", Instant.parse("2026-10-24T14:00:00Z"), 4L,
                new EstadisticasRequest.Lado(11L, 2, 15, 6, 7, 10, 2, 0),
                new EstadisticasRequest.Lado(17L, 1, 9, 3, 4, 13, 3, 0)), enviadas.getValue());
        // Fila 1: bet365 y Pinnacle. Filas 2 y 3: solo bet365.
        verify(cuotas, times(4)).guardar(any());
        assertEquals(EstadoEjecucion.OK, ejecucion.getEstado());
        assertEquals(3, ejecucion.getFilas());
        assertNull(ejecucion.getError());
    }

    // RN-ING-04 y CB-02: un partido sin jugar no manda estadísticas, pero sí sus cuotas.
    @Test
    void unPartidoSinJugarNoMandaEstadisticas() throws Exception {
        when(catalogo.upsert(any())).thenReturn(partido());

        procesar("fixtures.csv");

        verify(catalogo, times(2)).upsert(any());
        verify(estadisticas, never()).guardar(any());
        verify(cuotas, times(2)).guardar(any());
        assertEquals(2, ejecucion.getFilas());
    }

    // RN-ING-05: una fila que falla se anota y la ejecución sigue con las demás.
    @Test
    void unaFilaConErrorNoDetieneLaEjecucion() throws Exception {
        when(catalogo.upsert(any())).thenThrow(new RuntimeException("ms-catalogo respondió 400")).thenReturn(partido());

        procesar("historico.csv");

        verify(catalogo, times(3)).upsert(any());
        assertEquals(EstadoEjecucion.OK, ejecucion.getEstado());
        assertEquals(2, ejecucion.getFilas());
        assertEquals("1 de 3 filas fallaron. El detalle está en el log de ms-ingesta.", ejecucion.getError());
    }

    // RN-ING-05: si la fuente no responde, la ejecución termina en ERROR y no se llama a ningún servicio.
    @Test
    void siLaFuenteNoRespondeLaEjecucionTerminaEnError() throws Exception {
        when(descargador.descargar(URL)).thenThrow(new IOException("football-data respondió 503."));

        procesador.procesarArchivo(new ProcesadorIngesta.Archivo(1L, URL, "2627"));

        verifyNoInteractions(catalogo, estadisticas, cuotas);
        assertEquals(EstadoEjecucion.ERROR, ejecucion.getEstado());
        assertTrue(ejecucion.getError().contains("503"));
    }
}
