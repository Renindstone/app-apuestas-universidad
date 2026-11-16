# Guía paso a paso: ms-cuotas

Guarda las cuotas de las casas de apuestas y calcula dos cosas: el **margen** de la casa y la **probabilidad sin margen** de cada selección. Estas probabilidades son las que el mercado «cree», y `ms-apuestas` las compara con las del modelo.

- Contrato: [README.md](README.md) (endpoints, fórmulas y reglas RN-CUO-01 a RN-CUO-05). Tabla: sección 6.5 de la documentación. Mercados válidos: tabla del [README raíz](../../../README.md).
- Guía general y orden de trabajo: [docs/guia-paso-a-paso.md](../../../docs/guia-paso-a-paso.md).
- Si es tu primer servicio con base de datos, mira antes [la guía de ms-usuarios](../../renzo/ms-usuarios/GUIA.md).
- Todos los archivos van en `src/main/java/pe/edu/unu/apuestas/cuotas/`. Para ver uno en la solución:
  `git show universidad/codigo-completo:servicios/joseph/ms-cuotas/src/main/java/pe/edu/unu/apuestas/cuotas/<archivo>`

## Paso 0. Lo que ya trae el esqueleto

- **`pom.xml`:** Web, Data JPA, PostgreSQL, Validation, OpenFeign, Eureka Client, Actuator, `springdoc` y H2 para las pruebas.
- **`application.yml`:** `cuotas_db` en `localhost:5441`.
- **La clase principal** ya trae `@EnableFeignClients`, para el cliente del paso 6.

Levanta PostgreSQL y luego `./mvnw spring-boot:run`. Debe arrancar en el puerto 8085.

## Paso 1. Las fórmulas, antes que nada

**Archivo:** `service/CuotaService.java`, solo el método estático `quitarMargen`

Es lógica pura, sin base de datos: empieza por aquí y pruébala primero (paso 8).

- Probabilidad implícita: `q = 1 / cuota`.
- Margen de la casa (RN-01): `margen = Σq − 1`.
- Probabilidad sin margen, método multiplicativo (RN-02): `p = q / Σq`.

Ejemplo: 2.10 / 3.40 / 3.60 → q = 0.476 + 0.294 + 0.278 = 1.048 → margen 0.048 (4.8 %).

Haz que `quitarMargen(List<Double>)` devuelva un `record SinMargen(double margen, List<Double> probabilidades)` y que lance `CuotaInvalidaException` (400 `CUOTA_INVALIDA`) si alguna cuota es menor o igual que 1 (CB-04).

**Para pensar:** ¿por qué la suma de las probabilidades implícitas pasa de 1? Ese exceso es la ganancia de la casa. Quitarlo es necesario para comparar con el modelo, cuyas probabilidades sí suman 1.

## Paso 2. Mercados válidos

**Archivo:** `service/Mercados.java`

Una clase de utilidad (constructor privado y métodos estáticos) con la tabla de mercados del README raíz:

| Mercado | Selecciones | Líneas |
|---|---|---|
| `1X2` | LOCAL, EMPATE, VISITA | ninguna |
| `GOLES` | MAS, MENOS | 1.5, 2.5, 3.5 |
| `AMBOS_MARCAN` | SI, NO | ninguna |
| `TARJETAS` | MAS, MENOS | 3.5, 4.5, 5.5 |
| `CORNERS` | MAS, MENOS | 8.5, 9.5, 10.5 |

- `selecciones(mercado, linea)`: devuelve las selecciones del mercado. Lanza 400 si el mercado no existe, si lleva línea y no debía, o si la línea no es una de las suyas.
- `validar(mercado, seleccion, linea)` y `validarCasa(casa)`. Las casas válidas son `bet365`, `pinnacle` y `betano`.

**Pista:** compara las líneas con `compareTo`, no con `equals`. Para `BigDecimal`, `2.5` y `2.50` no son `equals`.

