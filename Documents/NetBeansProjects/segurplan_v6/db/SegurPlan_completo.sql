-- =====================================================================
-- SEGURPLAN - SCRIPT ÚNICO DE BASE DE DATOS (PostgreSQL 14 o superior)
-- Base: SegurPlan      Codificación: UTF-8
--
-- Reemplaza a TODOS los scripts anteriores de la carpeta db/:
--   SegurPlan_schema.sql, migracion_pago.sql, migracion_pago_billetera.sql,
--   migracion_precio_cotizacion.sql, migracion_v5.sql, datos_iniciales.sql
--   y SEGURPLANBASEDEDATOSRESPALDO.sql
--
-- CONTENIDO
--   1) Reinicio       : borra las tablas del sistema si existen
--   2) Esquema        : 16 tablas, restricciones, índices y trigger de historial
--   3) Datos iniciales: roles, catálogos (tipos de seguro y aseguradoras),
--                       matriz de permisos y cuenta de administrador
--   4) Datos de prueba: usuarios, cotizaciones, pagos, pólizas, siniestros,
--                       AFP, evidencias e historial (BORRABLE, ver sección 4)
--   5) Ajuste de secuencias y consultas de verificación
--
-- USO (una sola ejecución; se puede repetir, recrea todo desde cero):
--   createdb -U postgres SegurPlan                      -- solo la primera vez
--   psql -U postgres -d SegurPlan -v ON_ERROR_STOP=1 -f db/SegurPlan_completo.sql
--
-- ADVERTENCIA: al inicio se BORRAN las tablas existentes (se pierden los
-- datos actuales). Haga un respaldo antes si la base ya tiene información.
-- Todo el script corre en una transacción: si algo falla, no queda nada a medias.
--
-- CUENTAS DE ACCESO (cámbielas antes de cualquier uso real):
--   admin@segurplan.com    Admin123!     Administrador   (datos iniciales)
--   asesor@segurplan.com   Asesor123!    Asesor          (prueba)
--   cliente@segurplan.com  Cliente123!   Cliente         (prueba)
--   Los demás usuarios de prueba usan la clave de su rol (Asesor123! / Cliente123!).
--
-- NOTA: el código Java actual solo mapea (JPA) las tablas base. Las tablas
-- comunicacion_aseguradora, notificacion, rol_permiso y las columnas marcadas
-- "v5" provienen de migracion_v5.sql; se conservan porque son inofensivas
-- (ddl-auto=none) y dejan la BD lista para esas funciones.
-- =====================================================================

SET client_encoding = 'UTF8';

BEGIN;

-- ---------------------------------------------------------------------
-- 1) REINICIO
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS rol_permiso              CASCADE;
DROP TABLE IF EXISTS notificacion             CASCADE;
DROP TABLE IF EXISTS comunicacion_aseguradora CASCADE;
DROP TABLE IF EXISTS pago                     CASCADE;
DROP TABLE IF EXISTS historial                CASCADE;
DROP TABLE IF EXISTS documento                CASCADE;
DROP TABLE IF EXISTS prevision                CASCADE;
DROP TABLE IF EXISTS siniestro                CASCADE;
DROP TABLE IF EXISTS poliza                   CASCADE;
DROP TABLE IF EXISTS revision_legal           CASCADE;
DROP TABLE IF EXISTS contratacion             CASCADE;
DROP TABLE IF EXISTS cotizacion               CASCADE;
DROP TABLE IF EXISTS aseguradora              CASCADE;
DROP TABLE IF EXISTS tipo_seguro              CASCADE;
DROP TABLE IF EXISTS usuario                  CASCADE;
DROP TABLE IF EXISTS rol                      CASCADE;
DROP FUNCTION IF EXISTS fn_historial_inmutable() CASCADE;

-- ---------------------------------------------------------------------
-- 2) ESQUEMA
-- ---------------------------------------------------------------------
CREATE TABLE rol (
    id_rol      SERIAL PRIMARY KEY,
    nombre      VARCHAR(50) NOT NULL UNIQUE,
    descripcion VARCHAR(150)
);

CREATE TABLE usuario (
    id_usuario         BIGSERIAL PRIMARY KEY,
    nombres            VARCHAR(100) NOT NULL,
    apellidos          VARCHAR(100) NOT NULL,
    correo             VARCHAR(150) NOT NULL UNIQUE,
    telefono           VARCHAR(20),
    password           VARCHAR(255) NOT NULL,          -- hash BCrypt
    estado             VARCHAR(20)  NOT NULL DEFAULT 'Activo',
    fecha_registro     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_rol             INT          NOT NULL,
    direccion          VARCHAR(200),                   -- v5 (perfil)
    fecha_modificacion TIMESTAMP,                      -- v5 (perfil)
    area_especialidad  VARCHAR(30),                    -- v5 (asesor)
    CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol) REFERENCES rol(id_rol),
    CONSTRAINT chk_usuario_estado CHECK (estado IN ('Activo', 'Inactivo', 'Bloqueado'))
);

CREATE TABLE tipo_seguro (
    id_tipo_seguro SERIAL PRIMARY KEY,
    nombre         VARCHAR(80) NOT NULL UNIQUE,
    descripcion    VARCHAR(200),
    estado         VARCHAR(20) NOT NULL DEFAULT 'Activo',
    CONSTRAINT chk_tipo_seguro_estado CHECK (estado IN ('Activo', 'Inactivo'))
);

CREATE TABLE aseguradora (
    id_aseguradora SERIAL PRIMARY KEY,
    nombre         VARCHAR(150) NOT NULL,
    ruc            VARCHAR(20) UNIQUE,
    telefono       VARCHAR(20),
    correo         VARCHAR(150),
    estado         VARCHAR(20) NOT NULL DEFAULT 'Activo',
    CONSTRAINT chk_aseguradora_estado CHECK (estado IN ('Activo', 'Inactivo'))
);

CREATE TABLE cotizacion (
    id_cotizacion         BIGSERIAL PRIMARY KEY,
    id_usuario            BIGINT NOT NULL,
    id_tipo_seguro        INT    NOT NULL,
    id_aseguradora        INT    NOT NULL,
    -- Datos de la solicitud
    fecha_solicitud       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    descripcion_solicitud TEXT,
    datos_json            TEXT,                        -- v5 (formulario dinámico)
    -- Datos de la cotización (respuesta del asesor)
    precio                NUMERIC(12,2),
    vigencia              DATE,
    cobertura             TEXT,
    deducible             NUMERIC(12,2),
    estado                VARCHAR(30) NOT NULL DEFAULT 'Solicitada',
    fecha_respuesta       TIMESTAMP,
    CONSTRAINT fk_cotizacion_usuario     FOREIGN KEY (id_usuario)     REFERENCES usuario(id_usuario),
    CONSTRAINT fk_cotizacion_tipo_seguro FOREIGN KEY (id_tipo_seguro) REFERENCES tipo_seguro(id_tipo_seguro),
    CONSTRAINT fk_cotizacion_aseguradora FOREIGN KEY (id_aseguradora) REFERENCES aseguradora(id_aseguradora),
    CONSTRAINT chk_cotizacion_precio    CHECK (precio IS NULL OR precio >= 0),
    CONSTRAINT chk_cotizacion_deducible CHECK (deducible IS NULL OR deducible >= 0),
    CONSTRAINT chk_cotizacion_estado CHECK (estado IN (
        'Solicitada', 'En revisión', 'Disponible', 'Aceptada', 'Rechazada', 'Vencida'))
);

CREATE TABLE contratacion (
    id_contratacion       BIGSERIAL PRIMARY KEY,
    id_cotizacion         BIGINT NOT NULL UNIQUE,
    id_usuario            BIGINT NOT NULL,
    fecha_contratacion    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Evidencia de aceptación de condiciones
    version_condiciones   VARCHAR(30),
    condiciones_aceptadas BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_aceptacion      TIMESTAMP,
    estado                VARCHAR(40) NOT NULL DEFAULT 'Iniciada',
    observaciones         TEXT,
    CONSTRAINT fk_contratacion_cotizacion FOREIGN KEY (id_cotizacion) REFERENCES cotizacion(id_cotizacion),
    CONSTRAINT fk_contratacion_usuario    FOREIGN KEY (id_usuario)    REFERENCES usuario(id_usuario),
    CONSTRAINT chk_contratacion_estado CHECK (estado IN (
        'Iniciada', 'Documentación pendiente', 'Documentación validada',
        'En revisión legal', 'Enviada a aseguradora', 'En evaluación',
        'Aprobada', 'Rechazada', 'Finalizada'))
);

