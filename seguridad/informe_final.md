# Informe de seguridad - SegurPlan v6.5

**Estado:** implementacion de pruebas y preparacion de herramientas; escaneos dinamicos y mediciones externas PENDIENTES.

## Resumen ejecutivo
Se incorporan pruebas de regresion para hash BCrypt y bloqueo de fuerza bruta, inventario de endpoints, plantillas de trazabilidad, evidencias, fuzzing limitado, carga JMeter, pipeline y documentacion de mitigacion. No se han demostrado vulnerabilidades explotables ni CVE especificas con la informacion disponible.

## Riesgos de revision estatica (requieren validacion)
1. CSP permite `unsafe-inline` para scripts; estudiar migracion a nonce/hash y verificar compatibilidad.
2. Swagger habilitado por defecto segun `springdoc.api-docs.enabled:true`; deshabilitar en produccion y comprobar.
3. Control de intentos en memoria no distribuido; evaluar Redis y limites de solicitudes en despliegues multiinstancia.
4. HSTS solo protege conexiones HTTPS; desplegar tras proxy TLS configurado.

## Evidencia y resultados
- JUnit: ejecutar en NetBeans/Maven y anexar XML de Surefire.
- SCA: pendiente de obtener CVE reales.
- ZAP/Burp/Nmap/Nikto/sqlmap: pendiente de laboratorio.
- Fuzzing/JMeter: pendientes de ejecucion y medidas p95/p99.

## Roadmap
P0: confirmar control de acceso, CSRF, secretos y dependencias criticas. P1: endurecer CSP/Swagger, limites de peticiones. P2: automatizar escaneos de staging y revalidar en cada release.

## Criterio de cierre
Registrar severidad, solicitud/respuesta anonimizada, reproduccion, mitigacion, responsable y prueba de revalidacion por cada hallazgo. No marcar como corregido sin evidencia.
