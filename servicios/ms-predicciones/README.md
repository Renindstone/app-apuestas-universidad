# ms-predicciones · Integrante 2 · puerto 8086 · sin BD

**Responsabilidad:** calcula la probabilidad de cada mercado de un partido con un **modelo de Poisson** a partir de los promedios de `ms-estadisticas`. No guarda nada: cada petición pide los datos y calcula. Es el corazón "matemático" del sistema.

## Spring Initializr

- Dependencias: **Spring Web**, **OpenFeign**, **Eureka Discovery Client**, **Actuator** y springdoc.
- No usa base de datos. Si hace falta, se puede cachear con `@Cacheable` y **Spring Cache**.

## Datos que pide (Feign)

| A | Endpoint | Para qué |
|---|---|---|
| ms-catalogo | `GET /partidos/{id}` | Liga, equipos y árbitro |
| ms-estadisticas | `GET /ligas/{ligaId}/promedios` | Medias de la liga (μ) |
| ms-estadisticas | `GET /equipos/{localId}/promedios?condicion=LOCAL&ultimos=10` | El local jugando en casa |
| ms-estadisticas | `GET /equipos/{visitanteId}/promedios?condicion=VISITANTE&ultimos=10` | El visitante jugando fuera |
| ms-estadisticas | `GET /arbitros/{arbitroId}/perfil` | Solo si el partido tiene árbitro |

## El modelo, paso a paso

**1. Valor esperado de una estadística** (la misma función sirve para goles, córners, tarjetas y tiros):

```
μL = liga.local.<stat>          μV = liga.visitante.<stat>
ataqueL  = local.aFavor.<stat>      / μL     defensaV = visitante.enContra.<stat> / μL
ataqueV  = visitante.aFavor.<stat>  / μV     defensaL = local.enContra.<stat>     / μV
λL = μL × ataqueL × defensaV
λV = μV × ataqueV × defensaL
```

**2. Suavizado** para equipos con pocos partidos. Cada factor se acerca a 1 (la media de la liga) según la muestra:

```
factor' = (n × factor + 5 × 1) / (n + 5)        n = partidos de la muestra
```

**3. Goles.** Matriz de Poisson con i, j de 0 a 10: `P(i, j) = Pois(i; λL) × Pois(j; λV)`, con `Pois(k; λ) = e^−λ λ^k / k!`.

| Mercado | Probabilidad |
|---|---|
| 1X2 LOCAL / EMPATE / VISITA | suma de P(i, j) con i > j, i = j o i < j |
| GOLES MAS línea | suma de P(i, j) con i + j > línea; MENOS = 1 − MAS |
| AMBOS_MARCAN SI | (1 − e^−λL) × (1 − e^−λV); NO = 1 − SI |

**4. Córners.** λ = λL + λV (con la stat `corners`). Entonces `P(MAS línea) = 1 − Σ_{k ≤ línea} Pois(k; λ)`.

**5. Tarjetas con factor árbitro.** λ = (λL + λV) con la stat `tarjetas`, multiplicado por el factor del árbitro:

```
factorArbitro = [(n × tarjetasPorPartido + 10 × mediaLigaTarjetas) / (n + 10)] / mediaLigaTarjetas
```

Si no hay árbitro (LaLiga o Bundesliga), el factor vale 1. El 10 hace que un árbitro con pocos partidos se parezca a la media.

**6. Cuota justa** = 1 / probabilidad.

## Endpoint (vía gateway: `/api/predicciones` + ruta)

### `GET /partidos/{partidoId}`

