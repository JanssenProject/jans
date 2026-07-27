/**
 * Bundles a sandboxed test runner (workerd or vercel-edge) with the esbuild
 * JS API.
 *
 * Usage: node tests/build-sandbox.mjs <workerd|edge>
 *
 * - The `workerd` build keeps the SDK's static `.wasm` import external; the
 *   workerd capnp config provides it as a precompiled module, mirroring
 *   wrangler's CompiledWasm module rule.
 * - The `edge` build emulates Vercel's `.wasm?module` behavior: the imported
 *   source becomes a `WebAssembly.Module` precompiled at bundle evaluation
 *   inside the simulator (the real Vercel pipeline precompiles at build
 *   time). This proves the SDK edge entry passes the imported source
 *   unchanged to the generated asynchronous initializer.
 */
import { readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { build } from "esbuild";

const runtime = process.argv[2];
if (runtime !== "workerd" && runtime !== "edge") {
  console.error("usage: node tests/build-sandbox.mjs <workerd|edge>");
  process.exit(1);
}

const packageRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  "..",
);

/**
 * Maps the edge entry's `.wasm?module` import to the precompiled module
 * injected by the harness host (`tests/run-edge.mjs`). This mirrors
 * the Vercel build pipeline, which precompiles the asset before the isolate
 * evaluates application code.
 */
const wasmModuleQueryPlugin = {
  name: "wasm-module-query",
  setup(buildApi) {
    buildApi.onResolve({ filter: /\.wasm\?module$/ }, (args) => ({
      path: args.path,
      namespace: "wasm-module-query",
    }));
    buildApi.onLoad(
      { filter: /.*/, namespace: "wasm-module-query" },
      () => ({
        contents:
          "export default globalThis.__CEDARLING_PRECOMPILED_WASM__;",
      }),
    );
  },
};

/**
 * Maps the compiled runner's fixture import onto the source-tree asset
 * (tsc does not copy non-JavaScript assets into `.test-dist`).
 */
const fixtureArchivePlugin = {
  name: "fixture-archive",
  setup(buildApi) {
    buildApi.onResolve({ filter: /tracer-policy-store\.cjar$/ }, () => ({
      path: path.join(
        packageRoot,
        "tests",
        "fixtures",
        "tracer-policy-store.cjar",
      ),
      namespace: "fixture-archive",
    }));
    buildApi.onLoad(
      { filter: /.*/, namespace: "fixture-archive" },
      async (args) => {
        const bytes = await readFile(args.path);
        return {
          contents:
            `export default new Uint8Array([${bytes.join(",")}]);`,
          loader: "js",
        };
      },
    );
  },
};

const config = {
  logLevel: "warning",
  format: "esm",
  bundle: true,
  target: "esnext",
  absWorkingDir: packageRoot,
  loader: { ".cjar": "binary", ".wasm": "binary" },
  external: ["node:*"],
  ...(runtime === "workerd"
      ? {
        entryPoints: [".test-dist/runners/workerd.js"],
        conditions: ["workerd"],
        external: [
          "node:*",
          "@janssenproject/cedarling_wasm/cedarling_wasm_bg.wasm",
        ],
        plugins: [fixtureArchivePlugin],
        outfile: ".test-dist/.build/run-workerd.js",
      }
    : {
        entryPoints: [".test-dist/runners/edge.js"],
        conditions: ["edge-light"],
        plugins: [wasmModuleQueryPlugin, fixtureArchivePlugin],
        outfile: ".test-dist/.build/run-edge.js",
      }),
};

await build(config);
