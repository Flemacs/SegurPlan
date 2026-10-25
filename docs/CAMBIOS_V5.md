# Cambios de la versión 5

Resumen de lo implementado a partir del análisis del documento "Avance de Proyecto Final 2".

## 1. Para poder levantarlo
- `db/SegurPlan_schema.sql`: instalación completa y limpia (tabla `pago` ya no duplicada).
- `db/migracion_v5.sql`: migración idempotente para bases ya creadas.
- `db/datos_iniciales.sql`: roles (Cliente, Asesor, Administrador), 3 usuarios de prueba,
  14 tipos de seguro, 7 aseguradoras y matriz de permisos por defecto.
- `Dockerfile`, `docker-compose.yml`, `.env.example`, `.dockerignore`.
- `application.properties` configurable por variables de entorno (BD, puerto, ruta de `pg_dump`, cookies seguras, SSL).
- `.github/workflows/ci.yml` (RNF08).

## 2. Seguridad
- `/api/usuarios/admin/**` ahora exige rol Administrador (antes solo sesión).
- Cotizaciones: listado general solo para personal; responder solo Asesor/Administrador; ver una cotización o
  las de un usuario solo el dueño o el personal; el solicitante es siempre el usuario autenticado.
- Pólizas de un usuario: solo el dueño o el personal.
- Las páginas sin sesión redirigen al login y la API responde 401 (antes 403).
- Política de contraseñas (mínimo 8, letras y números) en registro, asesores y cambio de contraseña.
- Cookies `HttpOnly` + `SameSite`, caducidad de sesión de 30 min, soporte de HTTPS (proxy o SSL directo).
- Historial inmutable: un *trigger* de PostgreSQL impide `UPDATE`/`DELETE` (RF033).
- Eliminado el usuario demo con contraseña en texto plano que guardaba `app.js` en `localStorage`.
- Política de privacidad (Ley 29733) accesible sin iniciar sesión.

## 3. Requisitos funcionales
| RF | Implementación |
|---|---|
| RF003 | Pantalla **Mi perfil** (`s27`): datos, dirección y cambio de contraseña con verificación de la actual |
| RF004 | Datos del formulario dinámico guardados (`datos_json`) y adjuntos opcionales (PDF/JPG/PNG, 10 MB) |
| RF005 | Pantalla **Gestión de cotizaciones** (`s29`): respuesta, estados y registro de comunicaciones con la aseguradora |
| RF006 / RF008 | PDF de la cotización y de la póliza |
| RF008 | El Asesor también puede generar pólizas |
| RF009 | Límite de 10 MB para fotos y documentos; descarga/visualización segura de archivos |
| RF011 | Validar / observar / rechazar documentos con motivo y aviso al cliente |
| RF022 a RF026 | Pantalla **Gestión de siniestros** (`s28`): revisión, solicitud de información, aprobar para derivación, derivar, evaluación (dictamen + monto + fecha), cierre |
| RF024 | Resumen del caso en PDF |
| RF027 / RF033 | Línea de tiempo del siniestro para cliente y asesor (también en `s12`) |
| RF029 | Área de especialidad (Ventas, Siniestros, AFP) y consulta de actividad: `GET /api/admin/usuarios/{id}/actividad` |
| RF030 | Matriz de permisos editable en *Roles y permisos* |
| RF031 | Reportes con exportación a PDF y Excel. **El rol Gerente se unificó con Administrador**: el administrador accede a todo |
| RF032 | Notificaciones internas (campana con contador y marcado como leído) |
| RNF04 | Ver sección Seguridad |
| RNF06 | Navegación por rol, foco visible y etiquetas ARIA en lo nuevo |
| RNF09 | Respaldo automático cada 6 horas (02:00, 08:00, 14:00, 20:00) |
| RNF11 | Máquina de estados del siniestro (`FlujoSiniestro`): no se permiten saltos ilógicos |

## 4. Estados: documento ↔ código
| Documento | Código |
|---|---|
| Pendiente de revisión | Solicitada |
| En gestión | En revisión |
| Cotización recibida | Disponible |
| En proceso de contratación / Contratado | Aceptada (+ contratación y póliza) |
| Siniestro: Información completa | Información completa |
| Siniestro: Evaluación recibida | Evaluación recibida |

Sugerencia: usar esta tabla también en el documento para que ambos coincidan.

## 5. Límites conocidos (para decirlo con honestidad en la defensa)
- **Los permisos de la matriz se guardan y se administran, pero la autorización real sigue siendo por rol**
  (`SecurityConfig`). No se aplican celda por celda.
- **Exportación de reportes**: Excel se genera como archivo `.xls` (HTML que Excel abre) y el PDF usa el diálogo
  de impresión del navegador ("Guardar como PDF"). Los PDF de cotización, póliza y siniestro sí se generan en el servidor.
- **Respaldos**: cada 6 horas son *completos*; PostgreSQL no hace incrementales con `pg_dump`. El RPO real es 6 h, no 1 h.
- **Video de evidencia**: se mantiene el tope técnico de 100 MB solo para videos de siniestro.
- **Pago**: sigue siendo simulado (fuera del alcance del documento).
- Lo nuevo se probó con compilación, pruebas de lógica y generación real de PDF; **no se pudo ejecutar contra PostgreSQL
  en el entorno de desarrollo**, así que conviene hacer el recorrido de prueba del README una vez antes de presentar.

## 6. Decisión de diseño: Gerente = Administrador
Se eliminó el rol Gerente y sus funciones (reportes e indicadores) pasaron al Administrador. `migracion_v5.sql`
reasigna a Administrador a cualquier usuario que tuviera el rol Gerente y borra el rol. Actualizar el documento
(RF030, RF031 y la tabla de roles) para que coincida. Contra: se pierde un perfil de solo lectura para gerencia.

## 7. Pantallas separadas por función
Cada módulo tiene su propia pantalla; los paneles son solo menús:
- `s14` Administración: menú de módulos (ya no incluye la lista de cotizaciones ni el panel de revisión).
- `s13` Panel del asesor: menú con contadores (cotizaciones pendientes, siniestros por revisar, AFP pendientes).
- `s29` Gestión de cotizaciones · `s28` Gestión de siniestros · `s30` Solicitudes AFP (atención de orientaciones).
- Corregido: las tarjetas "Cotizaciones" y "Documentos" del asesor apuntaban a pantallas del cliente.
- Seguridad: `/api/contrataciones/**` ahora es solo para Asesor/Administrador (antes cualquier usuario con sesión
  podía listar las contrataciones de todos). El asesor también puede abrir "Generar póliza".

## 8. Diseño responsivo (celulares y tablets)
- Auditoría con navegador en 375 px (celular), 320 px (celular pequeño) y 768 px (tablet): **42 de 42 pantallas sin
  desbordamiento horizontal** (antes: 26 de 42 se desbordaban en celular).
- Reglas globales en `assets/sp-extras.css`: el encabezado pasa a dos filas, el menú por rol se desplaza horizontalmente,
  las tablas se desplazan dentro de su tarjeta, los botones tienen áreas táctiles de 40 px y los campos usan 16 px
  (evita el zoom automático de iPhone).
- En celular el nombre del usuario del encabezado se oculta para ahorrar espacio.
- Probado con Chromium emulando pantalla táctil; **conviene abrirlo también en un celular real** (misma red Wi-Fi:
  `http://IP-DE-LA-PC:8080`).
