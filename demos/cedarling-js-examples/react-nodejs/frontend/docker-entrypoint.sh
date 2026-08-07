#!/bin/sh

set -eu

printf 'TaskApp frontend: %s\n' "${FRONTEND_URL:-http://localhost:3000}"
exec "$@"
