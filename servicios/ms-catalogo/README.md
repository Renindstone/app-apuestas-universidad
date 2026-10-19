# ms-catalogo · Integrante 1 · puerto 8082 · BD `catalogo_db`

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

## Tareas

- [ ] Entidades `Liga`, `Equipo`, `Arbitro` y `Partido` con sus restricciones UNIQUE (sección 6.2).
- [ ] Mock de `GET /partidos` el día 1, para no bloquear al Integrante 2.
- [ ] Endpoints públicos con filtros.
- [ ] Upsert interno (idempotente: enviar la misma fila 2 veces no duplica nada).
- [ ] Pruebas: el upsert es idempotente y el filtro por fecha usa la hora de Lima.
- [ ] Dockerfile.

## Pistas

- Filtros opcionales: un `@Query` con `(:ligaId IS NULL OR p.liga.id = :ligaId)`, o `Specification` de JPA.
- Fecha de Lima a rango UTC: `fecha.atStartOfDay(ZoneId.of("America/Lima")).toInstant()`, y lo mismo con `fecha.plusDays(1)`.
