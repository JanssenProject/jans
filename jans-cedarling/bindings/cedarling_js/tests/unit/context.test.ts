import type QUnitApi from "qunit";

import { createClientForEngine } from "../../dist/client/client.js";
import { createGeneratedEngine } from "../../dist/engine/generated.js";
import { createTestEngine } from "./engine-fixture.js";

/** Returns the generated operations required outside each focused test. */
function generatedClient(overrides: Readonly<Record<string, unknown>>): object {
  return {
    async authorize_unsigned() {
      throw new Error("authorization is outside this test");
    },
    async authorize_multi_issuer() {
      throw new Error("authorization is outside this test");
    },
    async shut_down() {},
    free() {},
    ...overrides,
  };
}

/** Registers context-data validation, conversion, and disposal unit tests. */
export default function registerContextUnitTests(QUnit: QUnitApi): void {
  QUnit.module("context");

  QUnit.test("validates and detaches writes before the engine", async (assert) => {
    let accepted: unknown;
    const engine = createTestEngine({
      async setContext(key, value, ttlSeconds) {
        accepted = { key, value, ttlSeconds };
      },
    });
    const client = createClientForEngine(engine, {
      contextMaxTtlSeconds: 10,
    });

    for (const [key, value, options] of [
      ["", true, undefined],
      ["fact", null, undefined],
      ["fact", Number.NaN, undefined],
      ["fact", true, { ttlSeconds: 0 }],
      ["fact", true, { ttlSeconds: 11 }],
      ["fact", true, { ttlSeconds: 1, extra: true }],
    ] as const) {
      const result = await client.context.set(
        key,
        value as never,
        options as never,
      );
      assert.false(result.ok);
      if (!result.ok) {
        assert.strictEqual(result.error.code, "INVALID_INPUT");
      }
    }

    const value = { nested: { enabled: true }, score: 2 };
    const pending = client.context.set("fact", value, { ttlSeconds: 5 });
    value.nested.enabled = false;
    value.score = 9;
    assert.true((await pending).ok);
    assert.deepEqual(accepted, {
      key: "fact",
      value: { nested: { enabled: true }, score: 2 },
      ttlSeconds: 5,
    });
    await client.shutDown();
  });

  QUnit.test("closed context operations do not inspect caller values", async (assert) => {
    const client = createClientForEngine(createTestEngine());
    await client.shutDown();
    let inspections = 0;
    const value = new Proxy({ enabled: true }, {
      getPrototypeOf(target) {
        inspections += 1;
        return Reflect.getPrototypeOf(target);
      },
    });

    const result = await client.context.set("fact", value);
    assert.false(result.ok);
    if (!result.ok) {
      assert.strictEqual(result.error.code, "CLIENT_CLOSED");
    }
    assert.strictEqual(inspections, 0);
  });

  QUnit.test("copies metadata and releases generated entry wrappers", async (assert) => {
    let disposals = 0;
    const rawValue = { nested: { enabled: true }, score: 2 };
    const engine = createGeneratedEngine(generatedClient({
      get_data_entry_ctx: () => ({
        key: "fact",
        data_type: "record",
        created_at: "2026-07-24T00:00:00Z",
        expires_at: "2026-07-24T00:01:00Z",
        access_count: 2n,
        value: () => rawValue,
        free() {
          disposals += 1;
        },
      }),
    }));
    assert.ok(engine, "the generated client is compatible");
    if (engine === undefined) {
      throw new Error("unreachable: assert.ok already failed");
    }

    const entry = await engine.getContextEntry("fact");
    rawValue.nested.enabled = false;
    assert.deepEqual(entry, {
      key: "fact",
      value: { nested: { enabled: true }, score: 2 },
      dataType: "record",
      createdAt: "2026-07-24T00:00:00Z",
      expiresAt: "2026-07-24T00:01:00Z",
      accessCount: 2,
    });
    assert.strictEqual(disposals, 1);
    await engine.shutDown();
  });

  QUnit.test("rejects unsafe generated counters and still releases wrappers", async (assert) => {
    let entryDisposals = 0;
    let statsDisposals = 0;
    const engine = createGeneratedEngine(generatedClient({
      get_data_entry_ctx: () => ({
        key: "fact",
        data_type: "bool",
        created_at: "2026-07-24T00:00:00Z",
        expires_at: undefined,
        access_count: BigInt(Number.MAX_SAFE_INTEGER) + 1n,
        value: () => true,
        free() {
          entryDisposals += 1;
        },
      }),
      get_stats_ctx: () => ({
        entry_count: Number.MAX_SAFE_INTEGER + 1,
        max_entries: 10,
        max_entry_size: 100,
        metrics_enabled: true,
        total_size_bytes: 1,
        avg_entry_size_bytes: 1,
        capacity_usage_percent: 10,
        memory_alert_threshold: 80,
        memory_alert_triggered: false,
        free() {
          statsDisposals += 1;
        },
      }),
    }));
    assert.ok(engine, "the generated client is compatible");
    if (engine === undefined) {
      throw new Error("unreachable: assert.ok already failed");
    }

    for (const operation of [
      () => engine.getContextEntry("fact"),
      () => engine.contextStats(),
    ]) {
      try {
        await operation();
        assert.pushResult({
          result: false,
          actual: "resolved",
          expected: "RESULT_CONVERSION_FAILED",
          message: "unsafe counters must reject",
        });
      } catch (error: unknown) {
        assert.strictEqual(
          (error as { code?: unknown }).code,
          "RESULT_CONVERSION_FAILED",
        );
      }
    }
    assert.strictEqual(entryDisposals, 1);
    assert.strictEqual(statsDisposals, 1);
    await engine.shutDown();
  });

  QUnit.test("releases every listed wrapper when one entry is malformed", async (assert) => {
    let disposals = 0;
    const entry = (key: string, accessCount: bigint) => ({
      key,
      data_type: "bool",
      created_at: "2026-07-24T00:00:00Z",
      expires_at: undefined,
      access_count: accessCount,
      value: () => true,
      free() {
        disposals += 1;
      },
    });
    const engine = createGeneratedEngine(generatedClient({
      list_data_ctx: () => [
        entry("valid", 1n),
        entry("unsafe", BigInt(Number.MAX_SAFE_INTEGER) + 1n),
      ],
    }));
    assert.ok(engine, "the generated client is compatible");
    if (engine === undefined) {
      throw new Error("unreachable: assert.ok already failed");
    }

    try {
      await engine.contextEntries();
      assert.pushResult({
        result: false,
        actual: "resolved",
        expected: "RESULT_CONVERSION_FAILED",
        message: "a malformed list entry must reject the observation",
      });
    } catch (error: unknown) {
      assert.strictEqual(
        (error as { code?: unknown }).code,
        "RESULT_CONVERSION_FAILED",
      );
    }
    assert.strictEqual(disposals, 2);
    await engine.shutDown();
  });
}
