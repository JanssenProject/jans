#!/usr/bin/env bash
set -e
bun run .test-dist/runners/bun.js unit
bun run .test-dist/runners/bun.js contract
