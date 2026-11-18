# Superficie de ataque (revision de codigo)

| Activo | Ruta/servicio | Control a verificar |
|---|---|---|
| Autenticacion | POST /api/usuarios/login | credenciales, bloqueo, CSRF exento por diseno |
| Registro | POST /api/usuarios/registro | validacion y abuso, CSRF exento por diseno |
| Sesion | /api/usuarios/logout | invalidacion de sesion y cookie |
| Token | GET /api/csrf | emision de token |
| Administracion | /api/admin/** | ROLE_Administrador |
| Polizas | GET /api/polizas/mis-polizas | ROLE_Cliente y propiedad del recurso |
| Pagos | /api/pagos/** | ROLE_Cliente |
| Cotizaciones | GET /api/cotizaciones | ROLE_Asesor / ROLE_Administrador |
| Siniestros | /api/siniestros/asesor/** | ROLE_Asesor / ROLE_Administrador |
| Reportes | /api/reportes/** | ROLE_Administrador |
| API docs | /swagger-ui/** | revisar activacion en entorno publico |
| PostgreSQL | JDBC / JPA | acceso restringido, secretos fuera de git |

Observacion de codigo: la CSP contiene `script-src 'self' 'unsafe-inline'` y `object-src 'self' blob:`; evaluar endurecimiento compatible con el frontend. No se afirma explotabilidad sin verificacion dinamica.
