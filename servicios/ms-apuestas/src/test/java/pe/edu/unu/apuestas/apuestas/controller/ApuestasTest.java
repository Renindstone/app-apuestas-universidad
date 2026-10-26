package pe.edu.unu.apuestas.apuestas.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;

import feign.FeignException;
import feign.Request;
import feign.Response;
import pe.edu.unu.apuestas.apuestas.client.CatalogoClient;
import pe.edu.unu.apuestas.apuestas.client.CuotasClient;
import pe.edu.unu.apuestas.apuestas.client.EstadisticasClient;
import pe.edu.unu.apuestas.apuestas.client.PrediccionesClient;
import pe.edu.unu.apuestas.apuestas.dto.CuotaMercadoDTO;
import pe.edu.unu.apuestas.apuestas.dto.EstadisticasPartidoDTO;
import pe.edu.unu.apuestas.apuestas.dto.PartidoDTO;
import pe.edu.unu.apuestas.apuestas.dto.PrediccionDTO;

/**
 * Pruebas por HTTP contra H2, con ms-catalogo, ms-predicciones, ms-cuotas y ms-estadisticas simulados (Mockito).
 * @Transactional deshace los cambios de cada prueba al terminar.
 */
@SpringBootTest
@Transactional
class ApuestasTest {

    private static final String HOY = LocalDate.now(ZoneId.of("America/Lima")).toString();

    @Autowired
    private WebApplicationContext contexto;

    @MockitoBean
    private CatalogoClient catalogo;

    @MockitoBean
    private PrediccionesClient predicciones;

    @MockitoBean
    private CuotasClient cuotas;

    @MockitoBean
    private EstadisticasClient estadisticas;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    // ---------- Datos simulados ----------

    // Lo que respondería ms-catalogo: Arsenal vs Chelsea, con o sin resultado.
    private static PartidoDTO partido(long id, Integer golesLocal, Integer golesVisitante) {
        return new PartidoDTO(id, new PartidoDTO.Referencia(11L, "Arsenal"), new PartidoDTO.Referencia(17L, "Chelsea"), golesLocal,
                golesVisitante, golesLocal == null ? "PROGRAMADO" : "FINALIZADO");
    }

    private static PrediccionDTO.Mercado mercado(String mercado, String seleccion, Double linea, double probabilidad) {
        return new PrediccionDTO.Mercado(mercado, seleccion, linea, probabilidad, Math.round(100 / probabilidad) / 100.0);
    }

    // La predicción con n partidos de muestra por equipo y estas probabilidades.
    private static PrediccionDTO prediccion(long partidoId, long muestra, PrediccionDTO.Mercado... mercados) {
        return new PrediccionDTO(partidoId, "Arsenal vs Chelsea", new PrediccionDTO.Muestra(muestra, muestra), List.of(mercados));
    }

    private static CuotaMercadoDTO cuota(String casa, String mercado, String seleccion, String linea, String valor) {
        return new CuotaMercadoDTO(casa, mercado, seleccion, linea == null ? null : new BigDecimal(linea), new BigDecimal(valor));
    }

    // El error que lanza Feign cuando un servicio responde con ese código.
    private static FeignException error(int estado) {
        Request pedido = Request.create(Request.HttpMethod.GET, "/", Map.of(), null, StandardCharsets.UTF_8, null);
        return FeignException.errorStatus("llamada", Response.builder().status(estado).request(pedido).headers(Map.of()).build());
    }

    /**
     * Hoy se juegan el partido 1532, con 10 partidos de muestra, y el 1533, con solo 4. Las cuotas del 1532:
     * GOLES MAS 2.5 en bet365 a 2.00 (p 0.531, EV 0.062) y 1X2 LOCAL en betano a 2.10 (p 0.50, EV 0.05) son candidatas.
     * No lo son: la de Pinnacle (no se usa), la de 1.40 (cuota baja), la de p 0.41 (probabilidad baja) y la de EV 0.02.
     */
    private void hoySeJuegan() {
        doReturn(List.of(partido(1532L, null, null), partido(1533L, null, null))).when(catalogo).partidos(HOY, "PROGRAMADO");
        doReturn(prediccion(1532L, 10, mercado("GOLES", "MAS", 2.5, 0.531), mercado("1X2", "LOCAL", null, 0.50),
                mercado("1X2", "VISITA", null, 0.25), mercado("AMBOS_MARCAN", "SI", null, 0.41), mercado("CORNERS", "MAS", 9.5, 0.51)))
                .when(predicciones).prediccion(1532L);
        doReturn(List.of(cuota("bet365", "GOLES", "MAS", "2.50", "2.00"), cuota("betano", "1X2", "LOCAL", null, "2.10"),
                cuota("pinnacle", "GOLES", "MAS", "2.50", "2.20"), cuota("bet365", "1X2", "VISITA", null, "1.40"),
                cuota("bet365", "AMBOS_MARCAN", "SI", null, "2.60"), cuota("bet365", "CORNERS", "MAS", "9.50", "2.00")))
                .when(cuotas).cuotas(1532L);
        doReturn(prediccion(1533L, 4, mercado("GOLES", "MAS", 2.5, 0.70))).when(predicciones).prediccion(1533L);
    }

