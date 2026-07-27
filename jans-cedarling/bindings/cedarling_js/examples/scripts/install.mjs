#!/usr/bin/env node

/**
 * Temporary pre-publication entry point that installs every example.
 * Remove it after the scoped SDK packages are published.
 */

import { spawnSync } from "node:child_process";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const examplesRoot = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const installer = resolve(examplesRoot, "../scripts/install-example.mjs");
const result = spawnSync(process.execPath, [installer, "--all"], {
  cwd: examplesRoot,
  stdio: "inherit",
});

if (result.error !== undefined) {
  throw result.error;
}

process.exitCode = result.status ?? 1;
