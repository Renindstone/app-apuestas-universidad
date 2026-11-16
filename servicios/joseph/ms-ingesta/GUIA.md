# Guía paso a paso: ms-ingesta

Descarga los CSV de football-data.co.uk y reparte cada fila entre tres servicios: el catálogo, las estadísticas y las cuotas. Es el único servicio que habla con internet y el primero que llama a otros con **OpenFeign**.

- Contrato: [README.md](README.md) (fuentes, columnas del CSV, flujo por fila y reglas RN-ING-01 a RN-ING-06). Tabla: sección 6.3 de la documentación.
- Guía general y orden de trabajo: [docs/guia-paso-a-paso.md](../../../docs/guia-paso-a-paso.md).
- Depende de `ms-catalogo`, `ms-estadisticas` y `ms-cuotas`. Mientras no estén, se avanza con pruebas que simulan esos servicios (paso 9).
- Todos los archivos van en `src/main/java/pe/edu/unu/apuestas/ingesta/`. Para ver uno en la solución:
  `git show universidad/codigo-completo:servicios/guillermo/ms-ingesta/src/main/java/pe/edu/unu/apuestas/ingesta/<archivo>`

## Paso 0. Lo que ya trae el esqueleto

- **`pom.xml`:** Web, Data JPA, PostgreSQL, OpenFeign, Eureka Client, Actuator, `commons-csv` para leer los CSV y H2 para las pruebas.
- **`application.yml`:** `ingesta_db`, y el bloque `ingesta.*` con la URL base de football-data y las dos temporadas (`2627` y `2526`).
- **La clase principal** ya trae:
  - `@EnableFeignClients`: activa los clientes Feign del paso 4;
  - `@EnableAsync`: permite correr la ingesta en segundo plano (paso 7);
  - `@EnableScheduling`: activa las tareas programadas (paso 8);
  - `@ConfigurationPropertiesScan`: lee el bloque `ingesta.*` (paso 1).
- **`src/test/resources/csv/`:** `historico.csv` y `fixtures.csv`, dos archivos pequeños para las pruebas.

## Paso 1. Las propiedades

**Archivo:** `config/IngestaProperties.java`

Un `record` con `@ConfigurationProperties("ingesta")` y los campos `urlBase`, `temporadaActual` y `temporadaAnterior`. Spring lo llena solo con el bloque `ingesta:` del `application.yml` (`url-base` → `urlBase`). Así la URL y las temporadas no quedan escritas en el código.

## Paso 2. La entidad y el repositorio

**Archivos:** `entity/TipoIngesta.java`, `entity/EstadoEjecucion.java`, `entity/Ejecucion.java` y `repository/EjecucionRepository.java`

- Enums: `TipoIngesta` (`HISTORICO`, `PROXIMOS`) y `EstadoEjecucion` (`EN_CURSO`, `OK`, `ERROR`).
- `Ejecucion`, tabla `ejecucion_ingesta`: una fila **por archivo** descargado (RN-ING-06):

| Columna | Detalle |
|---|---|
| `EjeIdEjecucion` | id |
| `EjeTipo` | enum, hasta 10 |
| `EjeFuente` | la URL, hasta 200 |
| `EjeInicio`, `EjeFin` | `Instant`; el fin es nulo mientras corre |
| `EjeEstado` | enum, hasta 10 |
| `EjeFilas` | filas que pasaron por los tres servicios |
| `EjeError` | `columnDefinition = "text"` |

- Los cambios de estado van como métodos de la entidad:
  - el constructor la crea `EN_CURSO`;
  - `iniciar()` pone el inicio en este momento;
  - `terminar(filas, error)` la pasa a `OK`;
  - `fallar(error)` la pasa a `ERROR`.
- `EjecucionRepository`: `existsByEstado` y `findByEstado`.

## Paso 3. Leer el CSV

**Archivos:** `csv/EstadisticasCsv.java`, `csv/PartidoCsv.java` y `csv/CsvFootballDataParser.java`

Es el corazón del servicio y el único lugar que conoce los nombres de las columnas. Primero los dos `record`:

- `EstadisticasCsv`: tiros, tiros a puerta, córners, faltas, amarillas y rojas de **un** equipo.
- `PartidoCsv`: la fila ya traducida (liga, `fechaHora` en UTC, equipos, árbitro, goles, estadísticas de cada lado y cuotas agrupadas por casa), con dos métodos: `jugado()` (llegan los dos goles) y `tieneEstadisticas()`.

Luego `CsvFootballDataParser`, un `@Component` con dos métodos públicos:

