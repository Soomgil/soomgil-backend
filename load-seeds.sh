#!/usr/bin/env bash
# Loads the repository-owned Seoul + Daejeon demo dataset.
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

CONTAINER="${CONTAINER:-backend-postgres-1}"
DB_USER="${DB_USERNAME:-soomgil}"
DB_NAME="${DB_NAME:-soomgil}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SEED_FILE="${SEED_FILE:-${SCRIPT_DIR}/seeds/soomgil_demo_seoul_daejeon.sql}"
REALISTIC_PATCH_FILE="${REALISTIC_PATCH_FILE:-${SCRIPT_DIR}/seeds/soomgil_demo_realistic_patch.sql}"
VERIFY_FILE="${VERIFY_FILE:-${SCRIPT_DIR}/seeds/verify_demo_data.sql}"

if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER}$"; then
  echo "ERROR: container ${CONTAINER} is not running."
  echo "       Start it first:  docker compose up -d postgres"
  exit 1
fi

if [[ ! -f "${SEED_FILE}" ]]; then
  echo "ERROR: seed file not found: ${SEED_FILE}"
  exit 1
fi

if [[ ! -f "${REALISTIC_PATCH_FILE}" ]]; then
  echo "ERROR: realistic seed patch not found: ${REALISTIC_PATCH_FILE}"
  exit 1
fi

if [[ ! -f "${VERIFY_FILE}" ]]; then
  echo "ERROR: demo verifier not found: ${VERIFY_FILE}"
  exit 1
fi

echo "Applying ${SEED_FILE} ..."
docker exec -i "${CONTAINER}" psql -U "${DB_USER}" -d "${DB_NAME}" \
  -v ON_ERROR_STOP=1 < "${SEED_FILE}"

echo "Applying ${REALISTIC_PATCH_FILE} ..."
docker exec -i "${CONTAINER}" psql -U "${DB_USER}" -d "${DB_NAME}" \
  -v ON_ERROR_STOP=1 < "${REALISTIC_PATCH_FILE}"

echo "Verifying realistic demo invariants ..."
docker exec -i "${CONTAINER}" psql -U "${DB_USER}" -d "${DB_NAME}" \
  -v ON_ERROR_STOP=1 < "${VERIFY_FILE}"

echo
echo "Done. Verify with:"
echo "  docker exec ${CONTAINER} psql -U ${DB_USER} -d ${DB_NAME} -c \"SELECT COUNT(*) FROM auth.users;\""
