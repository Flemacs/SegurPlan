# Endpoints de SEGURPLAN

Total: **102 operaciones** en 20 grupos. Generado desde el código (`docs/openapi.yaml` es la versión importable en [swagger.io](https://editor.swagger.io)).

- **Swagger UI** con el sistema en marcha: `http://localhost:8080/swagger-ui.html`
- **Definición viva (JSON):** `/v3/api-docs` · **YAML:** `/v3/api-docs.yaml`

Todas las rutas, salvo las marcadas *Público*, exigen sesión. `POST`, `PUT` y `DELETE` exigen además la cabecera `X-CSRF-TOKEN` (valor de `GET /api/csrf`).

## Admin · Asesores

| Método | Ruta | Quién puede usarlo | Qué hace |
|---|---|---|---|
| `GET` | `/api/admin/asesores` | Administrador | Listar asesores. Incluye el área de especialidad. |
| `GET` | `/api/admin/asesores/{idUsuario}` | Administrador | Ver un asesor. Detalle del asesor. |
| `POST` | `/api/admin/asesores` | Administrador | Registrar un asesor. Área de especialidad: Ventas, Siniestros o AFP. |
| `PUT` | `/api/admin/asesores/{idUsuario}` | Administrador | Actualizar un asesor. Datos y especialidad. |
| `PUT` | `/api/admin/asesores/{idUsuario}/estado` | Administrador | Activar o desactivar un asesor. Estado Activo o Inactivo. |

## Admin · Respaldos

| Método | Ruta | Quién puede usarlo | Qué hace |
|---|---|---|---|
| `GET` | `/api/admin/backups` | Administrador | Listar respaldos. Archivos .sql disponibles. |
| `POST` | `/api/admin/backups` | Administrador | Crear un respaldo ahora. Ejecuta pg_dump. También hay respaldo automático cada 6 horas. |
| `GET` | `/api/admin/backups/{nombre}/descargar` | Administrador | Descargar un respaldo. Entrega el archivo .sql. |
| `POST` | `/api/admin/backups/{nombre}/restaurar` | Administrador | Restaurar un respaldo. Operación destructiva: reemplaza los datos actuales. |
| `DELETE` | `/api/admin/backups/{nombre}` | Administrador | Eliminar un respaldo. Borra el archivo del servidor. |

## Admin · Catálogos

| Método | Ruta | Quién puede usarlo | Qué hace |
|---|---|---|---|
| `GET` | `/api/admin/catalogos/aseguradoras` | Administrador | Listar aseguradoras (todas). Incluye las inactivas. |
| `POST` | `/api/admin/catalogos/aseguradoras` | Administrador | Crear una aseguradora. Alta en el catálogo. |
| `PUT` | `/api/admin/catalogos/aseguradoras/{id}` | Administrador | Actualizar una aseguradora. Datos de la aseguradora. |
| `PUT` | `/api/admin/catalogos/aseguradoras/{id}/estado` | Administrador | Activar o desactivar una aseguradora. Una aseguradora inactiva deja de ofrecerse en los formularios. |
| `GET` | `/api/admin/catalogos/tipos-seguro` | Administrador | Listar tipos de seguro (todos). Incluye los inactivos. |
| `POST` | `/api/admin/catalogos/tipos-seguro` | Administrador | Crear un tipo de seguro. Alta en el catálogo. |
| `PUT` | `/api/admin/catalogos/tipos-seguro/{id}` | Administrador | Actualizar un tipo de seguro. Datos del tipo. |
| `PUT` | `/api/admin/catalogos/tipos-seguro/{id}/estado` | Administrador | Activar o desactivar un tipo de seguro. Un tipo inactivo deja de ofrecerse. |

## Catálogos

| Método | Ruta | Quién puede usarlo | Qué hace |
|---|---|---|---|
| `GET` | `/api/catalogos/tipos-seguro` | Cualquier usuario autenticado | Listar tipos de seguro activos. Solo devuelve los tipos en estado Activo. |
| `GET` | `/api/catalogos/aseguradoras` | Cualquier usuario autenticado | Listar aseguradoras activas. Solo devuelve las aseguradoras en estado Activo. |

## Contrataciones

| Método | Ruta | Quién puede usarlo | Qué hace |
|---|---|---|---|
| `GET` | `/api/contrataciones/pendientes` | Asesor, Administrador | Listar contrataciones pendientes. Contrataciones aún sin póliza. |
| `POST` | `/api/contrataciones` | Asesor, Administrador | Registrar una contratación. Crea la contratación de una cotización aceptada. |

## Cotizaciones

| Método | Ruta | Quién puede usarlo | Qué hace |
|---|---|---|---|
| `POST` | `/api/cotizaciones` | Cualquier usuario autenticado | Solicitar una cotización. El solicitante es siempre el usuario en sesión. Admite datosJson con el formulario dinámico. |
| `GET` | `/api/cotizaciones/{idCotizacion}` | Dueño del registro, Asesor o Administrador | Ver una cotización. Detalle de la cotización. |
| `GET` | `/api/cotizaciones/usuario/{idUsuario}` | Dueño del registro, Asesor o Administrador | Listar cotizaciones de un usuario. Cotizaciones del usuario indicado. |
| `GET` | `/api/cotizaciones` | Asesor, Administrador | Listar todas las cotizaciones. Listado general para el personal. |
| `PUT` | `/api/cotizaciones/{idCotizacion}/respuesta` | Asesor, Administrador | Responder una cotización. Registra prima, cobertura, deducible y vigencia, y notifica al cliente. |
| `POST` | `/api/cotizaciones/{id}/comunicaciones` | Asesor, Administrador | Registrar comunicación con la aseguradora. Deja constancia de un contacto con la aseguradora sobre la cotización. |
| `GET` | `/api/cotizaciones/{id}/comunicaciones` | Asesor, Administrador | Listar comunicaciones con la aseguradora. Historial de contactos de la cotización. |
| `GET` | `/api/cotizaciones/{id}/pdf` | Dueño del registro, Asesor o Administrador | Descargar la cotización en PDF. PDF generado con el generador propio (SimplePdf/PdfService). |

## Autenticación

| Método | Ruta | Quién puede usarlo | Qué hace |
|---|---|---|---|
| `GET` | `/api/csrf` | Público (sin sesión) | Obtener el token CSRF. Devuelve headerName, parameterName y token. Envíe el token en la cabecera X-CSRF-TOKEN en POST, PUT y DELETE. |
| `POST` | `/api/usuarios/registro` | Público (sin sesión) | Registrar un cliente nuevo. Crea una cuenta con rol Cliente. Contraseña: mínimo 8 caracteres con letras y números. |
| `POST` | `/api/usuarios/login` | Público (sin sesión) | Iniciar sesión. Crea la sesión (cookie JSESSIONID). Tras 5 intentos fallidos del mismo correo (o 20 de la misma IP) responde 429 durante 15 minutos. |
| `GET` | `/api/usuarios/admin` | Administrador | Listar usuarios (ruta heredada). Duplicado de GET /api/admin/usuarios; se conserva por compatibilidad. |
| `GET` | `/api/usuarios/admin/{idUsuario}` | Administrador | Ver un usuario (ruta heredada). Duplicado de GET /api/admin/usuarios/{idUsuario}. |
| `PUT` | `/api/usuarios/admin/{idUsuario}` | Administrador | Actualizar un usuario (ruta heredada). Duplicado de PUT /api/admin/usuarios/{idUsuario}. |
| `PUT` | `/api/usuarios/admin/{idUsuario}/estado` | Administrador | Activar o desactivar un usuario (ruta heredada). Duplicado de PUT /api/admin/usuarios/{idUsuario}/estado. |

## Documentos

| Método | Ruta | Quién puede usarlo | Qué hace |
|---|---|---|---|
| `POST` | `/api/documentos/siniestro/{idSiniestro}` | Cualquier usuario autenticado | Subir una evidencia de siniestro. multipart/form-data. PDF, JPG o PNG hasta 10 MB; video hasta 100 MB. Solo el dueño. |
| `GET` | `/api/documentos/siniestro/{idSiniestro}` | Cualquier usuario autenticado | Listar evidencias de mi siniestro. Solo el dueño del siniestro. |
| `DELETE` | `/api/documentos/{idDocumento}` | Cualquier usuario autenticado | Eliminar una evidencia. Solo el dueño y mientras el caso lo permita. |
| `GET` | `/api/documentos/asesor/siniestro/{idSiniestro}` | Asesor, Administrador | Listar evidencias (vista del asesor). Todas las evidencias del siniestro. |
| `PUT` | `/api/documentos/{id}/validacion` | Asesor, Administrador | Validar, observar o rechazar un documento. Exige motivo al observar o rechazar; avisa al cliente. |
| `GET` | `/api/documentos/{id}/archivo` | Dueño del registro, Asesor o Administrador | Descargar o ver un archivo. Entrega el archivo de forma segura. |
| `POST` | `/api/documentos/cotizacion/{idCotizacion}` | Cualquier usuario autenticado | Adjuntar un archivo a una cotización. multipart/form-data. PDF, JPG o PNG hasta 10 MB. |
| `GET` | `/api/documentos/cotizacion/{idCotizacion}` | Dueño del registro, Asesor o Administrador | Listar adjuntos de una cotización. Adjuntos de la cotización. |

## Admin · Historial

| Método | Ruta | Quién puede usarlo | Qué hace |
|---|---|---|---|
| `GET` | `/api/admin/historial` | Administrador | Listar el historial de cambios. Bitácora inmutable (un trigger impide UPDATE y DELETE). |
| `GET` | `/api/admin/historial/entidad/{entidad}` | Administrador | Historial por entidad. Filtra por tipo de registro. |

## Mis procesos

| Método | Ruta | Quién puede usarlo | Qué hace |
|---|---|---|---|
| `GET` | `/api/mis-procesos` | Cliente | Historial unificado del cliente. Cotizaciones, siniestros y simulaciones AFP con el siguiente paso de cada una. |

## Reportes PDF (JasperReports)

| Método | Ruta | Quién puede usarlo | Qué hace |
|---|---|---|---|
| `GET` | `/api/reportes/jasper/{codigo}/pdf` | Administrador | Descargar un reporte PDF (JasperReports). Códigos: REP-DG-01, REP-DG-02, REP-LA-01, REP-MV-01, REP-MV-02, REP-MV-05, REP-OT-02, REP-PO-01, REP-PO-02, REP-PO-03. Código desconocido: 404. |

## Notificaciones

| Método | Ruta | Quién puede usarlo | Qué hace |
|---|---|---|---|
| `GET` | `/api/notificaciones` | Cualquier usuario autenticado | Listar mis notificaciones. Incluye el contador de no leídas. |
| `PUT` | `/api/notificaciones/{id}/leida` | Cualquier usuario autenticado | Marcar una notificación como leída. Solo las propias. |
| `PUT` | `/api/notificaciones/leidas` | Cualquier usuario autenticado | Marcar todas como leídas. Todas las del usuario en sesión. |

## Pagos (simulados)

| Método | Ruta | Quién puede usarlo | Qué hace |
|---|---|---|---|
| `GET` | `/api/pagos/datos-qr` | Cliente | Datos del QR de pago. Número y nombre del comercio para Yape/Plin (simulado). |
| `POST` | `/api/pagos` | Cliente | Pagar una contratación (simulado). Tarjeta, Yape o Plin (todo simulado). Si se aprueba, registra la contratación. Una tarjeta con número válido se aprueba, salvo si termina en 0002 (Fondos insuficientes) o en 0069 (Tarjeta vencida). |

## Mi perfil

| Método | Ruta | Quién puede usarlo | Qué hace |
|---|---|---|---|
| `GET` | `/api/usuarios/perfil` | Cualquier usuario autenticado | Ver mi perfil. Datos del usuario en sesión. |
| `PUT` | `/api/usuarios/perfil` | Cualquier usuario autenticado | Actualizar mi perfil. Cambia nombres, teléfono y dirección del usuario en sesión. |
| `PUT` | `/api/usuarios/perfil/password` | Cualquier usuario autenticado | Cambiar mi contraseña. Exige la contraseña actual y cumple la política (8+ caracteres, letras y números). |

## Admin · Roles y permisos

| Método | Ruta | Quién puede usarlo | Qué hace |
|---|---|---|---|
| `GET` | `/api/admin/roles/{idRol}/permisos` | Administrador | Ver la matriz de permisos de un rol. La matriz se guarda y administra; la autorización real es por rol (SecurityConfig). |
| `PUT` | `/api/admin/roles/{idRol}/permisos` | Administrador | Guardar la matriz de permisos de un rol. Actualiza la matriz; no cambia la autorización real, que es por rol. |
| `GET` | `/api/admin/usuarios/{idUsuario}/actividad` | Administrador | Actividad de un usuario. Acciones registradas en el historial. |
| `GET` | `/api/admin/roles` | Administrador | Listar roles. Roles del sistema. |
| `GET` | `/api/admin/roles/{idRol}` | Administrador | Ver un rol. Detalle del rol. |

## Pólizas

| Método | Ruta | Quién puede usarlo | Qué hace |
|---|---|---|---|
| `POST` | `/api/polizas` | Asesor, Administrador | Generar una póliza. Emite la póliza de una contratación y notifica al cliente. |
| `GET` | `/api/polizas/usuario/{idUsuario}` | Dueño del registro, Asesor o Administrador | Listar pólizas de un usuario. Pólizas del usuario indicado. |
| `GET` | `/api/polizas/mis-polizas` | Cliente | Listar mis pólizas. Pólizas del cliente en sesión. |
| `GET` | `/api/polizas/{id}/pdf` | Dueño del registro, Asesor o Administrador | Descargar la póliza en PDF. PDF generado con el generador propio (SimplePdf/PdfService). |

## AFP / Previsión

| Método | Ruta | Quién puede usarlo | Qué hace |
|---|---|---|---|
| `POST` | `/api/previsiones` | Cualquier usuario autenticado | Registrar una simulación AFP. Calcula y guarda la proyección de pensión. |
| `GET` | `/api/previsiones/mis-simulaciones` | Cualquier usuario autenticado | Listar mis simulaciones AFP. Simulaciones del usuario en sesión. |
| `GET` | `/api/previsiones/{idPrevision}` | Cualquier usuario autenticado | Ver una simulación AFP. Solo el dueño de la simulación. |
| `PUT` | `/api/previsiones/{idPrevision}/solicitar-orientacion` | Cliente | Solicitar orientación AFP. Notifica a los asesores. |
| `GET` | `/api/previsiones/asesor/pendientes` | Asesor, Administrador | Listar solicitudes AFP pendientes. Orientaciones por atender. |
| `PUT` | `/api/previsiones/{idPrevision}/respuesta` | Asesor, Administrador | Responder una solicitud AFP. Registra la respuesta y notifica al cliente. |

## Reportes (JSON)

| Método | Ruta | Quién puede usarlo | Qué hace |
|---|---|---|---|
| `GET` | `/api/reportes/general` | Administrador | Reporte general. Parámetros anio y mes (0 = todo el histórico / año completo). |
| `GET` | `/api/reportes/siniestros` | Administrador | Reporte de siniestros. Parámetros anio y mes. |
| `GET` | `/api/reportes/afp` | Administrador | Reporte AFP. Parámetros anio y mes. |
| `GET` | `/api/reportes/marketing/cotizaciones-estado` | Administrador | Cotizaciones por estado. Parámetros anio y mes. |
| `GET` | `/api/reportes/marketing/conversion-ventas` | Administrador | Conversión de ventas. Parámetros anio y mes. |
| `GET` | `/api/reportes/marketing/cartera-polizas` | Administrador | Cartera de pólizas. Parámetros anio y mes. |
| `GET` | `/api/reportes/logistica/documentos` | Administrador | Reporte de documentos. Parámetros anio y mes. |
| `GET` | `/api/reportes/operaciones/afp` | Administrador | Reporte operativo AFP. Parámetros anio y mes. |
| `GET` | `/api/reportes/operaciones/siniestros-aseguradora` | Administrador | Siniestros por aseguradora. Parámetros anio y mes. |
| `GET` | `/api/reportes/gerencia/indicadores` | Administrador | Indicadores de gerencia. Parámetros anio y mes. |
| `GET` | `/api/reportes/trazabilidad` | Administrador | Trazabilidad. Parámetros anio y mes. |

## Siniestros

| Método | Ruta | Quién puede usarlo | Qué hace |
|---|---|---|---|
| `POST` | `/api/siniestros` | Cualquier usuario autenticado | Registrar un siniestro. Crea el expediente en estado Registrado. |
| `GET` | `/api/siniestros/mis-siniestros` | Cualquier usuario autenticado | Listar mis siniestros. Siniestros del usuario en sesión. |
| `GET` | `/api/siniestros/{idSiniestro}` | Cualquier usuario autenticado | Ver mi siniestro. Solo el dueño del siniestro. |
| `PUT` | `/api/siniestros/{idSiniestro}/derivar` | Asesor, Administrador | Derivar a la aseguradora (ruta heredada). Versión anterior; use PUT /api/siniestros/gestion/{id}/derivar. |
| `GET` | `/api/siniestros/asesor/pendientes` | Asesor, Administrador | Listar siniestros pendientes. Siniestros por revisar. |
| `GET` | `/api/siniestros/asesor/{idSiniestro}` | Asesor, Administrador | Ver un siniestro (vista del asesor). Detalle completo para revisión. |
| `GET` | `/api/siniestros/gestion` | Asesor, Administrador | Listar siniestros para gestión. Todos los siniestros con su estado. |
| `GET` | `/api/siniestros/gestion/{id}` | Asesor, Administrador | Ver un siniestro para gestión. Detalle, estados siguientes permitidos y evidencias. |
| `POST` | `/api/siniestros/gestion/{id}/revision` | Asesor, Administrador | Iniciar revisión o solicitar información. Cambia a En revisión o Información pendiente. |
| `PUT` | `/api/siniestros/gestion/{id}/derivar` | Asesor, Administrador | Derivar el siniestro a la aseguradora. Solo desde Información completa. Estado resultante: Derivado. |
| `PUT` | `/api/siniestros/gestion/{id}/evaluacion` | Asesor, Administrador | Registrar la evaluación de la aseguradora. Dictamen, monto aprobado y fecha de evaluación. |
| `PUT` | `/api/siniestros/gestion/{id}/estado` | Asesor, Administrador | Cambiar el estado del siniestro. Valida la transición con la máquina de estados (no permite saltos). |
| `GET` | `/api/siniestros/{id}/timeline` | Dueño del registro, Asesor o Administrador | Línea de tiempo del siniestro. Historial cronológico de estados. |
| `GET` | `/api/siniestros/{id}/resumen.pdf` | Dueño del registro, Asesor o Administrador | Resumen del siniestro en PDF. PDF generado con el generador propio. |

## Admin · Usuarios

| Método | Ruta | Quién puede usarlo | Qué hace |
|---|---|---|---|
| `GET` | `/api/admin/usuarios` | Administrador | Listar usuarios. Todos los usuarios. |
| `GET` | `/api/admin/usuarios/{idUsuario}` | Administrador | Ver un usuario. Detalle del usuario. |
| `PUT` | `/api/admin/usuarios/{idUsuario}` | Administrador | Actualizar un usuario. Datos personales del usuario. |
| `PUT` | `/api/admin/usuarios/{idUsuario}/estado` | Administrador | Activar o desactivar un usuario. Estado Activo o Inactivo. |
| `PUT` | `/api/admin/usuarios/{idUsuario}/rol` | Administrador | Cambiar el rol de un usuario. Cliente, Asesor o Administrador. |

