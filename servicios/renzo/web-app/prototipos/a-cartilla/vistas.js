// =============================================================
// Propuesta A: «Cartilla». La web se lee como una cartilla de
// pronósticos impresa: filas numeradas, casillas 1 X 2 con la
// cuota de la casa y la apuesta del día como un boleto con
// talón perforado. Paleta melocotón.
// =============================================================
import { navegacion, opcionesMercado, graficoBeneficio, esc } from '../comun/app.js';
import * as M from '../comun/modelo.js';

// ---------- Piezas pequeñas ----------

// Mensaje de error de un formulario, si lo hay.
const err = (ctx, form) => (ctx.error(form) ? `<p class="error" role="alert">${ctx.error(form)}</p>` : '');

// Campo de texto con su etiqueta encima y solo una línea debajo (sin cajas).
const campo = (etiqueta, nombre, valor = '', tipo = 'text', extra = '') => `
<label class="campo"><span>${etiqueta}</span><input name="${nombre}" type="${tipo}" value="${valor}" ${extra}></label>`;

// Número de cartilla: el día del año, como en los cupones impresos.
const numeroCartilla = () => Math.round((new Date(M.HOY) - new Date('2026-01-01')) / 864e5) + 1;

// Pie común: juego responsable (RN-WEB-07) y nota de honestidad sobre el modelo (RN-15).
const pie = () => `
<footer class="pie">
  <p class="pie-aviso">Solo para mayores de 18 años. Juega con responsabilidad.</p>
  <p>Las probabilidades salen de un modelo de Poisson didáctico. Sirven para aprender y comparar con las casas, no garantizan ganancias.</p>
</footer>`;

// ---------- Marco ----------

export const marco = {
  // Login y registro: sin menú.
  publico: (cuerpo, ctx) => `
<div class="hoja hoja-publica">
  ${ctx.aviso ? `<p class="aviso" role="status">${ctx.aviso}</p>` : ''}
  <main class="vt-contenido">${cuerpo}</main>
  ${pie()}
</div>`,

  // Pantallas con sesión: encabezado de cartilla, menú con indicador que se desliza y pie.
  privado: (cuerpo, ctx) => `
<div class="hoja">
  <header class="cabecera">
    <a class="marca" href="#/">Cartilla</a>
    <p class="cabecera-fecha"><span>${M.fechaLarga(M.HOY)}</span><span>N.º ${numeroCartilla()}</span></p>
    <p class="cabecera-usuario">
      <span>${esc(ctx.sesion.nombreUsuario)}</span>
      <span>Banca ${M.soles(M.perfil().banca)}</span>
      <button class="enlace" data-clic="salir">Salir</button>
    </p>
    <nav class="menu" aria-label="Secciones">
      ${navegacion(ctx).map(i => `<a href="${i.href}" ${i.activo ? 'aria-current="page"' : ''}>${i.texto}${i.activo ? '<span class="vt-indicador" aria-hidden="true"></span>' : ''}</a>`).join('')}
    </nav>
  </header>
  ${ctx.aviso ? `<p class="aviso" role="status">${ctx.aviso}</p>` : ''}
  <main class="vt-contenido">${cuerpo}</main>
  ${pie()}
</div>`,
};

// ---------- Hoy ----------

// Boleto de la apuesta del día: el cuerpo con el partido y el talón con la cuota, el EV y el stake.
function boleto(dia) {
  if (!dia.pick) return `<section class="boleto boleto-vacio"><p>${dia.mensaje}</p></section>`;
  const k = dia.pick, p = M.partido(k.partidoId), banca = M.perfil().banca;
  return `
<section class="boleto" aria-label="Apuesta del día">
  <div class="boleto-cuerpo">
    <p class="boleto-titulo">Apuesta del día</p>
    <h1 class="boleto-partido"><a href="#/partidos/${p.id}">${p.local.nombre} <i>–</i> ${p.visitante.nombre}</a></h1>
    <p class="boleto-seleccion">${M.etiqueta(k, p)} en ${k.casa}</p>
    <dl class="datos">
      <div><dt>Hora de Lima</dt><dd>${M.horaLima(p.fechaHora)}</dd></div>
      <div><dt>Probabilidad del modelo</dt><dd>${M.pct(k.probabilidad)}</dd></div>
      <div><dt>Cuota justa</dt><dd>${M.cuota(k.cuotaJusta)}</dd></div>
      <div><dt>Otras candidatas</dt><dd>${dia.otrasCandidatas}</dd></div>
    </dl>
  </div>
  <div class="boleto-talon">
    <span class="talon-cuota">${M.cuota(k.cuota)}</span>
    <span>Valor esperado <b>${M.signo(k.valorEsperado)}</b></span>
    <span>Stake ${M.pct(k.stakePct)} de tu banca: <b>${M.soles(k.stakePct * banca)}</b></span>
  </div>
</section>`;
}

