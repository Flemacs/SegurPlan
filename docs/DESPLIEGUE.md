# Despliegue de SEGURPLAN

## Variables de entorno

| Variable | Para qué sirve | Valor recomendado |
|---|---|---|
| `DB_PASSWORD` | Clave de PostgreSQL. **Obligatoria en Docker** | Larga y propia |
| `DB_URL`, `DB_USER` | Conexión a la base | `jdbc:postgresql://db:5432/SegurPlan`, `postgres` |
| `PORT` | Puerto publicado | `8080` |
| `COOKIE_SECURE` | Marca la cookie de sesión como `Secure` | `true` con HTTPS |
| `SWAGGER_ENABLED` | Muestra Swagger UI y `/v3/api-docs` | `true` en pruebas, `false` en producción |
| `PG_BIN_DIR` | Carpeta de `pg_dump`/`psql` para respaldos | Vacío en Docker |
| `JAVA_OPTS` | Opciones de la JVM | `-Xmx512m` |

## Docker Compose

```bash
cp .env.example .env     # defina DB_PASSWORD
docker compose up --build
```

- La base **no** se publica fuera de la red de Docker.
- Volúmenes persistentes: `pgdata`, `uploads`, `backups`, `logs`.
- La aplicación corre como usuario sin privilegios y tiene *healthcheck* (puerto 8080).
- Los scripts `db/SegurPlan_schema.sql` y `db/datos_iniciales.sql` se cargan solo con la base vacía.
- Reinicio limpio: `docker compose down -v` (borra los datos).

## Publicar en internet (Render, Railway, Fly.io)

1. Cree un PostgreSQL gestionado y cargue `db/SegurPlan_schema.sql` y `db/datos_iniciales.sql`.
2. Cree un servicio web desde el repositorio (usa el `Dockerfile`).
3. Variables: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `COOKIE_SECURE=true`, `SWAGGER_ENABLED=false`.
4. Cambie las contraseñas de las cuentas de demostración desde *Mi perfil*.
5. El proveedor aporta HTTPS; con él el servidor emite además la cabecera HSTS.

## Respaldos

Respaldo automático a las 02:00, 08:00, 14:00 y 20:00 con retención de 14 días; manual desde
*Administración → Respaldo*. Detalle en `RESPALDO_Y_ARQUITECTURA.md`. Los respaldos se guardan en el volumen `backups`:
cópielos también fuera del servidor.

## Construcción

La primera construcción descarga dependencias de Maven Central y del repositorio de Jaspersoft
(`jaspersoft.jfrog.io`). El servidor de compilación necesita salida a ambos.

## Lista de verificación antes de publicar

- [ ] `DB_PASSWORD` fuerte y definida solo en el entorno (no en el repositorio)
- [ ] `COOKIE_SECURE=true` y HTTPS activo
- [ ] `SWAGGER_ENABLED=false`
- [ ] Contraseñas de demostración cambiadas
- [ ] `mvn clean verify` en verde
- [ ] Recorrido de prueba del README completo contra PostgreSQL
