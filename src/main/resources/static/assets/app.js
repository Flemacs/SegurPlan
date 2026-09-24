/* ═══════════════════════════════════════════════════════════
   SEGURPLAN — Capa de datos compartida (localStorage)
   Esquema alineado con base de datos SQL del proyecto
═══════════════════════════════════════════════════════════ */

/* ── Logo SVG ───────────────────────────────────────────── */
const LOGO_SVG = `<svg width="34" height="34" viewBox="0 0 34 34" fill="none" xmlns="http://www.w3.org/2000/svg" style="flex-shrink:0">
  <path d="M17 2L3 8v10c0 7.5 5.9 14.5 14 16.3C25.1 32.5 31 25.5 31 18V8L17 2z"
        fill="rgba(255,255,255,0.18)" stroke="rgba(255,255,255,0.85)" stroke-width="1.5"/>
  <text x="17" y="22" font-family="Segoe UI,Arial,sans-serif" font-size="11"
        font-weight="700" fill="white" text-anchor="middle" letter-spacing="0.5">SP</text>
</svg>`;

/* ── Claves localStorage ────────────────────────────────── */
const KEY_SESSION      = 'sp_session';
const KEY_USUARIOS     = 'sp_usuarios';
const KEY_COTIZACIONES = 'sp_cotizaciones';
const KEY_SINIESTROS   = 'sp_siniestros';
const KEY_PREVISION    = 'sp_prevision';
const KEY_DOCUMENTOS   = 'sp_documentos';
const KEY_COUNTER      = 'sp_counters';

/* ── Catálogos (en memoria, no persisten) ───────────────── */
const ROLES_CAT = [
  { id_rol: 1, nombre: 'Usuario',       descripcion: 'Cliente del sistema' },
  { id_rol: 2, nombre: 'Asesor',        descripcion: 'Asesor comercial' },
  { id_rol: 3, nombre: 'Administrador', descripcion: 'Administrador del sistema' },
  { id_rol: 4, nombre: 'Gerente',       descripcion: 'Gerente con acceso a reportes' },
];

const TIPOS_SEGURO_CAT = [
  { id_tipo_seguro: 1,  nombre: 'Seguro vehicular',            estado: 'Activo' },
  { id_tipo_seguro: 2,  nombre: 'SOAT',                        estado: 'Activo' },
  { id_tipo_seguro: 3,  nombre: 'Seguro de vida',              estado: 'Activo' },
  { id_tipo_seguro: 4,  nombre: 'Seguro de vida con ahorro',   estado: 'Activo' },
  { id_tipo_seguro: 5,  nombre: 'Seguro de salud',             estado: 'Activo' },
  { id_tipo_seguro: 6,  nombre: 'Seguro oncológico',           estado: 'Activo' },
  { id_tipo_seguro: 7,  nombre: 'Seguro de hogar',             estado: 'Activo' },
  { id_tipo_seguro: 8,  nombre: 'Seguro de accidentes personales', estado: 'Activo' },
  { id_tipo_seguro: 9,  nombre: 'Seguro de viaje',             estado: 'Activo' },
  { id_tipo_seguro: 10, nombre: 'Seguro de desgravamen',       estado: 'Activo' },
  { id_tipo_seguro: 11, nombre: 'Seguro agrícola',             estado: 'Activo' },
  { id_tipo_seguro: 12, nombre: 'Seguro estudiantil',          estado: 'Activo' },
  { id_tipo_seguro: 13, nombre: 'Seguro de sepelio',           estado: 'Activo' },
  { id_tipo_seguro: 14, nombre: 'Seguro de desempleo',         estado: 'Activo' },
];

const ASEGURADORAS_CAT = [
  { id_aseguradora: 1, nombre: 'Rímac Seguros',     ruc: '20100041953', estado: 'Activo' },
  { id_aseguradora: 2, nombre: 'Pacífico Seguros',  ruc: '20112929794', estado: 'Activo' },
  { id_aseguradora: 3, nombre: 'La Positiva',        ruc: '20100055237', estado: 'Activo' },
  { id_aseguradora: 4, nombre: 'Mapfre Perú',        ruc: '20293847346', estado: 'Activo' },
  { id_aseguradora: 5, nombre: 'Interseguro',        ruc: '20330791684', estado: 'Activo' },
  { id_aseguradora: 6, nombre: 'Seguros SURA',       ruc: '20418896915', estado: 'Activo' },
  { id_aseguradora: 7, nombre: 'BNP Paribas Cardif', ruc: '20376819428', estado: 'Activo' },
];