```json
{
  "partidoId": 1532, "partido": "Arsenal vs Chelsea", "fechaHora": "2026-10-24T14:00:00Z",
  "esperados": { "golesLocal": 1.82, "golesVisitante": 0.97, "corners": 10.4, "tarjetas": 4.6, "tiros": 24.3 },
  "factorArbitro": 1.08,
  "muestra": { "partidosLocal": 10, "partidosVisitante": 10 },
  "mercados": [
    { "mercado": "1X2", "seleccion": "LOCAL", "linea": null, "probabilidad": 0.552, "cuotaJusta": 1.81 },
    { "mercado": "GOLES", "seleccion": "MAS", "linea": 2.5, "probabilidad": 0.531, "cuotaJusta": 1.88 },
    { "mercado": "TARJETAS", "seleccion": "MAS", "linea": 4.5, "probabilidad": 0.497, "cuotaJusta": 2.01 }
  ],
  "explicacion": [
    "Arsenal marca 2.1 goles por partido en casa (media de la liga: 1.55)",
    "M Oliver saca 4.3 tarjetas por partido (media de la liga: 3.9) → factor 1.08"
  ]
}
```

Incluye **todos** los mercados y líneas de la tabla del README raíz. Si `ms-catalogo` o `ms-estadisticas` no responden, devuelve **503** `{"error":"SERVICIO_NO_DISPONIBLE","detalle":"ms-estadisticas no responde"}`.

## Reglas de negocio

Reglas específicas de este servicio, con el mismo código que en la [sección 4.5 de la documentación](../../docs/Documentacion.md#45-reglas-de-negocio). Si cambian, se actualizan los dos documentos y se avisa en el daily.

| ID | Regla | Relación |
|---|---|---|
| RN-PRE-01 | No guarda nada: en cada petición pide los datos y vuelve a calcular. | RF-08 |
| RN-PRE-02 | El valor esperado de cada estadística combina la media de la liga con el ataque de un equipo y la defensa del otro. El local se mide jugando en casa y el visitante jugando fuera, en sus últimos 10 partidos. | RF-08 |
| RN-PRE-03 | Si un equipo tiene pocos partidos, cada factor se acerca a la media de la liga: `factor' = (n × factor + 5) / (n + 5)`. | RF-08 |
| RN-PRE-04 | Los mercados de goles, 1X2 y ambos marcan salen de una matriz de Poisson de 0 a 10 goles por equipo. Los de córners y tarjetas usan Poisson sobre el total del partido. | RF-08 · CB-03 |
| RN-PRE-05 | Las tarjetas se ajustan con el factor árbitro. Ese factor se acerca a 1 cuando el árbitro tiene pocos partidos (con un peso de 10), y vale 1 si el partido no tiene árbitro. | RF-07 · RN-08 |
| RN-PRE-06 | La cuota justa de una selección es `1 / probabilidad`. La respuesta trae todos los mercados y líneas de RN-09, con una explicación de dónde sale cada número. | RF-08 · RN-15 |
| RN-PRE-07 | Si `ms-catalogo` o `ms-estadisticas` no responden, devuelve 503: nunca inventa una probabilidad. | RNF-04 |

Reglas generales que aplica: RN-08 (factor árbitro solo en la Premier), RN-09 (mercados válidos) y RN-15 (honestidad).

## Tareas

- [ ] Mock del endpoint el día 1, para que `ms-apuestas` y la web puedan avanzar.
- [ ] `PoissonService`: `pmf`, matriz, 1X2, más/menos y ambos marcan.
- [ ] Función genérica `esperado(stat)` con suavizado.
- [ ] Clientes Feign y armado de la respuesta.
- [ ] Factor árbitro, córners y tarjetas.
- [ ] Pruebas: CB-03 (el 1X2 suma 1) y un caso con λ conocidos (λ = 1 → P(0) = 0.3679).
- [ ] Dockerfile.

## Pistas

- Para no desbordar con `k!`, calculen `Pois(k)` de forma iterativa: `p0 = e^−λ` y `p_k = p_{k−1} × λ / k`.
- **Honestidad:** Poisson subestima la varianza de córners y tarjetas, y no conoce alineaciones ni lesiones. Las casas de apuestas sí. El modelo sirve para aprender y para comparar, no para "ganarle" a bet365. La rama `main` lo comprobó con un backtest de unos 2.100 partidos.
