package pe.edu.unu.apuestas.web.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;

import pe.edu.unu.apuestas.web.client.CatalogoClient;
import pe.edu.unu.apuestas.web.client.CuotasClient;
import pe.edu.unu.apuestas.web.client.PrediccionesClient;
import pe.edu.unu.apuestas.web.dto.CuotaDTO;
import pe.edu.unu.apuestas.web.dto.MercadoDTO;
import pe.edu.unu.apuestas.web.dto.PartidoDTO;
import pe.edu.unu.apuestas.web.dto.PrediccionDTO;
import pe.edu.unu.apuestas.web.vista.CeldaCuota;
import pe.edu.unu.apuestas.web.vista.FilaMercado;
import pe.edu.unu.apuestas.web.vista.Formatos;
import pe.edu.unu.apuestas.web.vista.GrupoMercados;
import pe.edu.unu.apuestas.web.vista.MatrizPoisson;
import pe.edu.unu.apuestas.web.vista.Mercados;

/**
 * Arma la pantalla de Análisis de un partido: modelo, matriz de marcadores y cuotas de las tres casas lado a lado.
 */
@Service
public class AnalisisService {

    public static final List<String> CASAS = List.of("bet365", "pinnacle", "betano");

    private final CatalogoClient catalogo;
    private final PrediccionesClient predicciones;
    private final CuotasClient cuotas;
    private final EjecutorParalelo paralelo;
    private final Formatos fmt;

    public AnalisisService(CatalogoClient catalogo, PrediccionesClient predicciones, CuotasClient cuotas, EjecutorParalelo paralelo,
            Formatos fmt) {
        this.catalogo = catalogo;
        this.predicciones = predicciones;
        this.cuotas = cuotas;
        this.paralelo = paralelo;
        this.fmt = fmt;
    }

    /**
     * Todo lo que necesita partido.html. prediccion o cuotas quedan en null si su servicio no respondió (RN-WEB-04).
     */
    public record VistaAnalisis(PartidoDTO partido, PrediccionDTO prediccion, List<CuotaDTO> cuotas, List<GrupoMercados> grupos,
            MatrizPoisson matriz) {

        public boolean sinPrediccion() {
            return prediccion == null;
        }

        public boolean sinCuotas() {
            return cuotas == null;
        }

        // Probabilidad del modelo para una clave como "1X2|LOCAL|", o null.
        public Double probabilidad(String clave) {
            MercadoDTO m = prediccion == null ? null : prediccion.mercado(clave);
            return m == null ? null : m.probabilidad();
        }

        // Las cuotas de una sola casa (por ejemplo, las de Betano ya cargadas).
        public List<CuotaDTO> cuotasDe(String casa) {
            return cuotas == null ? List.of() : cuotas.stream().filter(c -> casa.equals(c.casa())).toList();
        }
    }

    // Pide predicción y cuotas a la vez. Si el partido no existe, el catálogo responde 404 y la excepción sube al controlador.
    public VistaAnalisis armar(long partidoId) {
        CompletableFuture<PrediccionDTO> prediccion = paralelo.intentar(() -> predicciones.prediccion(partidoId));
        CompletableFuture<List<CuotaDTO>> cuotasPartido = paralelo.intentar(() -> cuotas.cuotas(partidoId));
        PartidoDTO partido = catalogo.partido(partidoId);

        PrediccionDTO pr = prediccion.join();
        List<CuotaDTO> cs = cuotasPartido.join();
        MatrizPoisson matriz = pr == null ? null
                : MatrizPoisson.calcular(pr.esperados().golesLocal(), pr.esperados().golesVisitante(), 5);
        return new VistaAnalisis(partido, pr, cs, grupos(partido, pr, cs), matriz);
    }

    // Una tabla por mercado con todas sus selecciones y líneas, aunque falte la predicción o las cuotas.
    private List<GrupoMercados> grupos(PartidoDTO partido, PrediccionDTO prediccion, List<CuotaDTO> cuotasPartido) {
        List<GrupoMercados> grupos = new ArrayList<>();
        Mercados.NOMBRES.forEach((mercado, nombre) -> {
            List<FilaMercado> filas = Mercados.delMercado(mercado).stream().map(opcion -> {
                MercadoDTO m = prediccion == null ? null : prediccion.mercado(opcion.clave());
                Double probabilidad = m == null ? null : m.probabilidad();
                Map<String, CeldaCuota> casas = new LinkedHashMap<>();
                for (String casa : CASAS) {
                    Double valor = cuotasPartido == null ? null
                            : cuotasPartido.stream().filter(c -> casa.equals(c.casa()) && c.clave().equals(opcion.clave()))
                                    .map(CuotaDTO::valor).findFirst().orElse(null);
                    casas.put(casa, CeldaCuota.de(valor, probabilidad));
                }
                String etiqueta = fmt.etiqueta(opcion, partido.local().nombre(), partido.visitante().nombre());
                return new FilaMercado(opcion, etiqueta, probabilidad, m == null ? null : m.cuotaJusta(), casas);
            }).toList();
            grupos.add(new GrupoMercados(nombre, filas));
        });
        return grupos;
    }
}
