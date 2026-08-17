import type QUnitApi from "qunit";
import { assertCedarlingError } from "../run.js";

import { createClientForEngine } from "../../dist/client/client.js";
import {
  createGeneratedEngineFixture,
  createTestEngine,
} from "./engine-fixture.js";

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

    for (
      const [key, value, options, code, path] of [
        ["", true, undefined, "INPUT_REQUIRED", []],
        ["fact", null, undefined, "INPUT_INVALID_TYPE", []],
        ["fact", Number.NaN, undefined, "INPUT_INVALID_TYPE", []],
        ["fact", true, { ttlSeconds: 0 }, "INPUT_OUT_OF_RANGE", [
          "options",
          "ttlSeconds",
        ]],
        ["fact", true, { ttlSeconds: 11 }, "INPUT_OUT_OF_RANGE", [
          "options",
          "ttlSeconds",
        ]],
        ["fact", true, { ttlSeconds: 1, extra: true }, "INPUT_UNKNOWN_FIELD", [
          "options",
          "extra",
        ]],
      ] as const
    ) {
      const result = await client.context.set(
        key,
        value as never,
        options as never,
      );
      assertCedarlingError(assert, result, {
        code,
        operation: "context.set",
        path,
      });
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

  QUnit.test("normalizes generated context read and write failures", async (assert) => {
    const secret = "generated-context-secret"; // # gitleaks:allow
    const engine = createGeneratedEngineFixture({
      push_data_ctx() {
        throw new Error(secret);
      },
      get_data_ctx() {
        throw new Error(secret);
      },
    });

    for (
      const [operation, work] of [
        ["context.set", () => engine.setContext("fact", true)],
        ["context.get", () => engine.getContext("fact")],
      ] as const
    ) {
      try {
        await work();
        assert.pushResult({
          result: false,
          actual: "resolved",
          expected: "CONTEXT_OPERATION_FAILED",
          message: `${operation} must reject an opaque generated failure`,
        });
      } catch (error: unknown) {
        const normalized = error as {
          readonly code?: unknown;
          readonly operation?: unknown;
          readonly details?: unknown;
        };
        assert.strictEqual(
          normalized.code,
          "CONTEXT_OPERATION_FAILED",
          `${operation} uses the context error policy`,
        );
        assert.strictEqual(
          normalized.operation,
          operation,
          `${operation} retains its public operation`,
        );
        assert.strictEqual(normalized.details, undefined);
        assert.false("cause" in normalized);
        assert.false(
          JSON.stringify(error).includes(secret),
          `${operation} does not retain opaque generated details`,
        );
      }
    }

    await engine.shutDown();
  });

  QUnit.test("copies metadata and releases generated entry wrappers", async (assert) => {
    let disposals = 0;
    const rawValue = { nested: { enabled: true }, score: 2 };
    const engine = createGeneratedEngineFixture({
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
    });

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

  QUnit.test("validates generated metadata before reading its value", async (assert) => {
    let valueReads = 0;
    let disposals = 0;
    const engine = createGeneratedEngineFixture({
      get_data_entry_ctx: () => ({
        key: "",
        data_type: "bool",
        created_at: "2026-07-24T00:00:00Z",
        expires_at: undefined,
        access_count: 1n,
        value() {
          valueReads += 1;
          return true;
        },
        free() {
          disposals += 1;
        },
      }),
    });

    try {
      await engine.getContextEntry("fact");
      assert.true(false, "invalid generated metadata must reject");
    } catch (error) {
      assert.strictEqual((error as { code?: unknown }).code, "GENERATED_PROTOCOL_ERROR");
    }
    assert.strictEqual(valueReads, 0, "the generated value is not inspected");
    assert.strictEqual(disposals, 1, "the wrapper is still released");
    await engine.shutDown();
  });

  QUnit.test("rejects unsafe generated counters and still releases wrappers", async (assert) => {
    let entryDisposals = 0;
    let statsDisposals = 0;
    const engine = createGeneratedEngineFixture({
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
    });

    for (
      const operation of [
        () => engine.getContextEntry("fact"),
        () => engine.contextStats(),
      ]
    ) {
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
    const engine = createGeneratedEngineFixture({
      list_data_ctx: () => [
        entry("valid", 1n),
        entry("unsafe", BigInt(Number.MAX_SAFE_INTEGER) + 1n),
      ],
    });

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
