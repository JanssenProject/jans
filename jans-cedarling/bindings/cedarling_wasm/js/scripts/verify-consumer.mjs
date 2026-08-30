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

import { build } from "esbuild";

const execute = promisify((await import("node:child_process")).execFile);
const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const sdkName = "@janssenproject/cedarling_wasm";
const defaultVerificationVersion = "0.0.0-consumer-verification";
const exactSemver =
  /^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-[0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*)?(?:\+[0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*)?$/;
const preservedManifestSections = [
  "keywords",
  "homepage",
  "bugs",
  "publishConfig",
  "dependencies",
  "optionalDependencies",
  "peerDependencies",
  "peerDependenciesMeta",
];

function argumentsFrom(values) {
  if (values.length === 0) return {};
  const options = {};
  for (let index = 0; index < values.length; index += 2) {
    const name = values[index];
    const value = values[index + 1];
    if (
      (name !== "--artifact" && name !== "--version") ||
      name in options ||
      value === undefined ||
      value.startsWith("--")
    ) {
      throw new Error(
        "Usage: verify-consumer.mjs [--artifact <tgz> --version <semver>]",
      );
    }
    options[name] = value;
  }
  if (
    typeof options["--artifact"] !== "string" ||
    typeof options["--version"] !== "string" ||
    !exactSemver.test(options["--version"])
  ) {
    throw new Error("--artifact and an exact --version must be used together");
  }
  return options;
}

function output(error) {
  process.stderr.write(error.stderr ?? "");
  process.stdout.write(error.stdout ?? "");
  throw error;
}

async function artifact(directory) {
  const matches = (await readdir(directory)).filter(
    (name) => name.startsWith("janssenproject-cedarling_wasm-") && name.endsWith(".tgz"),
  );
  if (matches.length !== 1) throw new Error("Expected one SDK artifact");
  return join(directory, matches[0]);
}

async function verifyEdgeConsumer(consumer, installedRoot) {
  const entry = join(consumer, "edge-consumer.mjs");
  const output = join(consumer, "edge-output");
  await writeFile(
    entry,
    `export { default, init, initSync, initWasm, initFromArchiveBytes } from "${sdkName}/edge";\n`,
  );
  const result = await build({
    entryPoints: [entry],
    outdir: output,
    assetNames: "[name]",
    bundle: true,
    format: "esm",
    metafile: true,
    write: false,
    plugins: [{
      name: "edge-wasm-module",
      setup(esbuild) {
        esbuild.onResolve({ filter: /\.wasm\?module$/ }, (args) => ({
          path: resolve(args.resolveDir, args.path.slice(0, -"?module".length)),
          namespace: "edge-wasm-module",
        }));
        esbuild.onLoad({ filter: /.*/, namespace: "edge-wasm-module" }, async ({ path }) => ({
          contents: await readFile(path),
          loader: "file",
        }));
      },
    }],
  });
  const wasm = result.outputFiles.filter((file) => file.path.endsWith(".wasm"));
  if (wasm.length !== 1) throw new Error("Packed edge consumer must emit one WASM asset");
  const expected = await readFile(
    join(installedRoot, "dist/wasm/cedarling_wasm_bg.wasm"),
  );
  if (!expected.equals(wasm[0].contents)) {
    throw new Error("Packed edge consumer emitted the wrong WASM asset");
  }
  const javascript = result.outputFiles.find((file) => file.path.endsWith(".js"));
  if (
    javascript === undefined ||
    javascript.text.includes("WebAssembly.compile") ||
    javascript.text.includes("WebAssembly.instantiateStreaming")
  ) {
    throw new Error("Packed edge consumer lost static WASM loading");
  }
}

async function verifyBrowserConsumer(consumer, archive) {
  const entry = join(consumer, "browser-consumer.mjs");
  const browserBundle = join(consumer, "browser-consumer.js");
  await writeFile(entry, `
import initWasm, { initFromArchiveBytes } from "${sdkName}";
let cedarling;
try {
  await initWasm();
  const response = await fetch("/policy.cjar");
  if (!response.ok) throw new Error("Policy archive request failed");
  cedarling = await initFromArchiveBytes({
    CEDARLING_APPLICATION_NAME: "browser-consumer",
    CEDARLING_LOG_TYPE: "memory",
    CEDARLING_LOG_TTL: 120,
    CEDARLING_LOG_LEVEL: "INFO",
    CEDARLING_JWT_SIG_VALIDATION: "disabled",
    CEDARLING_JWT_STATUS_VALIDATION: "disabled",
  }, new Uint8Array(await response.arrayBuffer()));
  const result = await cedarling.authorizeUnsigned(JSON.stringify({
    principal: { cedar_entity_mapping: { entity_type: "Tracer::User", id: "alice" } },
    action: 'Tracer::Action::"Read"',
    resource: { cedar_entity_mapping: { entity_type: "Tracer::Resource", id: "document" } },
    context: {},
  }));
  if (!result.decision) throw new Error("Browser consumer did not authorize");
  if (typeof result.free !== "function" || typeof cedarling.free !== "function") {
    throw new Error("Browser consumer lost generated resource methods");
  }
  result.free();
  globalThis.cedarlingTestResult = { ok: true };
} catch (error) {
  globalThis.cedarlingTestResult = { error: String(error?.stack ?? error) };
} finally {
  if (cedarling !== undefined) {
    await cedarling.shutDown();
    cedarling.free();
  }
}
`);
  await build({
    entryPoints: [entry],
    outfile: browserBundle,
    bundle: true,
    conditions: ["browser"],
    format: "esm",
    platform: "browser",
    target: "es2022",
  });
  const { stdout, stderr } = await execute(process.execPath, [
    "--test",
    join(root, "tests/browser.test.mjs"),
  ], {
    cwd: root,
    env: {
      ...process.env,
      CEDARLING_BROWSER_ARCHIVE: archive,
      CEDARLING_BROWSER_BUNDLE: browserBundle,
    },
  }).catch(output);
  process.stdout.write(stdout);
  process.stderr.write(stderr);
}

async function verifyManualBrowserConsumer(consumer, archive) {
  const entry = join(consumer, "manual-browser-consumer.mjs");
  const output = join(consumer, "manual-browser-output");
  await writeFile(entry, `
import initWasm, { initFromArchiveBytes } from "${sdkName}/manual";
import wasmUrl from "${sdkName}/wasm";
let cedarling;
try {
  await initWasm(wasmUrl);
  const response = await fetch("/policy.cjar");
  if (!response.ok) throw new Error("Policy archive request failed");
  cedarling = await initFromArchiveBytes({
    CEDARLING_APPLICATION_NAME: "manual-browser-consumer",
    CEDARLING_LOG_TYPE: "memory",
    CEDARLING_LOG_TTL: 120,
    CEDARLING_LOG_LEVEL: "INFO",
    CEDARLING_JWT_SIG_VALIDATION: "disabled",
    CEDARLING_JWT_STATUS_VALIDATION: "disabled",
  }, new Uint8Array(await response.arrayBuffer()));
  const result = await cedarling.authorizeUnsigned(JSON.stringify({
    principal: { cedar_entity_mapping: { entity_type: "Tracer::User", id: "alice" } },
    action: 'Tracer::Action::"Read"',
    resource: { cedar_entity_mapping: { entity_type: "Tracer::Resource", id: "document" } },
    context: {},
  }));
  if (!result.decision) throw new Error("Manual browser consumer did not authorize");
  result.free();
  globalThis.cedarlingTestResult = { ok: true };
} catch (error) {
  globalThis.cedarlingTestResult = { error: String(error?.stack ?? error) };
} finally {
  if (cedarling !== undefined) {
    await cedarling.shutDown();
    cedarling.free();
  }
}
`);
  const result = await build({
    entryPoints: [entry],
    outdir: output,
    assetNames: "[name]-[hash]",
    bundle: true,
    format: "esm",
    loader: { ".wasm": "file" },
    metafile: true,
    write: true,
  });
  const wasm = Object.keys(result.metafile.outputs)
    .map((path) => resolve(path))
    .filter((path) => path.endsWith(".wasm"));
  if (wasm.length !== 1) throw new Error("Manual browser consumer must emit one WASM asset");
  const javascript = Object.keys(result.metafile.outputs)
    .map((path) => resolve(path))
    .find((path) => path.endsWith(".js"));
  if (javascript === undefined) throw new Error("Manual browser consumer omitted JavaScript");
  const { stdout, stderr } = await execute(process.execPath, [
    "--test",
    join(root, "tests/browser.test.mjs"),
  ], {
    cwd: root,
    env: {
      ...process.env,
      CEDARLING_BROWSER_ARCHIVE: archive,
      CEDARLING_BROWSER_BUNDLE: javascript,
      CEDARLING_BROWSER_WASM: wasm[0],
      CEDARLING_BROWSER_EXPECT_WASM_REQUEST: "true",
    },
  }).catch(output);
  process.stdout.write(stdout);
  process.stderr.write(stderr);
}

const options = argumentsFrom(process.argv.slice(2));
const externalArtifact = options["--artifact"] === undefined
  ? undefined
  : resolve(options["--artifact"]);
const verificationVersion = options["--version"] ?? defaultVerificationVersion;
const sourceManifest = JSON.parse(
  await readFile(join(root, "package.json"), "utf8"),
);
if (
  externalArtifact === undefined &&
  verificationVersion === sourceManifest.version
) {
  throw new Error("The consumer verification version must override the source");
}

const temporary = await mkdtemp(join(tmpdir(), "cedarling-wasm-consumer-"));
try {
  const artifacts = join(temporary, "artifacts");
  const consumer = join(temporary, "consumer");
  await mkdir(consumer, { recursive: true });
  let sdk = externalArtifact;
  if (sdk === undefined) {
    await mkdir(artifacts, { recursive: true });
    const { stdout, stderr } = await execute(process.execPath, [
      join(root, "scripts/stage-packages.mjs"),
      "--output",
      artifacts,
      "--version",
      verificationVersion,
    ], { cwd: root }).catch(output);
    process.stdout.write(stdout);
    process.stderr.write(stderr);
    sdk = await artifact(artifacts);
  } else {
    await access(sdk);
  }
  await writeFile(join(consumer, "package.json"), JSON.stringify({
    name: "cedarling-installed-consumer",
    private: true,
    type: "module",
  }));
  await execute("npm", [
    "install",
    "--ignore-scripts",
    "--no-audit",
    "--no-fund",
    "--no-package-lock",
    "--offline",
    sdk,
  ], { cwd: consumer }).catch(output);

  const installedRoot = join(
    consumer,
    "node_modules",
    "@janssenproject",
    "cedarling_wasm",
  );
  const installed = JSON.parse(
    await readFile(join(installedRoot, "package.json"), "utf8"),
  );
  await Promise.all([
    access(join(installedRoot, "LICENSE")),
    access(join(installedRoot, "dist/browser/index.js")),
    access(join(installedRoot, "dist/esm/index.js")),
    access(join(installedRoot, "dist/cjs/index.cjs")),
    access(join(installedRoot, "dist/edge/index.js")),
    access(join(installedRoot, "dist/manual/index.js")),
    access(join(installedRoot, "dist/types/esm/index.d.ts")),
    access(join(installedRoot, "dist/types/esm/edge.d.ts")),
    access(join(installedRoot, "dist/types/esm/manual.d.ts")),
    access(join(installedRoot, "dist/types/esm/wasm.d.ts")),
    access(join(installedRoot, "dist/types/cjs/index.d.ts")),
    access(join(installedRoot, "dist/types/cjs/package.json")),
    access(join(installedRoot, "dist/wasm/cedarling_wasm_bg.wasm")),
  ]);
  const wasmFiles = (await readdir(join(installedRoot, "dist"), { recursive: true }))
    .filter((path) => path.endsWith(".wasm"));
  if (wasmFiles.length !== 1 || wasmFiles[0] !== "wasm/cedarling_wasm_bg.wasm") {
    throw new Error(`Installed SDK must contain exactly one WASM: ${wasmFiles}`);
  }

  if (
    installed.name !== sdkName ||
    installed.version !== verificationVersion ||
    (externalArtifact === undefined
      ? installed.private !== true
      : Object.hasOwn(installed, "private")) ||
    installed.types !== "./dist/types/esm/index.d.ts" ||
    preservedManifestSections.some(
      (section) =>
        JSON.stringify(installed[section]) !==
        JSON.stringify(sourceManifest[section]),
    )
  ) {
    throw new Error("Installed SDK manifest violates the package contract");
  }
  await verifyEdgeConsumer(consumer, installedRoot);

  const typeTests = join(consumer, "type-tests");
  const esmTypes = join(typeTests, "esm");
  const commonJsTypes = join(typeTests, "commonjs");
  await Promise.all([
    mkdir(esmTypes, { recursive: true }),
    mkdir(commonJsTypes, { recursive: true }),
  ]);
  const esmTypeConsumer = `
import initWasm, { init, initSync, initWasm as namedInitWasm, initFromArchiveBytes } from "${sdkName}";
import type { AuthorizeResult, Cedarling, InitInput, InitOutput } from "${sdkName}";
import generatedInitWasm, { Cedarling as GeneratedCedarling, init as generatedInit } from "${sdkName}/manual";
import { initWasm as edgeInitWasm } from "${sdkName}/edge";
import wasmUrl from "${sdkName}/wasm";
void initWasm;
void init;
void initSync;
void namedInitWasm;
void initFromArchiveBytes;
void generatedInitWasm;
void GeneratedCedarling;
void generatedInit;
void edgeInitWasm;
edgeInitWasm();
// @ts-expect-error Edge initialization receives its static module from the host.
edgeInitWasm(new Uint8Array());
void wasmUrl;
declare const cedarling: Cedarling;
declare const result: AuthorizeResult;
declare const input: InitInput;
declare const output: InitOutput;
cedarling.free();
cedarling[Symbol.dispose]();
result.free();
void input;
void output;
`;
  await Promise.all([
    writeFile(join(esmTypes, "package.json"), JSON.stringify({ type: "module" })),
    writeFile(join(esmTypes, "index.ts"), esmTypeConsumer),
    writeFile(join(commonJsTypes, "package.json"), JSON.stringify({ type: "commonjs" })),
    writeFile(join(commonJsTypes, "index.ts"), `
import cedarling = require("${sdkName}");
import type { AuthorizeResult, Cedarling } from "${sdkName}";
void cedarling.default;
void cedarling.init;
void cedarling.initSync;
void cedarling.initWasm;
void cedarling.initFromArchiveBytes;
declare const client: Cedarling;
declare const result: AuthorizeResult;
client.free();
client[Symbol.dispose]();
result.free();
`),
    writeFile(join(typeTests, "tsconfig.json"), JSON.stringify({
      compilerOptions: {
        target: "ES2022",
        module: "Node16",
        moduleResolution: "Node16",
        lib: ["ES2022", "DOM"],
        types: [],
        strict: true,
        noEmit: true,
        skipLibCheck: false,
      },
      include: ["esm/index.ts", "commonjs/index.ts"],
    })),
  ]);
  await execute(process.execPath, [
    join(root, "node_modules/typescript/bin/tsc"),
    "--project",
    join(typeTests, "tsconfig.json"),
  ], { cwd: consumer }).catch(output);

  await writeFile(join(consumer, "verify.mjs"), `
import { readFile } from "node:fs/promises";
import { createRequire } from "node:module";
const edge = import.meta.resolve("${sdkName}/edge");
const manual = import.meta.resolve("${sdkName}/manual");
const wasm = import.meta.resolve("${sdkName}/wasm");
const expectedEdge = new URL(
  "./node_modules/@janssenproject/cedarling_wasm/dist/edge/index.js",
  import.meta.url,
).href;
if (edge !== expectedEdge) {
  throw new Error("The edge export resolved to an unexpected target");
}
if (!manual.endsWith("/dist/manual/index.js")) {
  throw new Error("The manual export resolved to an unexpected target");
}
if (!wasm.endsWith("/dist/wasm/cedarling_wasm_bg.wasm")) {
  throw new Error("The WASM export resolved to an unexpected target");
}
const esm = await import("${sdkName}");
const cjs = createRequire(import.meta.url)("${sdkName}");
const manualEntry = await import("${sdkName}/manual");
const archive = new Uint8Array(await readFile(process.argv[2]));
if (typeof manualEntry.Cedarling !== "function") {
  throw new Error("The manual export omitted generated runtime values");
}
for (const [label, entry] of [["ESM", esm], ["CommonJS", cjs]]) {
  if (
    typeof entry.default !== "function" ||
    typeof entry.init !== "function" ||
    typeof entry.initSync !== "function" ||
    typeof entry.initWasm !== "function" ||
    typeof entry.initFromArchiveBytes !== "function"
  ) {
    throw new Error(label + " omitted a generated initialization export");
  }
  await entry.default();
  const cedarling = await entry.initFromArchiveBytes({
    CEDARLING_APPLICATION_NAME: "installed-" + label.toLowerCase(),
    CEDARLING_LOG_TYPE: "memory",
    CEDARLING_LOG_TTL: 120,
    CEDARLING_LOG_LEVEL: "INFO",
    CEDARLING_JWT_SIG_VALIDATION: "disabled",
    CEDARLING_JWT_STATUS_VALIDATION: "disabled",
  }, archive);
  const result = await cedarling.authorizeUnsigned(JSON.stringify({
    principal: { cedar_entity_mapping: { entity_type: "Tracer::User", id: "alice" } },
    action: 'Tracer::Action::"Read"',
    resource: { cedar_entity_mapping: { entity_type: "Tracer::Resource", id: "document" } },
    context: {},
  }));
  if (!result.decision) throw new Error(label + " consumer did not authorize");
  if (typeof result.free !== "function" || typeof cedarling.free !== "function") {
    throw new Error(label + " consumer lost generated resource methods");
  }
  result.free();
  await cedarling.shutDown();
  cedarling.free();
}
`);
  const execution = await execute(process.execPath, [
    "verify.mjs",
    join(root, "tests/fixtures/tracer-policy-store.cjar"),
  ], { cwd: consumer }).catch(output);
  process.stdout.write(execution.stdout);
  process.stderr.write(execution.stderr);
  await verifyBrowserConsumer(
    consumer,
    join(root, "tests/fixtures/tracer-policy-store.cjar"),
  );
  await verifyManualBrowserConsumer(
    consumer,
    join(root, "tests/fixtures/tracer-policy-store.cjar"),
  );
} finally {
  await rm(temporary, { force: true, recursive: true, maxRetries: 3, retryDelay: 50 });
}
