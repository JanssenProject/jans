#!/usr/bin/env node

import {
  copyFile,
  mkdir,
  readFile,
  readdir,
  writeFile,
} from "node:fs/promises";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

import { build } from "esbuild";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const compiled = join(root, ".build/src");
const distribution = join(root, "dist");
const browserOutput = join(distribution, "browser");
const esmOutput = join(distribution, "esm");
const commonJsOutput = join(distribution, "cjs");
const edgeOutput = join(distribution, "edge");
const wasmOutput = join(distribution, "wasm");
const typesOutput = join(distribution, "types");
const declarations = join(typesOutput, "esm");
const commonJsDeclarations = join(typesOutput, "cjs");
const wasmPath = resolve(
  root,
  "../cedarling_wasm/pkg/cedarling_wasm_bg.wasm",
);
const generatedGluePath = join(
  root,
  "../cedarling_wasm/pkg/cedarling_wasm.js",
);
const distributedWasmPath = join(wasmOutput, "cedarling_wasm_bg.wasm");
const wasmImport = "../wasm/cedarling_wasm_bg.wasm";
const browserImport = `${wasmImport}?url`;
const browserWasmBinding = "__cedarlingWasmBytes";
const browserImportStatement =
  `import ${browserWasmBinding} from "${browserImport}" with { type: "bytes" };`;
const edgeImport = `${wasmImport}?module`;
const realmSensitiveModuleCheck = `    if (!(module instanceof WebAssembly.Module)) {
        module = new WebAssembly.Module(module);
    }`;
const realmNeutralModuleCheck = `    try {
        WebAssembly.Module.exports(module);
    } catch {
        throw new TypeError('Cedarling initialization requires a WebAssembly.Module');
    }`;

const generatedGlue = {
  name: "cedarling-generated-glue",
  setup(build) {
    build.onLoad({ filter: /cedarling_wasm\.js$/ }, async ({ path }) => {
      if (resolve(path) !== generatedGluePath) return undefined;
      const source = await readFile(path, "utf8");
      if (!source.includes(realmSensitiveModuleCheck)) {
        throw new Error("Generated glue changed its WebAssembly.Module check");
      }
      return {
        contents: source.replace(
          realmSensitiveModuleCheck,
          realmNeutralModuleCheck,
        ),
        loader: "js",
      };
    });
  },
};

const embeddedWasm = {
  name: "cedarling-embedded-wasm",
  setup(build) {
    build.onResolve(
      { filter: /^cedarling:wasm-bytes$/ },
      () => ({ path: "wasm-bytes", namespace: "cedarling" }),
    );
    build.onLoad(
      { filter: /^wasm-bytes$/, namespace: "cedarling" },
      () => ({
        contents: `export default ${browserWasmBinding};`,
        loader: "js",
      }),
    );
  },
};

function nodeWasmFile(format) {
  return {
    name: `cedarling-node-wasm-file-${format}`,
    setup(build) {
      build.onResolve(
        { filter: /^cedarling:wasm-file$/ },
        () => ({ path: "wasm-file", namespace: "cedarling" }),
      );
      build.onLoad(
        { filter: /^wasm-file$/, namespace: "cedarling" },
        () => ({
          contents: format === "esm"
            ? `import { readFile } from "node:fs/promises";
const wasmUrl = new URL(${JSON.stringify(wasmImport)}, import.meta.url);
export default () => readFile(wasmUrl);`
            : `import { readFile } from "node:fs/promises";
import { pathToFileURL } from "node:url";
const wasmUrl = new URL(${JSON.stringify(wasmImport)}, pathToFileURL(__filename));
export default () => readFile(wasmUrl);`,
          loader: "js",
        }),
      );
    },
  };
}

const precompiledWasm = {
  name: "cedarling-precompiled-wasm",
  setup(build) {
    build.onResolve(
      { filter: /cedarling_wasm_bg\.wasm\?module$/ },
      () => ({ path: edgeImport, external: true }),
    );
  },
};

function outputImports(result) {
  return Object.values(result.metafile.outputs)
    .flatMap((output) => output.imports);
}

function assertTextExcludes(label, source, values) {
  for (const value of values) {
    if (source.includes(value)) {
      throw new Error(`${label} retained forbidden output text: ${value}`);
    }
  }
}

async function assertSelfContainedSourceMap(label, path) {
  const map = JSON.parse(await readFile(path, "utf8"));
  if (
    !Array.isArray(map.sources) ||
    !Array.isArray(map.sourcesContent) ||
    map.sources.length !== map.sourcesContent.length ||
    map.sourcesContent.some((source) => typeof source !== "string")
  ) {
    throw new Error(`${label} source map does not contain every source`);
  }
}

