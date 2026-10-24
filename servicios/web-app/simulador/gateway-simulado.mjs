// =============================================================
// Gateway simulado para probar la web-app sin levantar los
// microservicios. Responde las mismas rutas /api/... que el
// api-gateway real, con el JSON de los contratos.
//
// Uso:  node servicios/web-app/simulador/gateway-simulado.mjs
//       (escucha en el puerto 8080; otro puerto: PORT=8088 node ...)
//
// Casos para probar:
//   - Cualquier usuario entra con una clave de 6 caracteres o más.
//     «admin» entra con rol ADMIN; una clave más corta da 401.
//   - «renzo» y «admin» ya existen: registrarlos da 409.
//   - Liverpool vs Brighton (partido 1533) simula que ms-cuotas
//     no responde (503), para ver el aviso de RN-WEB-04.
// =============================================================
import http from 'node:http';
import * as D from './datos.js';

const PUERTO = Number(process.env.PORT ?? 8080);

// Tokens entregados en el login: token → { nombreUsuario, rol }.
const tokens = new Map();

// Respuesta JSON con su código HTTP.
function responder(res, codigo, cuerpo) {
  res.writeHead(codigo, { 'Content-Type': 'application/json; charset=utf-8' });
  res.end(cuerpo === undefined ? '' : JSON.stringify(cuerpo));
}

// Error con el formato común {"error", "detalle"}.
const error = (res, codigo, clave, detalle) => responder(res, codigo, { error: clave, detalle });

// Lee el cuerpo JSON de la petición.
async function leerCuerpo(req) {
  let texto = '';
  for await (const parte of req) texto += parte;
  try { return texto ? JSON.parse(texto) : {}; } catch { return {}; }
}

// Usuario del token "Authorization: Bearer …", o null.
function usuarioDe(req) {
  const [, token] = (req.headers.authorization ?? '').split(' ');
  return tokens.get(token) ?? null;
}

// Una apuesta del modelo de datos con los nombres de la tabla apuesta_usuario.
const apuestaDTO = a => ({ id: a.id, partidoId: a.partidoId, partido: a.partido, mercado: a.mercado, seleccion: a.seleccion,
  linea: a.linea, casa: a.casa, cuota: a.cuota, monto: a.monto, fecha: a.creadaEn, resultado: a.resultado, ganancia: a.ganancia });

