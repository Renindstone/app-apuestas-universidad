# Guía paso a paso: api-gateway

El gateway es la única puerta de entrada: recibe `/api/...`, revisa el JWT y reenvía la petición al microservicio correcto. Es **reactivo** (Spring Cloud Gateway sobre WebFlux), así que aquí no hay `@RestController`: se trabaja con filtros que devuelven `Mono<Void>`.

- Contrato: [README.md](README.md). Las reglas RN-GAT-01 a RN-GAT-05 son la especificación de esta guía.
- Guía general y orden de trabajo: [docs/guia-paso-a-paso.md](../../../docs/guia-paso-a-paso.md).
- Todos los archivos de esta guía van en `src/main/java/pe/edu/unu/apuestas/gateway/`. Para ver cómo quedó uno en la solución:
  `git show universidad/codigo-completo:servicios/renzo/api-gateway/src/main/java/pe/edu/unu/apuestas/gateway/<archivo>`

## Paso 0. Lo que ya trae el esqueleto

- **`pom.xml`:** `spring-cloud-starter-gateway-server-webflux` (el gateway reactivo), `spring-cloud-starter-netflix-eureka-client`, Actuator y las tres librerías `jjwt` para leer el token.
- **`application.yml`:**
  - `connect-timeout: 2000` y `response-timeout: 10s`: si un servicio no responde, el gateway corta en vez de quedarse esperando.
  - `registry-fetch-interval-seconds: 5`: pide la lista de servicios a Eureka cada 5 s.
  - `jwt.secreto`: la variable `JWT_SECRET`, la misma de `ms-usuarios`.
  - `web.url`: la única web que puede llamar desde un navegador (CORS).

Arranca `discovery-server` y luego el gateway con `./mvnw spring-boot:run`. Arranca en el puerto 8080, pero todavía responde 404 a todo porque no tiene rutas.

## Paso 1. Las rutas

**Archivo:** `config/RutasConfig.java`

Una clase `@Configuration` con un `@Bean` que devuelve un `RouteLocator`. Para cada uno de los 7 servicios de negocio:

- `path("/api/catalogo/**")`: qué peticiones atrapa la ruta.
- `filters(f -> f.stripPrefix(2))`: quita los 2 primeros tramos, así `/api/catalogo/partidos` llega a `ms-catalogo` como `/partidos`.
- `uri("lb://ms-catalogo")`: `lb://` significa «pregúntale a Eureka dónde está `ms-catalogo` y reparte la carga entre sus instancias».
- Dale a cada ruta un id igual al nombre del servicio. En el paso 7 lo vas a usar para decir qué servicio se cayó.

**Pista:** guarda los pares prefijo → servicio en un `Map` y recórrelo con `forEach`, en vez de escribir 7 rutas a mano.

**Comprueba:** con `discovery-server` y el esqueleto de `ms-catalogo` encendidos, la petición `curl http://localhost:8080/api/catalogo/actuator/health` debe responder `{"status":"UP"}`. Ese JSON viene de `ms-catalogo`, a través del gateway.

## Paso 2. Un formato de error único

**Archivo:** `error/RespuestaError.java`

Una clase con un método estático que corta la petición y responde el JSON común `{"error": "CODIGO", "detalle": "mensaje"}`:

1. `exchange.getResponse().setStatusCode(estado)`.
2. Content-Type `application/json`.
3. Escribir el cuerpo con `respuesta.writeWith(Mono.just(buffer))`, donde el buffer sale de `respuesta.bufferFactory().wrap(bytes)`.

Escapa las comillas y los saltos de línea del mensaje, o el JSON quedará roto. Los pasos 5 y 7 usan este método.

## Paso 3. Validar el JWT

**Archivos:** `seguridad/TokenInvalidoException.java` y `seguridad/ValidadorJwt.java`

- `ValidadorJwt` es un `@Component` que recibe `@Value("${jwt.secreto}")` en el constructor.
- Si la clave tiene menos de 32 bytes, lanza `IllegalStateException`. Así el gateway **no arranca** con una clave débil, y el error se ve enseguida y no en la primera petición.
- La clave se arma con `Keys.hmacShaKeyFor(secreto.getBytes(UTF_8))`.
- `validar(token)` usa `Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload()` y traduce los errores:
  - `ExpiredJwtException` → `TokenInvalidoException("El token venció...")`.
  - `JwtException` o `IllegalArgumentException` → `TokenInvalidoException("El token no es válido.")`.
  - Un token sin `sub` o sin el claim `rol` también es inválido.

## Paso 4. Las rutas públicas

**Archivo:** `seguridad/RutasPublicas.java`

Un método `esPublica(HttpMethod, String ruta)` que devuelve `true` solo para (RN-GAT-01):

- `POST /api/usuarios/auth/...` (registro y login).
- `GET /api/catalogo` y `GET /api/catalogo/...`.
- `GET /api/<servicio>/v3/api-docs...` (Swagger). Usa un `Pattern`.

`/actuator/health` no pasa por aquí: lo atiende el propio gateway, no una ruta.

## Paso 5. El filtro global

**Archivo:** `seguridad/JwtAuthFilter.java`

Un `@Component` que implementa `GlobalFilter` y `Ordered`. Corre antes de reenviar **cualquier** petición. El orden de las reglas importa:

