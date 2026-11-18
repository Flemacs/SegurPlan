# Pruebas de SEGURPLAN

## Ejecutarlas

```bash
# PostgreSQL con el esquema cargado (SegurplanApplicationTests arranca la aplicación)
psql -U postgres -d SegurPlan -f db/SegurPlan_schema.sql
psql -U postgres -d SegurPlan -f db/datos_iniciales.sql
mvn clean verify
```
Informes en `target/surefire-reports/`. Sin base de datos solo falla `SegurplanApplicationTests`; el resto son pruebas unitarias con Mockito.

## Inventario

**302 pruebas en 39 archivos.**

| Origen | Pruebas | Detalle |
|---|---|---|
| v6.5 | 278 | Las 278 originales (35 archivos con pruebas; el 36.º era una clase vacía y se eliminó) |
| v5 | 11 | `FlujoSiniestroTest` (7) y `UtilidadesTest` (4) |
| Nuevas | 13 | Bloqueo de login (10), cotizaciones y pólizas ajenas (3) |

Cubren controladores, servicios, flujo de siniestros, reportes (JSON y JasperReports), respaldo, catálogos, pagos y seguridad de acceso.

## Qué se adaptó de las pruebas de v6.5

| Archivo | Ajuste | Motivo |
|---|---|---|
| `CotizacionControllerTest` | Constructor de 3 argumentos; `Authentication`; +2 casos 403 | El controlador ahora verifica al dueño |
| `PolizaControllerTest` | Constructor de 4 argumentos; `Authentication`; +1 caso 403 | Ídem |
| `DocumentoControllerTest`, `PrevisionControllerTest` | Constructor con el servicio adicional | Constructores de v5 |
| `SiniestroControllerBehaviorTest` | Constructor y derivación vía `SiniestroGestionService` | La derivación ahora pasa por la máquina de estados |
| `UsuarioControllerBehaviorTest` | Constructor de 4 argumentos | Bloqueo de login y notificaciones |
| `CotizacionServiceTest`, `SiniestroServiceTest` | Catálogos de prueba en estado `Activo` | v5 rechaza catálogos inactivos; el estado por defecto solo se asigna al guardar |
| `CatalogoControllerTest` | Consulta `findByEstadoOrderByNombreAsc("Activo")` | v5 lista solo catálogos activos |

Además, 7 archivos cuyo nombre no coincidía con su clase se renombraron y 2 se movieron a su carpeta correcta.

## Estado de verificación

Las pruebas se revisaron de forma **estática** (sintaxis y firmas contra el código). **No se ejecutaron**
en el entorno donde se unificó el proyecto. Ejecute `mvn clean verify` y revise `target/surefire-reports/`.
`SEGURPLAN_Documentacion_Pruebas.docx/pdf` es la documentación original de v6.5; su recuento no incluye las pruebas añadidas.
