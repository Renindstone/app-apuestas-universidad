# Guía paso a paso: ms-apuestas

Es el servicio más grande y el último en construirse, porque llama a cuatro servicios: `ms-catalogo`, `ms-predicciones`, `ms-cuotas` y `ms-estadisticas`. Hace cuatro cosas:

- evalúa si una cuota tiene valor;
- elige la apuesta del día;
- guarda las apuestas de cada usuario con su rendimiento;
- liquida las apuestas cuando termina el partido.

Conviene construirlo de adentro hacia afuera: primero las fórmulas puras, que se prueban sin nada más, y al final lo que habla con otros servicios.

- Contrato: [README.md](README.md) (fórmulas, endpoints, tabla de liquidación y reglas RN-APU-01 a RN-APU-08). Tablas: sección 6.6 de la documentación.
- Guía general y orden de trabajo: [docs/guia-paso-a-paso.md](../../../docs/guia-paso-a-paso.md).
- Todos los archivos van en `src/main/java/pe/edu/unu/apuestas/apuestas/`. Para ver uno en la solución:
  `git show universidad/codigo-completo:servicios/joseph/ms-apuestas/src/main/java/pe/edu/unu/apuestas/apuestas/<archivo>`

## Paso 0. Lo que ya trae el esqueleto

- **`pom.xml`:** Web, Data JPA, PostgreSQL, Validation, OpenFeign, Eureka Client, Actuator, `springdoc` y H2 para las pruebas.
- **`application.yml`:** `apuestas_db` en `localhost:5441`.
- **La clase principal** ya trae `@EnableFeignClients` y `@EnableScheduling`, esta última para las tareas de las 06:00 y las 09:00.

Levanta PostgreSQL y luego `./mvnw spring-boot:run`. Debe arrancar en el puerto 8087.

## Paso 1. Valor esperado, stake y veredicto

**Archivo:** `service/ValorService.java`, por ahora solo el método estático `evaluar(probabilidad, cuota)`

| Concepto | Fórmula |
|---|---|
| Valor esperado (RN-03) | `EV = p × cuota − 1` |
| Kelly | `f = EV / (cuota − 1)` |
| Stake (RN-05) | `min(0.25 × f, 0.03)` si EV > 0; si no, 0 |
| Veredicto (RN-04) | `CON_VALOR` si EV ≥ 0.03; `MARGINAL` si 0 < EV < 0.03; `SIN_VALOR` en otro caso |

Ejemplo: p = 0.50 y cuota 2.20 → EV = +0.10, f = 0.083 y stake = 2.1 % de la banca.

- Devuelve un `record Valor(valorEsperado, stakePct, veredicto)`, con un enum `Veredicto`.
- Redondea el EV y el stake a 3 decimales.
- Decide el veredicto con el EV **ya redondeado**, para que coincida con el número que ve el usuario.

**Para pensar:** ¿por qué solo un cuarto de Kelly y con un tope de 3 %? Kelly completo supone que la probabilidad es exacta, y la del modelo no lo es. Apostar menos protege la banca cuando el modelo se equivoca (RN-16, juego responsable).

## Paso 2. Liquidar en una función

**Archivo:** `service/LiquidacionService.java`, por ahora solo los métodos estáticos `liquidar(...)` y `ganancia(...)`, y el `record Final(golesLocal, golesVisitante, tarjetas, corners)`

`liquidar(mercado, seleccion, linea, final)` devuelve `GANADA`, `PERDIDA` o `null`, según la tabla del contrato:

| Mercado | Gana si |
|---|---|
| 1X2 | LOCAL: gl > gv. EMPATE: gl = gv. VISITA: gl < gv. |
| GOLES | MAS: gl + gv > línea. MENOS: al revés. |
| AMBOS_MARCAN | SI: los dos marcan. NO: al revés. |
| TARJETAS y CORNERS | El total frente a la línea. Si las estadísticas aún no están (`null`), devuelve `null`: la apuesta sigue pendiente. |

`ganancia(resultado, monto, cuota)`: `monto × (cuota − 1)` si ganó y `−monto` si perdió, con 2 decimales (RN-07).