-- Sin entidad JPA en el código actual; se conserva por el modelo de datos.
CREATE TABLE revision_legal (
    id_revision        BIGSERIAL PRIMARY KEY,
    id_contratacion    BIGINT NOT NULL,
    id_responsable     BIGINT NOT NULL,
    fecha_revision     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resultado          VARCHAR(30) NOT NULL DEFAULT 'Pendiente',
    observaciones      TEXT,
    contrato_conforme  BOOLEAN,
    estado_observacion VARCHAR(30) DEFAULT 'Pendiente',
    fecha_cierre       TIMESTAMP,
    CONSTRAINT fk_revision_contratacion FOREIGN KEY (id_contratacion) REFERENCES contratacion(id_contratacion),
    CONSTRAINT fk_revision_responsable  FOREIGN KEY (id_responsable)  REFERENCES usuario(id_usuario),
    CONSTRAINT chk_revision_resultado CHECK (resultado IN ('Pendiente', 'Conforme', 'Observado', 'Rechazado')),
    CONSTRAINT chk_revision_observacion CHECK (estado_observacion IN ('Pendiente', 'Atendida', 'Cerrada'))
);

CREATE TABLE poliza (
    id_poliza       BIGSERIAL PRIMARY KEY,
    id_contratacion BIGINT NOT NULL UNIQUE,
    numero_poliza   VARCHAR(80) NOT NULL UNIQUE,       -- formato POL-00000001
    fecha_inicio    DATE NOT NULL,
    fecha_fin       DATE NOT NULL,
    prima           NUMERIC(12,2) NOT NULL,
    cobertura       TEXT NOT NULL,
    deducible       NUMERIC(12,2),
    condiciones     TEXT,
    estado          VARCHAR(30) NOT NULL DEFAULT 'Recibida',
    CONSTRAINT fk_poliza_contratacion FOREIGN KEY (id_contratacion) REFERENCES contratacion(id_contratacion),
    CONSTRAINT chk_poliza_prima     CHECK (prima > 0),
    CONSTRAINT chk_poliza_deducible CHECK (deducible IS NULL OR deducible >= 0),
    CONSTRAINT chk_poliza_fechas    CHECK (fecha_fin > fecha_inicio),
    CONSTRAINT chk_poliza_estado CHECK (estado IN (
        'Recibida', 'En validación', 'Validada', 'Observada',
        'Entregada', 'Vigente', 'Vencida'))
);

CREATE TABLE siniestro (
    id_siniestro         BIGSERIAL PRIMARY KEY,
    id_usuario           BIGINT NOT NULL,
    id_aseguradora       INT    NOT NULL,
    numero_siniestro     VARCHAR(80),                  -- formato SIN-XXXXXXXX
    fecha_registro       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_accidente      TIMESTAMP,
    -- Datos del vehículo
    placa                VARCHAR(15),
    marca                VARCHAR(60),
    modelo               VARCHAR(60),
    anio                 INT,
    -- Datos del accidente
    lugar                VARCHAR(200),
    descripcion          TEXT,
    -- Seguimiento
    resultado_evaluacion TEXT,
    observaciones        TEXT,
    estado               VARCHAR(40) NOT NULL DEFAULT 'Registrado',
    monto_aprobado       NUMERIC(12,2),                -- v5
    fecha_evaluacion     TIMESTAMP,                    -- v5
    fecha_derivacion     TIMESTAMP,                    -- v5
    CONSTRAINT fk_siniestro_usuario     FOREIGN KEY (id_usuario)     REFERENCES usuario(id_usuario),
    CONSTRAINT fk_siniestro_aseguradora FOREIGN KEY (id_aseguradora) REFERENCES aseguradora(id_aseguradora),
    CONSTRAINT chk_siniestro_estado CHECK (estado IN (
        'Registrado', 'En revisión', 'Información pendiente', 'Información completa',
        'Derivado', 'En evaluación', 'Evaluación recibida', 'Observado',
        'Aprobado', 'Rechazado', 'Cerrado')),
    CONSTRAINT chk_siniestro_anio  CHECK (anio IS NULL OR anio >= 1900),
    CONSTRAINT chk_siniestro_monto CHECK (monto_aprobado IS NULL OR monto_aprobado >= 0)
);

CREATE TABLE prevision (
    id_prevision           BIGSERIAL PRIMARY KEY,
    id_usuario             BIGINT NOT NULL,
    tipo_sistema           VARCHAR(20) NOT NULL,
    edad                   INT,
    ingreso_mensual        NUMERIC(12,2),
    anios_aporte           INT,
    fecha_registro         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Datos de simulación
    monto_pension_estimado NUMERIC(12,2),
    resultado_simulacion   TEXT,
    -- Orientación del asesor
    consulta               TEXT,
    respuesta              TEXT,
    estado                 VARCHAR(30) NOT NULL DEFAULT 'Registrada',
    CONSTRAINT fk_prevision_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario),
    CONSTRAINT chk_prevision_tipo     CHECK (tipo_sistema IN ('AFP', 'ONP')),
    CONSTRAINT chk_prevision_edad     CHECK (edad IS NULL OR edad > 0),
    CONSTRAINT chk_prevision_ingreso  CHECK (ingreso_mensual IS NULL OR ingreso_mensual >= 0),
    CONSTRAINT chk_prevision_aporte   CHECK (anios_aporte IS NULL OR anios_aporte >= 0),
    CONSTRAINT chk_prevision_pension  CHECK (monto_pension_estimado IS NULL OR monto_pension_estimado >= 0),
    CONSTRAINT chk_prevision_estado   CHECK (estado IN (
        'Registrada', 'En revisión', 'Simulada', 'Respondida', 'Cerrada'))
);

CREATE TABLE documento (
    id_documento    BIGSERIAL PRIMARY KEY,
    id_usuario      BIGINT NOT NULL,
    id_contratacion BIGINT,
    id_siniestro    BIGINT,
    id_cotizacion   BIGINT,                            -- v5 (adjuntos de cotización)
    tipo_documento  VARCHAR(80)  NOT NULL,             -- p. ej. FOTO_ACCIDENTE
    nombre_archivo  VARCHAR(200) NOT NULL,
    ruta_archivo    TEXT         NOT NULL,             -- uploads/siniestros/{id}/{uuid}.ext
    version         INT          NOT NULL DEFAULT 1,
    fecha_carga     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado          VARCHAR(30)  NOT NULL DEFAULT 'Pendiente',
    observacion     TEXT,                              -- v5 (observación del asesor)
    CONSTRAINT fk_documento_usuario      FOREIGN KEY (id_usuario)      REFERENCES usuario(id_usuario),
    CONSTRAINT fk_documento_contratacion FOREIGN KEY (id_contratacion) REFERENCES contratacion(id_contratacion),
    CONSTRAINT fk_documento_siniestro    FOREIGN KEY (id_siniestro)    REFERENCES siniestro(id_siniestro),
    CONSTRAINT fk_documento_cotizacion   FOREIGN KEY (id_cotizacion)   REFERENCES cotizacion(id_cotizacion),
    CONSTRAINT chk_documento_version CHECK (version > 0),
    CONSTRAINT chk_documento_estado  CHECK (estado IN ('Pendiente', 'Validado', 'Observado', 'Rechazado')),
    CONSTRAINT chk_documento_referencia CHECK (
        id_contratacion IS NOT NULL OR id_siniestro IS NOT NULL OR id_cotizacion IS NOT NULL)
);

CREATE TABLE historial (
    id_historial   BIGSERIAL PRIMARY KEY,
    id_usuario     BIGINT,
    entidad        VARCHAR(50) NOT NULL,
    id_registro    BIGINT      NOT NULL,
    accion         VARCHAR(50) NOT NULL,
    valor_anterior TEXT,
    valor_nuevo    TEXT,
    fecha_hora     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_historial_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
);

-- Pago SIMULADO de la contratación (tarjeta, Yape o Plin; sin pasarela real)
CREATE TABLE pago (
    id_pago          BIGSERIAL PRIMARY KEY,
    id_cotizacion    BIGINT NOT NULL,
    id_contratacion  BIGINT,
    id_usuario       BIGINT NOT NULL,
    metodo           VARCHAR(30) NOT NULL DEFAULT 'Tarjeta (simulado)',
    monto            NUMERIC(12,2) NOT NULL,
    moneda           VARCHAR(5)  NOT NULL DEFAULT 'PEN',
    estado           VARCHAR(20) NOT NULL,
    referencia       VARCHAR(40) NOT NULL UNIQUE,      -- formato PAG-XXXXXXXXXXXX
    tarjeta_ultimos4 VARCHAR(4),
    titular          VARCHAR(120),
    motivo_rechazo   VARCHAR(200),
    celular_origen   VARCHAR(15),                      -- Yape/Plin: celular enmascarado
    numero_operacion VARCHAR(20),                      -- Yape/Plin: nº de operación
    destino          VARCHAR(20),                      -- Yape/Plin: nº ficticio del QR
    fecha_pago       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pago_cotizacion   FOREIGN KEY (id_cotizacion)   REFERENCES cotizacion(id_cotizacion),
    CONSTRAINT fk_pago_contratacion FOREIGN KEY (id_contratacion) REFERENCES contratacion(id_contratacion),
    CONSTRAINT fk_pago_usuario      FOREIGN KEY (id_usuario)      REFERENCES usuario(id_usuario),
    CONSTRAINT chk_pago_monto  CHECK (monto > 0),
    CONSTRAINT chk_pago_estado CHECK (estado IN ('Aprobado', 'Rechazado'))
);

