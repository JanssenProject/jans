#!/bin/sh

set -e

export CN_CONTAINER_METADATA_NAMESPACE="${CN_CONFIG_KUBERNETES_NAMESPACE}"

exec python3 /app/scripts/bootstrap.py "$@"