**Pista:** un `switch` con flechas (`case "1X2" -> ...`) queda corto y claro. Todas las líneas terminan en .5, así que nunca hay empate.

Haz ya las pruebas de los pasos 1 y 2 (ver el paso 10): no necesitan nada más.

## Paso 3. Mercados válidos

**Archivo:** `service/Mercados.java`

Es la misma clase que en `ms-cuotas`: la tabla de mercados, selecciones y líneas, más las casas. Cada servicio tiene su propia copia: los microservicios no comparten código.

## Paso 4. Entidades y repositorios

**Carpetas:** `entity/` y `repository/`

- Enums:
  - `Resultado`: `GANADA`, `PERDIDA`;
  - `EstadoPick`: `ABIERTO`, `LIQUIDADO`;
  - `dto/EstadoApuesta`: `PENDIENTE`, `LIQUIDADA`, solo como filtro.
- **`Apostable`:** una interfaz con `getPartidoId()`, `getMercado()`, `getSeleccion()` y `getLinea()`. La implementan las dos entidades, y así un pick y una apuesta se liquidan con el mismo código.
- **`Pick`**, tabla `pick`: una candidata a apuesta del día.
  - Columnas: `PicFecha`, `PicIdPartido`, `PicDescripcion`, `PicMercado`, `PicSeleccion`, `PicLinea`, `PicCasa`, `PicCuota`, `PicProbabilidad`, `PicValorEsperado`, `PicStakePct`, `PicEsApuestaDelDia`, `PicEstado` y `PicResultado`. Lleva un índice en la fecha.
  - Métodos: `marcarComoApuestaDelDia()` y `liquidar(resultado)`.
- **`ApuestaUsuario`**, tabla `apuesta_usuario`.
  - Columnas: `ApuIdUsuario`, `ApuIdPartido`, `ApuDescripcion` (130), `ApuMercado`, `ApuSeleccion`, `ApuLinea`, `ApuCasa`, `ApuCuota`, `ApuMonto`, `ApuFecha`, `ApuResultado` (nulo mientras está pendiente) y `ApuGanancia`. Lleva un índice en el usuario.
  - Método: `liquidar(resultado, ganancia)`.
- **Repositorios** (métodos derivados):
  - `PickRepository`: `findByFecha`, `findByEstado` y `findByEsApuestaDelDiaTrueAndEstadoAndFechaGreaterThanEqualOrderByFechaDesc`;
  - `ApuestaUsuarioRepository`: `findByUsuarioIdOrderByFechaDescIdDesc`, la misma con `AndResultadoIsNull` y con `AndResultadoIsNotNull`, y `findByResultadoIsNull`.

## Paso 5. Clientes Feign y errores remotos

**Archivos:** `client/` (4 interfaces), los DTO de entrada en `dto/` y `service/Remoto.java`

| Cliente | Llamada | DTO |
|---|---|---|
| `CatalogoClient` | `GET /partidos/{id}` y `GET /partidos?fecha=&estado=` | `PartidoDTO`, con un método `descripcion()` que devuelve «Local vs Visitante» |
| `PrediccionesClient` | `GET /partidos/{id}` | `PrediccionDTO`, con su lista de mercados y su muestra |
| `CuotasClient` | `GET /partidos/{id}/cuotas` | `CuotaMercadoDTO` |
| `EstadisticasClient` | `GET /partidos/{id}/estadisticas` | `EstadisticasPartidoDTO`, con `tarjetas()` (amarillas + rojas de ambos) y `corners()` |

`Remoto.llamar(servicio, partidoId, () -> cliente.metodo(...))` envuelve cada llamada y traduce los errores de Feign. Así no repites el `try/catch` en cada servicio:

| Excepción | Respuesta |
|---|---|
| `FeignException.NotFound` | 404 `PARTIDO_NO_ENCONTRADO` |
| `FeignException.Conflict` | 409 `SIN_DATOS` (el modelo aún no tiene datos) |
| cualquier otra `FeignException` | 503 `SERVICIO_NO_DISPONIBLE` |

