#!/bin/bash
set -euo pipefail
WASM_DIR=$(cd "$1"/jans-cedarling/bindings/cedarling_wasm && pwd)
OUTPUT_DIRECTORY=$2
SDK_DIR="$WASM_DIR"/js
DOCS_TYPES="$SDK_DIR"/.build/docs/types
WORK_DIR=$(mktemp -d)
trap 'rm -rf "$WORK_DIR"' EXIT

echo "Building the cedarling_wasm package"
(cd "$WASM_DIR" && wasm-pack build --release --locked --target web --scope janssenproject)
if [ ! -f "$WASM_DIR"/pkg/cedarling_wasm.d.ts ]; then
    echo "ERROR: wasm-pack produced no cedarling_wasm.d.ts." >&2
    exit 1
fi

(cd "$SDK_DIR" && npm ci --ignore-scripts && npm run build)
if [ ! -f "$SDK_DIR"/dist/types/esm/index.d.ts ]; then
    echo "ERROR: the SDK build produced no type declarations." >&2
    exit 1
fi

# Hide the wasm-bindgen ABI from the docs on a copy, since dist/ ships to npm.
rm -rf "$DOCS_TYPES"
mkdir -p "$DOCS_TYPES"
cp "$SDK_DIR"/dist/types/esm/*.d.ts "$DOCS_TYPES"/
sed -i -E \
    -e 's#^(export interface InitOutput\b)#/** @internal */\n\1#' \
    -e 's#^(declare class IntoUnderlying[A-Za-z]*\b)#/** @internal */\n\1#' \
    -e 's#^(export type ReadableStreamType\b)#/** @internal */\n\1#' \
    "$DOCS_TYPES"/generated.d.ts
if ! grep -B1 '^export interface InitOutput' "$DOCS_TYPES"/generated.d.ts | grep -q '@internal'; then
    echo "ERROR: could not hide InitOutput; the wasm-bindgen output changed shape." >&2
    exit 1
fi

# TypeDoc cannot drive the SDK's TypeScript 7, so it gets its own pinned TypeScript 6.
echo "Installing TypeDoc"
npm install --prefix "$WORK_DIR"/tools --no-audit --no-fund \
    typedoc@0.28.20 typescript@6.0.3

echo "Generating JS/TS docs with TypeDoc"
"$WORK_DIR"/tools/node_modules/.bin/typedoc \
    --options "$SDK_DIR"/docs/typedoc.json --out "$WORK_DIR"/out

if [ ! -f "$WORK_DIR"/out/index.html ]; then
    echo "ERROR: TypeDoc produced no output for cedarling_wasm." >&2
    exit 1
fi

leaked=$(grep -rlE '__wbindgen_|rust_zstd_wasm_shim|IntoUnderlying' "$WORK_DIR"/out || true)
if [ -n "$leaked" ] || find "$WORK_DIR"/out -iname '*InitOutput*' | grep -q .; then
    echo "ERROR: wasm-bindgen internals leaked into the docs:" >&2
    echo "${leaked:-InitOutput page present}" >&2
    exit 1
fi

echo "Copying JS/TS docs to $OUTPUT_DIRECTORY"
mkdir -p "$OUTPUT_DIRECTORY"
cp -r "$WORK_DIR"/out/. "$OUTPUT_DIRECTORY"/
