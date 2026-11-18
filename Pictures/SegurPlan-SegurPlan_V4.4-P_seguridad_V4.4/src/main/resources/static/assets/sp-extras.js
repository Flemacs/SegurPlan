/* SEGURPLAN v5 - funciones globales: campana de notificaciones (RF032), menú por rol,
   acceso a "Mi perfil" (RF003), enlace de privacidad (RNF04) y exportación de reportes (RF031).
   Se carga automáticamente desde app.js. */
(function () {
  'use strict';
  if (window.__spExtras) { return; }   // ya cargado (carga síncrona + dinámica)
  window.__spExtras = true;
  const EN_PAGES = location.pathname.indexOf('/pages/') !== -1;
  const BASE = EN_PAGES ? '' : 'pages/';          // para enlaces entre páginas
  const sesion = (typeof sp_getSession === 'function') ? sp_getSession() : null;

  // ---------- utilidades globales ----------
  window.spEsc = function (t) {
    return String(t == null ? '' : t).replace(/[&<>"']/g, c =>
      ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  };
  window.spToast = function (msg, tipo) {
    const d = document.createElement('div');
    d.className = 'sp-toast ' + (tipo === 'err' ? 'err' : 'ok');
    d.setAttribute('role', 'status');
    d.textContent = msg;
    document.body.appendChild(d);
    setTimeout(() => d.remove(), 4200);
  };
  window.spFecha = function (f) {
    if (!f) return '-';
    const d = new Date(f);
    return isNaN(d) ? String(f) : d.toLocaleString('es-PE', { dateStyle: 'short', timeStyle: 'short' });
  };
  /** fetch con CSRF, JSON y manejo de sesión vencida. Lanza Error con el mensaje del servidor. */
  window.spApi = async function (metodo, url, cuerpo) {
    const opts = { method: metodo, credentials: 'same-origin', headers: {} };
    if (metodo !== 'GET') {
      try { const c = await sp_getCsrfToken(); opts.headers[c.headerName] = c.token; } catch (e) { /* se informa abajo */ }
    }
    if (cuerpo !== undefined) {
      opts.headers['Content-Type'] = 'application/json';
      opts.body = JSON.stringify(cuerpo);
    }
    const r = await fetch(url, opts);
    if (r.status === 401) { location.replace((EN_PAGES ? '' : 'pages/') + 's01-login.html'); throw new Error('Sesión expirada.'); }
    let data = null;
    try { data = await r.json(); } catch (e) { /* respuesta sin JSON */ }
    if (!r.ok) throw new Error((data && data.mensaje) || ('Error ' + r.status));
    return data;
  };
  /** Sube un archivo (multipart) con CSRF. */
  window.spSubir = async function (url, campos, archivo) {
    const c = await sp_getCsrfToken();
    const fd = new FormData();
    Object.keys(campos || {}).forEach(k => fd.append(k, campos[k]));
    fd.append('archivo', archivo);
    const r = await fetch(url, { method: 'POST', credentials: 'same-origin', headers: { [c.headerName]: c.token }, body: fd });
    let d = null; try { d = await r.json(); } catch (e) {}
    if (!r.ok) throw new Error((d && d.mensaje) || ('Error ' + r.status));
    return d;
  };
  window.spDescargar = async function (url, nombre) {
    const r = await fetch(url, { credentials: 'same-origin' });
    if (!r.ok) { let m = 'No se pudo descargar el archivo.'; try { m = (await r.json()).mensaje || m; } catch (e) {} spToast(m, 'err'); return; }
    const blob = await r.blob();
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob); a.download = nombre; document.body.appendChild(a); a.click();
    setTimeout(() => { URL.revokeObjectURL(a.href); a.remove(); }, 500);
  };
  window.spAbrirArchivo = async function (url) {
    const r = await fetch(url, { credentials: 'same-origin' });
    if (!r.ok) { spToast('No se pudo abrir el archivo.', 'err'); return; }
    const blob = await r.blob();
    window.open(URL.createObjectURL(blob), '_blank', 'noopener');
  };

  function iniciarExtras() {
    // hoja de estilos
    if (!document.getElementById('sp-extras-css')) {
      const l = document.createElement('link');
      l.id = 'sp-extras-css'; l.rel = 'stylesheet';
      l.href = (EN_PAGES ? '../assets/' : 'assets/') + 'sp-extras.css';
      document.head.appendChild(l);
    }
    if (!sesion) return;                       // páginas públicas: sin campana ni menú

    const rol = String(sesion.rol || '').toLowerCase();

    // ---------- Mi perfil ----------
    document.querySelectorAll('.profile-btn').forEach(b => {
      b.addEventListener('click', () => { location.href = BASE + 's27-perfil.html'; });
    });

    // ---------- menú por rol ----------
    const header = document.querySelector('.site-header');
    if (header && !document.querySelector('.sp-nav')) {
      const items = [];
      if (rol === 'cliente') {
        items.push(['s02-panel-usuario.html', 'Mi panel'], ['s19-mis-polizas.html', 'Mis pólizas'],
                   ['s12-seguimiento.html', 'Mis siniestros'], ['s27-perfil.html', 'Mi perfil']);
      } else if (rol === 'asesor') {
        items.push(['s13-panel-asesor.html', 'Panel del asesor'], ['s29-gestion-cotizaciones.html', 'Cotizaciones'],
                   ['s28-gestion-siniestros.html', 'Siniestros'], ['s30-solicitudes-afp.html', 'Solicitudes AFP'],
                   ['s18-generar-poliza.html', 'Generar póliza'], ['s27-perfil.html', 'Mi perfil']);
      } else if (rol === 'administrador') {
        items.push(['s14-administracion.html', 'Administración'], ['s29-gestion-cotizaciones.html', 'Cotizaciones'],
                   ['s28-gestion-siniestros.html', 'Siniestros'], ['s30-solicitudes-afp.html', 'Solicitudes AFP'],
                   ['s20-reportes.html', 'Reportes'], ['s27-perfil.html', 'Mi perfil']);
      }
      if (items.length) {
        const nav = document.createElement('nav');
        nav.className = 'sp-nav'; nav.setAttribute('aria-label', 'Navegación principal');
        const actual = location.pathname.split('/').pop();
        nav.innerHTML = items.map(i => '<a href="' + BASE + i[0] + '"' + (i[0] === actual ? ' class="activo"' : '') + '>' + i[1] + '</a>').join('');
        header.insertAdjacentElement('afterend', nav);
      }
    }

    // ---------- campana de notificaciones ----------
    const bell = document.querySelector('.bell-btn');
    if (bell) {
      const wrap = document.createElement('span');
      wrap.className = 'sp-bell-wrap';
      bell.parentNode.insertBefore(wrap, bell);
      wrap.appendChild(bell);
      const badge = document.createElement('span'); badge.className = 'sp-badge'; wrap.appendChild(badge);
      const panel = document.createElement('div'); panel.className = 'sp-panel';
      panel.setAttribute('role', 'dialog'); panel.setAttribute('aria-label', 'Notificaciones');
      panel.innerHTML = '<div class="sp-panel-head"><span>Notificaciones</span>' +
        '<button type="button" id="sp-leer-todas">Marcar todas como leídas</button></div>' +
        '<div class="sp-panel-list" id="sp-lista"><div class="sp-vacio">Cargando...</div></div>';
      wrap.appendChild(panel);
      bell.setAttribute('aria-haspopup', 'true');

      function destino(n) {
        const c = rol === 'cliente';
        if (n.entidad === 'SINIESTRO') {
          if (n.idRegistro) sessionStorage.setItem('sp_last_sin', n.idRegistro);
          return c ? 's12-seguimiento.html' : 's28-gestion-siniestros.html#' + n.idRegistro;
        }
        if (n.entidad === 'COTIZACION') return c ? 's02-panel-usuario.html' : 's29-gestion-cotizaciones.html#' + n.idRegistro;
        if (n.entidad === 'POLIZA') return 's19-mis-polizas.html';
        if (n.entidad === 'PREVISION') return c ? 's06-afp-perfil.html' : 's30-solicitudes-afp.html';
        return null;
      }
      async function cargar() {
        try {
          const r = await spApi('GET', '/api/notificaciones');
          badge.textContent = r.noLeidas > 99 ? '99+' : r.noLeidas;
          badge.style.display = r.noLeidas > 0 ? 'block' : 'none';
          bell.setAttribute('aria-label', 'Notificaciones: ' + r.noLeidas + ' sin leer');
          const lista = panel.querySelector('#sp-lista');
          if (!r.items.length) { lista.innerHTML = '<div class="sp-vacio">No tiene notificaciones.</div>'; return; }
          lista.innerHTML = r.items.map(n =>
            '<div class="sp-notif' + (n.leida ? '' : ' nueva') + '" data-id="' + n.id + '" data-ent="' + spEsc(n.entidad || '') + '" data-reg="' + (n.idRegistro || '') + '">' +
            '<strong>' + spEsc(n.titulo) + '</strong>' + spEsc(n.mensaje || '') + '<small>' + spFecha(n.fecha) + '</small></div>').join('');
          lista.querySelectorAll('.sp-notif').forEach(el => el.addEventListener('click', async () => {
            const n = { entidad: el.dataset.ent, idRegistro: el.dataset.reg };
            try { await spApi('PUT', '/api/notificaciones/' + el.dataset.id + '/leida'); } catch (e) {}
            const d = destino(n);
            if (d) location.href = BASE + d; else cargar();
          }));
        } catch (e) { /* sin sesión o sin red: la campana queda vacía */ }
      }
      bell.addEventListener('click', ev => { ev.stopPropagation(); panel.classList.toggle('open'); if (panel.classList.contains('open')) cargar(); });
      document.addEventListener('click', ev => { if (!wrap.contains(ev.target)) panel.classList.remove('open'); });
      document.addEventListener('keydown', ev => { if (ev.key === 'Escape') panel.classList.remove('open'); });
      panel.querySelector('#sp-leer-todas').addEventListener('click', async () => {
        try { await spApi('PUT', '/api/notificaciones/leidas'); } catch (e) {}
        cargar();
      });
      cargar();
      setInterval(cargar, 60000);
    }

    // ---------- enlace de privacidad en el pie ----------
    const fl = document.querySelector('.footer-links');
    if (fl && !fl.querySelector('.sp-priv')) {
      const a = document.createElement('a'); a.className = 'sp-priv'; a.href = BASE + 'privacidad.html'; a.textContent = 'Privacidad';
      fl.appendChild(a);
    }

    // ---------- módulos específicos de algunas páginas ----------
    const pagina = location.pathname.split('/').pop();
    if (pagina === 's04-cotizacion-recibida.html') botonPdfCotizacion();
    if (pagina === 's12-seguimiento.html') lineaDeTiempo();
    if (pagina === 's23-roles-permisos.html' && rol === 'administrador') matrizPermisos();

    // ---------- exportación de reportes a PDF / Excel (RF031) ----------
    const archivo = location.pathname.split('/').pop();
    if (/^(rep-.*|s15-.*|s16-.*|s17-.*)\.html$/.test(archivo)) activarExportacion();
  }
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', iniciarExtras);
  } else {
    iniciarExtras();
  }

  // RF006: PDF de la cotización recibida
  function botonPdfCotizacion() {
    const t = document.querySelector('.page-title');
    if (!t) return;
    const b = document.createElement('button');
    b.type = 'button'; b.className = 'btn btn-outline btn-sm'; b.textContent = 'DESCARGAR COTIZACIÓN EN PDF';
    b.style.margin = '0 0 14px';
    b.addEventListener('click', () => {
      const id = sessionStorage.getItem('sp_last_quote');
      if (!id) { spToast('No hay una cotización seleccionada.', 'err'); return; }
      spDescargar('/api/cotizaciones/' + id + '/pdf', 'cotizacion-' + id + '.pdf');
    });
    t.insertAdjacentElement('afterend', b);
  }

  // RF027 / RF033: línea de tiempo del siniestro para cliente y asesor
  function lineaDeTiempo() {
    const wrap = document.querySelector('.page-wrap');
    if (!wrap) return;
    const card = document.createElement('div');
    card.className = 'card'; card.style.marginTop = '20px'; card.style.display = 'none';
    card.innerHTML = '<div class="card-header"><div><h2>Línea de tiempo del caso</h2><p id="tl-estado"></p></div></div>' +
      '<div class="card-body"><div id="tl-proximo" class="notice info" style="margin-bottom:14px"></div>' +
      '<ul class="sp-tl" id="tl-lista"></ul>' +
      '<div class="btn-row" style="margin-top:10px"><button type="button" class="btn btn-outline btn-sm" id="tl-pdf">DESCARGAR RESUMEN EN PDF</button></div></div>';
    wrap.appendChild(card);
    let ultimo = null, datos = null;
    document.getElementById('tl-pdf').addEventListener('click', () => {
      if (datos) spDescargar('/api/siniestros/' + datos.idSiniestro + '/resumen.pdf', 'siniestro-' + datos.numeroSiniestro + '.pdf');
    });
    async function pintar() {
      const id = sessionStorage.getItem('sp_last_sin');
      if (!id || id === ultimo) return;
      try {
        const t = await spApi('GET', '/api/siniestros/' + id + '/timeline');
        ultimo = id; datos = t;
        document.getElementById('tl-estado').textContent = 'Siniestro ' + t.numeroSiniestro + ' — estado actual: ' + t.estado;
        document.getElementById('tl-proximo').textContent = t.descripcionEstado +
          (t.montoAprobado ? ' Monto aprobado: S/ ' + Number(t.montoAprobado).toFixed(2) + '.' : '');
        document.getElementById('tl-lista').innerHTML = t.eventos.length ? t.eventos.map(e =>
          '<li><b>' + spEsc(e.accion) + '</b><small>' + spFecha(e.fecha) + (e.usuario ? ' · ' + spEsc(e.usuario) : '') + '</small>' + spEsc(e.detalle || '') + '</li>').join('')
          : '<li>Sin movimientos todavía.</li>';
        card.style.display = 'block';
      } catch (e) { /* aún sin siniestro válido seleccionado */ }
    }
    pintar(); setInterval(pintar, 2000);
  }

  // RF030: matriz de permisos por rol y módulo (editable)
  function matrizPermisos() {
    const wrap = document.querySelector('.page-wrap');
    if (!wrap) return;
    const card = document.createElement('div');
    card.className = 'card'; card.style.marginTop = '24px';
    card.innerHTML = '<div class="card-header"><div><h2>Matriz de permisos por rol</h2><p>Defina qué puede ver, crear, editar o eliminar cada rol en cada módulo.</p></div></div>' +
      '<div class="card-body"><div class="form-group"><label for="mp-rol">Rol</label><select class="form-control" id="mp-rol"></select></div>' +
      '<div class="table-wrap"><table class="sp-table"><thead><tr><th>Módulo</th><th>Ver</th><th>Crear</th><th>Editar</th><th>Eliminar</th></tr></thead><tbody id="mp-body"></tbody></table></div>' +
      '<div class="btn-row" style="margin-top:12px"><button class="btn btn-primary" id="mp-guardar" type="button">GUARDAR PERMISOS</button></div></div>';
    wrap.appendChild(card);
    const sel = card.querySelector('#mp-rol'), body = card.querySelector('#mp-body');
    async function cargarRol() {
      try {
        const r = await spApi('GET', '/api/admin/roles/' + sel.value + '/permisos');
        body.innerHTML = r.permisos.map(p => '<tr data-m="' + spEsc(p.modulo) + '"><td>' + spEsc(p.modulo) + '</td>' +
          ['ver', 'crear', 'editar', 'eliminar'].map(k => '<td><input type="checkbox" data-k="' + k + '" aria-label="' + k + ' ' + spEsc(p.modulo) + '"' + (p[k] ? ' checked' : '') + '></td>').join('') + '</tr>').join('');
        // marcar escritura exige lectura
        body.querySelectorAll('input[data-k]:not([data-k="ver"])').forEach(c => c.addEventListener('change', () => {
          if (c.checked) c.closest('tr').querySelector('[data-k="ver"]').checked = true; }));
        body.querySelectorAll('input[data-k="ver"]').forEach(c => c.addEventListener('change', () => {
          if (!c.checked) c.closest('tr').querySelectorAll('input[data-k]').forEach(x => x.checked = false); }));
      } catch (e) { spToast(e.message, 'err'); }
    }
    spApi('GET', '/api/admin/roles').then(roles => {
      sel.innerHTML = roles.map(r => '<option value="' + r.idRol + '">' + spEsc(r.nombre) + '</option>').join('');
      sel.addEventListener('change', cargarRol); cargarRol();
    }).catch(e => spToast(e.message, 'err'));
    card.querySelector('#mp-guardar').addEventListener('click', async () => {
      const permisos = Array.from(body.querySelectorAll('tr')).map(tr => {
        const o = { modulo: tr.dataset.m };
        tr.querySelectorAll('input[data-k]').forEach(c => { o[c.dataset.k] = c.checked; });
        return o;
      });
      try { const r = await spApi('PUT', '/api/admin/roles/' + sel.value + '/permisos', permisos); spToast(r.mensaje); }
      catch (e) { spToast(e.message, 'err'); }
    });
  }

  function activarExportacion() {
    const titulo = document.querySelector('.page-title');
    if (!titulo || document.querySelector('.sp-export')) return;
    const barra = document.createElement('div');
    barra.className = 'sp-export';
    barra.innerHTML = '<button type="button" class="btn btn-outline btn-sm" id="sp-exp-pdf">EXPORTAR PDF</button>' +
                      '<button type="button" class="btn btn-outline btn-sm" id="sp-exp-xls">EXPORTAR EXCEL</button>';
    titulo.insertAdjacentElement('afterend', barra);

    function tablas() { return Array.from(document.querySelectorAll('.page-wrap table')).filter(t => t.rows.length > 0); }
    function nombre() { return (document.title || 'reporte').replace(/[^\w\- ]+/g, '').trim().replace(/\s+/g, '_').toLowerCase(); }

    document.getElementById('sp-exp-xls').addEventListener('click', () => {
      const ts = tablas();
      if (!ts.length) { spToast('No hay tablas para exportar. Genere el reporte primero.', 'err'); return; }
      const html = '<html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel">' +
        '<head><meta charset="UTF-8"></head><body><h2>' + spEsc(titulo.textContent.trim()) + '</h2>' +
        ts.map(t => t.outerHTML).join('<br>') + '</body></html>';
      const blob = new Blob(['\ufeff', html], { type: 'application/vnd.ms-excel;charset=utf-8' });
      const a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = nombre() + '.xls';
      document.body.appendChild(a); a.click(); setTimeout(() => { URL.revokeObjectURL(a.href); a.remove(); }, 500);
      spToast('Archivo de Excel generado.');
    });

    document.getElementById('sp-exp-pdf').addEventListener('click', () => {
      const ts = tablas();
      if (!ts.length) { spToast('No hay tablas para exportar. Genere el reporte primero.', 'err'); return; }
      const w = window.open('', '_blank');
      if (!w) { spToast('Permita las ventanas emergentes para exportar el PDF.', 'err'); return; }
      w.document.write('<html><head><meta charset="UTF-8"><title>' + spEsc(document.title) + '</title><style>' +
        'body{font-family:Arial,sans-serif;font-size:12px;margin:24px}h1{font-size:18px;color:#1c3f6e}' +
        'table{border-collapse:collapse;width:100%;margin-bottom:18px}th,td{border:1px solid #999;padding:5px 7px;text-align:left}' +
        'th{background:#e8eef6}small{color:#555}</style></head><body><h1>' + spEsc(titulo.textContent.trim()) +
        '</h1><small>SEGURPLAN - generado el ' + new Date().toLocaleString('es-PE') + '</small><br><br>' +
        ts.map(t => t.outerHTML).join('') + '</body></html>');
      w.document.close(); w.focus();
      setTimeout(() => w.print(), 400);          // en el diálogo elija "Guardar como PDF"
    });
  }
})();