Nunca se inventa un dato que falta (RNF-04).

## Paso 6. Evaluar una cuota

**Archivos:** `ValorService.evaluacion`, `dto/EvaluacionRequest.java` y `dto/EvaluacionDTO.java`

1. Valida el mercado, la selección, la línea y la casa, y que la cuota sea mayor que 1.
2. Pide la predicción a `ms-predicciones` y busca la probabilidad de esa selección. Compara las líneas como números: 2.5 y 2.50 son la misma.
3. `evaluar(p, cuota)` y responde con la probabilidad, la cuota justa, el EV, el stake y el veredicto.

**Para pensar (RN-APU-01):** el usuario solo manda la cuota y la probabilidad sale siempre del modelo. Por eso evaluar funciona aunque `ms-cuotas` esté caído.

## Paso 7. Las apuestas del usuario

**Archivos:** `service/MisApuestasService.java`, `controller/MisApuestasController.java` y sus DTO (`ApuestaRequest`, `ApuestaDTO`, `RendimientoDTO`, `RendimientoMercadoDTO`)

- **`registrar(usuarioId, datos)`:**
  1. Valida la selección y la casa.
  2. Pide el partido a `ms-catalogo` y guarda su descripción con la apuesta (RN-APU-05). Así el historial no depende de `ms-catalogo`.
  3. El monto debe ser mayor que 0 y la cuota mayor que 1: usa las validaciones del DTO.
- **`listar(usuarioId, estado)`:** solo las del usuario, de la más reciente a la más antigua.
- **`rendimiento(usuarioId)`:** cuenta todas las apuestas, pero el monto, el beneficio y el ROI salen **solo de las liquidadas** (RN-APU-07).
  - `ROI = beneficio / monto apostado`, en total y por mercado.
  - Agrupa por mercado con `Collectors.groupingBy(..., TreeMap::new, ...)`, que deja los mercados ordenados.
- El usuario sale siempre de `@RequestHeader("X-Usuario-Id")`, nunca del JSON (RN-14). `POST` responde 201.

## Paso 8. La apuesta del día

**Archivos:** `service/ApuestaDelDiaService.java`, `controller/RecomendacionController.java` y sus DTO (`PickDTO`, `ApuestaDelDiaDTO`, `PickHistorialDTO`, `HistorialDTO`)

**`apuestaDelDia(fecha, recalcular)`**, `synchronized`:

1. Sin fecha, usa hoy en Lima: `LocalDate.now(ZoneId.of("America/Lima"))`.
2. Si ya hay picks guardados para ese día y no se pidió `recalcular`, devuélvelos (RN-APU-02).
3. Si alguno ya está liquidado, no se puede recalcular: responde 409 `YA_LIQUIDADA`.
4. Si no, genera las candidatas, marca la de mayor EV como apuesta del día, borra las anteriores y guarda las nuevas (RN-APU-03).

**Las candidatas** (RN-06):

1. Pide a `ms-catalogo` los partidos `PROGRAMADO` del día.
2. Para cada partido, pide la predicción. Sáltalo si no hay predicción (404 o 409) o si algún equipo tiene menos de 5 partidos de muestra. Si `ms-predicciones` no responde, 503.
3. Pide sus cuotas a `ms-cuotas`. Si no responde, 503: sin cuotas no hay apuesta del día (RN-APU-04).
4. Para cada cuota de **bet365 o betano** con un valor entre 1.50 y 2.60:
   - calcula `p_mercado`, la probabilidad sin margen de la **misma casa, mercado y línea** (la fórmula de `ms-cuotas`). Si a esa casa le falta una selección del mercado, la cuota no es candidata;
   - mezcla: `p = 0.5 × p_modelo + 0.5 × p_mercado`;
   - es candidata si `p ≥ 0.42` y `EV ≥ 0.03`.
5. Sin candidatas, la respuesta trae `pick: null` y el mensaje «Hoy no hay apuesta del día: ninguna apuesta supera los umbrales. No apostar también es una decisión.» (RN-16).

