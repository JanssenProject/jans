#!/usr/bin/env bash
# Runs once on first container start (via /docker-entrypoint-initdb.d).
# Creates the cedarling_pg extension in the default database so users get
# a working install out of the box.

set -euo pipefail

: "${POSTGRES_DB:=postgres}"
: "${POSTGRES_USER:=postgres}"

psql -v ON_ERROR_STOP=1 --username "${POSTGRES_USER}" --dbname "${POSTGRES_DB}" <<-'SQL'
    CREATE EXTENSION IF NOT EXISTS cedarling_pg;
SQL
