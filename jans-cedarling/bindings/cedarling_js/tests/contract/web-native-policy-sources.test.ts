import type QUnitApi from "qunit";

import {
  createCedarling,
  type CedarlingOptions,
} from "@janssenproject/cedarling";

/** Registers public exclusions for non-Web-native policy sources. */
export default function registerWebNativePolicySourceTests(
  QUnit: QUnitApi,
): void {
  QUnit.module("web-native-policy-sources");

  QUnit.test("a filesystem source is rejected before WASM loading", async (assert) => {
    const options = {
      applicationName: "filesystem-source-is-deferred",
      policyStore: {
        type: "file",
        path: "/private/policy.cjar",
      },
    } as unknown as CedarlingOptions;

    const result = await createCedarling(options);

    assert.false(result.ok);
    if (!result.ok) {
      assert.strictEqual(result.error.code, "INVALID_INPUT");
      assert.deepEqual(result.error.issues?.[0]?.path, [
        "policyStore",
        "type",
      ]);
    }
  });

  QUnit.test("unknown policy-source types are rejected before WASM loading", async (assert) => {
    const unknownTypes: readonly string[] = [
      "gateway",
      "remote",
      "object",
      "s3",
      "https",
    ];

    for (const unknownType of unknownTypes) {
      const options = {
        applicationName: `unknown-source-${unknownType}`,
        policyStore: {
          type: unknownType,
          url: "https://policy.example/store",
        },
      } as unknown as CedarlingOptions;

      const result = await createCedarling(options);

      assert.false(result.ok, `type "${unknownType}" is rejected`);
      if (!result.ok) {
        assert.strictEqual(result.error.code, "INVALID_INPUT");
        assert.deepEqual(result.error.issues?.[0]?.path, [
          "policyStore",
          "type",
        ]);
      }
    }
  });
}