/* Estados válidos por entidad */
const ESTADOS_COTIZACION   = ['Solicitada','En revisión','Disponible','Aceptada','Rechazada','Vencida'];
const ESTADOS_SINIESTRO    = ['Registrado','En revisión','Información pendiente','Derivado','En evaluación','Observado','Aprobado','Rechazado','Cerrado'];
const ESTADOS_CONTRATACION = ['Iniciada','Documentación pendiente','Documentación validada','En revisión legal','Enviada a aseguradora','En evaluación','Aprobada','Rechazada','Finalizada'];
const ESTADOS_DOCUMENTO    = ['Pendiente','Validado','Observado','Rechazado'];
const ESTADOS_PREVISION    = ['Registrada','En revisión','Simulada','Respondida','Cerrada'];

/* ══════════════════════════════════════════════════════════
   UTILIDADES
══════════════════════════════════════════════════════════ */
function sp_get(key)        { try { return JSON.parse(localStorage.getItem(key)); } catch(e) { return null; } }
function sp_set(key, value) { localStorage.setItem(key, JSON.stringify(value)); }

function sp_nextId(tipo) {
  const c = sp_get(KEY_COUNTER) || {};
  c[tipo] = (c[tipo] || 0) + 1;
  sp_set(KEY_COUNTER, c);
  return c[tipo];
}

function sp_now() {
  return new Date().toLocaleString('es-PE', {
    day:'2-digit', month:'2-digit', year:'numeric',
    hour:'2-digit', minute:'2-digit'
  });
}

function sp_aseguradoraById(id)         { return ASEGURADORAS_CAT.find(a => a.id_aseguradora === id)  || null; }
function sp_aseguradoraByNombre(nombre) { return ASEGURADORAS_CAT.find(a => a.nombre === nombre)       || null; }
function sp_tipoById(id)                { return TIPOS_SEGURO_CAT.find(t => t.id_tipo_seguro === id)  || null; }
function sp_tipoByNombre(nombre)        { return TIPOS_SEGURO_CAT.find(t => t.nombre === nombre)       || null; }
function sp_rolById(id)                 { return ROLES_CAT.find(r => r.id_rol === id)                  || null; }

/* ══════════════════════════════════════════════════════════
   SESIÓN / AUTENTICACIÓN
   Campo de acceso: correo  (tabla usuario)
══════════════════════════════════════════════════════════ */
async function sp_login(correo, password) {

  try {

    const response = await fetch('/api/usuarios/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        correo: correo,
        password: password
      })
    });

    const data = await response.json();

    if (!response.ok) {
      return {
        ok: false,
        error: data.mensaje || 'Correo o contraseña incorrectos.'
      };
    }

    const usuario = {
      id_usuario: data.idUsuario,
      nombres: data.nombres,
      apellidos: data.apellidos,
      correo: data.correo,
      rol: data.rol,
      loginAt: sp_now()
    };

    // Conservamos la sesión del frontend existente
    sp_set(KEY_SESSION, usuario);

    return {
      ok: true,
      usuario: usuario
    };

  } catch (error) {

    console.error('Error al iniciar sesión:', error);

    return {
      ok: false,
      error: 'No se pudo conectar con el servidor.'
    };
  }
}
async function sp_logout() {

    try {

        // Obtener token CSRF
        const csrf = await sp_getCsrfToken();

        // Cerrar la sesión real de Spring Security
        const response = await fetch('/api/usuarios/logout', {
            method: 'POST',
            credentials: 'same-origin',
            headers: {
                [csrf.headerName]: csrf.token
            }
        });

        if (!response.ok) {
            console.error(
                'No se pudo cerrar la sesión del servidor. HTTP',
                response.status
            );
        }

    } catch (error) {

        console.error(
            'Error al cerrar la sesión:',
            error
        );

    } finally {

        // Eliminar datos locales aunque haya ocurrido
        // algún problema de comunicación.
        localStorage.removeItem('sp_session');

        // Limpiar información temporal relacionada
        // con operaciones del usuario.
        sessionStorage.removeItem('sp_last_quote');
        sessionStorage.removeItem('sp_last_contratacion');
        sessionStorage.removeItem('sp_admin_quote');

        // Volver al login.
        window.location.replace('/pages/s01-login.html');
    }
}

