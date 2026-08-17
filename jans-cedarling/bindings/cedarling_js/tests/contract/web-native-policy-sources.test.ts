import type QUnitApi from "qunit";

import {
  type CedarlingOptions,
  createCedarling,
} from "@janssenproject/cedarling";

export default function registerWebNativePolicySourceTests(
  QUnit: QUnitApi,
): void {
  QUnit.module("web-native-policy-sources");

  QUnit.test("unsupported policy-source types are rejected before WASM loading", async (assert) => {
    const unknownTypes: readonly string[] = [
      "file",
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
        assert.strictEqual(result.error.code, "INPUT_UNSUPPORTED");
        assert.deepEqual(result.error.path, [
          "policyStore",
          "type",
        ]);
      }
    }
  });
}