1. **`leer(texto)`:** lo convierte en filas con `CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true)...` y se queda solo con las de `Div` E0, SP1 o D1.
   - **Ojo:** algunos archivos empiezan con un BOM (`﻿`). Si no lo quitas, la primera columna se llama «﻿Div» y nunca la encuentras.
2. **`parsear(fila)`:** arma un `PartidoCsv`.
   - **Fecha (RN-ING-03):** `Date` viene como `dd/MM/yyyy` (o `dd/MM/yy` en archivos viejos) y `Time` en **hora de Londres**. Conviértelas así:

     ```java
     Instant utc = LocalDateTime.of(fecha, hora).atZone(ZoneId.of("Europe/London")).toInstant();
     ```

     Con `Europe/London`, el cambio de horario británico se aplica solo: en verano Londres es UTC+1 y en invierno UTC+0.
   - Busca las columnas **por nombre** con `fila.isSet(columna)`: los encabezados cambian entre archivos. Si una columna falta o viene vacía, el valor es `null`.
   - Si falta la liga, la fecha, la hora o un equipo, lanza `IllegalArgumentException`.
   - Estadísticas: `H` + `S`, `ST`, `C`, `F`, `Y`, `R` para el local y `A` + lo mismo para el visitante. Si falta una, ese lado queda en `null`.
   - Cuotas: una tabla fija columna → (casa, mercado, selección, línea), por ejemplo `B365H` → (`bet365`, `1X2`, `LOCAL`, null) y `P>2.5` → (`pinnacle`, `GOLES`, `MAS`, 2.5). Se saltan los valores que no son mayores que 1.

## Paso 4. Los clientes Feign y sus DTO

**Archivos:** `client/CatalogoClient.java`, `client/EstadisticasClient.java`, `client/CuotasClient.java` y los DTO de `dto/`

Cada cliente es una **interfaz** con `@FeignClient(name = "ms-catalogo")`, donde `name` es el nombre del servicio en Eureka. Spring crea la implementación sola.

```java
@FeignClient(name = "ms-catalogo")
public interface CatalogoClient {
    @PostMapping("/interno/partidos")
    PartidoDTO upsert(@RequestBody PartidoUpsertRequest fila);
}
```

- `EstadisticasClient`: `POST /interno/estadisticas` con `EstadisticasRequest`.
- `CuotasClient`: `POST /interno/cuotas` con `CuotasRequest`.

DTO que necesitas, copiados del contrato de cada servicio. No se comparte un jar común: cada servicio es dueño de su copia.

- `PartidoUpsertRequest` y `PartidoDTO` (solo los campos que usas).
- `EstadisticasRequest`, con un `record Lado` interno para cada equipo.
- `CuotasRequest` y `CuotaDTO`.
- `EjecucionDTO`, `EjecucionIniciadaDTO` y `ErrorDTO`.

## Paso 5. Descargar

**Archivo:** `service/DescargadorCsv.java`

Un `@Component` con un `java.net.http.HttpClient`: que siga redirecciones, con 15 s para conectar y 60 s para responder. `descargar(url)` hace **un solo** GET, sin reintentos (RN-ING-01). Si el código no es 200, lanza `IOException`. El servidor no declara la codificación, así que convierte los bytes a `String` en UTF-8.

## Paso 6. Procesar un archivo

**Archivo:** `service/ProcesadorIngesta.java`

Un `record Archivo(ejecucionId, url, temporada)` y tres métodos:

- **`procesarFila(fila, temporada)`**, en el orden de RN-ING-04:
  1. `catalogo.upsert(...)`: devuelve el partido con todos los IDs.
  2. Si `fila.tieneEstadisticas()`: `estadisticas.guardar(...)` con los IDs del paso 1.
  3. Por cada casa que trae cuotas: `cuotas.guardar(...)` con el tipo `APERTURA`.
- **`procesarArchivo(archivo)`:**
  1. Marca la ejecución como iniciada.
  2. Descarga, lee y recorre las filas. Cada fila va dentro de su propio `try/catch`: si una falla, se anota en el log y **se sigue** (RN-ING-05).
  3. Al final, `terminar(procesadas, resumen)`. Si falló la descarga o la lectura, `fallar(mensaje)`.
- **`procesar(archivos)`** con `@Async`: procesa los archivos uno tras otro, en segundo plano.

**Para pensar:** `@Async` solo funciona cuando el método se llama **desde otro bean**. Por eso el procesador es una clase aparte de `IngestaService`: si `IngestaService` se llamara a sí mismo, el método correría en el mismo hilo y la petición HTTP esperaría a que termine toda la ingesta.