function sp_getSession() { return sp_get(KEY_SESSION); }

function sp_requireAuth(redirectTo) {
  const redir = redirectTo || '../pages/s01-login.html';
  const session = sp_getSession();
  if (!session) { window.location.href = redir; }
  return session;
}

/* ══════════════════════════════════════════════════════════
   USUARIOS  (tabla: usuario)
══════════════════════════════════════════════════════════ */
async function sp_registerUser(data) {
  try {
    const response = await fetch('/api/usuarios/registro', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        nombres: data.nombres,
        apellidos: data.apellidos,
        correo: data.correo,
        telefono: data.telefono,
        password: data.password
      })
    });
    const resultado = await response.json();
    if (!response.ok) {
      return {
        ok: false,
        error: resultado.mensaje || 'No se pudo registrar el usuario.'
      };
    }
    return {
      ok: true,
      usuario: resultado
    };
  } catch (error) {
    console.error('Error al registrar usuario:', error);
    return {
      ok: false,
      error: 'No se pudo conectar con el servidor.'
    };
  }
}

function sp_getUsuarios() { return sp_get(KEY_USUARIOS) || []; }
function sp_getUsuario(id) { return sp_getUsuarios().find(u => u.id_usuario === id) || null; }

/* ══════════════════════════════════════════════════════════
   COTIZACIONES  (tabla: cotizacion)
══════════════════════════════════════════════════════════ */
async function sp_saveCotizacion(data) {
    try {
        const session = sp_getSession();

        if (!session) {
            return {
                ok: false,
                error: 'Debes iniciar sesión.'
            };
        }

        // Obtener el token CSRF de la sesión actual.
        const csrf = await sp_getCsrfToken();

        // Registrar la cotización en Spring Boot.
        const response = await fetch('/api/cotizaciones', {
            method: 'POST',
            credentials: 'same-origin',
            headers: {
                'Content-Type': 'application/json',
                [csrf.headerName]: csrf.token
            },
            body: JSON.stringify({
                idUsuario: Number(session.id_usuario),
                idTipoSeguro: Number(data.idTipoSeguro),
                idAseguradora: Number(data.idAseguradora),
                descripcionSolicitud: data.descripcionSolicitud
            })
        });

        const texto = await response.text();

        let resultado;

        try {
            resultado = texto ? JSON.parse(texto) : {};
        } catch {
            resultado = {
                mensaje: texto
            };
        }

        if (!response.ok) {
            console.error(
                'Error HTTP al registrar cotización:',
                response.status,
                resultado
            );

            let mensaje = resultado.mensaje ||
                          resultado.detail ||
                          resultado.error;

            if (!mensaje) {
                if (response.status === 401) {
                    mensaje = 'Tu sesión no está activa. Inicia sesión nuevamente.';
                } else if (response.status === 403) {
                    mensaje = 'Acceso denegado. Comprueba tu sesión, el token CSRF y tus permisos.';
                } else {
                    mensaje = 'No se pudo registrar la cotización. HTTP ' +
                              response.status;
                }
            }

            return {
                ok: false,
                error: mensaje
            };
        }

        return {
            ok: true,
            cotizacion: resultado
        };

    } catch (error) {
        console.error(
            'Error al registrar cotización:',
            error
        );

        return {
            ok: false,
            error: error.message ||
                   'No se pudo conectar con el servidor.'
        };
    }
}

/* Alias retrocompatibilidad */
const sp_saveQuote = sp_saveCotizacion;

function sp_getCotizaciones(id_usuario) {
  const lista = sp_get(KEY_COTIZACIONES) || [];
  return id_usuario !== undefined ? lista.filter(c => c.id_usuario === id_usuario) : lista;
}
const sp_getQuotes = sp_getCotizaciones;

async function sp_getCotizacion(idCotizacion) {

    try {

        const response = await fetch(
            '/api/cotizaciones/' + idCotizacion
        );

        const resultado = await response.json();

        if (!response.ok) {

            return {
                ok: false,
                error:
                    resultado.mensaje ||
                    'No se encontró la cotización.'
            };
        }

        return {
            ok: true,
            cotizacion: resultado
        };

    } catch (error) {

        console.error(
            'Error obteniendo cotización:',
            error
        );

        return {
            ok: false,
            error:
                'No se pudo conectar con el servidor.'
        };
    }
}
const sp_getQuote = sp_getCotizacion;

