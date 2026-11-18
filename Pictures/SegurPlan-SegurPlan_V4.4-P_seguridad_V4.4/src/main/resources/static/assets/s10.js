/* =========================================================
   SEGURPLAN
   Pantalla 10 - Carga real de evidencias del siniestro
   ========================================================= */

/* =========================================================
   DOCUMENTOS OBLIGATORIOS
   ========================================================= */

const DOCS_BASE = [
    {
        id: 'dni',
        tipo: 'DNI',
        label: 'DNI del titular',
        obligatorio: true
    },
    {
        id: 'licencia',
        tipo: 'LICENCIA',
        label: 'Licencia de conducir',
        obligatorio: true
    },
    {
        id: 'tarjeta',
        tipo: 'TARJETA_PROPIEDAD',
        label: 'Tarjeta de propiedad del vehículo',
        obligatorio: true
    },
    {
        id: 'soat',
        tipo: 'SOAT',
        label: 'SOAT vigente',
        obligatorio: true
    }
];

let docs = [];

let fotos = [];

let videos = [];

let denunciaArchivos = [];

let huboPolicia = false;

let idSiniestroActual = null;


/* =========================================================
   INICIALIZAR PANTALLA
   ========================================================= */

async function initS10() {

    idSiniestroActual =
        sessionStorage.getItem('sp_last_sin');

    if (!idSiniestroActual) {

        alert(
            'No se encontró el siniestro que acaba de registrar.'
        );

        window.location.href = 's09-siniestro.html';

        return;
    }

    docs = DOCS_BASE.map(doc => ({
        ...doc,
        archivo: null,
        cargado: false,
        idDocumento: null
    }));

    renderDocs();
    renderFotos();
    renderVideos();
    renderDenuncia();

    /*
     * Intentamos consultar el siniestro para saber
     * si se indicó intervención policial.
     */
    await cargarDatosSiniestro();

    /*
     * Si el usuario vuelve a esta pantalla,
     * recuperamos las evidencias ya guardadas.
     */
    await cargarEvidenciasGuardadas();
}


/* =========================================================
   DATOS DEL SINIESTRO
   ========================================================= */

async function cargarDatosSiniestro() {

    try {

        const response = await fetch(
            `/api/siniestros/${idSiniestroActual}`,
            {
                method: 'GET',
                credentials: 'same-origin'
            }
        );

        if (!response.ok) {
            return;
        }

        const siniestro = await response.json();

        /*
         * En el registro anterior guardamos información
         * adicional dentro de observaciones.
         *
         * Si aparece "Policía: Si" o una variante similar,
         * mostramos la sección de denuncia.
         */

        const observaciones =
            (siniestro.observaciones || '').toLowerCase();

        huboPolicia =
            observaciones.includes('policia: si') ||
            observaciones.includes('policía: si') ||
            observaciones.includes('policia=si') ||
            observaciones.includes('policía=si');

        actualizarSeccionDenuncia();

    } catch (error) {

        console.error(
            'No se pudieron consultar los datos del siniestro:',
            error
        );
    }
}


/* =========================================================
   MOSTRAR / OCULTAR DENUNCIA
   ========================================================= */

function actualizarSeccionDenuncia() {

    const seccion =
        document.getElementById('sec-denuncia');

    if (!seccion) {
        return;
    }

    seccion.style.display =
        huboPolicia ? 'block' : 'none';
}


/* =========================================================
   DOCUMENTOS OBLIGATORIOS
   ========================================================= */