-- v5: comunicación del asesor con la aseguradora (RF005)
CREATE TABLE comunicacion_aseguradora (
    id_comunicacion BIGSERIAL PRIMARY KEY,
    id_cotizacion   BIGINT NOT NULL REFERENCES cotizacion(id_cotizacion),
    id_asesor       BIGINT NOT NULL REFERENCES usuario(id_usuario),
    fecha           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    medio           VARCHAR(30) NOT NULL DEFAULT 'Correo',
    contacto        VARCHAR(150),
    observaciones   TEXT NOT NULL
);

-- v5: notificaciones internas (RF032)
CREATE TABLE notificacion (
    id_notificacion BIGSERIAL PRIMARY KEY,
    id_usuario      BIGINT NOT NULL REFERENCES usuario(id_usuario),
    tipo            VARCHAR(40)  NOT NULL,
    titulo          VARCHAR(150) NOT NULL,
    mensaje         TEXT,
    entidad         VARCHAR(40),
    id_registro     BIGINT,
    leida           BOOLEAN   NOT NULL DEFAULT FALSE,
    fecha           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- v5: permisos por rol y módulo (RF030)
CREATE TABLE rol_permiso (
    id_rol   INT         NOT NULL REFERENCES rol(id_rol),
    modulo   VARCHAR(40) NOT NULL,
    ver      BOOLEAN NOT NULL DEFAULT FALSE,
    crear    BOOLEAN NOT NULL DEFAULT FALSE,
    editar   BOOLEAN NOT NULL DEFAULT FALSE,
    eliminar BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id_rol, modulo)
);

-- Un solo pago aprobado por cotización (los rechazados pueden repetirse)
CREATE UNIQUE INDEX ux_pago_cotizacion_aprobado
    ON pago (id_cotizacion) WHERE estado = 'Aprobado';

-- Índices de consulta
CREATE INDEX idx_usuario_rol            ON usuario(id_rol);
CREATE INDEX idx_cotizacion_usuario     ON cotizacion(id_usuario);
CREATE INDEX idx_cotizacion_tipo        ON cotizacion(id_tipo_seguro);
CREATE INDEX idx_cotizacion_aseguradora ON cotizacion(id_aseguradora);
CREATE INDEX idx_cotizacion_estado      ON cotizacion(estado);
CREATE INDEX idx_contratacion_usuario   ON contratacion(id_usuario);
CREATE INDEX idx_revision_contratacion  ON revision_legal(id_contratacion);
CREATE INDEX idx_revision_responsable   ON revision_legal(id_responsable);
CREATE INDEX idx_poliza_contratacion    ON poliza(id_contratacion);
CREATE INDEX idx_siniestro_usuario      ON siniestro(id_usuario);
CREATE INDEX idx_siniestro_aseguradora  ON siniestro(id_aseguradora);
CREATE INDEX idx_siniestro_estado       ON siniestro(estado);
CREATE INDEX idx_prevision_usuario      ON prevision(id_usuario);
CREATE INDEX idx_prevision_estado       ON prevision(estado);
CREATE INDEX idx_documento_usuario      ON documento(id_usuario);
CREATE INDEX idx_documento_contratacion ON documento(id_contratacion);
CREATE INDEX idx_documento_siniestro    ON documento(id_siniestro);
CREATE INDEX idx_historial_usuario      ON historial(id_usuario);
CREATE INDEX idx_historial_entidad      ON historial(entidad, id_registro);
CREATE INDEX idx_pago_usuario           ON pago(id_usuario);
CREATE INDEX idx_pago_cotizacion        ON pago(id_cotizacion);
CREATE INDEX ix_notificacion_usuario    ON notificacion(id_usuario, leida, fecha DESC);

-- Historial inmutable (RF033): impide UPDATE y DELETE sobre la trazabilidad
CREATE FUNCTION fn_historial_inmutable() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'El historial no se puede modificar ni eliminar';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_historial_inmutable
    BEFORE UPDATE OR DELETE ON historial
    FOR EACH ROW EXECUTE FUNCTION fn_historial_inmutable();

-- ---------------------------------------------------------------------
-- 3) DATOS INICIALES (necesarios para que el sistema funcione)
-- ---------------------------------------------------------------------

-- Roles: los nombres deben coincidir con SecurityConfig (hasRole)
INSERT INTO rol (id_rol, nombre, descripcion) VALUES
    (1, 'Cliente',       'Cliente / asegurado de la plataforma'),
    (2, 'Asesor',        'Asesor comercial de SegurPlan'),
    (3, 'Administrador', 'Administrador del sistema (incluye reportes y gerencia)');

-- Tipos de seguro (los nombres deben coincidir con los de s03.js / app.js)
INSERT INTO tipo_seguro (id_tipo_seguro, nombre, descripcion) VALUES
    ( 1, 'Seguro vehicular',              'Daños propios y a terceros para su vehículo'),
    ( 2, 'SOAT',                          'Seguro Obligatorio de Accidentes de Tránsito'),
    ( 3, 'Seguro de vida',                'Protección económica para su familia'),
    ( 4, 'Seguro de vida con ahorro',     'Cobertura de vida con componente de ahorro'),
    ( 5, 'Seguro de salud',               'Atención médica y hospitalaria'),
    ( 6, 'Seguro oncológico',             'Cobertura para diagnóstico y tratamiento oncológico'),
    ( 7, 'Seguro de hogar',               'Protección de su vivienda y contenido'),
    ( 8, 'Seguro de accidentes personales','Cobertura ante accidentes'),
    ( 9, 'Seguro de viaje',               'Asistencia y cobertura en viajes'),
    (10, 'Seguro de desgravamen',         'Cancela deudas ante fallecimiento o invalidez'),
    (11, 'Seguro agrícola',               'Protección de cultivos'),
    (12, 'Seguro estudiantil',            'Cobertura para estudiantes'),
    (13, 'Seguro de sepelio',             'Gastos de sepelio'),
    (14, 'Seguro de desempleo',           'Cobertura ante pérdida involuntaria de empleo');

-- Aseguradoras (los nombres deben coincidir con las de s03.js)
INSERT INTO aseguradora (id_aseguradora, nombre, ruc, telefono, correo) VALUES
    (1, 'Rímac Seguros',      '20100041953', '012111000', 'contacto@rimac.example'),
    (2, 'Pacífico Seguros',   '20100035392', '012222000', 'contacto@pacifico.example'),
    (3, 'La Positiva',        '20100210909', '012333000', 'contacto@lapositiva.example'),
    (4, 'Mapfre Perú',        '20202380621', '012444000', 'contacto@mapfre.example'),
    (5, 'Interseguro',        '20100053455', '012555000', 'contacto@interseguro.example'),
    (6, 'Seguros SURA',       '20517207331', '012666000', 'contacto@sura.example'),
    (7, 'BNP Paribas Cardif', '20513328644', '012777000', 'contacto@cardif.example');

