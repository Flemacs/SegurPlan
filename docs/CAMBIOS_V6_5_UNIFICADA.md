# SEGURPLAN v6.5 unificada: informe de cambios

## 1. Resumen

El proyecto tenía dos ramas que no eran una continuación de la otra:

- **v5** (`SegurPlan.zip`): funciones, seguridad y despliegue.
- **v6.5** (`segurplan_v6_5.zip`): interfaz pulida, reportes con JasperReports y 278 pruebas.

La versión unificada usa **v5 como base del backend** y **v6.5 como interfaz**, y le suma a v5 lo que solo existía en v6.5.
Además corrige fallos de seguridad encontrados al revisar el código, documenta la API con Swagger/OpenAPI y mejora el despliegue.

| Resultado | Cifra |
|---|---|
| Endpoints documentados en Swagger | 102 (20 grupos) |
| Pantallas con la interfaz de v6.5 | 43 |
| Reportes JasperReports | 10 |
| Pruebas | 302 en 39 archivos |
| Archivos Java nuevos / modificados respecto a v5 | 5 / 27 |

> **Estado de verificación.** El entorno donde se hizo la unificación no tiene Maven Central ni `javac`, así que
> **no se compiló ni se ejecutó ninguna prueba**. Se hizo revisión estática (sintaxis y firmas), validación del
> `openapi.yaml` con un validador oficial, validación de YAML/XML/JS y revisión visual de 88 capturas.
> Ejecute `mvn clean verify` y el recorrido de prueba del README antes de entregar.

## 2. De dónde viene cada pieza

| Componente | Origen | Motivo |
|---|---|---|
| Lógica de negocio, flujo de siniestros, cotizaciones, notificaciones, perfil, respaldo | v5 | v6.5 no las tiene |
| `SecurityConfig` | v5, ampliado | v6.5 dejaba rutas abiertas a cualquier usuario con sesión |
| Constructores y validaciones (contraseña de 8 caracteres, catálogos activos, 10 MB) | v5 | Cumplen los requisitos; son el comportamiento correcto |
| Interfaz: portada, estilos `pro.css`/`landing.css`, imágenes | v6.5 | Pedido expreso; es la interfaz más completa |
| Menús de módulos y pantallas de gestión (`s13`, `s14`, `s27` a `s30`) | v5, con la interfaz de v6.5 | v6.5 mezclaba todo en un archivo de 40 KB |
| JasperReports, `ReporteService` con "todo el histórico", "Mis procesos" | v6.5 | Solo existían allí |
| PDF propio (`SimplePdf`/`PdfService`) | v5 | Se conserva junto a Jasper |
| Dockerfile, compose, CI, scripts SQL, scripts de respaldo | v5, mejorados | Solo existían en v5 |
| 278 pruebas | v6.5, adaptadas | Pedido expreso |
| 11 pruebas (`FlujoSiniestroTest`, `UtilidadesTest`) | v5 | Cubren el flujo de siniestros |

## 3. Cambios en el backend

### 3.1 Traído desde v6.5
- `JasperReportController` y `JasperReportService`: `GET /api/reportes/jasper/{codigo}/pdf` (solo Administrador).
- 10 plantillas `.jrxml` en `src/main/resources/reports`. Sus consultas usan solo tablas que existen en el esquema de v5.
- `ReporteService` de v6.5: admite `anio=0` y `mes=0` ("todo el histórico"). v5 solo difería en esa función.
- `HistorialClienteController`: `GET /api/mis-procesos` (cotizaciones, siniestros y AFP del cliente con el siguiente paso). Sus estados coinciden con los de v5.

### 3.2 `pom.xml`
- Nombre de artefacto unificado: `segurplan`, versión `6.5.0` (antes `segurplan_v4.2` en una rama y `segurplan_v3` en la otra).
- Dependencias nuevas: `jasperreports`, `jasperreports-pdf`, `jasperreports-jdt` (7.0.8) y `springdoc-openapi-starter-webmvc-ui` (3.0.3).
- Repositorio de Jaspersoft para sus artefactos de terceros.