**Para pensar (honestidad, RN-15):** ¿por qué mezclar con el mercado? Usado solo, el modelo de Poisson daba apuestas del día con EV de 20–30 %, y eso no es valor real: es el modelo exagerando una racha de 10 partidos. El mercado lo modera (sección 7.1).

**Programada:** `@Scheduled(cron = "0 0 9 * * *", zone = "America/Lima")` llama a `apuestaDelDia(null, false)`. Si falla, lo anota en el log.

**`historial(dias)`** (RN-APU-08):
- solo las apuestas del día ya liquidadas, con un stake fijo de 1 unidad;
- una ganada suma `cuota − 1` y una perdida resta 1;
- `ROI = beneficio / picks`.

## Paso 9. La liquidación completa

**Archivos:** el resto de `LiquidacionService` y `controller/InternoController.java`

`liquidarPendientes()`, `synchronized` y **sin** `@Transactional`, porque espera a otros servicios:

1. Junta los picks `ABIERTO` y las apuestas sin resultado.
2. Consulta **una sola vez** cada partido involucrado a `ms-catalogo`, y sus estadísticas solo si alguna apuesta de ese partido es de tarjetas o córners.
3. Si el partido no está `FINALIZADO`, o el servicio no responde, se salta: se reintenta en la próxima liquidación.
4. Liquida con las funciones del paso 2 y guarda.
5. Devuelve cuántos picks y apuestas se liquidaron y cuántos siguen pendientes.

- Programada a las 06:00 de Lima (RN-APU-06).
- `POST /interno/liquidar` permite lanzarla a mano. Es interna: el gateway la bloquea desde fuera.

## Paso 10. Errores y pruebas

**Archivos:** `exception/` y `src/test/java/pe/edu/unu/apuestas/apuestas/`

`ManejadorErrores` cubre:
- `ApiException`, con su propio código;
- las validaciones, los parámetros inválidos y un JSON roto (400);
- la falta de la cabecera de usuario (401);
- cualquier otra excepción (500).

**Pruebas unitarias** (hazlas junto con los pasos 1 y 2):

| Archivo | Caso | Qué comprueba |
|---|---|---|
| `ValorServiceTest` | CB-05 | p = 0.50 y cuota 2.20 → EV 0.10, stake ≤ 3 % y CON_VALOR |
| | CB-06 | p = 0.40 y cuota 2.20 → EV −0.12, stake 0 y SIN_VALOR |
| | Marginal y tope | Un caso MARGINAL y el tope de 3 % del stake |
| `LiquidacionServiceTest` | CB-07 | 2-1 con MAS 2.5 → GANADA y ganancia = monto × (cuota − 1) |
| | Perdida | Una perdida resta el monto |
| | Tabla | Cada mercado según la tabla |
| | Sin estadísticas | Tarjetas y córners esperan |

**Prueba de integración** `ApuestasTest`: `@SpringBootTest`, `@Transactional` y MockMvc contra H2, con `@MockitoBean` para los **cuatro** clientes Feign.

| Caso | Qué comprueba |
|---|---|
| Evaluación | Con la probabilidad del modelo, más las inválidas |
| Apuesta del día | Es la candidata de mayor valor |
| Reutilización | Una apuesta del día guardada se reutiliza |
| Sin candidatas | Responde con el mensaje |
| `ms-cuotas` caído | 503 |
| Historial | Cuenta la apuesta del día liquidada |
| Liquidación y rendimiento | Registrar, liquidar y que el rendimiento sea solo del usuario: una ganada (+22.00), una perdida (−10.00) y una de tarjetas que sigue pendiente; las apuestas de otro usuario no aparecen |
| Apuestas inválidas | Se rechazan |

## Terminado cuando

- [ ] `POST /evaluaciones` responde aunque `ms-cuotas` esté apagado.
- [ ] La apuesta del día se genera, se guarda y se reutiliza; sin candidatas lo dice.
- [ ] La liquidación deja pendientes las de tarjetas y córners sin estadísticas.
- [ ] El rendimiento solo muestra las apuestas del usuario del token.
- [ ] `./mvnw test` pasa.

Si no alcanza el tiempo, recupera solo este servicio desde la solución (ver la guía general).