function renderDocs() {

    const list =
        document.getElementById('docs-list');

    const status =
        document.getElementById('docs-status');

    if (!list) {
        return;
    }

    list.innerHTML = '';

    docs.forEach((doc, index) => {

        const row =
            document.createElement('div');

        row.className = 'file-item';


        /* -------------------------------
           Parte izquierda
           ------------------------------- */

        const left =
            document.createElement('div');

        left.className = 'file-info';


        const chk =
            document.createElement('span');

        chk.className =
            'file-check ' +
            (doc.cargado ? 'ok' : 'pending');

        chk.textContent =
            doc.cargado ? '✓' : '';


        const info =
            document.createElement('div');

        let descripcion = '';

        if (doc.cargado) {

            descripcion = `
                <div style="
                    font-size:11px;
                    color:var(--success-text);
                    margin-top:2px;">
                    ${escapeHtml(
                        doc.archivo
                            ? doc.archivo.name
                            : 'Documento cargado'
                    )}
                </div>
            `;

        } else {

            descripcion = `
                <div style="
                    font-size:11px;
                    color:var(--error-text);
                    margin-top:2px;">
                    * Documento obligatorio
                </div>
            `;
        }

        info.innerHTML = `
            <div style="
                font-weight:600;
                font-size:13px;">
                ${escapeHtml(doc.label)}
            </div>

            ${descripcion}
        `;

        left.appendChild(chk);
        left.appendChild(info);


        /* -------------------------------
           Botón
           ------------------------------- */

        const btn =
            document.createElement('button');

        btn.className =
            doc.cargado
                ? 'btn btn-sm btn-outline'
                : 'btn btn-sm btn-secondary';

        btn.textContent =
            doc.cargado
                ? 'Eliminar'
                : '+ Adjuntar';


        if (doc.cargado) {

            btn.onclick = async () => {

                if (doc.idDocumento) {

                    await eliminarDocumento(
                        doc.idDocumento
                    );

                } else {

                    docs[index].archivo = null;
                    docs[index].cargado = false;

                    renderDocs();
                }
            };

        } else {

            btn.onclick = () => {

                seleccionarDocumento(
                    index
                );
            };
        }


        row.appendChild(left);
        row.appendChild(btn);

        list.appendChild(row);
    });


    /* =====================================================
       ESTADO DE DOCUMENTACIÓN
       ===================================================== */

    const faltantes =
        docs.filter(
            doc =>
                doc.obligatorio &&
                !doc.cargado
        ).length;


    if (status) {

        if (faltantes > 0) {

            status.innerHTML = `
                <div class="notice warning"
                     style="margin-top:10px;">

                    ⚠ Faltan
                    <strong>${faltantes}</strong>
                    documento${faltantes > 1 ? 's' : ''}
                    obligatorio${faltantes > 1 ? 's' : ''}
                    por adjuntar.

                </div>
            `;

        } else {

            status.innerHTML = `
                <div class="notice success"
                     style="margin-top:10px;">

                    ✓ Documentación completa.

                </div>
            `;
        }
    }
}


/* =========================================================
   SELECCIONAR DOCUMENTO
   ========================================================= */

function seleccionarDocumento(index) {

    const doc = docs[index];

    const input =
        document.createElement('input');

    input.type = 'file';

    input.accept =
        '.pdf,.jpg,.jpeg,.png';

    input.onchange = async () => {

        const archivo =
            input.files &&
            input.files.length > 0
                ? input.files[0]
                : null;

        if (!archivo) {
            return;
        }

        if (!validarTamano(
            archivo,
            10
        )) {
            return;
        }

        const resultado =
            await subirArchivo(
                doc.tipo,
                archivo
            );

        if (!resultado) {
            return;
        }

        docs[index].archivo =
            archivo;

        docs[index].cargado =
            true;

        docs[index].idDocumento =
            resultado.idDocumento;

        renderDocs();
    };

    input.click();
}


/* =========================================================
   DENUNCIA POLICIAL
   ========================================================= */

function addDenuncia() {

    const input =
        document.createElement('input');

    input.type = 'file';

    input.accept =
        '.pdf,.jpg,.jpeg,.png';

    input.onchange = async () => {

        const archivo =
            input.files &&
            input.files.length > 0
                ? input.files[0]
                : null;

        if (!archivo) {
            return;
        }

        if (!validarTamano(
            archivo,
            10
        )) {
            return;
        }

        const resultado =
            await subirArchivo(
                'DENUNCIA_POLICIAL',
                archivo
            );

        if (!resultado) {
            return;
        }

        denunciaArchivos.push({

            idDocumento:
                resultado.idDocumento,

            nombre:
                resultado.nombreArchivo,

            ext:
                obtenerExtension(
                    resultado.nombreArchivo
                )
        });

        renderDenuncia();
    };

    input.click();
}


