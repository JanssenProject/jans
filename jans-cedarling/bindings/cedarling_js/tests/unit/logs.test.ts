import type QUnitApi from "qunit";

import { createClientForEngine } from "../../dist/client/client.js";
import type { CedarlingEngine } from "../../dist/engine/engine.js";
import { normalizeGeneratedLog } from "../../dist/logs/normalize.js";
import { createTestEngine } from "./engine-fixture.js";

/** Creates a no-op engine whose log calls are observable. */
function recordingEngine(calls: string[]): CedarlingEngine {
  return createTestEngine({
    async logIds() {
      calls.push("ids");
      return [];
    },
    async findLogs() {
      calls.push("find");
      return [];
    },
    async drainLogs() {
      calls.push("drain");
      return [];
    },
  });
}

/** Registers retained-log validation, lifecycle, and conversion unit tests. */
export default function registerLogsUnitTests(QUnit: QUnitApi): void {
  QUnit.module("logs");

  QUnit.test("rejects unsupported query combinations before the engine", async (assert) => {
    const calls: string[] = [];
    const client = createClientForEngine(recordingEngine(calls), {
      memoryLogging: true,
    });

    for (const query of [
      {},
      { id: "log", requestId: "request" },
      { tag: "future" },
      { requestId: "" },
      { requestId: "request", extra: true },
    ]) {
      const result = await client.logs.find(query as never);
      assert.false(result.ok);
      if (!result.ok) {
        assert.strictEqual(result.error.code, "INVALID_INPUT");
        assert.strictEqual(result.error.operation, "logs.find");
      }
    }

    assert.deepEqual(calls, []);
    await client.close();
  });

  QUnit.test("closed log operations reject before inspecting input", async (assert) => {
    const client = createClientForEngine(recordingEngine([]), {
      memoryLogging: true,
    });
    await client.close();
    let inspections = 0;
    const query = new Proxy({ id: "log" }, {
      getPrototypeOf(target) {
        inspections += 1;
        return Reflect.getPrototypeOf(target);
      },
    });

    const result = await client.logs.find(query);
    assert.false(result.ok);
    if (!result.ok) {
      assert.strictEqual(result.error.code, "CLIENT_CLOSED");
      assert.strictEqual(result.error.operation, "logs.find");
    }
    assert.strictEqual(inspections, 0);
  });

  QUnit.test("closed logs.ids and logs.drain return CLIENT_CLOSED", async (assert) => {
    const client = createClientForEngine(recordingEngine([]), {
      memoryLogging: true,
    });
    await client.close();

    const ids = await client.logs.ids();
    assert.false(ids.ok);
    if (!ids.ok) {
      assert.strictEqual(ids.error.code, "CLIENT_CLOSED");
      assert.strictEqual(ids.error.operation, "logs.ids");
    }

    const drain = await client.logs.drain();
    assert.false(drain.ok);
    if (!drain.ok) {
      assert.strictEqual(drain.error.code, "CLIENT_CLOSED");
      assert.strictEqual(drain.error.operation, "logs.drain");
    }
  });

  QUnit.test("normalizes known heterogeneous and Lock envelopes", (assert) => {
    const decision = normalizeGeneratedLog({
      id: "decision-id",
      request_id: "request-id",
      timestamp: "2026-07-24T00:00:00Z",
      log_kind: "Decision",
      pdp_id: "pdp",
      application_id: "application",
      decision: "ALLOW",
      nested_claims: { custom_claim: null },
    }, "logs.find");
    assert.deepEqual(decision, {
      id: "decision-id",
      requestId: "request-id",
      timestamp: "2026-07-24T00:00:00Z",
      kind: "decision",
      pdpId: "pdp",
      applicationId: "application",
      payload: {
        decision: "ALLOW",
        nested_claims: { custom_claim: null },
      },
    });

    const lock = normalizeGeneratedLog({
      id: "lock-id",
      level: "WARN",
      message: "Lock delivery is delayed",
      pdp_id: "pdp",
      application_id: "application",
    }, "logs.find");
    assert.strictEqual(lock.kind, "system");
    assert.strictEqual(lock.level, "warn");
    assert.deepEqual(lock.payload, {
      message: "Lock delivery is delayed",
    });

    for (const [name, raw, expectedKind] of [
      [
        "metric",
        {
          id: "metric-id",
          log_kind: "Metric",
          pdp_id: "pdp",
          metric_type: "token_cache",
          value: 1.5,
        },
        "metric",
      ],
      [
        "JWT system",
        {
          id: "jwt-id",
          log_kind: "System",
          level: "ERROR",
          pdp_id: "pdp",
          msg: "JWT validation failed",
          error_msg: "signature rejected",
        },
        "system",
      ],
      [
        "policy-store system",
        {
          id: "policy-id",
          log_kind: "System",
          level: "INFO",
          pdp_id: "pdp",
          msg: "Policy store loaded",
          policy_count: 5,
        },
        "system",
      ],
    ] as const) {
      const normalized = normalizeGeneratedLog(raw, "logs.find");
      assert.strictEqual(normalized.kind, expectedKind, name);
      assert.notOk(
        "log_kind" in normalized.payload ||
          "pdp_id" in normalized.payload,
        `${name} envelope fields do not leak into payload`,
      );
    }

    assert.throws(
      () =>
        normalizeGeneratedLog({
          id: "unknown-id",
          pdp_id: "pdp",
          message: 42,
        }, "logs.find"),
      (error: unknown) =>
        (error as { code?: unknown }).code === "GENERATED_PROTOCOL_ERROR",
    );
  });

  QUnit.test("normalizes raw failures without retaining secrets", async (assert) => {
    const secret = "raw-log-operation-secret";
    const engine = recordingEngine([]);
    engine.findLogs = async () => {
      throw new Error(secret);
    };
    const client = createClientForEngine(engine, {
      memoryLogging: true,
    });

    const result = await client.logs.find();
    assert.false(result.ok);
    if (!result.ok) {
      assert.strictEqual(result.error.code, "LOG_OPERATION_FAILED");
      assert.false(JSON.stringify(result.error).includes(secret));
    }
    await client.close();
  });
}