## Paso 7. Lanzar la ingesta

**Archivo:** `service/IngestaService.java`

- **`lanzar(tipo, soloTemporadaActual)`:**
  1. Si ya hay una ejecución `EN_CURSO`, responde 409 `INGESTA_EN_CURSO`: dos ingestas a la vez descargarían todo dos veces.
  2. Arma la lista de URLs:
     - PROXIMOS: solo `/fixtures.csv`, con la temporada actual;
     - HISTORICO: `/mmz4281/{temporada}/{liga}.csv` para cada liga y temporada.
  3. Guarda una `Ejecucion` por URL, se las pasa al procesador y devuelve la primera, sin esperar a que termine.
- **`ejecuciones(limite)`:** las últimas, de la más reciente a la más antigua. El límite va de 1 a 100.
- **`cerrarInterrumpidas()`** con `@EventListener(ApplicationReadyEvent.class)`: al arrancar, las ejecuciones que quedaron `EN_CURSO` se cortaron por un reinicio; se cierran en `ERROR` para que no bloqueen las siguientes.

## Paso 8. Tareas programadas

En el mismo `IngestaService` (RN-ING-02):

```java
@Scheduled(cron = "0 0 5 * * *", zone = "America/Lima")      // HISTORICO, solo la temporada actual
@Scheduled(cron = "0 0 8,16 * * *", zone = "America/Lima")   // PROXIMOS
```

Si al dispararse ya hay una ingesta en curso, se escribe un aviso en el log y no se lanza ninguna.

## Paso 9. Controlador y errores

**Archivos:** `controller/IngestaController.java`, `exception/ApiException.java` y `exception/ManejadorErrores.java`

- `POST /ejecutar?tipo=HISTORICO|PROXIMOS`: `@ResponseStatus(HttpStatus.ACCEPTED)` (202), porque la ingesta sigue en segundo plano.
- `GET /ejecuciones?limite=20`.
- No compruebes el rol aquí: el gateway ya bloquea `/api/ingesta/**` a quien no es ADMIN (RN-GAT-02).
- Errores:
  - un `tipo` inválido o un parámetro que falta responden 400 `PARAMETRO_INVALIDO`;
  - `ApiException` responde con su propio código;
  - cualquier otra excepción responde 500.

**Comprueba**, con `ms-catalogo`, `ms-estadisticas` y `ms-cuotas` encendidos:

```bash
curl -s -X POST 'localhost:8083/ejecutar?tipo=HISTORICO'
curl -s 'localhost:8083/ejecuciones'      # repítelo hasta ver los 6 archivos en OK
```

Por el gateway hace falta el token del admin: `Authorization: Bearer <token>` en `/api/ingesta/...`.

## Paso 10. Pruebas

**Archivos:** `src/test/java/pe/edu/unu/apuestas/ingesta/csv/CsvFootballDataParserTest.java` y `.../service/ProcesadorIngestaTest.java`

- **`CsvFootballDataParserTest`** es unitaria, sin Spring:

| Caso | Qué comprueba |
|---|---|
| CB-01 | Fila completa: la hora de Londres pasa a UTC (15:00 en Londres en octubre = 14:00 UTC) y las estadísticas se llenan |
| Cambio de horario | El horario de verano británico termina el 25/10/2026: un partido del 31/10 a las 15:00 de Londres es a las 15:00 UTC |
| Formatos | Año con 2 dígitos y árbitro vacío |
| CB-02 | Partido sin jugar: goles nulos |

- **`ProcesadorIngestaTest`** usa Mockito (`@ExtendWith(MockitoExtension.class)` y `@Mock`) para los tres clientes, el repositorio y el descargador. Así se prueba sin red ni otros servicios. El CSV sale de `src/test/resources/csv/`.

| Caso | Qué comprueba |
|---|---|
| CB-02 | Una fila jugada pasa por los tres servicios **en orden**: usa `InOrder` de Mockito |
| Partido sin jugar | No manda estadísticas |
| Fila con error | No detiene la ejecución: termina en OK con el error resumido |
| Fuente caída | La ejecución termina en ERROR |

## Terminado cuando

- [ ] `POST /ejecutar` responde 202 enseguida y la ingesta corre en segundo plano.
- [ ] Una ingesta HISTORICO deja 6 ejecuciones en OK y llena el catálogo (prueba CN-05).
- [ ] Una fila mala no detiene la ejecución.
- [ ] `./mvnw test` pasa.

Si no alcanza el tiempo, recupera solo este servicio desde la solución (ver la guía general).
