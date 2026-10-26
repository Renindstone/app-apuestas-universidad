package pe.edu.unu.apuestas.predicciones.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import feign.FeignException;
import feign.Request;
import feign.Response;
import pe.edu.unu.apuestas.predicciones.client.CatalogoClient;
import pe.edu.unu.apuestas.predicciones.client.EstadisticasClient;
import pe.edu.unu.apuestas.predicciones.dto.PartidoDTO;
import pe.edu.unu.apuestas.predicciones.dto.PartidoDTO.Referencia;
import pe.edu.unu.apuestas.predicciones.dto.PerfilArbitroDTO;
import pe.edu.unu.apuestas.predicciones.dto.PromediosDTO;
import pe.edu.unu.apuestas.predicciones.dto.PromediosEquipoDTO;
import pe.edu.unu.apuestas.predicciones.dto.PromediosLigaDTO;
import pe.edu.unu.apuestas.predicciones.service.PoissonService;

/**
 * Pruebas por HTTP con ms-catalogo y ms-estadisticas simulados (Mockito).
 * La liga de prueba: el local hace 1.5 goles, 5 córners y 2 tarjetas; el visitante 1.2, 4 y 2.
 */
@SpringBootTest
class PrediccionTest {

    private static final PromediosDTO LIGA_LOCAL = new PromediosDTO(1.5, 12.0, 4.0, 5.0, 11.0, 2.0);
    private static final PromediosDTO LIGA_VISITANTE = new PromediosDTO(1.2, 10.0, 3.5, 4.0, 12.0, 2.0);

    @Autowired
    private WebApplicationContext contexto;

    @MockitoBean
    private CatalogoClient catalogo;

