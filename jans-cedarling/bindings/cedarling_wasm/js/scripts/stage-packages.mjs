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
const dependencySections = [
  "dependencies",
  "optionalDependencies",
  "peerDependencies",
];

function argumentsFrom(values) {
  const options = {};
  for (let index = 0; index < values.length; index += 1) {
    const name = values[index];
    if (name === "--publishable" && !(name in options)) {
      options[name] = true;
      continue;
    }
    const value = values[index + 1];
    if (
      (name !== "--output" && name !== "--version") ||
      name in options ||
      value === undefined ||
      value.startsWith("--")
    ) {
      throw new Error(
        "Usage: stage-packages.mjs --output <directory> [--version <semver>] [--publishable]",
      );
    }
    options[name] = value;
    index += 1;
  }
  if (typeof options["--output"] !== "string") {
    throw new Error("--output is required");
  }
  return options;
}

function assertExactDependencies(manifest) {
  for (const section of dependencySections) {
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
  const [{ filename, integrity }] = result;
  if (
    typeof filename !== "string" ||
    filename.length === 0 ||
    typeof integrity !== "string" ||
    !integrity.startsWith("sha512-")
  ) {
    throw new Error("npm pack omitted artifact identity metadata");
  }
  const files = new Set(result[0].files?.map(({ path }) => path));
  for (const required of [
    "LICENSE",
    "dist/browser/index.js",
    "dist/browser/index.js.map",
    "dist/esm/index.js",
    "dist/esm/index.js.map",
    "dist/cjs/index.cjs",
    "dist/cjs/index.cjs.map",
    "dist/types/esm/generated.d.ts",
    "dist/types/esm/index.d.ts",
    "dist/types/esm/edge.d.ts",
    "dist/types/cjs/generated.d.ts",
    "dist/types/cjs/index.d.ts",
    "dist/types/cjs/package.json",
    "dist/edge/index.js",
    "dist/edge/index.js.map",
    "dist/manual/index.js",
    "dist/manual/index.js.map",
    "dist/types/esm/manual.d.ts",
    "dist/types/esm/wasm.d.ts",
    "dist/wasm/cedarling_wasm_bg.wasm",
  ]) {
    if (!files.has(required)) throw new Error(`Packed SDK omitted ${required}`);
  }
  if ([...files].some((path) => path.includes("node_modules"))) {
    throw new Error("Packed SDK contains a node_modules entry");
  }
  const wasmFiles = [...files].filter((path) => path.endsWith(".wasm"));
  if (
    wasmFiles.length !== 1 ||
    wasmFiles[0] !== "dist/wasm/cedarling_wasm_bg.wasm"
  ) {
    throw new Error(`Packed SDK must contain exactly one WASM: ${wasmFiles}`);
  }
  const publicJavaScript = new Set([
    "dist/browser/index.js",
    "dist/esm/index.js",
    "dist/cjs/index.cjs",
    "dist/edge/index.js",
    "dist/manual/index.js",
  ]);
  const publicSourceMaps = new Set([
    "dist/browser/index.js.map",
    "dist/esm/index.js.map",
    "dist/cjs/index.cjs.map",
    "dist/edge/index.js.map",
    "dist/manual/index.js.map",
  ]);
  for (const path of files) {
    if (/\.(?:c?js|mjs)$/.test(path) && !publicJavaScript.has(path)) {
      throw new Error(`Packed SDK contains intermediate JavaScript: ${path}`);
    }
    if (/\.(?:c?js|mjs)\.map$/.test(path) && !publicSourceMaps.has(path)) {
      throw new Error(`Packed SDK contains an intermediate source map: ${path}`);
    }
    if (/\.d\.(?:ts|cts|mts)\.map$/.test(path)) {
      throw new Error(`Packed SDK contains a declaration map: ${path}`);
    }
  }
  return { filename, integrity };
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
  ...(options["--publishable"] ? {} : { private: true }),
  license: sourceManifest.license,
  keywords: sourceManifest.keywords,
  homepage: sourceManifest.homepage,
  bugs: sourceManifest.bugs,
  repository: sourceManifest.repository,
  sideEffects: sourceManifest.sideEffects,
  publishConfig: sourceManifest.publishConfig,
  engines: sourceManifest.engines,
  types: sourceManifest.types,
  files: sourceManifest.files,
  exports: sourceManifest.exports,
  ...Object.fromEntries(
    dependencySections
      .filter((section) => sourceManifest[section] !== undefined)
      .map((section) => [section, sourceManifest[section]]),
  ),
  ...(sourceManifest.peerDependenciesMeta === undefined
    ? {}
    : { peerDependenciesMeta: sourceManifest.peerDependenciesMeta }),
};
assertExactDependencies(stagedManifest);

const stage = await mkdtemp(join(tmpdir(), "cedarling-wasm-package-"));
let packed;
try {
  await Promise.all([
    mkdir(output, { recursive: true }),
    cp(join(root, "dist"), join(stage, "dist"), { recursive: true }),
    copyFile(join(root, "README.md"), join(stage, "README.md")),
    copyFile(resolve(root, "../../../..", "LICENSE"), join(stage, "LICENSE")),
  ]);
  await writeFile(
    join(stage, "package.json"),
    `${JSON.stringify(stagedManifest, undefined, 2)}\n`,
  );
  packed = await pack(stage, output);
} finally {
  await rm(stage, { force: true, recursive: true, maxRetries: 3, retryDelay: 50 });
}
process.stdout.write(`${JSON.stringify({
  path: join(output, packed.filename),
  filename: packed.filename,
  integrity: packed.integrity,
  version,
})}\n`);