## Paso 3. La entidad y el repositorio

**Archivos:** `entity/TipoCuota.java`, `entity/Cuota.java` y `repository/CuotaRepository.java`

- `TipoCuota`: `APERTURA`, `CIERRE` y `MANUAL`.
- `Cuota`, tabla `cuota`:

| Columna | Detalle |
|---|---|
| `CuoIdCuota` | id |
| `CuoIdPartido` | el ID de `ms-catalogo`, sin clave foránea |
| `CuoCasa`, `CuoMercado`, `CuoSeleccion` | textos |
| `CuoLinea` | `precision = 4, scale = 2`, nula en 1X2 y AMBOS_MARCAN |
| `CuoValor` | `precision = 6, scale = 2`, con un CHECK `"CuoValor" > 1` (RN-CUO-01) |
| `CuoTipo` | enum |
| `CuoIdUsuario` | solo en las MANUAL |
| `CuoCapturadaEn` | `Instant` |

- El CHECK se declara en `@Table(check = @CheckConstraint(name = "CK_CuoValor", constraint = "\"CuoValor\" > 1"))`. Las comillas van porque PostgreSQL guarda el nombre con mayúsculas.
- Un método `actualizar(valor, usuarioId)` pone el valor y `capturadaEn = Instant.now()`.
- Un método `clave()` que devuelva `casa|mercado|seleccion|linea`, con la línea normalizada con `stripTrailingZeros()`.
- Repositorio:
  - `findFirstByPartidoIdAndCasaAndTipoAndMercadoAndSeleccionAndLinea(...)` para el upsert. Si la línea es `null`, Spring Data genera `is null` solo;
  - `findByPartidoIdOrderByCapturadaEnDescIdDesc` y la misma con `AndCasa`.

## Paso 4. Guardar las cuotas de la ingesta

**Archivos:** `dto/CuotasRequest.java`, `dto/GuardadasDTO.java`, `CuotaService.guardar` y `controller/InternoController.java`

- `CuotasRequest`: `partidoId`, `casa`, `tipo` y una lista de `Item(mercado, seleccion, linea, valor)`, con validaciones.
- `guardar(datos)`, con `@Transactional`:
  1. Por la ingesta solo llegan `bet365` y `pinnacle`, y nunca con tipo `MANUAL` (RN-CUO-02). Si no, 400.
  2. **Valida todas las cuotas antes de guardar ninguna:** así una cuota mala no deja la mitad guardada.
  3. Upsert de cada una: si ya existe la misma (partido, casa, tipo, mercado, selección, línea), se actualiza; si no, se crea (RN-CUO-04).
- `POST /interno/cuotas` responde `{ "guardadas": n }`.

## Paso 5. Consultar

**Archivos:** `dto/CuotaDTO.java`, `dto/ProbabilidadesMercadoDTO.java` y los métodos de consulta de `CuotaService`

- **La cuota más reciente de cada clave:** trae las cuotas del partido de la más reciente a la más antigua y quédate con la primera de cada `clave()`. Un `LinkedHashMap` con `putIfAbsent` lo hace en una línea.
- **`cuotasDePartido(partidoId, casa)`:** esa lista, con la casa opcional.
- **`probabilidadesMercado(partidoId, casa, mercado, linea)`:**
  1. Valida la casa y el mercado con `Mercados`.
  2. Junta los valores de las selecciones de ese mercado y esa línea.
  3. Si falta alguna selección, responde 409 `MERCADO_INCOMPLETO` (RN-CUO-05).
  4. Si están todas, llama a `quitarMargen` y redondea a 3 decimales.

## Paso 6. Cuota manual con Feign

**Archivos:** `client/CatalogoClient.java`, `dto/CuotaManualRequest.java` y `CuotaService.guardarManual`

```java
@FeignClient(name = "ms-catalogo")
public interface CatalogoClient {
    @GetMapping("/partidos/{id}")
    void partido(@PathVariable Long id);   // solo interesa si existe
}
```

