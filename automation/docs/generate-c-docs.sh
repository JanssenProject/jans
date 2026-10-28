#!/bin/bash
set -euo pipefail
CEDARLING_DIR=$(cd "$1"/jans-cedarling && pwd)
OUTPUT_DIRECTORY=$2
BINDING_DIR="$CEDARLING_DIR"/bindings/cedarling_c
HEADER="${CARGO_TARGET_DIR:-$BINDING_DIR/target}"/include/cedarling_c.h
WORK_DIR=$(mktemp -d)
trap 'rm -rf "$WORK_DIR"' EXIT

echo "Generating the C header for the cedarling_c crate"
cargo check --manifest-path "$CEDARLING_DIR"/Cargo.toml -p cedarling_c

if [ ! -f "$HEADER" ]; then
    echo "ERROR: cbindgen produced no header at $HEADER." >&2
    exit 1
fi

cat > "$WORK_DIR"/Doxyfile <<DOXYFILE
PROJECT_NAME           = "Cedarling C API"
INPUT                  = "$HEADER" "$BINDING_DIR/README.md"
USE_MDFILE_AS_MAINPAGE = "$BINDING_DIR/README.md"
OUTPUT_DIRECTORY       = "$WORK_DIR/out"
OPTIMIZE_OUTPUT_FOR_C  = YES
EXTRACT_ALL            = YES
FULL_PATH_NAMES        = NO
GENERATE_LATEX         = NO
QUIET                  = YES
DOXYFILE

echo "Generating C docs with doxygen"
doxygen "$WORK_DIR"/Doxyfile

if [ ! -f "$WORK_DIR"/out/html/index.html ]; then
    echo "ERROR: doxygen produced no output for cedarling_c." >&2
    exit 1
fi

echo "Copying C docs to $OUTPUT_DIRECTORY"
mkdir -p "$OUTPUT_DIRECTORY"
cp -r "$WORK_DIR"/out/html/. "$OUTPUT_DIRECTORY"/
