# ms-cuotas · Integrante 2 · puerto 8085 · BD `cuotas_db`

**Responsabilidad:** guarda las cuotas de cada partido por casa, mercado y selección, y calcula el **margen** de la casa y las **probabilidades sin margen**. Las cuotas de bet365 y Pinnacle llegan desde `ms-ingesta`. Las de Betano las ingresa el usuario a mano, porque no hay una fuente gratuita.

## Spring Initializr

- Dependencias: **Spring Web**, **Spring Data JPA**, **PostgreSQL Driver**, **Validation**, **OpenFeign**, **Eureka Discovery Client**, **Actuator** y springdoc.

## Endpoint interno (solo lo usa `ms-ingesta`)

### `POST /interno/cuotas`

```json
{ "partidoId": 1532, "casa": "bet365", "tipo": "APERTURA",
  "cuotas": [
    { "mercado": "1X2",   "seleccion": "LOCAL",  "linea": null, "valor": 2.10 },
    { "mercado": "1X2",   "seleccion": "EMPATE", "linea": null, "valor": 3.40 },
    { "mercado": "1X2",   "seleccion": "VISITA", "linea": null, "valor": 3.60 },
    { "mercado": "GOLES", "seleccion": "MAS",    "linea": 2.5,  "valor": 1.90 },
    { "mercado": "GOLES", "seleccion": "MENOS",  "linea": 2.5,  "valor": 1.90 } ] }
```

Hace un upsert por (partido, casa, mercado, selección, línea, tipo). Responde **200** `{ "guardadas": 5 }`.

## Endpoints públicos (vía gateway: `/api/cuotas` + ruta)

### `GET /partidos/{partidoId}/cuotas?casa=bet365`

Devuelve la cuota más reciente de cada (casa, mercado, selección, línea). El filtro `casa` es opcional.

```json
[ { "casa": "bet365", "mercado": "1X2", "seleccion": "LOCAL", "linea": null, "valor": 2.10, "tipo": "APERTURA", "capturadaEn": "2026-10-22T10:00:00Z" } ]
```

### `GET /partidos/{partidoId}/probabilidades-mercado?casa=pinnacle&mercado=1X2&linea=`

```json
{ "casa": "pinnacle", "mercado": "1X2", "linea": null, "margen": 0.026,
  "probabilidades": { "LOCAL": 0.468, "EMPATE": 0.276, "VISITA": 0.256 } }
```

Si falta alguna selección del mercado (por ejemplo, solo hay `MAS` sin `MENOS`), responde **409** `{"error":"MERCADO_INCOMPLETO", ...}`.

### `POST /cuotas/manual` (con token)

```json
{ "partidoId": 1532, "casa": "betano", "mercado": "TARJETAS", "seleccion": "MAS", "linea": 4.5, "valor": 1.85 }
```

- Comprueba con **Feign** que el partido existe en `ms-catalogo` (`GET /partidos/{id}`). Si no existe, responde 404.
- Guarda la cuota con `tipo = MANUAL` y `CuoIdUsuario = X-Usuario-Id`, y responde **201**.
- Valida que el mercado y la selección sean válidos (tabla de mercados del README raíz) y que la cuota sea mayor que 1.01.

## Fórmulas

- Probabilidad implícita: `q_i = 1 / cuota_i`.
- Margen: `margen = Σ q_i − 1`. Por ejemplo, 2.10 / 3.40 / 3.60 da 0.476 + 0.294 + 0.278 − 1 = **0.048** (4.8 %).
- Probabilidad sin margen (método multiplicativo): `p_i = q_i / Σ q_j`.

## Reglas de negocio

Reglas específicas de este servicio, con el mismo código que en la [sección 4.5 de la documentación](../../docs/Documentacion.md#45-reglas-de-negocio). Si cambian, se actualizan los dos documentos y se avisa en el daily.

| ID | Regla | Relación |
|---|---|---|
| RN-CUO-01 | Toda cuota es mayor que 1, y las que ingresa el usuario deben ser mayores que 1.01. | RF-10 · CB-04 |
| RN-CUO-02 | Las cuotas de bet365 y Pinnacle solo llegan por la ingesta. Las de Betano solo se ingresan a mano y se guardan con tipo MANUAL y con el ID del usuario que las cargó. | RN-10 · RF-10 |
| RN-CUO-03 | Una cuota manual solo se acepta si el partido existe en `ms-catalogo` (si no, 404) y si el mercado, la selección y la línea son válidos (si no, 400). | RF-10 · RN-09 |
| RN-CUO-04 | La consulta muestra la cuota más reciente de cada casa, mercado, selección y línea. Si la misma cuota se guarda dos veces, se actualiza en lugar de duplicarse. | RF-09 |
| RN-CUO-05 | El margen y las probabilidades sin margen (RN-01 y RN-02) solo se calculan con el mercado completo. Si falta una selección, la respuesta es 409 MERCADO_INCOMPLETO. | RF-09 |

Este servicio implementa las reglas generales RN-01 (margen) y RN-02 (probabilidad sin margen), y aplica RN-09 (mercados válidos) y RN-10 (casas).

## Tareas

- [x] Entidad y repositorio (`entity/Cuota`, con `CHECK(CuoValor > 1)` e índice en `CuoIdPartido`). El mock del día 1 ya no hace falta: el endpoint real está listo.
- [x] Upsert interno: se validan todas las cuotas antes de guardar, así una cuota mala no deja la mitad guardada.
- [x] Consulta de la última cuota por selección.
- [x] `CuotaService.quitarMargen()` y su endpoint. Los mercados válidos están en `service/Mercados`.
- [x] Cuota manual con validación y Feign (`client/CatalogoClient`).
- [x] Pruebas: CB-04 y el margen del ejemplo (`CuotaServiceTest`); upsert, filtro por casa, mercado incompleto, cuota manual (con ms-catalogo simulado) y validaciones (`CuotasTest`, con H2 en memoria).
- [x] Dockerfile.

**Detalles del contrato:**

- `margen` y `probabilidades` van con 3 decimales. Si no hay ninguna cuota de esa casa y mercado, también responde 409 `MERCADO_INCOMPLETO`.
- `POST /interno/cuotas` solo acepta `bet365` y `pinnacle`, y nunca el tipo `MANUAL`. `POST /cuotas/manual` solo acepta `betano` (RN-CUO-02).
- `POST /cuotas/manual` responde 201 con la cuota guardada, en el mismo formato que `GET /partidos/{id}/cuotas`. Sin la cabecera `X-Usuario-Id`, responde 401 `NO_AUTORIZADO`. Si ms-catalogo no responde, 503 `SERVICIO_NO_DISPONIBLE`.
- Una cuota manual de la misma selección y línea reemplaza a la anterior, aunque la cargue otro usuario.
- **Pinnacle:** desde la temporada 2627, football-data ya no trae cuotas de Pinnacle (ver el README de `ms-ingesta`). Para los partidos actuales, `probabilidades-mercado?casa=pinnacle` responde 409.

**Variables de entorno:** `DB_HOST`, `DB_PORT`, `POSTGRES_USER`, `POSTGRES_PASSWORD` y `EUREKA_URL`.

## Pistas

- Guarden los valores como `BigDecimal` en la entidad, pero hagan los cálculos con `double`: es más simple y la precisión alcanza.
- Pinnacle tiene el margen más bajo del mercado. Sus probabilidades sin margen son la mejor referencia de la "probabilidad real" que tendrán. Úsenla para contrastar el modelo.