### 3.3 Correcciones
- `DocumentoController`: la subida de evidencias lanzaba `NullPointerException` si el documento no traía usuario. Ahora lo comprueba.
- `CotizacionController` y `Usuario`: ver sección 4.

### 3.4 Decisiones donde las ramas diferían
| Tema | v5 | v6.5 | Se mantuvo |
|---|---|---|---|
| Tamaño máximo de documentos | 10 MB | 20 MB | **10 MB** (requisito RF009) |
| Contraseña mínima | 8 con letras y números | 6 | **v5** |
| Catálogos en formularios | Solo activos | Todos | **v5** |
| Derivar siniestro | Máquina de estados | Directo | **v5** |
| Celular y nombre de comercio del QR | `935953460`, "SEGURPLAN S.A.C." | `999888777`, "(ficticio)" | **v5** (revisar: parece un número real) |

## 4. Seguridad

Detalle completo en `docs/SEGURIDAD.md`.

### Fallos encontrados y corregidos
1. **Cotizaciones de otros usuarios.** `GET /api/cotizaciones/{id}` y `/usuario/{id}` no comprobaban al dueño.
   `CotizacionController` tenía inyectado `AccesoService` y un método `prohibido()` que nunca se usaban.
   Ahora se verifica con `acceso.puedeVer(...)` y se responde 403.
2. **Hash de contraseña en JSON.** Esas respuestas devolvían la entidad `Cotizacion` con su `Usuario` completo, incluido el hash BCrypt.
   `Usuario.password` es ahora `WRITE_ONLY` y no se serializa.

### Medidas añadidas
- **Bloqueo por intentos fallidos** (`LoginAttemptService`): 5 fallos por correo o 20 por IP → 15 minutos (HTTP 429 con `Retry-After`).
- **Cabeceras de seguridad:** CSP, `X-Frame-Options: DENY`, `Referrer-Policy`, `Permissions-Policy` y HSTS.
- **Reglas por ruta:** `/api/mis-procesos` solo Cliente; Swagger visible solo si `SWAGGER_ENABLED=true`.
- **Errores:** `server.error.include-message/stacktrace/binding-errors=never`.
- **Contenedor y despliegue:** usuario sin privilegios, `DB_PASSWORD` obligatoria, base sin puerto publicado.

### Se mantuvo de v5
Roles por ruta, comprobación de dueño en siniestros/documentos/pólizas/AFP, CSRF, cookie `HttpOnly`/`SameSite`, sesión de 30 min,
BCrypt, historial inmutable y validación de archivos.

## 5. Swagger / OpenAPI

- **Swagger UI:** `/swagger-ui.html`. **Definición:** `/v3/api-docs` (JSON) y `/v3/api-docs.yaml`.
- `OpenApiConfig`: descripción con la guía de uso, esquema `sesion` (cookie `JSESSIONID`) y esquema `csrf` (cabecera `X-CSRF-TOKEN`).
- 102 anotaciones `@Operation` con resumen, roles que pueden usarla y regla de negocio relevante; 20 etiquetas (Autenticación, Cotizaciones, Siniestros, Admin · Usuarios, etc.).
- Las respuestas 401 y 403 se agregan automáticamente a toda operación protegida. Login, registro y CSRF se marcan como públicos.
- Documentos estáticos generados desde el código: `docs/openapi.yaml` (validado como OpenAPI 3.0, importable en swagger.io) y `docs/API_ENDPOINTS.md`.
- Rutas heredadas duplicadas (`/api/usuarios/admin/**`, derivación antigua) se conservan y se rotulan como tales.

## 6. Interfaz

