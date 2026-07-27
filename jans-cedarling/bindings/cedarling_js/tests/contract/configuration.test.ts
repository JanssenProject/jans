import type QUnitApi from "qunit";

import type { RuntimeFixtures } from "../run.js";

/** Registers public configuration-validation contracts. */
export default function registerConfigurationTests(
  QUnit: QUnitApi,
  _fixtures: RuntimeFixtures,
): void {
  QUnit.module("configuration");

  QUnit.test("unknown top-level options are rejected", async (assert) => {
    const { createCedarling } = await import("@janssenproject/cedarling");
    const result = await createCedarling({
      applicationName: "configuration-contract",
      policyStore: {
        type: "inline",
        document: {},
      },
      misspelledLogging: {},
    } as never);

    assert.false(result.ok);
    if (!result.ok) {
      assert.strictEqual(result.error.code, "INVALID_INPUT");
      assert.strictEqual(result.error.operation, "initialize");
      assert.deepEqual(result.error.issues, [
        {
          path: ["misspelledLogging"],
          code: "unknownField",
          message: "The field is not supported.",
        },
      ]);
    }
  });

  QUnit.test("required values and nested unknown fields have stable paths", async (assert) => {
    const { createCedarling } = await import("@janssenproject/cedarling");
    const blankName = await createCedarling({
      applicationName: "   ",
      policyStore: { type: "inline", document: {} },
    });
    const nestedUnknown = await createCedarling({
      applicationName: "nested-unknown",
      policyStore: { type: "inline", document: {} },
      logging: { type: "memory", retainedItems: 10 },
    } as never);

    assert.false(blankName.ok);
    if (!blankName.ok) {
      assert.deepEqual(blankName.error.issues?.[0], {
        path: ["applicationName"],
        code: "required",
        message: "A required value is missing.",
      });
    }
    assert.false(nestedUnknown.ok);
    if (!nestedUnknown.ok) {
      assert.deepEqual(nestedUnknown.error.issues?.[0], {
        path: ["logging", "retainedItems"],
        code: "unknownField",
        message: "The field is not supported.",
      });
    }
  });

  QUnit.test("refresh belongs only to URL policy sources", async (assert) => {
    const { createCedarling } = await import("@janssenproject/cedarling");
    const result = await createCedarling({
      applicationName: "invalid-refresh-source",
      policyStore: {
        type: "inline",
        document: {},
        refresh: { intervalSeconds: 30 },
      },
    } as never);

    assert.false(result.ok);
    if (!result.ok) {
      assert.deepEqual(result.error.issues?.[0], {
        path: ["policyStore", "refresh"],
        code: "unknownField",
        message: "The field is not supported.",
      });
    }
  });

  QUnit.test("documented numeric boundaries reject unsafe values", async (assert) => {
    const { createCedarling } = await import("@janssenproject/cedarling");
    const cases = [
      {
        path: ["policyStore", "refresh", "intervalSeconds"],
        options: {
          applicationName: "refresh-floor",
          policyStore: {
            type: "url",
            url: "https://policy.example/store",
            refresh: { intervalSeconds: 4 },
          },
        },
      },
      {
        path: ["logging", "ttlSeconds"],
        options: {
          applicationName: "logging-ceiling",
          policyStore: { type: "inline", document: {} },
          logging: { type: "memory", ttlSeconds: 3_601 },
        },
      },
      {
        path: ["logging", "ttlSeconds"],
        options: {
          applicationName: "logging-zero-ttl",
          policyStore: { type: "inline", document: {} },
          logging: { type: "memory", ttlSeconds: 0 },
        },
      },
      {
        path: ["contextStore", "defaultTtlSeconds"],
        options: {
          applicationName: "context-zero-default-ttl",
          policyStore: { type: "inline", document: {} },
          contextStore: { defaultTtlSeconds: 0 },
        },
      },
      {
        path: ["contextStore", "maxTtlSeconds"],
        options: {
          applicationName: "context-zero-max-ttl",
          policyStore: { type: "inline", document: {} },
          contextStore: { maxTtlSeconds: 0 },
        },
      },
      {
        path: ["jwt", "jwksRefreshMinIntervalSeconds"],
        options: {
          applicationName: "jwks-zero-minimum",
          policyStore: { type: "inline", document: {} },
          jwt: { jwksRefreshMinIntervalSeconds: 0 },
        },
      },
      {
        path: ["jwt", "statusListRefreshMaxSeconds"],
        options: {
          applicationName: "status-zero-maximum",
          policyStore: { type: "inline", document: {} },
          jwt: { statusListRefreshMaxSeconds: 0 },
        },
      },
      {
        path: ["issuerLoading", "workers"],
        options: {
          applicationName: "worker-ceiling",
          policyStore: { type: "inline", document: {} },
          issuerLoading: { workers: 7 },
        },
      },
      {
        path: ["http", "maxRetries"],
        options: {
          applicationName: "retry-ceiling",
          policyStore: { type: "inline", document: {} },
          http: { maxRetries: 32 },
        },
      },
    ] as const;

    for (const testCase of cases) {
      const result = await createCedarling(testCase.options as never);
      assert.false(result.ok);
      if (!result.ok) {
        assert.deepEqual(result.error.issues?.[0]?.path, testCase.path);
        assert.strictEqual(result.error.issues?.[0]?.code, "range");
      }
    }
  });

  QUnit.test("credential-bearing URLs are rejected without disclosure", async (assert) => {
    const { createCedarling } = await import("@janssenproject/cedarling");
    const secret = "not-for-errors"; // # gitleaks:allow
    const result = await createCedarling({
      applicationName: "url-credentials",
      policyStore: {
        type: "url",
        url: `https://user:${secret}@policy.example/store.cjar?token=${secret}`,
      },
    });

    assert.false(result.ok);
    if (!result.ok) {
      assert.deepEqual(result.error.issues?.[0], {
        path: ["policyStore", "url"],
        code: "format",
        message: "The value has an invalid format.",
      });
      assert.false(JSON.stringify(result.error).includes(secret));
    }
  });

  QUnit.test("algorithm accessors are rejected without invocation", async (assert) => {
    const { createCedarling } = await import("@janssenproject/cedarling");
    let reads = 0;
    const allowedAlgorithms: string[] = [];
    Object.defineProperty(allowedAlgorithms, "0", {
      enumerable: true,
      get() {
        reads += 1;
        return "ES256";
      },
    });
    allowedAlgorithms.length = 1;

    const result = await createCedarling({
      applicationName: "algorithm-accessor",
      policyStore: { type: "inline", document: {} },
      jwt: { allowedAlgorithms },
    } as never);

    assert.false(result.ok);
    if (!result.ok) {
      assert.strictEqual(result.error.code, "INVALID_INPUT");
      assert.deepEqual(result.error.issues?.[0]?.path, [
        "jwt",
        "allowedAlgorithms",
      ]);
    }
    assert.strictEqual(reads, 0, "the accessor is never invoked");
  });

  QUnit.test("validation completes before runtime capability checks", async (assert) => {
    await _fixtures.withMissingWebAssembly(async (sdk) => {
      const result = await sdk.createCedarling({
        applicationName: "",
        policyStore: { type: "inline", document: {} },
      });

      assert.false(result.ok);
      if (!result.ok) {
        assert.strictEqual(result.error.code, "INVALID_INPUT");
        assert.deepEqual(result.error.issues?.[0]?.path, ["applicationName"]);
      }
    });
  });
}