function renderDenuncia() {

    const list =
        document.getElementById(
            'denuncia-list'
        );

    const status =
        document.getElementById(
            'denuncia-status'
        );

    if (!list) {
        return;
    }

    list.innerHTML = '';


    if (denunciaArchivos.length === 0) {

        list.innerHTML = `
            <div style="
                padding:14px 0;
                color:var(--text-muted);
                font-size:13px;">

                No se han adjuntado archivos
                de denuncia.

            </div>
        `;

    } else {

        denunciaArchivos.forEach(
            (documento) => {

                const row =
                    document.createElement('div');

                row.className =
                    'file-item';


                const left =
                    document.createElement('div');

                left.className =
                    'file-info';

                left.innerHTML = `
                    <div class="file-icon">
                        ${escapeHtml(
                            documento.ext
                        )}
                    </div>

                    <div>
                        <div style="
                            font-weight:600;
                            font-size:13px;">

                            ${escapeHtml(
                                documento.nombre
                            )}

                        </div>

                        <div style="
                            font-size:11px;
                            color:var(--text-muted);">

                            Denuncia / parte policial

                        </div>
                    </div>
                `;


                const btn =
                    document.createElement('button');

                btn.className =
                    'btn btn-sm btn-outline';

                btn.textContent =
                    'Eliminar';

                btn.onclick =
                    async () => {

                        await eliminarDocumento(
                            documento.idDocumento
                        );
                    };


                row.appendChild(left);
                row.appendChild(btn);

                list.appendChild(row);
            }
        );
    }


    if (status) {

        if (
            huboPolicia &&
            denunciaArchivos.length === 0
        ) {

            status.innerHTML = `
                <div class="notice warning"
                     style="margin-top:10px;">

                    ⚠ Se registró intervención policial.
                    Adjunte el acta o denuncia.

                </div>
            `;

        } else if (
            denunciaArchivos.length > 0
        ) {

            status.innerHTML = `
                <div class="notice success"
                     style="margin-top:10px;">

                    ✓ Denuncia adjuntada.

                </div>
            `;

        } else {

            status.innerHTML = '';
        }
    }
}


/* =========================================================
   FOTOGRAFÍAS
   ========================================================= */

function addFoto() {

    const input =
        document.createElement('input');

    input.type = 'file';

    input.accept =
        '.jpg,.jpeg,.png,.heic';

    input.onchange = async () => {

        const archivo =
            input.files &&
            input.files.length > 0
                ? input.files[0]
                : null;

        if (!archivo) {
            return;
        }

        if (!validarTamano(
            archivo,
            20
        )) {
            return;
        }

        const resultado =
            await subirArchivo(
                'FOTO_ACCIDENTE',
                archivo
            );

        if (!resultado) {
            return;
        }

        fotos.push({

            idDocumento:
                resultado.idDocumento,

            nombre:
                resultado.nombreArchivo,

            ext:
                obtenerExtension(
                    resultado.nombreArchivo
                )
        });

        renderFotos();
    };

    input.click();
}


function renderFotos() {

    const list =
        document.getElementById(
            'media-list'
        );

    if (!list) {
        return;
    }

    list.innerHTML = '';


    if (fotos.length === 0) {

        list.innerHTML = `
            <div style="
                padding:14px 0;
                color:var(--text-muted);
                font-size:13px;
                text-align:center;">

                No hay fotografías cargadas.

            </div>
        `;

        return;
    }


    fotos.forEach(
        (foto) => {

            const row =
                document.createElement('div');

            row.className =
                'file-item';


            const left =
                document.createElement('div');

            left.className =
                'file-info';

            left.innerHTML = `
                <div class="file-icon img">
                    ${escapeHtml(foto.ext)}
                </div>

                <div>
                    <div style="
                        font-weight:600;
                        font-size:13px;">

                        ${escapeHtml(
                            foto.nombre
                        )}

                    </div>

                    <div style="
                        font-size:11px;
                        color:var(--text-muted);">

                        Fotografía del accidente

                    </div>
                </div>
            `;


            const btn =
                document.createElement('button');

            btn.className =
                'btn btn-sm btn-outline';

            btn.textContent =
                'Eliminar';

            btn.onclick =
                async () => {

                    await eliminarDocumento(
                        foto.idDocumento
                    );
                };


            row.appendChild(left);
            row.appendChild(btn);

            list.appendChild(row);
        }
    );
}


/* =========================================================
   VIDEOS
   ========================================================= */