- **Capa visual de v6.5** aplicada a las 43 pantallas: tipografías, `pro.css`, banner de cada módulo y estilo de tarjetas. La portada es la de v6.5, con enlace nuevo a la política de privacidad.
- **Pantallas solo de v5** (`s13`, `s14`, `s27`, `s28`, `s29`, `s30`, privacidad) recibieron la misma capa y un banner propio.
- **`s31-mis-procesos`**: la pantalla de v6.5 pasó de `s28` a `s31` (en v5 `s28` es *Gestión de siniestros*), con la cabecera estándar. El panel del cliente (`s02`) ganó la tarjeta *Historial*.
- **Cabecera uniforme:** se eliminó el prefijo "Usuario:" en 13 pantallas y `s24-historial`, que tenía estructura propia, ahora usa la cabecera estándar.
- **Reportes:** `app.js` recupera el botón *DESCARGAR PDF (JASPER)* y el filtro *Todo el histórico*. El botón de impresión de v5 se renombró *IMPRIMIR / GUARDAR PDF* para no confundirlo.
- Se conservó de v5: el comportamiento responsivo (`sp-extras.css/js`), la campana de notificaciones, el menú por rol y los roles actualizados en `app.js`; se descartó el usuario de demostración que v6.5 sembraba en el navegador.
- Revisión: 88 capturas (escritorio 1280 px y móvil 390 px) con la sesión y la API simuladas. Sin desbordes horizontales ni errores de JavaScript. No se vieron con datos reales.

## 7. Pruebas

- 302 pruebas: 278 de v6.5, 11 de v5 y 13 nuevas. Detalle y adaptaciones en `docs/PRUEBAS.md`.
- **Adaptaciones por cambios de v5:** constructores de 6 controladores, comprobación de dueño, catálogos activos y derivación vía máquina de estados.
- **Nuevas:** `LoginAttemptServiceTest` (7), `UsuarioControllerLoginBloqueoTest` (3) y 3 casos 403 en `CotizacionControllerTest` y `PolizaControllerTest`.
- 7 archivos renombrados y 2 movidos; se eliminó una clase vacía (`ReporteServiceTest/java.java`).
- Un verificador estático comparó cada llamada de las pruebas contra las firmas del código: sin incompatibilidades reales pendientes. No detecta tipos de argumento ni de retorno.

## 8. Despliegue y repositorio

- **Dockerfile:** ejecución sin root, `HEALTHCHECK`, `SWAGGER_ENABLED`.
- **docker-compose.yml:** `DB_PASSWORD` obligatoria desde `.env`, `restart: unless-stopped`, variable `SWAGGER_ENABLED`, base sin puerto expuesto.
- **`.env.example`**, `.dockerignore` y `.gitignore` actualizados (`.env` y `uploads/*` fuera del repositorio).
- **CI:** conserva compilación y pruebas con PostgreSQL; añade subida de informes y un trabajo que construye la imagen.
- Se excluyeron del paquete `target/`, `logs/`, `backups/`, `uploads/` con datos, `.git` y la copia anidada `Documents/NetBeansProjects/segurplan_v6`.
- Se retiró `endpoints_segurplan.txt` (desactualizado); lo reemplaza `docs/API_ENDPOINTS.md`.
- Se eliminó `static/segurplan.html` (portada antigua en blanco y negro, sin enlaces) y `static/privacidad.html` ahora redirige a `pages/privacidad.html`, para que exista una sola interfaz.

## 9. Riesgos y pendientes

| Pendiente | Qué hacer |
|---|---|
| Compilación y pruebas sin ejecutar | `mvn clean verify` con PostgreSQL |
| `springdoc` 3.0.3 y APIs de cabeceras de Spring Security 7 con Boot 4.1.1 | Confirmar al compilar; si falla, ajustar versión o llamada |
| Repositorio de Jaspersoft | La compilación necesita salida a `jaspersoft.jfrog.io` |
| Número de comercio del QR (`935953460`) | Confirmar que sea el del equipo o cambiarlo |
| Contraseña por defecto `emer` en `application.properties` | Solo local; en servidores usar variables |
| Cuentas de demostración | Cambiar contraseñas antes de publicar |
| Rutas por rol y cabeceras sin prueba automática | Añadir pruebas de integración con MockMvc |
| Fotos de `assets/img` | Verificar que tengan licencia de uso |
| Matriz de permisos | Hoy no gobierna la autorización; decidir si debe hacerlo |
