#!/bin/bash
set -euo pipefail
PERSISTENCE=$1
echo "Generate RDBMS docs"
git checkout -b cn-jans-update-rdbms-auto-generated-docs
git pull origin cn-jans-update-rdbms-auto-generated-docs || echo "Nothing to pull"
docker cp automation/docs/generate-rdbms-docs.py docker-jans-monolith-jans-1:/opt/generate-rdbms-docs.py
docker exec docker-jans-monolith-jans-1 python3 /opt/generate-rdbms-docs.py -hostname "$PERSISTENCE" -username "jans" -password "1t5Fin3#security" -database "jans" -rdbm-type "$PERSISTENCE"
docker cp docker-jans-monolith-jans-1:/opt/mysql-schema.md docs/admin/reference/database/"$PERSISTENCE"-schema.md
docker cp docker-jans-monolith-jans-1:/opt/mysql-schema-indexes.md docs/admin/reference/database/"$PERSISTENCE"-schema-indexes.md
git add docs/* && git update-index --refresh
git commit -m "docs: update rdbms docs"]
git push --set-upstream origin cn-jans-update-rdbms-auto-generated-docs
MESSAGE="fix(docs): autogenerate RDBMS docs"
gh pr create --body "Auto generated RDBMS docs" --title "${MESSAGE}"
