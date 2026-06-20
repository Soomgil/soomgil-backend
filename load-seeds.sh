#!/usr/bin/env bash
# Loads dummy data (11 seed files) into the running postgres container.
#
# Prerequisites:
#   1. Postgres container `backend-postgres-1` is running (docker compose up -d postgres)
#   2. Flyway migrations have been applied by starting the Spring Boot backend once
#      (STS: Run As > Spring Boot App). flyway_schema_history must show V1..V23.
#
# Usage (Git Bash on Windows):
#   ./load-seeds.sh
#
# Re-running is safe — every seed file uses BEGIN/COMMIT + ON CONFLICT DO NOTHING.
set -euo pipefail

SEED_SOURCE="${SEED_SOURCE:-C:/Users/tkfvh/Downloads/123}"
CONTAINER="${CONTAINER:-backend-postgres-1}"
DB_USER="${DB_USERNAME:-soomgil}"
DB_NAME="${DB_NAME:-soomgil}"

if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER}$"; then
  echo "ERROR: container ${CONTAINER} is not running."
  echo "       Start it first:  docker compose up -d postgres"
  exit 1
fi

echo "Copying seed files into ${CONTAINER}:/tmp/seeds/ ..."
docker exec "${CONTAINER}" mkdir -p /tmp/seeds
docker cp "${SEED_SOURCE}/." "${CONTAINER}:/tmp/seeds/"

echo "Applying 00_run_all.sql (loads 01..11 in FK-safe order) ..."
docker exec "${CONTAINER}" psql -U "${DB_USER}" -d "${DB_NAME}" \
  -v ON_ERROR_STOP=1 -f /tmp/seeds/00_run_all.sql

echo
echo "Done. Verify with:"
echo "  docker exec ${CONTAINER} psql -U ${DB_USER} -d ${DB_NAME} -c \"SELECT COUNT(*) FROM auth.users;\""
