#!/bin/bash
set -euo pipefail
CEDARLING_DIR=$(cd "$1"/jans-cedarling && pwd)
OUTPUT_DIRECTORY=$2
BINDING_DIR="$CEDARLING_DIR"/bindings/cedarling_c
HEADER="${CARGO_TARGET_DIR:-$BINDING_DIR/target}"/include/cedarling_c.h
WORK_DIR=$(mktemp -d)
trap 'rm -rf "$WORK_DIR"' EXIT

echo "Generating the C header for the cedarling_c crate"
cargo build --release --manifest-path "$CEDARLING_DIR"/Cargo.toml -p cedarling_c

if [ ! -f "$HEADER" ]; then
    echo "ERROR: cbindgen produced no header at $HEADER." >&2
    exit 1
fi

if ! doxygen --version >/dev/null 2>&1; then
    echo "ERROR: doxygen is not installed or cannot run." >&2
    exit 1
fi

# Doxygen rendering changes between versions, so record which one built the docs.
echo "Generating C docs with doxygen $(doxygen --version | head -1)"
mkdir -p "$WORK_DIR"/out
(cd "$BINDING_DIR"/docs &&
    CEDARLING_C_HEADER="$HEADER" CEDARLING_C_DOCS_OUT="$WORK_DIR"/out doxygen Doxyfile)

if [ ! -f "$WORK_DIR"/out/html/index.html ]; then
    echo "ERROR: doxygen produced no output for cedarling_c." >&2
    exit 1
fi

echo "Copying C docs to $OUTPUT_DIRECTORY"
mkdir -p "$OUTPUT_DIRECTORY"
cp -r "$WORK_DIR"/out/html/. "$OUTPUT_DIRECTORY"/
