// =============================================================
// Propuesta C: «Cuaderno». La web como el cuaderno donde se
// hacen las cuentas: hoja cuadriculada color arena, tinta azul,
// línea de margen con notas a mano y cada número importante
// escrito como una cuenta que se puede seguir (RN-15).
// Los formularios se llenan como frases con espacios en blanco.
// =============================================================
import { navegacion, opcionesMercado, graficoBeneficio, esc } from '../comun/app.js';
import * as M from '../comun/modelo.js';

// ---------- Piezas pequeñas ----------

const err = (ctx, form) => (ctx.error(form) ? `<p class="error" role="alert">${ctx.error(form)}</p>` : '');

// Un renglón del cuaderno: la nota del margen a la izquierda y el contenido a la derecha.
const bloque = (nota, contenido, clase = '') => `
<section class="bloque ${clase}">
  <div class="margen">${nota ? `<p class="mano">${nota}</p>` : ''}</div>
  <div class="cuerpo">${contenido}</div>
</section>`;

// Una línea de cuenta: lo que se calcula, el signo igual y el resultado.
const cuenta = (izquierda, derecha, clase = '') => `<div class="cuenta ${clase}"><span>${izquierda}</span><span class="igual">=</span><span>${derecha}</span></div>`;

// Resaltado con marcador melocotón para el número que importa.
const marca = texto => `<mark>${texto}</mark>`;

// Campo apilado (para login y registro).
const campo = (etiqueta, nombre, valor = '', tipo = 'text', extra = '') => `
<label class="campo"><span>${etiqueta}</span><input name="${nombre}" type="${tipo}" value="${valor}" ${extra}></label>`;

// Espacio en blanco dentro de una frase: un input con solo una línea debajo.
const blanco = (nombre, valor, ancho = 5, extra = '') => `<input class="blanco" name="${nombre}" value="${valor}" size="${ancho}" ${extra}>`;

const claseSigno = x => (x > 0 ? 'positivo' : x < 0 ? 'negativo' : '');

// Círculo hecho a mano alrededor de la sección activa del menú; se desliza al cambiar de pantalla.
const circuloMano = () => `
<svg class="vt-indicador" viewBox="0 0 120 46" preserveAspectRatio="none" aria-hidden="true">
  <path d="M10 24 C 8 10, 46 3, 88 5 C 112 7, 119 19, 111 31 C 101 43, 34 45, 13 37 C 4 33, 3 23, 16 14" vector-effect="non-scaling-stroke"></path>
</svg>`;

// Pie común: juego responsable (RN-WEB-07) y honestidad del modelo (RN-15).
const pie = () => bloque('Importante', `
<footer class="pie">
  <p><strong>Solo para mayores de 18 años. Juega con responsabilidad.</strong></p>
  <p>Las probabilidades salen de un modelo de Poisson didáctico. Sirven para aprender y comparar, no garantizan ganancias.</p>
</footer>`, 'bloque-pie');

// ---------- Marco ----------

export const marco = {
  // Login y registro: la tapa del cuaderno.
  publico: (cuerpo, ctx) => `
<div class="escritorio">
  <div class="hoja hoja-publica">
    ${ctx.aviso ? bloque('', `<p class="aviso" role="status">${ctx.aviso}</p>`) : ''}
    <main class="vt-contenido">${cuerpo}</main>
    ${pie()}
  </div>
</div>`,

  // Con sesión: encabezado de la hoja con el menú y el círculo a mano en la sección activa.
  privado: (cuerpo, ctx) => `
<div class="escritorio">
  <div class="hoja">
    <header class="bloque cabecera">
      <div class="margen"><p class="mano">Hoja del ${M.fechaCorta(M.HOY)}</p></div>
      <div class="cuerpo cabecera-cuerpo">
        <a class="marca" href="#/">Cuaderno de apuestas</a>
        <p class="usuario">${esc(ctx.sesion.nombreUsuario)}, banca ${M.soles(M.perfil().banca)} <button class="enlace" data-clic="salir">Salir</button></p>
        <nav class="menu" aria-label="Secciones">
          ${navegacion(ctx).map(i => `<a href="${i.href}" ${i.activo ? 'aria-current="page"' : ''}>${i.texto}${i.activo ? circuloMano() : ''}</a>`).join('')}
        </nav>
      </div>
    </header>
    ${ctx.aviso ? bloque('', `<p class="aviso" role="status">${ctx.aviso}</p>`) : ''}
    <main class="vt-contenido">${cuerpo}</main>
    ${pie()}
  </div>
</div>`,
};

