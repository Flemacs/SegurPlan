-- =====================================================
-- MIGRACIÓN: tabla de pagos (pago SIMULADO, sin pasarela real)
-- Ejecutar una sola vez sobre una base ya creada:
--   psql -U postgres -d SegurPlan -f db/migracion_pago.sql
-- =====================================================

CREATE TABLE IF NOT EXISTS pago (
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
    celular_origen VARCHAR(15),    -- Yape/Plin: celular enmascarado
    numero_operacion VARCHAR(20),  -- Yape/Plin: nº de operación
    destino VARCHAR(20),           -- Yape/Plin: nº ficticio del QR
    fecha_pago TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pago_cotizacion FOREIGN KEY (id_cotizacion) REFERENCES cotizacion(id_cotizacion),
    CONSTRAINT fk_pago_contratacion FOREIGN KEY (id_contratacion) REFERENCES contratacion(id_contratacion),
    CONSTRAINT fk_pago_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario),
    CONSTRAINT chk_pago_monto CHECK (monto > 0),
    CONSTRAINT chk_pago_estado CHECK (estado IN ('Aprobado', 'Rechazado'))
);

-- Un solo pago aprobado por cotización (los rechazados pueden repetirse)
CREATE UNIQUE INDEX IF NOT EXISTS ux_pago_cotizacion_aprobado
    ON pago (id_cotizacion) WHERE estado = 'Aprobado';
