# Guía paso a paso: ms-catalogo

Ligas, equipos, árbitros y partidos. **Es el dueño de los IDs**: los demás servicios guardan `partidoId`, `equipoId` y `ligaId` tal como los entrega este servicio, así que conviene terminarlo primero entre los servicios de negocio.

- Contrato: [README.md](README.md) (endpoints, `PartidoDTO` y reglas RN-CAT-01 a RN-CAT-06). Tablas: sección 6.2 de la documentación.
- Guía general y orden de trabajo: [docs/guia-paso-a-paso.md](../../../docs/guia-paso-a-paso.md).
- Antes de empezar, conviene leer [la guía de ms-usuarios](../../renzo/ms-usuarios/GUIA.md): explica con más detalle el recorrido entidad → repositorio → DTO → excepciones → servicio → controlador.
- Todos los archivos van en `src/main/java/pe/edu/unu/apuestas/catalogo/`. Para ver uno en la solución:
  `git show universidad/codigo-completo:servicios/guillermo/ms-catalogo/src/main/java/pe/edu/unu/apuestas/catalogo/<archivo>`

## Paso 0. Lo que ya trae el esqueleto

- **`pom.xml`:** Web, Data JPA, PostgreSQL, Validation, Eureka Client, Actuator, `springdoc` y H2 para las pruebas.
- **`application.yml`:** `catalogo_db` en `localhost:5441` y los nombres de columna respetados tal cual (`ParIdPartido`).
- **`src/test/resources/config/application.yml`:** H2 y sin Eureka para las pruebas.

Levanta PostgreSQL y luego `./mvnw spring-boot:run`. Debe arrancar en el puerto 8082.

## Paso 1. Las entidades

**Carpeta:** `entity/`

| Clase | Tabla | Columnas | Restricción |
|---|---|---|---|
| `Liga` | `liga` | `LigIdLiga`, `LigCodigo` (5), `LigNombre` (50), `LigPais` (50) | `UQ_LigCodigo` |
| `Equipo` | `equipo` | `EquIdEquipo`, `EquNombre` (60), `EquIdLiga` → liga | `UQ_EquNombre` |
| `Arbitro` | `arbitro` | `ArbIdArbitro`, `ArbNombre` (60) | `UQ_ArbNombre` |
| `Partido` | `partido` | `ParIdPartido`, `ParIdLiga`, `ParTemporada` (4), `ParFechaHora`, `ParIdEquipoLocal`, `ParIdEquipoVisitante`, `ParIdArbitro` (puede ser nulo), `ParGolesLocal`, `ParGolesVisitante`, `ParEstado` | `UQ_ParCruceTemporada` sobre (local, visitante, temporada), más un índice en `ParFechaHora` |

Más un enum `EstadoPartido` con `PROGRAMADO` y `FINALIZADO`.

**Pistas:**
- Las relaciones son `@ManyToOne(fetch = FetchType.LAZY)` con `@JoinColumn(name = "...", foreignKey = @ForeignKey(name = "FK_..."))`.
- La regla RN-CAT-04 va **dentro de la entidad**, en un método `Partido.actualizar(fechaHora, arbitro, golesLocal, golesVisitante)`:
  - la fecha se actualiza siempre, porque un partido puede reprogramarse;
  - el árbitro, solo si llega;
  - si llegan los dos goles, se guardan y el estado pasa a `FINALIZADO`;
  - si no llegan, el estado **no** cambia. Así un partido finalizado nunca vuelve a programado.
- Un partido nuevo nace `PROGRAMADO` en su constructor.

## Paso 2. Las ligas iniciales

**Archivos:** `repository/LigaRepository.java` y `config/LigasIniciales.java`

`LigasIniciales` es un `CommandLineRunner` como el `AdminInicial` de `ms-usuarios`: si `ligas.count() == 0`, guarda E0, SP1 y D1 con su nombre y país (RN-CAT-01).

**Comprueba:** arranca el servicio y mira en DBeaver que la tabla `liga` tiene 3 filas.

## Paso 3. Los repositorios

**Carpeta:** `repository/`

