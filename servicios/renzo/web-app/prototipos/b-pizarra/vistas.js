// =============================================================
// Propuesta B: «Pizarra». La web como la pizarra táctica de un
// vestuario: fondo salvia, líneas de tiza que dividen como las
// de una cancha, y la apuesta del día dibujada sobre un campo
// partido en dos mitades (local a la izquierda, visita a la
// derecha). Menú lateral, como la línea de banda.
// =============================================================
import { navegacion, opcionesMercado, graficoBeneficio, esc } from '../comun/app.js';
import * as M from '../comun/modelo.js';

// ---------- Piezas pequeñas ----------

const err = (ctx, form) => (ctx.error(form) ? `<p class="error" role="alert">${ctx.error(form)}</p>` : '');

const campo = (etiqueta, nombre, valor = '', tipo = 'text', extra = '') => `
<label class="campo"><span>${etiqueta}</span><input name="${nombre}" type="${tipo}" value="${valor}" ${extra}></label>`;

// Línea de medio campo: separa bloques grandes con una línea de tiza y su círculo central.
const lineaMedia = () => '<hr class="linea-media">';

// Cifras sueltas en fila, separadas por líneas finas.
const marcador = celdas => `<dl class="marcador">${celdas.map(([t, v, clase = '']) => `<div><dt>${t}</dt><dd class="${clase}">${v}</dd></div>`).join('')}</dl>`;
const claseSigno = x => (x > 0 ? 'positivo' : x < 0 ? 'negativo' : '');

// Encabezado de cada pantalla interna.
const titulo = (texto, bajada = '') => `<header class="titulo"><h1>${texto}</h1>${bajada ? `<p>${bajada}</p>` : ''}</header>`;

// Pie común: juego responsable (RN-WEB-07) y honestidad del modelo (RN-15).
const pie = () => `
<footer class="pie">
  <p><strong>Solo para mayores de 18 años. Juega con responsabilidad.</strong></p>
  <p>Las probabilidades salen de un modelo de Poisson didáctico. Sirven para aprender y comparar, no garantizan ganancias.</p>
</footer>`;

// ---------- Marco ----------

export const marco = {
  // Login y registro: la pizarra limpia, sin menú.
  publico: (cuerpo, ctx) => `
<div class="publico">
  ${ctx.aviso ? `<p class="aviso" role="status">${ctx.aviso}</p>` : ''}
  <main class="vt-contenido">${cuerpo}</main>
  ${pie()}
</div>`,

  // Con sesión: menú lateral (la banda) y el campo de juego a la derecha.
  privado: (cuerpo, ctx) => `
<div class="tablero">
  <aside class="banda">
    <a class="marca" href="#/">Pizarra</a>
    <p class="banda-fecha">${M.fechaLarga(M.HOY)}</p>
    <nav class="menu" aria-label="Secciones">
      ${navegacion(ctx).map(i => `<a href="${i.href}" ${i.activo ? 'aria-current="page"' : ''}>${i.activo ? '<span class="vt-indicador" aria-hidden="true"></span>' : ''}${i.texto}</a>`).join('')}
    </nav>
    <div class="banda-usuario">
      <p>${esc(ctx.sesion.nombreUsuario)}</p>
      <p class="banda-banca">${M.soles(M.perfil().banca)}</p>
      <button class="enlace" data-clic="salir">Salir</button>
    </div>
  </aside>
  <div class="juego">
    ${ctx.aviso ? `<p class="aviso" role="status">${ctx.aviso}</p>` : ''}
    <main class="vt-contenido">${cuerpo}</main>
    ${pie()}
  </div>
</div>`,
};

// ---------- La cancha ----------

// Campo dividido en dos mitades. `izquierda` y `derecha` son el HTML de cada mitad y `centro` va en el círculo.
const cancha = (izquierda, centro, derecha, enlace = null) => `
<div class="cancha vt-cancha">
  <div class="mitad mitad-local">${izquierda}</div>
  ${enlace ? `<a class="circulo" href="${enlace}">${centro}</a>` : `<div class="circulo">${centro}</div>`}
  <div class="mitad mitad-visita">${derecha}</div>
</div>`;

