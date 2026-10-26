package pe.edu.unu.apuestas.gateway.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Rutas hacia los 7 microservicios de negocio. Cada una quita los 2 primeros tramos (StripPrefix=2),
 * así /api/catalogo/partidos llega a ms-catalogo como /partidos.
 */
@Configuration
public class RutasConfig {

    // Prefijo público → nombre del servicio en Eureka. El nombre también es el id de la ruta.
    private static final Map<String, String> SERVICIOS = new LinkedHashMap<>();

    static {
        SERVICIOS.put("usuarios", "ms-usuarios");
        SERVICIOS.put("catalogo", "ms-catalogo");
        SERVICIOS.put("ingesta", "ms-ingesta");
        SERVICIOS.put("estadisticas", "ms-estadisticas");
        SERVICIOS.put("cuotas", "ms-cuotas");
        SERVICIOS.put("predicciones", "ms-predicciones");
        SERVICIOS.put("apuestas", "ms-apuestas");
    }

    // lb://ms-catalogo: Eureka da la dirección del servicio y el balanceador elige una instancia.
    @Bean
    public RouteLocator rutas(RouteLocatorBuilder builder) {
        RouteLocatorBuilder.Builder rutas = builder.routes();
        SERVICIOS.forEach((prefijo, servicio) -> rutas.route(servicio, r -> r
                .path("/api/" + prefijo + "/**")
                .filters(f -> f.stripPrefix(2))
                .uri("lb://" + servicio)));
        return rutas.build();
    }
}
