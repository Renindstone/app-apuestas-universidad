package pe.edu.unu.apuestas.predicciones.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import feign.FeignException;
import pe.edu.unu.apuestas.predicciones.client.CatalogoClient;
import pe.edu.unu.apuestas.predicciones.client.EstadisticasClient;
import pe.edu.unu.apuestas.predicciones.dto.EsperadosDTO;
import pe.edu.unu.apuestas.predicciones.dto.MercadoDTO;
import pe.edu.unu.apuestas.predicciones.dto.MuestraDTO;
import pe.edu.unu.apuestas.predicciones.dto.PartidoDTO;
import pe.edu.unu.apuestas.predicciones.dto.PerfilArbitroDTO;
import pe.edu.unu.apuestas.predicciones.dto.PrediccionDTO;
import pe.edu.unu.apuestas.predicciones.dto.PromediosDTO;
import pe.edu.unu.apuestas.predicciones.dto.PromediosEquipoDTO;
import pe.edu.unu.apuestas.predicciones.dto.PromediosLigaDTO;
import pe.edu.unu.apuestas.predicciones.exception.ApiException;

/**
 * Arma la predicción de un partido: pide los datos a ms-catalogo y ms-estadisticas, calcula los valores esperados
 * y las probabilidades de todos los mercados de RN-09, y explica de dónde sale cada número (RN-15).
 */
@Service
public class PrediccionService {

    // RN-PRE-02: el local se mide en casa y el visitante fuera, en sus últimos 10 partidos.
    private static final int ULTIMOS = 10;
    // RN-PRE-03: peso de la media de la liga al suavizar los factores de un equipo.
    private static final int PESO_EQUIPO = 5;
    // RN-PRE-05: peso de la media de la liga al suavizar el factor árbitro.
    private static final int PESO_ARBITRO = 10;
    // Líneas de cada mercado (RN-09, tabla del README raíz).
    private static final List<Double> LINEAS_GOLES = List.of(1.5, 2.5, 3.5);
    private static final List<Double> LINEAS_TARJETAS = List.of(3.5, 4.5, 5.5);
    private static final List<Double> LINEAS_CORNERS = List.of(8.5, 9.5, 10.5);

    /** Lo que se espera que haga cada equipo en una estadística (λL y λV). */
    record Esperado(double local, double visitante) {

        double total() {
            return local + visitante;
        }
    }

    private final CatalogoClient catalogo;
    private final EstadisticasClient estadisticas;

    public PrediccionService(CatalogoClient catalogo, EstadisticasClient estadisticas) {
        this.catalogo = catalogo;
        this.estadisticas = estadisticas;
    }

    /**
     * GET /partidos/{id}. 404 si el partido no existe, 503 si ms-catalogo o ms-estadisticas no responden (RN-PRE-07)
     * y 409 si la liga aún no tiene partidos jugados: sin datos no se inventa una probabilidad.
     */
    public PrediccionDTO predecir(Long partidoId) {
        PartidoDTO partido = partido(partidoId);
        PromediosLigaDTO liga = llamar("ms-estadisticas", () -> estadisticas.liga(partido.liga().id()));
        if (liga.partidos() == 0) {
            throw new ApiException(HttpStatus.CONFLICT, "SIN_DATOS",
                    "La liga " + partido.liga().nombre() + " aún no tiene partidos jugados para calcular el modelo.");
        }
        PromediosEquipoDTO local = llamar("ms-estadisticas", () -> estadisticas.equipo(partido.local().id(), "LOCAL", ULTIMOS));
        PromediosEquipoDTO visitante = llamar("ms-estadisticas",
                () -> estadisticas.equipo(partido.visitante().id(), "VISITANTE", ULTIMOS));
        PerfilArbitroDTO arbitro = partido.arbitro() == null ? null
                : llamar("ms-estadisticas", () -> estadisticas.arbitro(partido.arbitro().id()));

        Esperado goles = esperado(PromediosDTO::goles, liga, local, visitante);
        Esperado corners = esperado(PromediosDTO::corners, liga, local, visitante);
        Esperado tiros = esperado(PromediosDTO::tiros, liga, local, visitante);
        double factorArbitro = factorArbitro(arbitro);
        // RN-PRE-05: el árbitro solo ajusta las tarjetas.
        double tarjetas = esperado(PromediosDTO::tarjetas, liga, local, visitante).total() * factorArbitro;

        return new PrediccionDTO(partidoId, partido.local().nombre() + " vs " + partido.visitante().nombre(), partido.fechaHora(),
                new EsperadosDTO(redondear(goles.local()), redondear(goles.visitante()), redondear(corners.total()),
                        redondear(tarjetas), redondear(tiros.total())),
                redondear(factorArbitro), new MuestraDTO(local.partidos(), visitante.partidos()),
                mercados(goles, corners.total(), tarjetas), explicacion(partido, liga, local, visitante, arbitro, factorArbitro));
    }