    @MockitoBean
    private EstadisticasClient estadisticas;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        when(estadisticas.liga(1L)).thenReturn(new PromediosLigaDTO(1L, 90, LIGA_LOCAL, LIGA_VISITANTE));
    }

    // Arsenal (11) recibe a Chelsea (17), con o sin árbitro (4). doReturn y doThrow permiten cambiar la respuesta en una misma prueba.
    private void partido(boolean conArbitro) {
        doReturn(new PartidoDTO(1532L, new Referencia(1L, "Premier League"),
                Instant.parse("2026-10-24T14:00:00Z"), new Referencia(11L, "Arsenal"), new Referencia(17L, "Chelsea"),
                conArbitro ? new Referencia(4L, "M Oliver") : null)).when(catalogo).partido(1532L);
    }

    // Promedios de un equipo en sus n partidos: a favor y en contra iguales a los de la liga (factores = 1).
    private void comoLaMedia(long equipoId, String condicion, long n) {
        PromediosDTO aFavor = condicion.equals("LOCAL") ? LIGA_LOCAL : LIGA_VISITANTE;
        PromediosDTO enContra = condicion.equals("LOCAL") ? LIGA_VISITANTE : LIGA_LOCAL;
        when(estadisticas.equipo(equipoId, condicion, 10))
                .thenReturn(new PromediosEquipoDTO(equipoId, condicion, n, n == 0 ? null : aFavor, n == 0 ? null : enContra));
    }

    private ResultActions prediccion() throws Exception {
        return mvc.perform(get("/partidos/1532"));
    }

    // El error que lanza Feign cuando un servicio responde con ese código.
    private static FeignException error(int estado) {
        Request pedido = Request.create(Request.HttpMethod.GET, "/", Map.of(), null, StandardCharsets.UTF_8, null);
        return FeignException.errorStatus("llamada", Response.builder().status(estado).request(pedido).headers(Map.of()).build());
    }

    // Dos equipos iguales a la media: λ = μ de la liga, y las probabilidades son las de Poisson con esos λ (RN-PRE-02 y RN-PRE-04).
    @Test
    void dosEquiposPromedioDanLaMediaDeLaLiga() throws Exception {
        partido(false);
        comoLaMedia(11L, "LOCAL", 10);
        comoLaMedia(17L, "VISITANTE", 10);
        double[] unoXDos = PoissonService.unoXDos(PoissonService.matriz(1.5, 1.2));

        prediccion().andExpect(status().isOk())
                .andExpect(jsonPath("$.partidoId").value(1532))
                .andExpect(jsonPath("$.partido").value("Arsenal vs Chelsea"))
                .andExpect(jsonPath("$.fechaHora").value("2026-10-24T14:00:00Z"))
                .andExpect(jsonPath("$.esperados.golesLocal").value(1.5))
                .andExpect(jsonPath("$.esperados.golesVisitante").value(1.2))
                .andExpect(jsonPath("$.esperados.corners").value(9.0))
                .andExpect(jsonPath("$.esperados.tarjetas").value(4.0))
                .andExpect(jsonPath("$.esperados.tiros").value(22.0))
                .andExpect(jsonPath("$.factorArbitro").value(1.0))
                .andExpect(jsonPath("$.muestra.partidosLocal").value(10))
                .andExpect(jsonPath("$.muestra.partidosVisitante").value(10))
                // RN-PRE-06: 3 de 1X2, 6 de goles, 2 de ambos marcan, 6 de tarjetas y 6 de córners.
                .andExpect(jsonPath("$.mercados", hasSize(23)))
                .andExpect(jsonPath("$.mercados[0].mercado").value("1X2"))
                .andExpect(jsonPath("$.mercados[0].seleccion").value("LOCAL"))
                .andExpect(jsonPath("$.mercados[0].probabilidad").value(Math.round(unoXDos[0] * 1000) / 1000.0))
                .andExpect(jsonPath("$.mercados[0].cuotaJusta").value(Math.round(100 / unoXDos[0]) / 100.0))
                .andExpect(jsonPath("$.mercados[?(@.mercado == 'CORNERS' && @.linea == 10.5)].seleccion").value(hasItem("MENOS")))
                .andExpect(jsonPath("$.explicacion[2]").value("El partido no tiene árbitro asignado, así que el factor árbitro vale 1."));
        verify(estadisticas, never()).arbitro(anyLong());
    }

    // RN-PRE-03: un local que hace el doble de goles que la media en 10 partidos tiene factor (10 × 2 + 5) / 15 = 1.667.
    @Test
    void losFactoresSeSuavizanSegunLaMuestra() throws Exception {
        partido(false);
        when(estadisticas.equipo(11L, "LOCAL", 10)).thenReturn(new PromediosEquipoDTO(11L, "LOCAL", 10,
                new PromediosDTO(3.0, 12.0, 4.0, 5.0, 11.0, 2.0), LIGA_VISITANTE));
        comoLaMedia(17L, "VISITANTE", 10);

        // λL = 1.5 × 1.667 × 1 = 2.5
        prediccion().andExpect(jsonPath("$.esperados.golesLocal").value(2.5))
                .andExpect(jsonPath("$.esperados.golesVisitante").value(1.2));
    }

    // RN-PRE-03 y RN-EST-04: un equipo sin partidos se toma como la media de la liga y la explicación lo dice.
    @Test
    void unEquipoSinPartidosEsComoLaMedia() throws Exception {
        partido(false);
        comoLaMedia(11L, "LOCAL", 0);
        comoLaMedia(17L, "VISITANTE", 3);

        prediccion().andExpect(status().isOk())
                .andExpect(jsonPath("$.esperados.golesLocal").value(1.5))
                .andExpect(jsonPath("$.muestra.partidosLocal").value(0))
                .andExpect(jsonPath("$.explicacion[0]").value("Arsenal aún no tiene partidos en casa en los datos: se usa la media de la liga."))
                .andExpect(jsonPath("$.explicacion").value(hasItem(startsWith("Chelsea solo tiene 3 partidos fuera de muestra"))));
    }

    // RN-PRE-05: con 20 partidos a 5 tarjetas y una media de 4, el factor es [(20 × 5 + 10 × 4) / 30] / 4 = 1.17.
    @Test
    void lasTarjetasSeAjustanConElFactorArbitro() throws Exception {
        partido(true);
        comoLaMedia(11L, "LOCAL", 10);
        comoLaMedia(17L, "VISITANTE", 10);
        when(estadisticas.arbitro(4L)).thenReturn(new PerfilArbitroDTO(4L, 20, 5.0, 22.0, 4.0));

        prediccion().andExpect(jsonPath("$.factorArbitro").value(1.17))
                // 4 tarjetas esperadas × 1.1667
                .andExpect(jsonPath("$.esperados.tarjetas").value(4.67))
                .andExpect(jsonPath("$.explicacion[2]").value(
                        "M Oliver saca 5.0 tarjetas por partido en 20 partidos (media de la liga: 4.0), así que el factor árbitro es 1.17."));
    }

    // Un árbitro sin partidos en los datos no cambia las tarjetas.
    @Test
    void unArbitroSinPartidosNoCambiaLasTarjetas() throws Exception {
        partido(true);
        comoLaMedia(11L, "LOCAL", 10);
        comoLaMedia(17L, "VISITANTE", 10);
        when(estadisticas.arbitro(4L)).thenReturn(new PerfilArbitroDTO(4L, 0, null, null, null));

        prediccion().andExpect(jsonPath("$.factorArbitro").value(1.0))
                .andExpect(jsonPath("$.esperados.tarjetas").value(4.0));
    }

    // RN-PRE-07: si un servicio no responde, 503 y ninguna probabilidad. Si el partido no existe, 404.
    @Test
    void sinDatosNoHayProbabilidades() throws Exception {
        doThrow(error(404)).when(catalogo).partido(1532L);
        prediccion().andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("PARTIDO_NO_ENCONTRADO"));

        doThrow(error(503)).when(catalogo).partido(1532L);
        prediccion().andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("SERVICIO_NO_DISPONIBLE"))
                .andExpect(jsonPath("$.detalle").value("ms-catalogo no responde."));

        partido(false);
        when(estadisticas.equipo(11L, "LOCAL", 10)).thenThrow(error(500));
        prediccion().andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.detalle").value("ms-estadisticas no responde."));

        when(estadisticas.liga(1L)).thenReturn(new PromediosLigaDTO(1L, 0, null, null));
        prediccion().andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("SIN_DATOS"));

        mvc.perform(get("/partidos/abc")).andExpect(status().isBadRequest());
    }
}
