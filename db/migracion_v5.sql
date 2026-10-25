-- =====================================================================
-- SEGURPLAN v5 - MIGRACIÓN (idempotente). Ejecutar sobre una base ya creada.
--   psql -U postgres -d SegurPlan -f db/migracion_v5.sql
-- Se puede ejecutar varias veces sin problema.
-- =====================================================================
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
