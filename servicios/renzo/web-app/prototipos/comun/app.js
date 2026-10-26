// =============================================================
// Motor común de los prototipos: rutas, sesión, transiciones
// entre pantallas y acciones de los formularios. Cada propuesta
// solo aporta sus vistas (HTML) y su hoja de estilos.
//
// En la web real cada ruta es una plantilla de Thymeleaf y las
// transiciones se activan con `@view-transition { navigation: auto; }`
// en el CSS. Aquí se simula con rutas por hash para poder ver
// las pantallas sin levantar el servidor.
// =============================================================
import * as M from './modelo.js';

// Orden de las pantallas: decide si la transición va hacia adelante o hacia atrás.
const ORDEN = ['login', 'registro', 'hoy', 'partido', 'betano', 'misApuestas', 'historial', 'perfil', 'admin'];
const PUBLICAS = ['login', 'registro'];

// Escapa texto que escribe el usuario antes de meterlo en el HTML.
export const esc = v => String(v ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

// Convierte el hash (#/partidos/1532) en { nombre, id }.
function resolver(hash) {
  const ruta = hash.replace(/^#/, '').replace(/\/+$/, '') || '/';
  let m;
  if (ruta === '/login') return { nombre: 'login' };
  if (ruta === '/registro') return { nombre: 'registro' };
  if ((m = ruta.match(/^\/partidos\/(\d+)\/cuota-betano$/))) return { nombre: 'betano', id: Number(m[1]) };
  if ((m = ruta.match(/^\/partidos\/(\d+)$/))) return { nombre: 'partido', id: Number(m[1]) };
  if (ruta === '/mis-apuestas') return { nombre: 'misApuestas' };
  if (ruta === '/historial') return { nombre: 'historial' };
  if (ruta === '/perfil') return { nombre: 'perfil' };
  if (ruta === '/admin/ingesta') return { nombre: 'admin' };
  return { nombre: 'hoy' };
}

// Entradas del menú. La de ingesta solo aparece para el ADMIN (RN-WEB-06).
export function navegacion(ctx) {
  const activa = { partido: 'hoy', betano: 'hoy' }[ctx.ruta.nombre] ?? ctx.ruta.nombre;
  return [
    { clave: 'hoy', href: '#/', texto: 'Hoy' },
    { clave: 'misApuestas', href: '#/mis-apuestas', texto: 'Mis apuestas' },
    { clave: 'historial', href: '#/historial', texto: 'Historial' },
    { clave: 'perfil', href: '#/perfil', texto: 'Perfil' },
    { clave: 'admin', href: '#/admin/ingesta', texto: 'Ingesta', soloAdmin: true },
  ].filter(i => !i.soloAdmin || ctx.sesion?.rol === 'ADMIN').map(i => ({ ...i, activo: i.clave === activa }));
}

// Opciones agrupadas por mercado para los <select> de selección (todas las líneas de RN-09).
export function opcionesMercado(p, seleccionada = 'GOLES|MAS|2.5') {
  const pr = M.prediccion(p.id);
  return Object.entries(M.NOMBRE_MERCADO).map(([mercado, nombre]) => `
    <optgroup label="${nombre}">${pr.mercados.filter(m => m.mercado === mercado).map(m => {
      const k = M.clave(m);
      return `<option value="${k}" ${k === seleccionada ? 'selected' : ''}>${M.etiqueta(m, p)}</option>`;
    }).join('')}</optgroup>`).join('');
}

// Gráfico SVG del beneficio acumulado de los picks. Cada propuesta lo pinta con sus clases.
export function graficoBeneficio(puntos, { ancho = 640, alto = 180, margen = 12 } = {}) {
  const valores = [0, ...puntos.map(p => p.valor)];
  const max = Math.max(...valores), min = Math.min(...valores);
  const x = i => margen + (i * (ancho - 2 * margen)) / puntos.length;
  const y = v => margen + ((max - v) * (alto - 2 * margen)) / (max - min || 1);
  const linea = [`M${x(0)},${y(0)}`, ...puntos.map((p, i) => `L${x(i + 1)},${y(p.valor)}`)].join(' ');
  return `
<svg class="grafico" viewBox="0 0 ${ancho} ${alto}" role="img" aria-label="Beneficio acumulado de la apuesta del día, de ${puntos[0].fecha} a ${puntos.at(-1).fecha}">
  <line class="g-cero" x1="${margen}" x2="${ancho - margen}" y1="${y(0)}" y2="${y(0)}"></line>
  <path class="g-area" d="${linea} L${x(puntos.length)},${y(0)} L${x(0)},${y(0)} Z"></path>
  <path class="g-linea" d="${linea}"></path>
  ${puntos.map((p, i) => `<rect class="g-punto ${p.resultado === 'GANADA' ? 'g-gana' : 'g-pierde'}" x="${x(i + 1) - 3}" y="${y(p.valor) - 3}" width="6" height="6"><title>${M.fechaCorta(p.fecha)}: ${M.conSigno(p.valor)} u</title></rect>`).join('')}
</svg>`;
}

// Arranca un prototipo: pinta la ruta actual y escucha la navegación y los formularios.
export function iniciar({ vistas, marco }) {
  const app = document.getElementById('app');
  const estado = { liga: 'TODAS', tab: 'PENDIENTE', aviso: null, errores: {}, form: {}, evaluacion: null };
  let actual = null;

  // Construye el contexto que reciben las vistas y pinta la pantalla dentro del marco.
  function pintar(ruta) {
    const ctx = {
      ruta, estado, sesion: M.sesion(), aviso: estado.aviso,
      error: form => estado.errores[form] ?? '',
      valor: (form, campo, porDefecto = '') => esc(estado.form[form]?.[campo] ?? porDefecto),
    };
    const cuerpo = vistas[ruta.nombre](ctx);
    app.innerHTML = PUBLICAS.includes(ruta.nombre) ? marco.publico(cuerpo, ctx) : marco.privado(cuerpo, ctx);
    // Los avisos y errores se muestran una sola vez.
    estado.aviso = null;
    estado.errores = {};
  }

  // Envuelve un cambio de pantalla en una View Transition. La dirección la usa el CSS de cada propuesta.
  function conTransicion(cambio, direccion) {
    document.documentElement.dataset.direccion = direccion;
    const reducir = matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (!document.startViewTransition || reducir) { cambio(); return; }
    document.startViewTransition(cambio);
  }

  // Vuelve a pintar la misma pantalla (filtros, pestañas y resultados de formularios).
  function refrescar() {
    conTransicion(() => pintar(actual), 'misma');
  }

  // Reacciona a cada cambio de hash: aplica las reglas de acceso y anima el cambio de pantalla.
  function navegar() {
    const ruta = resolver(location.hash);
    const s = M.sesion();
    if (!s && !PUBLICAS.includes(ruta.nombre)) { location.replace('#/login'); return; }
    if (s && PUBLICAS.includes(ruta.nombre)) { location.replace('#/'); return; }
    if (ruta.nombre === 'admin' && s.rol !== 'ADMIN') {
      estado.aviso = 'La ingesta solo está disponible para el administrador.';
      location.replace('#/');
      return;
    }
    const antes = actual;
    const misma = antes && antes.nombre === ruta.nombre && antes.id === ruta.id;
    const direccion = !antes || misma ? 'misma' : ORDEN.indexOf(ruta.nombre) >= ORDEN.indexOf(antes.nombre) ? 'adelante' : 'atras';
    actual = ruta;
    if (!antes) { pintar(ruta); return; }
    conTransicion(() => { pintar(ruta); window.scrollTo(0, 0); }, direccion);
  }

  // Guarda un error de validación para un formulario y vuelve a pintar.
  const fallar = (form, datos, mensaje) => { estado.errores[form] = mensaje; estado.form[form] = datos; refrescar(); };
  const numero = v => Number(String(v ?? '').replace(',', '.'));

  // Acciones de los formularios: cada una corresponde a un POST o PUT del contrato.
  const acciones = {
    // POST /api/usuarios/auth/login
    login(d) {
      if (!d.nombreUsuario?.trim() || !d.clave) return fallar('login', d, 'Escribe tu usuario y tu contraseña.');
      if (d.clave.length < 6) return fallar('login', d, 'Usuario o contraseña incorrectos.');
      M.iniciarSesion(d.nombreUsuario.trim());
      location.hash = '#/';
    },
    // POST /api/usuarios/auth/registro
    registro(d) {
      if (!d.nombreUsuario?.trim() || d.nombreUsuario.length > 30) return fallar('registro', d, 'El usuario debe tener entre 1 y 30 caracteres.');
      if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(d.email ?? '')) return fallar('registro', d, 'Escribe un correo válido.');
      if ((d.clave ?? '').length < 6 || d.clave.length > 72) return fallar('registro', d, 'La contraseña debe tener entre 6 y 72 caracteres.');
      if (d.clave !== d.claveConfirmacion) return fallar('registro', d, 'Las contraseñas no coinciden.');
      if (['renzo', 'admin'].includes(d.nombreUsuario.trim().toLowerCase())) return fallar('registro', d, 'El nombre de usuario ya está en uso.');
      estado.form.login = { nombreUsuario: d.nombreUsuario.trim() };
      estado.aviso = 'Cuenta creada. Ya puedes iniciar sesión.';
      location.hash = '#/login';
    },
    // POST /api/apuestas/evaluaciones
    evaluar(d) {
      const c = numero(d.cuota);
      if (!(c > 1)) return fallar('evaluar', d, 'La cuota debe ser mayor que 1.');
      const sel = M.desdeClave(d.seleccion);
      estado.form.evaluar = d;
      estado.evaluacion = { partidoId: Number(d.partidoId), casa: d.casa, cuota: c, ...sel, ...M.evaluar({ partidoId: Number(d.partidoId), ...sel, cuota: c }) };
      refrescar();
    },
    // POST /api/cuotas/cuotas/manual
    betano(d) {
      const c = numero(d.cuota);
      if (!(c > 1.01)) return fallar('betano', d, 'La cuota de Betano debe ser mayor que 1.01.');
      M.guardarCuotaManual({ partidoId: Number(d.partidoId), ...M.desdeClave(d.seleccion), valor: c });
      estado.aviso = 'Cuota de Betano guardada. Ya aparece en la tabla de cuotas.';
      location.hash = `#/partidos/${d.partidoId}`;
    },
    // POST /api/apuestas/mis-apuestas
    apuesta(d) {
      const c = numero(d.cuota), monto = numero(d.monto);
      if (!(c > 1)) return fallar('apuesta', d, 'La cuota debe ser mayor que 1.');
      if (!(monto > 0)) return fallar('apuesta', d, 'El monto debe ser mayor que 0.');
      M.registrarApuesta({ partidoId: Number(d.partidoId), ...M.desdeClave(d.seleccion), casa: d.casa, cuota: c, monto });
      estado.tab = 'PENDIENTE';
      estado.aviso = 'Apuesta registrada. Se liquidará sola cuando termine el partido.';
      refrescar();
    },
    // PUT /api/usuarios/me/banca
    banca(d) {
      const b = numero(d.banca);
      if (!(b >= 0)) return fallar('banca', d, 'La banca debe ser un monto mayor o igual a 0.');
      M.actualizarBanca(b);
      estado.aviso = 'Banca actualizada.';
      refrescar();
    },
  };

  // Acciones de los botones: filtros, pestañas, ingesta y salir.
  const clics = {
    liga: v => { estado.liga = v; refrescar(); },
    tab: v => { estado.tab = v; refrescar(); },
    ingesta: tipo => {
      M.lanzarIngesta(tipo, () => { if (actual?.nombre === 'admin') refrescar(); });
      estado.aviso = `Ingesta ${tipo === 'HISTORICO' ? 'histórica' : 'de próximos partidos'} en curso. La tabla se actualiza sola al terminar.`;
      refrescar();
    },
    salir: () => { M.cerrarSesion(); location.hash = '#/login'; },
  };

  app.addEventListener('submit', e => {
    const form = e.target.closest('form[data-accion]');
    if (!form) return;
    e.preventDefault();
    acciones[form.dataset.accion]?.(Object.fromEntries(new FormData(form)));
  });

  app.addEventListener('click', e => {
    const boton = e.target.closest('[data-clic]');
    if (!boton) return;
    const [accion, valor] = boton.dataset.clic.split(':');
    clics[accion]?.(valor);
  });

  // Un <select data-recargar> (por ejemplo, el partido) vuelve a pintar el formulario con sus opciones.
  app.addEventListener('change', e => {
    if (!e.target.matches('[data-recargar]')) return;
    const form = e.target.closest('form[data-accion]');
    estado.form[form.dataset.accion] = Object.fromEntries(new FormData(form));
    refrescar();
  });

  // Atajo del prototipo: ?usuario=admin entra directo con esa sesión (útil para revisar las pantallas).
  const usuario = new URLSearchParams(location.search).get('usuario');
  if (usuario && M.sesion()?.nombreUsuario !== usuario) M.iniciarSesion(usuario);

  window.addEventListener('hashchange', navegar);
  navegar();
}
