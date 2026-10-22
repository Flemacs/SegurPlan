-- =====================================================
-- MIGRACIÓN: pago con billetera digital (Yape / Plin) por QR
-- Agrega columnas a la tabla "pago" (que ya debe existir; ver
-- db/migracion_pago.sql). Se puede ejecutar más de una vez.
--   psql -U postgres -d SegurPlan -f db/migracion_pago_billetera.sql
-- =====================================================

ALTER TABLE pago ADD COLUMN IF NOT EXISTS celular_origen   VARCHAR(15);
ALTER TABLE pago ADD COLUMN IF NOT EXISTS numero_operacion VARCHAR(20);
ALTER TABLE pago ADD COLUMN IF NOT EXISTS destino          VARCHAR(20);

-- Verificación:
-- SELECT id_pago, metodo, monto, estado, celular_origen, numero_operacion, destino
-- FROM pago ORDER BY id_pago DESC;
