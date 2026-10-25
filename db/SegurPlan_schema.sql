-- =====================================================================
-- SEGURPLAN v5 - Script de instalación COMPLETO (PostgreSQL 14+)
--   createdb -U postgres SegurPlan
--   psql -U postgres -d SegurPlan -f db/SegurPlan_schema.sql
--   psql -U postgres -d SegurPlan -f db/datos_iniciales.sql
-- Si la base YA existe de una versión anterior use db/migracion_v5.sql
-- =====================================================================

CREATE TABLE rol (
    id_rol SERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    descripcion VARCHAR(150)
);

CREATE TABLE usuario (
    id_usuario BIGSERIAL PRIMARY KEY,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    correo VARCHAR(150) NOT NULL UNIQUE,
    telefono VARCHAR(20),
    password VARCHAR(255) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'Activo',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_rol INT NOT NULL,
    CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol) REFERENCES rol(id_rol),
    CONSTRAINT chk_usuario_estado CHECK (estado IN ('Activo', 'Inactivo', 'Bloqueado'))
);


CREATE TABLE tipo_seguro (
    id_tipo_seguro SERIAL PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL UNIQUE,
    descripcion VARCHAR(200),
    estado VARCHAR(20) NOT NULL DEFAULT 'Activo',
    CONSTRAINT chk_tipo_seguro_estado CHECK (estado IN ('Activo','Inactivo' ))
);


CREATE TABLE aseguradora (
    id_aseguradora SERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    ruc VARCHAR(20) UNIQUE,
    telefono VARCHAR(20),
    correo VARCHAR(150),
    estado VARCHAR(20) NOT NULL DEFAULT 'Activo',
    CONSTRAINT chk_aseguradora_estado CHECK (estado IN ('Activo', 'Inactivo'))
);


CREATE TABLE cotizacion (
    id_cotizacion BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL,
    id_tipo_seguro INT NOT NULL,
    id_aseguradora INT NOT NULL,
    -- Datos de la solicitud
    fecha_solicitud TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    descripcion_solicitud TEXT,
    -- Datos de la cotización
    precio NUMERIC(12,2),
    vigencia DATE,
    cobertura TEXT,
    deducible NUMERIC(12,2),
    estado VARCHAR(30) NOT NULL DEFAULT 'Solicitada',
    fecha_respuesta TIMESTAMP,
    CONSTRAINT fk_cotizacion_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario),
    CONSTRAINT fk_cotizacion_tipo_seguro FOREIGN KEY (id_tipo_seguro) REFERENCES tipo_seguro(id_tipo_seguro),
    CONSTRAINT fk_cotizacion_aseguradora FOREIGN KEY (id_aseguradora) REFERENCES aseguradora(id_aseguradora),
    CONSTRAINT chk_cotizacion_precio CHECK (precio IS NULL OR precio >= 0),
    CONSTRAINT chk_cotizacion_deducible CHECK (deducible IS NULL OR deducible >= 0),
    CONSTRAINT chk_cotizacion_estado CHECK (estado IN (
            'Solicitada',
            'En revisión',
            'Disponible',
            'Aceptada',
            'Rechazada',
            'Vencida'
        ))
);



CREATE TABLE contratacion (
    id_contratacion BIGSERIAL PRIMARY KEY,
    id_cotizacion BIGINT NOT NULL UNIQUE,
    id_usuario BIGINT NOT NULL,
    fecha_contratacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Evidencia de aceptación de condiciones
    version_condiciones VARCHAR(30),
    condiciones_aceptadas BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_aceptacion TIMESTAMP,
    estado VARCHAR(40) NOT NULL DEFAULT 'Iniciada',
    observaciones TEXT,
    CONSTRAINT fk_contratacion_cotizacion FOREIGN KEY (id_cotizacion) REFERENCES cotizacion(id_cotizacion),
    CONSTRAINT fk_contratacion_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario),
    CONSTRAINT chk_contratacion_estado CHECK (estado IN (
            'Iniciada',
            'Documentación pendiente',
            'Documentación validada',
            'En revisión legal',
            'Enviada a aseguradora',
            'En evaluación',
            'Aprobada',
            'Rechazada',
            'Finalizada'
        ))
);



