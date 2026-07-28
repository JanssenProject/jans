#!/usr/bin/env node

/**
 * Temporary pre-publication installer for locally staged Cedarling packages.
 * Remove it after both scoped packages are published and examples use npm ci.
 */

import { execFile, spawn } from "node:child_process";
import { mkdtemp, readFile, readdir, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { dirname, join, relative, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { promisify } from "node:util";

const execute = promisify(execFile);
const examplesRoot = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const packageRoot = resolve(
  examplesRoot,
  "../../jans-cedarling/bindings/cedarling_js",
);
const sdkPackageName = "@janssenproject/cedarling";
const wasmPackageName = "@janssenproject/cedarling_wasm";
const exactSemver =
  /^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-[0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*)?(?:\+[0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*)?$/;
const consumingExamples = [
  "hono",
  "react-nodejs/backend",
  "react-nodejs/frontend",
  "vercel-nextjs",
  "electron",
];
const supportedExamples = new Set(consumingExamples);

const requestedTarget = process.argv[2] ?? ".";
const installAll = requestedTarget === "--all";
const targetRoots = installAll
  ? consumingExamples.map((directory) => join(examplesRoot, directory))
  : [resolve(process.cwd(), requestedTarget)];

function targetName(targetRoot) {
  return relative(examplesRoot, targetRoot).replaceAll("\\", "/");
}

for (const targetRoot of targetRoots) {
  const name = targetName(targetRoot);
  if (!supportedExamples.has(name)) {
    console.error(
      `Expected one Cedarling example directory, received ${name || "."}.`,
    );
    process.exit(1);
  }
}

const targetVersions = await Promise.all(
  targetRoots.map(async (targetRoot) => {
    const manifest = JSON.parse(
      await readFile(join(targetRoot, "package.json"), "utf8"),
    );
    const version = manifest.dependencies?.[sdkPackageName];
    if (typeof version !== "string" || !exactSemver.test(version)) {
      throw new Error(
        `${targetName(targetRoot)} must declare ${sdkPackageName} at an exact semantic version.`,
      );
    }
    return version;
  }),
);
const installVersion = targetVersions[0];
if (
  installVersion === undefined ||
  targetVersions.some((version) => version !== installVersion)
) {
  throw new Error(
    "All Cedarling examples must declare the same exact SDK version.",
  );
}
const stageRoot = await mkdtemp(join(tmpdir(), "cedarling-example-install-"));
const npmCommand = process.platform === "win32" ? "npm.cmd" : "npm";

async function singleTarball(prefix) {
  const matches = (await readdir(stageRoot)).filter(
    (entry) => entry.startsWith(prefix) && entry.endsWith(".tgz"),
  );
  if (matches.length !== 1 || matches[0] === undefined) {
    throw new Error(`Expected exactly one ${prefix} release artifact.`);
  }
  return join(stageRoot, matches[0]);
}

async function runNpm(arguments_, cwd) {
  await new Promise((resolveRun, rejectRun) => {
    const child = spawn(npmCommand, arguments_, {
      cwd,
      stdio: "inherit",
    });
    child.once("error", rejectRun);
    child.once("close", (code, signal) => {
      if (code === 0) {
        resolveRun();
        return;
      }
      rejectRun(
        new Error(
          `${npmCommand} ${arguments_[0]} failed in ${cwd} (${signal ?? code}).`,
        ),
      );
    });
  });
}

async function installedPackageVersion(targetRoot, packageName) {
  const manifestPath = join(
    targetRoot,
    "node_modules",
    ...packageName.split("/"),
    "package.json",
  );
  const manifest = JSON.parse(await readFile(manifestPath, "utf8"));
  return manifest.version;
}

try {
  if (installAll) {
    await runNpm(
      ["ci", "--no-audit", "--no-fund"],
      join(examplesRoot, "common"),
    );
    console.log("Installed dependencies in common.");
  }

  const stage = await execute(
    process.execPath,
    [
      join(packageRoot, "scripts/stage-release.mjs"),
      "--pack-destination",
      stageRoot,
      "--version",
      installVersion,
    ],
    { cwd: packageRoot },
  );
  process.stdout.write(stage.stdout);
  process.stderr.write(stage.stderr);

  const wasmTarball = await singleTarball(
    `janssenproject-cedarling_wasm-${installVersion}`,
  );
  const sdkTarball = await singleTarball(
    `janssenproject-cedarling-${installVersion}`,
  );
  for (const targetRoot of targetRoots) {
    const name = targetName(targetRoot);
    console.log(`Installing dependencies in ${name}...`);
    await runNpm(
      [
        "install",
        "--no-save",
        "--package-lock=false",
        "--no-audit",
        "--no-fund",
        wasmTarball,
        sdkTarball,
      ],
      targetRoot,
    );
    for (const packageName of [sdkPackageName, wasmPackageName]) {
      const installedVersion = await installedPackageVersion(
        targetRoot,
        packageName,
      );
      if (installedVersion !== installVersion) {
        throw new Error(
          `${name} installed ${packageName}@${installedVersion}; expected ${installVersion}.`,
        );
      }
    }
    await runNpm(
      ["ls", sdkPackageName, "--depth=0"],
      targetRoot,
    );
    console.log(
      `Installed local Cedarling packages at ${installVersion} in ${name}.`,
    );
  }
} finally {
  await rm(stageRoot, {
    force: true,
    recursive: true,
    maxRetries: 3,
    retryDelay: 50,
  });
}
