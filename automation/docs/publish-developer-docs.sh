#!/bin/bash
set -euo pipefail
GENERATED_DIRECTORY=$(cd "$1" && pwd)
RELEASE_TAG=$2
DRY_RUN=${DRY_RUN:-0}
if [ "$DRY_RUN" != "1" ]; then
    : "${DEVELOPER_DOCS_TOKEN:?DEVELOPER_DOCS_TOKEN must be set}"
fi
DEVELOPER_DOCS_REPO=${DEVELOPER_DOCS_REPO:-JanssenProject/developer-docs}
DEVELOPER_DOCS_BRANCH=${DEVELOPER_DOCS_BRANCH:-main}
DEVELOPER_DOCS_URL=${DEVELOPER_DOCS_URL:-https://github.com/${DEVELOPER_DOCS_REPO}.git}
DEVELOPER_DOCS_PUSH_URL=${DEVELOPER_DOCS_PUSH_URL:-https://x-access-token@github.com/${DEVELOPER_DOCS_REPO}.git}
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

echo "Staging docs generated from $GENERATED_DIRECTORY"
cp -r "$GENERATED_DIRECTORY"/. "$CLONE_DIR"/

cd "$CLONE_DIR"
write_landing_page() {
    local entries=()
    if [ -f cedarling/index.html ]; then
        entries+=("cedarling/index.html|Cedarling Rust and WASM")
    fi
    if [ -f cedarling-python/cedarling_python.html ]; then
        entries+=("cedarling-python/cedarling_python.html|Cedarling Python")
    fi
    local page
    while IFS= read -r page; do
        entries+=("${page#./}|${page#./}")
    done < <(find . -mindepth 2 -name index.html \
        -not -path "./cedarling/*" -not -path "./.git/*" | sort)

    {
        echo '<!DOCTYPE html>'
        echo '<html lang="en">'
        echo '<head>'
        echo '  <meta charset="utf-8">'
        echo "  <title>Janssen API references (${RELEASE_TAG})</title>"
        echo '</head>'
        echo '<body>'
        echo "  <h1>Janssen API references (${RELEASE_TAG})</h1>"
        echo '  <ul>'
        local entry
        for entry in "${entries[@]}"; do
            echo "    <li><a href=\"${entry%%|*}\">${entry#*|}</a></li>"
        done
        echo '  </ul>'
        echo '</body>'
        echo '</html>'
    } > index.html
}
write_landing_page

git checkout --orphan republish
git add --all .
commit_opts=(-s)
if [ "${DOCS_SIGN_COMMITS:-1}" = "1" ]; then
    commit_opts+=(-S)
fi
git commit "${commit_opts[@]}" -m "docs: API references for ${RELEASE_TAG}"
if [ "$DRY_RUN" = "1" ]; then
    echo "DRY_RUN: not pushing to ${DEVELOPER_DOCS_REPO}. Publishable tree:"
    git show --stat --oneline HEAD | head -5
    echo "$(git ls-files | wc -l) files staged for ${DEVELOPER_DOCS_BRANCH}"
    exit 0
fi

ASKPASS="$WORK_DIR"/askpass.sh
cat > "$ASKPASS" <<'ASKPASS_SCRIPT'
#!/bin/sh
echo "$DEVELOPER_DOCS_TOKEN"
ASKPASS_SCRIPT
chmod +x "$ASKPASS"
GIT_ASKPASS="$ASKPASS" GIT_TERMINAL_PROMPT=0 git push --force \
    "$DEVELOPER_DOCS_PUSH_URL" "HEAD:${DEVELOPER_DOCS_BRANCH}"
echo "Published developer docs for ${RELEASE_TAG}"