CREATE TABLE revision_legal (
    id_revision BIGSERIAL PRIMARY KEY,
    id_contratacion BIGINT NOT NULL,
    id_responsable BIGINT NOT NULL,
    fecha_revision TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resultado VARCHAR(30) NOT NULL DEFAULT 'Pendiente',
    observaciones TEXT,
    contrato_conforme BOOLEAN,
    estado_observacion VARCHAR(30) DEFAULT 'Pendiente',
    fecha_cierre TIMESTAMP,
    CONSTRAINT fk_revision_contratacion FOREIGN KEY (id_contratacion) REFERENCES contratacion(id_contratacion),
    CONSTRAINT fk_revision_responsable FOREIGN KEY (id_responsable) REFERENCES usuario(id_usuario),
    CONSTRAINT chk_revision_resultado CHECK (resultado IN (
            'Pendiente',
            'Conforme',
            'Observado',
            'Rechazado'
        )),
    CONSTRAINT chk_revision_observacion CHECK (estado_observacion IN (
            'Pendiente',
            'Atendida',
            'Cerrada'
        ))
);



CREATE TABLE poliza (
    id_poliza BIGSERIAL PRIMARY KEY,
    id_contratacion BIGINT NOT NULL UNIQUE,
    numero_poliza VARCHAR(80) NOT NULL UNIQUE,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    prima NUMERIC(12,2) NOT NULL,
    cobertura TEXT NOT NULL,
    deducible NUMERIC(12,2),
    condiciones TEXT,
    estado VARCHAR(30) NOT NULL DEFAULT 'Recibida',
    CONSTRAINT fk_poliza_contratacion FOREIGN KEY (id_contratacion) REFERENCES contratacion(id_contratacion),
    CONSTRAINT chk_poliza_prima CHECK (prima > 0),
    CONSTRAINT chk_poliza_deducible CHECK (deducible IS NULL OR deducible >= 0),
    CONSTRAINT chk_poliza_fechas CHECK (fecha_fin > fecha_inicio),
    CONSTRAINT chk_poliza_estado CHECK (estado IN (
            'Recibida',
            'En validación',
            'Validada',
            'Observada',
            'Entregada',
            'Vigente',
            'Vencida'
        ))
);


CREATE TABLE siniestro (
    id_siniestro BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL,
    id_aseguradora INT NOT NULL,
    numero_siniestro VARCHAR(80),
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_accidente TIMESTAMP,
    -- Datos del vehículo
    placa VARCHAR(15),
    marca VARCHAR(60),
    modelo VARCHAR(60),
    anio INT,
    -- Datos del accidente
    lugar VARCHAR(200),
    descripcion TEXT,
    -- Seguimiento
    resultado_evaluacion TEXT,
    observaciones TEXT,
    estado VARCHAR(40) NOT NULL DEFAULT 'Registrado',
    CONSTRAINT fk_siniestro_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario),
    CONSTRAINT fk_siniestro_aseguradora FOREIGN KEY (id_aseguradora) REFERENCES aseguradora(id_aseguradora),
    CONSTRAINT chk_siniestro_estado CHECK (estado IN (
            'Registrado',
            'En revisión',
            'Información pendiente',
            'Derivado',
            'En evaluación',
            'Observado',
            'Aprobado',
            'Rechazado',
            'Cerrado'
        )),
    CONSTRAINT chk_siniestro_anio CHECK (anio IS NULL OR anio >= 1900)
);


CREATE TABLE prevision (
    id_prevision BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL,
    tipo_sistema VARCHAR(20) NOT NULL,
    edad INT,
    ingreso_mensual NUMERIC(12,2),
    anios_aporte INT,
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Datos de simulación
    monto_pension_estimado NUMERIC(12,2),
    resultado_simulacion TEXT,
    -- Orientación
    consulta TEXT,
    respuesta TEXT,
    estado VARCHAR(30) NOT NULL DEFAULT 'Registrada',
    CONSTRAINT fk_prevision_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario),
    CONSTRAINT chk_prevision_tipo CHECK (tipo_sistema IN ( 'AFP','ONP')),
    CONSTRAINT chk_prevision_edad CHECK (edad IS NULL OR edad > 0),
    CONSTRAINT chk_prevision_ingreso CHECK (ingreso_mensual IS NULL OR ingreso_mensual >= 0),
    CONSTRAINT chk_prevision_aporte CHECK (anios_aporte IS NULL OR anios_aporte >= 0),
    CONSTRAINT chk_prevision_pension CHECK (monto_pension_estimado IS NULL OR monto_pension_estimado >= 0),
    CONSTRAINT chk_prevision_estado
        CHECK (estado IN (
            'Registrada',
            'En revisión',
            'Simulada',
            'Respondida',
            'Cerrada'
        ))
);


