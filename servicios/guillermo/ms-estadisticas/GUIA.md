# Guía paso a paso: ms-estadisticas

Guarda lo que hizo cada equipo en cada partido (goles, tiros, córners, faltas y tarjetas) y calcula promedios de equipo, de liga y de árbitro. Los usa `ms-predicciones` para su modelo y `ms-apuestas` para liquidar tarjetas y córners.

- Contrato: [README.md](README.md) (JSON de cada endpoint y reglas RN-EST-01 a RN-EST-06). Tabla: sección 6.4 de la documentación.
- Guía general y orden de trabajo: [docs/guia-paso-a-paso.md](../../../docs/guia-paso-a-paso.md).
- Si es tu primer servicio con base de datos, mira antes [la guía de ms-usuarios](../../renzo/ms-usuarios/GUIA.md).
- Todos los archivos van en `src/main/java/pe/edu/unu/apuestas/estadisticas/`. Para ver uno en la solución:
  `git show universidad/codigo-completo:servicios/guillermo/ms-estadisticas/src/main/java/pe/edu/unu/apuestas/estadisticas/<archivo>`

## Paso 0. Lo que ya trae el esqueleto

- **`pom.xml`:** Web, Data JPA, PostgreSQL, Validation, Eureka Client, Actuator, `springdoc` y H2 para las pruebas.
- **`application.yml`:** `estadisticas_db` en `localhost:5441`.

Levanta PostgreSQL y luego `./mvnw spring-boot:run`. Debe arrancar en el puerto 8084.

## Paso 1. La idea clave: dos filas por partido

Un partido Arsenal 2–1 Chelsea se guarda como **dos filas**, una por equipo:

| Equipo | Rival | Condición | Goles | Tiros | … |
|---|---|---|---|---|---|
| Arsenal | Chelsea | LOCAL | 2 | 15 | … |
| Chelsea | Arsenal | VISITANTE | 1 | 9 | … |

¿Por qué? Porque así los promedios salen con un simple `AVG`, sin joins:

- **A favor** de Arsenal: el promedio de las filas donde `equipo = Arsenal`.
- **En contra** de Arsenal: el promedio de las filas donde `rival = Arsenal`, que son las de sus rivales en esos mismos partidos (RN-EST-02).

## Paso 2. La entidad

**Archivos:** `entity/Condicion.java` y `entity/EstadisticaPartido.java`

- `Condicion`: `LOCAL`, `VISITANTE` y `TODOS`. `TODOS` solo se usa como filtro; ninguna fila se guarda con ese valor.
- `EstadisticaPartido`, tabla `estadistica_partido`, con estas columnas:
  - `EpaIdEstadistica`, `EpaIdPartido`, `EpaIdLiga`, `EpaTemporada` (4), `EpaFecha`;
  - `EpaIdEquipo`, `EpaIdRival`, `EpaCondicion`;
  - `EpaIdArbitro` (puede ser nulo);
  - `EpaGoles`, `EpaTiros`, `EpaTirosPuerta`, `EpaCorners`, `EpaFaltas`, `EpaAmarillas`, `EpaRojas`.
- Restricción `UQ_EpaPartidoEquipo` sobre (partido, equipo), e índices en equipo, rival y árbitro.

**Para pensar:** aquí **no** hay `@ManyToOne` hacia equipos ni partidos, y la tabla no tiene claves foráneas. Los IDs son los de `ms-catalogo`, y esa base de datos es de otro servicio. Un microservicio nunca accede a la base de otro.

**Pista:** un método `actualizar(request, ladoPropio, rivalId, condicion)` que copie todos los datos sirve para crear la fila y también para actualizarla.

## Paso 3. Guardar un partido

**Archivos:** `dto/EstadisticasRequest.java`, `repository/EstadisticaRepository.java` (por ahora solo `findByPartidoIdAndEquipoId`), `service/EstadisticasService.java` (método `guardar`) y `controller/InternoController.java`

- `EstadisticasRequest`: `partidoId`, `ligaId`, `temporada`, `fecha`, `arbitroId` y dos `Lado` (`local`, `visitante`).
  - `Lado` es un `record` interno con `equipoId`, `goles`, `tiros`, `tirosPuerta`, `corners`, `faltas`, `amarillas` y `rojas`.
  - Agrega las validaciones (`@NotNull`, `@Valid` en los lados, `@PositiveOrZero`).
- `guardar(request)`, con `@Transactional`:
  1. Si el local y el visitante son el mismo equipo, responde 400.
  2. Guarda el lado local con rival = visitante y condición LOCAL.
  3. Guarda el lado visitante con rival = local y condición VISITANTE.
  4. Cada lado se busca primero con `findByPartidoIdAndEquipoId`: si existe se actualiza y si no se crea. Así, reenviar el mismo partido no lo duplica (RN-EST-01).
- `POST /interno/estadisticas` responde 200 sin cuerpo. Lo llama `ms-ingesta`.

**Comprueba:**

```bash
curl -i -X POST localhost:8084/interno/estadisticas -H 'Content-Type: application/json' -d '{
  "partidoId": 1, "ligaId": 1, "temporada": "2627", "fecha": "2026-10-24T14:00:00Z", "arbitroId": 4,
  "local":     {"equipoId": 11, "goles": 2, "tiros": 15, "tirosPuerta": 6, "corners": 7, "faltas": 10, "amarillas": 1, "rojas": 0},
  "visitante": {"equipoId": 17, "goles": 1, "tiros": 9,  "tirosPuerta": 3, "corners": 4, "faltas": 12, "amarillas": 3, "rojas": 0}}'
```

En DBeaver deben aparecer 2 filas. Si lo envías otra vez, siguen siendo 2.

## Paso 4. Los promedios en la base de datos

