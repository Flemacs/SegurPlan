-- =====================================================
-- MIGRACIÓN: permitir precio y deducible reales
-- La base actual tiene restricciones que solo aceptan 0:
--   chk_cotizacion_precio    -> precio    IS NULL OR precio    = 0
--   chk_cotizacion_deducible -> deducible IS NULL OR deducible = 0
--   chk_poliza_deducible     -> deducible IS NULL OR deducible = 0
-- Se reemplazan por las definidas en db/SegurPlan_schema.sql (>= 0).
-- Ejecutar UNA vez:  psql -U postgres -d SegurPlan -f db/migracion_precio_cotizacion.sql
-- =====================================================

BEGIN;

ALTER TABLE cotizacion DROP CONSTRAINT IF EXISTS chk_cotizacion_precio;
ALTER TABLE cotizacion DROP CONSTRAINT IF EXISTS chk_cotizacion_deducible;
ALTER TABLE poliza     DROP CONSTRAINT IF EXISTS chk_poliza_deducible;

ALTER TABLE cotizacion ADD CONSTRAINT chk_cotizacion_precio
    CHECK (precio IS NULL OR precio >= 0);
ALTER TABLE cotizacion ADD CONSTRAINT chk_cotizacion_deducible
    CHECK (deducible IS NULL OR deducible >= 0);
ALTER TABLE poliza ADD CONSTRAINT chk_poliza_deducible
    CHECK (deducible IS NULL OR deducible >= 0);

COMMIT;

-- Verificación:
-- SELECT conrelid::regclass AS tabla, conname, pg_get_constraintdef(oid)
-- FROM pg_constraint WHERE conname IN
--   ('chk_cotizacion_precio','chk_cotizacion_deducible','chk_poliza_deducible');
