package pe.edu.unu.apuestas.gateway.seguridad;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriUtils;

import io.jsonwebtoken.Claims;
import pe.edu.unu.apuestas.gateway.error.RespuestaError;
import reactor.core.publisher.Mono;

/**
 * Filtro global que corre antes de reenviar cualquier petición a un microservicio.
 * Aplica, en orden, las reglas de seguridad del contrato (RN-GAT-01 a RN-GAT-04).
 */
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    public static final String CABECERA_ID = "X-Usuario-Id";
    public static final String CABECERA_ROL = "X-Usuario-Rol";

    private final ValidadorJwt validador;

    public JwtAuthFilter(ValidadorJwt validador) {
        this.validador = validador;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest peticion = exchange.getRequest();
        String ruta = peticion.getPath().pathWithinApplication().value();

        // Regla 1 (RN-GAT-03): las rutas /interno/ solo existen dentro de la red de Docker. Se mira la ruta
        // decodificada y en minúsculas para que "/%69nterno/" o "/Interno/" tampoco pasen.
        String rutaNormalizada = UriUtils.decode(ruta, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        if (rutaNormalizada.contains("/interno/") || rutaNormalizada.endsWith("/interno")) {
            return RespuestaError.escribir(exchange, HttpStatus.FORBIDDEN, "RUTA_INTERNA",
                    "Las rutas internas solo se usan dentro de la red de los servicios.");
        }

        // Regla 5 (RN-GAT-04), primera parte: se borran las cabeceras de usuario que mande el cliente.
        ServerHttpRequest.Builder sinCabecerasFalsas = peticion.mutate().headers(h -> {
            h.remove(CABECERA_ID);
            h.remove(CABECERA_ROL);
        });

        // Regla 2 (RN-GAT-01): las rutas públicas pasan sin token.
        if (RutasPublicas.esPublica(peticion.getMethod(), ruta)) {
            return chain.filter(exchange.mutate().request(sinCabecerasFalsas.build()).build());
        }

        // Regla 3: el resto exige "Authorization: Bearer <token>" bien firmado y vigente.
        String autorizacion = peticion.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (autorizacion == null || !autorizacion.startsWith("Bearer ")) {
            return RespuestaError.escribir(exchange, HttpStatus.UNAUTHORIZED, "NO_AUTORIZADO", "Falta el token. Inicia sesión.");
        }
        Claims datos;
        try {
            datos = validador.validar(autorizacion.substring("Bearer ".length()).trim());
        } catch (TokenInvalidoException e) {
            return RespuestaError.escribir(exchange, HttpStatus.UNAUTHORIZED, "NO_AUTORIZADO", e.getMessage());
        }

        // Regla 4 (RN-GAT-02): la ingesta es solo para el ADMIN.
        String rol = datos.get("rol", String.class);
        if (ruta.startsWith("/api/ingesta") && !"ADMIN".equals(rol)) {
            return RespuestaError.escribir(exchange, HttpStatus.FORBIDDEN, "SIN_PERMISO", "Solo el administrador puede usar la ingesta.");
        }

        // Regla 5, segunda parte: el servicio recibe quién es el usuario según el token.
        ServerHttpRequest conUsuario = sinCabecerasFalsas.headers(h -> {
            h.set(CABECERA_ID, datos.getSubject());
            h.set(CABECERA_ROL, rol);
        }).build();
        return chain.filter(exchange.mutate().request(conUsuario).build());
    }

    // Orden negativo: corre antes que los filtros que reenvían la petición.
    @Override
    public int getOrder() {
        return -100;
    }
}
