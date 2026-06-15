#!/bin/bash
set -euo pipefail
PERSISTENCE=$1
# Runs inside the all-in-one demo container (start automation/start_janssen_aio_demo.sh first).
echo "Generate RDBMS docs"
docker cp automation/docs/generate-rdbms-docs.py jans:/opt/generate-rdbms-docs.py
docker exec jans python3 /opt/generate-rdbms-docs.py -hostname "$PERSISTENCE" -username "jans" -password "1t5Fin3#security" -database "jans" -rdbm-type "$PERSISTENCE" -schema-file "/opt/$PERSISTENCE-schema.md" -schema-indexes-file "/opt/$PERSISTENCE-schema-indexes.md"
docker exec jans ls -l /opt/
docker cp jans:/opt/"$PERSISTENCE"-schema.md ./docs/admin/reference/database/"$PERSISTENCE"-schema.md || echo "No schema file found"
docker cp jans:/opt/"$PERSISTENCE"-schema-indexes.md ./docs/admin/reference/database/"$PERSISTENCE"-schema-indexes.md || echo "No schema indexes file found"
