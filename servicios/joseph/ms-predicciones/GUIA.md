# Guía paso a paso: ms-predicciones

Calcula la probabilidad de cada mercado de un partido con un **modelo de Poisson**. No tiene base de datos: en cada petición pide los datos a `ms-catalogo` y `ms-estadisticas`, y vuelve a calcular (RN-PRE-01).

Honestidad ante todo (RN-15): es un modelo **didáctico**. No conoce alineaciones, lesiones ni noticias, y no le gana a las casas de apuestas de forma sostenida. La respuesta lo dice.

- Contrato: [README.md](README.md) (JSON de la respuesta y reglas RN-PRE-01 a RN-PRE-07).
- Guía general y orden de trabajo: [docs/guia-paso-a-paso.md](../../../docs/guia-paso-a-paso.md).
- Depende de `ms-catalogo` y `ms-estadisticas`. Las matemáticas (pasos 1 a 3) se hacen y prueban sin ellos.
- Todos los archivos van en `src/main/java/pe/edu/unu/apuestas/predicciones/`. Para ver uno en la solución:
  `git show universidad/codigo-completo:servicios/joseph/ms-predicciones/src/main/java/pe/edu/unu/apuestas/predicciones/<archivo>`

## Paso 0. Lo que ya trae el esqueleto

- **`pom.xml`:** Web, OpenFeign, Eureka Client, Actuator y `springdoc`. **No** tiene JPA ni PostgreSQL: este servicio no guarda nada.
- **La clase principal** ya trae `@EnableFeignClients`.

`./mvnw spring-boot:run` arranca en el puerto 8086, sin necesidad de PostgreSQL.

## Paso 1. Poisson en una función

**Archivo:** `service/PoissonService.java`

Una clase `final` con métodos estáticos: son funciones puras, sin Spring. Empieza por:

```
P(X = k) = e^(−λ) · λ^k / k!
```

**Pista:** no calcules `k!` directamente, porque se desborda. Hazlo de forma iterativa:

```java
double p = Math.exp(-lambda);
for (int i = 1; i <= k; i++) p = p * lambda / i;
```

**Para pensar:** λ es el promedio de goles que se espera de un equipo. Poisson responde: si un equipo hace en promedio 1.5 goles, ¿qué probabilidad hay de que haga exactamente 0, 1, 2…?

## Paso 2. La matriz de marcadores y los mercados

En el mismo `PoissonService` (RN-PRE-04):

- **`matriz(λL, λV)`:** una matriz de 11 × 11 (de 0 a 10 goles) con `P(i, j) = Pois(i; λL) × Pois(j; λV)`, la probabilidad de que el partido termine i a j.
- **`unoXDos(m)`:** suma las celdas con i > j (LOCAL), i = j (EMPATE) e i < j (VISITA).
- **`masDe(m, linea)`:** suma las celdas con i + j > línea. MENOS es 1 − MAS.
- **`ambosMarcan(λL, λV)`:** `(1 − e^−λL) × (1 − e^−λV)`.
- **`masDeTotal(λ, linea)`**, para córners y tarjetas, que se modelan sobre el total del partido: `1 − Σ Pois(k; λ)` con k desde 0 hasta la línea.

**Comprueba con la prueba del paso 7** antes de seguir: si LOCAL + EMPATE + VISITA no suman 1 (± 0.001), algo está mal (CB-03).

## Paso 3. Los valores esperados

**Archivo:** `service/PrediccionService.java`, métodos estáticos `esperado`, `factor` y `factorArbitro`

¿De dónde sale λ? De combinar la media de la liga con el ataque de un equipo y la defensa del otro (RN-PRE-02):

```
λL = μL × ataqueL × defensaV
λV = μV × ataqueV × defensaL
```

- `μL` y `μV`: la media de la liga como local y como visitante.
- `ataqueL = (lo que marca el local en casa) / μL` y `defensaV = (lo que recibe el visitante fuera) / μL`. Lo mismo para el otro lado.
- El mismo cálculo sirve para goles, córners, tiros y tarjetas. Pasa la estadística como `Function<PromediosDTO, Double>` (por ejemplo `PromediosDTO::goles`) para no repetirlo.

**Suavizado (RN-PRE-03):** con pocos partidos, un factor puede salir absurdo. Por eso se acerca a 1, que es «como la media de la liga»:

```
factor' = (n × factor + 5) / (n + 5)
```

Si `n = 0` o la media es 0, el factor vale 1.

**Factor árbitro (RN-PRE-05):** solo ajusta las tarjetas.

```
factor = [(n × tarjetasPorPartido + 10 × mediaLiga) / (n + 10)] / mediaLiga
```

Vale 1 si el partido no tiene árbitro (LaLiga y la Bundesliga) o si el árbitro no tiene partidos.

## Paso 4. Los clientes Feign y sus DTO

**Archivos:** `client/CatalogoClient.java`, `client/EstadisticasClient.java` y los DTO de `dto/`

