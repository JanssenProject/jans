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
# One landing entry per binding section: "<slug>/<entry page>|<title>".
SECTIONS=(
    "cedarling/index.html|Cedarling Rust, WASM and UniFFI"
    "cedarling-python/index.html|Cedarling Python"
    "cedarling-java/index.html|Cedarling Java/Kotlin"
    "cedarling-c/index.html|Cedarling C"
    "cedarling-go/cedarling_go.html|Cedarling Go"
    "cedarling-js/index.html|Cedarling JavaScript/TypeScript"
)

# The publish replaces the whole site, so a missing section would silently vanish.
missing=0
for slug in ${EXPECTED_SECTIONS:-}; do
    page=""
    for section in "${SECTIONS[@]}"; do
        if [ "${section%%/*}" = "$slug" ]; then
            page=${section%%|*}
            break
        fi
    done
    if [ -z "$page" ]; then
        echo "ERROR: '$slug' is not a known docs section." >&2
        missing=1
    elif [ ! -f "$GENERATED_DIRECTORY/$page" ]; then
        echo "ERROR: expected section '$slug' has no $page." >&2
        missing=1
    fi
done
if [ "$missing" -ne 0 ]; then
    exit 1
fi

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
    local entries=() modules=() prune=() section page root
    for section in "${SECTIONS[@]}"; do
        if [ -f "${section%%|*}" ]; then
            entries+=("$section")
        fi
        prune+=(-not -path "./${section%%/*}/*")
    done
    for kept in "${KEEP[@]}"; do
        prune+=(-not -path "./$kept/*")
    done

    # Module javadocs: link each tree's top index.html, not the pages nested below it.
    while IFS= read -r page; do
        page=${page#./}
        for root in ${modules[@]+"${modules[@]}"}; do
            if [[ "$page" == "${root%/index.html}/"* ]]; then
                continue 2
            fi
        done
        modules+=("$page")
    done < <(find . -mindepth 2 -name index.html "${prune[@]}" |
        awk -F/ '{ print NF "\t" $0 }' | LC_ALL=C sort -k1,1n -k2 | cut -f2-)

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
        for entry in ${entries[@]+"${entries[@]}"}; do
            echo "    <li><a href=\"${entry%%|*}\">${entry#*|}</a></li>"
        done
        echo '  </ul>'
        if [ ${#modules[@]} -gt 0 ]; then
            echo '  <h2>Java modules</h2>'
            echo '  <ul>'
            while IFS= read -r page; do
                echo "    <li><a href=\"${page}\">${page%/index.html}</a></li>"
            done < <(printf '%s\n' "${modules[@]}" | LC_ALL=C sort)
            echo '  </ul>'
        fi
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
    echo "DRY_RUN: not pushing to ${DEVELOPER_DOCS_REPO}."
    echo "Publishable commit: $(git log -1 --format='%h %s')"
    echo "Files staged for ${DEVELOPER_DOCS_BRANCH}: $(git ls-files | wc -l)"
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
