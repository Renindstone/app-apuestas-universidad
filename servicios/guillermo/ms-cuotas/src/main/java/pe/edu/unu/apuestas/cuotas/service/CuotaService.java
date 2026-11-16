package pe.edu.unu.apuestas.cuotas.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import feign.FeignException;
import pe.edu.unu.apuestas.cuotas.client.CatalogoClient;
import pe.edu.unu.apuestas.cuotas.dto.CuotaDTO;
import pe.edu.unu.apuestas.cuotas.dto.CuotaManualRequest;
import pe.edu.unu.apuestas.cuotas.dto.CuotasRequest;
import pe.edu.unu.apuestas.cuotas.dto.GuardadasDTO;
import pe.edu.unu.apuestas.cuotas.dto.ProbabilidadesMercadoDTO;
import pe.edu.unu.apuestas.cuotas.entity.Cuota;
import pe.edu.unu.apuestas.cuotas.entity.TipoCuota;
import pe.edu.unu.apuestas.cuotas.exception.ApiException;
import pe.edu.unu.apuestas.cuotas.exception.CuotaInvalidaException;
import pe.edu.unu.apuestas.cuotas.repository.CuotaRepository;

/**
 * Guarda las cuotas (de la ingesta y las manuales), muestra la más reciente de cada selección y calcula el margen
 * de la casa y las probabilidades sin margen (RN-01 y RN-02).
 */
@Service
public class CuotaService {

    // RN-CUO-02: por la ingesta solo llegan bet365 y Pinnacle; Betano solo se ingresa a mano.
    private static final Set<String> CASAS_INGESTA = Set.of("bet365", "pinnacle");
    private static final String CASA_MANUAL = "betano";
    // RN-CUO-01: las cuotas que ingresa el usuario deben ser mayores que 1.01.
    private static final BigDecimal MINIMO_MANUAL = new BigDecimal("1.01");

    /** Resultado de quitarMargen: el margen de la casa y la probabilidad sin margen de cada cuota, en el mismo orden. */
    public record SinMargen(double margen, List<Double> probabilidades) {
    }

    private final CuotaRepository cuotas;
    private final CatalogoClient catalogo;

    public CuotaService(CuotaRepository cuotas, CatalogoClient catalogo) {
        this.cuotas = cuotas;
        this.catalogo = catalogo;
    }

    /**
     * RN-01 y RN-02: q = 1 / cuota, margen = Σq − 1 y, con el método multiplicativo, p = q / Σq.
     * Lanza CuotaInvalidaException si alguna cuota no es mayor que 1 (CB-04).
     */
    public static SinMargen quitarMargen(List<Double> valores) {
        for (double valor : valores) {
            if (valor <= 1) {
                throw new CuotaInvalidaException("La cuota " + valor + " no es válida: toda cuota es mayor que 1.");
            }
        }
        double suma = valores.stream().mapToDouble(valor -> 1 / valor).sum();
        return new SinMargen(suma - 1, valores.stream().map(valor -> 1 / valor / suma).toList());
    }

    /**
     * POST /interno/cuotas: guarda las cuotas de una casa para un partido. Primero se validan todas, para que una
     * cuota mala no deje la mitad guardada; luego cada una se actualiza si ya existía o se crea (RN-CUO-04).
     */
    @Transactional
    public GuardadasDTO guardar(CuotasRequest datos) {
        if (!CASAS_INGESTA.contains(datos.casa())) {
            throw invalido("Por la ingesta solo llegan cuotas de bet365 y pinnacle. Las de betano se ingresan a mano.");
        }
        if (datos.tipo() == TipoCuota.MANUAL) {
            throw invalido("Las cuotas MANUAL se ingresan con POST /cuotas/manual.");
        }
        for (CuotasRequest.Item item : datos.cuotas()) {
            Mercados.validar(item.mercado(), item.seleccion(), item.linea());
            if (item.valor().compareTo(BigDecimal.ONE) <= 0) {
                throw new CuotaInvalidaException("La cuota " + item.valor() + " no es válida: toda cuota es mayor que 1.");
            }
        }
        datos.cuotas().forEach(item -> upsert(datos.partidoId(), datos.casa(), datos.tipo(), item.mercado(), item.seleccion(),
                item.linea(), item.valor(), null));
        return new GuardadasDTO(datos.cuotas().size());
    }

