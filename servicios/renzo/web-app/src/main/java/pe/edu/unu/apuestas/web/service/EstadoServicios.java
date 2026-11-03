package pe.edu.unu.apuestas.web.service;

import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import pe.edu.unu.apuestas.web.dto.EstadoServicioDTO;

/**
 * Qué servicios están encendidos, para avisarlo en el pie de todas las páginas (${@estadoServicios}).
 * Le pregunta al gateway (GET /estado), que mira Eureka. La web nunca habla directo con Eureka ni con un servicio (RN-WEB-01).
 * La respuesta se guarda 3 segundos para no hacer una llamada extra por cada página.
 */
@Component("estadoServicios")
public class EstadoServicios {

    /** Lo que se muestra de un servicio: su nombre, cuántas instancias tiene y, si está apagado, qué se pierde. */
    public record Linea(String servicio, int instancias, String consecuencia) {

        public boolean encendido() {
            return instancias > 0;
        }
    }

    // Qué deja de funcionar cuando un servicio está apagado, dicho en palabras sencillas.
    private static final Map<String, String> CONSECUENCIAS = Map.of(
            "ms-usuarios", "no se puede iniciar sesión ni crear cuentas",
            "ms-catalogo", "no se ven los partidos",
            "ms-ingesta", "no se pueden cargar datos nuevos",
            "ms-estadisticas", "no hay promedios de goles, tarjetas ni córners",
            "ms-cuotas", "no se ven las cuotas de las casas",
            "ms-predicciones", "no se ven las probabilidades del modelo",
            "ms-apuestas", "no se pueden registrar ni liquidar apuestas");

    private static final long VIGENCIA_MS = 3000;

    private final RestClient gateway;

    // El resultado de la última consulta. null en la lista = el gateway no respondió.
    private volatile List<Linea> ultimas;
    private volatile long consultadoEn;

    public EstadoServicios(RestClient gateway) {
        this.gateway = gateway;
    }

    // Una línea por servicio, en el orden del gateway. Lista vacía si el gateway no responde: mira gatewayCaido().
    public List<Linea> lineas() {
        refrescar();
        List<Linea> lineas = ultimas;
        return lineas == null ? List.of() : lineas;
    }

    // Los servicios apagados (0 instancias), para mostrarlos como avisos.
    public List<Linea> apagados() {
        return lineas().stream().filter(l -> !l.encendido()).toList();
    }

    // true si ni siquiera el gateway respondió: entonces no se sabe nada de los demás.
    public boolean gatewayCaido() {
        refrescar();
        return ultimas == null;
    }

    private synchronized void refrescar() {
        long ahora = System.currentTimeMillis();
        if (consultadoEn != 0 && ahora - consultadoEn < VIGENCIA_MS) {
            return;
        }
        try {
            List<EstadoServicioDTO> estado = gateway.get().uri("/estado").retrieve()
                    .body(new ParameterizedTypeReference<List<EstadoServicioDTO>>() {
                    });
            ultimas = estado == null ? null : estado.stream()
                    .map(e -> new Linea(e.servicio(), e.instancias(), CONSECUENCIAS.getOrDefault(e.servicio(), "una parte de la app")))
                    .toList();
        } catch (RuntimeException e) {
            // El gateway está apagado o dio error: se muestra el aviso del gateway y la web sigue funcionando.
            ultimas = null;
        }
        consultadoEn = ahora;
    }
}
