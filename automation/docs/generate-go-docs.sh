#!/bin/bash
set -euo pipefail
BINDING_DIR=$(cd "$1"/jans-cedarling/bindings/cedarling_go && pwd)
OUTPUT_DIRECTORY=$2
WORK_DIR=$(mktemp -d)
trap 'rm -rf "$WORK_DIR"' EXIT

echo "Generating go docs for the cedarling_go package"
cd "$BINDING_DIR"
go mod download
go doc -all . > "$WORK_DIR"/doc.txt

if [ ! -s "$WORK_DIR"/doc.txt ]; then
    echo "ERROR: go doc produced no output for cedarling_go." >&2
    exit 1
fi

{
    cat <<'HTML'
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <title>Cedarling Go API documentation</title>
  <style>
    body { font-family: system-ui, sans-serif; margin: 2rem; }
    pre { white-space: pre-wrap; font-size: 0.95rem; line-height: 1.45; }
  </style>
</head>
<body>
<h1>Cedarling Go API documentation</h1>
<pre>
HTML
    sed -e 's/&/\&amp;/g' -e 's/</\&lt;/g' -e 's/>/\&gt;/g' "$WORK_DIR"/doc.txt
    cat <<'HTML'
</pre>
</body>
</html>
HTML
} > "$WORK_DIR"/cedarling_go.html

echo "Copying go docs to $OUTPUT_DIRECTORY"
mkdir -p "$OUTPUT_DIRECTORY"
cp "$WORK_DIR"/cedarling_go.html "$OUTPUT_DIRECTORY"/cedarling_go.html