// ---------- Hoy ----------

// La apuesta del día escrita como una cuenta: probabilidad, cuota, valor esperado y stake.
function apuestaDelDia(dia) {
  if (!dia.pick) return bloque('Hoy no', `<h1>Apuesta del día</h1><p class="grande">${dia.mensaje}</p>`);
  const k = dia.pick, p = M.partido(k.partidoId), pr = M.prediccion(p.id), banca = M.perfil().banca;
  const nota = `Poisson con λ = ${pr.esperados.golesLocal} y ${pr.esperados.golesVisitante} goles.<br><br>En promedio ganarías ${(k.valorEsperado * 100).toFixed(1)} céntimos por sol apostado. No es seguro: es un promedio.`;
  return bloque(nota, `
<h1>Apuesta del día</h1>
<p class="pick-partido"><a href="#/partidos/${p.id}">${p.local.nombre} – ${p.visitante.nombre}</a></p>
<p class="pick-detalle">${M.etiqueta(k, p)} en ${k.casa}, ${M.horaLima(p.fechaHora)} hora de Lima. Hubo ${dia.otrasCandidatas} candidatas más; esta es la de mayor valor esperado.</p>
<div class="cuentas">
  ${cuenta(`P(${M.etiqueta(k, p).toLowerCase()})`, k.probabilidad.toFixed(3))}
  ${cuenta(`Cuota de ${k.casa}`, M.cuota(k.cuota))}
  ${cuenta('Valor esperado', `${k.probabilidad.toFixed(3)} × ${M.cuota(k.cuota)} − 1 = ${marca(M.conSigno(k.valorEsperado, 3))}`, 'cuenta-fuerte')}
  ${cuenta('Stake (¼ de Kelly, tope 3 %)', `${M.pct(k.stakePct)} de ${M.soles(banca)} = ${marca(M.soles(k.stakePct * banca))}`)}
</div>`, 'bloque-pick');
}

// Un renglón de partido: hora, equipos, probabilidades del modelo y cuotas 1X2 de bet365.
function renglonPartido(p) {
  const pr = M.prediccion(p.id), cs = M.cuotas(p.id);
  const ms = ['LOCAL', 'EMPATE', 'VISITA'].map(s => pr.mercados.find(m => m.mercado === '1X2' && m.seleccion === s));
  const cuotas = cs
    ? ms.map(m => { const c = M.buscarCuota(cs, 'bet365', M.clave(m)); return `<span class="${c && c.valor > m.cuotaJusta ? 'con-valor' : ''}">${M.cuota(c?.valor)}</span>`; }).join('')
    : '<span class="sin-cuotas">Cuotas no disponibles por ahora</span>';
  return `
<li class="renglon" style="--vt: fila-${p.id}">
  <time>${M.horaLima(p.fechaHora)}</time>
  <a href="#/partidos/${p.id}">${p.local.nombre} – ${p.visitante.nombre}</a>
  <span class="modelo">${ms.map(m => Math.round(m.probabilidad * 100)).join(' / ')}</span>
  <span class="cuotas">${cuotas}</span>
</li>`;
}

// ---------- Análisis ----------

// Matriz de marcadores de 0 a 5 goles, sombreada según su probabilidad.
function matriz(p, pr) {
  const m = M.matrizPoisson(pr.esperados.golesLocal, pr.esperados.golesVisitante, 5);
  const max = Math.max(...m.flat());
  return `
<table class="matriz">
  <caption>Goles de ${p.local.nombre} en filas y de ${p.visitante.nombre} en columnas. Cada casilla es la probabilidad de ese marcador exacto.</caption>
  <thead><tr><th></th>${m[0].map((_, j) => `<th scope="col">${j}</th>`).join('')}</tr></thead>
  <tbody>${m.map((fila, i) => `<tr><th scope="row">${i}</th>${fila.map((v, j) => `<td class="${i === j ? 'diagonal' : ''} ${v / max > 0.5 ? 'oscura' : ''}" style="--t:${(v / max).toFixed(3)}" title="${i}-${j}: ${M.pct(v)}">${(v * 100).toFixed(1)}</td>`).join('')}</tr>`).join('')}</tbody>
</table>`;
}