// Una fila del cupón: número, hora, liga, equipos y las casillas 1 X 2 con la cuota de bet365.
function filaCupon(p, numero) {
  const pr = M.prediccion(p.id), cs = M.cuotas(p.id);
  const casillas = cs
    ? ['LOCAL', 'EMPATE', 'VISITA'].map((s, i) => {
      const m = pr.mercados.find(x => x.mercado === '1X2' && x.seleccion === s);
      const c = M.buscarCuota(cs, 'bet365', M.clave(m));
      const valor = c && c.valor > m.cuotaJusta;
      return `<span class="casilla ${valor ? 'con-valor' : ''}"><b>${['1', 'X', '2'][i]}</b><span>${M.cuota(c?.valor)}</span><small>${M.pct(m.probabilidad, 0)}</small></span>`;
    }).join('')
    : '<span class="casilla-vacia">Cuotas no disponibles por ahora</span>';
  return `
<li class="fila" style="--vt: fila-${p.id}">
  <span class="fila-num">${numero}</span>
  <time class="fila-hora">${M.horaLima(p.fechaHora)}</time>
  <span class="fila-liga" title="${p.liga.nombre}">${p.liga.codigo}</span>
  <a class="fila-partido" href="#/partidos/${p.id}"><span>${p.local.nombre}</span><span>${p.visitante.nombre}</span></a>
  <span class="casillas">${casillas}</span>
</li>`;
}

// ---------- Análisis ----------

// Tabla de mercados: probabilidad del modelo y cuota justa frente a las cuotas de cada casa.
function tablaMercados(p, pr, cs) {
  const casas = cs ? ['bet365', 'pinnacle', 'betano'] : [];
  const grupos = Object.entries(M.NOMBRE_MERCADO).map(([mercado, nombre]) => `
    <tbody>
      <tr class="grupo"><th colspan="${3 + casas.length}" scope="rowgroup">${nombre}</th></tr>
      ${pr.mercados.filter(m => m.mercado === mercado).map(m => `
      <tr>
        <th scope="row">${M.etiqueta(m, p)}</th>
        <td>${M.pct(m.probabilidad)}</td>
        <td class="justa">${M.cuota(m.cuotaJusta)}</td>
        ${casas.map(casa => {
          const c = M.buscarCuota(cs, casa, M.clave(m));
          const ev = c ? m.probabilidad * c.valor - 1 : null;
          return `<td class="${ev > 0 ? 'con-valor' : ''}" ${ev != null ? `title="EV ${M.signo(ev)}"` : ''}>${M.cuota(c?.valor)}</td>`;
        }).join('')}
      </tr>`).join('')}
    </tbody>`).join('');
  return `
<table class="tabla tabla-mercados">
  <thead><tr><th scope="col">Selección</th><th scope="col">Modelo</th><th scope="col">Justa</th>${casas.map(c => `<th scope="col">${c}</th>`).join('')}</tr></thead>
  ${grupos}
</table>`;
}

// Resultado de evaluar una cuota, como un talón pequeño.
function resultadoEvaluacion(ev) {
  if (!ev) return '<p class="nota">Escribe la cuota que te ofrece la casa y el sistema la compara con la probabilidad del modelo.</p>';
  return `
<div class="resultado resultado-${ev.veredicto.toLowerCase()}">
  <p class="resultado-veredicto">${M.TEXTO_VEREDICTO[ev.veredicto]}</p>
  <dl class="datos datos-columna">
    <div><dt>Probabilidad</dt><dd>${M.pct(ev.probabilidad)}</dd></div>
    <div><dt>Cuota justa</dt><dd>${M.cuota(ev.cuotaJusta)}</dd></div>
    <div><dt>Valor esperado</dt><dd>${M.signo(ev.valorEsperado)}</dd></div>
    <div><dt>Stake sugerido</dt><dd>${M.pct(ev.stakePct)} (${M.soles(ev.stakePct * M.perfil().banca)})</dd></div>
  </dl>
</div>`;
}