    /**
     * RN-PRE-02 y RN-PRE-03: λL = μL × ataqueL × defensaV y λV = μV × ataqueV × defensaL, donde μ es la media de la liga
     * y cada factor (el promedio del equipo dividido entre μ) se acerca a 1 cuando el equipo tiene pocos partidos.
     */
    static Esperado esperado(Function<PromediosDTO, Double> stat, PromediosLigaDTO liga, PromediosEquipoDTO local,
            PromediosEquipoDTO visitante) {
        double muLocal = stat.apply(liga.local());
        double muVisitante = stat.apply(liga.visitante());
        double ataqueLocal = factor(local.aFavor(), stat, muLocal, local.partidos());
        double defensaVisitante = factor(visitante.enContra(), stat, muLocal, visitante.partidos());
        double ataqueVisitante = factor(visitante.aFavor(), stat, muVisitante, visitante.partidos());
        double defensaLocal = factor(local.enContra(), stat, muVisitante, local.partidos());
        return new Esperado(muLocal * ataqueLocal * defensaVisitante, muVisitante * ataqueVisitante * defensaLocal);
    }

    // factor' = (n × factor + 5 × 1) / (n + 5). Sin partidos (o con media 0) vale 1: el equipo es como la media de la liga.
    private static double factor(PromediosDTO promedios, Function<PromediosDTO, Double> stat, double media, long n) {
        if (n == 0 || media == 0) {
            return 1;
        }
        double crudo = stat.apply(promedios) / media;
        return (n * crudo + PESO_EQUIPO) / (n + PESO_EQUIPO);
    }

    /**
     * RN-PRE-05: factor = [(n × tarjetasPorPartido + 10 × mediaLiga) / (n + 10)] / mediaLiga. Vale 1 si el partido no tiene
     * árbitro (LaLiga y la Bundesliga) o si el árbitro aún no tiene partidos en los datos.
     */
    static double factorArbitro(PerfilArbitroDTO arbitro) {
        if (arbitro == null || arbitro.partidos() == 0 || arbitro.mediaLigaTarjetas() == null || arbitro.mediaLigaTarjetas() == 0) {
            return 1;
        }
        double media = arbitro.mediaLigaTarjetas();
        long n = arbitro.partidos();
        return ((n * arbitro.tarjetasPorPartido() + PESO_ARBITRO * media) / (n + PESO_ARBITRO)) / media;
    }

    // Todos los mercados y líneas de RN-09, en el orden de la web: 1X2, GOLES, AMBOS_MARCAN, TARJETAS y CORNERS.
    private static List<MercadoDTO> mercados(Esperado goles, double corners, double tarjetas) {
        List<MercadoDTO> mercados = new ArrayList<>();
        double[][] matriz = PoissonService.matriz(goles.local(), goles.visitante());
        double[] unoXDos = PoissonService.unoXDos(matriz);
        mercados.add(MercadoDTO.de("1X2", "LOCAL", null, unoXDos[0]));
        mercados.add(MercadoDTO.de("1X2", "EMPATE", null, unoXDos[1]));
        mercados.add(MercadoDTO.de("1X2", "VISITA", null, unoXDos[2]));
        LINEAS_GOLES.forEach(linea -> masMenos(mercados, "GOLES", linea, PoissonService.masDe(matriz, linea)));
        double ambos = PoissonService.ambosMarcan(goles.local(), goles.visitante());
        mercados.add(MercadoDTO.de("AMBOS_MARCAN", "SI", null, ambos));
        mercados.add(MercadoDTO.de("AMBOS_MARCAN", "NO", null, 1 - ambos));
        LINEAS_TARJETAS.forEach(linea -> masMenos(mercados, "TARJETAS", linea, PoissonService.masDeTotal(tarjetas, linea)));
        LINEAS_CORNERS.forEach(linea -> masMenos(mercados, "CORNERS", linea, PoissonService.masDeTotal(corners, linea)));
        return mercados;
    }

