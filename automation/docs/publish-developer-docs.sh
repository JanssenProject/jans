#!/bin/bash
set -euo pipefail
MAIN_DIRECTORY_LOCATION=$(cd "$1" && pwd)
RELEASE_TAG=$2
: "${DEVELOPER_DOCS_URL:?DEVELOPER_DOCS_URL must be set}"
DEVELOPER_DOCS_BRANCH=${DEVELOPER_DOCS_BRANCH:-main}
KEEP=(".git" ".github" "LICENSE" "README.md" "automation")

WORK_DIR=$(mktemp -d)
trap 'rm -rf "$WORK_DIR"' EXIT
CLONE_DIR="$WORK_DIR"/developer-docs

git clone --depth 1 --branch "$DEVELOPER_DOCS_BRANCH" "$DEVELOPER_DOCS_URL" "$CLONE_DIR"

echo "Clearing previously generated docs"
for entry in "$CLONE_DIR"/* "$CLONE_DIR"/.[!.]*; do
    [ -e "$entry" ] || continue
    name=$(basename "$entry")
    keep=false
    for kept in "${KEEP[@]}"; do
        if [ "$name" = "$kept" ]; then
            keep=true
            break
        fi
    done
    if [ "$keep" = false ]; then
        rm -rf "$entry"
    fi
done

bash "$MAIN_DIRECTORY_LOCATION"/automation/docs/generate-javadocs.sh \
    "$MAIN_DIRECTORY_LOCATION" "$CLONE_DIR" "$RELEASE_TAG"
bash "$MAIN_DIRECTORY_LOCATION"/automation/docs/generate-rustdocs.sh \
    "$MAIN_DIRECTORY_LOCATION" "$CLONE_DIR"/cedarling
bash "$MAIN_DIRECTORY_LOCATION"/automation/docs/generate-python-docs.sh \
    "$MAIN_DIRECTORY_LOCATION" "$CLONE_DIR"/cedarling-python

cd "$CLONE_DIR"
git checkout --orphan republish
git add --all .
commit_opts=(-s)
if [ "${DOCS_SIGN_COMMITS:-1}" = "1" ]; then
    commit_opts+=(-S)
fi
git commit "${commit_opts[@]}" -m "docs: API references for ${RELEASE_TAG}"
git push --force origin "HEAD:${DEVELOPER_DOCS_BRANCH}"
echo "Published developer docs for ${RELEASE_TAG}"
