package pe.edu.unu.apuestas.web.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;

import pe.edu.unu.apuestas.web.client.ApuestasClient;
import pe.edu.unu.apuestas.web.client.CatalogoClient;
import pe.edu.unu.apuestas.web.client.CuotasClient;
import pe.edu.unu.apuestas.web.client.PrediccionesClient;
import pe.edu.unu.apuestas.web.dto.ApuestaDelDiaDTO;
import pe.edu.unu.apuestas.web.dto.CuotaDTO;
import pe.edu.unu.apuestas.web.dto.LigaDTO;
import pe.edu.unu.apuestas.web.dto.MercadoDTO;
import pe.edu.unu.apuestas.web.dto.PartidoDTO;
import pe.edu.unu.apuestas.web.dto.PrediccionDTO;
import pe.edu.unu.apuestas.web.vista.CeldaCuota;
import pe.edu.unu.apuestas.web.vista.FilaPartido;
import pe.edu.unu.apuestas.web.vista.Formatos;

/**
 * Arma la pantalla Hoy: la apuesta del día y los partidos del día agrupados por liga.
 * Cada parte se pide por separado; si una falla, el resto se muestra igual (RN-WEB-04).
 */
@Service
public class HoyService {

    private static final List<String> SELECCIONES_1X2 = List.of("1X2|LOCAL|", "1X2|EMPATE|", "1X2|VISITA|");

    private final ApuestasClient apuestas;
    private final CatalogoClient catalogo;
    private final PrediccionesClient predicciones;
    private final CuotasClient cuotas;
    private final EjecutorParalelo paralelo;
    private final Formatos fmt;

    public HoyService(ApuestasClient apuestas, CatalogoClient catalogo, PrediccionesClient predicciones, CuotasClient cuotas,
            EjecutorParalelo paralelo, Formatos fmt) {
        this.apuestas = apuestas;
        this.catalogo = catalogo;
        this.predicciones = predicciones;
        this.cuotas = cuotas;
        this.paralelo = paralelo;
        this.fmt = fmt;
    }

    /**
     * Todo lo que necesita la plantilla hoy.html. Un campo en null significa que su servicio no respondió.
     *
     * @param partidoPick    el partido de la apuesta del día (para mostrar su hora)
     * @param prediccionPick su predicción (para mostrar los goles esperados)
     * @param partidosPorLiga nombre de la liga → renglones de sus partidos
     */
    public record VistaHoy(LocalDate fecha, ApuestaDelDiaDTO dia, PartidoDTO partidoPick, PrediccionDTO prediccionPick,
            List<LigaDTO> ligas, Map<String, List<FilaPartido>> partidosPorLiga) {
    }

    // Arma la vista. codigoLiga filtra los partidos ("E0", "SP1" o "D1"); null o vacío muestra todas.
    public VistaHoy armar(String codigoLiga) {
        LocalDate hoy = fmt.hoy();
        CompletableFuture<ApuestaDelDiaDTO> dia = paralelo.intentar(apuestas::apuestaDelDia);
        CompletableFuture<List<LigaDTO>> ligas = paralelo.intentar(catalogo::ligas);
        List<PartidoDTO> partidos = paralelo.intentar(() -> catalogo.partidosDelDia(hoy)).join();

        // Por cada partido visible se piden la predicción y las cuotas, todas a la vez.
        List<PartidoDTO> visibles = partidos == null ? List.of()
                : partidos.stream().filter(p -> codigoLiga == null || codigoLiga.isBlank() || p.liga().codigo().equals(codigoLiga)).toList();
        Map<Long, CompletableFuture<PrediccionDTO>> prediccionDe = new LinkedHashMap<>();
        Map<Long, CompletableFuture<List<CuotaDTO>>> cuotasDe = new LinkedHashMap<>();
        for (PartidoDTO p : visibles) {
            prediccionDe.put(p.id(), paralelo.intentar(() -> predicciones.prediccion(p.id())));
            cuotasDe.put(p.id(), paralelo.intentar(() -> cuotas.cuotas(p.id())));
        }

        Map<String, List<FilaPartido>> porLiga = null;
        if (partidos != null) {
            porLiga = new LinkedHashMap<>();
            for (PartidoDTO p : visibles) {
                FilaPartido fila = fila(p, prediccionDe.get(p.id()).join(), cuotasDe.get(p.id()).join());
                porLiga.computeIfAbsent(p.liga().nombre(), k -> new ArrayList<>()).add(fila);
            }
        }

        // Datos extra del partido de la apuesta del día: su hora y sus goles esperados.
        ApuestaDelDiaDTO apuesta = dia.join();
        PartidoDTO partidoPick = null;
        PrediccionDTO prediccionPick = null;
        if (apuesta != null && apuesta.pick() != null) {
            Long id = apuesta.pick().partidoId();
            partidoPick = partidos == null ? null : partidos.stream().filter(p -> p.id().equals(id)).findFirst().orElse(null);
            prediccionPick = prediccionDe.containsKey(id) ? prediccionDe.get(id).join()
                    : paralelo.intentar(() -> predicciones.prediccion(id)).join();
        }
        return new VistaHoy(hoy, apuesta, partidoPick, prediccionPick, ligas.join(), porLiga);
    }

    // Un renglón: probabilidades 1X2 del modelo y cuotas 1X2 de bet365, marcando las que tienen valor.
    private FilaPartido fila(PartidoDTO partido, PrediccionDTO prediccion, List<CuotaDTO> cuotasPartido) {
        List<Double> modelo = prediccion == null ? null
                : SELECCIONES_1X2.stream().map(k -> {
                    MercadoDTO m = prediccion.mercado(k);
                    return m == null ? null : m.probabilidad();
                }).toList();
        List<CeldaCuota> celdas = cuotasPartido == null ? null
                : SELECCIONES_1X2.stream().map(k -> {
                    Double valor = cuotasPartido.stream().filter(c -> "bet365".equals(c.casa()) && c.clave().equals(k))
                            .map(CuotaDTO::valor).findFirst().orElse(null);
                    MercadoDTO m = prediccion == null ? null : prediccion.mercado(k);
                    return CeldaCuota.de(valor, m == null ? null : m.probabilidad());
                }).toList();
        return new FilaPartido(partido, modelo, celdas);
    }

    // Las ligas para el filtro. Si el catálogo no las devolvió, se usan las 3 fijas de RN-CAT-01.
    public static List<LigaDTO> ligasPorDefecto() {
        return List.of(new LigaDTO(1L, "E0", "Premier League", "Inglaterra"), new LigaDTO(2L, "SP1", "LaLiga", "España"),
                new LigaDTO(3L, "D1", "Bundesliga", "Alemania"));
    }
}