async function copyDeclarations(source, destination, relative = "") {
  const entries = await readdir(join(source, relative), { withFileTypes: true });
  await Promise.all(entries.map(async (entry) => {
    const path = join(relative, entry.name);
    if (entry.isDirectory()) {
      await copyDeclarations(source, destination, path);
    } else if (entry.isFile() && entry.name.endsWith(".d.ts")) {
      const target = join(destination, path);
      await mkdir(dirname(target), { recursive: true });
      await copyFile(join(source, path), target);
    }
  }));
}

await Promise.all([
  mkdir(browserOutput, { recursive: true }),
  mkdir(esmOutput, { recursive: true }),
  mkdir(commonJsOutput, { recursive: true }),
  mkdir(edgeOutput, { recursive: true }),
  mkdir(wasmOutput, { recursive: true }),
  mkdir(commonJsDeclarations, { recursive: true }),
]);

const browser = await build({
  entryPoints: [join(compiled, "index.js")],
  outfile: join(browserOutput, "index.js"),
  bundle: true,
  format: "esm",
  platform: "neutral",
  mainFields: ["module", "main"],
  target: "es2022",
  banner: { js: browserImportStatement },
  plugins: [generatedGlue, embeddedWasm],
  allowOverwrite: true,
  sourcemap: true,
  metafile: true,
});

const esm = await build({
  entryPoints: [join(compiled, "node.js")],
  outfile: join(esmOutput, "index.js"),
  bundle: true,
  format: "esm",
  platform: "node",
  target: "node22",
  plugins: [generatedGlue, nodeWasmFile("esm")],
  allowOverwrite: true,
  sourcemap: true,
  metafile: true,
});

const commonJs = await build({
  entryPoints: [join(compiled, "node.js")],
  outfile: join(commonJsOutput, "index.cjs"),
  bundle: true,
  format: "cjs",
  platform: "node",
  target: "node22",
  plugins: [generatedGlue, nodeWasmFile("cjs")],
  logOverride: { "empty-import-meta": "silent" },
  sourcemap: true,
  metafile: true,
});

const edge = await build({
  entryPoints: [join(compiled, "edge.js")],
  outfile: join(edgeOutput, "index.js"),
  bundle: true,
  format: "esm",
  platform: "neutral",
  mainFields: ["module", "main"],
  target: "es2022",
  plugins: [generatedGlue, precompiledWasm],
  sourcemap: true,
  metafile: true,
});

const edgeImports = outputImports(edge);
if (
  edgeImports.length !== 1 ||
  edgeImports[0]?.path !== edgeImport ||
  edgeImports[0]?.external !== true
) {
  throw new Error(
    `edge ESM must retain exactly one static WASM import: ${JSON.stringify(edgeImports)}`,
  );
}

const [browserSource, esmSource, commonJsSource, edgeSource, wasm] =
  await Promise.all([
  readFile(join(browserOutput, "index.js"), "utf8"),
  readFile(join(esmOutput, "index.js"), "utf8"),
  readFile(join(commonJsOutput, "index.cjs"), "utf8"),
  readFile(join(edgeOutput, "index.js"), "utf8"),
  readFile(wasmPath),
]);
if (!browserSource.startsWith(`${browserImportStatement}\n`)) {
  throw new Error(
    "browser ESM did not retain the standards-based WASM byte import",
  );
}
const forbiddenBrowserText = [
  "@janssenproject/cedarling_wasm",
  "node:fs",
  "node:module",
  "node:path",
  "node:url",
];
assertTextExcludes("browser ESM", browserSource, forbiddenBrowserText);
assertTextExcludes("root ESM", esmSource, [
  "@janssenproject/cedarling_wasm",
]);
assertTextExcludes("root CommonJS", commonJsSource, [
  "@janssenproject/cedarling_wasm",
  "import.meta",
  "import_meta",
]);
assertTextExcludes("edge ESM", edgeSource, [
  "WebAssembly.compile",
  "WebAssembly.instantiateStreaming",
  "new WebAssembly.Module",
]);
await Promise.all([
  assertSelfContainedSourceMap(
    "browser ESM",
    join(browserOutput, "index.js.map"),
  ),
  assertSelfContainedSourceMap("root ESM", join(esmOutput, "index.js.map")),
  assertSelfContainedSourceMap(
    "root CommonJS",
    join(commonJsOutput, "index.cjs.map"),
  ),
  assertSelfContainedSourceMap("edge ESM", join(edgeOutput, "index.js.map")),
]);

await copyFile(wasmPath, distributedWasmPath);
const copiedWasm = await readFile(distributedWasmPath);
if (!wasm.equals(copiedWasm)) {
  throw new Error("distributed WASM differs from the generated build input");
}

await copyDeclarations(declarations, commonJsDeclarations);
await copyDeclarations(declarations, compiled);
await writeFile(
  join(commonJsDeclarations, "package.json"),
  `${JSON.stringify({ type: "commonjs" })}\n`,
);
