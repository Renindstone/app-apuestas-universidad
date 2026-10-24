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

## Reglas de negocio

Reglas específicas de este servicio, con el mismo código que en la [sección 4.5 de la documentación](../../docs/Documentacion.md#45-reglas-de-negocio). Si cambian, se actualizan los dos documentos y se avisa en el daily.

| ID | Regla | Relación |
|---|---|---|
| RN-GAT-01 | Solo son públicas (sin token) estas rutas: `POST /api/usuarios/auth/**`, los `GET` de `/api/catalogo/**`, `/actuator/health` y Swagger. Todo lo demás exige un token bien firmado y vigente. Si falta, responde 401. | RNF-03 · CB-09 · CN-02 |
| RN-GAT-02 | `/api/ingesta/**` exige además el rol ADMIN. Si no lo tiene, responde 403. | RN-13 · CN-03 |
| RN-GAT-03 | Ninguna ruta `/interno/**` se atiende desde fuera: responde 403 sin reenviar la petición. | RNF-03 · CN-04 |
| RN-GAT-04 | Las cabeceras `X-Usuario-Id` y `X-Usuario-Rol` que mande el cliente se borran y se reemplazan por las del token, para que nadie pueda suplantar a otro usuario. | RN-14 |
| RN-GAT-05 | Solo la web (`http://localhost:8090`) puede llamar a la API desde un navegador (CORS). | RNF-03 |

Reglas generales que aplica: RN-13 (roles) y RN-14 (privacidad).

## Tareas

- [x] Rutas hacia los 7 servicios (`config/RutasConfig`).
- [x] `JwtAuthFilter` con las 6 reglas (`seguridad/`). CORS en `config/CorsConfig`.
- [x] Respuesta de error en JSON `{"error":"NO_AUTORIZADO","detalle":"..."}`, también cuando un servicio está caído (`error/ManejadorErroresGateway`: 503 `SERVICIO_NO_DISPONIBLE`).
- [x] Pruebas: CB-09 (token vencido), CN-02, CN-03 y CN-04 (`JwtAuthFilterTest`).
- [x] Dockerfile.

**Variables de entorno:** `JWT_SECRET` (la misma de `ms-usuarios`, 32 caracteres o más; si es más corta, el gateway no arranca), `EUREKA_URL` y `WEB_URL` (por defecto `http://localhost:8090`). En local, sin `.env`, `JWT_SECRET` toma un valor de desarrollo que `ms-usuarios` también debe usar.

## Pistas

- En el gateway reactivo, el filtro devuelve `Mono<Void>`. Para cortar la petición: `exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED); return exchange.getResponse().setComplete();`.
- Para modificar cabeceras: `exchange.mutate().request(r -> r.headers(h -> { h.remove("X-Usuario-Id"); h.add("X-Usuario-Id", id); })).build()`.
- Den al filtro un orden alto con `getOrder()` negativo para que corra antes del enrutado.
