import type QUnitApi from "qunit";

import { createCedarling } from "@janssenproject/cedarling";

/** Policy store whose decision depends on one retained context-data fact. */
const contextPolicyStore = {
  cedar_version: "v4.0.0",
  policy_stores: {
    context: {
      cedar_version: "v4.0.0",
      name: "Context data",
      policies: {
        feature: {
          description: "allow when the retained feature flag is enabled",
          creation_date: "2026-07-24T00:00:00Z",
          policy_content: {
            encoding: "none",
            content_type: "cedar",
            body:
              'permit(principal, action == Context::Action::"Use", resource) when { context.data.feature_enabled };',
          },
        },
      },
      schema: {
        encoding: "none",
        content_type: "cedar",
        body: [
          "namespace Context {",
          "entity User;",
          "entity Resource;",
          'action "Use" appliesTo {',
          "  principal: [User],",
          "  resource: [Resource],",
          "  context: { data: { feature_enabled: Bool } }",
          "};",
          "}",
        ].join("\n"),
      },
    },
  },
} as const;

/** Registers real-WASM public context-data contracts. */
export default function registerContextContractTests(QUnit: QUnitApi): void {
  QUnit.module("context");

  QUnit.test("writes, reads, and authorizes with one retained context fact", async (assert) => {
    const created = await createCedarling({
      applicationName: "cedarling-js-context-fact",
      policyStore: { type: "inline", document: contextPolicyStore },
    });
    assert.true(created.ok);
    if (!created.ok) {
      return;
    }

    try {
      assert.deepEqual(
        await created.value.context.set("feature_enabled", true),
        { ok: true, value: undefined },
      );
      assert.deepEqual(
        await created.value.context.get("feature_enabled"),
        { ok: true, value: true },
      );

      const authorized = await created.value.authorizeUnsigned({
        principal: { type: "Context::User", id: "alice" },
        action: 'Context::Action::"Use"',
        resource: { type: "Context::Resource", id: "feature" },
      });
      assert.true(authorized.ok);
      if (authorized.ok) {
        assert.true(authorized.value.decision);
      }
    } finally {
      assert.true((await created.value.close()).ok);
    }
  });

  QUnit.test("round-trips detached metadata, replacement, deletion, clearing, and metrics", async (assert) => {
    const created = await createCedarling({
      applicationName: "cedarling-js-context-operations",
      contextStore: {
        maxEntries: 4,
        maxEntrySizeBytes: 1_024,
        metrics: true,
        memoryAlertThresholdPercent: 25,
      },
      policyStore: { type: "inline", document: contextPolicyStore },
    });
    assert.true(created.ok);
    if (!created.ok) {
      return;
    }

    try {
      const source = { nested: { enabled: true }, score: 2 };
      assert.true((await created.value.context.set("profile", source)).ok);
      source.nested.enabled = false;
      source.score = 9;

      assert.propEqual(await created.value.context.get("profile"), {
        ok: true,
        value: { nested: { enabled: true }, score: 2 },
      });
      const entry = await created.value.context.getEntry("profile");
      assert.true(entry.ok);
      if (entry.ok) {
        assert.strictEqual(entry.value?.key, "profile");
        assert.strictEqual(entry.value?.dataType, "record");
        // The test performs exactly one `get` and one `getEntry` on the
        // entry above; both register as accesses.
        assert.strictEqual(entry.value?.accessCount, 2);
        assert.strictEqual(typeof entry.value?.createdAt, "string");
      }

      assert.true(
        (await created.value.context.set("profile", { replaced: true }))
          .ok,
      );
      assert.propEqual(await created.value.context.get("profile"), {
        ok: true,
        value: { replaced: true },
      });

      const entries = await created.value.context.entries();
      const stats = await created.value.context.stats();
      assert.true(entries.ok);
      assert.true(stats.ok);
      if (entries.ok && stats.ok) {
        assert.strictEqual(entries.value.length, 1);
        assert.strictEqual(entries.value[0]?.key, "profile");
        assert.strictEqual(stats.value.entryCount, 1);
        assert.strictEqual(stats.value.maxEntries, 4);
        assert.strictEqual(stats.value.maxEntrySizeBytes, 1_024);
        assert.true(stats.value.metricsEnabled);
        assert.true(stats.value.totalSizeBytes > 0);
        assert.strictEqual(
          stats.value.memoryAlertThresholdPercent,
          25,
        );
      }

      assert.deepEqual(await created.value.context.delete("missing"), {
        ok: true,
        value: false,
      });
      assert.deepEqual(await created.value.context.delete("profile"), {
        ok: true,
        value: true,
      });
      assert.true((await created.value.context.set("one", 1)).ok);
      assert.true((await created.value.context.set("two", 2)).ok);
      assert.deepEqual(await created.value.context.clear(), {
        ok: true,
        value: undefined,
      });
      assert.deepEqual(await created.value.context.entries(), {
        ok: true,
        value: [],
      });
    } finally {
      assert.true((await created.value.close()).ok);
    }
  });

  QUnit.test("omits expired entries and evicts them for new writes", async (assert) => {
    assert.timeout(5_000);
    const created = await createCedarling({
      applicationName: "cedarling-js-context-expiry",
      contextStore: { maxEntries: 1, maxTtlSeconds: 60 },
      policyStore: { type: "inline", document: contextPolicyStore },
    });
    assert.true(created.ok);
    if (!created.ok) {
      return;
    }

    try {
      assert.true(
        (await created.value.context.set("expired", true, {
          ttlSeconds: 1,
        })).ok,
      );
      // Wait 2s (2× the configured TTL) so the 1s TTL elapses comfortably
      // even on slow CI machines with timer drift.
      await new Promise<void>((resolve) => {
        setTimeout(resolve, 2_000);
      });
      assert.deepEqual(await created.value.context.get("expired"), {
        ok: true,
        value: undefined,
      });
      assert.deepEqual(
        await created.value.context.getEntry("expired"),
        { ok: true, value: undefined },
      );
      assert.deepEqual(await created.value.context.entries(), {
        ok: true,
        value: [],
      });
      assert.true(
        (await created.value.context.set("replacement", true)).ok,
        "an expired entry does not consume active capacity",
      );
      const full = await created.value.context.set("overflow", true);
      assert.false(full.ok);
      if (!full.ok) {
        assert.strictEqual(full.error.code, "CONTEXT_OPERATION_FAILED");
      }
    } finally {
      assert.true((await created.value.close()).ok);
    }
  });

  QUnit.test("enforces configured entry-size limits without partial writes", async (assert) => {
    const created = await createCedarling({
      applicationName: "cedarling-js-context-size",
      contextStore: { maxEntrySizeBytes: 8 },
      policyStore: { type: "inline", document: contextPolicyStore },
    });
    assert.true(created.ok);
    if (!created.ok) {
      return;
    }

    try {
      const oversized = await created.value.context.set(
        "oversized",
        "this value is too large",
      );
      assert.false(oversized.ok);
      if (!oversized.ok) {
        assert.strictEqual(
          oversized.error.code,
          "CONTEXT_OPERATION_FAILED",
        );
      }
      assert.deepEqual(await created.value.context.get("oversized"), {
        ok: true,
        value: undefined,
      });
    } finally {
      assert.true((await created.value.close()).ok);
    }
  });

  QUnit.test("rejects context data that cannot be injected into Cedar", async (assert) => {
    const created = await createCedarling({
      applicationName: "cedarling-js-context-injection",
      authorization: { dangerouslyDisableSchemaValidation: true },
      policyStore: { type: "inline", document: contextPolicyStore },
    });
    assert.true(created.ok);
    if (!created.ok) {
      return;
    }

    try {
      assert.true(
        (await created.value.context.set("feature_enabled", true)).ok,
      );
      const conflict = await created.value.authorizeUnsigned({
        principal: { type: "Context::User", id: "alice" },
        action: 'Context::Action::"Use"',
        resource: { type: "Context::Resource", id: "feature" },
        context: { data: { feature_enabled: false } },
      });
      assert.false(conflict.ok, "request and retained keys never overwrite");

      const invalid = await created.value.context.set("invalid", {
        nested: null,
        score: 1.5,
      } as never);
      assert.false(invalid.ok);
      if (!invalid.ok) {
        assert.strictEqual(invalid.error.code, "INVALID_INPUT");
        assert.deepEqual(invalid.error.issues?.[0]?.path, []);
      }

      const authorized = await created.value.authorizeUnsigned({
        principal: { type: "Context::User", id: "alice" },
        action: 'Context::Action::"Use"',
        resource: { type: "Context::Resource", id: "feature" },
      });
      assert.true(authorized.ok);
      if (authorized.ok) {
        assert.true(authorized.value.decision);
      }
      assert.deepEqual(await created.value.context.get("invalid"), {
        ok: true,
        value: undefined,
      });
    } finally {
      assert.true((await created.value.close()).ok);
    }
  });
}
