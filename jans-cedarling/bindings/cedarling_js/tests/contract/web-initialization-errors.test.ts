import type QUnitApi from "qunit";

import type { RuntimeFixtures } from "../run.js";
import { tracerPolicyStore } from "../fixtures/tracer-policy-store.js";

/** Registers public generated Web-module initialization error contracts. */
export default function registerWebInitializationErrorTests(
  QUnit: QUnitApi,
  fixtures: RuntimeFixtures,
): void {
  QUnit.module("web-initialization-errors");

  QUnit.test("a missing WASM asset is safe and a later call can retry", async (assert) => {
    assert.strictEqual(
      fixtures.runtime,
      "node",
      "this integration run uses the thin Node fixture",
    );

    await fixtures.withMissingWasmAsset(async ({ sdk, restoreWasmAsset }) => {
      const first = await sdk.createCedarling({
        applicationName: "missing-wasm-asset",
        policyStore: {
          type: "inline",
          document: tracerPolicyStore,
        },
      });

      assert.false(first.ok, "the missing dependency asset is reported");
      if (!first.ok) {
        assert.strictEqual(first.error.code, "WASM_LOAD_FAILED");
        assert.strictEqual(first.error.operation, "initialize");
        assert.false(
          JSON.stringify(first.error).includes("cedarling_wasm_bg.wasm"),
          "the dependency asset path is not exposed",
        );
      }

      await restoreWasmAsset();
      const retried = await sdk.createCedarling({
        applicationName: "restored-wasm-asset",
        policyStore: {
          type: "inline",
          document: tracerPolicyStore,
        },
      });

      assert.true(retried.ok, "a failed module attempt is not cached");
      if (retried.ok) {
        const closed = await retried.value.shutDown();
        assert.true(closed.ok);
      }
    });
  });

  QUnit.test("a missing WebAssembly capability is reported publicly", async (assert) => {
    await fixtures.withMissingWebAssembly(async (sdk) => {
      const result = await sdk.createCedarling({
        applicationName: "missing-webassembly-capability",
        policyStore: {
          type: "inline",
          document: tracerPolicyStore,
        },
      });

      assert.false(result.ok);
      if (!result.ok) {
        assert.strictEqual(
          result.error.code,
          "UNSUPPORTED_RUNTIME_CAPABILITY",
        );
        assert.strictEqual(result.error.operation, "initialize");
        assert.deepEqual(result.error.details, {
          runtimeCapability: "webAssembly",
        });
      }
    });
  });
}
