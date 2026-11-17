#!/usr/bin/env bash
# Respaldo manual de la base SegurPlan con pg_dump.
# Uso:  DB_PASSWORD=clave ./scripts/backup.sh [carpeta_destino]
set -euo pipefail

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-SegurPlan}"
DB_USER="${DB_USER:-postgres}"
DESTINO="${1:-./backups}"

export PGPASSWORD="${DB_PASSWORD:?Defina DB_PASSWORD}"
mkdir -p "$DESTINO"

ARCHIVO="$DESTINO/segurplan_$(date +%Y%m%d_%H%M%S).sql"

pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" \
        --format=plain --clean --if-exists --no-owner \
        -f "$ARCHIVO" "$DB_NAME"

echo "Respaldo creado: $ARCHIVO ($(du -h "$ARCHIVO" | cut -f1))"
