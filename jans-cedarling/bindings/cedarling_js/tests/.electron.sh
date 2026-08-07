#!/usr/bin/env bash
set -e
unset ELECTRON_RUN_AS_NODE
./node_modules/.bin/electron --no-sandbox .test-dist/runners/electron.js unit
./node_modules/.bin/electron --no-sandbox .test-dist/runners/electron.js contract
