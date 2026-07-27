import { createRequire } from "node:module";
import { pathToFileURL } from "node:url";

import type QUnitApi from "qunit";

// Producer-export contract lock for the generated `cedarling_wasm` package.
//
// These tests intentionally verify only that the wasm-bindgen producer
// package exposes the names the SDK adapters import at runtime. They do NOT
// exercise WASM behavior; that is the responsibility of the engine-init
// suites (`web-initialization`, `node-initialization`, `edge-initialization`,
// `workerd-initialization`) and the contract suites.
interface GeneratedPackageContract {
  readonly default?: unknown;
  readonly initSync?: unknown;
  readonly init?: unknown;
  readonly init_from_archive_bytes?: unknown;
}

const localRequire = createRequire(import.meta.url);

/** Registers the generated WASM package producer contract. */
export default function registerWasmProducerContractTests(
  QUnit: QUnitApi,
): void {
  QUnit.module("wasm-producer-contract");

  QUnit.test(
    "the installed generated package exposes the SDK initialization surface",
    async (assert) => {
      const gluePath = localRequire.resolve(
        "@janssenproject/cedarling_wasm",
      );
      const wasmPath = localRequire.resolve(
        "@janssenproject/cedarling_wasm/cedarling_wasm_bg.wasm",
      );
      const generated = (await import(
        pathToFileURL(gluePath).href
      )) as GeneratedPackageContract;

      assert.strictEqual(
        typeof generated.default,
        "function",
        "the asynchronous module initializer is exported",
      );
      assert.strictEqual(
        typeof generated.initSync,
        "function",
        "the synchronous module initializer is exported",
      );
      assert.strictEqual(
        typeof generated.init,
        "function",
        "ordinary Cedarling construction is exported",
      );
      assert.strictEqual(
        typeof generated.init_from_archive_bytes,
        "function",
        "archive-byte Cedarling construction is exported",
      );
      assert.true(
        wasmPath.endsWith("cedarling_wasm_bg.wasm"),
        "the dependency-owned WASM asset subpath resolves",
      );
    },
  );
}