- `CatalogoClient`: `GET /partidos/{id}` → `PartidoDTO`, con solo lo que usas: liga, fecha, equipos y árbitro.
- `EstadisticasClient`:
  - `GET /ligas/{ligaId}/promedios` → `PromediosLigaDTO`;
  - `GET /equipos/{equipoId}/promedios?condicion=&ultimos=` → `PromediosEquipoDTO`;
  - `GET /arbitros/{arbitroId}/perfil` → `PerfilArbitroDTO`.
- Los DTO de entrada son copias de los contratos de esos servicios: `PromediosDTO`, `PromediosLigaDTO`, `PromediosEquipoDTO` y `PerfilArbitroDTO`.
- Los de salida:
  - `PrediccionDTO`, `EsperadosDTO` y `MuestraDTO`;
  - `MercadoDTO`, con un método `de(mercado, seleccion, linea, probabilidad)` que calcula la cuota justa `1 / p` (RN-PRE-06). Usa como mínimo 0.001 para no dividir entre 0.

## Paso 5. Armar la predicción

**Archivo:** `service/PrediccionService.java`, método `predecir(partidoId)`

1. Pide el partido a `ms-catalogo`. Si no existe, 404.
2. Pide los promedios de la liga. Si la liga tiene 0 partidos, responde 409 `SIN_DATOS`: sin datos no se inventa nada.
3. Pide los promedios del local (`LOCAL`, últimos 10) y del visitante (`VISITANTE`, últimos 10), y el perfil del árbitro si hay.
4. Calcula los esperados de goles, córners y tiros; las tarjetas, multiplicadas por el factor árbitro.
5. Arma **todos** los mercados y líneas del README raíz (RN-09):
   - 1X2 y ambos marcan;
   - goles en 1.5, 2.5 y 3.5;
   - tarjetas en 3.5, 4.5 y 5.5;
   - córners en 8.5, 9.5 y 10.5.
6. Arma la explicación (RN-15):
   - una frase por equipo con sus números y la media de la liga;
   - una frase del árbitro;
   - un aviso si la muestra es corta;
   - siempre, la frase de que el modelo es didáctico.

**Errores de Feign (RN-PRE-07):**
- `FeignException.NotFound` del catálogo → 404 `PARTIDO_NO_ENCONTRADO`.
- Cualquier otra `FeignException` → 503 `SERVICIO_NO_DISPONIBLE` con el nombre del servicio («ms-estadisticas no responde.»).

**Pista:** un método auxiliar `llamar("ms-estadisticas", () -> estadisticas.liga(id))` que envuelva el `try/catch` evita repetirlo en cada llamada.

## Paso 6. Controlador y errores

**Archivos:** `controller/PrediccionController.java` y `exception/`

- `GET /partidos/{partidoId}` → `PrediccionDTO`.
- `ManejadorErrores`:
  - `ApiException` responde con su propio código;
  - `/partidos/abc` responde 400 `PARAMETRO_INVALIDO`;
  - las excepciones de Spring MVC (`ServletException`, por ejemplo una ruta que no existe) responden con su propio código;
  - cualquier otra excepción responde 500.

**Comprueba**, con el catálogo y las estadísticas cargados por la ingesta:

```bash
curl -s localhost:8086/partidos/1 | head -c 600
```

Busca en DBeaver un partido de la Premier que tenga árbitro para ver el factor distinto de 1.

## Paso 7. Pruebas

**Archivos:** `src/test/java/pe/edu/unu/apuestas/predicciones/service/PoissonServiceTest.java` y `.../controller/PrediccionTest.java`

- **`PoissonServiceTest`** es unitaria:

| Caso | Qué comprueba |
|---|---|
| CB-03 | Con λL = 1.5 y λV = 1.1, 1X2 suma 1 (± 0.001) |
| PMF | Con λ = 1 coincide con la fórmula |
| MAS | `masDe` por la matriz y `masDeTotal` dan lo mismo para el total de goles |
| Ambos marcan | La fórmula cerrada |

- **`PrediccionTest`** usa `@SpringBootTest` y MockMvc, con `@MockitoBean` para `CatalogoClient` y `EstadisticasClient`. Así cada prueba decide qué responden los otros servicios.

| Caso | Qué comprueba |
|---|---|
| Equipos promedio | Dos equipos iguales a la media de la liga dan λ igual a la media |
| Suavizado | El suavizado depende de la muestra |
| Equipo sin partidos | Es como la media |
| Árbitro | Las tarjetas se ajustan con el factor árbitro |
| Árbitro sin partidos | No cambia las tarjetas |
| Errores | 404, 503 de cada servicio, 409 `SIN_DATOS` y 400 |

## Terminado cuando

- [ ] 1X2 suma 1 y la respuesta trae todos los mercados y líneas.
- [ ] Si `ms-estadisticas` está apagado, responde 503, nunca una probabilidad inventada.
- [ ] La explicación dice de dónde salen los números y que el modelo es didáctico.
- [ ] `./mvnw test` pasa.

Si no alcanza el tiempo, recupera solo este servicio desde la solución (ver la guía general).