    // MAS y MENOS de una línea: MENOS = 1 − MAS.
    private static void masMenos(List<MercadoDTO> mercados, String mercado, double linea, double mas) {
        mercados.add(MercadoDTO.de(mercado, "MAS", linea, mas));
        mercados.add(MercadoDTO.de(mercado, "MENOS", linea, 1 - mas));
    }

    // RN-15: frases que dicen de dónde salen los números y qué no sabe el modelo.
    private static List<String> explicacion(PartidoDTO partido, PromediosLigaDTO liga, PromediosEquipoDTO local,
            PromediosEquipoDTO visitante, PerfilArbitroDTO arbitro, double factorArbitro) {
        List<String> frases = new ArrayList<>();
        frases.add(frase(partido.local().nombre(), "en casa", local, liga.local()));
        frases.add(frase(partido.visitante().nombre(), "fuera", visitante, liga.visitante()));
        if (partido.arbitro() == null) {
            frases.add("El partido no tiene árbitro asignado, así que el factor árbitro vale 1.");
        } else if (arbitro.partidos() == 0) {
            frases.add(partido.arbitro().nombre() + " aún no tiene partidos dirigidos en los datos, así que el factor árbitro vale 1.");
        } else {
            frases.add(texto("%s saca %.1f tarjetas por partido en %d partidos (media de la liga: %.1f), así que el factor árbitro es %.2f.",
                    partido.arbitro().nombre(), arbitro.tarjetasPorPartido(), arbitro.partidos(), arbitro.mediaLigaTarjetas(), factorArbitro));
        }
        // Con menos partidos que el peso del suavizado, pesa más la media de la liga que los datos del equipo.
        if (local.partidos() < PESO_EQUIPO) {
            frases.add(muestraCorta(partido.local().nombre(), local.partidos(), "en casa"));
        }
        if (visitante.partidos() < PESO_EQUIPO) {
            frases.add(muestraCorta(partido.visitante().nombre(), visitante.partidos(), "fuera"));
        }
        frases.add("Es un modelo de Poisson didáctico: no conoce alineaciones, lesiones ni noticias, y las casas de apuestas sí. "
                + "Sirve para aprender y comparar, no para ganarles.");
        return frases;
    }

    // "Arsenal marca 2.1 y recibe 0.8 goles por partido en casa, en sus últimos 10 (media de la liga en casa: 1.55)."
    private static String frase(String equipo, String donde, PromediosEquipoDTO e, PromediosDTO mediaLiga) {
        if (e.partidos() == 0) {
            return equipo + " aún no tiene partidos " + donde + " en los datos: se usa la media de la liga.";
        }
        return texto("%s marca %.1f y recibe %.1f goles por partido %s, en sus últimos %d (media de la liga %s: %.2f).",
                equipo, e.aFavor().goles(), e.enContra().goles(), donde, e.partidos(), donde, mediaLiga.goles());
    }

    private static String muestraCorta(String equipo, long partidos, String donde) {
        return texto("%s solo tiene %d partidos %s de muestra: sus factores se acercan a la media de la liga.", equipo, partidos, donde);
    }

    // Números con punto decimal, como en el resto de la API.
    private static String texto(String formato, Object... valores) {
        return String.format(Locale.ROOT, formato, valores);
    }

    // RN-PRE-07: 404 si el partido no existe y 503 si ms-catalogo no responde.
    private PartidoDTO partido(Long partidoId) {
        try {
            return catalogo.partido(partidoId);
        } catch (FeignException.NotFound e) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PARTIDO_NO_ENCONTRADO", "No existe el partido " + partidoId + ".");
        } catch (FeignException e) {
            throw noResponde("ms-catalogo");
        }
    }

    // RN-PRE-07: cualquier fallo de un servicio se responde con 503, nunca con una probabilidad inventada.
    private static <T> T llamar(String servicio, Supplier<T> llamada) {
        try {
            return llamada.get();
        } catch (FeignException e) {
            throw noResponde(servicio);
        }
    }

    private static ApiException noResponde(String servicio) {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SERVICIO_NO_DISPONIBLE", servicio + " no responde.");
    }

    private static double redondear(double valor) {
        return Math.round(valor * 100) / 100.0;
    }
}
