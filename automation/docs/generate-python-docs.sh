#!/bin/bash
set -euo pipefail
MAIN_DIRECTORY_LOCATION=$1
OUTPUT_DIRECTORY=$2
BINDING_DIR="$MAIN_DIRECTORY_LOCATION"/jans-cedarling/bindings/cedarling_python
VENV_DIR=$(mktemp -d)
WORK_DIR=$(mktemp -d)
trap 'rm -rf "$VENV_DIR" "$WORK_DIR"' EXIT

echo "Generating python docs for the cedarling_python binding"
python3 -m venv "$VENV_DIR"
"$VENV_DIR"/bin/pip install --quiet "maturin[patchelf]" pdoc==16.0.0
(cd "$BINDING_DIR" && VIRTUAL_ENV="$VENV_DIR" "$VENV_DIR"/bin/maturin develop --release)

# Docstrings come from Rust `///` comments, which are Markdown.
"$VENV_DIR"/bin/python -m pdoc --docformat markdown -o "$WORK_DIR"/out cedarling_python

if [ ! -f "$WORK_DIR"/out/index.html ]; then
    echo "ERROR: pdoc produced no output for cedarling_python." >&2
    exit 1
fi

echo "Copying python docs to $OUTPUT_DIRECTORY"
mkdir -p "$OUTPUT_DIRECTORY"
cp -r "$WORK_DIR"/out/. "$OUTPUT_DIRECTORY"/
