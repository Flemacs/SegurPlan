# Plan ejecutable y evidencias - SegurPlan v6.5

## Alcance
Solo equipo propio, localhost, base de datos ficticia y respaldo previo. No lanzar ataques sobre produccion. Este paquete prepara pruebas; **no inventa ejecuciones ni CVE**.

## S1: planificación y dependencias
`seguridad/dependencias.csv`, `seguridad/trazabilidad.csv` y `seguridad/inventario_endpoints.csv`. Ejecutar `powershell -File seguridad/scripts/dependencias-local.ps1`. Revisar alertas Dependabot/Snyk manualmente y registrar CVE confirmadas.

## S2: reconocimiento
Con la aplicación arrancada: `powershell -File seguridad/scripts/recon-local.ps1`. Revisar Nmap/Nikto, verificar falsos positivos. Inventario de rutas extraído estáticamente: las rutas son aproximadas y deben contrastarse con `/v3/api-docs`.

## S3: OWASP ZAP
`powershell -File seguridad/scripts/zap-baseline-local.ps1` con Docker. Exporta HTML y JSON si se ejecuta. Escaneo activo: **solo manualmente en laboratorio aislado y con autorización**. Confirmar hasta tres alertas reales si existen.

## S4: Burp y SQLmap
Verificar autenticación, autorización por rol, cookies, CSRF, XSS y SQLi con datos sintéticos. Registrar peticiones y respuestas redactadas. No extraer bases de datos reales. No se incluye ejecución automática destructiva.

## S5: fuzzing
`powershell -File seguridad/scripts/fuzz-local.ps1` únicamente contra localhost, y registrar anomalías 500/timeout. Limitar solicitudes.

## S6: carga
Importar `seguridad/jmeter/segurplan-local.jmx`. Empezar por 2 usuarios, verificar límites y respaldo. Registrar p95, p99 y porcentaje de errores. No ejecutar 100-500 usuarios sin entorno aislado dimensionado.

## Smoke endpoints
`powershell -File seguridad/scripts/smoke-local.ps1`. 401/403 en rutas privadas sin sesión son resultados esperados, no fallos. Probar login real con credenciales ficticias por Swagger UI manualmente.

## CI/CD
`.github/workflows/seguridad.yml` automatiza tests de seguridad, dependency tree y revisión de dependencias en PR; ZAP staging requiere URL autorizada.

## Criterio de cierre
Completar `hallazgos.csv` y `informe_final.md` con evidencia, CVE verificadas, mitigación, retest y métricas reales. Las herramientas externas y la base PostgreSQL local del usuario no son accesibles desde este entorno.
