#!/usr/bin/env node

import {
  access,
  mkdir,
  mkdtemp,
  readFile,
  readdir,
  rm,
  writeFile,
} from "node:fs/promises";
import { tmpdir } from "node:os";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { promisify } from "node:util";

const execute = promisify((await import("node:child_process")).execFile);
const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const sdkName = "@janssenproject/cedarling";
const wasmName = "@janssenproject/cedarling_wasm";

async function artifact(directory) {
  const matches = (await readdir(directory)).filter(
    (name) => name.startsWith("janssenproject-cedarling-") &&
      name.endsWith(".tgz"),
  );
  if (matches.length !== 1) throw new Error("Expected one SDK artifact");
  return join(directory, matches[0]);
}

const sourceManifest = JSON.parse(
  await readFile(join(root, "package.json"), "utf8"),
);
const temporary = await mkdtemp(join(tmpdir(), "cedarling-js-consumer-"));

try {
  const artifacts = join(temporary, "artifacts");
  const consumer = join(temporary, "consumer");
  const cache = join(temporary, "npm-cache");
  await Promise.all([
    mkdir(artifacts, { recursive: true }),
    mkdir(consumer, { recursive: true }),
  ]);
  await execute(process.execPath, [
    join(root, "scripts/stage-packages.mjs"),
    "--output",
    artifacts,
  ], { cwd: root });

  const sdk = await artifact(artifacts);
  await writeFile(join(consumer, "package.json"), JSON.stringify({
    name: "cedarling-installed-consumer",
    private: true,
    type: "module",
  }));
  // Offline installation proves the staged SDK has no hidden runtime packages.
  await execute("npm", [
    "install",
    "--ignore-scripts",
    "--no-audit",
    "--no-fund",
    "--no-package-lock",
    "--offline",
    sdk,
  ], {
    cwd: consumer,
    env: { ...process.env, npm_config_cache: cache },
  });

  const installedRoot = join(
    consumer,
    "node_modules",
    "@janssenproject",
    "cedarling",
  );
  const installed = JSON.parse(
    await readFile(join(installedRoot, "package.json"), "utf8"),
  );
  await Promise.all([
    access(join(installedRoot, "dist/index.js")),
    access(join(installedRoot, "dist/index.cjs")),
    access(join(installedRoot, "dist/index.d.ts")),
    access(join(installedRoot, "dist/edge/index.js")),
    access(join(installedRoot, "dist/edge/cedarling_wasm_bg.wasm")),
  ]);
  if (
    installed.version !== sourceManifest.version ||
    installed.private !== true ||
    installed.types !== "./dist/index.d.ts" ||
    installed.dependencies?.[wasmName] !== undefined
  ) {
    throw new Error("Installed SDK manifest violates the package contract");
  }
  try {
    await access(join(consumer, "node_modules", ...wasmName.split("/")));
    throw new Error("The generated WASM package leaked into the consumer");
  } catch (error) {
    if (error?.code !== "ENOENT") throw error;
  }

  await writeFile(join(consumer, "verify.mjs"), `
import { readFile } from "node:fs/promises";
import { createRequire } from "node:module";
const esm = await import("${sdkName}");
const cjs = createRequire(import.meta.url)("${sdkName}");
const archive = new Uint8Array(await readFile(process.argv[2]));
for (const [label, entry] of [["ESM", esm], ["CommonJS", cjs]]) {
  if (Object.keys(entry).join(",") !== "createCedarling") {
    throw new Error(label + " exposed an unexpected runtime surface");
  }
  const created = await entry.createCedarling({
    applicationName: "installed-" + label.toLowerCase(),
    policyStore: { type: "archive", bytes: archive },
  });
  if (!created.ok) throw created.error;
  const result = await created.value.authorizeUnsigned({
    principal: { type: "Tracer::User", id: "alice" },
    action: 'Tracer::Action::"Read"',
    resource: { type: "Tracer::Resource", id: "document" },
  });
  if (!result.ok || !result.value.decision) {
    throw new Error(label + " consumer did not authorize");
  }
  const shutdown = await created.value.shutDown();
  if (!shutdown.ok) throw shutdown.error;
}
`);
  const { stdout, stderr } = await execute(process.execPath, [
    "verify.mjs",
    join(root, "tests/fixtures/tracer-policy-store.cjar"),
  ], { cwd: consumer });
  process.stdout.write(stdout);
  process.stderr.write(stderr);
} finally {
  await rm(temporary, {
    force: true,
    recursive: true,
    maxRetries: 3,
    retryDelay: 50,
  });
}
