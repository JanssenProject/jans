#!/usr/bin/env sh
set -eu

: "${GUNICORN_LOG_LEVEL:=info}"
: "${GUNICORN_GRACEFUL_TIMEOUT:=8}"

echo "Starting Gunicorn..."

exec gunicorn main.core:app \
  -c python:main.gunicorn_conf \
  -b :5000 \
  --graceful-timeout "$GUNICORN_GRACEFUL_TIMEOUT" \
  --log-level "$GUNICORN_LOG_LEVEL" \
  --workers 1 \
  --threads 8 \
  --worker-class gthread \
  --access-logfile - \
  --error-logfile - \
  --worker-tmp-dir /dev/shm \
  --log-config /api/gunicorn_logging.conf
