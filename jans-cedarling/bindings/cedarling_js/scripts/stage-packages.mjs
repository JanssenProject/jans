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
const wasmRoot = resolve(root, "../cedarling_wasm/pkg");
const wasmName = "@janssenproject/cedarling_wasm";
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
    const dependencies = manifest[section];
    if (dependencies === undefined) continue;
    if (
      typeof dependencies !== "object" ||
      dependencies === null ||
      Array.isArray(dependencies)
    ) {
      throw new Error(`${section} must be an object`);
    }
    for (const [name, specification] of Object.entries(dependencies)) {
      if (typeof specification !== "string" || !exactSemver.test(specification)) {
        throw new Error(`${section}.${name} must use an exact version`);
      }
    }
  }
}

async function pack(directory, output) {
  const { stdout } = await execute(
    "npm",
    ["pack", "--json", "--pack-destination", output],
    {
      cwd: directory,
      env: { ...process.env, npm_config_ignore_scripts: "true" },
    },
  );
  const result = JSON.parse(stdout);
  if (!Array.isArray(result) || result.length !== 1) {
    throw new Error("npm pack did not produce exactly one artifact");
  }
}

const options = argumentsFrom(process.argv.slice(2));
const output = resolve(options["--output"]);
const sdkManifest = JSON.parse(
  await readFile(join(root, "package.json"), "utf8"),
);
const wasmManifest = JSON.parse(
  await readFile(join(wasmRoot, "package.json"), "utf8"),
);
const version = options["--version"] ?? sdkManifest.version;

if (typeof version !== "string" || !exactSemver.test(version)) {
  throw new Error("The staged package version must be exact semantic version");
}

const stagedSdkManifest = {
  name: sdkManifest.name,
  version,
  description: sdkManifest.description,
  type: sdkManifest.type,
  private: true,
  license: sdkManifest.license,
  repository: sdkManifest.repository,
  sideEffects: sdkManifest.sideEffects,
  engines: sdkManifest.engines,
  files: sdkManifest.files,
  exports: sdkManifest.exports,
  dependencies: { ...sdkManifest.dependencies, [wasmName]: version },
};
const stagedWasmManifest = {
  ...wasmManifest,
  version,
  private: true,
};
assertExactDependencies(stagedSdkManifest);
assertExactDependencies(stagedWasmManifest);

const stage = await mkdtemp(join(tmpdir(), "cedarling-js-packages-"));
const sdkStage = join(stage, "sdk");
const wasmStage = join(stage, "wasm");

try {
  await Promise.all([
    mkdir(output, { recursive: true }),
    mkdir(sdkStage, { recursive: true }),
    cp(wasmRoot, wasmStage, { recursive: true }),
  ]);
  await Promise.all([
    cp(join(root, "dist"), join(sdkStage, "dist"), { recursive: true }),
    copyFile(join(root, "README.md"), join(sdkStage, "README.md")),
  ]);
  await Promise.all([
    writeFile(
      join(sdkStage, "package.json"),
      `${JSON.stringify(stagedSdkManifest, undefined, 2)}\n`,
    ),
    writeFile(
      join(wasmStage, "package.json"),
      `${JSON.stringify(stagedWasmManifest, undefined, 2)}\n`,
    ),
  ]);
  await pack(wasmStage, output);
  await pack(sdkStage, output);
} finally {
  await rm(stage, { force: true, recursive: true, maxRetries: 3, retryDelay: 50 });
}