// ---------- Mis apuestas e historial ----------

// Fila de una apuesta del usuario, con su resultado si ya se liquidó.
const filaApuesta = a => `
<tr style="--vt: apuesta-${a.id}">
  <td>${M.fechaCorta(a.creadaEn.slice(0, 10))}</td>
  <th scope="row">${a.partido}<small>${M.etiqueta(a, a.partido)}</small></th>
  <td>${a.casa}</td>
  <td>${M.cuota(a.cuota)}</td>
  <td>${M.soles(a.monto)}</td>
  <td class="${a.ganancia > 0 ? 'positivo' : a.ganancia < 0 ? 'negativo' : ''}">${a.ganancia == null ? 'Pendiente' : M.soles(a.ganancia)}</td>
</tr>`;

// Franja de cifras separadas por reglas verticales.
const franja = celdas => `<dl class="franja">${celdas.map(([t, v, clase = '']) => `<div><dt>${t}</dt><dd class="${clase}">${v}</dd></div>`).join('')}</dl>`;
const claseSigno = x => (x > 0 ? 'positivo' : x < 0 ? 'negativo' : '');

// ---------- Vistas ----------

export const vistas = {
  // /login
  login: ctx => `
<section class="acceso">
  <div class="acceso-portada">
    <p class="marca-grande">Cartilla</p>
    <p>Compara la probabilidad de un modelo de Poisson con las cuotas de bet365, Pinnacle y Betano antes de decidir.</p>
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
    <p class="marca-grande">Cartilla</p>
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

  // / (Hoy): apuesta del día y cupón con los partidos, filtrable por liga.
  hoy: ctx => {
    const todos = M.partidosDelDia();
    const visibles = M.partidosDelDia(ctx.estado.liga);
    const filtros = [['TODAS', 'Todas'], ...M.LIGAS.map(l => [l.codigo, l.nombre])];
    return `
${boleto(M.apuestaDelDia())}
<section class="cupon" aria-label="Partidos de hoy">
  <div class="cupon-cabecera">
    <h2>Partidos de hoy</h2>
    <div class="filtro" role="group" aria-label="Filtrar por liga">
      ${filtros.map(([c, t]) => `<button data-clic="liga:${c}" aria-pressed="${ctx.estado.liga === c}">${t}</button>`).join('')}
    </div>
  </div>
  <p class="cupon-leyenda">En cada casilla, la cuota de bet365 y debajo la probabilidad del modelo. En verde, las cuotas que pagan más que la cuota justa.</p>
  <ol class="filas">${visibles.map(p => filaCupon(p, todos.indexOf(p) + 1)).join('')}</ol>
</section>`;
  },

  // /partidos/{id} (Análisis): esperados, promedios, mercados con cuotas y el formulario para evaluar una cuota.
  partido: ctx => {
    const p = M.partido(ctx.ruta.id);
    if (!p) return '<p class="vacio">No encontramos ese partido. <a href="#/">Volver a Hoy</a></p>';
    const pr = M.prediccion(p.id), cs = M.cuotas(p.id), prom = M.promedios(p.id), e = pr.esperados;
    const ev = ctx.estado.evaluacion?.partidoId === p.id ? ctx.estado.evaluacion : null;
    return `
<a class="volver" href="#/">Volver a Hoy</a>
<section class="partido-cabecera">
  <p>${p.liga.nombre}, ${M.horaLima(p.fechaHora)} (hora de Lima)</p>
  <h1>${p.local.nombre} <i>–</i> ${p.visitante.nombre}</h1>
  <p>${p.arbitro ? `Árbitro ${p.arbitro.nombre}, factor ${pr.factorArbitro.toFixed(2)}` : 'Sin árbitro asignado, factor 1.00'}. Muestra de ${pr.muestra.partidosLocal} y ${pr.muestra.partidosVisitante} partidos.</p>
</section>
${franja([[`Goles ${p.local.nombre}`, e.golesLocal.toFixed(2)], [`Goles ${p.visitante.nombre}`, e.golesVisitante.toFixed(2)], ['Córners', e.corners.toFixed(1)], ['Tarjetas', e.tarjetas.toFixed(1)], ['Tiros', e.tiros.toFixed(1)]])}
<div class="columnas">
  <section>
    <h2>Mercados</h2>
    ${cs ? '' : '<p class="aviso-seccion" role="status">Cuotas no disponibles por ahora. Las probabilidades del modelo siguen disponibles.</p>'}
    ${tablaMercados(p, pr, cs)}
    <p class="nota">bet365 y Pinnacle solo publican 1X2 y más/menos 2.5 goles. <a href="#/partidos/${p.id}/cuota-betano">Ingresar una cuota de Betano</a></p>
  </section>
  <aside>
    <h2>Evaluar una cuota</h2>
    <form class="form-evaluar" data-accion="evaluar" novalidate>
      <input type="hidden" name="partidoId" value="${p.id}">
      <label class="campo"><span>Selección</span><select name="seleccion">${opcionesMercado(p, ctx.estado.form.evaluar?.seleccion)}</select></label>
      <label class="campo"><span>Casa</span><select name="casa">${['bet365', 'betano', 'pinnacle'].map(c => `<option ${ctx.estado.form.evaluar?.casa === c ? 'selected' : ''}>${c}</option>`).join('')}</select></label>
      ${campo('Cuota', 'cuota', ctx.valor('evaluar', 'cuota', '2.00'), 'text', 'inputmode="decimal"')}
      ${err(ctx, 'evaluar')}
      <button class="boton">Evaluar cuota</button>
    </form>
    <div class="vt-resultado">${resultadoEvaluacion(ev)}</div>
    <h2>Promedios</h2>
    <table class="tabla tabla-compacta">
      <thead><tr><th></th><th scope="col">${p.local.nombre} en casa</th><th scope="col">${p.visitante.nombre} fuera</th></tr></thead>
      <tbody>
        <tr><th scope="row">Goles a favor</th><td>${prom.local.golesAFavor.toFixed(2)}</td><td>${prom.visitante.golesAFavor.toFixed(2)}</td></tr>
        <tr><th scope="row">Goles en contra</th><td>${prom.local.golesEnContra.toFixed(2)}</td><td>${prom.visitante.golesEnContra.toFixed(2)}</td></tr>
        <tr><th scope="row">Córners</th><td>${prom.local.corners.toFixed(2)}</td><td>${prom.visitante.corners.toFixed(2)}</td></tr>
        <tr><th scope="row">Tarjetas</th><td>${prom.local.tarjetas.toFixed(2)}</td><td>${prom.visitante.tarjetas.toFixed(2)}</td></tr>
      </tbody>
    </table>
    <h2>De dónde salen los números</h2>
    <ul class="explicacion">${pr.explicacion.map(f => `<li>${f}</li>`).join('')}</ul>
  </aside>
</div>`;
  },

  // /partidos/{id}/cuota-betano: formulario para cargar una cuota de Betano a mano.
  betano: ctx => {
    const p = M.partido(ctx.ruta.id);
    if (!p) return '<p class="vacio">No encontramos ese partido. <a href="#/">Volver a Hoy</a></p>';
    const manuales = (M.cuotas(p.id) ?? []).filter(c => c.casa === 'betano');
    return `
<a class="volver" href="#/partidos/${p.id}">Volver al análisis</a>
<section class="partido-cabecera">
  <p>Cuota de Betano</p>
  <h1>${p.local.nombre} <i>–</i> ${p.visitante.nombre}</h1>
  <p>Betano no publica sus cuotas en una fuente gratuita, así que se cargan a mano. Se guardan con tu usuario.</p>
</section>
<div class="columnas">
  <form class="form-betano" data-accion="betano" novalidate>
    <input type="hidden" name="partidoId" value="${p.id}">
    <label class="campo"><span>Selección</span><select name="seleccion">${opcionesMercado(p, ctx.estado.form.betano?.seleccion ?? 'TARJETAS|MAS|4.5')}</select></label>
    ${campo('Cuota de Betano', 'cuota', ctx.valor('betano', 'cuota'), 'text', 'inputmode="decimal" placeholder="1.85"')}
    ${err(ctx, 'betano')}
    <button class="boton">Guardar cuota</button>
  </form>
  <aside>
    <h2>Ya cargadas</h2>
    ${manuales.length ? `<table class="tabla tabla-compacta"><tbody>${manuales.map(c => `<tr><th scope="row">${M.etiqueta(c, p)}</th><td>${M.cuota(c.valor)}</td><td>${M.fechaHoraLima(c.capturadaEn)}</td></tr>`).join('')}</tbody></table>` : '<p class="nota">Todavía nadie cargó cuotas de Betano para este partido.</p>'}
  </aside>
</div>`;
  },

  // /mis-apuestas: rendimiento, lista por estado y formulario para registrar una apuesta.
  misApuestas: ctx => {
    const r = M.rendimiento(), lista = M.misApuestas(ctx.estado.tab), sel = ctx.estado.form.apuesta ?? {};
    return `
<section class="partido-cabecera"><h1>Mis apuestas</h1><p>Solo tú ves estas apuestas. El rendimiento cuenta solo las ya liquidadas.</p></section>
${franja([['Apuestas', r.apuestas], ['Pendientes', r.pendientes], ['Ganadas', r.ganadas], ['Perdidas', r.perdidas], ['Apostado', M.soles(r.montoApostado)], ['Beneficio', M.soles(r.beneficio), claseSigno(r.beneficio)], ['ROI', M.signo(r.roi), claseSigno(r.roi)]])}
<div class="columnas">
  <section>
    <div class="filtro" role="group" aria-label="Estado">
      <button data-clic="tab:PENDIENTE" aria-pressed="${ctx.estado.tab === 'PENDIENTE'}">Pendientes</button>
      <button data-clic="tab:LIQUIDADA" aria-pressed="${ctx.estado.tab === 'LIQUIDADA'}">Liquidadas</button>
    </div>
    <table class="tabla tabla-apuestas">
      <thead><tr><th scope="col">Fecha</th><th scope="col">Partido</th><th scope="col">Casa</th><th scope="col">Cuota</th><th scope="col">Monto</th><th scope="col">Resultado</th></tr></thead>
      <tbody>${lista.map(filaApuesta).join('') || '<tr><td colspan="6" class="vacio">No tienes apuestas en este estado.</td></tr>'}</tbody>
    </table>
    <h2>Rendimiento por mercado</h2>
    <table class="tabla tabla-compacta">
      <thead><tr><th scope="col">Mercado</th><th scope="col">Apuestas</th><th scope="col">Beneficio</th><th scope="col">ROI</th></tr></thead>
      <tbody>${r.porMercado.map(m => `<tr><th scope="row">${M.NOMBRE_MERCADO[m.mercado]}</th><td>${m.apuestas}</td><td class="${claseSigno(m.beneficio)}">${M.soles(m.beneficio)}</td><td class="${claseSigno(m.roi)}">${M.signo(m.roi)}</td></tr>`).join('')}</tbody>
    </table>
  </section>
  <aside>
    <h2>Registrar una apuesta</h2>
    <form data-accion="apuesta" novalidate>
      <label class="campo"><span>Partido</span><select name="partidoId" data-recargar>${M.partidosDelDia().map(p => `<option value="${p.id}" ${String(p.id) === (sel.partidoId ?? '1532') ? 'selected' : ''}>${M.horaLima(p.fechaHora)} ${p.local.nombre} vs ${p.visitante.nombre}</option>`).join('')}</select></label>
      <label class="campo"><span>Selección</span><select name="seleccion">${opcionesMercado(M.partido(Number(sel.partidoId) || 1532), sel.seleccion)}</select></label>
      <label class="campo"><span>Casa</span><select name="casa">${['bet365', 'betano'].map(c => `<option ${sel.casa === c ? 'selected' : ''}>${c}</option>`).join('')}</select></label>
      ${campo('Cuota', 'cuota', ctx.valor('apuesta', 'cuota'), 'text', 'inputmode="decimal" placeholder="2.10"')}
      ${campo('Monto (S/)', 'monto', ctx.valor('apuesta', 'monto'), 'text', 'inputmode="decimal" placeholder="20.00"')}
      ${err(ctx, 'apuesta')}
      <button class="boton">Registrar apuesta</button>
    </form>
  </aside>
</div>`;
  },

  // /historial: cómo le fue a la apuesta del día en los últimos 30 días, con stake de 1 unidad.
  historial: () => {
    const h = M.historialPicks();
    return `
<section class="partido-cabecera"><h1>Historial de la apuesta del día</h1><p>Últimos 30 días, apostando 1 unidad a cada pick. Mide la calidad de las recomendaciones, no la de tu banca.</p></section>
${franja([['Picks', h.picks], ['Ganados', h.ganados], ['Perdidos', h.perdidos], ['Beneficio', `${M.conSigno(h.beneficioUnidades)} u`, claseSigno(h.beneficioUnidades)], ['ROI', M.signo(h.roi), claseSigno(h.roi)]])}
<figure class="figura">${graficoBeneficio(M.curvaBeneficio(), { ancho: 960, alto: 200 })}<figcaption>Beneficio acumulado en unidades. Cada cuadro es un pick: verde si ganó, vino si perdió.</figcaption></figure>
<table class="tabla">
  <thead><tr><th scope="col">Fecha</th><th scope="col">Partido</th><th scope="col">Selección</th><th scope="col">Cuota</th><th scope="col">Resultado</th></tr></thead>
  <tbody>${h.ultimos.map(k => `<tr><td>${M.fechaCorta(k.fecha)}</td><th scope="row">${k.partido}</th><td>${M.etiqueta(k, k.partido)}</td><td>${M.cuota(k.cuota)}</td><td class="${k.resultado === 'GANADA' ? 'positivo' : 'negativo'}">${k.resultado === 'GANADA' ? `Ganada, ${M.conSigno(k.cuota - 1)} u` : 'Perdida, −1.00 u'}</td></tr>`).join('')}</tbody>
</table>`;
  },

  // /perfil: datos de la cuenta y la banca.
  perfil: ctx => {
    const u = M.perfil();
    return `
<section class="partido-cabecera"><h1>Perfil</h1><p>La banca es el monto que dedicas a apostar. Lo declaras tú: la web no mueve dinero real.</p></section>
<div class="columnas">
  <dl class="datos datos-columna datos-perfil">
    <div><dt>Usuario</dt><dd>${esc(u.nombreUsuario)}</dd></div>
    <div><dt>Correo</dt><dd>${esc(u.email)}</dd></div>
    <div><dt>Rol</dt><dd>${u.rol === 'ADMIN' ? 'Administrador' : 'Apostador'}</dd></div>
    <div><dt>Banca</dt><dd>${M.soles(u.banca)}</dd></div>
  </dl>
  <form data-accion="banca" novalidate>
    <h2>Actualizar banca</h2>
    ${campo('Nueva banca (S/)', 'banca', ctx.valor('banca', 'banca', u.banca.toFixed(2)), 'text', 'inputmode="decimal"')}
    ${err(ctx, 'banca')}
    <button class="boton">Guardar banca</button>
    <p class="nota">Con ${M.soles(u.banca)}, el stake de hoy (${M.pct(M.apuestaDelDia().pick?.stakePct ?? 0)}) es ${M.soles((M.apuestaDelDia().pick?.stakePct ?? 0) * u.banca)}.</p>
  </form>
</div>`;
  },

  // /admin/ingesta (solo ADMIN): lanzar la carga a mano y ver las ejecuciones.
  admin: () => `
<section class="partido-cabecera"><h1>Ingesta de datos</h1><p>Corre sola a las 05:00 (histórico) y a las 08:00 y 16:00 (próximos), hora de Lima. Desde aquí puedes lanzarla a mano.</p></section>
<div class="acciones-ingesta">
  <button class="boton" data-clic="ingesta:HISTORICO">Cargar histórico</button>
  <button class="boton boton-secundario" data-clic="ingesta:PROXIMOS">Cargar próximos partidos</button>
</div>
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
