#!/bin/bash
set -euo pipefail
WASM_DIR=$(cd "$1"/jans-cedarling/bindings/cedarling_wasm && pwd)
OUTPUT_DIRECTORY=$2
SDK_DIR="$WASM_DIR"/js
TYPES_DIR="$SDK_DIR"/dist/types/esm
WORK_DIR=$(mktemp -d)
trap 'rm -rf "$WORK_DIR"' EXIT

echo "Building the cedarling_wasm package"
(cd "$WASM_DIR" && wasm-pack build --release --locked --target web --scope janssenproject)
(cd "$SDK_DIR" && npm ci --ignore-scripts && npm run build)

if [ ! -f "$TYPES_DIR"/index.d.ts ]; then
    echo "ERROR: the SDK build produced no type declarations in $TYPES_DIR." >&2
    exit 1
fi

# TypeDoc cannot drive the SDK's TypeScript 7, so it gets its own pinned TypeScript 6.
echo "Installing TypeDoc"
npm install --prefix "$WORK_DIR"/tools --no-audit --no-fund \
    typedoc@0.28.20 typescript@6.0.3

cat > "$WORK_DIR"/tsconfig.json <<JSON
{
  "compilerOptions": {
    "target": "es2022",
    "module": "nodenext",
    "moduleResolution": "nodenext",
    "lib": ["es2022", "dom"],
    "skipLibCheck": true,
    "noEmit": true
  },
  "include": ["$TYPES_DIR/*.d.ts"]
}
JSON

echo "Generating JS/TS docs with TypeDoc"
"$WORK_DIR"/tools/node_modules/.bin/typedoc \
    --tsconfig "$WORK_DIR"/tsconfig.json \
    --name "Cedarling JavaScript/TypeScript API" \
    --readme "$SDK_DIR"/README.md \
    --out "$WORK_DIR"/out \
    "$TYPES_DIR"/index.d.ts "$TYPES_DIR"/edge.d.ts "$TYPES_DIR"/manual.d.ts

if [ ! -f "$WORK_DIR"/out/index.html ]; then
    echo "ERROR: TypeDoc produced no output for cedarling_wasm." >&2
    exit 1
fi

echo "Copying JS/TS docs to $OUTPUT_DIRECTORY"
mkdir -p "$OUTPUT_DIRECTORY"
cp -r "$WORK_DIR"/out/. "$OUTPUT_DIRECTORY"/