- **`LigaRepository`:** `findByCodigo`.
- **`EquipoRepository`:** `findByNombre`, `findAllByOrderByNombreAsc` y `findByLigaIdOrderByNombreAsc`.
- **`ArbitroRepository`:** `findByNombre`.
- **`PartidoRepository`:**
  - extiende `JpaRepository` y `JpaSpecificationExecutor<Partido>`;
  - `findByLocalAndVisitanteAndTemporada`, para el upsert.
- **`PartidoSpecs`:** una clase con métodos estáticos que devuelven `Specification<Partido>`. Cada uno es un filtro que se puede combinar con `and()`:
  - `enLiga(ligaId)`: `cb.equal(raiz.get("liga").get("id"), ligaId)`;
  - `conEstado(estado)`;
  - `desde(instante)` (incluido) y `antesDe(instante)` (sin incluir);
  - `deEquipo(equipoId)`: un `cb.or(...)` porque el equipo pudo jugar de local o de visitante.

**Para pensar (N+1):** al listar 300 partidos, ¿cuántas consultas hace Hibernate para traer la liga y los equipos de cada uno? Sin ayuda, una por partido. La solución sobrescribe `findAll(Specification, Sort)`, `findAll(Specification, Pageable)` y `findById` con `@EntityGraph(attributePaths = {"liga", "local", "visitante", "arbitro"})`, que trae todo en una sola consulta.

## Paso 4. Los DTO

**Carpeta:** `dto/` (todos como `record`)

| DTO | Campos |
|---|---|
| `LigaDTO` | `id`, `codigo`, `nombre`, `pais` |
| `EquipoDTO` | `id`, `nombre`, `ligaId` |
| `LigaResumenDTO` | `id`, `codigo`, `nombre`: la liga dentro de un partido |
| `ReferenciaDTO` | `id`, `nombre`: un equipo o un árbitro dentro de un partido |
| `PartidoDTO` | el JSON del contrato; `arbitro` es `null` si no se conoce |
| `PartidoUpsertRequest` | `ligaCodigo`, `temporada` (`@Pattern("\\d{4}")`), `fechaHora`, `local`, `visitante` (`@Size(max = 60)`), `arbitro`, `golesLocal`, `golesVisitante` |
| `ErrorDTO` | `error`, `detalle` |

Agrega a cada DTO un método estático `de(entidad)` que lo arme a partir de la entidad. Así el servicio queda corto: `.map(PartidoDTO::de)`.

## Paso 5. Excepciones

**Carpeta:** `exception/`

- `ApiException`: la base, igual que en `ms-usuarios`.
- `NoEncontradoException`: 404 con un código propio, por ejemplo `PARTIDO_NO_ENCONTRADO` o `EQUIPO_NO_ENCONTRADO`.
- `ValidacionException`: 400 `VALIDACION`.

`ManejadorErrores` cubre además:

| Excepción | Respuesta | Ejemplo |
|---|---|---|
| `MethodArgumentTypeMismatchException` | 400 `PARAMETRO_INVALIDO` | `?fecha=ayer` o `?estado=XYZ` |
| `DataIntegrityViolationException` | 409 `DUPLICADO` | dos upserts simultáneos chocan con un UNIQUE |

## Paso 6. Las consultas

**Archivo:** `service/CatalogoService.java` (`@Transactional(readOnly = true)` en la clase)

- `ligas()`: todas, ordenadas por id.
- `equipos(ligaId)`: por nombre; `ligaId` es opcional.
- `partidos(fecha, desde, hasta, ligaId, estado)`:
  1. Si llega `fecha`, es lo mismo que `desde = hasta = fecha`.
  2. Si `desde` es posterior a `hasta`, lanza `ValidacionException`.
  3. Arranca con `Specification.unrestricted()` y agrega con `and()` solo los filtros que llegaron.
  4. Ordena por `fechaHora` y luego por `id`.
- `partido(id)`: el partido, o 404.
- `partidosDeEquipo(equipoId, estado, limite)`: 404 si el equipo no existe; el límite va de 1 a 100; del más reciente al más antiguo, con `PageRequest.of(0, limite, orden)`.