1. **Ruta interna (RN-GAT-03):** si la ruta contiene `/interno/` (o termina en `/interno`), responde 403 `RUTA_INTERNA`. Antes, decodifica la ruta y pásala a minúsculas, para que `/%69nterno/` o `/Interno/` tampoco pasen.
2. **Borrar cabeceras falsas (RN-GAT-04):** quita `X-Usuario-Id` y `X-Usuario-Rol` de la petición, venga como venga.
3. **Rutas públicas:** si `RutasPublicas.esPublica(...)`, reenvía sin pedir token.
4. **Token:** lee `Authorization: Bearer <token>`. Si falta o el validador lanza `TokenInvalidoException`, responde 401 `NO_AUTORIZADO`.
5. **Rol (RN-GAT-02):** si la ruta empieza con `/api/ingesta` y el rol no es `ADMIN`, responde 403 `SIN_PERMISO`.
6. **Cabeceras reales:** agrega `X-Usuario-Id` (el `sub` del token) y `X-Usuario-Rol`, y reenvía con `chain.filter(...)`.

`getOrder()` devuelve un número negativo (por ejemplo `-100`) para que corra antes que los filtros que reenvían.

**Pistas:**
- Cortar la petición: `return RespuestaError.escribir(exchange, HttpStatus.UNAUTHORIZED, "NO_AUTORIZADO", "...")`.
- Cambiar cabeceras: `exchange.mutate().request(r -> r.headers(h -> { h.remove(...); h.set(...); })).build()`.

**Comprueba** (con `ms-usuarios` listo podrás probar también con un token real):
- `curl -i http://localhost:8080/api/apuestas/mis-apuestas` → 401.
- `curl -i -X POST http://localhost:8080/api/catalogo/interno/partidos` → 403.

## Paso 6. CORS

**Archivo:** `config/CorsConfig.java`

Un `@Bean CorsWebFilter` que permite solo el origen `${web.url}`, con los métodos GET, POST, PUT, DELETE y OPTIONS, y las cabeceras `Authorization` y `Content-Type`. Este filtro corre antes que el del JWT, así las peticiones `OPTIONS` que manda el navegador se responden sin token.

## Paso 7. Errores cuando un servicio está caído

**Archivo:** `error/ManejadorErroresGateway.java`

Un `@Component` con `@Order(-2)` que implementa `ErrorWebExceptionHandler`. Convierte los errores en el JSON común (RNF-04) en vez de un 500 genérico:

| Situación | Respuesta |
|---|---|
| `ConnectException` en la cadena de causas (el servicio no acepta conexiones) | 503 `SERVICIO_NO_DISPONIBLE` |
| `TimeoutException` | 504 `SERVICIO_NO_DISPONIBLE` |
| `ResponseStatusException` 503 (Eureka no tiene instancias) | 503 `SERVICIO_NO_DISPONIBLE` |
| 404 sin ruta | 404 `RUTA_NO_ENCONTRADA` |
| Otro 4xx | el mismo código, `PETICION_INVALIDA` |
| Cualquier otro | 500 `ERROR_INTERNO` |

**Pistas:**
- El nombre del servicio sale del id de la ruta: `exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR)`.
- Netty envuelve la `ConnectException`: recorre `getCause()` hasta encontrarla.
- Si `exchange.getResponse().isCommitted()`, ya no se puede escribir: devuelve `Mono.error(error)`.

**Comprueba:** apaga `ms-catalogo` y llama a `/api/catalogo/ligas`. Debe responder 503 con el JSON.

## Paso 8. Pruebas

**Archivo:** `src/test/java/pe/edu/unu/apuestas/gateway/seguridad/JwtAuthFilterTest.java`

Son pruebas unitarias, sin levantar Spring:

- Crea el filtro a mano: `new JwtAuthFilter(new ValidadorJwt(SECRETO))`.
- Arma la petición con `MockServerHttpRequest.get(...)` y `MockServerWebExchange.from(peticion)`.
- Usa una cadena falsa que guarde lo que le llega: si al final está vacía, el filtro cortó la petición.
- Firma los tokens de prueba con `Jwts.builder()`, igual que lo hace `ms-usuarios`.

Casos mínimos, del plan de pruebas (sección 8):

| Caso | Esperado |
|---|---|
| CN-02: sin token a `/api/apuestas/mis-apuestas` | 401 y no se reenvía |
| CB-09: token vencido | 401 con «venció» |
| Token firmado con otra clave | 401 |
| CN-03: APOSTADOR en `/api/ingesta/...` | 403 `SIN_PERMISO` |
| ADMIN en `/api/ingesta/...` | se reenvía |
| CN-04: `/interno/`, también `/%69nterno/` | 403 |
| Cabeceras `X-Usuario-*` falsas | se reemplazan por las del token |
| Clave de menos de 32 caracteres | `IllegalStateException` |

## Terminado cuando

- [ ] Las 7 rutas llegan a su servicio.
- [ ] Las 5 reglas de seguridad responden lo que dice el contrato.
- [ ] Un servicio apagado da 503 en JSON, no un 500.
- [ ] `./mvnw test` pasa.

Si no alcanza el tiempo, recupera solo este servicio desde la solución (ver la guía general).
