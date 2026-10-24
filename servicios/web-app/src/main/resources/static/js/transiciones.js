// =============================================================
// Transiciones entre páginas (View Transitions entre documentos).
// El CSS activa la transición con @view-transition; este script
// solo decide de qué tipo es, marcando <html data-transicion="…">:
//   adelante: a una sección que está más a la derecha en el menú
//   atras:    a una sección anterior
//   misma:    la misma pantalla con otros datos (filtro, pestaña o
//             formulario enviado): solo se mueven los renglones
// En navegadores sin soporte no pasa nada: la página cambia sin animación.
// =============================================================
(() => {
  // Orden de las secciones, igual que en el menú.
  const ORDEN = ['/login', '/registro', '/', '/partidos', '/mis-apuestas', '/historial', '/perfil', '/admin'];

  // Posición de una dirección en el orden. La cuota de Betano va justo después del análisis.
  function posicion(url) {
    const ruta = new URL(url).pathname;
    if (ruta === '/') return ORDEN.indexOf('/');
    const indice = ORDEN.findIndex(r => r !== '/' && ruta.startsWith(r));
    return indice + (ruta.endsWith('/cuota-betano') ? 0.5 : 0);
  }

  // La pantalla concreta, sin parámetros: /partidos/1532 y /partidos/1532/evaluar son la misma.
  function pantalla(url) {
    return new URL(url).pathname.replace(/\/evaluar$/, '').replace(/\/ejecutar$/, '').replace(/\/banca$/, '') || '/';
  }

  // Tipo de transición para ir de una dirección a otra.
  function tipo(desde, hacia) {
    if (pantalla(desde) === pantalla(hacia)) return 'misma';
    return posicion(hacia) >= posicion(desde) ? 'adelante' : 'atras';
  }

  // Página que se va: se marca antes de tomar su «foto», para que los renglones lleven su nombre.
  window.addEventListener('pageswap', evento => {
    if (!evento.viewTransition || !evento.activation) return;
    document.documentElement.dataset.transicion = tipo(location.href, evento.activation.entry.url);
  });

  // Página que llega: se marca con el mismo tipo y se limpia al terminar la animación.
  window.addEventListener('pagereveal', evento => {
    const desde = window.navigation?.activation?.from?.url;
    if (!evento.viewTransition || !desde) return;
    document.documentElement.dataset.transicion = tipo(desde, location.href);
    evento.viewTransition.finished.finally(() => delete document.documentElement.dataset.transicion);
  });
})();