CREATE TABLE documento (
    id_documento BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL,
    id_contratacion BIGINT,
    id_siniestro BIGINT,
    tipo_documento VARCHAR(80) NOT NULL,
    nombre_archivo VARCHAR(200) NOT NULL,
    ruta_archivo TEXT NOT NULL,
    version INT NOT NULL DEFAULT 1,
    fecha_carga TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado VARCHAR(30) NOT NULL DEFAULT 'Pendiente',
    CONSTRAINT fk_documento_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario),
    CONSTRAINT fk_documento_contratacion FOREIGN KEY (id_contratacion) REFERENCES contratacion(id_contratacion),
    CONSTRAINT fk_documento_siniestro FOREIGN KEY (id_siniestro) REFERENCES siniestro(id_siniestro),
    CONSTRAINT chk_documento_version CHECK (version > 0),
    CONSTRAINT chk_documento_estado
        CHECK (estado IN (
            'Pendiente',
            'Validado',
            'Observado',
            'Rechazado'
        )),
    CONSTRAINT chk_documento_referencia CHECK ( id_contratacion IS NOT NULL OR id_siniestro IS NOT NULL )
);


CREATE TABLE historial (
    id_historial BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT,
    entidad VARCHAR(50) NOT NULL,
    id_registro BIGINT NOT NULL,
    accion VARCHAR(50) NOT NULL,
    valor_anterior TEXT,
    valor_nuevo TEXT,
    fecha_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_historial_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
);

-- Pago simulado de la contratación
CREATE TABLE pago (
    id_pago BIGSERIAL PRIMARY KEY,
    id_cotizacion BIGINT NOT NULL,
    id_contratacion BIGINT,
    id_usuario BIGINT NOT NULL,
    metodo VARCHAR(30) NOT NULL DEFAULT 'Tarjeta (simulado)',
    monto NUMERIC(12,2) NOT NULL,
    moneda VARCHAR(5) NOT NULL DEFAULT 'PEN',
    estado VARCHAR(20) NOT NULL,
    referencia VARCHAR(40) NOT NULL UNIQUE,
    tarjeta_ultimos4 VARCHAR(4),
    titular VARCHAR(120),
    motivo_rechazo VARCHAR(200),
    celular_origen VARCHAR(15),
    numero_operacion VARCHAR(20),
    destino VARCHAR(20),
    fecha_pago TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pago_cotizacion FOREIGN KEY (id_cotizacion) REFERENCES cotizacion(id_cotizacion),
    CONSTRAINT fk_pago_contratacion FOREIGN KEY (id_contratacion) REFERENCES contratacion(id_contratacion),
    CONSTRAINT fk_pago_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario),
    CONSTRAINT chk_pago_monto CHECK (monto > 0),
    CONSTRAINT chk_pago_estado CHECK (estado IN ('Aprobado', 'Rechazado'))
);
CREATE UNIQUE INDEX ux_pago_cotizacion_aprobado ON pago (id_cotizacion) WHERE estado = 'Aprobado';

-- =============== Extensiones v5 (idéntico a migracion_v5.sql) ===============
BEGIN;

-- ---- Usuario: perfil (RF003) y especialidad de asesor (RF029) --------
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS direccion          VARCHAR(200);
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS fecha_modificacion TIMESTAMP;
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS area_especialidad  VARCHAR(30);

-- ---- Cotización: formulario dinámico por tipo de seguro (RF004) ------
ALTER TABLE cotizacion ADD COLUMN IF NOT EXISTS datos_json TEXT;

-- ---- Siniestro: evaluación de la aseguradora (RF024/RF025) ----------
ALTER TABLE siniestro ADD COLUMN IF NOT EXISTS monto_aprobado   NUMERIC(12,2);
ALTER TABLE siniestro ADD COLUMN IF NOT EXISTS fecha_evaluacion TIMESTAMP;
ALTER TABLE siniestro ADD COLUMN IF NOT EXISTS fecha_derivacion TIMESTAMP;
ALTER TABLE siniestro DROP CONSTRAINT IF EXISTS chk_siniestro_estado;
ALTER TABLE siniestro ADD CONSTRAINT chk_siniestro_estado CHECK (estado IN (
    'Registrado','En revisión','Información pendiente','Información completa',
    'Derivado','En evaluación','Evaluación recibida','Observado','Aprobado',
    'Rechazado','Cerrado'));