// Apuesta del día dibujada en la cancha: cada equipo en su mitad y la cuota en el círculo central.
function apuestaEnCancha(dia) {
  if (!dia.pick) return `<section class="sin-pick"><p>${dia.mensaje}</p></section>`;
  const k = dia.pick, p = M.partido(k.partidoId), pr = M.prediccion(p.id), banca = M.perfil().banca;
  return `
<section class="apuesta-dia" aria-label="Apuesta del día">
  <p class="rotulo">Apuesta del día, ${M.horaLima(p.fechaHora)} en Lima</p>
  ${cancha(
    `<p class="equipo">${p.local.nombre}</p><p class="mitad-dato">${pr.esperados.golesLocal.toFixed(2)} goles esperados</p>`,
    `<span class="circulo-cuota">${M.cuota(k.cuota)}</span><span>${k.casa}</span>`,
    `<p class="equipo">${p.visitante.nombre}</p><p class="mitad-dato">${pr.esperados.golesVisitante.toFixed(2)} goles esperados</p>`,
    `#/partidos/${p.id}`)}
  <h2 class="seleccion">${M.etiqueta(k, p)}</h2>
  ${marcador([['Modelo', M.pct(k.probabilidad)], ['Cuota justa', M.cuota(k.cuotaJusta)], ['Valor esperado', M.signo(k.valorEsperado), 'positivo'], ['Stake', `${M.pct(k.stakePct)} = ${M.soles(k.stakePct * banca)}`], ['Otras candidatas', dia.otrasCandidatas]])}
</section>`;
}

// Barra 1X2 entre los dos nombres: cuánto «tira» cada resultado según el modelo.
function barra1x2(pr) {
  const [l, e, v] = ['LOCAL', 'EMPATE', 'VISITA'].map(s => pr.mercados.find(m => m.mercado === '1X2' && m.seleccion === s).probabilidad);
  return `
<span class="barra" role="img" aria-label="Modelo: local ${M.pct(l, 0)}, empate ${M.pct(e, 0)}, visita ${M.pct(v, 0)}">
  <span class="b-local" style="flex-basis:${l * 100}%">${M.pct(l, 0)}</span>
  <span class="b-empate" style="flex-basis:${e * 100}%">${M.pct(e, 0)}</span>
  <span class="b-visita" style="flex-basis:${v * 100}%">${M.pct(v, 0)}</span>
</span>`;
}

// Fila de un partido: hora, local, barra del modelo, visita y las cuotas 1X2 de bet365.
function filaPartido(p) {
  const pr = M.prediccion(p.id), cs = M.cuotas(p.id);
  const cuotas = cs
    ? ['LOCAL', 'EMPATE', 'VISITA'].map(s => {
      const m = pr.mercados.find(x => x.mercado === '1X2' && x.seleccion === s);
      const c = M.buscarCuota(cs, 'bet365', M.clave(m));
      return `<span class="${c && c.valor > m.cuotaJusta ? 'con-valor' : ''}">${M.cuota(c?.valor)}</span>`;
    }).join('')
    : '<span class="sin-cuotas">Cuotas no disponibles por ahora</span>';
  return `
<li class="partido" style="--vt: fila-${p.id}">
  <time>${M.horaLima(p.fechaHora)}</time>
  <a class="partido-local" href="#/partidos/${p.id}">${p.local.nombre}</a>
  ${barra1x2(pr)}
  <a class="partido-visita" href="#/partidos/${p.id}">${p.visitante.nombre}</a>
  <span class="cuotas-1x2">${cuotas}</span>
</li>`;
}

// ---------- Análisis ----------

// Comparación en espejo: el local crece hacia la izquierda y la visita hacia la derecha desde la línea media.
function espejo(p, prom) {
  const filas = [['Goles a favor', 'golesAFavor'], ['Goles en contra', 'golesEnContra'], ['Córners', 'corners'], ['Tarjetas', 'tarjetas']];
  return `
<div class="espejo" role="table" aria-label="Promedios: ${p.local.nombre} en casa frente a ${p.visitante.nombre} fuera">
  ${filas.map(([t, k]) => {
    const a = prom.local[k], b = prom.visitante[k], max = Math.max(a, b) * 1.15;
    return `
  <div class="espejo-fila" role="row">
    <span role="cell" class="espejo-valor">${a.toFixed(2)}</span>
    <span role="cell" class="espejo-lado izq"><i style="width:${(a / max) * 100}%"></i></span>
    <span role="rowheader" class="espejo-etiqueta">${t}</span>
    <span role="cell" class="espejo-lado der"><i style="width:${(b / max) * 100}%"></i></span>
    <span role="cell" class="espejo-valor">${b.toFixed(2)}</span>
  </div>`;
  }).join('')}
</div>`;
}

// Tabla de mercados con las tres casas; en verde las cuotas que pagan más que la justa.
function tablaMercados(p, pr, cs) {
  const casas = cs ? ['bet365', 'pinnacle', 'betano'] : [];
  return `
<table class="tabla tabla-mercados">
  <thead><tr><th scope="col">Selección</th><th scope="col">Modelo</th><th scope="col">Justa</th>${casas.map(c => `<th scope="col">${c}</th>`).join('')}</tr></thead>
  ${Object.entries(M.NOMBRE_MERCADO).map(([mercado, nombre]) => `
  <tbody>
    <tr class="grupo"><th colspan="${3 + casas.length}" scope="rowgroup">${nombre}</th></tr>
    ${pr.mercados.filter(m => m.mercado === mercado).map(m => `
    <tr>
      <th scope="row">${M.etiqueta(m, p)}</th>
      <td><span class="prob" style="--p:${m.probabilidad}">${M.pct(m.probabilidad)}</span></td>
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

function resultadoEvaluacion(ev) {
  if (!ev) return '<p class="nota">El sistema toma la probabilidad del modelo; tú solo escribes la cuota.</p>';
  return `
<div class="resultado resultado-${ev.veredicto.toLowerCase()}">
  <p class="resultado-veredicto">${M.TEXTO_VEREDICTO[ev.veredicto]}</p>
  ${marcador([['Probabilidad', M.pct(ev.probabilidad)], ['Justa', M.cuota(ev.cuotaJusta)], ['EV', M.signo(ev.valorEsperado), claseSigno(ev.valorEsperado)], ['Stake', `${M.pct(ev.stakePct)} (${M.soles(ev.stakePct * M.perfil().banca)})`]])}
</div>`;
}

// ---------- Vistas ----------

export const vistas = {
  // /login: la cancha vacía como portada.
  login: ctx => `
<section class="acceso">
  <div class="acceso-portada">
    <p class="marca-grande">Pizarra</p>
    <p>Antes de apostar, mira el partido como lo ve un modelo: goles esperados, probabilidades y cuánto paga cada casa.</p>
  </div>
  <form class="acceso-form" data-accion="login" novalidate>
    <nav class="pestanas"><a href="#/login" aria-current="page">Iniciar sesión</a><a href="#/registro">Crear cuenta</a></nav>
    ${campo('Usuario', 'nombreUsuario', ctx.valor('login', 'nombreUsuario', 'renzo'), 'text', 'autocomplete="username"')}
    ${campo('Contraseña', 'clave', '', 'password', 'autocomplete="current-password"')}
    ${err(ctx, 'login')}
    <button class="boton">Entrar</button>
    <p class="nota">Prototipo: cualquier contraseña de 6 caracteres o más sirve. Entra como «admin» para ver la ingesta.</p>
  </form>
</section>`,

  // /registro
  registro: ctx => `
<section class="acceso">
  <div class="acceso-portada">
    <p class="marca-grande">Pizarra</p>
    <p>Tu cuenta guarda tus apuestas y tu rendimiento. Nadie más los ve.</p>
  </div>
  <form class="acceso-form" data-accion="registro" novalidate>
    <nav class="pestanas"><a href="#/login">Iniciar sesión</a><a href="#/registro" aria-current="page">Crear cuenta</a></nav>
    ${campo('Usuario', 'nombreUsuario', ctx.valor('registro', 'nombreUsuario'), 'text', 'maxlength="30" autocomplete="username"')}
    ${campo('Correo', 'email', ctx.valor('registro', 'email'), 'email', 'autocomplete="email"')}
    ${campo('Contraseña', 'clave', '', 'password', 'autocomplete="new-password"')}
    ${campo('Repite la contraseña', 'claveConfirmacion', '', 'password', 'autocomplete="new-password"')}
    ${err(ctx, 'registro')}
    <button class="boton">Crear cuenta</button>
  </form>
</section>`,

  // / (Hoy): la apuesta del día en la cancha y los partidos agrupados por liga.
  hoy: ctx => {
    const filtros = [['TODAS', 'Todas'], ...M.LIGAS.map(l => [l.codigo, l.nombre])];
    const ligas = M.LIGAS.filter(l => ctx.estado.liga === 'TODAS' || l.codigo === ctx.estado.liga);
    return `
${apuestaEnCancha(M.apuestaDelDia())}
${lineaMedia()}
<section aria-label="Partidos de hoy">
  <div class="cabecera-lista">
    <h2>Partidos de hoy</h2>
    <div class="filtro" role="group" aria-label="Filtrar por liga">
      ${filtros.map(([c, t]) => `<button data-clic="liga:${c}" aria-pressed="${ctx.estado.liga === c}">${t}</button>`).join('')}
    </div>
  </div>
  <p class="nota">La barra reparte la probabilidad del modelo entre local, empate y visita. A la derecha, las cuotas 1X2 de bet365; en verde, las que pagan más que la cuota justa.</p>
  ${ligas.map(l => `
  <h3 class="liga" style="--vt: liga-${l.codigo}">${l.nombre}</h3>
  <ol class="partidos">${M.partidosDelDia(l.codigo).map(filaPartido).join('')}</ol>`).join('')}
</section>`;
  },

  // /partidos/{id} (Análisis)
  partido: ctx => {
    const p = M.partido(ctx.ruta.id);
    if (!p) return '<p class="vacio">No encontramos ese partido. <a href="#/">Volver a Hoy</a></p>';
    const pr = M.prediccion(p.id), cs = M.cuotas(p.id), prom = M.promedios(p.id), e = pr.esperados;
    const ev = ctx.estado.evaluacion?.partidoId === p.id ? ctx.estado.evaluacion : null;
    const x = s => pr.mercados.find(m => m.mercado === '1X2' && m.seleccion === s).probabilidad;
    return `
<a class="volver" href="#/">Volver a Hoy</a>
<p class="rotulo">${p.liga.nombre}, ${M.horaLima(p.fechaHora)} en Lima. ${p.arbitro ? `Árbitro ${p.arbitro.nombre}.` : 'Sin árbitro asignado.'}</p>
${cancha(
  `<p class="equipo">${p.local.nombre}</p><p class="mitad-dato">Gana ${M.pct(x('LOCAL'))}</p><p class="mitad-dato">${e.golesLocal.toFixed(2)} goles esperados</p>`,
  `<span class="circulo-titulo">Empate</span><span class="circulo-cuota">${M.pct(x('EMPATE'), 0)}</span>`,
  `<p class="equipo">${p.visitante.nombre}</p><p class="mitad-dato">Gana ${M.pct(x('VISITA'))}</p><p class="mitad-dato">${e.golesVisitante.toFixed(2)} goles esperados</p>`)}
${espejo(p, prom)}
${marcador([['Córners esperados', e.corners.toFixed(1)], ['Tarjetas esperadas', e.tarjetas.toFixed(1)], ['Tiros esperados', e.tiros.toFixed(1)], ['Factor árbitro', pr.factorArbitro.toFixed(2)], ['Muestra', `${pr.muestra.partidosLocal} y ${pr.muestra.partidosVisitante}`]])}
${lineaMedia()}
<div class="columnas">
  <section>
    <h2>Mercados</h2>
    ${cs ? '' : '<p class="aviso-seccion" role="status">Cuotas no disponibles por ahora. Las probabilidades del modelo siguen disponibles.</p>'}
    ${tablaMercados(p, pr, cs)}
    <p class="nota">bet365 y Pinnacle solo publican 1X2 y más/menos 2.5 goles. <a href="#/partidos/${p.id}/cuota-betano">Ingresar una cuota de Betano</a></p>
  </section>
  <aside>
    <h2>Evaluar una cuota</h2>
    <form data-accion="evaluar" novalidate>
      <input type="hidden" name="partidoId" value="${p.id}">
      <label class="campo"><span>Selección</span><select name="seleccion">${opcionesMercado(p, ctx.estado.form.evaluar?.seleccion)}</select></label>
      <div class="campos-par">
        <label class="campo"><span>Casa</span><select name="casa">${['bet365', 'betano', 'pinnacle'].map(c => `<option ${ctx.estado.form.evaluar?.casa === c ? 'selected' : ''}>${c}</option>`).join('')}</select></label>
        ${campo('Cuota', 'cuota', ctx.valor('evaluar', 'cuota', '2.00'), 'text', 'inputmode="decimal"')}
      </div>
      ${err(ctx, 'evaluar')}
      <button class="boton">Evaluar cuota</button>
    </form>
    <div class="vt-resultado">${resultadoEvaluacion(ev)}</div>
    <h2>De dónde salen los números</h2>
    <ul class="explicacion">${pr.explicacion.map(f => `<li>${f}</li>`).join('')}</ul>
  </aside>
</div>`;
  },

  // /partidos/{id}/cuota-betano
  betano: ctx => {
    const p = M.partido(ctx.ruta.id);
    if (!p) return '<p class="vacio">No encontramos ese partido. <a href="#/">Volver a Hoy</a></p>';
    const manuales = (M.cuotas(p.id) ?? []).filter(c => c.casa === 'betano');
    return `
<a class="volver" href="#/partidos/${p.id}">Volver al análisis</a>
${titulo(`${p.local.nombre} – ${p.visitante.nombre}`, 'Betano no publica sus cuotas en una fuente gratuita, así que se cargan a mano y se guardan con tu usuario.')}
<div class="columnas">
  <form data-accion="betano" novalidate>
    <h2>Cuota de Betano</h2>
    <input type="hidden" name="partidoId" value="${p.id}">
    <label class="campo"><span>Selección</span><select name="seleccion">${opcionesMercado(p, ctx.estado.form.betano?.seleccion ?? 'TARJETAS|MAS|4.5')}</select></label>
    ${campo('Cuota', 'cuota', ctx.valor('betano', 'cuota'), 'text', 'inputmode="decimal" placeholder="1.85"')}
    ${err(ctx, 'betano')}
    <button class="boton">Guardar cuota</button>
  </form>
  <aside>
    <h2>Ya cargadas</h2>
    ${manuales.length ? `<table class="tabla"><tbody>${manuales.map(c => `<tr><th scope="row">${M.etiqueta(c, p)}</th><td>${M.cuota(c.valor)}</td><td>${M.fechaHoraLima(c.capturadaEn)}</td></tr>`).join('')}</tbody></table>` : '<p class="nota">Todavía nadie cargó cuotas de Betano para este partido.</p>'}
  </aside>
</div>`;
  },

  // /mis-apuestas
  misApuestas: ctx => {
    const r = M.rendimiento(), lista = M.misApuestas(ctx.estado.tab), sel = ctx.estado.form.apuesta ?? {};
    const partidoSel = M.partido(Number(sel.partidoId) || 1532);
    return `
${titulo('Mis apuestas', 'Solo tú ves estas apuestas. El rendimiento cuenta solo las ya liquidadas.')}
${marcador([['Apuestas', r.apuestas], ['Pendientes', r.pendientes], ['Ganadas', r.ganadas], ['Perdidas', r.perdidas], ['Apostado', M.soles(r.montoApostado)], ['Beneficio', M.soles(r.beneficio), claseSigno(r.beneficio)], ['ROI', M.signo(r.roi), claseSigno(r.roi)]])}
${lineaMedia()}
<div class="columnas">
  <section>
    <div class="filtro" role="group" aria-label="Estado">
      <button data-clic="tab:PENDIENTE" aria-pressed="${ctx.estado.tab === 'PENDIENTE'}">Pendientes</button>
      <button data-clic="tab:LIQUIDADA" aria-pressed="${ctx.estado.tab === 'LIQUIDADA'}">Liquidadas</button>
    </div>
    <table class="tabla">
      <thead><tr><th scope="col">Partido</th><th scope="col">Casa</th><th scope="col">Cuota</th><th scope="col">Monto</th><th scope="col">Resultado</th></tr></thead>
      <tbody>${lista.map(a => `
        <tr style="--vt: apuesta-${a.id}">
          <th scope="row">${a.partido}<small>${M.etiqueta(a, a.partido)}, ${M.fechaCorta(a.creadaEn.slice(0, 10))}</small></th>
          <td>${a.casa}</td><td>${M.cuota(a.cuota)}</td><td>${M.soles(a.monto)}</td>
          <td class="${claseSigno(a.ganancia ?? 0)}">${a.ganancia == null ? 'Pendiente' : M.soles(a.ganancia)}</td>
        </tr>`).join('') || '<tr><td colspan="5" class="vacio">No tienes apuestas en este estado.</td></tr>'}</tbody>
    </table>
    <h2>Por mercado</h2>
    <table class="tabla">
      <thead><tr><th scope="col">Mercado</th><th scope="col">Apuestas</th><th scope="col">Beneficio</th><th scope="col">ROI</th></tr></thead>
      <tbody>${r.porMercado.map(m => `<tr><th scope="row">${M.NOMBRE_MERCADO[m.mercado]}</th><td>${m.apuestas}</td><td class="${claseSigno(m.beneficio)}">${M.soles(m.beneficio)}</td><td class="${claseSigno(m.roi)}">${M.signo(m.roi)}</td></tr>`).join('')}</tbody>
    </table>
  </section>
  <aside>
    <h2>Registrar una apuesta</h2>
    <form data-accion="apuesta" novalidate>
      <label class="campo"><span>Partido</span><select name="partidoId" data-recargar>${M.partidosDelDia().map(p => `<option value="${p.id}" ${p.id === partidoSel.id ? 'selected' : ''}>${M.horaLima(p.fechaHora)} ${p.local.nombre} vs ${p.visitante.nombre}</option>`).join('')}</select></label>
      <label class="campo"><span>Selección</span><select name="seleccion">${opcionesMercado(partidoSel, sel.seleccion)}</select></label>
      <div class="campos-par">
        <label class="campo"><span>Casa</span><select name="casa">${['bet365', 'betano'].map(c => `<option ${sel.casa === c ? 'selected' : ''}>${c}</option>`).join('')}</select></label>
        ${campo('Cuota', 'cuota', ctx.valor('apuesta', 'cuota'), 'text', 'inputmode="decimal" placeholder="2.10"')}
      </div>
      ${campo('Monto (S/)', 'monto', ctx.valor('apuesta', 'monto'), 'text', 'inputmode="decimal" placeholder="20.00"')}
      ${err(ctx, 'apuesta')}
      <button class="boton">Registrar apuesta</button>
    </form>
  </aside>
</div>`;
  },

  // /historial
  historial: () => {
    const h = M.historialPicks();
    return `
${titulo('Historial de la apuesta del día', 'Últimos 30 días con 1 unidad por pick. Mide la calidad de las recomendaciones, no la de tu banca.')}
${marcador([['Picks', h.picks], ['Ganados', h.ganados], ['Perdidos', h.perdidos], ['Beneficio', `${M.conSigno(h.beneficioUnidades)} u`, claseSigno(h.beneficioUnidades)], ['ROI', M.signo(h.roi), claseSigno(h.roi)]])}
<figure class="figura">${graficoBeneficio(M.curvaBeneficio(), { ancho: 960, alto: 220 })}<figcaption>Beneficio acumulado en unidades. Cuadro verde: pick ganado. Rojo: perdido.</figcaption></figure>
<table class="tabla">
  <thead><tr><th scope="col">Partido</th><th scope="col">Selección</th><th scope="col">Cuota</th><th scope="col">Resultado</th></tr></thead>
  <tbody>${h.ultimos.map(k => `<tr><th scope="row">${k.partido}<small>${M.fechaCorta(k.fecha)}</small></th><td>${M.etiqueta(k, k.partido)}</td><td>${M.cuota(k.cuota)}</td><td class="${k.resultado === 'GANADA' ? 'positivo' : 'negativo'}">${k.resultado === 'GANADA' ? `${M.conSigno(k.cuota - 1)} u` : '−1.00 u'}</td></tr>`).join('')}</tbody>
</table>`;
  },

  // /perfil
  perfil: ctx => {
    const u = M.perfil(), k = M.apuestaDelDia().pick;
    return `
${titulo('Perfil', 'La banca es el monto que dedicas a apostar. Lo declaras tú: la web no mueve dinero real.')}
<div class="columnas">
  <section>
    ${marcador([['Usuario', esc(u.nombreUsuario)], ['Correo', esc(u.email)], ['Rol', u.rol === 'ADMIN' ? 'Administrador' : 'Apostador']])}
    <p class="banca-grande">${M.soles(u.banca)}</p>
    <p class="nota">Con esta banca, el stake de la apuesta de hoy (${M.pct(k?.stakePct ?? 0)}) es ${M.soles((k?.stakePct ?? 0) * u.banca)}.</p>
  </section>
  <form data-accion="banca" novalidate>
    <h2>Actualizar banca</h2>
    ${campo('Nueva banca (S/)', 'banca', ctx.valor('banca', 'banca', u.banca.toFixed(2)), 'text', 'inputmode="decimal"')}
    ${err(ctx, 'banca')}
    <button class="boton">Guardar banca</button>
  </form>
</div>`;
  },

  // /admin/ingesta (solo ADMIN)
  admin: () => `
${titulo('Ingesta de datos', 'Corre sola a las 05:00 (histórico) y a las 08:00 y 16:00 (próximos), hora de Lima. Desde aquí puedes lanzarla a mano.')}
<div class="acciones-ingesta">
  <button class="boton" data-clic="ingesta:HISTORICO">Cargar histórico</button>
  <button class="boton boton-linea" data-clic="ingesta:PROXIMOS">Cargar próximos partidos</button>
</div>
${lineaMedia()}
<table class="tabla">
  <thead><tr><th scope="col">N.º</th><th scope="col">Tipo</th><th scope="col">Fuente</th><th scope="col">Inicio</th><th scope="col">Fin</th><th scope="col">Filas</th><th scope="col">Estado</th></tr></thead>
  <tbody>${M.ejecuciones().map(e => `
    <tr style="--vt: ejecucion-${e.id}">
      <td>${e.id}</td><td>${e.tipo === 'HISTORICO' ? 'Histórico' : 'Próximos'}</td><td>${e.fuente}</td>
      <td>${M.fechaHoraLima(e.inicio)}</td><td>${e.fin ? M.fechaHoraLima(e.fin) : '—'}</td><td>${e.filas}</td>
      <td class="estado-${e.estado.toLowerCase()}">${{ OK: 'Correcta', ERROR: `Error: ${e.error}`, EN_CURSO: 'En curso' }[e.estado]}</td>
    </tr>`).join('')}</tbody>
</table>`,
};
