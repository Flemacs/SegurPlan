# SEGURPLAN — Plataforma de seguros y AFP

Aplicación web (Spring Boot 4 · Java 21 · PostgreSQL · HTML/JS) con módulos de cotización,
pólizas, simulación AFP, siniestros, administración, reportes, notificaciones y respaldo.

> Detalle de lo implementado en la v5: [docs/CAMBIOS_V5.md](docs/CAMBIOS_V5.md)
> Arquitectura y respaldo: [docs/RESPALDO_Y_ARQUITECTURA.md](docs/RESPALDO_Y_ARQUITECTURA.md)

## Cómo levantarlo

### Opción A — Docker (recomendada para presentar al profesor)
Requiere solo Docker Desktop.

```bash
docker compose up --build
```
Abrir **http://localhost:8080**. La primera vez crea la base `SegurPlan`, las tablas y los datos iniciales.
Para empezar de cero: `docker compose down -v`.

### Opción B — Local (NetBeans / terminal)
Requiere Java 21, Maven y PostgreSQL 14+.

```bash
createdb -U postgres SegurPlan
psql -U postgres -d SegurPlan -f db/SegurPlan_schema.sql
psql -U postgres -d SegurPlan -f db/datos_iniciales.sql
mvn spring-boot:run
```
Variables opcionales: `DB_URL`, `DB_USER`, `DB_PASSWORD` (por defecto `emer`), `PORT`, `PG_BIN_DIR`.
En Windows el respaldo usa `C:/Program Files/PostgreSQL/18/bin`; cámbielo con `PG_BIN_DIR` si su versión es otra.

**Si ya tenía la base creada con la versión anterior**, no la borre: ejecute solo
`psql -U postgres -d SegurPlan -f db/migracion_v5.sql` y luego `db/datos_iniciales.sql`.

### Opción C — Publicarlo en internet (Render, Railway, Fly.io)
1. Cree un PostgreSQL gestionado y ejecute en él `db/SegurPlan_schema.sql` y `db/datos_iniciales.sql`.
2. Cree un servicio web desde este repositorio (usa el `Dockerfile`).
3. Variables: `DB_URL=jdbc:postgresql://HOST:5432/BASE`, `DB_USER`, `DB_PASSWORD`, `COOKIE_SECURE=true`.
   El proveedor aporta HTTPS y la variable `PORT`.

## Usuarios de prueba
| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | admin@segurplan.com | Admin123! |
| Asesor | asesor@segurplan.com | Asesor123! |
| Cliente | cliente@segurplan.com | Cliente123! |

Cámbielas desde **Mi perfil** antes de un uso real. Los clientes nuevos se registran desde la pantalla de registro.

## Recorrido de prueba sugerido (demostración)
1. **Cliente**: cotizar un seguro (con adjunto) → registrar un siniestro → ver la campana y la línea de tiempo.
2. **Asesor**: *Gestión de cotizaciones* (responder y registrar comunicación) y *Gestión de siniestros*
   (iniciar revisión → solicitar información → aprobar → derivar → evaluar → cerrar).
3. **Cliente**: aceptar la cotización y pagar (simulado) → el asesor genera la póliza → el cliente la descarga en PDF.
4. **Administrador**: usuarios, asesores (con especialidad), *Roles y permisos* (matriz), historial, respaldo, catálogos.
5. **Administrador**: también consulta los reportes y los exporta a PDF/Excel.

## Pruebas y CI
`mvn test` (pruebas unitarias). `.github/workflows/ci.yml` ejecuta compilación y pruebas con PostgreSQL en cada *push*.
