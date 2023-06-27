#!/bin/bash
set -euo pipefail
PERSISTENCE=$1
echo "Generate RDBMS docs"
cd jans
git config pull.rebase true
git checkout -b cn-jans-update-"$PERSISTENCE"-auto-generated-docs || echo "Branch exists"
git pull origin cn-jans-update-"$PERSISTENCE"-auto-generated-docs || echo "Nothing to pull"
docker cp automation/docs/generate-rdbms-docs.py docker-jans-monolith-jans-1:/opt/generate-rdbms-docs.py
docker exec docker-jans-monolith-jans-1 python3 /opt/generate-rdbms-docs.py -hostname "$PERSISTENCE" -username "jans" -password "1t5Fin3#security" -database "jans" -rdbm-type "$PERSISTENCE" -schema-file "/opt/$PERSISTENCE-schema.md" -schema-indexes-file "/opt/$PERSISTENCE-schema-indexes.md"
docker exec docker-jans-monolith-jans-1 ls -l /opt/
docker cp docker-jans-monolith-jans-1:/opt/"$PERSISTENCE"-schema.md ./docs/admin/reference/database/"$PERSISTENCE"-schema.md || echo "No schema file found"
docker cp docker-jans-monolith-jans-1:/opt/"$PERSISTENCE"-schema-indexes.md ./docs/admin/reference/database/"$PERSISTENCE"-schema-indexes.md || echo "No schema indexes file found"
git add . || echo "generating rdbms docs failed !!!"
git commit -a -S -m "docs: auto-generated $PERSISTENCE docs" || echo "Nothing to commit"
git push --set-upstream origin cn-jans-update-"$PERSISTENCE"-auto-generated-docs || echo "generating rdbms docs failed !!!"
MESSAGE="fix(docs): autogenerate $PERSISTENCE RDBMS docs"
gh pr create --body "Auto generated RDBMS docs" --title "${MESSAGE}" || echo "PR exists"