    private ResultActions apuestaDelDia(String... parametros) throws Exception {
        var pedido = get("/apuesta-del-dia");
        for (int i = 0; i < parametros.length; i += 2) {
            pedido = pedido.param(parametros[i], parametros[i + 1]);
        }
        return mvc.perform(pedido);
    }

    // Registra una apuesta del usuario. linea va como texto: "null" o "2.5".
    private ResultActions apostar(long usuarioId, String mercado, String seleccion, String linea, String cuota, String monto) throws Exception {
        String json = """
                {"partidoId":1532,"mercado":"%s","seleccion":"%s","linea":%s,"casa":"bet365","cuota":%s,"monto":%s}"""
                .formatted(mercado, seleccion, linea, cuota, monto);
        return mvc.perform(post("/mis-apuestas").header("X-Usuario-Id", String.valueOf(usuarioId))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions evaluar(String mercado, String seleccion, String linea, String cuota) throws Exception {
        String json = """
                {"partidoId":1532,"mercado":"%s","seleccion":"%s","linea":%s,"casa":"betano","cuota":%s}"""
                .formatted(mercado, seleccion, linea, cuota);
        return mvc.perform(post("/evaluaciones").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions liquidar() throws Exception {
        return mvc.perform(post("/interno/liquidar"));
    }

    // ---------- Evaluaciones ----------

    // El ejemplo del contrato: TARJETAS MAS 4.5 a 2.20 con p = 0.497 da EV 0.093 y stake 1.9 %. No hace falta ms-cuotas (RN-APU-01).
    @Test
    void evaluaUnaCuotaConLaProbabilidadDelModelo() throws Exception {
        doReturn(prediccion(1532L, 10, mercado("TARJETAS", "MAS", 4.5, 0.497))).when(predicciones).prediccion(1532L);

        evaluar("TARJETAS", "MAS", "4.5", "2.20").andExpect(status().isOk())
                .andExpect(jsonPath("$.probabilidad").value(0.497))
                .andExpect(jsonPath("$.cuotaJusta").value(2.01))
                .andExpect(jsonPath("$.valorEsperado").value(0.093))
                .andExpect(jsonPath("$.stakePct").value(0.019))
                .andExpect(jsonPath("$.veredicto").value("CON_VALOR"));
        verifyNoInteractions(cuotas);
    }

    // Una selección que no existe es 400; un partido que no existe, 404; y sin ms-predicciones, 503.
    @Test
    void lasEvaluacionesInvalidasSeRechazan() throws Exception {
        evaluar("TARJETAS", "SI", "4.5", "2.20").andExpect(status().isBadRequest());
        evaluar("TARJETAS", "MAS", "4.5", "1.00").andExpect(status().isBadRequest());
        doThrow(error(404)).when(predicciones).prediccion(1532L);
        evaluar("TARJETAS", "MAS", "4.5", "2.20").andExpect(status().isNotFound());
        doThrow(error(503)).when(predicciones).prediccion(1532L);
        evaluar("TARJETAS", "MAS", "4.5", "2.20").andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detalle").value("ms-predicciones no responde."));
    }

    // ---------- Apuesta del día ----------

    // RN-06 y RN-APU-03: gana la candidata de mayor EV; las demás se guardan y se cuentan; Pinnacle no se usa.
    @Test
    void laApuestaDelDiaEsLaCandidataDeMayorValor() throws Exception {
        hoySeJuegan();

        apuestaDelDia().andExpect(status().isOk())
                .andExpect(jsonPath("$.fecha").value(HOY))
                .andExpect(jsonPath("$.pick.partido").value("Arsenal vs Chelsea"))
                .andExpect(jsonPath("$.pick.mercado").value("GOLES"))
                .andExpect(jsonPath("$.pick.seleccion").value("MAS"))
                .andExpect(jsonPath("$.pick.linea").value(2.5))
                .andExpect(jsonPath("$.pick.casa").value("bet365"))
                .andExpect(jsonPath("$.pick.cuota").value(2.0))
                .andExpect(jsonPath("$.pick.valorEsperado").value(0.062))
                .andExpect(jsonPath("$.otrasCandidatas").value(1))
                .andExpect(jsonPath("$.mensaje").value(nullValue()));
        // El partido con 4 partidos de muestra no llega a pedir sus cuotas.
        verify(cuotas, times(0)).cuotas(1533L);
    }

    // RN-APU-02: si ya existe, se devuelve la guardada sin volver a calcular; con recalcular=true se genera de nuevo.
    @Test
    void laApuestaDelDiaGuardadaSeReutiliza() throws Exception {
        hoySeJuegan();
        String primera = apuestaDelDia().andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(primera, "$.pick.id");

        apuestaDelDia().andExpect(jsonPath("$.pick.id").value(id));
        verify(predicciones, times(1)).prediccion(1532L);
        apuestaDelDia("recalcular", "true").andExpect(jsonPath("$.pick.id").value(not(id)))
                .andExpect(jsonPath("$.otrasCandidatas").value(1));
    }

    // RN-16 y CN-07: sin candidatas no hay apuesta del día, y se dice con claridad.
    @Test
    void sinCandidatasNoHayApuestaDelDia() throws Exception {
        doReturn(List.of()).when(catalogo).partidos("2026-10-27", "PROGRAMADO");

        apuestaDelDia("fecha", "2026-10-27").andExpect(status().isOk())
                .andExpect(jsonPath("$.pick").value(nullValue()))
                .andExpect(jsonPath("$.otrasCandidatas").value(0))
                .andExpect(jsonPath("$.mensaje").value(
                        "Hoy no hay apuesta del día: ninguna apuesta supera los umbrales. No apostar también es una decisión."));
    }

    // RN-APU-04: sin cuotas no hay apuesta del día.
    @Test
    void sinMsCuotasLaApuestaDelDiaDa503() throws Exception {
        hoySeJuegan();
        doThrow(error(503)).when(cuotas).cuotas(anyLong());

        apuestaDelDia().andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.detalle").value("ms-cuotas no responde."));
    }

    // RN-APU-08: el historial cuenta solo la apuesta del día liquidada, con 1 unidad; y un día liquidado no se recalcula.
    @Test
    void elHistorialCuentaLaApuestaDelDiaLiquidada() throws Exception {
        hoySeJuegan();
        apuestaDelDia();
        doReturn(partido(1532L, 2, 1)).when(catalogo).partido(1532L);

        liquidar().andExpect(jsonPath("$.picks").value(2));
        // GOLES MAS 2.5 a 2.00 ganó con 2-1: +1 unidad. La otra candidata (1X2 LOCAL) no cuenta.
        mvc.perform(get("/picks/historial")).andExpect(status().isOk())
                .andExpect(jsonPath("$.picks").value(1))
                .andExpect(jsonPath("$.ganados").value(1))
                .andExpect(jsonPath("$.perdidos").value(0))
                .andExpect(jsonPath("$.beneficioUnidades").value(1.0))
                .andExpect(jsonPath("$.roi").value(1.0))
                .andExpect(jsonPath("$.ultimos[0].resultado").value("GANADA"))
                .andExpect(jsonPath("$.ultimos[0].fecha").value(HOY));
        apuestaDelDia("recalcular", "true").andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("YA_LIQUIDADA"));
    }

    // ---------- Mis apuestas, liquidación y rendimiento ----------

    // RN-APU-05, RN-APU-06, RN-07 y RN-APU-07: se registran, se liquidan cuando hay resultado y el rendimiento es solo del usuario.
    @Test
    void lasApuestasSeLiquidanYElRendimientoEsDelUsuario() throws Exception {
        doReturn(partido(1532L, null, null)).when(catalogo).partido(1532L);
        apostar(7, "1X2", "LOCAL", "null", "2.10", "20.00").andExpect(status().isCreated())
                .andExpect(jsonPath("$.partido").value("Arsenal vs Chelsea"))
                .andExpect(jsonPath("$.resultado").value(nullValue()));
        apostar(7, "GOLES", "MENOS", "2.5", "1.90", "10.00");
        apostar(7, "TARJETAS", "MAS", "4.5", "2.20", "5.00");
        apostar(8, "1X2", "VISITA", "null", "3.60", "50.00");

        // Partido sin terminar: nada se liquida.
        liquidar().andExpect(jsonPath("$.apuestas").value(0)).andExpect(jsonPath("$.pendientes").value(4));

        // Termina 2-1, pero las tarjetas aún no están en ms-estadisticas.
        doReturn(partido(1532L, 2, 1)).when(catalogo).partido(1532L);
        doThrow(error(404)).when(estadisticas).estadisticas(1532L);
        liquidar().andExpect(jsonPath("$.apuestas").value(3)).andExpect(jsonPath("$.pendientes").value(1));

        // Usuario 7: LOCAL ganó (+22.00) y MENOS 2.5 perdió (−10.00). La de tarjetas sigue pendiente. La del usuario 8 no aparece.
        mvc.perform(get("/mis-apuestas/rendimiento").header("X-Usuario-Id", "7")).andExpect(status().isOk())
                .andExpect(jsonPath("$.apuestas").value(3))
                .andExpect(jsonPath("$.pendientes").value(1))
                .andExpect(jsonPath("$.ganadas").value(1))
                .andExpect(jsonPath("$.perdidas").value(1))
                .andExpect(jsonPath("$.montoApostado").value(30.0))
                .andExpect(jsonPath("$.beneficio").value(12.0))
                .andExpect(jsonPath("$.roi").value(0.4))
                .andExpect(jsonPath("$.porMercado", hasSize(2)))
                .andExpect(jsonPath("$.porMercado[0].mercado").value("1X2"))
                .andExpect(jsonPath("$.porMercado[0].beneficio").value(22.0))
                .andExpect(jsonPath("$.porMercado[1].mercado").value("GOLES"))
                .andExpect(jsonPath("$.porMercado[1].roi").value(-1.0));
        mvc.perform(get("/mis-apuestas").param("estado", "PENDIENTE").header("X-Usuario-Id", "7"))
                .andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].mercado").value("TARJETAS"));

        // Llegan las estadísticas: 6 tarjetas, así que MAS 4.5 gana 5 × 1.20 = 6.00.
        doReturn(new EstadisticasPartidoDTO(1532L, new EstadisticasPartidoDTO.Lado(7, 2, 0), new EstadisticasPartidoDTO.Lado(4, 3, 1)))
                .when(estadisticas).estadisticas(1532L);
        liquidar().andExpect(jsonPath("$.apuestas").value(1)).andExpect(jsonPath("$.pendientes").value(0));
        mvc.perform(get("/mis-apuestas").param("estado", "LIQUIDADA").header("X-Usuario-Id", "7"))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].mercado").value("TARJETAS"))
                .andExpect(jsonPath("$[0].resultado").value("GANADA"))
                .andExpect(jsonPath("$[0].ganancia").value(6.0));
        mvc.perform(get("/mis-apuestas").header("X-Usuario-Id", "8")).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].ganancia").value(-50.0));
    }

    // RN-APU-05 y RN-09: monto y cuota válidos, selección y casa existentes, partido existente y usuario del token.
    @Test
    void lasApuestasInvalidasSeRechazan() throws Exception {
        apostar(7, "1X2", "LOCAL", "null", "2.10", "0").andExpect(status().isBadRequest());
        apostar(7, "1X2", "LOCAL", "null", "1.00", "20").andExpect(status().isBadRequest());
        apostar(7, "GOLES", "MAS", "2.0", "1.90", "20").andExpect(status().isBadRequest());
        verifyNoInteractions(catalogo);
        doThrow(error(404)).when(catalogo).partido(1532L);
        apostar(7, "1X2", "LOCAL", "null", "2.10", "20").andExpect(status().isNotFound());
        mvc.perform(get("/mis-apuestas/rendimiento")).andExpect(status().isUnauthorized());
        mvc.perform(get("/mis-apuestas").param("estado", "GANADA").header("X-Usuario-Id", "7")).andExpect(status().isBadRequest());
    }
}
