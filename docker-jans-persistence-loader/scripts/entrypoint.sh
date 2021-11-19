#!/bin/sh
set -e

python3 /app/scripts/wait.py
exec python3 /app/scripts/bootstrap.py
