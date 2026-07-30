#!/usr/bin/env node

/**
 * Asserts that all production dependencies have exact semantic versions.
 * Prevents publishing the SDK with unsafe local or wildcard dependency paths.
 */

import { readFile } from "node:fs/promises";
import { resolve } from "node:path";

const dependencySections = [
  "dependencies",
  "optionalDependencies",
  "peerDependencies",
];

const exactSemver =
  /^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-[0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*)?(?:\+[0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*)?$/;
function isRecord(value) {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function isPublishableDependency(specification) {
  if (typeof specification !== "string") {
    return false;
  }

  return exactSemver.test(specification);
}

function collectUnsafeDependencies(manifest) {
  const unsafe = [];

  for (const section of dependencySections) {
    const dependencies = manifest[section];
    if (dependencies === undefined) {
      continue;
    }
    if (!isRecord(dependencies)) {
      unsafe.push(`${section} must be an object`);
      continue;
    }

    for (const [name, specification] of Object.entries(dependencies)) {
      if (!isPublishableDependency(specification)) {
        unsafe.push(`${section}.${name}`);
      }
    }
  }

  return unsafe;
}

const manifestPath = resolve(process.argv[2] ?? "package.json");

let manifest;
try {
  manifest = JSON.parse(await readFile(manifestPath, "utf8"));
} catch {
  console.error("Publishable manifest guard could not read a valid package manifest.");
  process.exit(1);
}

if (!isRecord(manifest)) {
  console.error("Publishable manifest guard requires a package manifest object.");
  process.exit(1);
}

const unsafeDependencies = collectUnsafeDependencies(manifest);
if (unsafeDependencies.length > 0) {
  for (const dependency of unsafeDependencies) {
    console.error(
      `Unsafe publish dependency ${dependency}; an exact semantic version is required.`,
    );
  }
  process.exit(1);
}