ALTER TABLE siniestro DROP CONSTRAINT IF EXISTS chk_siniestro_monto;
ALTER TABLE siniestro ADD CONSTRAINT chk_siniestro_monto
    CHECK (monto_aprobado IS NULL OR monto_aprobado >= 0);

-- ---- Documento: adjuntos de cotización y observación del asesor ------
ALTER TABLE documento ADD COLUMN IF NOT EXISTS id_cotizacion BIGINT;
ALTER TABLE documento ADD COLUMN IF NOT EXISTS observacion   TEXT;
ALTER TABLE documento DROP CONSTRAINT IF EXISTS fk_documento_cotizacion;
ALTER TABLE documento ADD CONSTRAINT fk_documento_cotizacion
    FOREIGN KEY (id_cotizacion) REFERENCES cotizacion(id_cotizacion);
ALTER TABLE documento DROP CONSTRAINT IF EXISTS chk_documento_referencia;
ALTER TABLE documento ADD CONSTRAINT chk_documento_referencia CHECK (
    id_contratacion IS NOT NULL OR id_siniestro IS NOT NULL OR id_cotizacion IS NOT NULL);

-- ---- Comunicación del asesor con la aseguradora (RF005) -------------
CREATE TABLE IF NOT EXISTS comunicacion_aseguradora (
    id_comunicacion BIGSERIAL PRIMARY KEY,
    id_cotizacion   BIGINT NOT NULL REFERENCES cotizacion(id_cotizacion),
    id_asesor       BIGINT NOT NULL REFERENCES usuario(id_usuario),
    fecha           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    medio           VARCHAR(30) NOT NULL DEFAULT 'Correo',
    contacto        VARCHAR(150),
    observaciones   TEXT NOT NULL
);

-- ---- Notificaciones internas (RF032) --------------------------------
CREATE TABLE IF NOT EXISTS notificacion (
    id_notificacion BIGSERIAL PRIMARY KEY,
    id_usuario      BIGINT NOT NULL REFERENCES usuario(id_usuario),
    tipo            VARCHAR(40) NOT NULL,
    titulo          VARCHAR(150) NOT NULL,
    mensaje         TEXT,
    entidad         VARCHAR(40),
    id_registro     BIGINT,
    leida           BOOLEAN NOT NULL DEFAULT FALSE,
    fecha           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS ix_notificacion_usuario ON notificacion (id_usuario, leida, fecha DESC);

-- ---- Permisos por rol y módulo (RF030) ------------------------------
CREATE TABLE IF NOT EXISTS rol_permiso (
    id_rol   INT NOT NULL REFERENCES rol(id_rol),
    modulo   VARCHAR(40) NOT NULL,
    ver      BOOLEAN NOT NULL DEFAULT FALSE,
    crear    BOOLEAN NOT NULL DEFAULT FALSE,
    editar   BOOLEAN NOT NULL DEFAULT FALSE,
    eliminar BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id_rol, modulo)
);

-- ---- Historial inmutable (RF033): impide UPDATE y DELETE -------------
CREATE OR REPLACE FUNCTION fn_historial_inmutable() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'El historial no se puede modificar ni eliminar';
END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS trg_historial_inmutable ON historial;
CREATE TRIGGER trg_historial_inmutable
    BEFORE UPDATE OR DELETE ON historial
    FOR EACH ROW EXECUTE FUNCTION fn_historial_inmutable();

-- ---- Unificación: el rol Gerente pasa a ser Administrador ------------
UPDATE usuario SET id_rol = (SELECT id_rol FROM rol WHERE nombre = 'Administrador')
 WHERE id_rol IN (SELECT id_rol FROM rol WHERE nombre = 'Gerente')
   AND EXISTS (SELECT 1 FROM rol WHERE nombre = 'Administrador');
DELETE FROM rol_permiso WHERE id_rol IN (SELECT id_rol FROM rol WHERE nombre = 'Gerente');
DELETE FROM rol r WHERE r.nombre = 'Gerente'
   AND NOT EXISTS (SELECT 1 FROM usuario u WHERE u.id_rol = r.id_rol);

COMMIT;