/* ══════════════════════════════════════════════════════════
   SINIESTROS  (tabla: siniestro)
══════════════════════════════════════════════════════════ */
function sp_saveSiniestro(data) {
  const lista   = sp_get(KEY_SINIESTROS) || [];
  const session = sp_getSession();
  const aseg    = sp_aseguradoraByNombre(data.aseguradora) || {};
  const sinId   = sp_nextId('siniestro');

  const s = {
    id_siniestro:         sinId,
    id_usuario:           session ? session.id_usuario : null,
    id_aseguradora:       aseg.id_aseguradora || null,
    numero_siniestro:     'SIN-' + String(sinId).padStart(6, '0'),
    fecha_registro:       sp_now(),
    fecha_accidente:      data.fechaAcc  || null,
    /* datos del vehículo */
    placa:                data.placa    || null,
    marca:                data.marca    || null,
    modelo:               data.modelo   || null,
    anio:                 data.anio ? parseInt(data.anio) : null,
    /* datos del accidente */
    lugar:                data.ubicacion || null,
    descripcion:          data.descripcion || null,
    /* seguimiento */
    resultado_evaluacion: null,
    observaciones:        null,
    estado:               'Registrado',
    historial:            [{ estado: 'Registrado', fecha: sp_now() }],
    /* campos extra del formulario */
    _extra: {
      tipoVehiculo:    data.tipoVehiculo   || null,
      color:           data.color          || null,
      tarjetaProp:     data.tarjetaProp    || null,
      tipoSiniestro:   data.tipoSiniestro  || null,
      horaAcc:         data.horaAcc        || null,
      depto:           data.depto          || null,
      distrito:        data.distrito       || null,
      heridos:         data.heridos        || 'No',
      numHeridos:      data.numHeridos     || null,
      gravedad:        data.gravedad       || null,
      policia:         data.policia        || 'No',
      actaPolicial:    data.actaPolicial   || null,
      comisaria:       data.comisaria      || null,
      numVehiculos:    data.numVehiculos   || '1',
      numPoliza:       data.numPoliza      || null,
      _nombre_aseguradora: aseg.nombre    || data.aseguradora || null,
      tipoCobertura:   data.tipoCobertura  || null,
      vigencia:        data.vigencia       || null,
      conductorNombre:    data.conductorNombre    || null,
      conductorDni:       data.conductorDni       || null,
      conductorLicencia:  data.conductorLicencia  || null,
      conductorTelefono:  data.conductorTelefono  || null,
      esTitular:          data.esTitular          || 'Si'
    }
  };
  lista.push(s);
  sp_set(KEY_SINIESTROS, lista);
  return s;
}

function sp_getSiniestros(id_usuario) {
  const lista = sp_get(KEY_SINIESTROS) || [];
  return id_usuario !== undefined ? lista.filter(s => s.id_usuario === id_usuario) : lista;
}

function sp_getSiniestro(id) {
  return (sp_get(KEY_SINIESTROS) || []).find(s =>
    s.id_siniestro === id || s.numero_siniestro === id
  ) || null;
}

/* ══════════════════════════════════════════════════════════
   PREVISIÓN AFP/ONP  (tabla: prevision)
══════════════════════════════════════════════════════════ */
function sp_savePrevision(data) {
  const lista   = sp_get(KEY_PREVISION) || [];
  const session = sp_getSession();
  const p = {
    id_prevision:           sp_nextId('prevision'),
    id_usuario:             session ? session.id_usuario : null,
    tipo_sistema:           data.tipo_sistema || 'AFP',
    edad:                   data.edad            ? parseInt(data.edad)                 : null,
    ingreso_mensual:        data.ingreso_mensual ? parseFloat(data.ingreso_mensual)    : null,
    anios_aporte:           data.anios_aporte    ? parseInt(data.anios_aporte)         : null,
    fecha_registro:         sp_now(),
    monto_pension_estimado: data.monto_pension_estimado ? parseFloat(data.monto_pension_estimado) : null,
    resultado_simulacion:   data.resultado_simulacion || null,
    consulta:               data.consulta  || null,
    respuesta:              data.respuesta || null,
    estado:                 'Simulada',
    _extra: {
      perfil:           data.perfil           || null,
      afp_seleccionada: data.afp_seleccionada || null
    }
  };
  lista.push(p);
  sp_set(KEY_PREVISION, lista);
  return p;
}

