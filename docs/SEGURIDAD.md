# Seguridad de SEGURPLAN

## Medidas implementadas

| Área | Medida | Dónde |
|---|---|---|
| Contraseñas | BCrypt; política de 8+ caracteres con letras y números | `SecurityConfig`, `PasswordPolicy` |
| Sesión | Cookie `HttpOnly`, `SameSite=Lax`, `Secure` con `COOKIE_SECURE=true`, vence a los 30 min | `application.properties` |
| CSRF | Activo. `POST`, `PUT` y `DELETE` exigen `X-CSRF-TOKEN` (de `GET /api/csrf`) | `SecurityConfig`, `CsrfController` |
| Roles | `Cliente`, `Asesor`, `Administrador` por ruta; el resto exige sesión | `SecurityConfig` |
| Dueño del dato | Siniestros, cotizaciones, pólizas, documentos y simulaciones AFP solo para su dueño o el personal | `AccesoService` y controladores |
| Fuerza bruta | 5 fallos por correo o 20 por IP bloquean 15 min (HTTP 429, `Retry-After`) | `LoginAttemptService` |
| Cabeceras | CSP, `X-Frame-Options: DENY`, `Referrer-Policy`, `Permissions-Policy`, HSTS (solo sobre HTTPS) | `SecurityConfig` |
| Errores | Sin mensajes internos ni trazas hacia el cliente | `application.properties`, `ManejadorErroresV5` |
| Datos sensibles | El hash de contraseña nunca sale en JSON; no se guarda el número completo de tarjeta ni el CVV | `Usuario`, `PagoService` |
| Auditoría | Historial inmutable: un *trigger* de PostgreSQL impide `UPDATE` y `DELETE` | `db/SegurPlan_schema.sql` |
| Archivos | Tipo y tamaño validados (10 MB documentos e imágenes, 100 MB video) | `DocumentoService` |
| Documentación API | Swagger solo si `SWAGGER_ENABLED=true` | `SecurityConfig`, `OpenApiConfig` |
| Contenedor | Usuario sin privilegios; base de datos sin puerto publicado; `DB_PASSWORD` obligatoria | `Dockerfile`, `docker-compose.yml` |

## Correcciones de esta versión

1. **Cotizaciones ajenas.** `GET /api/cotizaciones/{id}` y `GET /api/cotizaciones/usuario/{id}` no comprobaban al dueño:
   cualquier usuario con sesión podía leerlas. Ahora responden 403 si el recurso es de otro cliente.
2. **Hash de contraseña expuesto.** Esas respuestas devolvían la entidad completa, con el hash del usuario.
   `Usuario.password` pasó a ser solo de escritura en JSON.
3. **Fuerza bruta.** El login no tenía límite de intentos.
4. **Cabeceras de seguridad** ausentes.
5. **`/api/mis-procesos`** queda restringido al rol Cliente.

## Cómo se prueba cada regla

- Dueño del dato: `CotizacionControllerTest` y `PolizaControllerTest` (casos 403).
- Bloqueo de login: `LoginAttemptServiceTest` y `UsuarioControllerLoginBloqueoTest`.
- Rutas por rol y cabeceras: **no hay prueba automática**. Se comprueban a mano con Swagger o `curl`
  (sin sesión debe dar 401; con rol insuficiente, 403).

## Límites conocidos y pendientes

- El bloqueo de login vive en memoria: se reinicia con la aplicación y no se comparte entre instancias.
- La CSP admite `blob:` en `object-src` y `media-src` para ver evidencias abiertas desde el navegador; conviene comprobar a mano que el visor de PDF funcione en su navegador.
- La CSP permite `'unsafe-inline'` en scripts y estilos porque las páginas usan código en línea. Reducirlo exige mover ese código a archivos.
- La matriz de *Roles y permisos* se guarda y se edita, pero **no gobierna la autorización real**, que es por rol en `SecurityConfig`.
- `application.properties` conserva `emer` como contraseña de BD por defecto para uso local. No usar en servidores.
- Las cuentas de demostración tienen contraseñas públicas (README). Cambiarlas antes de publicar.
- El pago es simulado; no procesa dinero real ni cumple PCI DSS.
- Existen rutas heredadas duplicadas (`/api/usuarios/admin/**`, `PUT /api/siniestros/{id}/derivar`) por compatibilidad. Ambas exigen rol.
- Las clases de seguridad se escribieron sin poder compilarse en el entorno de preparación; verificar con `mvn clean verify`.
