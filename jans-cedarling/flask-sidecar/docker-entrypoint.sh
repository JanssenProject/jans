#!/usr/bin/env sh
set -eu

: "${GUNICORN_LOG_LEVEL:=info}"

echo "Starting Gunicorn..."

exec gunicorn main.core:app \
  -b :5000 \
  --log-level "$GUNICORN_LOG_LEVEL" \
  --workers 1 \
  --threads 8 \
  --worker-class gthread \
  --access-logfile - \
  --error-logfile - \
  --worker-tmp-dir /dev/shm \
  --log-config /api/gunicorn_logging.conf
