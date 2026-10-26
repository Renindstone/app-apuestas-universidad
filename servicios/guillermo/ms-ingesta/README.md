# ms-ingesta · Guillermo (Integrante 1) · puerto 8083 · BD `ingesta_db`

**Responsabilidad:** descarga los CSV gratuitos de [football-data.co.uk](https://www.football-data.co.uk) y reparte sus datos entre `ms-catalogo`, `ms-estadisticas` y `ms-cuotas`. **Es el único servicio que conoce el formato de football-data** (capa anticorrupción): los demás reciben JSON limpio.

## Spring Initializr

- Dependencias: **Spring Web**, **Spring Data JPA**, **PostgreSQL Driver**, **OpenFeign**, **Eureka Discovery Client** y **Actuator**.
- Además, `org.apache.commons:commons-csv` y springdoc.

## Fuentes

| Qué | URL |
|---|---|
| Temporada completa de una liga (resultados, estadísticas, árbitro y cuotas) | `https://www.football-data.co.uk/mmz4281/{temporada}/{liga}.csv`, por ejemplo `.../mmz4281/2627/E0.csv` |
| Próximos partidos de todas las ligas, con cuotas | `https://www.football-data.co.uk/fixtures.csv` (filtrar `Div` en E0, SP1 y D1) |

- **Temporadas:** la actual (`2627`) y la anterior (`2526`). Se cargan las 3 ligas: `E0`, `SP1` y `D1`.
- **Cortesía con la fuente:** descargar como mucho una vez por ejecución y nunca en un bucle. Si responde un error, registrar la ejecución con estado `ERROR` y seguir.

## Columnas que se usan

| Columna CSV | Significado | Va a |
|---|---|---|
| `Div` | Liga (E0, SP1, D1) | catálogo |
| `Date` (dd/MM/yyyy), `Time` (HH:mm, **hora de Londres**) | Fecha y hora | catálogo (convertir a UTC) |
| `HomeTeam`, `AwayTeam` | Equipos | catálogo |
| `FTHG`, `FTAG` | Goles local y visitante (vacíos si no se jugó) | catálogo y estadísticas |
| `Referee` | Árbitro (solo en la Premier) | catálogo |
| `HS`/`AS`, `HST`/`AST`, `HC`/`AC`, `HF`/`AF`, `HY`/`AY`, `HR`/`AR` | Tiros, tiros a puerta, córners, faltas, amarillas y rojas (local/visitante) | estadísticas |
| `B365H`, `B365D`, `B365A` | Cuotas 1X2 de bet365 | cuotas (`bet365`, `1X2`) |
| `PSH`, `PSD`, `PSA` | Cuotas 1X2 de Pinnacle | cuotas (`pinnacle`, `1X2`) |
| `B365>2.5`, `B365<2.5` | Más/menos de 2.5 goles en bet365 | cuotas (`bet365`, `GOLES`, línea 2.5) |
| `P>2.5`, `P<2.5` | Más/menos de 2.5 goles en Pinnacle | cuotas (`pinnacle`, `GOLES`, línea 2.5) |

Cuidado con los nombres: los encabezados exactos varían un poco entre archivos. Búsquenlos por nombre, no por posición, y si una columna no existe, sáltenla.

## Flujo por cada fila

1. `POST ms-catalogo /interno/partidos` → recibe el `PartidoDTO` con todos los IDs.
2. Si la fila tiene goles y estadísticas → `POST ms-estadisticas /interno/estadisticas`, con los IDs del paso 1.
3. Si tiene cuotas → `POST ms-cuotas /interno/cuotas`.
4. Si una fila falla, registrar el error en el log y **seguir con la siguiente**: una fila mala no tumba la ejecución.

## Endpoints (vía gateway: `/api/ingesta` + ruta, **solo ADMIN**)

| Método | Ruta | Respuesta |
|---|---|---|
| POST | `/ejecutar?tipo=HISTORICO` | `202 { "id": 7, "estado": "EN_CURSO" }`. Corre en segundo plano (`@Async`). |
| POST | `/ejecutar?tipo=PROXIMOS` | Igual, pero solo con `fixtures.csv` |
| GET | `/ejecuciones?limite=20` | `[{ "id": 7, "tipo": "HISTORICO", "fuente": "...E0.csv", "inicio": "...", "fin": "...", "estado": "OK", "filas": 1140, "error": null }]` |

## Tareas programadas (`@Scheduled`, zona `America/Lima`)

| Hora | Tarea |
|---|---|
| 05:00 todos los días | HISTORICO, solo de la temporada actual |
| 08:00 y 16:00 | PROXIMOS |

## Reglas de negocio

Reglas específicas de este servicio, con el mismo código que en la [sección 4.5 de la documentación](../../../docs/Documentacion.md#45-reglas-de-negocio). Si cambian, se actualizan los dos documentos y se avisa en el daily.

| ID | Regla | Relación |
|---|---|---|
| RN-ING-01 | Es el único servicio que lee football-data. Descarga cada archivo como mucho una vez por ejecución, nunca en un bucle. | RN-11 · RF-03 |
| RN-ING-02 | Se ejecuta sola en hora de Lima: HISTORICO a las 05:00 (solo la temporada actual) y PROXIMOS a las 08:00 y a las 16:00. Solo el ADMIN puede lanzarla a mano. | RF-03 y RF-04 · CU-09 y CU-10 |
| RN-ING-03 | La hora del CSV es la de Londres y se convierte a UTC, de modo que el cambio de horario británico se aplica solo. | RN-12 · CB-01 |
| RN-ING-04 | Cada fila se procesa en este orden: primero el catálogo, que devuelve los IDs; luego las estadísticas, solo si el partido ya se jugó; y por último las cuotas, solo si la fila las trae. | RF-03 · CB-02 |
| RN-ING-05 | Una fila con error se anota y no detiene la ejecución. Si la fuente no responde, la ejecución termina en ERROR y se reintenta en la siguiente hora programada. | CU-10 · RNF-04 |
| RN-ING-06 | Cada ejecución queda registrada con su tipo, fuente, inicio, fin, filas, estado y error. | RF-16 · CN-05 |

Reglas generales que aplica: RN-10 (ligas, temporadas y casas), RN-11 (origen de los datos) y RN-12 (fechas).

## Tareas

- [x] Cliente HTTP que descarga el CSV como texto (`service/DescargadorCsv`).
- [x] `CsvFootballDataParser` que convierte una fila en un `PartidoCsv` (record con todo lo de la tabla).
- [x] Clientes Feign hacia los 3 servicios.
- [x] Orquestador con registro en `ejecucion_ingesta` (`service/ProcesadorIngesta`).
- [x] Endpoints y `@Scheduled`.
- [x] Pruebas: CB-01 (fila completa y hora de Londres a UTC) y CB-02 (fila sin jugar), con CSV pequeños en `src/test/resources/csv`.
- [x] Dockerfile.

**Pinnacle:** football-data dejó de publicar `PSH`/`PSD`/`PSA` y `P>2.5`/`P<2.5` en enero de 2026, a mitad de la temporada 2526. Solo los traen 549 de sus 1066 partidos, y ninguno de la 2627 ni de `fixtures.csv`. El parser se salta las celdas vacías, así que de la temporada actual solo se cargan cuotas de bet365.

## Pistas

- **BOM:** `fixtures.csv` empieza con un BOM UTF-8 (`﻿`). Si no lo quitan, la primera columna se llama `﻿Div` y no `Div`. Usen `BOMInputStream` de commons-io, o quiten ese carácter a mano.
- **Hora:** `LocalDateTime.of(fecha, hora).atZone(ZoneId.of("Europe/London")).toInstant()`. Así el cambio de horario británico (25 de octubre de 2026) se aplica solo.
- Algunas temporadas antiguas traen el año con 2 dígitos (`24/10/26`). Prueben ambos formatos.
- Para desarrollar sin depender de la red, guarden una copia del CSV en `src/main/resources/muestras/` y añadan una propiedad `ingesta.usar-muestras=true`.
