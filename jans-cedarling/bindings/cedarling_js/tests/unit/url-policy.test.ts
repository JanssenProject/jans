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

  QUnit.test("accepts HTTPS and exact loopback HTTP endpoints", (assert) => {
    for (const [input, expected] of [
      ["https://policy.example/store", "https://policy.example/store"],
      ["http://localhost/store", "http://localhost/store"],
      ["http://127.0.0.1/store", "http://127.0.0.1/store"],
      ["http://127.255.255.254/store", "http://127.255.255.254/store"],
      ["http://[::1]/store", "http://[::1]/store"],
    ]) {
      const prepared = prepareCedarlingOptions({
        applicationName: "safe-policy-url",
        policyStore: { type: "url", url: input },
      });
      assert.strictEqual(
        prepared.bootstrapConfig.CEDARLING_POLICY_STORE_URI,
        expected,
        input,
      );
    }
  });

  QUnit.test("rejects remote HTTP and credential-bearing endpoints", (assert) => {
    for (const url of [
      "http://policy.example/store",
      "http://localhost.example/store",
      "https://user:password@policy.example/store",
    ]) {
      assert.throws(
        () =>
          prepareCedarlingOptions({
            applicationName: "unsafe-policy-url",
            policyStore: { type: "url", url },
          }),
        (error: unknown) =>
          (
            error as {
              issues?: readonly [{ readonly code?: unknown }];
            }
          ).issues?.[0]?.code === "format",
        url,
      );
    }
  });

  QUnit.test("applies the same transport policy to Lock configuration", (assert) => {
    const prepared = prepareCedarlingOptions({
      applicationName: "loopback-lock-url",
      policyStore: { type: "inline", document: {} },
      lock: { configurationUrl: "http://[::1]/lock" },
    });

    assert.strictEqual(
      prepared.bootstrapConfig.CEDARLING_LOCK_SERVER_CONFIGURATION_URI,
      "http://[::1]/lock",
    );
    assert.throws(
      () =>
        prepareCedarlingOptions({
          applicationName: "remote-lock-url",
          policyStore: { type: "inline", document: {} },
          lock: { configurationUrl: "http://lock.example/config" },
        }),
      (error: unknown) =>
        (
          error as {
            issues?: readonly [{ readonly code?: unknown }];
          }
        ).issues?.[0]?.code === "format",
    );
  });
}
