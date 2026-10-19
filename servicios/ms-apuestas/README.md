# ms-apuestas · Integrante 2 · puerto 8087 · BD `apuestas_db`

**Responsabilidad:** convierte probabilidades y cuotas en decisiones. Evalúa el valor esperado de una cuota y genera la **apuesta del día**. También guarda las apuestas de cada usuario, las liquida cuando termina el partido y calcula el rendimiento.

## Spring Initializr

- Dependencias: **Spring Web**, **Spring Data JPA**, **PostgreSQL Driver**, **Validation**, **OpenFeign**, **Eureka Discovery Client**, **Actuator** y springdoc.

## Fórmulas

| Concepto | Fórmula | Ejemplo (p = 0.50, cuota = 2.20) |
|---|---|---|
| Valor esperado (EV) | `p × cuota − 1` | 0.50 × 2.20 − 1 = **+0.10** (+10 %) |
| Kelly | `f = (p × cuota − 1) / (cuota − 1)` | 0.10 / 1.20 = 0.083 |
| Stake sugerido | `min(0.25 × f, 0.03)` si EV > 0; si no, 0 | min(0.021, 0.03) = **2.1 % de la banca** |
| Veredicto | `CON_VALOR` si EV ≥ 0.03; `MARGINAL` si 0 < EV < 0.03; `SIN_VALOR` en otro caso | CON_VALOR |

## Endpoints públicos (vía gateway: `/api/apuestas` + ruta, todos con token)

### `POST /evaluaciones`

```json
{ "partidoId": 1532, "mercado": "TARJETAS", "seleccion": "MAS", "linea": 4.5, "casa": "betano", "cuota": 2.20 }
```

Busca la probabilidad en `ms-predicciones` (`GET /partidos/{id}`) y responde:

```json
{ "probabilidad": 0.497, "cuotaJusta": 2.01, "valorEsperado": 0.093, "stakePct": 0.019, "veredicto": "CON_VALOR" }
```

### `GET /apuesta-del-dia?fecha=2026-10-24`

Si no se pasa `fecha`, toma el día de hoy en Lima. Si ya hay apuesta del día guardada para esa fecha, la devuelve. Con `&recalcular=true` la vuelve a generar.

**Algoritmo:**
1. Pedir a `ms-catalogo` los partidos `PROGRAMADO` de la fecha.
2. Para cada uno, pedir la predicción a `ms-predicciones` y las cuotas de **bet365 y betano** a `ms-cuotas`. Pinnacle no se usa porque el equipo apuesta en bet365 y Betano.
3. Calcular el EV de cada cuota con la probabilidad de su mercado, selección y línea.
4. Son candidatas las que cumplen: cuota entre 1.50 y 2.60, probabilidad ≥ 0.42, EV ≥ 0.03, y al menos 5 partidos de muestra por equipo.
5. Guardar todas las candidatas como `pick`. La de mayor EV se marca con `PicEsApuestaDelDia = true`.

```json
{ "fecha": "2026-10-24",
  "pick": { "id": 88, "partidoId": 1532, "partido": "Arsenal vs Chelsea", "mercado": "GOLES", "seleccion": "MAS",
            "linea": 2.5, "casa": "bet365", "cuota": 2.00, "probabilidad": 0.531, "valorEsperado": 0.062, "stakePct": 0.016 },
  "otrasCandidatas": 3,
  "mensaje": null }
```

Si no hay candidatas, `pick` es `null` y `mensaje` dice: "Hoy no hay apuesta del día: ninguna apuesta supera los umbrales. No apostar también es una decisión."

También corre sola con `@Scheduled` a las **09:00 (hora de Lima)**.

### `GET /picks/historial?dias=30`

```json
{ "picks": 21, "ganados": 11, "perdidos": 10, "beneficioUnidades": 1.35, "roi": 0.064,
  "ultimos": [ { "fecha": "2026-10-24", "partido": "Arsenal vs Chelsea", "mercado": "GOLES", "seleccion": "MAS", "linea": 2.5, "cuota": 2.00, "resultado": "GANADA" } ] }
```

Solo cuenta las apuestas del día ya liquidadas, con stake plano de 1 unidad.

### `POST /mis-apuestas`

```json
{ "partidoId": 1532, "mercado": "1X2", "seleccion": "LOCAL", "linea": null, "casa": "bet365", "cuota": 2.10, "monto": 20.00 }
```

Lee `X-Usuario-Id`. Pide el partido a `ms-catalogo` para guardar la descripción "Arsenal vs Chelsea", y responde **201** con la apuesta.

### `GET /mis-apuestas?estado=PENDIENTE` (o `LIQUIDADA`)

Lista solo las apuestas **del usuario del token**, de la más reciente a la más antigua.

### `GET /mis-apuestas/rendimiento`

```json
{ "apuestas": 12, "pendientes": 2, "ganadas": 6, "perdidas": 4, "montoApostado": 200.00, "beneficio": 18.40, "roi": 0.092,
  "porMercado": [ { "mercado": "1X2", "apuestas": 5, "beneficio": -4.00, "roi": -0.08 } ] }
```

`roi = beneficio / monto apostado`. Solo cuenta las apuestas liquidadas.

## Liquidación (`POST /interno/liquidar` y `@Scheduled` a las 06:00 de Lima)

Recorre los picks y las apuestas pendientes. Para cada uno pide el partido a `ms-catalogo`, y si está `FINALIZADO`:

| Mercado | Gana si |
|---|---|
| 1X2 | LOCAL: gl > gv. EMPATE: gl = gv. VISITA: gl < gv. |
| GOLES | MAS: gl + gv > línea. MENOS: al revés. |
| AMBOS_MARCAN | SI: gl ≥ 1 y gv ≥ 1. NO: al revés. |
| TARJETAS | Total de amarillas + rojas de ambos equipos frente a la línea (`ms-estadisticas` `GET /partidos/{id}/estadisticas`) |
| CORNERS | Total de córners frente a la línea (mismo endpoint) |

- Si gana: `ganancia = monto × (cuota − 1)`. Si pierde: `ganancia = −monto`.
- Todas las líneas terminan en .5, así que no hay empate (*push*).
- Si las estadísticas aún no están, la apuesta queda pendiente para el día siguiente.

## Tareas

- [ ] Entidades `Pick` y `ApuestaUsuario` (sección 6.6).
- [ ] `ValorService.evaluar()` y `POST /evaluaciones`.
- [ ] Apuesta del día y su `@Scheduled`.
- [ ] Mis apuestas y rendimiento.
- [ ] Liquidación.
- [ ] Pruebas: CB-05, CB-06 y CB-07, y que el rendimiento solo muestra las apuestas del usuario del token.
- [ ] Dockerfile.

## Pistas

- **Mejora opcional (si sobra tiempo):** combinar el modelo con el mercado, `p = 0.5 × p_modelo + 0.5 × p_pinnacle`, usando `GET /partidos/{id}/probabilidades-mercado?casa=pinnacle`. La rama `main` encontró que el mercado afilado (Pinnacle) predice mejor que un modelo propio, y que la combinación mejora al modelo solo.
- Si `ms-cuotas` no responde, la evaluación manual (`POST /evaluaciones`) sigue funcionando, porque la cuota la manda el usuario. Solo la apuesta del día devuelve 503.
