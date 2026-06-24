#!/usr/bin/env bash
# Loads the complete repository-owned dashboard demo dataset.
#
# Prerequisites:
#   1. Postgres container `backend-postgres-1` is running (docker compose up -d postgres)
#   2. Flyway migrations have been applied by starting the Spring Boot backend once
#      (STS: Run As > Spring Boot App). flyway_schema_history must show V1..V36.
#
# Usage (Git Bash on Windows):
#   ./load-seeds.sh
#
# Re-running is safe — deterministic keys and ON CONFLICT clauses prevent duplicates.
set -euo pipefail

if [[ -n "${CONTAINER:-}" ]]; then
  CONTAINER="${CONTAINER}"
elif docker ps --format '{{.Names}}' | grep -q '^soomgil-postgres-1$'; then
  CONTAINER="soomgil-postgres-1"
else
  CONTAINER="backend-postgres-1"
fi
DB_USER="${DB_USERNAME:-soomgil}"
DB_NAME="${DB_NAME:-soomgil}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DUMP_FILE="${DUMP_FILE:-${SCRIPT_DIR}/seeds/generated/soomgil_demo_dashboard_dump.sql}"
BUILD_DUMP_FILE="${SCRIPT_DIR}/seeds/build_demo_dashboard_dump.sh"
VERIFY_FILE="${VERIFY_FILE:-${SCRIPT_DIR}/seeds/verify_demo_data.sql}"

if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER}$"; then
  echo "ERROR: container ${CONTAINER} is not running."
  echo "       Start it first:  docker compose up -d postgres"
  exit 1
fi

if [[ ! -x "${BUILD_DUMP_FILE}" ]]; then
  echo "ERROR: dump builder is not executable: ${BUILD_DUMP_FILE}"
  exit 1
fi

"${BUILD_DUMP_FILE}"

if [[ ! -f "${DUMP_FILE}" ]]; then
  echo "ERROR: demo dump not found: ${DUMP_FILE}"
  exit 1
fi

if [[ ! -f "${VERIFY_FILE}" ]]; then
  echo "ERROR: demo verifier not found: ${VERIFY_FILE}"
  exit 1
fi

echo "Applying ${DUMP_FILE} ..."
docker exec -i "${CONTAINER}" psql -U "${DB_USER}" -d "${DB_NAME}" \
  -v ON_ERROR_STOP=1 < "${DUMP_FILE}"

echo "Verifying realistic demo invariants ..."
docker exec -i "${CONTAINER}" psql -U "${DB_USER}" -d "${DB_NAME}" \
  -v ON_ERROR_STOP=1 < "${VERIFY_FILE}"

echo
echo "Done. Verify with:"
echo "  docker exec ${CONTAINER} psql -U ${DB_USER} -d ${DB_NAME} -c \"SELECT COUNT(*) FROM auth.users;\""