-- Matriz de permisos por defecto (editable desde Administración > Roles y permisos)
INSERT INTO rol_permiso (id_rol, modulo, ver, crear, editar, eliminar) VALUES
    (1, 'Cotizaciones', TRUE,  TRUE,  FALSE, FALSE),
    (1, 'Pólizas',      TRUE,  FALSE, FALSE, FALSE),
    (1, 'AFP',          TRUE,  TRUE,  FALSE, FALSE),
    (1, 'Siniestros',   TRUE,  TRUE,  FALSE, FALSE),
    (1, 'Documentos',   TRUE,  TRUE,  FALSE, TRUE),
    (2, 'Cotizaciones', TRUE,  FALSE, TRUE,  FALSE),
    (2, 'Pólizas',      TRUE,  FALSE, FALSE, FALSE),
    (2, 'AFP',          TRUE,  FALSE, TRUE,  FALSE),
    (2, 'Siniestros',   TRUE,  FALSE, TRUE,  FALSE),
    (2, 'Documentos',   TRUE,  FALSE, TRUE,  FALSE),
    (2, 'Historial',    TRUE,  FALSE, FALSE, FALSE),
    (3, 'Cotizaciones', TRUE,  TRUE,  TRUE,  TRUE),
    (3, 'Pólizas',      TRUE,  TRUE,  TRUE,  TRUE),
    (3, 'AFP',          TRUE,  TRUE,  TRUE,  TRUE),
    (3, 'Siniestros',   TRUE,  TRUE,  TRUE,  TRUE),
    (3, 'Documentos',   TRUE,  TRUE,  TRUE,  TRUE),
    (3, 'Usuarios',     TRUE,  TRUE,  TRUE,  TRUE),
    (3, 'Asesores',     TRUE,  TRUE,  TRUE,  TRUE),
    (3, 'Roles',        TRUE,  TRUE,  TRUE,  TRUE),
    (3, 'Reportes',     TRUE,  TRUE,  TRUE,  TRUE),
    (3, 'Historial',    TRUE,  TRUE,  TRUE,  TRUE),
    (3, 'Respaldo',     TRUE,  TRUE,  TRUE,  TRUE),
    (3, 'Catálogos',    TRUE,  TRUE,  TRUE,  TRUE);

-- Administrador inicial (contraseña: Admin123! - hash BCrypt)
INSERT INTO usuario (id_usuario, nombres, apellidos, correo, telefono, password, estado,
                     fecha_registro, id_rol, direccion, area_especialidad) VALUES
    (1, 'Administrador', 'SegurPlan', 'admin@segurplan.com', '999000001',
     '$2a$10$jugvI4OYgB7zOfVeze48uePgar51XO/CJx5oAQp8ckEppdtUuDXPa', 'Activo',
     CURRENT_TIMESTAMP - INTERVAL '450 days', 3, 'Av. Principal 100, Oficina Central', NULL);

-- ---------------------------------------------------------------------
-- 4) DATOS DE PRUEBA
--    Para un entorno limpio (producción) BORRE todo desde esta línea hasta
--    el comentario "FIN DE LOS DATOS DE PRUEBA" y deje el resto del script.
--    Las fechas son relativas al día de ejecución, así que los estados
--    (vigente, vencida, pendiente...) siempre se ven coherentes.
-- ---------------------------------------------------------------------

-- Usuarios (Asesor123! para asesores, Cliente123! para clientes)
INSERT INTO usuario (id_usuario, nombres, apellidos, correo, telefono, password, estado,
                     fecha_registro, id_rol, direccion, area_especialidad) VALUES
    (2, 'Ana',    'Asesora Ventas',      'asesor@segurplan.com',        '999000002',
        '$2a$10$NYf5VhYoS48AcOC39xVTKukgmAFgczbJ68G9dakOHYT/jWnXweZF2', 'Activo',
        CURRENT_TIMESTAMP - INTERVAL '445 days', 2, NULL, 'Siniestros'),
    (3, 'Carlos', 'Cliente Prueba',      'cliente@segurplan.com',       '999000004',
        '$2a$10$iDLC2pBWql.pKNhiKcvzUeJDw50g5a1TLKihzSYX.ZAwhUdY83Xfq', 'Activo',
        CURRENT_TIMESTAMP - INTERVAL '430 days', 1, 'Calle Los Olivos 245, Arequipa', NULL),
    (4, 'Luis',   'Asesor Seguros',      'luis.asesor@segurplan.com',   '999000005',
        '$2a$10$NYf5VhYoS48AcOC39xVTKukgmAFgczbJ68G9dakOHYT/jWnXweZF2', 'Activo',
        CURRENT_TIMESTAMP - INTERVAL '440 days', 2, NULL, 'Seguros'),
    (5, 'Rosa',   'Asesora Previsional', 'rosa.asesora@segurplan.com',  '999000006',
        '$2a$10$NYf5VhYoS48AcOC39xVTKukgmAFgczbJ68G9dakOHYT/jWnXweZF2', 'Activo',
        CURRENT_TIMESTAMP - INTERVAL '438 days', 2, NULL, 'AFP'),
    (6, 'María',  'Torres Quispe',       'maria.torres@segurplan.com',  '987000101',
        '$2a$10$iDLC2pBWql.pKNhiKcvzUeJDw50g5a1TLKihzSYX.ZAwhUdY83Xfq', 'Activo',
        CURRENT_TIMESTAMP - INTERVAL '120 days', 1, 'Av. Bolognesi 530, Arequipa', NULL),
    (7, 'Jorge',  'Mamani Huamán',       'jorge.mamani@segurplan.com',  '987000102',
        '$2a$10$iDLC2pBWql.pKNhiKcvzUeJDw50g5a1TLKihzSYX.ZAwhUdY83Xfq', 'Activo',
        CURRENT_TIMESTAMP - INTERVAL '90 days',  1, 'Jr. Puno 118, Arequipa', NULL),
    (8, 'Lucía',  'Fernández Rojas',     'lucia.fernandez@segurplan.com','987000103',
        '$2a$10$iDLC2pBWql.pKNhiKcvzUeJDw50g5a1TLKihzSYX.ZAwhUdY83Xfq', 'Activo',
        CURRENT_TIMESTAMP - INTERVAL '110 days', 1, 'Urb. Las Begonias Mz B Lt 7, Arequipa', NULL),
    (9, 'Pedro',  'Ccori Salas',         'pedro.ccori@segurplan.com',   '987000104',
        '$2a$10$iDLC2pBWql.pKNhiKcvzUeJDw50g5a1TLKihzSYX.ZAwhUdY83Xfq', 'Inactivo',
        CURRENT_TIMESTAMP - INTERVAL '45 days',  1, NULL, NULL);

