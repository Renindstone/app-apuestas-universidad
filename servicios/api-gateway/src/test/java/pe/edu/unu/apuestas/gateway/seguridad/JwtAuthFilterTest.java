package pe.edu.unu.apuestas.gateway.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;

/**
 * Pruebas del filtro JWT con los casos del plan de pruebas (sección 8): CB-09, CN-02, CN-03 y CN-04,
 * más el reemplazo de las cabeceras de usuario (RN-GAT-04).
 */
class JwtAuthFilterTest {

    private static final String SECRETO = "clave-de-pruebas-del-gateway-de-32-o-mas-caracteres";

    private final JwtAuthFilter filtro = new JwtAuthFilter(new ValidadorJwt(SECRETO));

    // Lo que llega al servicio de destino; queda en null si el filtro cortó la petición.
    private final AtomicReference<ServerWebExchange> reenviada = new AtomicReference<>();
    private final GatewayFilterChain cadena = exchange -> {
        reenviada.set(exchange);
        return Mono.empty();
    };

    // Firma un token igual que ms-usuarios: sub = id, claims rol y usuario, que vence en "vence".
    private static String token(String id, String rol, Instant vence) {
        return Jwts.builder().subject(id).claim("rol", rol).claim("usuario", "prueba")
                .issuedAt(Date.from(vence.minus(8, ChronoUnit.HOURS))).expiration(Date.from(vence))
                .signWith(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8))).compact();
    }

    private static String tokenVigente(String id, String rol) {
        return token(id, rol, Instant.now().plus(1, ChronoUnit.HOURS));
    }

    private MockServerWebExchange ejecutar(MockServerHttpRequest peticion) {
        MockServerWebExchange exchange = MockServerWebExchange.from(peticion);
        filtro.filter(exchange, cadena).block();
        return exchange;
    }

    private static String cuerpo(MockServerWebExchange exchange) {
        return exchange.getResponse().getBodyAsString().block();
    }

    // CN-02: sin token no se puede ver mis apuestas.
    @Test
    void sinTokenResponde401() {
        MockServerWebExchange exchange = ejecutar(MockServerHttpRequest.get("/api/apuestas/mis-apuestas").build());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        assertTrue(cuerpo(exchange).contains("\"error\":\"NO_AUTORIZADO\""));
        assertNull(reenviada.get());
    }

    // CB-09: un token vencido responde 401 y la petición no llega al servicio.
    @Test
    void tokenVencidoResponde401() {
        String vencido = token("2", "APOSTADOR", Instant.now().minus(1, ChronoUnit.MINUTES));
        MockServerWebExchange exchange = ejecutar(MockServerHttpRequest.get("/api/apuestas/mis-apuestas")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + vencido).build());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        assertTrue(cuerpo(exchange).contains("venció"));
        assertNull(reenviada.get());
    }

    // Un token firmado con otra clave tampoco pasa.
    @Test
    void tokenMalFirmadoResponde401() {
        String ajeno = Jwts.builder().subject("2").claim("rol", "ADMIN")
                .signWith(Keys.hmacShaKeyFor("otra-clave-distinta-de-32-o-mas-caracteres!!".getBytes(StandardCharsets.UTF_8))).compact();
        MockServerWebExchange exchange = ejecutar(MockServerHttpRequest.get("/api/usuarios/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ajeno).build());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        assertNull(reenviada.get());
    }

    // CN-03: un APOSTADOR no puede lanzar la ingesta.
    @Test
    void apostadorEnIngestaResponde403() {
        MockServerWebExchange exchange = ejecutar(MockServerHttpRequest.post("/api/ingesta/ejecutar?tipo=HISTORICO")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenVigente("2", "APOSTADOR")).build());
        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        assertTrue(cuerpo(exchange).contains("SIN_PERMISO"));
        assertNull(reenviada.get());
    }

    @Test
    void adminPuedeUsarLaIngesta() {
        ejecutar(MockServerHttpRequest.post("/api/ingesta/ejecutar?tipo=HISTORICO")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenVigente("1", "ADMIN")).build());
        assertNotNull(reenviada.get());
    }

    // CN-04: las rutas internas se bloquean aunque el token sea válido, también escritas con otra codificación.
    @Test
    void rutaInternaResponde403() {
        String admin = "Bearer " + tokenVigente("1", "ADMIN");
        for (String ruta : new String[] { "/api/catalogo/interno/partidos", "/api/catalogo/%69nterno/partidos", "/api/cuotas/Interno/cuotas" }) {
            // URI.create deja la ruta tal como llega por la red, sin volver a codificar el "%".
            MockServerWebExchange exchange = ejecutar(MockServerHttpRequest.method(HttpMethod.POST, URI.create(ruta))
                    .header(HttpHeaders.AUTHORIZATION, admin).build());
            assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode(), ruta);
        }
        assertNull(reenviada.get());
    }

    // RN-GAT-01: los GET del catálogo y el login pasan sin token, pero sin las cabeceras de usuario que mande el cliente.
    @Test
    void rutasPublicasPasanSinTokenYSinCabecerasFalsas() {
        ejecutar(MockServerHttpRequest.get("/api/catalogo/ligas").header(JwtAuthFilter.CABECERA_ID, "1").build());
        assertNotNull(reenviada.get());
        assertNull(reenviada.get().getRequest().getHeaders().getFirst(JwtAuthFilter.CABECERA_ID));

        reenviada.set(null);
        ejecutar(MockServerHttpRequest.post("/api/usuarios/auth/login").build());
        assertNotNull(reenviada.get());
    }

    // El POST al catálogo no es público: solo lo son sus GET.
    @Test
    void postAlCatalogoPideToken() {
        MockServerWebExchange exchange = ejecutar(MockServerHttpRequest.post("/api/catalogo/partidos").build());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    // RN-GAT-04: si el cliente manda X-Usuario-Id: 1 para hacerse pasar por otro, se reemplaza por el del token.
    @Test
    void reemplazaLasCabecerasDeUsuarioPorLasDelToken() {
        ejecutar(MockServerHttpRequest.get("/api/apuestas/mis-apuestas")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenVigente("2", "APOSTADOR"))
                .header(JwtAuthFilter.CABECERA_ID, "1")
                .header(JwtAuthFilter.CABECERA_ROL, "ADMIN").build());
        HttpHeaders cabeceras = reenviada.get().getRequest().getHeaders();
        assertEquals("2", cabeceras.getFirst(JwtAuthFilter.CABECERA_ID));
        assertEquals("APOSTADOR", cabeceras.getFirst(JwtAuthFilter.CABECERA_ROL));
        assertEquals(1, cabeceras.get(JwtAuthFilter.CABECERA_ID).size());
    }

    // Una clave de menos de 32 caracteres no sirve para HS256: el gateway no arranca.
    @Test
    void rechazaUnaClaveCorta() {
        assertThrows(IllegalStateException.class, () -> new ValidadorJwt("corta"));
    }
}