function addVideo() {

    const input =
        document.createElement('input');

    input.type = 'file';

    input.accept =
        '.mp4,.mov,.avi';

    input.onchange = async () => {

        const archivo =
            input.files &&
            input.files.length > 0
                ? input.files[0]
                : null;

        if (!archivo) {
            return;
        }

        if (!validarTamano(
            archivo,
            100
        )) {
            return;
        }

        const resultado =
            await subirArchivo(
                'VIDEO_ACCIDENTE',
                archivo
            );

        if (!resultado) {
            return;
        }

        videos.push({

            idDocumento:
                resultado.idDocumento,

            nombre:
                resultado.nombreArchivo,

            ext:
                obtenerExtension(
                    resultado.nombreArchivo
                )
        });

        renderVideos();
    };

    input.click();
}


function renderVideos() {

    const list =
        document.getElementById(
            'video-list'
        );

    if (!list) {
        return;
    }

    list.innerHTML = '';


    if (videos.length === 0) {

        list.innerHTML = `
            <div style="
                padding:14px 0;
                color:var(--text-muted);
                font-size:13px;
                text-align:center;">

                No hay videos cargados.

            </div>
        `;

        return;
    }


    videos.forEach(
        (video) => {

            const row =
                document.createElement('div');

            row.className =
                'file-item';


            const left =
                document.createElement('div');

            left.className =
                'file-info';

            left.innerHTML = `
                <div class="file-icon vid">
                    ${escapeHtml(video.ext)}
                </div>

                <div>
                    <div style="
                        font-weight:600;
                        font-size:13px;">

                        ${escapeHtml(
                            video.nombre
                        )}

                    </div>

                    <div style="
                        font-size:11px;
                        color:var(--text-muted);">

                        Video del accidente

                    </div>
                </div>
            `;


            const btn =
                document.createElement('button');

            btn.className =
                'btn btn-sm btn-outline';

            btn.textContent =
                'Eliminar';

            btn.onclick =
                async () => {

                    await eliminarDocumento(
                        video.idDocumento
                    );
                };


            row.appendChild(left);
            row.appendChild(btn);

            list.appendChild(row);
        }
    );
}


/* =========================================================
   SUBIR ARCHIVO AL BACKEND
   ========================================================= */

async function subirArchivo(
    tipoDocumento,
    archivo
) {

    try {

        const csrf =
            await sp_getCsrfToken();


        const formData =
            new FormData();

        formData.append(
            'tipoDocumento',
            tipoDocumento
        );

        formData.append(
            'archivo',
            archivo
        );


        const response =
            await fetch(
                `/api/documentos/siniestro/${idSiniestroActual}`,
                {
                    method: 'POST',

                    credentials:
                        'same-origin',

                    headers: {
                        [csrf.headerName]:
                            csrf.token
                    },

                    body: formData
                }
            );


        let resultado = null;

        try {
            resultado =
                await response.json();
        } catch (error) {
            resultado = null;
        }


        if (!response.ok) {

            throw new Error(
                resultado?.mensaje ||
                `Error HTTP ${response.status}`
            );
        }


        return resultado;

    } catch (error) {

        console.error(
            'Error subiendo evidencia:',
            error
        );

        alert(
            'No se pudo cargar el archivo.\n\n' +
            error.message
        );

        return null;
    }
}


/* =========================================================
   CARGAR DOCUMENTOS YA GUARDADOS
   ========================================================= */