**La regla de las fechas (RN-CAT-06):** el filtro `fecha=2026-10-24` es un día **de Lima**, que va de las 05:00 UTC del 24 a las 05:00 UTC del 25:

```java
Instant inicio = fecha.atStartOfDay(ZoneId.of("America/Lima")).toInstant();
Instant fin = fecha.plusDays(1).atStartOfDay(ZoneId.of("America/Lima")).toInstant();
```

Usa `desde(inicio)` y `antesDe(fin)`. El fin no se incluye.

## Paso 7. El upsert interno

**Archivo:** `service/UpsertPartidoService.java`

`upsert(fila)`, con `@Transactional`, es lo que llama `ms-ingesta` por cada fila del CSV:

1. Valida que lleguen los dos goles o ninguno, y que el local y el visitante no sean el mismo equipo.
2. Busca la liga por código (en mayúsculas); si no existe, `ValidacionException`.
3. Busca el equipo local y el visitante por nombre; si no existen, los crea en esa liga (RN-CAT-03).
4. Lo mismo con el árbitro, si llegó.
5. Busca el partido por (local, visitante, temporada); si no existe, crea uno nuevo.
6. Llama a `partido.actualizar(...)` y guarda.

**Para pensar:** ¿por qué enviar la misma fila dos veces deja un solo partido? Porque el paso 5 encuentra el partido que creó la primera vez. Eso es la idempotencia (RN-CAT-02).

## Paso 8. Los controladores

**Carpeta:** `controller/`

| Controlador | Ruta |
|---|---|
| `LigaController` | `GET /ligas` |
| `EquipoController` | `GET /equipos?ligaId=`, `GET /equipos/{id}/partidos?estado=&limite=10` |
| `PartidoController` | `GET /partidos?fecha=&desde=&hasta=&ligaId=&estado=`, `GET /partidos/{id}` |
| `InternoController` | `POST /interno/partidos` con `@Valid @RequestBody PartidoUpsertRequest` |

Las fechas de los parámetros se leen con `@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha`.

**Comprueba:**

```bash
curl -s -X POST localhost:8082/interno/partidos -H 'Content-Type: application/json' \
  -d '{"ligaCodigo":"E0","temporada":"2627","fechaHora":"2026-10-24T14:00:00Z","local":"Arsenal","visitante":"Chelsea","arbitro":"M Oliver","golesLocal":null,"golesVisitante":null}'
curl -s 'localhost:8082/partidos?fecha=2026-10-24'
curl -s 'localhost:8082/equipos?ligaId=1'
```

Envía el POST dos veces: el id del partido debe ser el mismo. Después, a través del gateway: `curl -s localhost:8080/api/catalogo/ligas`.

## Paso 9. Pruebas

**Archivo:** `src/test/java/pe/edu/unu/apuestas/catalogo/controller/CatalogoTest.java`

- Es una prueba de integración por HTTP contra H2, con `@SpringBootTest` y `MockMvc`, igual que `FlujoUsuarioTest`.
- Con `@Transactional` en la clase, cada prueba deshace sus cambios al terminar.
- Conviene un método auxiliar `fila(...)` que haga el POST a `/interno/partidos`.

Casos:

| Caso | Qué comprueba |
|---|---|
| Ligas al arrancar | Existen las 3 ligas |
| Upsert idempotente | La misma fila dos veces da el mismo id y un solo partido |
| Finalizado no vuelve | Una fila con goles y después otra sin goles: sigue FINALIZADO |
| Día de Lima | Un partido a las 02:00 UTC del 25 es del día 24 en Lima |
| Filtros | Por liga y estado, ordenados por hora |
| Partidos de un equipo | Los últimos partidos de un equipo |
| Errores | 404, 400 y `PARAMETRO_INVALIDO` con el JSON común |

## Terminado cuando

- [ ] Los endpoints públicos responden como dice el contrato, también por `/api/catalogo/...`.
- [ ] `POST /interno/partidos` es idempotente.
- [ ] El filtro por fecha usa el día de Lima.
- [ ] `./mvnw test` pasa.

Si no alcanza el tiempo, recupera solo este servicio desde la solución (ver la guía general).