    /**
     * POST /cuotas/manual: una cuota de Betano cargada por el usuario (RN-CUO-02 y RN-CUO-03). Valida los datos, comprueba
     * en ms-catalogo que el partido existe y la guarda como MANUAL con el ID del usuario. Sin @Transactional: no tiene
     * sentido tener una transacción abierta mientras se espera a ms-catalogo, y el guardado es una sola operación.
     */
    public CuotaDTO guardarManual(CuotaManualRequest datos, Long usuarioId) {
        if (!CASA_MANUAL.equals(datos.casa())) {
            throw invalido("Solo las cuotas de betano se ingresan a mano. Las de bet365 y pinnacle llegan por la ingesta.");
        }
        Mercados.validar(datos.mercado(), datos.seleccion(), datos.linea());
        if (datos.valor().compareTo(MINIMO_MANUAL) <= 0) {
            throw new CuotaInvalidaException("La cuota debe ser mayor que 1.01.");
        }
        comprobarPartido(datos.partidoId());
        return CuotaDTO.de(upsert(datos.partidoId(), CASA_MANUAL, TipoCuota.MANUAL, datos.mercado(), datos.seleccion(),
                datos.linea(), datos.valor(), usuarioId));
    }

    // GET /partidos/{id}/cuotas: la cuota más reciente de cada casa, mercado, selección y línea (RN-CUO-04). casa es opcional.
    @Transactional(readOnly = true)
    public List<CuotaDTO> cuotasDePartido(Long partidoId, String casa) {
        return recientes(partidoId, casa).stream().map(CuotaDTO::de).toList();
    }

    /**
     * GET /partidos/{id}/probabilidades-mercado: el margen de una casa en un mercado y la probabilidad sin margen de cada
     * selección. Solo con el mercado completo: si falta una selección, 409 MERCADO_INCOMPLETO (RN-CUO-05).
     */
    @Transactional(readOnly = true)
    public ProbabilidadesMercadoDTO probabilidadesMercado(Long partidoId, String casa, String mercado, BigDecimal linea) {
        Mercados.validarCasa(casa);
        List<String> selecciones = Mercados.selecciones(mercado, linea);
        Map<String, Double> valores = new LinkedHashMap<>();
        for (Cuota cuota : recientes(partidoId, casa)) {
            if (cuota.getMercado().equals(mercado) && mismaLinea(cuota.getLinea(), linea)) {
                valores.put(cuota.getSeleccion(), cuota.getValor().doubleValue());
            }
        }
        List<String> faltan = selecciones.stream().filter(s -> !valores.containsKey(s)).toList();
        if (!faltan.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "MERCADO_INCOMPLETO",
                    "Faltan las cuotas de " + String.join(", ", faltan) + " de " + casa + " en " + mercado + ".");
        }
        SinMargen resultado = quitarMargen(selecciones.stream().map(valores::get).toList());
        Map<String, Double> probabilidades = new LinkedHashMap<>();
        for (int i = 0; i < selecciones.size(); i++) {
            probabilidades.put(selecciones.get(i), redondear(resultado.probabilidades().get(i)));
        }
        return new ProbabilidadesMercadoDTO(casa, mercado, linea, redondear(resultado.margen()), probabilidades);
    }

    // Si ya existe la misma cuota (partido, casa, tipo, mercado, selección y línea) se actualiza; si no, se crea.
    private Cuota upsert(Long partidoId, String casa, TipoCuota tipo, String mercado, String seleccion, BigDecimal linea,
            BigDecimal valor, Long usuarioId) {
        Cuota cuota = cuotas.findFirstByPartidoIdAndCasaAndTipoAndMercadoAndSeleccionAndLinea(partidoId, casa, tipo, mercado, seleccion, linea)
                .orElseGet(() -> new Cuota(partidoId, casa, tipo, mercado, seleccion, linea));
        cuota.actualizar(valor, usuarioId);
        return cuotas.save(cuota);
    }

    // Una sola cuota por (casa, mercado, selección, línea): la primera que aparece, que es la más reciente.
    private List<Cuota> recientes(Long partidoId, String casa) {
        List<Cuota> todas = casa == null ? cuotas.findByPartidoIdOrderByCapturadaEnDescIdDesc(partidoId)
                : cuotas.findByPartidoIdAndCasaOrderByCapturadaEnDescIdDesc(partidoId, casa);
        Map<String, Cuota> porClave = new LinkedHashMap<>();
        todas.forEach(cuota -> porClave.putIfAbsent(cuota.clave(), cuota));
        return new ArrayList<>(porClave.values());
    }

    // RN-CUO-03: 404 si el partido no existe en ms-catalogo y 503 si ms-catalogo no responde.
    private void comprobarPartido(Long partidoId) {
        try {
            catalogo.partido(partidoId);
        } catch (FeignException.NotFound e) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PARTIDO_NO_ENCONTRADO", "No existe el partido " + partidoId + ".");
        } catch (FeignException e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SERVICIO_NO_DISPONIBLE", "ms-catalogo no responde.");
        }
    }

    private static boolean mismaLinea(BigDecimal a, BigDecimal b) {
        return a == null ? b == null : b != null && a.compareTo(b) == 0;
    }

    private static double redondear(double valor) {
        return Math.round(valor * 1000) / 1000.0;
    }

    private static ApiException invalido(String detalle) {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALIDACION", detalle);
    }
}
