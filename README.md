# SEGURPLAN — Plataforma de seguros y AFP

Aplicación web (Spring Boot 4 · Java 21 · PostgreSQL · HTML/CSS/JS) con cotización de seguros, pólizas,
simulación AFP, siniestros, administración, reportes, notificaciones y respaldo.

**Versión 6.5 unificada:** backend y seguridad de la línea v5, interfaz y reportes de la línea v6.5.

## Documentación

| Documento | Contenido |
|---|---|
| [docs/CAMBIOS_V6_5_UNIFICADA.md](docs/CAMBIOS_V6_5_UNIFICADA.md) | Qué se unificó, qué cambió y por qué (también en PDF) |
| [docs/API_ENDPOINTS.md](docs/API_ENDPOINTS.md) | Los 102 endpoints con rol y descripción |
| [docs/openapi.yaml](docs/openapi.yaml) | Definición OpenAPI 3 para importar en [swagger.io](https://editor.swagger.io) |
| [docs/SEGURIDAD.md](docs/SEGURIDAD.md) | Medidas de seguridad, límites y lista de pendientes |
| [docs/DESPLIEGUE.md](docs/DESPLIEGUE.md) | Docker, instalación local, publicación y variables |
| [docs/PRUEBAS.md](docs/PRUEBAS.md) | Cómo ejecutar las 302 pruebas y qué cubren |
| [docs/RESPALDO_Y_ARQUITECTURA.md](docs/RESPALDO_Y_ARQUITECTURA.md) | Arquitectura y respaldo de la base |
| [docs/CAMBIOS_V5.md](docs/CAMBIOS_V5.md) | Historial de la línea v5 |

## Cómo levantarlo

### Opción A — Docker (recomendada)
Requiere solo Docker.

```bash
cp .env.example .env        # edite DB_PASSWORD (es obligatoria)
docker compose up --build
```
Abrir **http://localhost:8080**. La primera vez crea la base, las tablas y los datos iniciales.
Para empezar de cero: `docker compose down -v`.

### Opción B — Local
Requiere Java 21, Maven y PostgreSQL 14+.

```bash
createdb -U postgres SegurPlan
psql -U postgres -d SegurPlan -f db/SegurPlan_schema.sql
psql -U postgres -d SegurPlan -f db/datos_iniciales.sql
mvn spring-boot:run
```
Variables opcionales: `DB_URL`, `DB_USER`, `DB_PASSWORD` (en local el valor por defecto es `emer`;
**no lo use fuera de su equipo**), `PORT`, `PG_BIN_DIR`, `COOKIE_SECURE`, `SWAGGER_ENABLED`.
Si ya tiene la base de la versión anterior, no la borre: ejecute `db/migracion_v5.sql` y luego `db/datos_iniciales.sql`.

## Probar los endpoints con Swagger

Con el sistema en marcha abra **http://localhost:8080/swagger-ui.html** (definición: `/v3/api-docs`).

1. `GET /api/csrf` → copie el valor de `token`.
2. `POST /api/usuarios/login` con un usuario de prueba (la cookie de sesión queda en el navegador).
3. Pulse **Authorize**, pegue el token en `csrf` y confirme.
4. Ejecute cualquier endpoint. `POST`, `PUT` y `DELETE` envían el token en `X-CSRF-TOKEN`.
5. Si vuelve a iniciar sesión, pida un token nuevo.

Sin levantar el sistema: importe `docs/openapi.yaml` en https://editor.swagger.io para leer la API.
En producción use `SWAGGER_ENABLED=false`.

## Usuarios de prueba

| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | admin@segurplan.com | Admin123! |
| Asesor | asesor@segurplan.com | Asesor123! |
| Cliente | cliente@segurplan.com | Cliente123! |

Son cuentas de demostración: **cámbielas desde Mi perfil antes de un uso real.**

## Reportes y PDF

- **PDF propio** (`SimplePdf`/`PdfService`): cotización, póliza y resumen de siniestro.
- **JasperReports 7.0.8:** 10 reportes con plantillas `.jrxml` (`src/main/resources/reports`).
  Endpoint `GET /api/reportes/jasper/{codigo}/pdf` (solo Administrador). En cada pantalla de reporte aparece
  el botón *DESCARGAR PDF (JASPER)*. El botón *IMPRIMIR / GUARDAR PDF* usa el diálogo del navegador.

## Recorrido de prueba sugerido

1. **Cliente:** cotizar (con adjunto) → registrar un siniestro → campana y línea de tiempo → *Mis procesos*.
2. **Asesor:** *Gestión de cotizaciones* y *Gestión de siniestros* (revisión → información → derivación → evaluación).
3. **Cliente:** aceptar la cotización y pagar (simulado) → el asesor genera la póliza → el cliente la descarga en PDF.
4. **Administrador:** usuarios, asesores, roles y permisos, historial, respaldo, catálogos y reportes.

## Pruebas y CI

```bash
mvn clean verify        # requiere PostgreSQL con el esquema cargado (una prueba arranca la aplicación)
```
`.github/workflows/ci.yml` compila, ejecuta las pruebas con PostgreSQL y construye la imagen Docker.
