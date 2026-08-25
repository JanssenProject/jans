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
const defaultVerificationVersion = "0.0.0-consumer-verification";
const exactSemver =
  /^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-[0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*)?(?:\+[0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*)?$/;
const preservedManifestSections = [
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

async function artifact(directory) {
  const matches = (await readdir(directory)).filter(
    (name) => name.startsWith("janssenproject-cedarling-") &&
      name.endsWith(".tgz"),
  );
  if (matches.length !== 1) throw new Error("Expected one SDK artifact");
  return join(directory, matches[0]);
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
const temporary = await mkdtemp(join(tmpdir(), "cedarling-js-consumer-"));

try {
  const artifacts = join(temporary, "artifacts");
  const consumer = join(temporary, "consumer");
  const cache = join(temporary, "npm-cache");
  await mkdir(consumer, { recursive: true });
  let sdk = externalArtifact;
  if (sdk === undefined) {
    await mkdir(artifacts, { recursive: true });
    await execute(process.execPath, [
      join(root, "scripts/stage-packages.mjs"),
      "--output",
      artifacts,
      "--version",
      verificationVersion,
    ], { cwd: root });
    sdk = await artifact(artifacts);
  } else {
    await access(sdk);
  }
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
    access(join(installedRoot, "dist/esm/index.js")),
    access(join(installedRoot, "dist/esm/index.d.ts")),
    access(join(installedRoot, "dist/cjs/index.cjs")),
    access(join(installedRoot, "dist/edge/index.js")),
    access(join(installedRoot, "dist/edge/cedarling_wasm_bg.wasm")),
  ]);
  if (
    installed.name !== sdkName ||
    installed.version !== verificationVersion ||
    (externalArtifact === undefined
      ? installed.private !== true
      : Object.hasOwn(installed, "private")) ||
    installed.types !== "./dist/esm/index.d.ts" ||
    installed.dependencies?.[wasmName] !== undefined ||
    preservedManifestSections.some(
      (section) =>
        JSON.stringify(installed[section]) !==
          JSON.stringify(sourceManifest[section]),
    )
  ) {
    throw new Error("Installed SDK manifest violates the package contract");
  }
  try {
    await access(join(consumer, "node_modules", ...wasmName.split("/")));
    throw new Error("The generated WASM package leaked into the consumer");
  } catch (error) {
    if (error?.code !== "ENOENT") throw error;
  }

  const typeTests = join(consumer, "type-tests");
  const esmTypes = join(typeTests, "esm");
  const commonJsTypes = join(typeTests, "commonjs");
  await Promise.all([
    mkdir(esmTypes, { recursive: true }),
    mkdir(commonJsTypes, { recursive: true }),
  ]);
  const typeConsumer = `
import { createCedarling } from "${sdkName}";
void createCedarling;
`;
  await Promise.all([
    writeFile(join(esmTypes, "package.json"), JSON.stringify({ type: "module" })),
    writeFile(join(esmTypes, "index.ts"), typeConsumer),
    writeFile(
      join(commonJsTypes, "package.json"),
      JSON.stringify({ type: "commonjs" }),
    ),
    writeFile(join(commonJsTypes, "index.ts"), typeConsumer),
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
  ], { cwd: consumer });

  await writeFile(join(consumer, "verify.mjs"), `
import { readFile } from "node:fs/promises";
import { createRequire } from "node:module";
const edge = import.meta.resolve("${sdkName}/edge");
const expectedEdge = new URL(
  "./node_modules/@janssenproject/cedarling/dist/edge/index.js",
  import.meta.url,
).href;
if (edge !== expectedEdge) {
  throw new Error("The edge export resolved to an unexpected target");
}
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