// Tabla de mercados con las tres casas; en verde las cuotas que pagan más que la justa.
function tablaMercados(p, pr, cs) {
  const casas = cs ? ['bet365', 'pinnacle', 'betano'] : [];
  return `
<table class="tabla">
  <thead><tr><th scope="col">Selección</th><th scope="col">Modelo</th><th scope="col">Justa</th>${casas.map(c => `<th scope="col">${c}</th>`).join('')}</tr></thead>
  ${Object.entries(M.NOMBRE_MERCADO).map(([mercado, nombre]) => `
  <tbody>
    <tr class="grupo"><th colspan="${3 + casas.length}" scope="rowgroup">${nombre}</th></tr>
    ${pr.mercados.filter(m => m.mercado === mercado).map(m => `
    <tr>
      <th scope="row">${M.etiqueta(m, p)}</th>
      <td>${m.probabilidad.toFixed(3)}</td>
      <td class="justa">${M.cuota(m.cuotaJusta)}</td>
      ${casas.map(casa => {
        const c = M.buscarCuota(cs, casa, M.clave(m));
        const ev = c ? m.probabilidad * c.valor - 1 : null;
        return `<td class="${ev > 0 ? 'con-valor' : ''}" ${ev != null ? `title="EV ${M.signo(ev)}"` : ''}>${M.cuota(c?.valor)}</td>`;
      }).join('')}
    </tr>`).join('')}
  </tbody>`).join('')}
</table>`;
}

// Resultado de evaluar una cuota, escrito como cuenta.
function resultadoEvaluacion(ev) {
  if (!ev) return '<p class="nota">La probabilidad la pone el modelo; tú solo escribes la cuota que te ofrecen.</p>';
  return `
<div class="resultado resultado-${ev.veredicto.toLowerCase()}">
  ${cuenta('Valor esperado', `${ev.probabilidad.toFixed(3)} × ${M.cuota(ev.cuota)} − 1 = ${marca(M.conSigno(ev.valorEsperado, 3))}`, 'cuenta-fuerte')}
  ${cuenta('Cuota justa', `1 / ${ev.probabilidad.toFixed(3)} = ${M.cuota(ev.cuotaJusta)}`)}
  <p class="veredicto">${M.TEXTO_VEREDICTO[ev.veredicto]}. ${ev.stakePct > 0 ? `Stake sugerido: ${M.pct(ev.stakePct)} de tu banca (${M.soles(ev.stakePct * M.perfil().banca)}).` : 'No conviene apostar a esta cuota.'}</p>
</div>`;
}

// ---------- Vistas ----------