-- Cotizaciones: una por cada estado posible
-- (cada aseguradora/tipo respeta la lista que ofrece la pantalla s03-cotizacion)
INSERT INTO cotizacion (id_cotizacion, id_usuario, id_tipo_seguro, id_aseguradora, fecha_solicitud,
                        descripcion_solicitud, precio, vigencia, cobertura, deducible, estado, fecha_respuesta) VALUES
    (1, 3, 1, 1, CURRENT_TIMESTAMP - INTERVAL '45 days',
        'Seguro vehicular para automóvil particular. Placa C9L-123, Toyota Corolla 2020.',
        1850.00, CURRENT_DATE - 13,
        'Daños propios por accidente, responsabilidad civil frente a terceros, robo total y asistencia en carretera 24 h.',
        500.00, 'Aceptada', CURRENT_TIMESTAMP - INTERVAL '43 days'),
    (2, 6, 2, 2, CURRENT_TIMESTAMP - INTERVAL '102 days',
        'SOAT para automóvil particular. Placa V4C-205, Kia Rio 2021.',
        320.00, CURRENT_DATE - 71,
        'Cobertura obligatoria por muerte, invalidez permanente, incapacidad temporal y gastos médicos y de sepelio.',
        0.00, 'Aceptada', CURRENT_TIMESTAMP - INTERVAL '101 days'),
    (3, 7, 3, 5, CURRENT_TIMESTAMP - INTERVAL '6 days',
        'Seguro de vida. Fecha de nacimiento 12/03/1988. Suma asegurada S/ 150,000.',
        2400.00, CURRENT_DATE + 25,
        'Fallecimiento por cualquier causa, invalidez total y permanente y anticipo por enfermedad terminal.',
        0.00, 'Aceptada', CURRENT_TIMESTAMP - INTERVAL '5 days'),
    (4, 8, 7, 4, CURRENT_TIMESTAMP - INTERVAL '18 days',
        'Seguro de hogar para departamento en Urb. Las Begonias Mz B Lt 7.',
        780.00, CURRENT_DATE + 13,
        'Incendio, sismo, robo con violencia, daños por agua y responsabilidad civil familiar.',
        150.00, 'Aceptada', CURRENT_TIMESTAMP - INTERVAL '17 days'),
    (5, 3, 2, 3, CURRENT_TIMESTAMP - INTERVAL '405 days',
        'SOAT para automóvil particular. Placa C9L-123, Toyota Corolla 2020.',
        290.00, CURRENT_DATE - 374,
        'Cobertura obligatoria por muerte, invalidez permanente, incapacidad temporal y gastos médicos y de sepelio.',
        0.00, 'Aceptada', CURRENT_TIMESTAMP - INTERVAL '404 days'),
    (6, 3, 5, 2, CURRENT_TIMESTAMP - INTERVAL '4 days',
        'Seguro de salud familiar. Fecha de nacimiento 20/08/1990. 3 beneficiarios.',
        1450.00, CURRENT_DATE + 28,
        'Consultas ambulatorias, hospitalización, cirugías, emergencias y medicinas en red de clínicas afiliadas.',
        200.00, 'Disponible', CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (7, 3, 9, 1, CURRENT_TIMESTAMP - INTERVAL '1 days',
        'Seguro de viaje a España (Madrid). Salida en 5 semanas, retorno a los 14 días.',
        NULL, NULL, NULL, NULL, 'Solicitada', NULL),
    (8, 6, 4, 5, CURRENT_TIMESTAMP - INTERVAL '3 days',
        'Seguro de vida con ahorro. Fecha de nacimiento 05/11/1985. Suma asegurada S/ 100,000.',
        NULL, NULL, NULL, NULL, 'En revisión', NULL),
    (9, 7, 8, 6, CURRENT_TIMESTAMP - INTERVAL '30 days',
        'Seguro de accidentes personales para trabajador independiente.',
        95.00, CURRENT_DATE + 2,
        'Muerte accidental, invalidez permanente y gastos de curación por accidente.',
        0.00, 'Rechazada', CURRENT_TIMESTAMP - INTERVAL '28 days'),
    (10, 8, 6, 2, CURRENT_TIMESTAMP - INTERVAL '60 days',
        'Seguro oncológico. Fecha de nacimiento 14/02/1992.',
        520.00, CURRENT_DATE - 28,
        'Diagnóstico, quimioterapia, radioterapia y cirugía oncológica en clínicas afiliadas.',
        100.00, 'Vencida', CURRENT_TIMESTAMP - INTERVAL '58 days'),
    (11, 3, 10, 4, CURRENT_TIMESTAMP - INTERVAL '5 days',
        'Seguro de desgravamen para crédito de consumo. Entidad financiera: Banco Demo. Monto del crédito S/ 25,000.',
        610.00, CURRENT_DATE + 27,
        'Cancelación del saldo deudor por fallecimiento o invalidez total y permanente.',
        0.00, 'Disponible', CURRENT_TIMESTAMP - INTERVAL '3 days'),
    (12, 7, 1, 3, CURRENT_TIMESTAMP - INTERVAL '9 days',
        'Seguro vehicular para automóvil particular. Placa D2M-764, Chevrolet Sail 2020.',
        2100.00, CURRENT_DATE + 22,
        'Daños propios, responsabilidad civil frente a terceros, robo total, lunas y asistencia vial.',
        600.00, 'Aceptada', CURRENT_TIMESTAMP - INTERVAL '8 days');

-- Contrataciones (se crean al aprobarse el pago de una cotización)
INSERT INTO contratacion (id_contratacion, id_cotizacion, id_usuario, fecha_contratacion,
                          version_condiciones, condiciones_aceptadas, fecha_aceptacion, estado, observaciones) VALUES
    (1,  1, 3, CURRENT_TIMESTAMP - INTERVAL '41 days',  '1.0', TRUE, CURRENT_TIMESTAMP - INTERVAL '41 days',  'Finalizada', NULL),
    (2,  2, 6, CURRENT_TIMESTAMP - INTERVAL '100 days', '1.0', TRUE, CURRENT_TIMESTAMP - INTERVAL '100 days', 'Finalizada', NULL),
    (3,  3, 7, CURRENT_TIMESTAMP - INTERVAL '3 days',   '1.0', TRUE, CURRENT_TIMESTAMP - INTERVAL '3 days',   'Aprobada',   NULL),
    (4,  4, 8, CURRENT_TIMESTAMP - INTERVAL '16 days',  '1.0', TRUE, CURRENT_TIMESTAMP - INTERVAL '16 days',  'Finalizada', NULL),
    (5,  5, 3, CURRENT_TIMESTAMP - INTERVAL '402 days', '1.0', TRUE, CURRENT_TIMESTAMP - INTERVAL '402 days', 'Finalizada', NULL),
    -- Contratación 6: pagada y SIN póliza -> aparece en "Generar póliza" del administrador
    (6, 12, 7, CURRENT_TIMESTAMP - INTERVAL '6 days',   '1.0', TRUE, CURRENT_TIMESTAMP - INTERVAL '6 days',   'Iniciada',   NULL);

-- Pagos simulados (monto = precio de la cotización; un solo pago aprobado por cotización)
INSERT INTO pago (id_pago, id_cotizacion, id_contratacion, id_usuario, metodo, monto, moneda, estado,
                  referencia, tarjeta_ultimos4, titular, motivo_rechazo,
                  celular_origen, numero_operacion, destino, fecha_pago) VALUES
    (1,  1, 1,    3, 'Tarjeta (simulado)', 1850.00, 'PEN', 'Aprobado',  'PAG-8F3A21C07B4D', '4242', 'CARLOS CLIENTE PRUEBA',  NULL,
         NULL, NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '41 days'),
    (2,  2, 2,    6, 'Yape (simulado)',     320.00, 'PEN', 'Aprobado',  'PAG-1D9E6B52A8F0', NULL,   NULL,                     NULL,
         '9** *** 101', '48213907', '999888777', CURRENT_TIMESTAMP - INTERVAL '100 days'),
    (3,  3, 3,    7, 'Plin (simulado)',    2400.00, 'PEN', 'Aprobado',  'PAG-6C40F7E3925A', NULL,   NULL,                     NULL,
         '9** *** 102', '73920418', '999888777', CURRENT_TIMESTAMP - INTERVAL '3 days'),
    (4,  4, 4,    8, 'Tarjeta (simulado)',  780.00, 'PEN', 'Aprobado',  'PAG-A52B8D19E6C3', '1111', 'LUCIA FERNANDEZ ROJAS',  NULL,
         NULL, NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '16 days'),
    (5,  5, 5,    3, 'Yape (simulado)',     290.00, 'PEN', 'Aprobado',  'PAG-0E7F34B9D1A8', NULL,   NULL,                     NULL,
         '9** *** 004', '20571836', '999888777', CURRENT_TIMESTAMP - INTERVAL '402 days'),
    (6, 12, 6,    7, 'Tarjeta (simulado)', 2100.00, 'PEN', 'Aprobado',  'PAG-D18C5A6F2B70', '4242', 'JORGE MAMANI HUAMAN',    NULL,
         NULL, NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '6 days'),
    -- Intentos rechazados (no generan contratación)
    (7, 12, NULL, 7, 'Plin (simulado)',    2100.00, 'PEN', 'Rechazado', 'PAG-39B6E0F4C2A1', NULL,   NULL,                     'Saldo insuficiente en la billetera.',
         '9** *** 000', NULL, '999888777', CURRENT_TIMESTAMP - INTERVAL '7 days'),
    (8,  1, NULL, 3, 'Tarjeta (simulado)', 1850.00, 'PEN', 'Rechazado', 'PAG-F4A7192D8E35', '0002', 'CARLOS CLIENTE PRUEBA',  'Fondos insuficientes.',
         NULL, NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '42 days');

-- Pólizas (número = POL- + id con 8 dígitos; prima = precio de la cotización)
INSERT INTO poliza (id_poliza, id_contratacion, numero_poliza, fecha_inicio, fecha_fin,
                    prima, cobertura, deducible, condiciones, estado) VALUES
    (1, 1, 'POL-00000001', CURRENT_DATE - 40,  CURRENT_DATE + 325, 1850.00,
        'Daños propios por accidente, responsabilidad civil frente a terceros, robo total y asistencia en carretera 24 h.',
        500.00, 'Condiciones generales v1.0. Prima anual pagada en una sola cuota.', 'Vigente'),
    (2, 2, 'POL-00000002', CURRENT_DATE - 100, CURRENT_DATE + 265, 320.00,
        'Cobertura obligatoria por muerte, invalidez permanente, incapacidad temporal y gastos médicos y de sepelio.',
        0.00, 'Condiciones generales v1.0. Prima anual pagada en una sola cuota.', 'Vigente'),
    (3, 3, 'POL-00000003', CURRENT_DATE,       CURRENT_DATE + 365, 2400.00,
        'Fallecimiento por cualquier causa, invalidez total y permanente y anticipo por enfermedad terminal.',
        0.00, 'Condiciones generales v1.0. Pendiente de validación del cliente.', 'Recibida'),
    (4, 4, 'POL-00000004', CURRENT_DATE - 15,  CURRENT_DATE + 350, 780.00,
        'Incendio, sismo, robo con violencia, daños por agua y responsabilidad civil familiar.',
        150.00, 'Condiciones generales v1.0. Prima anual pagada en una sola cuota.', 'Vigente'),
    (5, 5, 'POL-00000005', CURRENT_DATE - 400, CURRENT_DATE - 35,  290.00,
        'Cobertura obligatoria por muerte, invalidez permanente, incapacidad temporal y gastos médicos y de sepelio.',
        0.00, 'Condiciones generales v1.0. Póliza vencida; puede solicitar la renovación.', 'Vencida');

-- Siniestros (cada cliente reporta con su propio vehículo; estados variados)
INSERT INTO siniestro (id_siniestro, id_usuario, id_aseguradora, numero_siniestro, fecha_registro, fecha_accidente,
                       placa, marca, modelo, anio, lugar, descripcion, resultado_evaluacion, observaciones,
                       estado, monto_aprobado, fecha_evaluacion, fecha_derivacion) VALUES
    (1, 8, 4, 'SIN-5A1C9E02', CURRENT_TIMESTAMP - INTERVAL '95 days', CURRENT_DATE - 96,
        'BKL-482', 'Toyota', 'Yaris', 2019, 'Av. Ejército con Av. Goyeneche, Arequipa',
        'Choque por alcance en semáforo en rojo. Daños en parachoques y maletero; sin heridos.',
        'Siniestro procedente. Reparación aprobada en taller afiliado a la aseguradora.',
        'Tipo de vehículo: Automóvil | Color: Plomo | Conductor: Lucía Fernández Rojas | DNI conductor: 45821367 | Licencia: Q45821367',
        'Cerrado', 1800.00, CURRENT_TIMESTAMP - INTERVAL '80 days', CURRENT_TIMESTAMP - INTERVAL '90 days'),
    (2, 6, 2, 'SIN-B07D41F3', CURRENT_TIMESTAMP - INTERVAL '70 days', CURRENT_DATE - 71,
        'V4C-205', 'Kia', 'Rio', 2021, 'Calle Mercaderes cuadra 2, Arequipa',
        'Rayón y abolladura en puerta lateral al estacionar; el responsable se retiró del lugar.',
        'Rechazado: los daños materiales propios no están cubiertos por el SOAT.',
        'Tipo de vehículo: Automóvil | Color: Rojo | Conductor: María Torres Quispe | DNI conductor: 40125687 | Licencia: Q40125687',
        'Rechazado', NULL, CURRENT_TIMESTAMP - INTERVAL '55 days', CURRENT_TIMESTAMP - INTERVAL '65 days'),
    (3, 7, 3, 'SIN-3E92AC18', CURRENT_TIMESTAMP - INTERVAL '50 days', CURRENT_DATE - 51,
        'D2M-764', 'Chevrolet', 'Sail', 2020, 'Av. Dolores y Av. Jesús, Arequipa',
        'Colisión lateral en intersección con otro vehículo. Daños en puerta y guardabarros delantero derecho.',
        'Aprobado. Monto autorizado para reparación y reposición de piezas.',
        'Tipo de vehículo: Automóvil | Color: Blanco | Conductor: Jorge Mamani Huamán | DNI conductor: 43087712 | Licencia: Q43087712',
        'Aprobado', 3200.00, CURRENT_TIMESTAMP - INTERVAL '35 days', CURRENT_TIMESTAMP - INTERVAL '45 days'),
    (4, 8, 4, 'SIN-C4F60B7D', CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_DATE - 21,
        'BKL-482', 'Toyota', 'Yaris', 2019, 'Variante de Uchumayo km 3, Arequipa',
        'Impacto contra baranda de contención por pista mojada. Daños en parte frontal y faro derecho.',
        NULL,
        'Tipo de vehículo: Automóvil | Color: Plomo | Conductor: Lucía Fernández Rojas | DNI conductor: 45821367 | Licencia: Q45821367',
        'En evaluación', NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '14 days'),
    (5, 7, 3, 'SIN-91D8E2A5', CURRENT_TIMESTAMP - INTERVAL '12 days', CURRENT_DATE - 13,
        'D2M-764', 'Chevrolet', 'Sail', 2020, 'Av. Venezuela cuadra 8, Arequipa',
        'Rotura de luna delantera por proyección de piedra en carretera.',
        NULL,
        'Tipo de vehículo: Automóvil | Color: Blanco | Conductor: Jorge Mamani Huamán | DNI conductor: 43087712',
        'Derivado', NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '9 days'),
    (6, 6, 2, 'SIN-7A35F0C6', CURRENT_TIMESTAMP - INTERVAL '6 days', CURRENT_DATE - 7,
        'V4C-205', 'Kia', 'Rio', 2021, 'Av. Parra con Av. Salaverry, Arequipa',
        'Choque leve en parachoques posterior en hora punta; se intercambiaron datos con el otro conductor.',
        NULL,
        'Tipo de vehículo: Automóvil | Color: Rojo | Conductor: María Torres Quispe | DNI conductor: 40125687 | Licencia: Q40125687',
        'En revisión', NULL, NULL, NULL),
    (7, 3, 1, 'SIN-E18B94D2', CURRENT_TIMESTAMP - INTERVAL '4 days', CURRENT_DATE - 5,
        'C9L-123', 'Toyota', 'Corolla', 2020, 'Av. Independencia cuadra 3, Arequipa',
        'Colisión frontal leve contra poste durante maniobra de retroceso.',
        NULL,
        'Tipo de vehículo: Automóvil | Color: Negro | Conductor: Carlos Cliente Prueba | DNI conductor: 12345678 | Se solicitó adjuntar fotos y parte policial',
        'Información pendiente', NULL, NULL, NULL),
    (8, 3, 1, 'SIN-2F6C07A9', CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_DATE - 3,
        'C9L-123', 'Toyota', 'Corolla', 2020, 'Av. Ejército cuadra 10, Arequipa',
        'Choque por alcance en vía de alto tránsito. Daños en parachoques trasero y luz posterior izquierda.',
        NULL,
        'Tipo de vehículo: Automóvil | Color: Negro | Conductor: Carlos Cliente Prueba | DNI conductor: 12345678 | Licencia: Q12345678',
        'Registrado', NULL, NULL, NULL);

-- Evidencias del siniestro 8. Las rutas apuntan a las 5 imágenes que YA existen en
-- uploads/siniestros/8/ del proyecto, por lo que se pueden abrir/descargar desde la app.
INSERT INTO documento (id_documento, id_usuario, id_siniestro, tipo_documento, nombre_archivo,
                       ruta_archivo, version, fecha_carga, estado, observacion) VALUES
    (1, 3, 8, 'FOTO_ACCIDENTE', 'danio_frontal.png',
        'uploads/siniestros/8/aa74f28a015f4505821f9ee043ae380c.png', 1,
        CURRENT_TIMESTAMP - INTERVAL '2 days', 'Validado', NULL),
    (2, 3, 8, 'FOTO_ACCIDENTE', 'danio_lateral_izquierdo.png',
        'uploads/siniestros/8/526bc318c2df459cbae50477a199a6b3.png', 1,
        CURRENT_TIMESTAMP - INTERVAL '2 days', 'Pendiente', NULL),
    (3, 3, 8, 'FOTO_ACCIDENTE', 'danio_lateral_derecho.png',
        'uploads/siniestros/8/87725371cd8b482f855145b986837eda.png', 1,
        CURRENT_TIMESTAMP - INTERVAL '2 days', 'Pendiente', NULL),
    (4, 3, 8, 'FOTO_ACCIDENTE', 'danio_posterior.png',
        'uploads/siniestros/8/16ab4a4493744aaba5930269afafd189.png', 1,
        CURRENT_TIMESTAMP - INTERVAL '2 days', 'Observado',
        'La imagen no muestra la placa; adjunte una toma donde se vea.'),
    (5, 3, 8, 'FOTO_ACCIDENTE', 'placa_y_tablero.png',
        'uploads/siniestros/8/165223038b994e5090a39a2897291278.png', 1,
        CURRENT_TIMESTAMP - INTERVAL '2 days', 'Pendiente', NULL);

-- Simulaciones AFP (pensión = ingreso x años de aporte x 0.005, como en PrevisionService)
INSERT INTO prevision (id_prevision, id_usuario, tipo_sistema, edad, ingreso_mensual, anios_aporte,
                       fecha_registro, monto_pension_estimado, resultado_simulacion, consulta, respuesta, estado) VALUES
    (1, 3, 'AFP', 35, 4500.00, 10, CURRENT_TIMESTAMP - INTERVAL '25 days', 225.00,
        'Simulación AFP referencial. Edad: 35 años. Años de aporte registrados: 10. Pensión mensual estimada: S/ 225.00. Este resultado es únicamente una simulación académica y no constituye una proyección oficial de una AFP.',
        NULL, NULL, 'Simulada'),
    (2, 6, 'AFP', 42, 3200.00, 15, CURRENT_TIMESTAMP - INTERVAL '12 days', 240.00,
        'Simulación AFP referencial. Edad: 42 años. Años de aporte registrados: 15. Pensión mensual estimada: S/ 240.00. Este resultado es únicamente una simulación académica y no constituye una proyección oficial de una AFP.',
        'Quisiera saber si me conviene realizar aportes voluntarios para mejorar mi pensión.', NULL, 'En revisión'),
    (3, 7, 'AFP', 50, 5000.00, 25, CURRENT_TIMESTAMP - INTERVAL '40 days', 625.00,
        'Simulación AFP referencial. Edad: 50 años. Años de aporte registrados: 25. Pensión mensual estimada: S/ 625.00. Este resultado es únicamente una simulación académica y no constituye una proyección oficial de una AFP.',
        'Tengo aportes en dos AFP distintas. ¿Cómo afecta esto a mi pensión?',
        'Gracias por su consulta. Para evaluar el efecto en su pensión necesitamos los estados de cuenta de ambas AFP. Un asesor se comunicará con usted para orientarle sobre las alternativas disponibles.',
        'Respondida'),
    (4, 8, 'AFP', 29, 2800.00, 5, CURRENT_TIMESTAMP - INTERVAL '8 days', 70.00,
        'Simulación AFP referencial. Edad: 29 años. Años de aporte registrados: 5. Pensión mensual estimada: S/ 70.00. Este resultado es únicamente una simulación académica y no constituye una proyección oficial de una AFP.',
        NULL, NULL, 'Simulada'),
    (5, 3, 'AFP', 58, 3900.00, 30, CURRENT_TIMESTAMP - INTERVAL '70 days', 585.00,
        'Simulación AFP referencial. Edad: 58 años. Años de aporte registrados: 30. Pensión mensual estimada: S/ 585.00. Este resultado es únicamente una simulación académica y no constituye una proyección oficial de una AFP.',
        'Estoy próximo a jubilarme. ¿A qué edad puedo solicitar mi jubilación y qué opciones tengo?',
        'A partir de los 65 años puede solicitar su jubilación ordinaria. Existen distintas modalidades de pensión según su caso; le sugerimos agendar una cita para revisarlo con sus estados de cuenta.',
        'Cerrada');

-- Historial / trazabilidad (generado con los mismos montos, referencias y números de arriba)
INSERT INTO historial (id_usuario, entidad, id_registro, accion, valor_anterior, valor_nuevo, fecha_hora) VALUES
    (1, 'Usuario', 4, 'CAMBIO_ROL', 'Cliente', 'Asesor', CURRENT_TIMESTAMP - INTERVAL '439 days'),
    (1, 'Usuario', 5, 'CAMBIO_ROL', 'Cliente', 'Asesor', CURRENT_TIMESTAMP - INTERVAL '437 days'),
    (3, 'COTIZACION', 5, 'CREAR', NULL, 'Cotización registrada', CURRENT_TIMESTAMP - INTERVAL '405 days'),
    (3, 'COTIZACION', 5, 'ACTUALIZAR', 'Estado: Solicitada', 'Estado: Disponible', CURRENT_TIMESTAMP - INTERVAL '404 days'),
    (3, 'Pago', 5, 'Pago aprobado', NULL, 'Yape (simulado) - PAG-0E7F34B9D1A8 - S/ 290.00 - contratación 5', CURRENT_TIMESTAMP - INTERVAL '402 days'),
    (3, 'POLIZA', 5, 'CREAR', NULL, 'Póliza POL-00000005 generada | Estado: Recibida | Prima: S/ 290.00', CURRENT_TIMESTAMP - INTERVAL '400 days'),
    (6, 'COTIZACION', 2, 'CREAR', NULL, 'Cotización registrada', CURRENT_TIMESTAMP - INTERVAL '102 days'),
    (6, 'COTIZACION', 2, 'ACTUALIZAR', 'Estado: Solicitada', 'Estado: Disponible', CURRENT_TIMESTAMP - INTERVAL '101 days'),
    (6, 'Pago', 2, 'Pago aprobado', NULL, 'Yape (simulado) - PAG-1D9E6B52A8F0 - S/ 320.00 - contratación 2', CURRENT_TIMESTAMP - INTERVAL '100 days'),
    (6, 'POLIZA', 2, 'CREAR', NULL, 'Póliza POL-00000002 generada | Estado: Recibida | Prima: S/ 320.00', CURRENT_TIMESTAMP - INTERVAL '100 days'),
    (8, 'SINIESTRO', 1, 'CREAR', NULL, 'Siniestro SIN-5A1C9E02 registrado | Estado: Registrado | Aseguradora: Mapfre Perú', CURRENT_TIMESTAMP - INTERVAL '95 days'),
    (6, 'SINIESTRO', 2, 'CREAR', NULL, 'Siniestro SIN-B07D41F3 registrado | Estado: Registrado | Aseguradora: Pacífico Seguros', CURRENT_TIMESTAMP - INTERVAL '70 days'),
    (8, 'COTIZACION', 10, 'CREAR', NULL, 'Cotización registrada', CURRENT_TIMESTAMP - INTERVAL '60 days'),
    (8, 'COTIZACION', 10, 'ACTUALIZAR', 'Estado: Solicitada', 'Estado: Disponible', CURRENT_TIMESTAMP - INTERVAL '58 days'),
    (7, 'SINIESTRO', 3, 'CREAR', NULL, 'Siniestro SIN-3E92AC18 registrado | Estado: Registrado | Aseguradora: La Positiva', CURRENT_TIMESTAMP - INTERVAL '50 days'),
    (3, 'COTIZACION', 1, 'CREAR', NULL, 'Cotización registrada', CURRENT_TIMESTAMP - INTERVAL '45 days'),
    (3, 'COTIZACION', 1, 'ACTUALIZAR', 'Estado: Solicitada', 'Estado: Disponible', CURRENT_TIMESTAMP - INTERVAL '43 days'),
    (3, 'Pago', 8, 'Pago rechazado', NULL, 'Tarjeta (simulado) - PAG-F4A7192D8E35 - Fondos insuficientes.', CURRENT_TIMESTAMP - INTERVAL '42 days'),
    (3, 'Pago', 1, 'Pago aprobado', NULL, 'Tarjeta (simulado) - PAG-8F3A21C07B4D - S/ 1850.00 - contratación 1', CURRENT_TIMESTAMP - INTERVAL '41 days'),
    (3, 'POLIZA', 1, 'CREAR', NULL, 'Póliza POL-00000001 generada | Estado: Recibida | Prima: S/ 1850.00', CURRENT_TIMESTAMP - INTERVAL '40 days'),
    (7, 'COTIZACION', 9, 'CREAR', NULL, 'Cotización registrada', CURRENT_TIMESTAMP - INTERVAL '30 days'),
    (7, 'COTIZACION', 9, 'ACTUALIZAR', 'Estado: Solicitada', 'Estado: Disponible', CURRENT_TIMESTAMP - INTERVAL '28 days'),
    (8, 'COTIZACION', 10, 'ACTUALIZAR', 'Estado: Disponible', 'Estado: Vencida', CURRENT_TIMESTAMP - INTERVAL '28 days'),
    (7, 'COTIZACION', 9, 'ACTUALIZAR', 'Estado: Disponible', 'Estado: Rechazada', CURRENT_TIMESTAMP - INTERVAL '27 days'),
    (8, 'SINIESTRO', 4, 'CREAR', NULL, 'Siniestro SIN-C4F60B7D registrado | Estado: Registrado | Aseguradora: Mapfre Perú', CURRENT_TIMESTAMP - INTERVAL '20 days'),
    (8, 'COTIZACION', 4, 'CREAR', NULL, 'Cotización registrada', CURRENT_TIMESTAMP - INTERVAL '18 days'),
    (8, 'COTIZACION', 4, 'ACTUALIZAR', 'Estado: Solicitada', 'Estado: Disponible', CURRENT_TIMESTAMP - INTERVAL '17 days'),
    (8, 'Pago', 4, 'Pago aprobado', NULL, 'Tarjeta (simulado) - PAG-A52B8D19E6C3 - S/ 780.00 - contratación 4', CURRENT_TIMESTAMP - INTERVAL '16 days'),
    (8, 'POLIZA', 4, 'CREAR', NULL, 'Póliza POL-00000004 generada | Estado: Recibida | Prima: S/ 780.00', CURRENT_TIMESTAMP - INTERVAL '15 days'),
    (7, 'SINIESTRO', 5, 'CREAR', NULL, 'Siniestro SIN-91D8E2A5 registrado | Estado: Registrado | Aseguradora: La Positiva', CURRENT_TIMESTAMP - INTERVAL '12 days'),
    (7, 'COTIZACION', 12, 'CREAR', NULL, 'Cotización registrada', CURRENT_TIMESTAMP - INTERVAL '9 days'),
    (7, 'COTIZACION', 12, 'ACTUALIZAR', 'Estado: Solicitada', 'Estado: Disponible', CURRENT_TIMESTAMP - INTERVAL '8 days'),
    (7, 'Pago', 7, 'Pago rechazado', NULL, 'Plin (simulado) - PAG-39B6E0F4C2A1 - Saldo insuficiente en la billetera.', CURRENT_TIMESTAMP - INTERVAL '7 days'),
    (7, 'COTIZACION', 3, 'CREAR', NULL, 'Cotización registrada', CURRENT_TIMESTAMP - INTERVAL '6 days'),
    (7, 'Pago', 6, 'Pago aprobado', NULL, 'Tarjeta (simulado) - PAG-D18C5A6F2B70 - S/ 2100.00 - contratación 6', CURRENT_TIMESTAMP - INTERVAL '6 days'),
    (6, 'SINIESTRO', 6, 'CREAR', NULL, 'Siniestro SIN-7A35F0C6 registrado | Estado: Registrado | Aseguradora: Pacífico Seguros', CURRENT_TIMESTAMP - INTERVAL '6 days'),
    (7, 'COTIZACION', 3, 'ACTUALIZAR', 'Estado: Solicitada', 'Estado: Disponible', CURRENT_TIMESTAMP - INTERVAL '5 days'),
    (3, 'COTIZACION', 11, 'CREAR', NULL, 'Cotización registrada', CURRENT_TIMESTAMP - INTERVAL '5 days'),
    (3, 'COTIZACION', 6, 'CREAR', NULL, 'Cotización registrada', CURRENT_TIMESTAMP - INTERVAL '4 days'),
    (3, 'SINIESTRO', 7, 'CREAR', NULL, 'Siniestro SIN-E18B94D2 registrado | Estado: Registrado | Aseguradora: Rímac Seguros', CURRENT_TIMESTAMP - INTERVAL '4 days'),
    (6, 'COTIZACION', 8, 'CREAR', NULL, 'Cotización registrada', CURRENT_TIMESTAMP - INTERVAL '3 days'),
    (3, 'COTIZACION', 11, 'ACTUALIZAR', 'Estado: Solicitada', 'Estado: Disponible', CURRENT_TIMESTAMP - INTERVAL '3 days'),
    (7, 'Pago', 3, 'Pago aprobado', NULL, 'Plin (simulado) - PAG-6C40F7E3925A - S/ 2400.00 - contratación 3', CURRENT_TIMESTAMP - INTERVAL '3 days'),
    (3, 'COTIZACION', 6, 'ACTUALIZAR', 'Estado: Solicitada', 'Estado: Disponible', CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (6, 'COTIZACION', 8, 'ACTUALIZAR', 'Estado: Solicitada', 'Estado: En revisión', CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (7, 'POLIZA', 3, 'CREAR', NULL, 'Póliza POL-00000003 generada | Estado: Recibida | Prima: S/ 2400.00', CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (3, 'SINIESTRO', 8, 'CREAR', NULL, 'Siniestro SIN-2F6C07A9 registrado | Estado: Registrado | Aseguradora: Rímac Seguros', CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (3, 'DOCUMENTO', 1, 'CREAR', NULL, 'Documento cargado | Tipo: FOTO_ACCIDENTE | Archivo: danio_frontal.png | Estado: Pendiente | Siniestro: 8', CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (3, 'DOCUMENTO', 2, 'CREAR', NULL, 'Documento cargado | Tipo: FOTO_ACCIDENTE | Archivo: danio_lateral_izquierdo.png | Estado: Pendiente | Siniestro: 8', CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (3, 'DOCUMENTO', 3, 'CREAR', NULL, 'Documento cargado | Tipo: FOTO_ACCIDENTE | Archivo: danio_lateral_derecho.png | Estado: Pendiente | Siniestro: 8', CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (3, 'DOCUMENTO', 4, 'CREAR', NULL, 'Documento cargado | Tipo: FOTO_ACCIDENTE | Archivo: danio_posterior.png | Estado: Pendiente | Siniestro: 8', CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (3, 'DOCUMENTO', 5, 'CREAR', NULL, 'Documento cargado | Tipo: FOTO_ACCIDENTE | Archivo: placa_y_tablero.png | Estado: Pendiente | Siniestro: 8', CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (3, 'COTIZACION', 7, 'CREAR', NULL, 'Cotización registrada', CURRENT_TIMESTAMP - INTERVAL '1 days');

-- ------------------- FIN DE LOS DATOS DE PRUEBA -----------------------

-- ---------------------------------------------------------------------
-- 5) AJUSTE DE SECUENCIAS
--    Los datos se insertaron con id explícito; así el próximo registro que
--    cree la aplicación recibe el siguiente id libre (sin choques de clave).
-- ---------------------------------------------------------------------
SELECT setval(pg_get_serial_sequence('rol',                      'id_rol'),          COALESCE(MAX(id_rol), 0) + 1,          false) FROM rol;
SELECT setval(pg_get_serial_sequence('usuario',                  'id_usuario'),      COALESCE(MAX(id_usuario), 0) + 1,      false) FROM usuario;
SELECT setval(pg_get_serial_sequence('tipo_seguro',              'id_tipo_seguro'),  COALESCE(MAX(id_tipo_seguro), 0) + 1,  false) FROM tipo_seguro;
SELECT setval(pg_get_serial_sequence('aseguradora',              'id_aseguradora'),  COALESCE(MAX(id_aseguradora), 0) + 1,  false) FROM aseguradora;
SELECT setval(pg_get_serial_sequence('cotizacion',               'id_cotizacion'),   COALESCE(MAX(id_cotizacion), 0) + 1,   false) FROM cotizacion;
SELECT setval(pg_get_serial_sequence('contratacion',             'id_contratacion'), COALESCE(MAX(id_contratacion), 0) + 1, false) FROM contratacion;
SELECT setval(pg_get_serial_sequence('revision_legal',           'id_revision'),     COALESCE(MAX(id_revision), 0) + 1,     false) FROM revision_legal;
SELECT setval(pg_get_serial_sequence('poliza',                   'id_poliza'),       COALESCE(MAX(id_poliza), 0) + 1,       false) FROM poliza;
SELECT setval(pg_get_serial_sequence('siniestro',                'id_siniestro'),    COALESCE(MAX(id_siniestro), 0) + 1,    false) FROM siniestro;
SELECT setval(pg_get_serial_sequence('prevision',                'id_prevision'),    COALESCE(MAX(id_prevision), 0) + 1,    false) FROM prevision;
SELECT setval(pg_get_serial_sequence('documento',                'id_documento'),    COALESCE(MAX(id_documento), 0) + 1,    false) FROM documento;
SELECT setval(pg_get_serial_sequence('historial',                'id_historial'),    COALESCE(MAX(id_historial), 0) + 1,    false) FROM historial;
SELECT setval(pg_get_serial_sequence('pago',                     'id_pago'),         COALESCE(MAX(id_pago), 0) + 1,         false) FROM pago;
SELECT setval(pg_get_serial_sequence('comunicacion_aseguradora', 'id_comunicacion'), COALESCE(MAX(id_comunicacion), 0) + 1, false) FROM comunicacion_aseguradora;
SELECT setval(pg_get_serial_sequence('notificacion',             'id_notificacion'), COALESCE(MAX(id_notificacion), 0) + 1, false) FROM notificacion;

COMMIT;

-- ---------------------------------------------------------------------
-- 6) VERIFICACIÓN (solo lectura)
-- ---------------------------------------------------------------------
-- Filas por tabla
SELECT 'rol' AS tabla, COUNT(*) AS filas FROM rol
UNION ALL SELECT 'usuario',      COUNT(*) FROM usuario
UNION ALL SELECT 'tipo_seguro',  COUNT(*) FROM tipo_seguro
UNION ALL SELECT 'aseguradora',  COUNT(*) FROM aseguradora
UNION ALL SELECT 'rol_permiso',  COUNT(*) FROM rol_permiso
UNION ALL SELECT 'cotizacion',   COUNT(*) FROM cotizacion
UNION ALL SELECT 'contratacion', COUNT(*) FROM contratacion
UNION ALL SELECT 'pago',         COUNT(*) FROM pago
UNION ALL SELECT 'poliza',       COUNT(*) FROM poliza
UNION ALL SELECT 'siniestro',    COUNT(*) FROM siniestro
UNION ALL SELECT 'documento',    COUNT(*) FROM documento
UNION ALL SELECT 'prevision',    COUNT(*) FROM prevision
UNION ALL SELECT 'historial',    COUNT(*) FROM historial;

-- Cuentas de acceso con su rol
SELECT u.id_usuario, u.correo, r.nombre AS rol, u.estado
FROM usuario u JOIN rol r ON r.id_rol = u.id_rol
ORDER BY u.id_usuario;