// Cada ruta: [método, expresión regular, ¿pública?, función].
const rutas = [
  // ---------- ms-usuarios ----------
  ['POST', /^\/api\/usuarios\/auth\/login$/, true, async (req, res) => {
    const { nombreUsuario, clave } = await leerCuerpo(req);
    if (!nombreUsuario || !clave || clave.length < 6) return error(res, 401, 'CREDENCIALES_INVALIDAS', 'Usuario o contraseña incorrectos');
    const sesion = D.iniciarSesion(nombreUsuario);
    const token = `simulado.${nombreUsuario}.${Date.now()}`;
    tokens.set(token, sesion);
    responder(res, 200, { token, tipo: 'Bearer', expiraEn: 28800, rol: sesion.rol, nombreUsuario });
  }],
  ['POST', /^\/api\/usuarios\/auth\/registro$/, true, async (req, res) => {
    const d = await leerCuerpo(req);
    if (d.clave !== d.claveConfirmacion) return error(res, 400, 'VALIDACION', 'Las contraseñas no coinciden');
    if (['renzo', 'admin'].includes(String(d.nombreUsuario).toLowerCase())) return error(res, 409, 'USUARIO_DUPLICADO', 'El nombre de usuario ya está en uso');
    responder(res, 201, { id: 3, nombreUsuario: d.nombreUsuario, rol: 'APOSTADOR' });
  }],
  ['GET', /^\/api\/usuarios\/me$/, false, (req, res) => responder(res, 200, D.perfil())],
  ['PUT', /^\/api\/usuarios\/me\/banca$/, false, async (req, res) => {
    const { banca } = await leerCuerpo(req);
    if (!(banca >= 0)) return error(res, 400, 'VALIDACION', 'La banca debe ser mayor o igual a 0');
    responder(res, 200, D.actualizarBanca(banca));
  }],

  // ---------- ms-catalogo (los GET son públicos) ----------
  ['GET', /^\/api\/catalogo\/ligas$/, true, (req, res) => responder(res, 200, D.LIGAS)],
  ['GET', /^\/api\/catalogo\/partidos$/, true, (req, res) => responder(res, 200, D.PARTIDOS)],
  ['GET', /^\/api\/catalogo\/partidos\/(\d+)$/, true, (req, res, [id]) => {
    const p = D.partido(Number(id));
    return p ? responder(res, 200, p) : error(res, 404, 'PARTIDO_NO_ENCONTRADO', `No existe el partido ${id}`);
  }],

  // ---------- ms-predicciones ----------
  ['GET', /^\/api\/predicciones\/partidos\/(\d+)$/, false, (req, res, [id]) => {
    const pr = D.prediccion(Number(id));
    return pr ? responder(res, 200, pr) : error(res, 404, 'PARTIDO_NO_ENCONTRADO', `No existe el partido ${id}`);
  }],

  // ---------- ms-cuotas ----------
  ['GET', /^\/api\/cuotas\/partidos\/(\d+)\/cuotas$/, false, (req, res, [id]) => {
    const lista = D.cuotas(Number(id));
    if (!lista) return error(res, 503, 'SERVICIO_NO_DISPONIBLE', 'ms-cuotas no responde');
    responder(res, 200, lista.map(({ partidoId, ...c }) => c));
  }],
  ['POST', /^\/api\/cuotas\/cuotas\/manual$/, false, async (req, res) => {
    const d = await leerCuerpo(req);
    if (!D.partido(d.partidoId)) return error(res, 404, 'PARTIDO_NO_ENCONTRADO', `No existe el partido ${d.partidoId}`);
    if (!(d.valor > 1.01)) return error(res, 400, 'CUOTA_INVALIDA', 'La cuota debe ser mayor que 1.01');
    if (d.partidoId === D.PARTIDO_SIN_CUOTAS) return error(res, 503, 'SERVICIO_NO_DISPONIBLE', 'ms-cuotas no responde');
    D.guardarCuotaManual(d);
    responder(res, 201, d);
  }],

  // ---------- ms-apuestas ----------
  ['GET', /^\/api\/apuestas\/apuesta-del-dia$/, false, (req, res) => responder(res, 200, D.apuestaDelDia())],
  ['POST', /^\/api\/apuestas\/evaluaciones$/, false, async (req, res) => {
    const d = await leerCuerpo(req);
    if (!D.partido(d.partidoId)) return error(res, 404, 'PARTIDO_NO_ENCONTRADO', `No existe el partido ${d.partidoId}`);
    if (!(d.cuota > 1)) return error(res, 400, 'CUOTA_INVALIDA', 'La cuota debe ser mayor que 1');
    responder(res, 200, D.evaluar(d));
  }],
  ['GET', /^\/api\/apuestas\/mis-apuestas$/, false, (req, res, _, url) => {
    const estado = url.searchParams.get('estado');
    responder(res, 200, D.misApuestas(estado || undefined).map(apuestaDTO));
  }],
  ['POST', /^\/api\/apuestas\/mis-apuestas$/, false, async (req, res) => {
    const d = await leerCuerpo(req);
    if (!D.partido(d.partidoId)) return error(res, 404, 'PARTIDO_NO_ENCONTRADO', `No existe el partido ${d.partidoId}`);
    if (!(d.monto > 0) || !(d.cuota > 1)) return error(res, 400, 'VALIDACION', 'El monto debe ser mayor que 0 y la cuota, mayor que 1');
    responder(res, 201, apuestaDTO(D.registrarApuesta(d)));
  }],
  ['GET', /^\/api\/apuestas\/mis-apuestas\/rendimiento$/, false, (req, res) => responder(res, 200, D.rendimiento())],
  ['GET', /^\/api\/apuestas\/picks\/historial$/, false, (req, res) => responder(res, 200, D.historialPicks())],

  // ---------- ms-ingesta (solo ADMIN, como exige el gateway) ----------
  ['POST', /^\/api\/ingesta\/ejecutar$/, false, (req, res, _, url, usuario) => {
    if (usuario.rol !== 'ADMIN') return error(res, 403, 'SIN_PERMISO', 'Solo el administrador puede lanzar la ingesta');
    const e = D.lanzarIngesta(url.searchParams.get('tipo') === 'PROXIMOS' ? 'PROXIMOS' : 'HISTORICO');
    responder(res, 202, { id: e.id, estado: e.estado });
  }],
  ['GET', /^\/api\/ingesta\/ejecuciones$/, false, (req, res, _, url, usuario) => {
    if (usuario.rol !== 'ADMIN') return error(res, 403, 'SIN_PERMISO', 'Solo el administrador puede ver la ingesta');
    responder(res, 200, D.ejecuciones());
  }],
];

// Busca la ruta, revisa el token (RN-GAT-01) y llama a su función.
const servidor = http.createServer(async (req, res) => {
  const url = new URL(req.url, `http://${req.headers.host}`);
  console.log(new Date().toISOString().slice(11, 19), req.method, url.pathname + url.search);
  for (const [metodo, patron, publica, accion] of rutas) {
    const m = req.method === metodo && url.pathname.match(patron);
    if (!m) continue;
    const usuario = usuarioDe(req);
    if (!publica && !usuario) return error(res, 401, 'TOKEN_INVALIDO', 'Falta el token o venció');
    try {
      return await accion(req, res, m.slice(1), url, usuario);
    } catch (e) {
      console.error(e);
      return error(res, 500, 'ERROR_INTERNO', 'Error del simulador');
    }
  }
  error(res, 404, 'RUTA_NO_ENCONTRADA', `No existe ${req.method} ${url.pathname}`);
});

servidor.listen(PUERTO, () => console.log(`Gateway simulado en http://localhost:${PUERTO}`));
