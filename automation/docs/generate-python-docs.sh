#!/bin/bash
set -euo pipefail
MAIN_DIRECTORY_LOCATION=$1
OUTPUT_DIRECTORY=$2
BINDING_DIR="$MAIN_DIRECTORY_LOCATION"/jans-cedarling/bindings/cedarling_python
VENV_DIR=$(mktemp -d)

echo "Generating python docs for the cedarling_python binding"
python3 -m venv "$VENV_DIR"
"$VENV_DIR"/bin/pip install --quiet "maturin[patchelf]"
(cd "$BINDING_DIR" && VIRTUAL_ENV="$VENV_DIR" "$VENV_DIR"/bin/maturin develop --release \
    && "$VENV_DIR"/bin/python -m pydoc -w cedarling_python)

if [ ! -f "$BINDING_DIR"/cedarling_python.html ]; then
    echo "ERROR: pydoc produced no output for cedarling_python." >&2
    exit 1
fi

echo "Copying python docs to $OUTPUT_DIRECTORY"
mkdir -p "$OUTPUT_DIRECTORY"
mv "$BINDING_DIR"/cedarling_python.html "$OUTPUT_DIRECTORY"/cedarling_python.html
rm -rf "$VENV_DIR"
