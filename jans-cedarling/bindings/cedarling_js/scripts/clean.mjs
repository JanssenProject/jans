#!/usr/bin/env node

import { rm } from "node:fs/promises";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const targets = Object.freeze({
  build: join(root, "dist"),
  tests: join(root, ".test-dist"),
});
const name = process.argv[2];
const target = targets[name];

if (target === undefined || process.argv.length !== 3) {
  throw new Error("Usage: clean.mjs <build|tests>");
}

await rm(target, { force: true, recursive: true, maxRetries: 3, retryDelay: 50 });
