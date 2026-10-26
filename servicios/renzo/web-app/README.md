# web-app · Integrante 3 · puerto 8090 (cliente, no cuenta como microservicio)

**Responsabilidad:** la interfaz para el usuario. Solo habla con el **api-gateway** (`http://localhost:8080`, o `http://api-gateway:8080` en Docker), nunca directamente con los microservicios.

## Spring Initializr

- Dependencias: **Spring Web**, **Thymeleaf**, **Validation** y **Actuator** (Spring Boot 4.1.2). Bootstrap 5 desde CDN como base, y encima la hoja propia `static/css/cuaderno.css` con el diseño «Cuaderno».
- El cliente HTTP es `RestClient` (incluido en Spring Web). La URL base sale de la variable `GATEWAY_URL`.

## Sesión

Tras el login, guarda el JWT en la `HttpSession` del servidor (no en `localStorage`) y lo añade como `Authorization: Bearer` en cada llamada. Si el gateway responde 401, borra la sesión y redirige a `/login`.

## Pantallas

| Ruta | Pantalla | Llama a |
|---|---|---|
| `/login`, `/registro` | Inicio de sesión y registro | `/api/usuarios/auth/login` y `/api/usuarios/auth/registro` |
| `/` | **Hoy**: la apuesta del día y los partidos del día, filtrables por liga | `/api/apuestas/apuesta-del-dia` y `/api/catalogo/partidos?fecha=`. Por cada partido, además, `/api/predicciones/partidos/{id}` y `/api/cuotas/partidos/{id}/cuotas` (en paralelo) para mostrar el 1X2 del modelo y de bet365 |
| `/partidos/{id}` | **Análisis**: promedios, árbitro, probabilidades y cuotas lado a lado (cuota justa frente a cuota de la casa, en verde si tiene valor), más un formulario "evaluar cuota" | `/api/catalogo/partidos/{id}`, `/api/predicciones/partidos/{id}`, `/api/cuotas/partidos/{id}/cuotas` y `/api/apuestas/evaluaciones` |
| `/partidos/{id}/cuota-betano` | Formulario para ingresar una cuota de Betano | `/api/cuotas/cuotas/manual` |
| `/mis-apuestas` | Registrar una apuesta, ver la lista y el rendimiento | `/api/apuestas/mis-apuestas` y `/api/apuestas/mis-apuestas/rendimiento` |
| `/historial` | Historial de la apuesta del día | `/api/apuestas/picks/historial` |
| `/perfil` | Banca | `/api/usuarios/me` y `/api/usuarios/me/banca` |
| `/admin/ingesta` (solo ADMIN) | Botones HISTORICO y PROXIMOS, y la tabla de ejecuciones | `/api/ingesta/ejecutar` y `/api/ingesta/ejecuciones` |

- Todas las horas se muestran en **hora de Lima**.
- Si una sección falla (por ejemplo, las cuotas devuelven 503), la página se muestra igual con el aviso "Cuotas no disponibles por ahora" (CN-08).
- La interfaz debe ser responsive (Bootstrap), porque se usará desde el celular.

## Reglas de negocio

Reglas específicas de este servicio, con el mismo código que en la [sección 4.5 de la documentación](../../docs/Documentacion.md#45-reglas-de-negocio). Si cambian, se actualizan los dos documentos y se avisa en el daily.

| ID | Regla | Relación |
|---|---|---|
| RN-WEB-01 | Solo habla con el `api-gateway`, nunca directamente con un microservicio. | RNF-01 |
| RN-WEB-02 | El token se guarda en la sesión del servidor, no en el navegador. Si el gateway responde 401, se borra la sesión y se vuelve a `/login`. | RNF-03 |
| RN-WEB-03 | Todas las horas se muestran en hora de Lima. | RN-12 |
| RN-WEB-04 | Si una sección falla, la página se muestra igual, con un aviso como «Cuotas no disponibles por ahora», y nunca con una pantalla de error. | RNF-04 · CN-08 |
| RN-WEB-05 | Una cuota se pinta de verde cuando supera a la cuota justa, es decir, cuando su valor esperado es positivo. | RN-03 |
| RN-WEB-06 | La pantalla de ingesta solo aparece para el ADMIN. | RN-13 |
| RN-WEB-07 | El pie de cada página dice: «Solo para mayores de 18 años. Juega con responsabilidad.» | RN-16 |

Reglas generales que aplica: RN-12 (fechas), RN-15 (honestidad) y RN-16 (juego responsable).

## Tareas

- [x] Login, registro y sesión.
- [x] Hoy y Análisis (prioridad máxima para la demo).
- [x] Mis apuestas, Historial y Perfil.
- [x] Admin de la ingesta.
- [x] Pie de página con el aviso «Solo para mayores de 18 años. Juega con responsabilidad.» (RN-WEB-07).
- [x] Dockerfile.
- [x] Probarla contra el gateway real cuando estén los servicios (25/10/2026: pasan las pruebas CN-01 a CN-09).

## Cómo está armada

| Paquete o carpeta | Qué tiene |
|---|---|
| `config` | `GatewayConfig`: el `RestClient` hacia el gateway (token, tiempos de espera y traducción de errores). `WebConfig`: registra el interceptor de sesión. |
| `sesion` | `SesionUsuario` (JWT, usuario, rol y banca en la `HttpSession`) y `SesionInterceptor` (sin sesión va a `/login`; `/admin/**` solo para el ADMIN). |
| `client` | Un cliente por microservicio: `UsuariosClient`, `CatalogoClient`, `PrediccionesClient`, `CuotasClient`, `ApuestasClient` e `IngestaClient`. |
| `dto` | Records con el JSON de cada contrato. |
| `service` | `HoyService` y `AnalisisService` arman esas pantallas; `EjecutorParalelo` hace varias llamadas a la vez. |
| `controller` | Un controlador por pantalla de la tabla de arriba. |
| `form` | Formularios con sus validaciones. Las cuotas y montos aceptan coma o punto decimal. |
| `vista` | Formatos para las plantillas (`@fmt`, horas de Lima), catálogo de mercados, matriz de Poisson y gráfico del historial. |
| `exception` | Errores del gateway: 401 cierra la sesión, 5xx o sin conexión se muestra como aviso y 4xx llega al formulario. |
| `templates/`, `static/` | Plantillas Thymeleaf, `cuaderno.css` y `transiciones.js` (animaciones entre páginas con View Transitions). |
| `simulador/` | Gateway simulado en Node para probar la web sin los microservicios. |
| `prototipos/` | Las 3 propuestas de diseño en HTML estático (se eligió la C, «Cuaderno»). |

## Probarla sin los microservicios

```bash
node servicios/web-app/simulador/gateway-simulado.mjs     # gateway simulado en el puerto 8080
cd servicios/web-app && ./mvnw spring-boot:run            # web en http://localhost:8090
```

Cualquier usuario entra con una clave de 6 caracteres o más, y `admin` entra con rol ADMIN. El partido Liverpool – Brighton simula que `ms-cuotas` no responde.

**Confirmado con `ms-apuestas`:** el contrato no muestra el JSON de cada apuesta en `GET /mis-apuestas`, pero `ms-apuestas` responde exactamente lo que la web espera: `id`, `partidoId`, `partido`, `mercado`, `seleccion`, `linea`, `casa`, `cuota`, `monto`, `fecha`, `resultado` y `ganancia` (`resultado` y `ganancia` en `null` mientras está pendiente), siguiendo la tabla `apuesta_usuario`.
