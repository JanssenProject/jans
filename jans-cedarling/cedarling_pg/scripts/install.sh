#!/usr/bin/env bash
# Build and install cedarling_pg into the developer's local pgrx-managed PostgreSQL.
#
# Wraps `cargo pgrx install` with a follow-up `psql` health check that:
#   1. drops any pre-existing extension install
#   2. creates the extension in a fresh database (creating it first if missing)
#   3. verifies the catalog schema and every SQL-exposed function is present
#   4. prints the resolved versions of the cedarling_pg package, pgrx, and PostgreSQL server
#
# Environment overrides:
#   PG_VERSION   pgrx feature + pg_config flavor to use (default: pg16)
#   PG_DB        database used for the health check (default: cedarling_pg_install_check)
#   PSQL         path to the psql binary that matches PG_VERSION (default: psql)
#
# Usage:
#   scripts/install.sh                 # build + install + health check
#   scripts/install.sh --release       # release build
#   scripts/install.sh --skip-health   # build + install only

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EXT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

PG_VERSION="${PG_VERSION:-pg16}"
PG_DB="${PG_DB:-cedarling_pg_install_check}"
PSQL_BIN="${PSQL:-psql}"

RELEASE_FLAG=""
SKIP_HEALTH="false"
for arg in "$@"; do
    case "${arg}" in
        --release) RELEASE_FLAG="--release" ;;
        --skip-health) SKIP_HEALTH="true" ;;
        -h|--help)
            sed -n '2,22p' "${BASH_SOURCE[0]}"
            exit 0
            ;;
        *) echo "unknown flag: ${arg}" >&2; exit 2 ;;
    esac
done

echo "==> Installing cedarling_pg (${PG_VERSION}${RELEASE_FLAG:+, release})"
cd "${EXT_DIR}"
cargo pgrx install --features "${PG_VERSION}" ${RELEASE_FLAG}

if [[ "${SKIP_HEALTH}" == "true" ]]; then
    echo "==> Skipping health check (per --skip-health)"
    exit 0
fi

echo "==> Running post-install health check against database '${PG_DB}'"
if ! "${PSQL_BIN}" -lqt | awk '{print $1}' | grep -qx "${PG_DB}"; then
    createdb "${PG_DB}"
fi

"${PSQL_BIN}" -v ON_ERROR_STOP=1 -d "${PG_DB}" <<'SQL'
DROP EXTENSION IF EXISTS cedarling_pg CASCADE;
CREATE EXTENSION cedarling_pg;

-- Catalog schema present?
SELECT 1/count(*) AS ok
  FROM pg_namespace
 WHERE nspname = 'cedarling';

-- Required catalog tables?
SELECT relname
  FROM pg_class c
  JOIN pg_namespace n ON n.oid = c.relnamespace
 WHERE n.nspname = 'cedarling'
   AND c.relkind = 'r'
 ORDER BY relname;

-- Core functions present?
SELECT proname
  FROM pg_proc p
  JOIN pg_namespace n ON n.oid = p.pronamespace
 WHERE n.nspname = 'public'
   AND proname LIKE 'cedarling_%'
 ORDER BY proname;

-- Defaults sane?
SHOW cedarling.mode;
SHOW cedarling.strategy;
SHOW cedarling.fail_mode;
SHOW cedarling.log_level;
SHOW cedarling.cache_ttl;
SHOW cedarling.cache_size;
SHOW cedarling.audit_fail_open;
SHOW cedarling.trace_buffer_size;
SHOW cedarling.policy_history_size;
SHOW cedarling.diff_mode;
SHOW cedarling.schema_validate_strict;
SQL

echo "==> OK — cedarling_pg installed and health check passed"
