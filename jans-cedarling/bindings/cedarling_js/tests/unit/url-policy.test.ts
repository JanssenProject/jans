import type QUnitApi from "qunit";

import { prepareCedarlingOptions } from "../../dist/configuration/prepare.js";

/** Registers focused URL policy preparation tests. */
export default function registerUrlPolicyTests(QUnit: QUnitApi): void {
  QUnit.module("url-policy");

  QUnit.test("URL ownership stays with the generated Cedarling bootstrap", (assert) => {
    const prepared = prepareCedarlingOptions({
      applicationName: "url-policy-unit",
      policyStore: {
        type: "url",
        url: new URL("https://policy.example/store?version=1#current"),
        refresh: { intervalSeconds: 30 },
      },
    });

    assert.deepEqual(prepared.policyStore, {
      type: "url",
      url: "https://policy.example/store?version=1#current",
    });
    assert.strictEqual(
      prepared.bootstrapConfig.CEDARLING_POLICY_STORE_URI,
      "https://policy.example/store?version=1#current",
    );
    assert.strictEqual(
      prepared.bootstrapConfig.CEDARLING_POLICY_STORE_REFRESH_INTERVAL,
      30,
    );
    assert.false(
      Object.hasOwn(prepared.bootstrapConfig, "CEDARLING_POLICY_STORE_LOCAL"),
      "the SDK does not fetch or rewrite URL policy material",
    );
  });
}