function sp_getPrevision(id_usuario) {
  const lista = sp_get(KEY_PREVISION) || [];
  return id_usuario !== undefined ? lista.filter(p => p.id_usuario === id_usuario) : lista;
}

/* ══════════════════════════════════════════════════════════
   DOCUMENTOS  (tabla: documento)
══════════════════════════════════════════════════════════ */
function sp_saveDocumento(data) {
  const lista   = sp_get(KEY_DOCUMENTOS) || [];
  const session = sp_getSession();
  const doc = {
    id_documento:    sp_nextId('documento'),
    id_usuario:      session ? session.id_usuario : null,
    id_contratacion: data.id_contratacion || null,
    id_siniestro:    data.id_siniestro    || null,
    tipo_documento:  data.tipo_documento,
    nombre_archivo:  data.nombre_archivo,
    ruta_archivo:    '/uploads/' + data.nombre_archivo,
    version:         data.version || 1,
    fecha_carga:     sp_now(),
    estado:          'Pendiente',
  };
  lista.push(doc);
  sp_set(KEY_DOCUMENTOS, lista);
  return doc;
}


/* ══════════════════════════════════════════════════════════
   INICIALIZACIÓN DE PÁGINA
══════════════════════════════════════════════════════════ */

document.addEventListener('DOMContentLoaded', () => {

    // Inyectar logo en cabecera.
    const logoEl = document.querySelector('.site-header .logo');

    if (logoEl) {
        logoEl.style.display = 'flex';
        logoEl.style.alignItems = 'center';
        logoEl.style.gap = '10px';
        logoEl.innerHTML =
            LOGO_SVG + '<div>' + logoEl.innerHTML + '</div>';
    }

    // Mostrar nombre del usuario en cabecera.
    const userNameEl =
        document.querySelector('.header-right .user-name');

    if (userNameEl) {
        const session = sp_getSession();

        if (session) {
            userNameEl.textContent =
                session.nombres + ' ' + session.apellidos;
        }
    }

    // Crear el usuario de demostración si no existe ninguno.
    if (sp_getUsuarios().length === 0) {
        sp_set(KEY_USUARIOS, [{
            id_usuario: 1,
            id_rol: 1,
            nombres: 'Juan Carlos',
            apellidos: 'García López',
            correo: 'juan@correo.com',
            password: 'Demo1234!',
            telefono: '+51 999 999 999',
            estado: 'Activo',
            fecha_registro: '01/01/2026 08:00',
            dni: '12345678',
            fnacimiento: '1990-05-15',
            genero: 'Masculino',
            departamento: 'Lima'
        }]);
    }

}); // Aquí termina DOMContentLoaded.//

    // ==========================================
    // BOTÓN CERRAR SESIÓN
    // ==========================================

    const botonesLogout = document.querySelectorAll(
        '.btn-logout, .logout-btn, [data-logout]'
    );

    botonesLogout.forEach(boton => {

        boton.addEventListener('click', async function(event) {

            event.preventDefault();

            // Evitar varios clics mientras se cierra la sesión
            if (boton.dataset.cerrando === 'true') {
                return;
            }

            boton.dataset.cerrando = 'true';

            const textoOriginal = boton.textContent;

            boton.textContent = 'Cerrando sesión...';

            if ('disabled' in boton) {
                boton.disabled = true;
            }

            try {
                await sp_logout();

            } catch (error) {

                console.error(
                    'Error al ejecutar cierre de sesión:',
                    error
                );

                boton.textContent = textoOriginal;

                if ('disabled' in boton) {
                    boton.disabled = false;
                }

                boton.dataset.cerrando = 'false';
            }
        });
    });

/* ══════════════════════════════════════════════════════════
   TOKEN CSRF - SPRING SECURITY
══════════════════════════════════════════════════════════ */

async function sp_getCsrfToken() {

    const response = await fetch('/api/csrf', {
        method: 'GET',
        credentials: 'same-origin'
    });

    if (!response.ok) {
        throw new Error(
            'No se pudo obtener el token de seguridad. HTTP ' +
            response.status
        );
    }

    const csrf = await response.json();

    if (!csrf.token || !csrf.headerName) {
        throw new Error(
            'El servidor devolvió un token CSRF inválido.'
        );
    }

    return csrf;
}
