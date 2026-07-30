#!/usr/bin/env node

/**
 * Stages the packages for release in a temporary workspace.
 * It compiles the SDK, bundles the WASM and JS packages, rewrites their manifests
 * (package.json) to convert workspace 'file:' dependencies to exact release version numbers,
 * and packages them using 'npm pack'.
 */

import {
  cp,
  copyFile,
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
const packageRoot = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const wasmPackageRoot = resolve(packageRoot, "../cedarling_wasm/pkg");
const wasmPackageName = "@janssenproject/cedarling_wasm";
const exactSemver =
  /^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-[0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*)?(?:\+[0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*)?$/;

const arguments_ = process.argv.slice(2);
const destinationIndex = arguments_.indexOf("--pack-destination");
const destinationValue =
  destinationIndex === -1 ? packageRoot : arguments_[destinationIndex + 1];
if (destinationValue === undefined) {
  console.error("--pack-destination requires a directory path.");
  process.exit(1);
}
const packDestination = resolve(destinationValue);
const dryRun = arguments_.includes("--dry-run");
const versionIndex = arguments_.indexOf("--version");
const versionValue =
  versionIndex === -1 ? undefined : arguments_[versionIndex + 1];

if (
  versionIndex !== -1 &&
  (versionValue === undefined || !exactSemver.test(versionValue))
) {
  console.error("--version requires an exact semantic version.");
  process.exit(1);
}

const sourceManifest = JSON.parse(
  await readFile(join(packageRoot, "package.json"), "utf8"),
);
const sourceWasmManifest = JSON.parse(
  await readFile(join(wasmPackageRoot, "package.json"), "utf8"),
);
const releaseVersion = versionValue ?? sourceManifest.version;

if (typeof releaseVersion !== "string" || !exactSemver.test(releaseVersion)) {
  console.error("The SDK package must declare an exact semantic version.");
  process.exit(1);
}

const stageDirectory = await mkdtemp(
  join(tmpdir(), "cedarling-js-release-stage-"),
);
const sdkStage = join(stageDirectory, "sdk");
const wasmStage = join(stageDirectory, "wasm");

async function pack(directory) {
  const packArguments = [
    "pack",
    "--json",
    "--pack-destination",
    packDestination,
  ];
  if (dryRun) {
    packArguments.splice(1, 0, "--dry-run");
  }

  const { stdout, stderr } = await execute("npm", packArguments, {
    cwd: directory,
  });
  if (stdout.length > 0) process.stdout.write(stdout);
  if (stderr.length > 0) process.stderr.write(stderr);
}

try {
  await execute("npm", ["run", "build"], { cwd: packageRoot });
  await Promise.all([
    mkdir(packDestination, { recursive: true }),
    mkdir(join(sdkStage, "scripts"), { recursive: true }),
    cp(wasmPackageRoot, wasmStage, { recursive: true }),
  ]);
  await Promise.all([
    cp(join(packageRoot, "dist"), join(sdkStage, "dist"), {
      recursive: true,
    }),
    copyFile(
      join(packageRoot, "README.md"),
      join(sdkStage, "README.md"),
    ),
    copyFile(
      join(packageRoot, "scripts/assert-publishable.mjs"),
      join(sdkStage, "scripts/assert-publishable.mjs"),
    ),
  ]);

  const stagedWasmManifest = {
    ...sourceWasmManifest,
    name: wasmPackageName,
    version: releaseVersion,
  };
  await writeFile(
    join(wasmStage, "package.json"),
    `${JSON.stringify(stagedWasmManifest, undefined, 2)}\n`,
  );

  const stagedSdkManifest = {
    name: sourceManifest.name,
    version: releaseVersion,
    description: sourceManifest.description,
    type: sourceManifest.type,
    license: sourceManifest.license,
    repository: sourceManifest.repository,
    sideEffects: sourceManifest.sideEffects,
    engines: sourceManifest.engines,
    exports: sourceManifest.exports,
    dependencies: {
      ...sourceManifest.dependencies,
      [wasmPackageName]: releaseVersion,
    },
    files: Array.from(new Set([
      ...(Array.isArray(sourceManifest.files) ? sourceManifest.files : []),
      "scripts/assert-publishable.mjs",
    ])),
    scripts: {
      prepack: "node scripts/assert-publishable.mjs",
    },
  };
  await writeFile(
    join(sdkStage, "package.json"),
    `${JSON.stringify(stagedSdkManifest, undefined, 2)}\n`,
  );

  await pack(wasmStage);
  await pack(sdkStage);
} finally {
  await rm(stageDirectory, {
    force: true,
    recursive: true,
    maxRetries: 3,
    retryDelay: 50,
  });
}
