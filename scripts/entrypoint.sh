#!/bin/sh

set -e

exec python3 /app/scripts/entrypoint.py "$@"
