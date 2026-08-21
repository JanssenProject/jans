#!/usr/bin/env node

import {
  copyFile,
  cp,
  mkdir,
  mkdtemp,
  readFile,
  rm,
  writeFile,
} from "node:fs/promises";
import { tmpdir } from "node:os";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { promisify } from "node:util";

const execute = promisify((await import("node:child_process")).execFile);
const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const exactSemver =
  /^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-[0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*)?(?:\+[0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*)?$/;

function argumentsFrom(values) {
  const options = {};
  for (let index = 0; index < values.length; index += 2) {
    const name = values[index];
    const value = values[index + 1];
    if (
      value === undefined ||
      (name !== "--output" && name !== "--version") ||
      name in options
    ) {
      throw new Error(
        "Usage: stage-packages.mjs --output <directory> [--version <semver>]",
      );
    }
    options[name] = value;
  }
  if (typeof options["--output"] !== "string") {
    throw new Error("--output is required");
  }
  return options;
}

function assertExactDependencies(manifest) {
  for (const section of [
    "dependencies",
    "optionalDependencies",
    "peerDependencies",
  ]) {
    for (const [name, specification] of Object.entries(manifest[section] ?? {})) {
      if (typeof specification !== "string" || !exactSemver.test(specification)) {
        throw new Error(`${section}.${name} must use an exact version`);
      }
    }
  }
}

async function pack(directory, output) {
  const { stdout, stderr } = await execute(
    "npm",
    ["pack", "--json", "--pack-destination", output],
    {
      cwd: directory,
      env: { ...process.env, npm_config_ignore_scripts: "true" },
    },
  );
  let result;
  try {
    result = JSON.parse(stdout);
  } catch (cause) {
    throw new Error(
      `npm pack returned invalid JSON.\nstdout:\n${stdout}\nstderr:\n${stderr}`,
      { cause },
    );
  }
  if (!Array.isArray(result) || result.length !== 1) {
    throw new Error("npm pack did not produce exactly one artifact");
  }
  const files = new Set(result[0].files?.map(({ path }) => path));
  for (const required of [
    "dist/index.js",
    "dist/index.cjs",
    "dist/index.d.ts",
    "dist/edge/index.js",
    "dist/edge/cedarling_wasm_bg.wasm",
  ]) {
    if (!files.has(required)) throw new Error(`Packed SDK omitted ${required}`);
  }
  if ([...files].some((path) => path.includes("node_modules"))) {
    throw new Error("Packed SDK contains a node_modules entry");
  }
}

const options = argumentsFrom(process.argv.slice(2));
const output = resolve(options["--output"]);
const sourceManifest = JSON.parse(
  await readFile(join(root, "package.json"), "utf8"),
);
const version = options["--version"] ?? sourceManifest.version;
if (typeof version !== "string" || !exactSemver.test(version)) {
  throw new Error("The staged package version must be exact semantic version");
}

const stagedManifest = {
  name: sourceManifest.name,
  version,
  description: sourceManifest.description,
  type: sourceManifest.type,
  private: true,
  license: sourceManifest.license,
  repository: sourceManifest.repository,
  sideEffects: sourceManifest.sideEffects,
  engines: sourceManifest.engines,
  types: sourceManifest.types,
  files: sourceManifest.files,
  exports: sourceManifest.exports,
  ...(sourceManifest.dependencies === undefined
    ? {}
    : { dependencies: sourceManifest.dependencies }),
};
assertExactDependencies(stagedManifest);

const stage = await mkdtemp(join(tmpdir(), "cedarling-js-package-"));
try {
  await Promise.all([
    mkdir(output, { recursive: true }),
    cp(join(root, "dist"), join(stage, "dist"), { recursive: true }),
    copyFile(join(root, "README.md"), join(stage, "README.md")),
  ]);
  await writeFile(
    join(stage, "package.json"),
    `${JSON.stringify(stagedManifest, undefined, 2)}\n`,
  );
  await pack(stage, output);
} finally {
  await rm(stage, { force: true, recursive: true, maxRetries: 3, retryDelay: 50 });
}
