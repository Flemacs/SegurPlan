/* SEGURPLAN — Pantalla 10: Carga de evidencias */

/* Documentos obligatorios base */
const DOCS_BASE = [
  { id: 'dni',      label: 'DNI del titular',                   obligatorio: true  },
  { id: 'licencia', label: 'Licencia de conducir',              obligatorio: true  },
  { id: 'tarjeta',  label: 'Tarjeta de propiedad del vehículo', obligatorio: true  },
  { id: 'soat',     label: 'SOAT vigente',                      obligatorio: true  },
];

let docs  = [];
let media = [];   /* fotos y videos */
let denunciaArchivos = [];
let huboPolicia = false;
let mediaCounter = 1;
let denunciaCounter = 1;

/* ── Inicializar ─────────────────────────────────────── */
function initS10() {
  /* Determinar si hubo intervención policial */
  const sinId = sessionStorage.getItem('sp_last_sin');
  if (sinId) {
    const s = sp_getSiniestro(sinId);
    huboPolicia = s && s.policia === 'Si';
  }

  /* Construir lista de documentos */
  docs = DOCS_BASE.map(d => ({ ...d, cargado: false }));

  /* Mostrar/ocultar sección de denuncia policial */
  const secDenuncia = document.getElementById('sec-denuncia');
  if (secDenuncia) secDenuncia.style.display = huboPolicia ? 'block' : 'none';

  renderDocs();
  renderMedia();
  renderDenuncia();
}

/* ── Documentos obligatorios ─────────────────────────── */
function renderDocs() {
  const list   = document.getElementById('docs-list');
  const status = document.getElementById('docs-status');
  if (!list) return;
  list.innerHTML = '';

  docs.forEach((doc, i) => {
    const row  = document.createElement('div');
    row.className = 'file-item';

    const left = document.createElement('div');
    left.className = 'file-info';

    const chk = document.createElement('span');
    chk.className = 'file-check ' + (doc.cargado ? 'ok' : 'pending');
    chk.textContent = doc.cargado ? '✓' : '';

    const info = document.createElement('div');
    info.innerHTML = `
      <div style="font-weight:600;font-size:13px;color:${doc.cargado ? 'var(--text)' : 'var(--text-muted)'};">${doc.label}</div>
      ${doc.obligatorio && !doc.cargado ? '<div style="font-size:11px;color:var(--error-text);margin-top:2px;">* Documento obligatorio</div>' : ''}
      ${doc.cargado ? '<div style="font-size:11px;color:var(--success-text);margin-top:2px;">Documento cargado</div>' : ''}`;

    left.appendChild(chk);
    left.appendChild(info);

    const btn = document.createElement('button');
    btn.className = doc.cargado ? 'btn btn-sm btn-outline' : 'btn btn-sm btn-secondary';
    btn.textContent = doc.cargado ? 'Eliminar' : '+ Adjuntar';
    btn.onclick = () => { docs[i].cargado = !docs[i].cargado; renderDocs(); };

    row.appendChild(left);
    row.appendChild(btn);
    list.appendChild(row);
  });

  const faltantes = docs.filter(d => d.obligatorio && !d.cargado).length;
  if (status) {
    status.innerHTML = faltantes > 0
      ? `<div class="notice warning" style="margin-top:10px;">⚠ Faltan <strong>${faltantes}</strong> documento${faltantes > 1 ? 's' : ''} obligatorio${faltantes > 1 ? 's' : ''} por adjuntar.</div>`
      : `<div class="notice success" style="margin-top:10px;">✓ Documentación completa.</div>`;
  }
}

/* ── Denuncia policial ───────────────────────────────── */
function renderDenuncia() {
  const list   = document.getElementById('denuncia-list');
  const status = document.getElementById('denuncia-status');
  if (!list) return;
  list.innerHTML = '';

  if (denunciaArchivos.length === 0) {
    list.innerHTML = '<div style="padding:14px 0;color:var(--text-muted);font-size:13px;">No se han adjuntado archivos de denuncia.</div>';
  } else {
    denunciaArchivos.forEach((d, i) => {
      const row = document.createElement('div');
      row.className = 'file-item';
      const left = document.createElement('div');
      left.className = 'file-info';
      left.innerHTML = `<div class="file-icon">${d.ext}</div>
        <div>
          <div style="font-weight:600;font-size:13px;">${d.nombre}</div>
          <div style="font-size:11px;color:var(--text-muted);">Denuncia / parte policial</div>
        </div>`;
      const btn = document.createElement('button');
      btn.className = 'btn btn-sm btn-outline';
      btn.textContent = 'Eliminar';
      btn.onclick = () => { denunciaArchivos.splice(i, 1); renderDenuncia(); };
      row.appendChild(left); row.appendChild(btn);
      list.appendChild(row);
    });
  }

  if (status) {
    status.innerHTML = (huboPolicia && denunciaArchivos.length === 0)
      ? `<div class="notice warning" style="margin-top:10px;">⚠ Se registró intervención policial. Adjunte el acta o denuncia.</div>`
      : (denunciaArchivos.length > 0 ? `<div class="notice success" style="margin-top:10px;">✓ Denuncia adjuntada.</div>` : '');
  }
}

function addDenuncia() {
  const exts = ['PDF', 'PDF', 'JPG', 'PNG'];
  const ext  = exts[denunciaCounter % exts.length];
  denunciaArchivos.push({ nombre: 'denuncia_policial_' + String(denunciaCounter).padStart(2,'0') + '.' + ext.toLowerCase(), ext });
  denunciaCounter++;
  renderDenuncia();
}

/* ── Archivos multimedia (fotos y videos) ────────────── */
function renderMedia() {
  const list = document.getElementById('media-list');
  if (!list) return;
  list.innerHTML = '';

  if (media.length === 0) {
    list.innerHTML = '<div style="padding:14px 0;color:var(--text-muted);font-size:13px;text-align:center;">No hay archivos cargados.</div>';
    return;
  }

  media.forEach((m, i) => {
    const row = document.createElement('div');
    row.className = 'file-item';
    const left = document.createElement('div');
    left.className = 'file-info';

    const isVideo = ['MP4','MOV','AVI','MKV'].includes(m.ext);
    const iconClass = isVideo ? 'file-icon vid' : 'file-icon img';
    left.innerHTML = `<div class="${iconClass}">${m.ext}</div>
      <div>
        <div style="font-weight:600;font-size:13px;">${m.nombre}</div>
        <div style="font-size:11px;color:var(--text-muted);">${isVideo ? 'Video del accidente' : 'Fotografía del accidente'}</div>
      </div>`;

    const btn = document.createElement('button');
    btn.className = 'btn btn-sm btn-outline';
    btn.textContent = 'Eliminar';
    btn.onclick = () => { media.splice(i, 1); renderMedia(); };

    row.appendChild(left); row.appendChild(btn);
    list.appendChild(row);
  });
}

function addFoto() {
  const exts   = ['JPG', 'JPG', 'PNG', 'HEIC'];
  const ext    = exts[mediaCounter % exts.length];
  media.push({ nombre: 'foto_accidente_' + String(mediaCounter).padStart(2,'0') + '.' + ext.toLowerCase(), ext });
  mediaCounter++;
  renderMedia();
}

function addVideo() {
  const exts = ['MP4', 'MP4', 'MOV', 'AVI'];
  const ext  = exts[mediaCounter % exts.length];
  media.push({ nombre: 'video_accidente_' + String(mediaCounter).padStart(2,'0') + '.' + ext.toLowerCase(), ext });
  mediaCounter++;
  renderMedia();
}
