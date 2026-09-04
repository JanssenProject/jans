#!/usr/bin/env node

import { rm } from "node:fs/promises";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
await Promise.all([join(root, "dist"), join(root, ".build")].map((target) => rm(target, {
  force: true,
  recursive: true,
  maxRetries: 3,
  retryDelay: 50,
})));
