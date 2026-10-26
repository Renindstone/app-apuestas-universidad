// =============================================================
// Datos de prueba del gateway simulado. Imitan el JSON de los
// contratos (servicios/<integrante>/<servicio>/README.md) y viven en memoria:
// se reinician cada vez que se arranca el simulador.
// =============================================================

export const HOY = '2026-10-24';
const ZONA_LIMA = 'America/Lima';

// ---------- Utilidades numéricas ----------

const r2 = x => Math.round(x * 100) / 100;
const r3 = x => Math.round(x * 1000) / 1000;

// Ruido determinista entre -1 y 1 a partir de un texto: así las cuotas simuladas no cambian entre recargas.
function ruido(texto) {
  let h = 2166136261;
  for (const c of texto) { h ^= c.charCodeAt(0); h = Math.imul(h, 16777619); }
  return ((h >>> 0) % 2001) / 1000 - 1;
}

// ---------- Formatos (RN-WEB-03: todas las horas en hora de Lima) ----------

// "09:00": hora de Lima de un instante ISO en UTC.
export function horaLima(iso) {
  return new Intl.DateTimeFormat('es-PE', { timeZone: ZONA_LIMA, hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).format(new Date(iso));
}

// "sábado 24 de octubre" a partir de una fecha "2026-10-24" (un día de Lima).
export function fechaLarga(fecha) {
  return new Intl.DateTimeFormat('es-PE', { timeZone: ZONA_LIMA, weekday: 'long', day: 'numeric', month: 'long' }).format(new Date(fecha + 'T17:00:00Z')).replace(',', '');
}

// "24 oct" para listas.
export function fechaCorta(fecha) {
  return new Intl.DateTimeFormat('es-PE', { timeZone: ZONA_LIMA, day: 'numeric', month: 'short' }).format(new Date(fecha + 'T17:00:00Z')).replace('.', '');
}

// "24/10 08:00": fecha y hora de Lima de un instante ISO.
export function fechaHoraLima(iso) {
  const f = new Intl.DateTimeFormat('es-PE', { timeZone: ZONA_LIMA, day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).formatToParts(new Date(iso));
  const v = t => f.find(p => p.type === t).value;
  return `${v('day')}/${v('month')} ${v('hour')}:${v('minute')}`;
}

export const pct = (x, d = 1) => `${(x * 100).toFixed(d)} %`;
export const cuota = x => (x == null ? '—' : x.toFixed(2));
export const soles = x => `${x < 0 ? '−' : ''}S/ ${Math.abs(x).toFixed(2)}`;
// "+5.6 %" o "−2.0 %": porcentaje con signo para EV y ROI.
export const signo = (x, d = 1) => `${x > 0 ? '+' : x < 0 ? '−' : ''}${Math.abs(x * 100).toFixed(d)} %`;
// "+1.35" o "−4.00": unidades o montos con signo.
export const conSigno = (x, d = 2) => `${x > 0 ? '+' : x < 0 ? '−' : ''}${Math.abs(x).toFixed(d)}`;

// ---------- ms-catalogo: ligas y partidos ----------

export const LIGAS = [
  { id: 1, codigo: 'E0', nombre: 'Premier League', pais: 'Inglaterra' },
  { id: 2, codigo: 'SP1', nombre: 'LaLiga', pais: 'España' },
  { id: 3, codigo: 'D1', nombre: 'Bundesliga', pais: 'Alemania' },
];
export const liga = codigo => LIGAS.find(l => l.codigo === codigo);

// Arma un PartidoDTO como el que devuelve GET /api/catalogo/partidos.
function dto(id, codigoLiga, fechaHora, [idL, local], [idV, visitante], arbitro) {
  const l = liga(codigoLiga);
  return {
    id, liga: { id: l.id, codigo: l.codigo, nombre: l.nombre }, temporada: '2627', fechaHora,
    local: { id: idL, nombre: local }, visitante: { id: idV, nombre: visitante },
    arbitro: arbitro && { id: arbitro[0], nombre: arbitro[1] },
    golesLocal: null, golesVisitante: null, estado: 'PROGRAMADO',
  };
}

// GET /api/catalogo/partidos?fecha=2026-10-24, ya ordenados por fechaHora.
export const PARTIDOS = [
  dto(1529, 'E0', '2026-10-24T11:30:00Z', [13, 'Manchester City'], [21, 'Aston Villa'], [6, 'S Hooper']),
  dto(1530, 'SP1', '2026-10-24T12:00:00Z', [44, 'Real Sociedad'], [47, 'Sevilla'], null),
  dto(1531, 'D1', '2026-10-24T13:30:00Z', [71, 'Leverkusen'], [78, 'Freiburg'], null),
  dto(1532, 'E0', '2026-10-24T14:00:00Z', [11, 'Arsenal'], [17, 'Chelsea'], [4, 'M Oliver']),
  dto(1533, 'E0', '2026-10-24T16:30:00Z', [12, 'Liverpool'], [19, 'Brighton'], [2, 'A Taylor']),
  dto(1534, 'D1', '2026-10-24T16:30:00Z', [72, 'Dortmund'], [80, 'Stuttgart'], null),
  dto(1535, 'SP1', '2026-10-24T19:00:00Z', [41, 'Barcelona'], [52, 'Girona'], null),
];

export const partido = id => PARTIDOS.find(p => p.id === id);

// Partidos del día filtrados por liga ("TODAS" o un código como "E0").
export const partidosDelDia = (codigo = 'TODAS') => PARTIDOS.filter(p => codigo === 'TODAS' || p.liga.codigo === codigo);

// ---------- ms-estadisticas / ms-predicciones: valores esperados ----------

// Valores esperados que ms-predicciones calcularía con los promedios de ms-estadisticas.
const MODELO = {
  1529: { gl: 2.21, gv: 0.88, corners: 11.8, tarjetas: 3.6, tiros: 26.0, factor: 1.02, muestra: [10, 10] },
  1530: { gl: 1.31, gv: 1.02, corners: 9.6, tarjetas: 5.4, tiros: 22.4, factor: 1, muestra: [10, 9] },
  1531: { gl: 1.95, gv: 1.05, corners: 10.1, tarjetas: 3.8, tiros: 25.2, factor: 1, muestra: [10, 10] },
  1532: { gl: 1.82, gv: 0.97, corners: 10.4, tarjetas: 4.6, tiros: 24.3, factor: 1.08, muestra: [10, 10] },
  1533: { gl: 2.05, gv: 1.21, corners: 11.2, tarjetas: 3.9, tiros: 27.1, factor: 0.94, muestra: [10, 10] },
  1534: { gl: 1.88, gv: 1.47, corners: 9.8, tarjetas: 4.0, tiros: 26.6, factor: 1, muestra: [10, 10] },
  1535: { gl: 2.48, gv: 1.10, corners: 10.9, tarjetas: 4.1, tiros: 28.3, factor: 1, muestra: [10, 4] },
};

// Medias de cada liga (μ), como las de GET /ligas/{ligaId}/promedios.
const MEDIAS_LIGA = {
  E0: { golesLocal: 1.55, golesVisitante: 1.25, corners: 10.2, tarjetas: 3.9 },
  SP1: { golesLocal: 1.42, golesVisitante: 1.08, corners: 9.5, tarjetas: 4.9 },
  D1: { golesLocal: 1.71, golesVisitante: 1.38, corners: 9.9, tarjetas: 3.7 },
};

// Promedios del local jugando en casa y del visitante jugando fuera (últimos 10 partidos).
export function promedios(id) {
  const p = partido(id), m = MODELO[id];
  const tarjetasSinArbitro = m.tarjetas / m.factor;
  return {
    liga: MEDIAS_LIGA[p.liga.codigo],
    local: { golesAFavor: r2(m.gl * 1.12), golesEnContra: r2(m.gv * 0.94), corners: r2(m.corners * 0.57), tarjetas: r2(tarjetasSinArbitro * 0.46) },
    visitante: { golesAFavor: r2(m.gv * 1.05), golesEnContra: r2(m.gl * 1.02), corners: r2(m.corners * 0.43), tarjetas: r2(tarjetasSinArbitro * 0.54) },
  };
}

// ---------- Modelo de Poisson (mismas fórmulas que el README de ms-predicciones) ----------

// Pois(k; λ) para k = 0..n, calculado de forma iterativa: p0 = e^−λ y p_k = p_{k−1} × λ / k.
function filaPoisson(lambda, n) {
  const fila = [Math.exp(-lambda)];
  for (let k = 1; k <= n; k++) fila.push(fila[k - 1] * lambda / k);
  return fila;
}

// Matriz P(i, j) = Pois(i; λL) × Pois(j; λV), con i y j de 0 a n goles.
export function matrizPoisson(lambdaL, lambdaV, n = 10) {
  const fl = filaPoisson(lambdaL, n), fv = filaPoisson(lambdaV, n);
  return fl.map(pi => fv.map(pj => pi * pj));
}

// P(X ≤ k) de una Poisson: se usa para córners y tarjetas, que van sobre el total del partido.
const acumulada = (k, lambda) => filaPoisson(lambda, k).reduce((a, b) => a + b, 0);

// GET /api/predicciones/partidos/{id}: probabilidad y cuota justa de todos los mercados y líneas.
export function prediccion(id) {
  const p = partido(id), m = MODELO[id];
  if (!p || !m) return null;
  const matriz = matrizPoisson(m.gl, m.gv);
  const suma = cond => matriz.reduce((t, fila, i) => t + fila.reduce((s, v, j) => s + (cond(i, j) ? v : 0), 0), 0);
  const mercados = [];
  const agregar = (mercado, seleccion, linea, prob) => mercados.push({ mercado, seleccion, linea, probabilidad: r3(prob), cuotaJusta: r2(1 / prob) });

  agregar('1X2', 'LOCAL', null, suma((i, j) => i > j));
  agregar('1X2', 'EMPATE', null, suma((i, j) => i === j));
  agregar('1X2', 'VISITA', null, suma((i, j) => i < j));
  for (const l of [1.5, 2.5, 3.5]) { const mas = suma((i, j) => i + j > l); agregar('GOLES', 'MAS', l, mas); agregar('GOLES', 'MENOS', l, 1 - mas); }
  const si = (1 - Math.exp(-m.gl)) * (1 - Math.exp(-m.gv));
  agregar('AMBOS_MARCAN', 'SI', null, si); agregar('AMBOS_MARCAN', 'NO', null, 1 - si);
  for (const l of [3.5, 4.5, 5.5]) { const mas = 1 - acumulada(Math.floor(l), m.tarjetas); agregar('TARJETAS', 'MAS', l, mas); agregar('TARJETAS', 'MENOS', l, 1 - mas); }
  for (const l of [8.5, 9.5, 10.5]) { const mas = 1 - acumulada(Math.floor(l), m.corners); agregar('CORNERS', 'MAS', l, mas); agregar('CORNERS', 'MENOS', l, 1 - mas); }

  return {
    partidoId: id, partido: `${p.local.nombre} vs ${p.visitante.nombre}`, fechaHora: p.fechaHora,
    esperados: { golesLocal: m.gl, golesVisitante: m.gv, corners: m.corners, tarjetas: m.tarjetas, tiros: m.tiros },
    factorArbitro: m.factor,
    muestra: { partidosLocal: m.muestra[0], partidosVisitante: m.muestra[1] },
    mercados,
    explicacion: explicacion(p, m),
  };
}

// Frases que dicen de dónde sale cada número (RN-15: honestidad).
function explicacion(p, m) {
  const prom = promedios(p.id), media = MEDIAS_LIGA[p.liga.codigo];
  const frases = [
    `${p.local.nombre} marca ${prom.local.golesAFavor.toFixed(1)} goles por partido en casa (media de la liga: ${media.golesLocal}).`,
    `${p.visitante.nombre} recibe ${prom.visitante.golesEnContra.toFixed(1)} goles por partido fuera (media de la liga: ${media.golesLocal}).`,
  ];
  if (p.arbitro) {
    // Despeja las tarjetas del árbitro a partir del factor suavizado, con 20 partidos dirigidos.
    const porPartido = (m.factor * media.tarjetas * 30 - 10 * media.tarjetas) / 20;
    frases.push(`${p.arbitro.nombre} saca ${porPartido.toFixed(1)} tarjetas por partido (media de la liga: ${media.tarjetas}), así que el factor árbitro es ${m.factor.toFixed(2)}.`);
  } else {
    frases.push('El partido no tiene árbitro asignado, así que el factor árbitro vale 1.');
  }
  const [nl, nv] = m.muestra;
  if (nl < 5 || nv < 5) {
    const equipo = nl < 5 ? p.local.nombre : p.visitante.nombre;
    frases.push(`${equipo} solo tiene ${Math.min(nl, nv)} partidos de muestra: sus factores se acercan a la media de la liga y el partido no entra en la apuesta del día.`);
  }
  return frases;
}

// ---------- Mercados: claves y textos ----------

export const NOMBRE_MERCADO = { '1X2': 'Resultado', GOLES: 'Goles', AMBOS_MARCAN: 'Ambos marcan', TARJETAS: 'Tarjetas', CORNERS: 'Córners' };

// Clave única de una selección: "GOLES|MAS|2.5" (la línea vacía si no tiene).
export const clave = m => `${m.mercado}|${m.seleccion}|${m.linea ?? ''}`;

// Convierte una clave en { mercado, seleccion, linea }.
export function desdeClave(k) {
  const [mercado, seleccion, linea] = k.split('|');
  return { mercado, seleccion, linea: linea === '' ? null : Number(linea) };
}

// Nombres de local y visitante, ya sea de un PartidoDTO o de un texto "Arsenal vs Chelsea".
function equipos(p) {
  if (typeof p === 'string') { const [l, v] = p.split(' vs '); return [l, v]; }
  return [p.local.nombre, p.visitante.nombre];
}

// Texto para personas: "Más de 2.5 goles", "Gana Arsenal", "Ambos marcan".
export function etiqueta(m, p) {
  const [local, visitante] = equipos(p);
  if (m.mercado === '1X2') return m.seleccion === 'EMPATE' ? 'Empate' : `Gana ${m.seleccion === 'LOCAL' ? local : visitante}`;
  if (m.mercado === 'AMBOS_MARCAN') return m.seleccion === 'SI' ? 'Ambos marcan' : 'No marcan los dos';
  const cosa = { GOLES: 'goles', TARJETAS: 'tarjetas', CORNERS: 'córners' }[m.mercado];
  return `${m.seleccion === 'MAS' ? 'Más' : 'Menos'} de ${m.linea} ${cosa}`;
}

// ---------- ms-cuotas ----------

// El partido 1533 simula que ms-cuotas no responde (503), para ver el aviso de CN-08.
export const PARTIDO_SIN_CUOTAS = 1533;
const MARGEN = { bet365: 0.055, pinnacle: 0.025 };

// Cuotas fijadas a mano para que la apuesta del día tenga candidatas realistas.
const FIJAS = {
  '1532|bet365|GOLES|MAS|2.5': 2.00, '1532|bet365|GOLES|MENOS|2.5': 1.80,
  '1531|bet365|GOLES|MAS|2.5': 1.80, '1531|bet365|GOLES|MENOS|2.5': 1.98,
  '1534|bet365|GOLES|MAS|2.5': 1.59, '1534|bet365|GOLES|MENOS|2.5': 2.30,
  '1530|bet365|1X2|LOCAL|': 2.43, '1530|bet365|1X2|EMPATE|': 3.10, '1530|bet365|1X2|VISITA|': 3.25,
};

// Cuotas de Betano que cargaron los usuarios a mano (tipo MANUAL).
const MANUALES = [
  { partidoId: 1532, casa: 'betano', mercado: 'TARJETAS', seleccion: 'MAS', linea: 4.5, valor: 1.95, tipo: 'MANUAL', capturadaEn: '2026-10-24T12:40:00Z' },
  { partidoId: 1532, casa: 'betano', mercado: 'CORNERS', seleccion: 'MAS', linea: 9.5, valor: 1.72, tipo: 'MANUAL', capturadaEn: '2026-10-24T12:42:00Z' },
  { partidoId: 1535, casa: 'betano', mercado: 'CORNERS', seleccion: 'MAS', linea: 9.5, valor: 1.62, tipo: 'MANUAL', capturadaEn: '2026-10-24T11:15:00Z' },
];

// GET /api/cuotas/partidos/{id}/cuotas: la cuota más reciente de cada casa y selección.
// Devuelve null cuando el servicio "no responde", y la vista muestra el aviso.
export function cuotas(id) {
  if (id === PARTIDO_SIN_CUOTAS) return null;
  const pr = prediccion(id);
  if (!pr) return [];
  const lista = [];
  // bet365 y Pinnacle solo traen 1X2 y más/menos 2.5 goles (lo que trae el CSV de football-data).
  for (const casa of ['bet365', 'pinnacle']) {
    for (const m of pr.mercados.filter(m => m.mercado === '1X2' || (m.mercado === 'GOLES' && m.linea === 2.5))) {
      const k = `${id}|${casa}|${clave(m)}`;
      const valor = FIJAS[k] ?? r2((1 / (m.probabilidad * (1 + MARGEN[casa]))) * (1 + 0.025 * ruido(k)));
      lista.push({ casa, mercado: m.mercado, seleccion: m.seleccion, linea: m.linea, valor, tipo: 'APERTURA', capturadaEn: '2026-10-22T10:00:00Z' });
    }
  }
  return lista.concat(MANUALES.filter(c => c.partidoId === id));
}

// Busca en una lista de cuotas la de una casa y una selección.
export const buscarCuota = (lista, casa, k) => lista?.find(c => c.casa === casa && clave(c) === k) ?? null;

// POST /api/cuotas/cuotas/manual: guarda (o actualiza) una cuota de Betano.
export function guardarCuotaManual({ partidoId, mercado, seleccion, linea, valor }) {
  const nueva = { partidoId, casa: 'betano', mercado, seleccion, linea, valor, tipo: 'MANUAL', capturadaEn: new Date().toISOString() };
  const i = MANUALES.findIndex(c => c.partidoId === partidoId && clave(c) === clave(nueva));
  if (i >= 0) MANUALES[i] = nueva; else MANUALES.push(nueva);
  return nueva;
}

// ---------- ms-apuestas: valor esperado y apuesta del día ----------

// Stake sugerido: un cuarto de Kelly, con un tope del 3 % de la banca. Si el EV no es positivo, 0.
function stake(ev, c) {
  return ev > 0 ? r3(Math.min(0.25 * (ev / (c - 1)), 0.03)) : 0;
}

// POST /api/apuestas/evaluaciones: la probabilidad sale del modelo y el usuario solo manda la cuota.
export function evaluar({ partidoId, mercado, seleccion, linea, cuota: c }) {
  const m = prediccion(partidoId).mercados.find(x => clave(x) === clave({ mercado, seleccion, linea }));
  const ev = m.probabilidad * c - 1;
  return {
    probabilidad: m.probabilidad, cuotaJusta: m.cuotaJusta, valorEsperado: r3(ev), stakePct: stake(ev, c),
    veredicto: ev >= 0.03 ? 'CON_VALOR' : ev > 0 ? 'MARGINAL' : 'SIN_VALOR',
  };
}

export const TEXTO_VEREDICTO = { CON_VALOR: 'Con valor', MARGINAL: 'Marginal', SIN_VALOR: 'Sin valor' };

let apuestaGuardada = null;

// GET /api/apuestas/apuesta-del-dia: recorre los partidos del día, calcula el EV de las cuotas de
// bet365 y Betano, aplica los umbrales y elige la de mayor EV. Se guarda la primera vez (RN-APU-02).
export function apuestaDelDia() {
  if (apuestaGuardada) return apuestaGuardada;
  const candidatas = [];
  for (const p of PARTIDOS.filter(p => p.estado === 'PROGRAMADO')) {
    const lista = cuotas(p.id);
    if (!lista) continue;
    const pr = prediccion(p.id);
    const muestraOk = pr.muestra.partidosLocal >= 5 && pr.muestra.partidosVisitante >= 5;
    for (const c of lista.filter(c => c.casa !== 'pinnacle')) {
      const m = pr.mercados.find(x => clave(x) === clave(c));
      const ev = m.probabilidad * c.valor - 1;
      if (muestraOk && c.valor >= 1.5 && c.valor <= 2.6 && m.probabilidad >= 0.42 && ev >= 0.03) {
        candidatas.push({ partidoId: p.id, partido: pr.partido, mercado: c.mercado, seleccion: c.seleccion, linea: c.linea, casa: c.casa,
          cuota: c.valor, probabilidad: m.probabilidad, cuotaJusta: m.cuotaJusta, valorEsperado: r3(ev), stakePct: stake(ev, c.valor) });
      }
    }
  }
  candidatas.sort((a, b) => b.valorEsperado - a.valorEsperado);
  const pick = candidatas[0] ? { id: 88, ...candidatas[0] } : null;
  apuestaGuardada = {
    fecha: HOY, pick, otrasCandidatas: Math.max(candidatas.length - 1, 0),
    mensaje: pick ? null : 'Hoy no hay apuesta del día: ninguna apuesta supera los umbrales. No apostar también es una decisión.',
  };
  return apuestaGuardada;
}

// ---------- ms-apuestas: mis apuestas y rendimiento ----------

const apuesta = (id, creadaEn, partidoId, partidoTxt, mercado, seleccion, linea, casa, c, monto, resultado) => ({
  id, creadaEn, partidoId, partido: partidoTxt, mercado, seleccion, linea, casa, cuota: c, monto,
  estado: resultado ? 'LIQUIDADA' : 'PENDIENTE', resultado: resultado ?? null,
  ganancia: resultado === 'GANADA' ? r2(monto * (c - 1)) : resultado === 'PERDIDA' ? -monto : null,
});

const APUESTAS = [
  apuesta(41, '2026-10-24T13:20:00Z', 1532, 'Arsenal vs Chelsea', 'GOLES', 'MAS', 2.5, 'bet365', 2.00, 8, null),
  apuesta(40, '2026-10-24T12:05:00Z', 1535, 'Barcelona vs Girona', '1X2', 'LOCAL', null, 'bet365', 1.62, 20, null),
  apuesta(39, '2026-10-19T14:10:00Z', 1488, 'Brentford vs Fulham', 'GOLES', 'MAS', 2.5, 'bet365', 1.95, 20, 'GANADA'),
  apuesta(38, '2026-10-19T13:55:00Z', 1491, 'Valencia vs Getafe', '1X2', 'EMPATE', null, 'bet365', 3.20, 10, 'PERDIDA'),
  apuesta(37, '2026-10-18T12:30:00Z', 1479, 'Bayern vs Mainz', '1X2', 'LOCAL', null, 'bet365', 1.30, 30, 'GANADA'),
  apuesta(36, '2026-10-18T11:45:00Z', 1484, 'Atlético vs Betis', 'AMBOS_MARCAN', 'NO', null, 'betano', 1.90, 20, 'GANADA'),
  apuesta(35, '2026-10-17T18:20:00Z', 1476, 'Everton vs Wolves', 'TARJETAS', 'MAS', 4.5, 'betano', 2.05, 15, 'PERDIDA'),
  apuesta(34, '2026-10-17T16:00:00Z', 1473, 'Leipzig vs Hoffenheim', 'GOLES', 'MAS', 3.5, 'bet365', 2.40, 10, 'PERDIDA'),
  apuesta(33, '2026-10-11T13:40:00Z', 1440, 'Tottenham vs Newcastle', 'CORNERS', 'MAS', 10.5, 'betano', 1.85, 20, 'GANADA'),
  apuesta(32, '2026-10-10T17:05:00Z', 1436, 'Villarreal vs Osasuna', '1X2', 'LOCAL', null, 'bet365', 1.72, 25, 'GANADA'),
  apuesta(31, '2026-10-06T15:30:00Z', 1421, 'West Ham vs Bournemouth', 'GOLES', 'MENOS', 2.5, 'bet365', 2.10, 20, 'PERDIDA'),
  apuesta(30, '2026-10-05T12:15:00Z', 1417, 'Wolfsburg vs Augsburg', '1X2', 'LOCAL', null, 'bet365', 1.95, 30, 'PERDIDA'),
];

// GET /api/apuestas/mis-apuestas?estado=…: solo las del usuario del token, de la más reciente a la más antigua.
export const misApuestas = estado => APUESTAS.filter(a => !estado || a.estado === estado).sort((a, b) => b.creadaEn.localeCompare(a.creadaEn));

// POST /api/apuestas/mis-apuestas: guarda la apuesta con el nombre del partido (RN-APU-05).
export function registrarApuesta({ partidoId, mercado, seleccion, linea, casa, cuota: c, monto }) {
  const p = partido(partidoId);
  const nueva = apuesta(Math.max(...APUESTAS.map(a => a.id)) + 1, new Date().toISOString(), partidoId,
    `${p.local.nombre} vs ${p.visitante.nombre}`, mercado, seleccion, linea, casa, c, monto, null);
  APUESTAS.push(nueva);
  return nueva;
}

// GET /api/apuestas/mis-apuestas/rendimiento: solo cuenta las liquidadas (RN-APU-07).
export function rendimiento() {
  const liq = APUESTAS.filter(a => a.estado === 'LIQUIDADA');
  const total = lista => {
    const monto = lista.reduce((s, a) => s + a.monto, 0), beneficio = lista.reduce((s, a) => s + a.ganancia, 0);
    return { monto: r2(monto), beneficio: r2(beneficio), roi: monto ? r3(beneficio / monto) : 0 };
  };
  const t = total(liq);
  const porMercado = Object.keys(NOMBRE_MERCADO).map(mercado => {
    const lista = liq.filter(a => a.mercado === mercado);
    const x = total(lista);
    return { mercado, apuestas: lista.length, beneficio: x.beneficio, roi: x.roi };
  }).filter(m => m.apuestas > 0);
  return {
    apuestas: APUESTAS.length, pendientes: APUESTAS.length - liq.length,
    ganadas: liq.filter(a => a.resultado === 'GANADA').length, perdidas: liq.filter(a => a.resultado === 'PERDIDA').length,
    montoApostado: t.monto, beneficio: t.beneficio, roi: t.roi, porMercado,
  };
}

// ---------- ms-apuestas: historial de la apuesta del día ----------

const pick = (fecha, partidoTxt, mercado, seleccion, linea, c, resultado) => ({ fecha, partido: partidoTxt, mercado, seleccion, linea, cuota: c, resultado });

const PICKS = [
  pick('2026-10-23', 'Leipzig vs Wolfsburg', 'GOLES', 'MAS', 2.5, 1.72, 'GANADA'),
  pick('2026-10-22', 'Getafe vs Mallorca', 'GOLES', 'MENOS', 2.5, 1.68, 'GANADA'),
  pick('2026-10-21', 'Newcastle vs Crystal Palace', '1X2', 'LOCAL', null, 1.95, 'PERDIDA'),
  pick('2026-10-19', 'Brentford vs Fulham', 'GOLES', 'MAS', 2.5, 1.95, 'GANADA'),
  pick('2026-10-18', 'Bayern vs Mainz', 'GOLES', 'MAS', 3.5, 2.05, 'PERDIDA'),
  pick('2026-10-17', 'Everton vs Wolves', 'TARJETAS', 'MAS', 4.5, 2.10, 'PERDIDA'),
  pick('2026-10-16', 'Athletic vs Celta', 'AMBOS_MARCAN', 'SI', null, 2.20, 'GANADA'),
  pick('2026-10-12', 'Frankfurt vs Hoffenheim', 'GOLES', 'MAS', 2.5, 1.80, 'PERDIDA'),
  pick('2026-10-11', 'Tottenham vs Newcastle', 'CORNERS', 'MAS', 10.5, 2.40, 'GANADA'),
  pick('2026-10-10', 'Villarreal vs Osasuna', '1X2', 'LOCAL', null, 1.85, 'GANADA'),
  pick('2026-10-06', 'West Ham vs Bournemouth', 'GOLES', 'MENOS', 2.5, 2.50, 'PERDIDA'),
  pick('2026-10-05', 'Wolfsburg vs Augsburg', '1X2', 'LOCAL', null, 2.10, 'GANADA'),
  pick('2026-10-04', 'Betis vs Rayo', 'TARJETAS', 'MAS', 5.5, 2.30, 'GANADA'),
  pick('2026-10-03', 'Man United vs Brentford', 'AMBOS_MARCAN', 'SI', null, 1.75, 'PERDIDA'),
  pick('2026-10-02', 'Gladbach vs Bremen', 'GOLES', 'MAS', 2.5, 1.90, 'GANADA'),
  pick('2026-09-29', 'Girona vs Alavés', '1X2', 'LOCAL', null, 2.00, 'PERDIDA'),
  pick('2026-09-28', 'Chelsea vs Brighton', 'CORNERS', 'MAS', 9.5, 2.05, 'GANADA'),
  pick('2026-09-27', 'Union Berlin vs Köln', 'AMBOS_MARCAN', 'NO', null, 2.25, 'PERDIDA'),
  pick('2026-09-26', 'Atlético vs Sevilla', 'GOLES', 'MENOS', 2.5, 1.60, 'PERDIDA'),
  pick('2026-09-25', 'Aston Villa vs Wolves', '1X2', 'LOCAL', null, 2.20, 'GANADA'),
  pick('2026-09-24', 'Stuttgart vs Heidenheim', 'GOLES', 'MAS', 2.5, 1.90, 'PERDIDA'),
];

// GET /api/apuestas/picks/historial?dias=30: stake plano de 1 unidad por pick (RN-APU-08).
export function historialPicks() {
  const ganancia = p => (p.resultado === 'GANADA' ? p.cuota - 1 : -1);
  const beneficio = PICKS.reduce((s, p) => s + ganancia(p), 0);
  return {
    picks: PICKS.length, ganados: PICKS.filter(p => p.resultado === 'GANADA').length, perdidos: PICKS.filter(p => p.resultado === 'PERDIDA').length,
    beneficioUnidades: r2(beneficio), roi: r3(beneficio / PICKS.length), ultimos: PICKS,
  };
}

// Beneficio acumulado día a día, del pick más antiguo al más reciente (para el gráfico).
export function curvaBeneficio() {
  let acumulado = 0;
  return [...PICKS].reverse().map(p => ({ fecha: p.fecha, valor: r2((acumulado += p.resultado === 'GANADA' ? p.cuota - 1 : -1)), resultado: p.resultado }));
}

// ---------- ms-usuarios: sesión y perfil ----------

const PERFIL = { id: 2, nombreUsuario: 'renzo', email: 'renzo@correo.pe', rol: 'APOSTADOR', banca: 500.00 };
export const perfil = () => PERFIL;

// PUT /api/usuarios/me/banca: la banca debe ser mayor o igual a 0.
export function actualizarBanca(banca) { PERFIL.banca = r2(banca); return PERFIL; }

const CLAVE_SESION = 'prototipo-apuestas-sesion';
let sesionEnMemoria = null;

// La web real guarda el JWT en la HttpSession del servidor (RN-WEB-02). Aquí basta con sessionStorage.
export function sesion() {
  try { return JSON.parse(sessionStorage.getItem(CLAVE_SESION)) ?? sesionEnMemoria; } catch { return sesionEnMemoria; }
}

// POST /api/usuarios/auth/login simulado: "admin" entra con rol ADMIN y cualquier otro como APOSTADOR.
export function iniciarSesion(nombreUsuario) {
  const s = { nombreUsuario, rol: nombreUsuario === 'admin' ? 'ADMIN' : 'APOSTADOR' };
  Object.assign(PERFIL, { nombreUsuario, rol: s.rol, email: `${nombreUsuario}@correo.pe` });
  sesionEnMemoria = s;
  try { sessionStorage.setItem(CLAVE_SESION, JSON.stringify(s)); } catch { /* sin almacenamiento: queda en memoria */ }
  return s;
}

export function cerrarSesion() {
  sesionEnMemoria = null;
  try { sessionStorage.removeItem(CLAVE_SESION); } catch { /* nada que borrar */ }
}

// Al recargar, el perfil toma el nombre y el rol de la sesión guardada.
{ const s = sesion(); if (s) Object.assign(PERFIL, { nombreUsuario: s.nombreUsuario, rol: s.rol, email: `${s.nombreUsuario}@correo.pe` }); }

// ---------- ms-ingesta: ejecuciones ----------

const EJECUCIONES = [
  { id: 14, tipo: 'PROXIMOS', fuente: 'fixtures.csv', inicio: '2026-10-24T13:00:00Z', fin: '2026-10-24T13:00:38Z', estado: 'OK', filas: 61, error: null },
  { id: 13, tipo: 'HISTORICO', fuente: 'mmz4281/2627/E0, SP1, D1', inicio: '2026-10-24T10:00:00Z', fin: '2026-10-24T10:02:51Z', estado: 'OK', filas: 268, error: null },
  { id: 12, tipo: 'PROXIMOS', fuente: 'fixtures.csv', inicio: '2026-10-23T21:00:00Z', fin: '2026-10-23T21:00:05Z', estado: 'ERROR', filas: 0, error: 'football-data.co.uk respondió 503' },
  { id: 11, tipo: 'PROXIMOS', fuente: 'fixtures.csv', inicio: '2026-10-23T13:00:00Z', fin: '2026-10-23T13:00:41Z', estado: 'OK', filas: 58, error: null },
  { id: 10, tipo: 'HISTORICO', fuente: 'mmz4281/2627/E0, SP1, D1', inicio: '2026-10-23T10:00:00Z', fin: '2026-10-23T10:02:37Z', estado: 'OK', filas: 262, error: null },
  { id: 9, tipo: 'HISTORICO', fuente: 'mmz4281/2526/E0, SP1, D1', inicio: '2026-10-20T15:12:00Z', fin: '2026-10-20T15:16:02Z', estado: 'OK', filas: 1056, error: null },
];

// GET /api/ingesta/ejecuciones?limite=20
export const ejecuciones = () => [...EJECUCIONES].sort((a, b) => b.id - a.id);

// POST /api/ingesta/ejecutar?tipo=…: responde EN_CURSO y termina sola unos segundos después (@Async).
export function lanzarIngesta(tipo, alTerminar) {
  const e = { id: Math.max(...EJECUCIONES.map(x => x.id)) + 1, tipo, fuente: tipo === 'HISTORICO' ? 'mmz4281/2627/E0, SP1, D1' : 'fixtures.csv',
    inicio: new Date().toISOString(), fin: null, estado: 'EN_CURSO', filas: 0, error: null };
  EJECUCIONES.push(e);
  setTimeout(() => {
    Object.assign(e, { fin: new Date().toISOString(), estado: 'OK', filas: tipo === 'HISTORICO' ? 271 : 61 });
    alTerminar?.(e);
  }, 2800);
  return e;
}
