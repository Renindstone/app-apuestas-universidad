package pe.edu.unu.apuestas.cuotas.controller;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
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

import feign.FeignException;
import feign.Request;
import feign.Response;
import pe.edu.unu.apuestas.cuotas.client.CatalogoClient;

/**
 * Pruebas por HTTP contra H2, con ms-catalogo simulado (Mockito). @Transactional deshace los cambios de cada prueba.
 */
@SpringBootTest
@Transactional
class CuotasTest {

    @Autowired
    private WebApplicationContext contexto;

    @MockitoBean
    private CatalogoClient catalogo;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    // Una cuota del cuerpo de /interno/cuotas. linea va como texto: "null" o "2.5".
    private static String cuota(String mercado, String seleccion, String linea, String valor) {
        return """
                {"mercado":"%s","seleccion":"%s","linea":%s,"valor":%s}""".formatted(mercado, seleccion, linea, valor);
    }

    // Las 3 cuotas 1X2 del ejemplo del README.
    private static String unoXDos(String local, String empate, String visita) {
        return String.join(",", cuota("1X2", "LOCAL", "null", local), cuota("1X2", "EMPATE", "null", empate),
                cuota("1X2", "VISITA", "null", visita));
    }

    // Lo que manda ms-ingesta: las cuotas de una casa para el partido 1532.
    private ResultActions ingesta(String casa, String tipo, String cuotas) throws Exception {
        String json = """
                {"partidoId":1532,"casa":"%s","tipo":"%s","cuotas":[%s]}""".formatted(casa, tipo, cuotas);
        return mvc.perform(post("/interno/cuotas").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    // Una cuota manual del usuario 7. linea va como texto: "null" o "4.5".
    private ResultActions manual(String casa, String mercado, String seleccion, String linea, String valor) throws Exception {
        String json = """
                {"partidoId":1532,"casa":"%s","mercado":"%s","seleccion":"%s","linea":%s,"valor":%s}"""
                .formatted(casa, mercado, seleccion, linea, valor);
        return mvc.perform(post("/cuotas/manual").header("X-Usuario-Id", "7").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions probabilidades(String casa, String mercado, String linea) throws Exception {
        return mvc.perform(get("/partidos/1532/probabilidades-mercado").param("casa", casa).param("mercado", mercado).param("linea", linea));
    }

    // El error que lanza Feign cuando ms-catalogo responde con ese código.
    private static FeignException errorDeCatalogo(int estado) {
        Request pedido = Request.create(Request.HttpMethod.GET, "/partidos/1532", Map.of(), null, StandardCharsets.UTF_8, null);
        return FeignException.errorStatus("partido", Response.builder().status(estado).request(pedido).headers(Map.of()).build());
    }

    // RN-CUO-04: la misma cuota guardada dos veces se actualiza; la consulta muestra una por selección.
    @Test
    void laMismaCuotaSeActualizaEnLugarDeDuplicarse() throws Exception {
        String goles = cuota("GOLES", "MAS", "2.5", "1.90") + "," + cuota("GOLES", "MENOS", "2.5", "1.90");
        ingesta("bet365", "APERTURA", unoXDos("2.10", "3.40", "3.60") + "," + goles)
                .andExpect(status().isOk()).andExpect(jsonPath("$.guardadas").value(5));
        ingesta("bet365", "APERTURA", unoXDos("2.20", "3.40", "3.50") + "," + goles).andExpect(status().isOk());

        mvc.perform(get("/partidos/1532/cuotas")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[?(@.seleccion == 'LOCAL')].valor").value(hasItem(2.2)))
                .andExpect(jsonPath("$[?(@.seleccion == 'LOCAL')].tipo").value(hasItem("APERTURA")))
                .andExpect(jsonPath("$[?(@.seleccion == 'MAS')].linea").value(hasItem(2.5)));
    }

    // El filtro casa es opcional.
    @Test
    void lasCuotasSeFiltranPorCasa() throws Exception {
        ingesta("bet365", "APERTURA", unoXDos("2.10", "3.40", "3.60"));
        ingesta("pinnacle", "APERTURA", unoXDos("2.15", "3.50", "3.70"));

        mvc.perform(get("/partidos/1532/cuotas")).andExpect(jsonPath("$", hasSize(6)));
        mvc.perform(get("/partidos/1532/cuotas").param("casa", "pinnacle"))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].casa").value(everyItem(is("pinnacle"))));
        mvc.perform(get("/partidos/9/cuotas")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    // RN-01 y RN-02: margen = Σ(1/cuota) − 1 y p = q / Σq, con 3 decimales.
    @Test
    void calculaElMargenYLasProbabilidadesSinMargen() throws Exception {
        ingesta("bet365", "APERTURA", unoXDos("2.10", "3.40", "3.60") + ","
                + cuota("GOLES", "MAS", "2.5", "1.90") + "," + cuota("GOLES", "MENOS", "2.5", "1.90"));

        probabilidades("bet365", "1X2", "").andExpect(status().isOk())
                .andExpect(jsonPath("$.casa").value("bet365"))
                .andExpect(jsonPath("$.linea").value(nullValue()))
                .andExpect(jsonPath("$.margen").value(0.048))
                .andExpect(jsonPath("$.probabilidades.LOCAL").value(0.454))
                .andExpect(jsonPath("$.probabilidades.EMPATE").value(0.281))
                .andExpect(jsonPath("$.probabilidades.VISITA").value(0.265));
        probabilidades("bet365", "GOLES", "2.5").andExpect(status().isOk())
                .andExpect(jsonPath("$.margen").value(0.053))
                .andExpect(jsonPath("$.probabilidades.MAS").value(0.5))
                .andExpect(jsonPath("$.probabilidades.MENOS").value(0.5));
    }

    // RN-CUO-05: sin el mercado completo no hay margen.
    @Test
    void unMercadoIncompletoDevuelve409() throws Exception {
        ingesta("bet365", "APERTURA", cuota("GOLES", "MAS", "2.5", "1.90"));

        probabilidades("bet365", "GOLES", "2.5").andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("MERCADO_INCOMPLETO"))
                .andExpect(jsonPath("$.detalle").value("Faltan las cuotas de MENOS de bet365 en GOLES."));
        probabilidades("pinnacle", "1X2", "").andExpect(status().isConflict());
    }

    // RN-CUO-02 y RN-CUO-03: la cuota de Betano se guarda como MANUAL; si se vuelve a cargar, se actualiza.
    @Test
    void laCuotaManualSeGuardaComoManual() throws Exception {
        manual("betano", "TARJETAS", "MAS", "4.5", "1.85").andExpect(status().isCreated())
                .andExpect(jsonPath("$.casa").value("betano"))
                .andExpect(jsonPath("$.tipo").value("MANUAL"))
                .andExpect(jsonPath("$.valor").value(1.85));
        manual("betano", "TARJETAS", "MAS", "4.5", "1.95").andExpect(status().isCreated());

        mvc.perform(get("/partidos/1532/cuotas").param("casa", "betano"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].valor").value(1.95));
    }

    // RN-CUO-03: 404 si el partido no existe en ms-catalogo y 503 si ms-catalogo no responde. En ningún caso se guarda.
    @Test
    void unaCuotaManualNecesitaQueElPartidoExista() throws Exception {
        doThrow(errorDeCatalogo(404)).when(catalogo).partido(1532L);
        manual("betano", "1X2", "LOCAL", "null", "2.05").andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PARTIDO_NO_ENCONTRADO"));

        doThrow(errorDeCatalogo(503)).when(catalogo).partido(1532L);
        manual("betano", "1X2", "LOCAL", "null", "2.05").andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("SERVICIO_NO_DISPONIBLE"));

        mvc.perform(get("/partidos/1532/cuotas")).andExpect(jsonPath("$", hasSize(0)));
    }

    // RN-CUO-01, RN-CUO-02 y RN-09: datos no válidos responden 400 (o 401 sin usuario) sin consultar a ms-catalogo.
    @Test
    void lasCuotasManualesSeValidan() throws Exception {
        manual("bet365", "1X2", "LOCAL", "null", "2.05").andExpect(status().isBadRequest());
        manual("betano", "1X2", "LOCAL", "null", "1.01").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CUOTA_INVALIDA"));
        manual("betano", "TARJETAS", "SI", "4.5", "1.85").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalle").value("La selección SI no existe en TARJETAS. Son MAS o MENOS."));
        manual("betano", "GOLES", "MAS", "2.0", "1.85").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalle").value("La línea de GOLES debe ser 1.5, 2.5 o 3.5."));
        manual("betano", "1X2", "LOCAL", "2.5", "2.05").andExpect(status().isBadRequest());
        manual("betano", "1X2", "LOCAL", "null", "2.055").andExpect(status().isBadRequest());
        mvc.perform(post("/cuotas/manual").contentType(MediaType.APPLICATION_JSON)
                .content("{\"partidoId\":1532,\"casa\":\"betano\",\"mercado\":\"1X2\",\"seleccion\":\"LOCAL\",\"valor\":2.05}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("NO_AUTORIZADO"));
        verifyNoInteractions(catalogo);
    }

    // RN-CUO-01 y RN-CUO-02: la ingesta solo trae bet365 y Pinnacle, nunca MANUAL, y una cuota mala no deja nada a medias.
    @Test
    void lasCuotasDeLaIngestaSeValidan() throws Exception {
        ingesta("betano", "APERTURA", unoXDos("2.10", "3.40", "3.60")).andExpect(status().isBadRequest());
        ingesta("bet365", "MANUAL", unoXDos("2.10", "3.40", "3.60")).andExpect(status().isBadRequest());
        ingesta("bet365", "APERTURA", unoXDos("2.10", "3.40", "1.00")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CUOTA_INVALIDA"));
        ingesta("bet365", "APERTURA", cuota("HANDICAP", "LOCAL", "null", "1.90")).andExpect(status().isBadRequest());
        ingesta("bet365", "OTRO", unoXDos("2.10", "3.40", "3.60")).andExpect(status().isBadRequest());
        mvc.perform(get("/partidos/1532/cuotas")).andExpect(jsonPath("$", hasSize(0)));

        probabilidades("betfair", "1X2", "").andExpect(status().isBadRequest());
        mvc.perform(get("/partidos/1532/probabilidades-mercado").param("casa", "bet365")).andExpect(status().isBadRequest());
    }
}
