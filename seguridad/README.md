# Plan de pruebas de seguridad SegurPlan v6.5 — Semana 14

Referencia: S14_s27 CI-I PruebasSeguridad.pdf, paginas 11-43.

## Alcance y autorizacion
Solo localhost, entorno de pruebas propio y datos ficticios. No ejecutar escaneos activos, sqlmap ni carga contra produccion o terceros. Realizar copia de la base de datos y limitar la concurrencia antes de empezar.

## S1 - Planificacion y SCA
- Ejecutar `mvn dependency:tree > seguridad/evidencias/dependency-tree.txt`.
- Activar Dependabot en GitHub y revisar las alertas; completar `dependencias.csv` con CVE verificadas, sin inventar hallazgos.
- Revisar roles, configuracion, secretos, HTTPS y endpoints. Inventario en `superficie_ataque.md`.

## S2 - Reconocimiento
- Revisar `superficie_ataque.md`; ejecutar solo contra `127.0.0.1` con herramientas instaladas: `nmap -sV -p 8080 127.0.0.1` y `nikto -host http://127.0.0.1:8080`.
- Guardar resultados en `evidencias/`; verificar manualmente falsos positivos.

## S3 - ZAP / OWASP
- Iniciar aplicacion en entorno aislado, configurar proxy ZAP; ejecutar spider, escaneo pasivo y (solo en laboratorio) activo; exportar HTML a `evidencias/`.
- Comprobar autenticacion, autorizacion por roles, CSRF, cabeceras, XSS, SQLi y exposicion de datos. Confirmar manualmente tres alertas **si existen**; no fabricar hallazgos.

## S4 - Pruebas manuales
- Interceptar solicitudes con Burp Repeater en localhost; comparar 401/403 con y sin sesion, verificar que un Cliente no acceda a `/api/admin/**`.
- Verificar peticiones POST/PUT sin token CSRF, manejo de entradas invalidas y no filtracion de errores. Registrar peticion/respuesta anonimizadas.
- Usar sqlmap solo en laboratorio autorizado con datos ficticios, sin extraccion de informacion real.

## S5 - Fuzzing
- `scripts/fuzz-local.ps1` usa una lista corta y segura de rutas; registrar HTTP 500 y timeouts, revisar logs. No hacer fuzzing masivo contra servidores externos.

## S6 - Carga y resiliencia
- Importar `jmeter/segurplan-local.jmx` en JMeter, empezar con 2 usuarios y 5 iteraciones; incrementar gradualmente solo en laboratorio con recursos suficientes. Medir p95/p99 y tasa de errores; la guia propone hasta 100-500 usuarios, NO se ejecutan automaticamente.

## CI, remediacion y entregables
- Workflow `.github/workflows/seguridad.yml`: pruebas Maven y escaneo de dependencias mediante Dependency Review en PR; ZAP Baseline opcional para staging expresamente configurado.
- Completar `hallazgos.csv`, `dependencias.csv`, `trazabilidad.csv`, `informe_final.md`, y `evidencias/` con resultados reales. El estado inicial es PENDIENTE DE EJECUCION.
- `scripts/backup-local.ps1` crea respaldo PostgreSQL con pg_dump si esta instalado; restaurar manualmente en una DB aislada tras revisar el archivo.
- Nunca subir credenciales, cookies, respaldos ni datos reales al repositorio.

## Ejecucion en NetBeans 25
`Clean and Build` o Maven integrado. Java 21 y PostgreSQL local segun configuracion del proyecto. Las pruebas unitarias de regresion no necesitan escanear un servidor externo.
