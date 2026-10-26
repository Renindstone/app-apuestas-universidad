# ms-catalogo · Guillermo (Integrante 1) · puerto 8082 · BD `catalogo_db`

**Responsabilidad:** ligas, equipos, árbitros y partidos. **Es el dueño de los IDs:** el resto de servicios guarda `partidoId`, `equipoId`, `ligaId` y `arbitroId` tal como los entrega este servicio. **Debe estar listo antes que los demás**, así que el día 1 tengan al menos el mock de `GET /partidos`.

## Spring Initializr

- Dependencias: **Spring Web**, **Spring Data JPA**, **PostgreSQL Driver**, **Validation**, **Eureka Discovery Client** y **Actuator**.
- Además, `springdoc-openapi-starter-webmvc-ui`.

## Datos iniciales

Al arrancar, si la tabla `liga` está vacía, insertar las 3 ligas:

| Código | Nombre | País |
|---|---|---|
| E0 | Premier League | Inglaterra |
| SP1 | LaLiga | España |
| D1 | Bundesliga | Alemania |

Los equipos y árbitros se crean solos con el upsert interno.

## DTO `PartidoDTO` (lo usan todos)

```json
{
  "id": 1532,
  "liga": { "id": 1, "codigo": "E0", "nombre": "Premier League" },
  "temporada": "2627",
  "fechaHora": "2026-10-24T14:00:00Z",
  "local": { "id": 11, "nombre": "Arsenal" },
  "visitante": { "id": 17, "nombre": "Chelsea" },
  "arbitro": { "id": 4, "nombre": "M Oliver" },
  "golesLocal": null,
  "golesVisitante": null,
  "estado": "PROGRAMADO"
}
```

`arbitro` es `null` cuando no se conoce (LaLiga y Bundesliga, o un partido futuro).

## Endpoints públicos (vía gateway: `/api/catalogo` + ruta)

| Método | Ruta | Respuesta |
|---|---|---|
| GET | `/ligas` | `[{ "id": 1, "codigo": "E0", "nombre": "Premier League", "pais": "Inglaterra" }]` |
| GET | `/equipos?ligaId=1` | `[{ "id": 11, "nombre": "Arsenal", "ligaId": 1 }]` |
| GET | `/partidos?fecha=2026-10-24&ligaId=1&estado=PROGRAMADO` | `[PartidoDTO]`, ordenados por `fechaHora`. Todos los filtros son opcionales. `fecha` es un día **de Lima**: hay que convertirlo al rango UTC de 05:00 a 05:00. |
| GET | `/partidos?desde=2026-10-24&hasta=2026-10-31` | `[PartidoDTO]` |
| GET | `/partidos/{id}` | `PartidoDTO`, o 404 |
| GET | `/equipos/{id}/partidos?estado=FINALIZADO&limite=10` | Últimos partidos del equipo |

## Endpoint interno (solo lo usa `ms-ingesta`)

### `POST /interno/partidos` (upsert)

Si el cruce (local, visitante, temporada) ya existe, **lo actualiza**. Si no, lo crea. También crea el equipo o el árbitro si no existen.

```json
{
  "ligaCodigo": "E0", "temporada": "2627", "fechaHora": "2026-10-24T14:00:00Z",
  "local": "Arsenal", "visitante": "Chelsea", "arbitro": "M Oliver",
  "golesLocal": 2, "golesVisitante": 1
}
```

- **200:** `PartidoDTO`. Si `golesLocal` llega con valor, el estado pasa a `FINALIZADO`.
- Un partido FINALIZADO **no** vuelve a PROGRAMADO, aunque llegue después una fila de `fixtures.csv` sin goles.

## Reglas de negocio

Reglas específicas de este servicio, con el mismo código que en la [sección 4.5 de la documentación](../../../docs/Documentacion.md#45-reglas-de-negocio). Si cambian, se actualizan los dos documentos y se avisa en el daily.

| ID | Regla | Relación |
|---|---|---|
| RN-CAT-01 | Solo existen tres ligas: E0, SP1 y D1. Se insertan al arrancar si la tabla está vacía. | RN-10 |
| RN-CAT-02 | Un cruce local–visitante ocurre una sola vez por temporada. Por eso el alta de partidos es un *upsert*: si el cruce ya existe, se actualiza. Enviar la misma fila dos veces no duplica nada. | RF-05 |
| RN-CAT-03 | Los equipos y los árbitros se crean solos la primera vez que llegan en una fila. Sus nombres son únicos. | RF-05 |
| RN-CAT-04 | Un partido nace PROGRAMADO y pasa a FINALIZADO cuando llegan sus goles. Un partido FINALIZADO nunca vuelve a PROGRAMADO, aunque después llegue una fila sin goles. | RF-05 · RF-14 |
| RN-CAT-05 | El árbitro queda vacío cuando no se conoce: siempre en LaLiga y la Bundesliga, y en los partidos futuros. | RN-08 |
| RN-CAT-06 | La fecha de los filtros es un día de Lima: va de las 05:00 UTC de ese día a las 05:00 UTC del día siguiente. | RN-12 |

Reglas generales que aplica: RN-10 (ligas y temporadas), RN-11 (dueño de los IDs) y RN-12 (fechas).

## Tareas

- [x] Entidades `Liga`, `Equipo`, `Arbitro` y `Partido` con sus restricciones UNIQUE (sección 6.2).
- [x] Mock de `GET /partidos` el día 1: ya no hace falta, el endpoint real está listo (la web usaba el simulador).
- [x] Endpoints públicos con filtros (`Specification` en `repository/PartidoSpecs`).
- [x] Upsert interno (idempotente: enviar la misma fila 2 veces no duplica nada), en `service/UpsertPartidoService`.
- [x] Pruebas: el upsert es idempotente y el filtro por fecha usa la hora de Lima, más filtros, últimos partidos de un equipo y errores (`CatalogoTest`, con H2 en memoria).
- [x] Dockerfile.

**Nombres de columnas:** igual que en ms-usuarios, PostgreSQL los guarda tal cual (`"ParIdPartido"`). En DBeaver van entre comillas: `SELECT "ParFechaHora" FROM partido;`.

**Variables de entorno:** `DB_HOST`, `DB_PORT`, `POSTGRES_USER`, `POSTGRES_PASSWORD` y `EUREKA_URL`.

## Pistas

- Filtros opcionales: un `@Query` con `(:ligaId IS NULL OR p.liga.id = :ligaId)`, o `Specification` de JPA.
- Fecha de Lima a rango UTC: `fecha.atStartOfDay(ZoneId.of("America/Lima")).toInstant()`, y lo mismo con `fecha.plusDays(1)`.
