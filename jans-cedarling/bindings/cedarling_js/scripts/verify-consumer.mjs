#!/usr/bin/env node

/**
 * Integration test helper that simulates package usage by an end-user.
 * Stages the release packages, installs them offline in a simulated environment,
 * checks package layout/hoisting correctness, and validates ESM and CommonJS
 * entrypoints under Node.
 */

import { execFile } from "node:child_process";
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

const execute = promisify(execFile);
const packageRoot = resolve(dirname(fileURLToPath(import.meta.url)), "..");

async function releaseTarball(directory, prefix) {
  const tarballs = (await readdir(directory)).filter(
    (entry) => entry.startsWith(prefix) && entry.endsWith(".tgz"),
  );
  if (tarballs.length !== 1 || tarballs[0] === undefined) {
    throw new Error(`Expected exactly one ${prefix} release artifact.`);
  }
  return join(directory, tarballs[0]);
}

const temporaryRoot = await mkdtemp(
  join(tmpdir(), "cedarling-consumer-sim-"),
);
try {
  const artifacts = join(temporaryRoot, "artifacts");
  const consumerRoot = join(temporaryRoot, "consumer");
  const npmCache = join(temporaryRoot, "npm-cache");
  await Promise.all([
    mkdir(artifacts, { recursive: true }),
    mkdir(consumerRoot, { recursive: true }),
  ]);

  await execute(process.execPath, [
    join(packageRoot, "scripts/stage-release.mjs"),
    "--pack-destination",
    artifacts,
  ], { cwd: packageRoot });

  const sdkTarball = await releaseTarball(
    artifacts,
    "janssenproject-cedarling-",
  );
  const wasmTarball = await releaseTarball(
    artifacts,
    "janssenproject-cedarling_wasm-",
  );
  await writeFile(join(consumerRoot, "package.json"), JSON.stringify({
    name: "cedarling-packed-consumer-sim",
    private: true,
    dependencies: {
      "@janssenproject/cedarling": `file:${sdkTarball}`,
      "@janssenproject/cedarling_wasm": `file:${wasmTarball}`,
    },
  }));
  await writeFile(join(consumerRoot, "verify.mjs"), `
import { readFile } from "node:fs/promises";
import { createCedarling } from "@janssenproject/cedarling";
const bytes = new Uint8Array(await readFile(process.argv[2]));
const result = await createCedarling({
  applicationName: "packed-esm-consumer",
  policyStore: { type: "archive", bytes },
});
if (!result.ok) throw new Error(result.error.code);
const authorized = await result.value.authorizeUnsigned({
  principal: { type: "Tracer::User", id: "alice" },
  action: 'Tracer::Action::"Read"',
  resource: { type: "Tracer::Resource", id: "document" },
});
if (!authorized.ok) throw new Error(authorized.error.code);
if (authorized.value.decision !== true) throw new Error("expected allow");
await result.value.shutDown();
console.log("ESM consumer initialized and authorized");
`);
  await writeFile(join(consumerRoot, "verify.cjs"), `
const { readFile } = require("node:fs/promises");
const { createCedarling } = require("@janssenproject/cedarling");
(async () => {
  const bytes = new Uint8Array(await readFile(process.argv[2]));
  const result = await createCedarling({
    applicationName: "packed-cjs-consumer",
    policyStore: { type: "archive", bytes },
  });
  if (!result.ok) throw new Error(result.error.code);
  const authorized = await result.value.authorizeUnsigned({
    principal: { type: "Tracer::User", id: "alice" },
    action: 'Tracer::Action::"Read"',
    resource: { type: "Tracer::Resource", id: "document" },
  });
  if (!authorized.ok) throw new Error(authorized.error.code);
  if (authorized.value.decision !== true) throw new Error("expected allow");
  await result.value.shutDown();
  console.log("CommonJS consumer initialized and authorized");
})().catch((error) => { console.error(error); process.exitCode = 1; });
`);

  await execute("npm", [
    "install",
    "--ignore-scripts",
    "--no-audit",
    "--no-fund",
    "--no-package-lock",
    "--offline",
  ], {
    cwd: consumerRoot,
    env: { ...process.env, npm_config_cache: npmCache },
  });

  await access(join(
    consumerRoot,
    "node_modules/@janssenproject/cedarling_wasm/package.json",
  ));
  let nestedDependencyExists = true;
  try {
    await access(join(
      consumerRoot,
      "node_modules/@janssenproject/cedarling/node_modules/@janssenproject/cedarling_wasm",
    ));
  } catch (error) {
    if (error?.code !== "ENOENT") throw error;
    nestedDependencyExists = false;
  }
  if (nestedDependencyExists) {
    throw new Error("The generated dependency was not hoisted.");
  }

  const archivePath = join(
    packageRoot,
    "tests/fixtures/tracer-policy-store.cjar",
  );
  const esm = await execute(process.execPath, ["verify.mjs", archivePath], {
    cwd: consumerRoot,
  });
  const cjs = await execute(process.execPath, ["verify.cjs", archivePath], {
    cwd: consumerRoot,
  });
  process.stdout.write(esm.stdout);
  process.stdout.write(cjs.stdout);
} finally {
  await rm(temporaryRoot, {
    force: true,
    recursive: true,
    maxRetries: 3,
    retryDelay: 50,
  });
}