async function cargarEvidenciasGuardadas() {

    try {

        const response =
            await fetch(
                `/api/documentos/siniestro/${idSiniestroActual}`,
                {
                    method: 'GET',
                    credentials: 'same-origin'
                }
            );


        if (!response.ok) {

            throw new Error(
                `HTTP ${response.status}`
            );
        }


        const documentos =
            await response.json();


        /*
         * Reiniciamos solamente las colecciones
         * recuperables desde PostgreSQL.
         */

        fotos = [];
        videos = [];
        denunciaArchivos = [];


        documentos.forEach(
            documento => {

                const tipo =
                    documento.tipoDocumento;


                /* DOCUMENTOS BASE */

                const docBase =
                    docs.find(
                        doc =>
                            doc.tipo === tipo
                    );

                if (docBase) {

                    docBase.cargado = true;

                    docBase.idDocumento =
                        documento.idDocumento;

                    docBase.archivo = {
                        name:
                            documento.nombreArchivo
                    };

                    return;
                }


                /* DENUNCIA */

                if (
                    tipo ===
                    'DENUNCIA_POLICIAL'
                ) {

                    denunciaArchivos.push({

                        idDocumento:
                            documento.idDocumento,

                        nombre:
                            documento.nombreArchivo,

                        ext:
                            obtenerExtension(
                                documento.nombreArchivo
                            )
                    });

                    return;
                }


                /* FOTOGRAFÍAS */

                if (
                    tipo ===
                    'FOTO_ACCIDENTE'
                ) {

                    fotos.push({

                        idDocumento:
                            documento.idDocumento,

                        nombre:
                            documento.nombreArchivo,

                        ext:
                            obtenerExtension(
                                documento.nombreArchivo
                            )
                    });

                    return;
                }


                /* VIDEOS */

                if (
                    tipo ===
                    'VIDEO_ACCIDENTE'
                ) {

                    videos.push({

                        idDocumento:
                            documento.idDocumento,

                        nombre:
                            documento.nombreArchivo,

                        ext:
                            obtenerExtension(
                                documento.nombreArchivo
                            )
                    });
                }
            }
        );


        renderDocs();
        renderDenuncia();
        renderFotos();
        renderVideos();

    } catch (error) {

        console.error(
            'No se pudieron cargar las evidencias:',
            error
        );
    }
}


/* =========================================================
   ELIMINAR DOCUMENTO
   ========================================================= */

async function eliminarDocumento(
    idDocumento
) {

    if (!idDocumento) {
        return;
    }


    if (!confirm(
        '¿Desea eliminar este archivo?'
    )) {
        return;
    }


    try {

        const csrf =
            await sp_getCsrfToken();


        const response =
            await fetch(
                `/api/documentos/${idDocumento}`,
                {
                    method: 'DELETE',

                    credentials:
                        'same-origin',

                    headers: {
                        [csrf.headerName]:
                            csrf.token
                    }
                }
            );


        let resultado = null;

        try {
            resultado =
                await response.json();
        } catch (error) {
            resultado = null;
        }


        if (!response.ok) {

            throw new Error(
                resultado?.mensaje ||
                `HTTP ${response.status}`
            );
        }


        await cargarEvidenciasGuardadas();

    } catch (error) {

        console.error(
            'Error eliminando evidencia:',
            error
        );

        alert(
            'No se pudo eliminar el archivo.\n\n' +
            error.message
        );
    }
}


/* =========================================================
   GUARDAR Y CONTINUAR
   ========================================================= */

async function guardarEvidencias() {

    const pendientes =
        docs.filter(
            doc =>
                doc.obligatorio &&
                !doc.cargado
        ).length;


    if (pendientes > 0) {

        alert(
            'Aún faltan ' +
            pendientes +
            ' documento(s) obligatorio(s) ' +
            'por adjuntar.'
        );

        return;
    }


    if (
        huboPolicia &&
        denunciaArchivos.length === 0
    ) {

        const continuar =
            confirm(
                'Se registró intervención policial, ' +
                'pero no adjuntó el acta o denuncia.\n\n' +
                '¿Desea continuar de todas formas?'
            );

        if (!continuar) {
            return;
        }
    }


    sessionStorage.setItem(
        'sp_last_sin',
        idSiniestroActual
    );


    window.location.href =
        's11-revision.html';
}


/* =========================================================
   VALIDAR TAMAÑO
   ========================================================= */

function validarTamano(
    archivo,
    maxMB
) {

    const limite =
        maxMB * 1024 * 1024;


    if (archivo.size > limite) {

        alert(
            `El archivo supera el límite de ${maxMB} MB.`
        );

        return false;
    }


    return true;
}


/* =========================================================
   OBTENER EXTENSIÓN
   ========================================================= */

function obtenerExtension(nombre) {

    if (!nombre) {
        return 'FILE';
    }


    const partes =
        nombre.split('.');


    if (partes.length < 2) {
        return 'FILE';
    }


    return partes
        .pop()
        .toUpperCase();
}


/* =========================================================
   ESCAPAR HTML
   ========================================================= */

function escapeHtml(valor) {

    return String(valor ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}