export const vistas = {
  // /login: la tapa del cuaderno.
  login: ctx => bloque('Apuntes de un modelo de Poisson', `
<div class="acceso">
  <p class="marca-grande">Cuaderno de apuestas</p>
  <p class="grande">Cada probabilidad viene con su cuenta, para que sepas de dónde sale antes de decidir.</p>
  <form class="acceso-form" data-accion="login" novalidate>
    <nav class="pestanas"><a href="#/login" aria-current="page">Iniciar sesión</a><a href="#/registro">Crear cuenta</a></nav>
    ${campo('Usuario', 'nombreUsuario', ctx.valor('login', 'nombreUsuario', 'renzo'), 'text', 'autocomplete="username"')}
    ${campo('Contraseña', 'clave', '', 'password', 'autocomplete="current-password"')}
    ${err(ctx, 'login')}
    <button class="boton">Entrar</button>
    <p class="nota">Prototipo: cualquier contraseña de 6 caracteres o más sirve. Entra como «admin» para ver la ingesta.</p>
  </form>
</div>`),

  // /registro
  registro: ctx => bloque('Tus apuestas solo las ves tú', `
<div class="acceso">
  <p class="marca-grande">Cuaderno de apuestas</p>
  <form class="acceso-form" data-accion="registro" novalidate>
    <nav class="pestanas"><a href="#/login">Iniciar sesión</a><a href="#/registro" aria-current="page">Crear cuenta</a></nav>
    ${campo('Usuario', 'nombreUsuario', ctx.valor('registro', 'nombreUsuario'), 'text', 'maxlength="30" autocomplete="username"')}
    ${campo('Correo', 'email', ctx.valor('registro', 'email'), 'email', 'autocomplete="email"')}
    ${campo('Contraseña', 'clave', '', 'password', 'autocomplete="new-password"')}
    ${campo('Repite la contraseña', 'claveConfirmacion', '', 'password', 'autocomplete="new-password"')}
    ${err(ctx, 'registro')}
    <button class="boton">Crear cuenta</button>
  </form>
</div>`),

  // / (Hoy)
  hoy: ctx => {
    const filtros = [['TODAS', 'todas'], ...M.LIGAS.map(l => [l.codigo, l.nombre])];
    const ligas = M.LIGAS.filter(l => ctx.estado.liga === 'TODAS' || l.codigo === ctx.estado.liga);
    return `
${apuestaDelDia(M.apuestaDelDia())}
${bloque('Local / empate / visita, en %', `
<h2>Partidos de hoy</h2>
<p class="filtro" role="group" aria-label="Filtrar por liga">Ver ${filtros.map(([c, t]) => `<button data-clic="liga:${c}" aria-pressed="${ctx.estado.liga === c}">${t}</button>`).join(', ')}</p>
<p class="nota">Primero, la probabilidad del modelo; después, las cuotas 1X2 de bet365. En verde, las que pagan más que la cuota justa.</p>`)}
${ligas.map(l => bloque(l.nombre, `<ol class="renglones">${M.partidosDelDia(l.codigo).map(renglonPartido).join('')}</ol>`, 'bloque-liga')).join('')}`;
  },

  // /partidos/{id} (Análisis): la cuenta completa, paso a paso.
  partido: ctx => {
    const p = M.partido(ctx.ruta.id);
    if (!p) return bloque('', '<p class="vacio">No encontramos ese partido. <a href="#/">Volver a Hoy</a></p>');
    const pr = M.prediccion(p.id), cs = M.cuotas(p.id), prom = M.promedios(p.id), e = pr.esperados;
    const ev = ctx.estado.evaluacion?.partidoId === p.id ? ctx.estado.evaluacion : null;
    const f = ctx.estado.form.evaluar ?? {};
    // Factores de ataque y defensa: se despejan para que la cuenta cierre con el λ del modelo.
    const muL = prom.liga.golesLocal, muV = prom.liga.golesVisitante;
    const ataL = prom.local.golesAFavor / muL, defV = e.golesLocal / (muL * ataL);
    const ataV = prom.visitante.golesAFavor / muV, defL = e.golesVisitante / (muV * ataV);
    const x = s => pr.mercados.find(m => m.mercado === '1X2' && m.seleccion === s).probabilidad;
    return `
${bloque('<a href="#/">Volver a Hoy</a>', `
<h1>${p.local.nombre} – ${p.visitante.nombre}</h1>
<p class="pick-detalle">${p.liga.nombre}, ${M.horaLima(p.fechaHora)} hora de Lima. ${p.arbitro ? `Árbitro ${p.arbitro.nombre} (factor ${pr.factorArbitro.toFixed(2)}).` : 'Sin árbitro asignado (factor 1).'} Muestra: ${pr.muestra.partidosLocal} y ${pr.muestra.partidosVisitante} partidos.</p>`)}
${bloque('Paso 1<br>media de la liga × ataque × defensa del rival', `
<h2>Goles esperados</h2>
<div class="cuentas">
  ${cuenta(`λ ${p.local.nombre}`, `${muL.toFixed(2)} × ${ataL.toFixed(2)} × ${defV.toFixed(2)} = ${marca(e.golesLocal.toFixed(2))}`)}
  ${cuenta(`λ ${p.visitante.nombre}`, `${muV.toFixed(2)} × ${ataV.toFixed(2)} × ${defL.toFixed(2)} = ${marca(e.golesVisitante.toFixed(2))}`)}
  ${cuenta('Córners esperados', e.corners.toFixed(1))}
  ${cuenta('Tarjetas esperadas', `${(e.tarjetas / pr.factorArbitro).toFixed(2)} × ${pr.factorArbitro.toFixed(2)} (árbitro) = ${e.tarjetas.toFixed(1)}`)}
</div>`)}
${bloque('Paso 2<br>la diagonal punteada son los empates', `
<h2>Marcadores posibles</h2>
${matriz(p, pr)}
<div class="cuentas">
  ${cuenta(`Gana ${p.local.nombre}`, `suma bajo la diagonal = ${marca(M.pct(x('LOCAL')))}`)}
  ${cuenta('Empate', `suma de la diagonal = ${marca(M.pct(x('EMPATE')))}`)}
  ${cuenta(`Gana ${p.visitante.nombre}`, `suma sobre la diagonal = ${marca(M.pct(x('VISITA')))}`)}
</div>`)}
${bloque('Paso 3<br>cuota justa = 1 / probabilidad', `
<h2>Comparar con las casas</h2>
${cs ? '' : '<p class="aviso-seccion" role="status">Cuotas no disponibles por ahora. Las probabilidades del modelo siguen disponibles.</p>'}
${tablaMercados(p, pr, cs)}
<p class="nota">bet365 y Pinnacle solo publican 1X2 y más/menos 2.5 goles. <a href="#/partidos/${p.id}/cuota-betano">Ingresar una cuota de Betano</a></p>`)}
${bloque('Paso 4<br>VE = p × cuota − 1', `
<h2>Evaluar tu cuota</h2>
<form class="frase" data-accion="evaluar" novalidate>
  <input type="hidden" name="partidoId" value="${p.id}">
  <p>Si <select name="casa" class="blanco">${['bet365', 'betano', 'pinnacle'].map(c => `<option ${f.casa === c ? 'selected' : ''}>${c}</option>`).join('')}</select>
  paga ${blanco('cuota', ctx.valor('evaluar', 'cuota', '2.00'), 4, 'inputmode="decimal" aria-label="Cuota"')}
  por <select name="seleccion" class="blanco" aria-label="Selección">${opcionesMercado(p, f.seleccion)}</select>,
  <button class="boton">calcula el valor esperado</button></p>
  ${err(ctx, 'evaluar')}
</form>
<div class="vt-resultado">${resultadoEvaluacion(ev)}</div>`)}
${bloque('Notas', `
<h2>De dónde salen los números</h2>
<ul class="explicacion">${pr.explicacion.map(t => `<li>${t}</li>`).join('')}</ul>
<table class="tabla tabla-corta">
  <thead><tr><th scope="col">Promedio</th><th scope="col">${p.local.nombre} en casa</th><th scope="col">${p.visitante.nombre} fuera</th></tr></thead>
  <tbody>
    <tr><th scope="row">Goles a favor</th><td>${prom.local.golesAFavor.toFixed(2)}</td><td>${prom.visitante.golesAFavor.toFixed(2)}</td></tr>
    <tr><th scope="row">Goles en contra</th><td>${prom.local.golesEnContra.toFixed(2)}</td><td>${prom.visitante.golesEnContra.toFixed(2)}</td></tr>
    <tr><th scope="row">Córners</th><td>${prom.local.corners.toFixed(2)}</td><td>${prom.visitante.corners.toFixed(2)}</td></tr>
    <tr><th scope="row">Tarjetas</th><td>${prom.local.tarjetas.toFixed(2)}</td><td>${prom.visitante.tarjetas.toFixed(2)}</td></tr>
  </tbody>
</table>`)}`;
  },

  // /partidos/{id}/cuota-betano: también como frase.
  betano: ctx => {
    const p = M.partido(ctx.ruta.id);
    if (!p) return bloque('', '<p class="vacio">No encontramos ese partido. <a href="#/">Volver a Hoy</a></p>');
    const manuales = (M.cuotas(p.id) ?? []).filter(c => c.casa === 'betano');
    return `
${bloque(`<a href="#/partidos/${p.id}">Volver al análisis</a>`, `
<h1>Cuota de Betano</h1>
<p class="pick-detalle">${p.local.nombre} – ${p.visitante.nombre}. Betano no publica sus cuotas en una fuente gratuita, así que se apuntan a mano y quedan con tu usuario.</p>`)}
${bloque('Debe ser mayor que 1.01', `
<form class="frase" data-accion="betano" novalidate>
  <input type="hidden" name="partidoId" value="${p.id}">
  <p>Betano paga ${blanco('cuota', ctx.valor('betano', 'cuota'), 4, 'inputmode="decimal" placeholder="1.85" aria-label="Cuota"')}
  por <select name="seleccion" class="blanco" aria-label="Selección">${opcionesMercado(p, ctx.estado.form.betano?.seleccion ?? 'TARJETAS|MAS|4.5')}</select>.
  <button class="boton">Guardar cuota</button></p>
  ${err(ctx, 'betano')}
</form>`)}
${bloque('Ya apuntadas', manuales.length
    ? `<table class="tabla tabla-corta"><tbody>${manuales.map(c => `<tr><th scope="row">${M.etiqueta(c, p)}</th><td>${M.cuota(c.valor)}</td><td>${M.fechaHoraLima(c.capturadaEn)}</td></tr>`).join('')}</tbody></table>`
    : '<p class="nota">Todavía nadie apuntó cuotas de Betano para este partido.</p>')}`;
  },

  // /mis-apuestas
  misApuestas: ctx => {
    const r = M.rendimiento(), lista = M.misApuestas(ctx.estado.tab), sel = ctx.estado.form.apuesta ?? {};
    const partidoSel = M.partido(Number(sel.partidoId) || 1532);
    return `
${bloque('Solo cuentan las liquidadas', `
<h1>Mis apuestas</h1>
<div class="cuentas">
  ${cuenta('Apuestas', `${r.apuestas}, de ellas ${r.pendientes} pendientes, ${r.ganadas} ganadas y ${r.perdidas} perdidas`)}
  ${cuenta('Beneficio', `<span class="${claseSigno(r.beneficio)}">${M.soles(r.beneficio)}</span> sobre ${M.soles(r.montoApostado)} apostados`)}
  ${cuenta('ROI', `${r.beneficio.toFixed(2)} / ${r.montoApostado.toFixed(2)} = ${marca(M.signo(r.roi))}`, 'cuenta-fuerte')}
</div>`)}
${bloque('Apunta una nueva', `
<form class="frase" data-accion="apuesta" novalidate>
  <p>Aposté S/ ${blanco('monto', ctx.valor('apuesta', 'monto', '20.00'), 5, 'inputmode="decimal" aria-label="Monto"')}
  a <select name="seleccion" class="blanco" aria-label="Selección">${opcionesMercado(partidoSel, sel.seleccion)}</select>
  en <select name="partidoId" class="blanco" data-recargar aria-label="Partido">${M.partidosDelDia().map(p => `<option value="${p.id}" ${p.id === partidoSel.id ? 'selected' : ''}>${p.local.nombre} – ${p.visitante.nombre}</option>`).join('')}</select>,
  con cuota ${blanco('cuota', ctx.valor('apuesta', 'cuota', '2.00'), 4, 'inputmode="decimal" aria-label="Cuota"')}
  en <select name="casa" class="blanco" aria-label="Casa">${['bet365', 'betano'].map(c => `<option ${sel.casa === c ? 'selected' : ''}>${c}</option>`).join('')}</select>.
  <button class="boton">Registrar apuesta</button></p>
  ${err(ctx, 'apuesta')}
</form>`)}
${bloque('', `
<p class="filtro" role="group" aria-label="Estado">Ver <button data-clic="tab:PENDIENTE" aria-pressed="${ctx.estado.tab === 'PENDIENTE'}">pendientes</button>, <button data-clic="tab:LIQUIDADA" aria-pressed="${ctx.estado.tab === 'LIQUIDADA'}">liquidadas</button></p>
<table class="tabla">
  <thead><tr><th scope="col">Partido</th><th scope="col">Casa</th><th scope="col">Cuota</th><th scope="col">Monto</th><th scope="col">Resultado</th></tr></thead>
  <tbody>${lista.map(a => `
    <tr style="--vt: apuesta-${a.id}">
      <th scope="row">${a.partido}<small>${M.etiqueta(a, a.partido)}, ${M.fechaCorta(a.creadaEn.slice(0, 10))}</small></th>
      <td>${a.casa}</td><td>${M.cuota(a.cuota)}</td><td>${M.soles(a.monto)}</td>
      <td class="${claseSigno(a.ganancia ?? 0)}">${a.ganancia == null ? 'Pendiente' : M.soles(a.ganancia)}</td>
    </tr>`).join('') || '<tr><td colspan="5" class="vacio">No tienes apuestas en este estado.</td></tr>'}</tbody>
</table>`)}
${bloque('Por mercado', `
<table class="tabla tabla-corta">
  <thead><tr><th scope="col">Mercado</th><th scope="col">Apuestas</th><th scope="col">Beneficio</th><th scope="col">ROI</th></tr></thead>
  <tbody>${r.porMercado.map(m => `<tr><th scope="row">${M.NOMBRE_MERCADO[m.mercado]}</th><td>${m.apuestas}</td><td class="${claseSigno(m.beneficio)}">${M.soles(m.beneficio)}</td><td class="${claseSigno(m.roi)}">${M.signo(m.roi)}</td></tr>`).join('')}</tbody>
</table>`)}`;
  },

  // /historial
  historial: () => {
    const h = M.historialPicks();
    return `
${bloque('1 unidad por pick, sin importar tu banca', `
<h1>Historial de la apuesta del día</h1>
<div class="cuentas">
  ${cuenta('Picks en 30 días', `${h.picks}: ${h.ganados} ganados y ${h.perdidos} perdidos`)}
  ${cuenta('Beneficio', `${M.conSigno(h.beneficioUnidades)} unidades`)}
  ${cuenta('ROI', `${h.beneficioUnidades.toFixed(2)} / ${h.picks} = ${marca(M.signo(h.roi))}`, 'cuenta-fuerte')}
</div>`)}
${bloque('Beneficio acumulado', `<figure class="figura">${graficoBeneficio(M.curvaBeneficio(), { ancho: 900, alto: 220 })}<figcaption>Cada cuadro es un pick: verde si ganó, rojo si perdió.</figcaption></figure>`)}
${bloque('Últimos', `
<table class="tabla">
  <thead><tr><th scope="col">Partido</th><th scope="col">Selección</th><th scope="col">Cuota</th><th scope="col">Resultado</th></tr></thead>
  <tbody>${h.ultimos.map(k => `<tr><th scope="row">${k.partido}<small>${M.fechaCorta(k.fecha)}</small></th><td>${M.etiqueta(k, k.partido)}</td><td>${M.cuota(k.cuota)}</td><td class="${k.resultado === 'GANADA' ? 'positivo' : 'negativo'}">${k.resultado === 'GANADA' ? `${M.conSigno(k.cuota - 1)} u` : '−1.00 u'}</td></tr>`).join('')}</tbody>
</table>`)}`;
  },

  // /perfil
  perfil: ctx => {
    const u = M.perfil(), k = M.apuestaDelDia().pick;
    return `
${bloque('La web no mueve dinero real', `
<h1>Perfil</h1>
<div class="cuentas">
  ${cuenta('Usuario', esc(u.nombreUsuario))}
  ${cuenta('Correo', esc(u.email))}
  ${cuenta('Rol', u.rol === 'ADMIN' ? 'Administrador' : 'Apostador')}
  ${cuenta('Stake de hoy', `${M.pct(k?.stakePct ?? 0)} de ${M.soles(u.banca)} = ${marca(M.soles((k?.stakePct ?? 0) * u.banca))}`)}
</div>`)}
${bloque('Mayor o igual a 0', `
<form class="frase" data-accion="banca" novalidate>
  <p>Mi banca es S/ ${blanco('banca', ctx.valor('banca', 'banca', u.banca.toFixed(2)), 7, 'inputmode="decimal" aria-label="Banca"')}. <button class="boton">Guardar banca</button></p>
  ${err(ctx, 'banca')}
</form>`)}`;
  },

  // /admin/ingesta (solo ADMIN)
  admin: () => `
${bloque('Sola a las 05:00, 08:00 y 16:00 de Lima', `
<h1>Ingesta de datos</h1>
<p class="pick-detalle">Descarga los CSV de football-data y reparte los datos entre catálogo, estadísticas y cuotas. Desde aquí puedes lanzarla a mano.</p>
<p class="acciones"><button class="boton" data-clic="ingesta:HISTORICO">Cargar histórico</button> <button class="boton boton-linea" data-clic="ingesta:PROXIMOS">Cargar próximos partidos</button></p>`)}
${bloque('Ejecuciones', `
<table class="tabla">
  <thead><tr><th scope="col">N.º</th><th scope="col">Tipo</th><th scope="col">Fuente</th><th scope="col">Inicio</th><th scope="col">Fin</th><th scope="col">Filas</th><th scope="col">Estado</th></tr></thead>
  <tbody>${M.ejecuciones().map(e => `
    <tr style="--vt: ejecucion-${e.id}">
      <td>${e.id}</td><td>${e.tipo === 'HISTORICO' ? 'Histórico' : 'Próximos'}</td><td>${e.fuente}</td>
      <td>${M.fechaHoraLima(e.inicio)}</td><td>${e.fin ? M.fechaHoraLima(e.fin) : '—'}</td><td>${e.filas}</td>
      <td class="estado-${e.estado.toLowerCase()}">${{ OK: 'Correcta', ERROR: `Error: ${e.error}`, EN_CURSO: 'En curso' }[e.estado]}</td>
    </tr>`).join('')}</tbody>
</table>`)}`,
};
