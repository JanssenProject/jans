import type QUnitApi from "qunit";

import { createCedarling } from "@janssenproject/cedarling";
import type { RuntimeFixtures } from "../run.js";

/** Registers public application-loader policy contracts. */
export default function registerPolicyLoaderTests(
  QUnit: QUnitApi,
  fixtures: RuntimeFixtures,
): void {
  QUnit.module("policy-loader");

  QUnit.test("one application loader call initializes the archive path", async (assert) => {
    const bytes = await fixtures.loadTracerArchive();
    let calls = 0;
    const created = await createCedarling({
      applicationName: "loader-policy",
      policyStore: {
        type: "loader",
        async load() {
          calls += 1;
          return bytes;
        },
      },
    });

    assert.strictEqual(calls, 1);
    assert.true(created.ok);
    if (!created.ok) {
      return;
    }
    try {
      const authorized = await created.value.authorizeUnsigned({
        principal: { type: "Tracer::User", id: "alice" },
        action: 'Tracer::Action::"Read"',
        resource: { type: "Tracer::Resource", id: "document" },
      });
      assert.true(authorized.ok);
      if (authorized.ok) {
        assert.true(authorized.value.decision);
      }
    } finally {
      const closed = await created.value.shutDown();
      assert.true(closed.ok);
    }
  });

  QUnit.test("validation and capability failures do not invoke the loader", async (assert) => {
    let calls = 0;
    const invalid = await createCedarling({
      applicationName: "",
      policyStore: {
        type: "loader",
        async load() {
          calls += 1;
          return new Uint8Array([1]);
        },
      },
    });

    assert.false(invalid.ok);
    assert.strictEqual(calls, 0, "validation stops before loader invocation");

    await fixtures.withMissingWebAssembly(async (sdk) => {
      const unsupported = await sdk.createCedarling({
        applicationName: "missing-wasm-loader",
        policyStore: {
          type: "loader",
          async load() {
            calls += 1;
            return new Uint8Array([1]);
          },
        },
      });
      assert.false(unsupported.ok);
      if (!unsupported.ok) {
        assert.strictEqual(
          unsupported.error.code,
          "UNSUPPORTED_RUNTIME_CAPABILITY",
        );
      }
    });
    assert.strictEqual(calls, 0, "capability checks stop before the loader");
  });

  QUnit.test("loader invocation and return failures are safe and stage-specific", async (assert) => {
    const secret = "private-loader-credential"; // # gitleaks:allow
    const cases = [
      async () =>
        await createCedarling({
          applicationName: "loader-rejection",
          policyStore: {
            type: "loader",
            async load(): Promise<Uint8Array> {
              throw new Error(secret);
            },
          },
        }),
      async () =>
        await createCedarling({
          applicationName: "loader-invalid-return",
          policyStore: {
            type: "loader",
            async load() {
              return "not bytes" as never;
            },
          },
        }),
      async () =>
        await createCedarling({
          applicationName: "loader-empty-return",
          policyStore: {
            type: "loader",
            async load() {
              return new Uint8Array();
            },
          },
        }),
    ];

    for (const run of cases) {
      const result = await run();
      assert.false(result.ok);
      if (!result.ok) {
        assert.strictEqual(result.error.code, "POLICY_LOADER_FAILED");
        assert.deepEqual(result.error.details, { sourceType: "loader" });
        assert.false(JSON.stringify(result.error).includes(secret));
      }
    }
  });
}
