#!/usr/bin/env bash
# Restaura la base SegurPlan desde un respaldo .sql
# Uso:  DB_PASSWORD=clave ./scripts/restore.sh ./backups/segurplan_AAAAMMDD_HHMMSS.sql
set -euo pipefail

ARCHIVO="${1:?Indique el archivo .sql a restaurar}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-SegurPlan}"
DB_USER="${DB_USER:-postgres}"

export PGPASSWORD="${DB_PASSWORD:?Defina DB_PASSWORD}"

read -r -p "Se reemplazaran los datos de '$DB_NAME'. Escriba SI para continuar: " R
[ "$R" = "SI" ] || { echo "Cancelado."; exit 1; }

psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
     -v ON_ERROR_STOP=1 -q -f "$ARCHIVO"

echo "Restauracion completada desde $ARCHIVO"
