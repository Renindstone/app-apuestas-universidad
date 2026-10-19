# App Apuestas: versión con microservicios (proyecto universitario)

Rama `universidad/microservicios`. Es un replanteamiento **simplificado** del proyecto de la rama `main` como **9 microservicios** con Spring Boot y Spring Cloud. Lo construye un equipo de 3 personas en una semana, del martes 20 al lunes 26 de octubre de 2026.

> La rama `main` no se toca: allí sigue el sistema completo en Python. Esta rama arranca vacía y el código lo escribe el equipo. Este repositorio trae la documentación, los contratos de API y la infraestructura mínima para empezar.

## Documentos

| Documento | Para qué |
|---|---|
| [docs/DocumentacionProyectoApuestas.docx](docs/DocumentacionProyectoApuestas.docx) | **Documento para entregar** (Word, formato del proyecto MTC). Los textos resaltados en amarillo se completan a mano. |
| [docs/Documentacion.md](docs/Documentacion.md) | Mismo contenido en Markdown (fuente para regenerar el Word) y documento del proyecto (perfil, análisis, arquitectura, BD, plan de la semana y pruebas), con la estructura del proyecto MTC |
| [docs/guia-inicio.md](docs/guia-inicio.md) | Cómo crear cada servicio con Spring Initializr y cómo levantar todo |
| `servicios/<servicio>/README.md` | **Contrato** del servicio: endpoints, JSON de ejemplo, tareas y pistas |

## Los 9 microservicios

| # | Servicio | Puerto | Base de datos | Dueño |
|---|---|---|---|---|
| 1 | [discovery-server](servicios/discovery-server/README.md) | 8761 | — | Integrante 3 |
| 2 | [api-gateway](servicios/api-gateway/README.md) | 8080 | — | Integrante 3 |
| 3 | [ms-usuarios](servicios/ms-usuarios/README.md) | 8081 | usuarios_db | Integrante 3 |
| 4 | [ms-catalogo](servicios/ms-catalogo/README.md) | 8082 | catalogo_db | Integrante 1 |
| 5 | [ms-ingesta](servicios/ms-ingesta/README.md) | 8083 | ingesta_db | Integrante 1 |
| 6 | [ms-estadisticas](servicios/ms-estadisticas/README.md) | 8084 | estadisticas_db | Integrante 1 |
| 7 | [ms-cuotas](servicios/ms-cuotas/README.md) | 8085 | cuotas_db | Integrante 2 |
| 8 | [ms-predicciones](servicios/ms-predicciones/README.md) | 8086 | — | Integrante 2 |
| 9 | [ms-apuestas](servicios/ms-apuestas/README.md) | 8087 | apuestas_db | Integrante 2 |
| — | [web-app](servicios/web-app/README.md) (cliente, no cuenta como microservicio) | 8090 | — | Integrante 3 |

## Convenciones comunes (todos los servicios)

- **Paquete base:** `pe.edu.unu.apuestas.<servicio>`, por ejemplo `pe.edu.unu.apuestas.catalogo`. Estructura por capas: `controller`, `service`, `repository`, `entity`, `dto`, `client` (Feign) y `exception`.
- **Fechas:** ISO-8601 en UTC (`2026-10-24T14:00:00Z`). La web las muestra en hora de Lima (`America/Lima`).
- **Errores:** siempre JSON `{"error": "CODIGO_CORTO", "detalle": "mensaje para humanos"}` con el código HTTP correcto: 400 validación, 401 sin token, 403 sin permiso, 404 no existe, 409 duplicado y 503 cuando otro servicio no responde. Se resuelven con un `@RestControllerAdvice` en cada servicio.
- **Usuario autenticado:** los servicios **no** validan el JWT. Leen las cabeceras `X-Usuario-Id` y `X-Usuario-Rol` que pone el gateway.
- **Rutas `/interno/**`:** solo las usan otros servicios dentro de la red Docker. El gateway las bloquea.
- **Mercados y selecciones.** Son los mismos códigos en todos los servicios:

| Mercado | Selecciones | Línea | Qué cuenta |
|---|---|---|---|
| `1X2` | `LOCAL`, `EMPATE`, `VISITA` | `null` | Resultado final |
| `GOLES` | `MAS`, `MENOS` | 1.5, 2.5 o 3.5 | Goles totales del partido |
| `AMBOS_MARCAN` | `SI`, `NO` | `null` | Los dos equipos anotan |
| `TARJETAS` | `MAS`, `MENOS` | 3.5, 4.5 o 5.5 | Amarillas + rojas de ambos equipos |
| `CORNERS` | `MAS`, `MENOS` | 8.5, 9.5 o 10.5 | Córners totales del partido |

- **Casas de apuestas:** `bet365`, `pinnacle` y `betano` (en minúsculas).
- **Commits:** en español y en presente ("Agrega endpoint de promedios"). Cada integrante trabaja en su rama `feature/<servicio>-<tema>` y abre un PR hacia `universidad/microservicios`, que revisa otro integrante.

## Arranque rápido

```bash
cd infra && docker compose up -d     # PostgreSQL con las 6 bases de datos
```

Luego levanta `discovery-server` y los servicios desde el IDE o con `./mvnw spring-boot:run`. El detalle está en [docs/guia-inicio.md](docs/guia-inicio.md).
