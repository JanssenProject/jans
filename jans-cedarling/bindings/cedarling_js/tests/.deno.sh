#!/usr/bin/env bash
set -e
deno run -A --unstable-sloppy-imports .test-dist/runners/deno.js unit
deno run -A --unstable-sloppy-imports .test-dist/runners/deno.js contract
