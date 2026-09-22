#!/bin/bash
set -euo pipefail
MAIN_DIRECTORY_LOCATION=$1
OUTPUT_DIRECTORY=$2
CEDARLING_DIR="$MAIN_DIRECTORY_LOCATION"/jans-cedarling
DOC_DIR="$CEDARLING_DIR"/target/doc

echo "Generating rust docs for the cedarling and cedarling_wasm crates"
cargo doc --no-deps --manifest-path "$CEDARLING_DIR"/Cargo.toml -p cedarling -p cedarling_wasm

for crate in cedarling cedarling_wasm; do
    if [ ! -f "$DOC_DIR/$crate/index.html" ]; then
        echo "ERROR: rustdoc produced no output for '$crate'." >&2
        exit 1
    fi
done

cat > "$DOC_DIR"/index.html <<'HTML'
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <title>Cedarling API documentation</title>
  <meta http-equiv="refresh" content="0; url=cedarling/index.html">
</head>
<body>
  <p>Cedarling API documentation:</p>
  <ul>
    <li><a href="cedarling/index.html">Rust (<code>cedarling</code>)</a></li>
    <li><a href="cedarling_wasm/index.html">JavaScript/WASM (<code>cedarling_wasm</code>)</a></li>
  </ul>
</body>
</html>
HTML

echo "Copying rust docs to $OUTPUT_DIRECTORY"
mkdir -p "$OUTPUT_DIRECTORY"
cp -r "$DOC_DIR"/. "$OUTPUT_DIRECTORY"/
