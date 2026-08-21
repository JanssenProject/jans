#!/usr/bin/env node

import { copyFile, mkdir, readFile } from "node:fs/promises";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

import { build } from "esbuild";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const compiled = join(root, "dist");
const wasmPath = resolve(
  root,
  "../cedarling_wasm/pkg/cedarling_wasm_bg.wasm",
);
const generatedGluePath = join(
  root,
  "../cedarling_wasm/pkg/cedarling_wasm.js",
);
const edgeWasmPath = join(root, "dist/edge/cedarling_wasm_bg.wasm");
const edgeImport = "./cedarling_wasm_bg.wasm?module";
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
      () => ({ path: wasmPath }),
    );
  },
};

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

function assertNoOutputImports(label, result) {
  const imports = outputImports(result);
  if (imports.length !== 0) {
    throw new Error(`${label} retained output imports: ${JSON.stringify(imports)}`);
  }
}

function assertTextExcludes(label, source, values) {
  for (const value of values) {
    if (source.includes(value)) {
      throw new Error(`${label} retained forbidden output text: ${value}`);
    }
  }
}

await mkdir(join(root, "dist/edge"), { recursive: true });

const esm = await build({
  entryPoints: [join(compiled, "index.js")],
  outfile: join(root, "dist/index.js"),
  bundle: true,
  format: "esm",
  platform: "neutral",
  mainFields: ["module", "main"],
  target: "es2022",
  loader: { ".wasm": "binary" },
  plugins: [generatedGlue, embeddedWasm],
  allowOverwrite: true,
  sourcemap: true,
  metafile: true,
});

const commonJs = await build({
  entryPoints: [join(root, "dist/index.js")],
  outfile: join(root, "dist/index.cjs"),
  bundle: true,
  format: "cjs",
  platform: "node",
  target: "node22",
  sourcemap: true,
  metafile: true,
});

const edge = await build({
  entryPoints: [join(compiled, "edge.js")],
  outfile: join(root, "dist/edge/index.js"),
  bundle: true,
  format: "esm",
  platform: "neutral",
  mainFields: ["module", "main"],
  target: "es2022",
  plugins: [generatedGlue, precompiledWasm],
  sourcemap: true,
  metafile: true,
});

assertNoOutputImports("root ESM", esm);
assertNoOutputImports("root CommonJS", commonJs);
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

const [esmSource, commonJsSource, edgeSource, wasm] = await Promise.all([
  readFile(join(root, "dist/index.js"), "utf8"),
  readFile(join(root, "dist/index.cjs"), "utf8"),
  readFile(join(root, "dist/edge/index.js"), "utf8"),
  readFile(wasmPath),
]);
const forbiddenRootText = [
  "@janssenproject/cedarling_wasm",
  "import.meta",
  "node:fs",
  "node:module",
  "node:path",
  "node:url",
];
assertTextExcludes("root ESM", esmSource, forbiddenRootText);
assertTextExcludes("root CommonJS", commonJsSource, forbiddenRootText);
assertTextExcludes("edge ESM", edgeSource, [
  "WebAssembly.compile",
  "WebAssembly.instantiateStreaming",
  "new WebAssembly.Module",
]);

await copyFile(wasmPath, edgeWasmPath);
const copiedWasm = await readFile(edgeWasmPath);
if (!wasm.equals(copiedWasm)) {
  throw new Error("edge WASM differs from the embedded build input");
}
