#!/usr/bin/env node

import { rm } from "node:fs/promises";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const targets = Object.freeze({
  build: [join(root, "dist"), join(root, ".build")],
  tests: [join(root, ".build/tests"), join(root, ".build/browser")],
});
const name = process.argv[2];
const selected = targets[name];

if (selected === undefined || process.argv.length !== 3) {
  throw new Error("Usage: clean.mjs <build|tests>");
}

await Promise.all(selected.map((target) => rm(target, {
  force: true,
  recursive: true,
  maxRetries: 3,
  retryDelay: 50,
})));