**Archivos:** `repository/Agregado.java`, `repository/Totales.java` y el resto de `EstadisticaRepository`

Los promedios se calculan con `AVG` en la base de datos, no trayendo todas las filas a Java. JPQL puede devolver un `record` directamente con `select new ...`:

```java
@Query("select new pe.edu.unu.apuestas.estadisticas.repository.Agregado(count(e), avg(e.goles), avg(e.tiros), "
     + "avg(e.tirosPuerta), avg(e.corners), avg(e.faltas), avg(e.amarillas + e.rojas)) "
     + "from EstadisticaPartido e where e.equipoId = :equipoId and e.partidoId in :partidos")
Agregado aFavor(Long equipoId, Collection<Long> partidos);
```

- **Tarjetas = amarillas + rojas** (RN-09).
- `Agregado(filas, goles, tiros, ...)` tiene un método `promedios()` que redondea a 2 decimales. Si no hubo filas, devuelve `null`.
- Las consultas que necesitas:
  - `aFavor`: `equipoId = :equipoId`;
  - `enContra`: `rivalId = :equipoId`, en los mismos partidos;
  - `deLiga`: liga y condición, con temporada opcional: `(:temporada is null or e.temporada = :temporada)`;
  - `totalesDeArbitro` y `totalesDeLiga`: devuelven un `Totales(partidos, tarjetas, faltas)` con `count(distinct e.partidoId)` y `sum(...)`;
  - los métodos derivados `findByPartidoId`, `findFirstByArbitroId`, `findByEquipoIdOrderByFechaDesc(equipoId, Pageable)` y `findByEquipoIdAndCondicionOrderByFechaDesc(...)`.

## Paso 5. Las consultas del servicio

**Archivo:** `service/EstadisticasService.java` (todo con `@Transactional(readOnly = true)`)

- **`promediosDeEquipo(equipoId, condicion, ultimos)`:**
  1. `ultimos` va de 1 a 100.
  2. Toma los últimos N partidos del equipo, en la condición pedida o en todas, con `PageRequest.of(0, ultimos)` (RN-EST-03).
  3. Si no hay ninguno, responde `partidos: 0` y los promedios en `null`, **nunca en cero**: no tener datos no es lo mismo que no hacer goles (RN-EST-04).
  4. Si hay, calcula `aFavor` y `enContra` sobre esos mismos IDs de partido.
- **`promediosDeLiga(ligaId, temporada)`:** `deLiga` como local y como visitante. Cada partido tiene una sola fila de local, así que contar esas filas es contar los partidos.
- **`perfilDeArbitro(arbitroId)`:** tarjetas y faltas por partido de los partidos que dirigió, sumando a los dos equipos (RN-EST-05). Para la media de su liga, busca primero una fila del árbitro y así sabes de qué liga es.
- **`estadisticasDePartido(partidoId)`:** las dos filas del partido. Si no hay ninguna, responde 404 `PARTIDO_SIN_ESTADISTICAS` (RN-EST-06).

## Paso 6. DTO, controlador y errores

**Archivos:** los DTO de `dto/`, `controller/EstadisticasController.java` y `exception/`

- DTO: `PromediosDTO`, `PromediosEquipoDTO`, `PromediosLigaDTO`, `PerfilArbitroDTO`, `EstadisticasPartidoDTO` (reutiliza `EstadisticasRequest.Lado`) y `ErrorDTO`.
- Rutas:
  - `GET /equipos/{equipoId}/promedios?condicion=TODOS&ultimos=10`, con esos dos valores por defecto en `@RequestParam(defaultValue = ...)`;
  - `GET /ligas/{ligaId}/promedios?temporada=`;
  - `GET /arbitros/{arbitroId}/perfil`;
  - `GET /partidos/{partidoId}/estadisticas`.
- Errores:
  - `ApiException` responde con su propio código;
  - una validación que falla, 400 `VALIDACION`;
  - un parámetro de tipo incorrecto (por ejemplo `condicion=XYZ`), 400 `PARAMETRO_INVALIDO`;
  - un JSON roto, 400 `FORMATO_INVALIDO`;
  - un choque con el UNIQUE, 409 `DUPLICADO`;
  - cualquier otra excepción, 500.

**Comprueba**, con el partido del paso 3:

```bash
curl -s 'localhost:8084/equipos/11/promedios?condicion=LOCAL'
curl -s 'localhost:8084/arbitros/4/perfil'
curl -s 'localhost:8084/partidos/1/estadisticas'
curl -i 'localhost:8084/partidos/999/estadisticas'   # 404
```

## Paso 7. Pruebas

**Archivo:** `src/test/java/pe/edu/unu/apuestas/estadisticas/controller/EstadisticasTest.java`

Es una prueba por HTTP contra H2, con `@SpringBootTest`, `@Transactional` y `MockMvc`. Conviene un método auxiliar que haga el POST interno.

| Caso | Qué comprueba |
|---|---|
| En contra = rival | Los promedios en contra salen de las filas del rival |
| Reenviar | El mismo partido otra vez actualiza las filas y no las duplica |
| Sin partidos | `partidos: 0` y los promedios en `null` |
| Últimos N | Toma solo los N más recientes en la condición pedida |
| Liga | Separa local y visitante |
| Árbitro | Suma las tarjetas de ambos equipos |
| Sin jugar | 404 |
| Peticiones inválidas | 400 con el JSON común |

## Terminado cuando

- [ ] Reenviar un partido no duplica filas.
- [ ] Los 4 GET responden como dice el contrato, también por `/api/estadisticas/...`.
- [ ] Un equipo sin partidos devuelve `null`, no 0.
- [ ] `./mvnw test` pasa.

Si no alcanza el tiempo, recupera solo este servicio desde la solución (ver la guía general).
