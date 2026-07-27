#!/usr/bin/env bash
set -e

mkdir -p .test-dist/.build

node tests/build-sandbox.mjs edge

node tests/run-edge.mjs
