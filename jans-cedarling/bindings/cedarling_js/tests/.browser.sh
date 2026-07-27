#!/usr/bin/env bash
set -e

mkdir -p .test-dist/.build

./node_modules/.bin/esbuild \
  --log-level=warning \
  --format=esm \
  --bundle \
  --external:node:\* \
  --target=esnext \
  --outfile=.test-dist/.build/run-browser.js \
  .test-dist/runners/browser.js

npx -y playwright test --config=tests/playwright.config.ts
