# SegurPlan — Respaldo, base de datos y arquitectura

## 1. Arquitectura (frontend + backend + persistencia)

| Capa | Tecnología | Dónde está |
|------|-----------|------------|
| Frontend | HTML + CSS + JavaScript (fetch + token CSRF) | `src/main/resources/static/` (37 pantallas en `pages/`) |
| Backend | Spring Boot 4.1.1, Java 21, Spring Security | `src/main/java/com/segurplan/` → `controller`, `service`, `dto`, `config` |
| Persistencia | Spring Data JPA / Hibernate + PostgreSQL | `model` (12 @Entity), `repository` (12 JpaRepository), `db/SegurPlan_completo.sql` |

Recorrido: Navegador → Controller → Service → Repository → PostgreSQL.

## 2. Configuración de la base de datos

1. Crear la base y cargar el script único (esquema + datos iniciales + datos de prueba):
   ```
   createdb -U postgres SegurPlan
   psql -U postgres -d SegurPlan -v ON_ERROR_STOP=1 -f db/SegurPlan_completo.sql
   ```
   El script se puede volver a ejecutar: borra y recrea las tablas (se pierden los datos actuales).
   Cuentas de prueba: `admin@segurplan.com / Admin123!`, `asesor@segurplan.com / Asesor123!`,
   `cliente@segurplan.com / Cliente123!`. Para un entorno limpio, elimine la sección
   «4) DATOS DE PRUEBA» del propio script (queda solo esquema, catálogos y el administrador).
2. Conexión en `src/main/resources/application.properties`
   (`spring.datasource.*`). La clave se puede definir con la variable de entorno `DB_PASSWORD`.
3. `spring.jpa.hibernate.ddl-auto=none`: el esquema lo gobierna el script SQL.

## 3. Respaldo (backup) y restauración

### Desde la aplicación (rol Administrador)
Panel de administración → **Respaldo de base de datos** (`/pages/s25-respaldo.html`):
crear, descargar, restaurar y eliminar. Antes de restaurar se crea un respaldo de
seguridad (`segurplan_pre_*.sql`).

API: `GET/POST /api/admin/backups`, `GET /api/admin/backups/{archivo}/descargar`,
`POST /api/admin/backups/{archivo}/restaurar`, `DELETE /api/admin/backups/{archivo}`.

### Automático
Todos los días a las 02:00 (`segurplan.backup.cron`). Los respaldos automáticos
(`segurplan_auto_*.sql`) se conservan 30 días (`segurplan.backup.retention-days`).

### Por consola
```
DB_PASSWORD=clave ./scripts/backup.sh
DB_PASSWORD=clave ./scripts/restore.sh backups/segurplan_AAAAMMDD_HHMMSS.sql
```
En Windows: `scripts\backup.bat` y `scripts\restore.bat`.

### Requisitos
`pg_dump` y `psql` deben estar en el PATH o indicarse en
`segurplan.backup.pg-bin-dir` (ej. `C:/Program Files/PostgreSQL/16/bin`).

## 4. Mantenimientos (solo Administrador)

| Mantenimiento | Controller / Service | Pantalla |
|---|---|---|
| Usuarios | UsuarioAdminController / UsuarioService | s21-usuarios |
| Asesores | AsesorAdminController / UsuarioService | s22-asesores |
| Roles y permisos (consulta) | RolAdminController / RolAdminService | s23-roles-permisos |
| Aseguradoras y tipos de seguro | CatalogoAdminController / CatalogoAdminService | s26-catalogos |
| Respaldo de la BD | BackupAdminController / BackupService | s25-respaldo |

## 5. Logs (Logback)

Configuración en `src/main/resources/logback-spring.xml` (Spring Boot la detecta sola).

| Aspecto | Valor |
|---|---|
| Fachada / implementación | SLF4J + Logback (`spring-boot-starter-logging`) |
| Salidas | Consola y archivo `logs/segurplan.log` |
| Rotación | Diaria y por tamaño (10 MB), archivos `.gz` |
| Retención | 30 días, tope total de 500 MB |
| Rendimiento | Escritura a archivo asíncrona (`AsyncAppender`) |
| Carpeta | Variable de entorno `LOG_DIR` (por defecto `./logs`, excluida de Git) |
| SQL de Hibernate | Apagado; se activa con `SHOW_SQL=true` |

En el código se usa `LoggerFactory.getLogger(...)` (controladores de siniestros y documentos,
`DocumentoService`, `BackupService`). Se registran solo metadatos (por ejemplo siniestro, tipo y
tamaño de una evidencia), nunca contraseñas ni el contenido de los archivos.

## 6. Pruebas unitarias

`src/test/java/com/segurplan/service/`: `PolizaServiceTest` (6 pruebas de reglas de negocio) y
`DocumentoServiceTest` (4 pruebas de validación de evidencias). Usan Mockito para simular los
repositorios (DAO), por lo que no necesitan base de datos:

```
mvn -Dtest=PolizaServiceTest,DocumentoServiceTest test
```

`SegurplanApplicationTests.contextLoads()` sí requiere PostgreSQL en ejecución.
