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

## Tareas

- [ ] Entidad y repositorio. Mock de `GET /partidos/{id}/cuotas` el día 1.
- [ ] Upsert interno.
- [ ] Consulta de la última cuota por selección.
- [ ] `CuotaService.quitarMargen()` y su endpoint.
- [ ] Cuota manual con validación y Feign.
- [ ] Pruebas: CB-04 (cuota ≤ 1 lanza `CuotaInvalidaException`) y una de margen con el ejemplo de arriba.
- [ ] Dockerfile.

## Pistas

- Guarden los valores como `BigDecimal` en la entidad, pero hagan los cálculos con `double`: es más simple y la precisión alcanza.
- Pinnacle tiene el margen más bajo del mercado. Sus probabilidades sin margen son la mejor referencia de la "probabilidad real" que tendrán. Úsenla para contrastar el modelo.
