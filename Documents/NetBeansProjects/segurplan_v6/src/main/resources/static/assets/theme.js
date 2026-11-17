/* ═══════════════════════════════════════════════════════
   SEGURPLAN — theme.js
   SOLO PRESENTACIÓN: agrega atributos/clases/iconos para theme.css.
   No hace fetch, no toca formularios, IDs, eventos ni datos.
   Si falla, la página funciona igual (todo en try/catch).
═══════════════════════════════════════════════════════ */
(function () {
    'use strict';

    var AREAS = {
        mv: ['blue', 'Marketing y Ventas'],
        la: ['orange', 'Logística y Almacén'],
        po: ['green', 'Producción y Operaciones'],
        dg: ['purple', 'Dirección / Gerencia General'],
        ot: ['teal', 'Otros procesos']
    };
    var REPORT_ICON = {
        'mv-01': 'bar-chart', 'mv-02': 'trending', 'mv-05': 'shield',
        'la-01': 'folder',
        'po-01': 'trending', 'po-02': 'alert', 'po-03': 'building',
        'dg-01': 'dashboard', 'dg-02': 'pie',
        'ot-02': 'clock'
    };
    /* módulo por archivo → [color, icono] */
    var PAGES = {
        's03-cotizacion': ['blue', 'calculator'], 's04-cotizacion-recibida': ['blue', 'file-text'],
        's05-poliza': ['orange', 'shield'], 's18-generar-poliza': ['orange', 'file-plus'], 's19-mis-polizas': ['orange', 'shield'],
        's06-afp-perfil': ['green', 'trending'], 's07-simulacion-afp': ['green', 'trending'],
        's08-resultado-afp': ['green', 'trending'], 's17-reporte-afp': ['green', 'trending'],
        's09-siniestro': ['red', 'alert'], 's10-evidencias': ['red', 'folder'],
        's11-revision': ['red', 'clipboard'], 's12-seguimiento': ['red', 'activity'],
        's15-reporte-general': ['purple', 'dashboard'], 's16-reporte-siniestros': ['green', 'alert'],
        's20-reportes': ['blue', 'bar-chart'],
        's21-usuarios': ['green', 'users'], 's22-asesores': ['orange', 'briefcase'],
        's23-roles-permisos': ['purple', 'shield-lock'], 's24-historial': ['green', 'clock']
    };

    function norm(t) {
        return String(t || '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/\s+/g, ' ').trim();
    }
    function ico(name) {
        var i = document.createElement('i');
        i.className = 'sp-ico';
        i.setAttribute('data-icon', name);
        i.setAttribute('aria-hidden', 'true');
        return i;
    }

    /* ── 1. Módulo/área de la página (se ejecuta ya en <head>) ── */
    var file = (location.pathname.split('/').pop() || '').replace('.html', '');
    var pageIcon = null, pageMod = null, areaKey = null;
    try {
        var m = /^rep-([a-z]{2})-(\d\d)/.exec(file);
        if (m && AREAS[m[1]]) {
            areaKey = m[1];
            pageMod = AREAS[m[1]][0];
            pageIcon = REPORT_ICON[m[1] + '-' + m[2]] || 'bar-chart';
        } else if (PAGES[file]) {
            pageMod = PAGES[file][0];
            pageIcon = PAGES[file][1];
        }
        if (pageMod) document.documentElement.setAttribute('data-mod', pageMod);
    } catch (e) { /* solo visual */ }

    /* ── 2. Reglas palabra clave → [color, icono] para tarjetas de módulo ── */
    var CARD_RULES = [
        [/historial|seguimiento/, 'green', 'clock'],
        [/\broles?\b|permisos/, 'purple', 'shield-lock'],
        [/asesor/, 'orange', 'briefcase'],
        [/usuario/, 'green', 'users'],
        [/generar.*poliza|poliza.*generar/, 'orange', 'file-plus'],
        [/poliza|contrat/, 'orange', 'shield'],
        [/cotizacion/, 'blue', 'calculator'],
        [/siniestro/, 'red', 'alert'],
        [/\bafp\b|pension|previsional/, 'green', 'trending'],
        [/document|evidencia/, 'orange', 'folder'],
        [/gerencia|indicador/, 'purple', 'dashboard'],
        [/reporte/, 'blue', 'bar-chart']
    ];
    function decorateCards() {
        var cards = document.querySelectorAll('.menu-card:not([data-mod]), .admin-btn:not([data-mod])');
        cards.forEach(function (card) {
            try {
                var href = card.getAttribute('href') || '';
                var titleEl = card.querySelector('.mc-title, .ab-title');
                var text = norm(titleEl ? titleEl.textContent : card.textContent);
                var color = null, icon = null;
                var rm = /rep-([a-z]{2})-(\d\d)/.exec(href);
                if (rm && AREAS[rm[1]]) { color = AREAS[rm[1]][0]; icon = REPORT_ICON[rm[1] + '-' + rm[2]]; }
                for (var i = 0; !color && i < CARD_RULES.length; i++) {
                    if (CARD_RULES[i][0].test(text)) { color = CARD_RULES[i][1]; icon = CARD_RULES[i][2]; }
                }
                if (!color) return;
                card.setAttribute('data-mod', color);
                if (card.classList.contains('admin-btn') && !card.querySelector('.ab-ico')) {
                    var body = document.createElement('div');
                    body.className = 'ab-body';
                    while (card.firstChild) body.appendChild(card.firstChild);
                    var wrap = document.createElement('div');
                    wrap.className = 'ab-ico';
                    wrap.appendChild(ico(icon || 'dashboard'));
                    card.appendChild(wrap);
                    card.appendChild(body);
                }
            } catch (e) { /* solo visual */ }
        });
    }

    /* ── 3. Icono en el título de la página ── */
    function decorateTitle() {
        if (!pageMod || !pageIcon) return;
        var t = document.querySelector('.page-title');
        if (!t || t.id || t.querySelector('.title-ico')) return;
        var s = document.createElement('span');
        s.className = 'title-ico';
        s.appendChild(ico(pageIcon));
        t.insertBefore(s, t.firstChild);
    }

    /* ── 4. Icono en tarjetas KPI (no toca los valores) ── */
    var KPI_RULES = [
        [/prima|monto|importe|aporte|saldo|fondo|pension|suma asegurada|costo|ingreso/, 'dollar'],
        [/conversion|tasa|porcentaje|%/, 'percent'],
        [/venta/, 'trending'],
        [/cotizacion/, 'file-text'],
        [/poliza|cartera/, 'shield'],
        [/siniestro/, 'alert'],
        [/aseguradora/, 'building'],
        [/document|archivo|evidencia/, 'folder'],
        [/cliente|usuario|asegurado|afiliad/, 'users'],
        [/aceptad|aprobad|activ|resuelt|cerrad|complet|vigente|disponible/, 'check'],
        [/rechazad|vencid|cancel|anulad|inactiv/, 'x-circle'],
        [/pendient|solicitad|revision|proceso|observad|dias|tiempo|promedio/, 'clock'],
        [/evento|registro|accion|trazab|historial|movimiento/, 'activity']
    ];
    function decorateKpis() {
        if (!pageMod) return;
        document.querySelectorAll('.report-kpi, .secondary-kpi, .stat-box').forEach(function (k) {
            try {
                if (k.querySelector('.kpi-ico')) return;
                var lab = k.querySelector('.stat-label') || k.querySelector('span') || k;
                var txt = norm(lab.textContent);
                var icon = pageIcon || 'bar-chart';
                for (var i = 0; i < KPI_RULES.length; i++) {
                    if (KPI_RULES[i][0].test(txt)) { icon = KPI_RULES[i][1]; break; }
                }
                var w = document.createElement('div');
                w.className = 'kpi-ico';
                w.appendChild(ico(icon));
                k.appendChild(w);
            } catch (e) { /* solo visual */ }
        });
    }

    /* ── 5. Navegación entre reportes: color por área y área actual ── */
    function decorateReportNav() {
        document.querySelectorAll('.report-nav a').forEach(function (a) {
            try {
                var rm = /rep-([a-z]{2})-/.exec(a.getAttribute('href') || '');
                if (rm && AREAS[rm[1]]) {
                    a.setAttribute('data-mod', AREAS[rm[1]][0]);
                    if (rm[1] === areaKey) a.classList.add('is-current');
                }
            } catch (e) { /* solo visual */ }
        });
    }

    /* ── 6. Badges de estado (color según el texto mostrado) ── */
    var ST_RULES = [
        ['sp-st-bad', /rechazad|denegad|anulad|error|fallid|no cubiert/],
        ['sp-st-neutral', /cancelad|inactiv|sin |desactivad|archivad|no disponible/],
        ['sp-st-orange', /observad|vencid|por vencer|suspendid|alerta|urgente/],
        ['sp-st-ok', /activ|aprobad|aceptad|vigente|resuelt|cerrad|complet|pagad|validad|verificad|finaliz|liquidad|habilitad|ok\b/],
        ['sp-st-warn', /pendient|solicitad|en revision|revision|en proceso|proceso|evaluacion|recibid|en espera/],
        ['sp-st-info', /disponible|emitid|registrad|generad|enviad|nuevo|nueva/]
    ];
    var ST_ALL = ['sp-st-ok', 'sp-st-warn', 'sp-st-orange', 'sp-st-bad', 'sp-st-info', 'sp-st-neutral'];
    function decorateBadges(root) {
        (root || document).querySelectorAll('.status-pill, .badge:not([class*="badge-"])').forEach(function (b) {
            try {
                var txt = norm(b.textContent);
                if (b.getAttribute('data-sp-txt') === txt) return;
                b.setAttribute('data-sp-txt', txt);
                ST_ALL.forEach(function (c) { b.classList.remove(c); });
                for (var i = 0; i < ST_RULES.length; i++) {
                    if (ST_RULES[i][1].test(txt)) { b.classList.add(ST_RULES[i][0]); break; }
                }
            } catch (e) { /* solo visual */ }
        });
    }

    function run() {
        decorateCards();
        decorateTitle();
        decorateKpis();
        decorateReportNav();
        decorateBadges();
        /* filas/badges que el propio reporte agrega después de consultar */
        if ('MutationObserver' in window) {
            var pending = false;
            new MutationObserver(function () {
                if (pending) return;
                pending = true;
                requestAnimationFrame(function () { pending = false; decorateBadges(); });
            }).observe(document.body, { childList: true, subtree: true, characterData: true });
        }
    }
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', run);
    else run();
})();
