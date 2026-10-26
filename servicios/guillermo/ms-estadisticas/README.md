# ms-estadisticas · Integrante 1 · puerto 8084 · BD `estadisticas_db`

**Responsabilidad:** guarda las estadísticas de cada equipo en cada partido y calcula promedios: de un equipo (a favor y en contra, como local o visitante, en los últimos N partidos), de la liga y del árbitro. Es la materia prima de `ms-predicciones`.

## Spring Initializr

- Dependencias: **Spring Web**, **Spring Data JPA**, **PostgreSQL Driver**, **Validation**, **Eureka Discovery Client**, **Actuator** y springdoc.

## Modelo

Una fila por equipo y partido (tabla `estadistica_partido`, sección 6.4 de la documentación). **Tarjetas = amarillas + rojas.**

## Endpoint interno (solo lo usa `ms-ingesta`)

### `POST /interno/estadisticas` (upsert por partido y equipo)

```json
{
  "partidoId": 1532, "ligaId": 1, "temporada": "2627", "fecha": "2026-10-24T14:00:00Z", "arbitroId": 4,
  "local":     { "equipoId": 11, "goles": 2, "tiros": 15, "tirosPuerta": 6, "corners": 7, "faltas": 10, "amarillas": 2, "rojas": 0 },
  "visitante": { "equipoId": 17, "goles": 1, "tiros": 9,  "tirosPuerta": 3, "corners": 4, "faltas": 13, "amarillas": 3, "rojas": 0 }
}
```

Guarda 2 filas: en la del local, `rival` es el visitante, y en la del visitante, al revés. Responde **200**.

## Endpoints públicos (vía gateway: `/api/estadisticas` + ruta)

### `GET /equipos/{equipoId}/promedios?condicion=LOCAL&ultimos=10`

- `condicion`: `LOCAL`, `VISITANTE` o `TODOS` (por defecto `TODOS`). `ultimos`: por defecto 10.
- Toma los últimos N partidos del equipo en esa condición, del más reciente al más antiguo, sin importar la temporada.

```json
{
  "equipoId": 11, "condicion": "LOCAL", "partidos": 10,
  "aFavor":   { "goles": 2.1, "tiros": 15.3, "tirosPuerta": 5.8, "corners": 6.9, "faltas": 9.8,  "tarjetas": 1.7 },
  "enContra": { "goles": 0.8, "tiros": 8.9,  "tirosPuerta": 2.9, "corners": 3.6, "faltas": 11.2, "tarjetas": 2.2 }
}
```

`enContra` se calcula sobre las filas del **rival** en esos mismos partidos. Si el equipo no tiene partidos: `partidos: 0` y los promedios en `null`.

### `GET /ligas/{ligaId}/promedios?temporada=2627`

Promedio por equipo y partido, separado por condición. Si no se pasa `temporada`, se usan las 2 cargadas.

```json
{ "ligaId": 1, "temporada": "2627", "partidos": 90,
  "local":     { "goles": 1.55, "tiros": 14.1, "tirosPuerta": 4.9, "corners": 5.6, "faltas": 10.4, "tarjetas": 1.8 },
  "visitante": { "goles": 1.25, "tiros": 11.6, "tirosPuerta": 4.0, "corners": 4.6, "faltas": 10.9, "tarjetas": 2.1 } }
```

### `GET /arbitros/{arbitroId}/perfil`

```json
{ "arbitroId": 4, "partidos": 45, "tarjetasPorPartido": 4.3, "faltasPorPartido": 21.0, "mediaLigaTarjetas": 3.9 }
```

`tarjetasPorPartido` suma las tarjetas de **ambos** equipos en cada partido que dirigió. `mediaLigaTarjetas` es lo mismo para todos los partidos de su liga y sirve para comparar.

### `GET /partidos/{partidoId}/estadisticas`

Devuelve `{ "partidoId": 1532, "local": {...}, "visitante": {...} }` con el mismo formato del POST, o **404** si el partido aún no se jugó. Lo usa `ms-apuestas` para liquidar tarjetas y córners.

## Reglas de negocio

Reglas específicas de este servicio, con el mismo código que en la [sección 4.5 de la documentación](../../docs/Documentacion.md#45-reglas-de-negocio). Si cambian, se actualizan los dos documentos y se avisa en el daily.

| ID | Regla | Relación |
|---|---|---|
| RN-EST-01 | Cada partido se guarda como dos filas, una por equipo, y en cada una el rival es el otro equipo. Si el mismo partido llega otra vez, se actualizan esas filas. | RF-06 |
| RN-EST-02 | Los promedios *a favor* salen de las filas del equipo. Los promedios *en contra* salen de las filas de su rival en esos mismos partidos. | RF-06 |
| RN-EST-03 | Se toman los últimos N partidos (10 por defecto), del más reciente al más antiguo y sin importar la temporada. Se pueden filtrar como local, como visitante o todos. | RF-06 · RN-10 |
| RN-EST-04 | Si un equipo no tiene partidos, la respuesta indica 0 partidos y deja los promedios vacíos (`null`), nunca en cero: no tener datos no es lo mismo que no hacer goles. | RF-06 |
| RN-EST-05 | El perfil del árbitro suma las tarjetas de ambos equipos en cada partido que dirigió y lo compara con la media de su liga. | RF-07 |
| RN-EST-06 | Un partido que aún no se jugó no tiene estadísticas: la consulta responde 404. | RF-14 |

Reglas generales que aplica: RN-09 (tarjetas = amarillas + rojas) y RN-12 (fechas).

## Tareas

- [x] Entidad e índices (`entity/EstadisticaPartido`, con UNIQUE(EpaIdPartido, EpaIdEquipo)).
- [x] Upsert interno (reenviar un partido actualiza sus 2 filas y no las duplica), en `service/EstadisticasService`.
- [x] Promedios de equipo, liga y árbitro con consultas `AVG` en el repositorio (`repository/EstadisticaRepository`).
- [x] Pruebas: los promedios en contra usan al rival, un equipo sin partidos devuelve `null`, últimos N por condición, liga por temporada, árbitro, 404 y validaciones (`EstadisticasTest`, con H2 en memoria).
- [x] Dockerfile.

**Detalles del contrato** (los usan `ms-predicciones` y `ms-apuestas`):

- Los promedios van con 2 decimales.
- Sin partidos, van en `null` los objetos completos: `"aFavor": null`, `"enContra": null` en un equipo y `"local": null`, `"visitante": null` en una liga. En el árbitro van en `null` las tres cifras.
- El 404 de `GET /partidos/{id}/estadisticas` es `{"error":"PARTIDO_SIN_ESTADISTICAS", ...}`.

**Variables de entorno:** `DB_HOST`, `DB_PORT`, `POSTGRES_USER`, `POSTGRES_PASSWORD` y `EUREKA_URL`.

## Pistas

- "Últimos N" con JPA: `findByEquipoIdAndCondicionOrderByFechaDesc(id, cond, PageRequest.of(0, n))`. Luego promedien en Java, que es más simple que una subconsulta.
- Para los promedios en contra, busquen las filas cuyo `partidoId` está en esa lista y cuyo `equipoId` es distinto del equipo.
