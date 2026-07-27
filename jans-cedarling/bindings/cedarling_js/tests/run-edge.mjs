/**
 * Vercel Edge test harness.
 *
 * Precompiles the packaged WASM artifact in the host (mirroring the Vercel
 * build pipeline's `.wasm?module` precompile step), injects it into the
 * edge-runtime VM, and evaluates the bundled runner. The runner exposes its
 * completion promise as `globalThis.__cedarlingTestDone`.
 *
 * The bundle is built as ESM, but the VM uses `runInContext` (script mode)
 * which rejects `import.meta` syntax. Before evaluation, dead references to
 * `import.meta.url` inside the wasm-bindgen glue are replaced with
 * `self.location.href` so they parse correctly (they are never reached at
 * runtime because the `.wasm?module` import resolves to a precompiled module).
 */
import { readFile } from "node:fs/promises";
import { EdgeRuntime } from "edge-runtime";

const [wasmBytes, bundleRaw] = await Promise.all([
  readFile(
    "node_modules/@janssenproject/cedarling_wasm/cedarling_wasm_bg.wasm",
  ),
  readFile(".test-dist/.build/run-edge.js", "utf-8"),
]);

const precompiled = await WebAssembly.compile(
  new Uint8Array(wasmBytes),
);

const runtime = new EdgeRuntime({
  extend: (context) => {
    context.__CEDARLING_PRECOMPILED_WASM__ = precompiled;
    return context;
  },
});

/*
 * The EdgeVM constructor does not evaluate the `code` option; only
 * `initialCode` is evaluated. Evaluate the bundle explicitly so that the
 * runner's top-level `runTestSuites(...)` call executes inside the VM.
 */
runtime.evaluate(
  bundleRaw.replace(/import\.meta\.url/g, '"/__edge_entry.js"'),
);

const stats = await runtime.evaluate("globalThis.__cedarlingTestDone");

if (stats === undefined || stats.failed !== 0) {
  console.error("Vercel Edge tests failed:", stats);
  process.exit(1);
}

console.log(
  `Vercel Edge tests passed: ${stats.passed}/${stats.total} assertions`,
);
