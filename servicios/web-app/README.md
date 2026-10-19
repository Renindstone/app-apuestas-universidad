# web-app · Integrante 3 · puerto 8090 (cliente, no cuenta como microservicio)

**Responsabilidad:** la interfaz para el usuario. Solo habla con el **api-gateway** (`http://localhost:8080`, o `http://api-gateway:8080` en Docker), nunca directamente con los microservicios.

## Spring Initializr

- Dependencias: **Spring Web**, **Thymeleaf**, **Validation** y **Actuator**. Bootstrap 5 desde CDN.
- El cliente HTTP es `RestClient` (incluido en Spring Web). La URL base sale de la variable `GATEWAY_URL`.

## Sesión

Tras el login, guarda el JWT en la `HttpSession` del servidor (no en `localStorage`) y lo añade como `Authorization: Bearer` en cada llamada. Si el gateway responde 401, borra la sesión y redirige a `/login`.

## Pantallas

| Ruta | Pantalla | Llama a |
|---|---|---|
| `/login`, `/registro` | Inicio de sesión y registro | `/api/usuarios/auth/login` y `/api/usuarios/auth/registro` |
| `/` | **Hoy**: la apuesta del día y los partidos del día, filtrables por liga | `/api/apuestas/apuesta-del-dia` y `/api/catalogo/partidos?fecha=` |
| `/partidos/{id}` | **Análisis**: promedios, árbitro, probabilidades y cuotas lado a lado (cuota justa frente a cuota de la casa, en verde si tiene valor), más un formulario "evaluar cuota" | `/api/predicciones/partidos/{id}`, `/api/cuotas/partidos/{id}/cuotas` y `/api/apuestas/evaluaciones` |
| `/partidos/{id}/cuota-betano` | Formulario para ingresar una cuota de Betano | `/api/cuotas/cuotas/manual` |
| `/mis-apuestas` | Registrar una apuesta, ver la lista y el rendimiento | `/api/apuestas/mis-apuestas` y `/api/apuestas/mis-apuestas/rendimiento` |
| `/historial` | Historial de la apuesta del día | `/api/apuestas/picks/historial` |
| `/perfil` | Banca | `/api/usuarios/me` y `/api/usuarios/me/banca` |
| `/admin/ingesta` (solo ADMIN) | Botones HISTORICO y PROXIMOS, y la tabla de ejecuciones | `/api/ingesta/ejecutar` y `/api/ingesta/ejecuciones` |

- Todas las horas se muestran en **hora de Lima**.
- Si una sección falla (por ejemplo, las cuotas devuelven 503), la página se muestra igual con el aviso "Cuotas no disponibles por ahora" (CN-08).
- La interfaz debe ser responsive (Bootstrap), porque se usará desde el celular.

## Tareas

- [ ] Login, registro y sesión.
- [ ] Hoy y Análisis (prioridad máxima para la demo).
- [ ] Mis apuestas, Historial y Perfil.
- [ ] Admin de la ingesta.
- [ ] Dockerfile.