`guardarManual(datos, usuarioId)`:

1. Solo se aceptan cuotas de `betano` (RN-CUO-02).
2. Valida el mercado y la selección, y que el valor sea mayor que 1.01 (RN-CUO-01).
3. Pregunta a `ms-catalogo` si el partido existe (RN-CUO-03):
   - si `ms-catalogo` responde 404, Feign lanza `FeignException.NotFound` → responde 404 `PARTIDO_NO_ENCONTRADO`;
   - si lanza cualquier otra `FeignException` (el servicio está caído), responde 503 `SERVICIO_NO_DISPONIBLE`.
4. Guarda con tipo `MANUAL` y el id del usuario.

**Para pensar:** este método **no** lleva `@Transactional`. No tiene sentido dejar una transacción de base de datos abierta mientras se espera la respuesta de otro servicio por la red.

## Paso 7. Controladores y errores

**Archivos:** `controller/CuotaController.java` y `exception/`

| Ruta | Detalle |
|---|---|
| `GET /partidos/{partidoId}/cuotas?casa=` | |
| `GET /partidos/{partidoId}/probabilidades-mercado?casa=&mercado=&linea=` | `casa` y `mercado` obligatorios |
| `POST /cuotas/manual` | `@RequestHeader("X-Usuario-Id")`, `@ResponseStatus(HttpStatus.CREATED)` |

Excepciones: `ApiException`, `CuotaInvalidaException` (400 `CUOTA_INVALIDA`) y `ManejadorErrores`. Este último cubre además las validaciones, los parámetros inválidos, la falta de cabecera (401) y el 500.

**Comprueba:**

```bash
curl -s -X POST localhost:8085/interno/cuotas -H 'Content-Type: application/json' -d '{"partidoId":1,"casa":"bet365","tipo":"APERTURA","cuotas":[
  {"mercado":"1X2","seleccion":"LOCAL","linea":null,"valor":2.10},
  {"mercado":"1X2","seleccion":"EMPATE","linea":null,"valor":3.40},
  {"mercado":"1X2","seleccion":"VISITA","linea":null,"valor":3.60}]}'
curl -s 'localhost:8085/partidos/1/probabilidades-mercado?casa=bet365&mercado=1X2'   # margen 0.048
```

## Paso 8. Pruebas

**Archivos:** `src/test/java/pe/edu/unu/apuestas/cuotas/service/CuotaServiceTest.java` y `.../controller/CuotasTest.java`

- **`CuotaServiceTest`** es unitaria, sobre `quitarMargen`:
  - CB-04: `[2.0, 0.9, 3.5]` lanza `CuotaInvalidaException`;
  - el ejemplo 2.10 / 3.40 / 3.60 da un margen de 0.048 y probabilidades que suman 1.
- **`CuotasTest`** va por HTTP contra H2, con `@SpringBootTest` y `@Transactional`. `ms-catalogo` se reemplaza con `@MockitoBean CatalogoClient catalogo`: así decides en cada prueba si el partido existe (`doThrow(...)`).

| Caso | Qué comprueba |
|---|---|
| Upsert | La misma cuota se actualiza, no se duplica |
| Filtro | Por casa |
| Margen | Margen y probabilidades sin margen |
| Incompleto | Mercado incompleto da 409 |
| Manual | Se guarda como MANUAL con el usuario |
| Partido inexistente | Una cuota manual da 404 |
| Validaciones | Las de la cuota manual y las de la ingesta |

## Terminado cuando

- [ ] La ingesta guarda cuotas sin duplicarlas.
- [ ] `probabilidades-mercado` da el margen y las probabilidades del ejemplo.
- [ ] Una cuota manual se valida contra `ms-catalogo`.
- [ ] `./mvnw test` pasa.

Si no alcanza el tiempo, recupera solo este servicio desde la solución (ver la guía general).
