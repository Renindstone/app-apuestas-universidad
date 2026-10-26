package pe.edu.unu.apuestas.catalogo.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;

/**
 * Pruebas por HTTP contra H2. @Transactional deshace los cambios de cada prueba al terminar.
 */
@SpringBootTest
@Transactional
class CatalogoTest {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    // Envía una fila a POST /interno/partidos. arbitro y goles pueden ser null.
    private ResultActions fila(String liga, String fechaHora, String local, String visitante, String arbitro, Integer gl, Integer gv)
            throws Exception {
        String json = """
                {"ligaCodigo":"%s","temporada":"2627","fechaHora":"%s","local":"%s","visitante":"%s",
                 "arbitro":%s,"golesLocal":%s,"golesVisitante":%s}"""
                .formatted(liga, fechaHora, local, visitante, arbitro == null ? "null" : "\"" + arbitro + "\"", gl, gv);
        return mvc.perform(post("/interno/partidos").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static long id(ResultActions r) throws Exception {
        return ((Number) JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    // RN-CAT-01: al arrancar existen las 3 ligas.
    @Test
    void lasTresLigasExistenAlArrancar() throws Exception {
        mvc.perform(get("/ligas")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].codigo").value("E0"))
                .andExpect(jsonPath("$[1].codigo").value("SP1"))
                .andExpect(jsonPath("$[2].codigo").value("D1"));
    }

    // RN-CAT-02 y RN-CAT-03: enviar la misma fila dos veces no duplica el partido, ni los equipos, ni el árbitro.
    @Test
    void elUpsertEsIdempotente() throws Exception {
        long primero = id(fila("E0", "2026-10-24T14:00:00Z", "Arsenal", "Chelsea", "M Oliver", null, null).andExpect(status().isOk()));
        long segundo = id(fila("E0", "2026-10-24T14:00:00Z", "Arsenal", "Chelsea", "M Oliver", null, null).andExpect(status().isOk()));
        assertEquals(primero, segundo);

        mvc.perform(get("/partidos").param("fecha", "2026-10-24")).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].liga.codigo").value("E0"))
                .andExpect(jsonPath("$[0].local.nombre").value("Arsenal"))
                .andExpect(jsonPath("$[0].arbitro.nombre").value("M Oliver"))
                .andExpect(jsonPath("$[0].estado").value("PROGRAMADO"));
        mvc.perform(get("/equipos").param("ligaId", "1")).andExpect(jsonPath("$", hasSize(2)));
    }

    // RN-CAT-04: con goles pasa a FINALIZADO, y una fila posterior sin goles no lo devuelve a PROGRAMADO.
    @Test
    void unPartidoFinalizadoNoVuelveAProgramado() throws Exception {
        long id = id(fila("E0", "2026-10-18T14:00:00Z", "Everton", "Wolves", null, 2, 1));
        fila("E0", "2026-10-18T14:00:00Z", "Everton", "Wolves", null, null, null).andExpect(status().isOk());
        mvc.perform(get("/partidos/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FINALIZADO"))
                .andExpect(jsonPath("$.golesLocal").value(2))
                .andExpect(jsonPath("$.golesVisitante").value(1));
    }

    // RN-CAT-06: un día de Lima va de las 05:00 UTC a las 05:00 UTC del día siguiente.
    @Test
    void elFiltroPorFechaUsaElDiaDeLima() throws Exception {
        fila("SP1", "2026-10-25T02:00:00Z", "Getafe", "Mallorca", null, null, null);   // 21:00 del 24 en Lima
        fila("SP1", "2026-10-25T05:30:00Z", "Valencia", "Celta", null, null, null);   // 00:30 del 25 en Lima
        mvc.perform(get("/partidos").param("fecha", "2026-10-24")).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].local.nombre").value("Getafe"));
        mvc.perform(get("/partidos").param("fecha", "2026-10-25")).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].local.nombre").value("Valencia"));
        mvc.perform(get("/partidos").param("desde", "2026-10-24").param("hasta", "2026-10-25")).andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void filtraPorLigaYEstadoYOrdenaPorHora() throws Exception {
        fila("E0", "2026-10-24T16:30:00Z", "Liverpool", "Brighton", null, null, null);
        fila("E0", "2026-10-24T11:30:00Z", "Man City", "Aston Villa", null, null, null);
        fila("D1", "2026-10-24T13:30:00Z", "Leverkusen", "Freiburg", null, null, null);
        fila("E0", "2026-10-11T13:00:00Z", "Tottenham", "Newcastle", null, 1, 1);
        mvc.perform(get("/partidos").param("fecha", "2026-10-24").param("ligaId", "1"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].local.nombre").value("Man City"))
                .andExpect(jsonPath("$[1].local.nombre").value("Liverpool"));
        mvc.perform(get("/partidos").param("estado", "FINALIZADO")).andExpect(jsonPath("$", hasSize(1)));
    }

    // Últimos partidos de un equipo, del más reciente al más antiguo, con límite.
    @Test
    void ultimosPartidosDeUnEquipo() throws Exception {
        fila("E0", "2026-09-20T14:00:00Z", "Arsenal", "Fulham", null, 2, 0);
        fila("E0", "2026-10-04T14:00:00Z", "Brentford", "Arsenal", null, 1, 1);
        long arsenalVsChelsea = id(fila("E0", "2026-10-18T14:00:00Z", "Arsenal", "Chelsea", null, 3, 1));
        fila("E0", "2026-10-25T14:00:00Z", "Arsenal", "Everton", null, null, null);
        String arsenal = JsonPath.read(mvc.perform(get("/partidos/" + arsenalVsChelsea)).andReturn().getResponse().getContentAsString(),
                "$.local.id").toString();
        mvc.perform(get("/equipos/" + arsenal + "/partidos").param("estado", "FINALIZADO").param("limite", "2"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].visitante.nombre").value("Chelsea"))
                .andExpect(jsonPath("$[1].local.nombre").value("Brentford"));
    }

    @Test
    void erroresConElFormatoComun() throws Exception {
        mvc.perform(get("/partidos/999999")).andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("PARTIDO_NO_ENCONTRADO"));
        mvc.perform(get("/equipos/999999/partidos")).andExpect(status().isNotFound());
        fila("XX", "2026-10-24T14:00:00Z", "A", "B", null, null, null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalle").value("La liga XX no existe. Solo hay E0, SP1 y D1."));
        fila("E0", "2026-10-24T14:00:00Z", "A", "B", null, 2, null).andExpect(status().isBadRequest());
        fila("E0", "2026-10-24T14:00:00Z", "A", "a", null, null, null).andExpect(status().isBadRequest());
        mvc.perform(get("/partidos").param("fecha", "24-10-2026")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("PARAMETRO_INVALIDO"));
        mvc.perform(get("/partidos").param("estado", "JUGANDO")).andExpect(status().isBadRequest());
        mvc.perform(get("/partidos").param("desde", "2026-10-25").param("hasta", "2026-10-24")).andExpect(status().isBadRequest());
    }
}
