# api-gateway · Integrante 3 · puerto 8080

**Responsabilidad:** es el único punto de entrada al sistema. Enruta cada petición al servicio que corresponde, valida el JWT, aplica las reglas por rol y bloquea las rutas internas.

## Spring Initializr

- Dependencias: **Gateway** (la variante *reactive/WebFlux*), **Eureka Discovery Client** y **Spring Boot Actuator**.
- Librería JWT: `io.jsonwebtoken:jjwt-api`, `jjwt-impl` y `jjwt-jackson`. Usen la misma versión que `ms-usuarios`.

## Rutas

Todas usan `StripPrefix=2`, así `/api/catalogo/partidos` llega a `ms-catalogo` como `/partidos`.

| Ruta pública | Destino |
|---|---|
| `/api/usuarios/**` | `lb://ms-usuarios` |
| `/api/catalogo/**` | `lb://ms-catalogo` |
| `/api/ingesta/**` | `lb://ms-ingesta` |
| `/api/estadisticas/**` | `lb://ms-estadisticas` |
| `/api/cuotas/**` | `lb://ms-cuotas` |
| `/api/predicciones/**` | `lb://ms-predicciones` |
| `/api/apuestas/**` | `lb://ms-apuestas` |

Swagger de cada servicio a través del gateway (opcional): `/api/<servicio>/v3/api-docs`.

## Reglas de seguridad (filtro global `JwtAuthFilter`, implementa `GlobalFilter`)

1. Si la ruta contiene `/interno/`, responder **403** sin reenviar.
2. **Rutas públicas** (sin token): `POST /api/usuarios/auth/**`, `GET /api/catalogo/**`, y `/actuator/health` y Swagger.
3. Para el resto, leer `Authorization: Bearer <token>`. Si falta, está mal firmado o venció, responder **401**.
4. Para `/api/ingesta/**`, si el rol no es `ADMIN`, responder **403**.
5. **Quitar** las cabeceras `X-Usuario-Id` y `X-Usuario-Rol` que mande el cliente, para que no pueda suplantar a nadie. Luego **añadirlas** con `sub` y `rol` del token.
6. Configurar CORS para `http://localhost:8090` (la web).

Clave: `JWT_SECRET`, la misma variable de entorno que usa `ms-usuarios`. Debe tener al menos 32 caracteres para HS256.

## Tareas

- [ ] Rutas hacia los 7 servicios (probar `GET /api/catalogo/ligas` con el mock).
- [ ] `JwtAuthFilter` con las 6 reglas.
- [ ] Respuesta de error en JSON `{"error":"NO_AUTORIZADO","detalle":"..."}`.
- [ ] Pruebas: CB-09 (token vencido), CN-02, CN-03 y CN-04.
- [ ] Dockerfile.

## Pistas

- En el gateway reactivo, el filtro devuelve `Mono<Void>`. Para cortar la petición: `exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED); return exchange.getResponse().setComplete();`.
- Para modificar cabeceras: `exchange.mutate().request(r -> r.headers(h -> { h.remove("X-Usuario-Id"); h.add("X-Usuario-Id", id); })).build()`.
- Den al filtro un orden alto con `getOrder()` negativo para que corra antes del enrutado.
