#!/usr/bin/env bash

# Build the Rust library
cargo build -r -p cedarling_go # build rust in release mode


# Detect the target OS
OS="$(uname -s)"
TARGET_DIR="../../target/release"   # Change to "./target/debug" for debug builds
OUTPUT_DIR="."             # Directory where files will be copied

# Create output directory
mkdir -p "$OUTPUT_DIR"

case "$OS" in
  Linux*)
    LIB_NAME="libcedarling_go.so"
    cp "$TARGET_DIR/$LIB_NAME" "$OUTPUT_DIR/"
    ;;
  Darwin*)
    LIB_NAME="libcedarling_go.dylib"
    cp "$TARGET_DIR/$LIB_NAME" "$OUTPUT_DIR/"
    ;;
  CYGWIN*|MINGW*|MSYS*)
    # Windows: Copy both .dll and .dll.lib
    cp "$TARGET_DIR/cedarling_go.dll" "$OUTPUT_DIR/"
    cp "$TARGET_DIR/cedarling_go.dll.lib" "$OUTPUT_DIR/cedarling_go.lib"
    ;;
  *)
    echo "Unsupported OS: $OS"
    exit 1
    ;;
esac

echo "Files copied to $OUTPUT_DIR:"

