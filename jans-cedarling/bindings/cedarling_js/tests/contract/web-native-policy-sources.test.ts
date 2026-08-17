import type QUnitApi from "qunit";

import {
  type CedarlingOptions,
  createCedarling,
} from "@janssenproject/cedarling";
import { assertCedarlingError } from "../run.js";

export default function registerWebNativePolicySourceTests(
  QUnit: QUnitApi,
): void {
  QUnit.module("web-native-policy-sources");

  QUnit.test("rejects unsupported policy-source types", async (assert) => {
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

      assertCedarlingError(assert, result, {
        code: "INPUT_UNSUPPORTED",
        operation: "initialize",
        path: ["policyStore", "type"],
      });
    }
  });
}
