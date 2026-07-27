#!/usr/bin/env node

/**
 * Cleans build output directories for cedarling_js.
 * Supports cleaning production builds ('dist') or test builds ('.test-dist').
 */

import { rm } from "node:fs/promises";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const packageRoot = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const targets = {
  build: join(packageRoot, "dist"),
  tests: join(packageRoot, ".test-dist"),
};
const targetName = process.argv[2] ?? "build";
const target = targets[targetName];

if (target === undefined) {
  console.error(`Unknown clean target: ${targetName}`);
  process.exit(1);
}

await rm(target, {
  force: true,
  recursive: true,
  maxRetries: 3,
  retryDelay: 50,
});
