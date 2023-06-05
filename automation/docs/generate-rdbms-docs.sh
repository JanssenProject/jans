#!/bin/bash
set -euo pipefail
git pull origin main
docker cp automation/docs/generate-rdbms-docs.py docker-jans-monolith-jans-1:/opt/generate-rdbms-docs.py
docker exec docker-jans-monolith-jans-1 python3 /opt/generate-rdbms-docs.py mysql jans 1t5Fin3#security jans utf8 mysql-schema.md mysql-schema-indexes.md
docker cp docker-jans-monolith-jans-1:/opt/mysql-schema.md docs/admin/reference/database/mysql-schema.md
docker cp docker-jans-monolith-jans-1:/opt/mysql-schema-indexes.md docs/admin/reference/database/mysql-schema-indexes.md
git add docs/* && git update-index --refresh
git commit -m "docs: update rdbms docs"
git diff-index --quiet HEAD -- || git commit -S -m "docs: update rdbms docs" && git push