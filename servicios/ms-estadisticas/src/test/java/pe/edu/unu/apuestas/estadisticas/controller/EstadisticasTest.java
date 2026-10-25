package pe.edu.unu.apuestas.estadisticas.controller;

import static org.hamcrest.Matchers.nullValue;
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

/**
 * Pruebas por HTTP contra H2. @Transactional deshace los cambios de cada prueba al terminar.
 */
@SpringBootTest
@Transactional
class EstadisticasTest {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    // Un lado del partido: equipoId, goles, tiros, tiros a puerta, córners, faltas, amarillas y rojas.
    private static String lado(int... v) {
        return """
                {"equipoId":%d,"goles":%d,"tiros":%d,"tirosPuerta":%d,"corners":%d,"faltas":%d,"amarillas":%d,"rojas":%d}"""
                .formatted(v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7]);
    }

    // Envía un partido de la liga 1 a POST /interno/estadisticas. arbitroId puede ser null.
    private ResultActions partido(long partidoId, String temporada, String fecha, Long arbitroId, String local, String visitante)
            throws Exception {
        String json = """
                {"partidoId":%d,"ligaId":1,"temporada":"%s","fecha":"%s","arbitroId":%s,"local":%s,"visitante":%s}"""
                .formatted(partidoId, temporada, fecha, arbitroId, local, visitante);
        return mvc.perform(post("/interno/estadisticas").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    // RN-EST-01 y RN-EST-02: un partido son dos filas; los promedios en contra salen de las filas del rival.
    @Test
    void losPromediosEnContraUsanAlRival() throws Exception {
        partido(1, "2627", "2026-10-24T14:00:00Z", 4L, lado(11, 2, 15, 6, 7, 10, 2, 0), lado(17, 1, 9, 3, 4, 13, 3, 1))
                .andExpect(status().isOk());

        mvc.perform(get("/equipos/11/promedios")).andExpect(status().isOk())
                .andExpect(jsonPath("$.condicion").value("TODOS"))
                .andExpect(jsonPath("$.partidos").value(1))
                .andExpect(jsonPath("$.aFavor.goles").value(2.0))
                .andExpect(jsonPath("$.aFavor.tiros").value(15.0))
                .andExpect(jsonPath("$.aFavor.tarjetas").value(2.0))
                .andExpect(jsonPath("$.enContra.goles").value(1.0))
                .andExpect(jsonPath("$.enContra.tiros").value(9.0))
                .andExpect(jsonPath("$.enContra.tarjetas").value(4.0));
        mvc.perform(get("/equipos/17/promedios").param("condicion", "VISITANTE"))
                .andExpect(jsonPath("$.partidos").value(1))
                .andExpect(jsonPath("$.aFavor.corners").value(4.0))
                .andExpect(jsonPath("$.enContra.corners").value(7.0));
        mvc.perform(get("/equipos/17/promedios").param("condicion", "LOCAL")).andExpect(jsonPath("$.partidos").value(0));
    }

    // RN-EST-01: si el mismo partido llega otra vez, se actualizan sus dos filas y no se duplican.
    @Test
    void elMismoPartidoOtraVezActualizaLasFilas() throws Exception {
        partido(1, "2627", "2026-10-24T14:00:00Z", null, lado(11, 0, 1, 1, 1, 1, 0, 0), lado(17, 0, 1, 1, 1, 1, 0, 0));
        partido(1, "2627", "2026-10-24T14:00:00Z", null, lado(11, 3, 12, 5, 6, 9, 1, 0), lado(17, 1, 8, 2, 3, 11, 2, 0))
                .andExpect(status().isOk());

        mvc.perform(get("/partidos/1/estadisticas")).andExpect(status().isOk())
                .andExpect(jsonPath("$.partidoId").value(1))
                .andExpect(jsonPath("$.local.equipoId").value(11))
                .andExpect(jsonPath("$.local.goles").value(3))
                .andExpect(jsonPath("$.visitante.equipoId").value(17))
                .andExpect(jsonPath("$.visitante.amarillas").value(2));
        mvc.perform(get("/equipos/11/promedios")).andExpect(jsonPath("$.partidos").value(1));
    }

    // RN-EST-04: un equipo sin partidos responde 0 y los promedios en null, no en cero.
    @Test
    void unEquipoSinPartidosDevuelveNull() throws Exception {
        mvc.perform(get("/equipos/99/promedios")).andExpect(status().isOk())
                .andExpect(jsonPath("$.partidos").value(0))
                .andExpect(jsonPath("$.aFavor").value(nullValue()))
                .andExpect(jsonPath("$.enContra").value(nullValue()));
    }

    // RN-EST-03: los últimos N partidos, del más reciente al más antiguo, filtrando por condición y sin importar la temporada.
    @Test
    void seTomanLosUltimosNPartidosEnLaCondicionPedida() throws Exception {
        partido(1, "2526", "2026-04-01T14:00:00Z", null, lado(11, 1, 10, 4, 5, 10, 1, 0), lado(17, 0, 8, 2, 3, 12, 2, 0));
        partido(2, "2627", "2026-10-08T14:00:00Z", null, lado(20, 2, 10, 4, 5, 10, 1, 0), lado(11, 0, 8, 2, 3, 12, 2, 0));
        partido(3, "2627", "2026-10-15T14:00:00Z", null, lado(11, 3, 10, 4, 5, 10, 1, 0), lado(21, 1, 8, 2, 3, 12, 2, 0));

        // Como local, el último es el partido 3.
        mvc.perform(get("/equipos/11/promedios").param("condicion", "LOCAL").param("ultimos", "1"))
                .andExpect(jsonPath("$.partidos").value(1))
                .andExpect(jsonPath("$.aFavor.goles").value(3.0));
        // Como local, los partidos 1 (de la temporada anterior) y 3.
        mvc.perform(get("/equipos/11/promedios").param("condicion", "LOCAL"))
                .andExpect(jsonPath("$.partidos").value(2))
                .andExpect(jsonPath("$.aFavor.goles").value(2.0));
        // Todos: los partidos 3 y 2.
        mvc.perform(get("/equipos/11/promedios").param("ultimos", "2"))
                .andExpect(jsonPath("$.partidos").value(2))
                .andExpect(jsonPath("$.aFavor.goles").value(1.5))
                .andExpect(jsonPath("$.enContra.goles").value(1.5));
        // Los 3: (1 + 0 + 3) / 3 = 1.33, con 2 decimales.
        mvc.perform(get("/equipos/11/promedios"))
                .andExpect(jsonPath("$.partidos").value(3))
                .andExpect(jsonPath("$.aFavor.goles").value(1.33));
    }

    // Promedios de liga: separados por condición, de una temporada o de todas.
    @Test
    void losPromediosDeLigaSeparanLocalYVisitante() throws Exception {
        partido(1, "2526", "2026-04-01T14:00:00Z", null, lado(11, 1, 10, 4, 5, 10, 1, 0), lado(17, 0, 8, 2, 3, 12, 2, 0));
        partido(2, "2627", "2026-10-08T14:00:00Z", null, lado(20, 3, 16, 6, 7, 9, 0, 0), lado(11, 2, 12, 3, 4, 14, 3, 1));

        mvc.perform(get("/ligas/1/promedios").param("temporada", "2627")).andExpect(status().isOk())
                .andExpect(jsonPath("$.temporada").value("2627"))
                .andExpect(jsonPath("$.partidos").value(1))
                .andExpect(jsonPath("$.local.goles").value(3.0))
                .andExpect(jsonPath("$.visitante.goles").value(2.0))
                .andExpect(jsonPath("$.visitante.tarjetas").value(4.0));
        mvc.perform(get("/ligas/1/promedios"))
                .andExpect(jsonPath("$.temporada").value(nullValue()))
                .andExpect(jsonPath("$.partidos").value(2))
                .andExpect(jsonPath("$.local.goles").value(2.0))
                .andExpect(jsonPath("$.visitante.tiros").value(10.0));
        mvc.perform(get("/ligas/2/promedios"))
                .andExpect(jsonPath("$.partidos").value(0))
                .andExpect(jsonPath("$.local").value(nullValue()));
    }

    // RN-EST-05: el árbitro suma las tarjetas de ambos equipos y se compara con la media de su liga.
    @Test
    void elPerfilDelArbitroSumaLasTarjetasDeAmbosEquipos() throws Exception {
        // Árbitro 4: 6 tarjetas y 23 faltas, luego 3 tarjetas y 20 faltas. Árbitro 5: 0 tarjetas.
        partido(1, "2627", "2026-10-01T14:00:00Z", 4L, lado(11, 2, 15, 6, 7, 10, 2, 0), lado(17, 1, 9, 3, 4, 13, 3, 1));
        partido(2, "2627", "2026-10-08T14:00:00Z", 4L, lado(20, 1, 10, 4, 5, 8, 1, 0), lado(21, 1, 8, 2, 3, 12, 2, 0));
        partido(3, "2627", "2026-10-15T14:00:00Z", 5L, lado(22, 0, 10, 4, 5, 5, 0, 0), lado(23, 0, 8, 2, 3, 5, 0, 0));

        mvc.perform(get("/arbitros/4/perfil")).andExpect(status().isOk())
                .andExpect(jsonPath("$.partidos").value(2))
                .andExpect(jsonPath("$.tarjetasPorPartido").value(4.5))
                .andExpect(jsonPath("$.faltasPorPartido").value(21.5))
                .andExpect(jsonPath("$.mediaLigaTarjetas").value(3.0));
        mvc.perform(get("/arbitros/99/perfil")).andExpect(status().isOk())
                .andExpect(jsonPath("$.partidos").value(0))
                .andExpect(jsonPath("$.tarjetasPorPartido").value(nullValue()))
                .andExpect(jsonPath("$.mediaLigaTarjetas").value(nullValue()));
    }

    // RN-EST-06: un partido que aún no se jugó no tiene estadísticas.
    @Test
    void unPartidoSinJugarDevuelve404() throws Exception {
        mvc.perform(get("/partidos/999/estadisticas")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PARTIDO_SIN_ESTADISTICAS"));
    }

    // Validaciones: datos incompletos o imposibles responden 400, y una ruta que no existe 404 (no 500).
    @Test
    void lasPeticionesInvalidasSeRechazan() throws Exception {
        partido(1, "2627", "2026-10-24T14:00:00Z", null, lado(11, 2, 15, 6, 7, 10, 2, 0), lado(11, 1, 9, 3, 4, 13, 3, 1))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDACION"));
        partido(1, "2627", "2026-10-24T14:00:00Z", null, lado(11, -1, 15, 6, 7, 10, 2, 0), lado(17, 1, 9, 3, 4, 13, 3, 1))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detalle").value("Las estadísticas no pueden ser negativas."));
        partido(1, "26-27", "2026-10-24T14:00:00Z", null, lado(11, 2, 15, 6, 7, 10, 2, 0), lado(17, 1, 9, 3, 4, 13, 3, 1))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/equipos/11/promedios").param("condicion", "CASA"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("PARAMETRO_INVALIDO"));
        mvc.perform(get("/equipos/11/promedios").param("ultimos", "0")).andExpect(status().isBadRequest());
        mvc.perform(get("/no-existe")).andExpect(status().isNotFound());
        mvc.perform(get("/partidos/1/estadisticas")).andExpect(status().isNotFound());
    }
}
