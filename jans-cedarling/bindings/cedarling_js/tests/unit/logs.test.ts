import type QUnitApi from "qunit";
import { assertCedarlingError } from "../run.js";

import { createClientForEngine } from "../../dist/client/client.js";
import type { CedarlingEngine } from "../../dist/engine/engine.js";
import { normalizeGeneratedLog } from "../../dist/logs/normalize.js";
import type { LogQuery } from "../../dist/logs/types.js";
import {
  createGeneratedEngineFixture,
  createTestEngine,
} from "./engine-fixture.js";

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

export default function registerLogsUnitTests(QUnit: QUnitApi): void {
  QUnit.module("logs");

  QUnit.test("rejects unsupported query combinations before the engine", async (assert) => {
    const calls: string[] = [];
    const client = createClientForEngine(recordingEngine(calls), {
      memoryLogging: true,
    });

    for (
      const { query, code, path } of [
        { query: {}, code: "INPUT_CONFLICT", path: [] },
        {
          query: { id: "log", requestId: "request" },
          code: "INPUT_CONFLICT",
          path: [],
        },
        { query: { tag: "future" }, code: "INPUT_UNSUPPORTED", path: ["tag"] },
        {
          query: { requestId: "" },
          code: "INPUT_REQUIRED",
          path: ["requestId"],
        },
        {
          query: { requestId: "request", extra: true },
          code: "INPUT_UNKNOWN_FIELD",
          path: ["extra"],
        },
      ] as const
    ) {
      const result = await client.logs.find(query as never);
      assertCedarlingError(assert, result, {
        code,
        operation: "logs.find",
        path,
      });
    }

    assert.deepEqual(calls, []);
    await client.shutDown();
  });

  QUnit.test("passes every supported query combination to the engine", async (assert) => {
    const received: Array<LogQuery | undefined> = [];
    const client = createClientForEngine(
      createTestEngine({
        async findLogs(query) {
          received.push(query);
          return [];
        },
      }),
      {
        memoryLogging: true,
      },
    );
    const queries: readonly (LogQuery | undefined)[] = [
      undefined,
      { id: "log" },
      { requestId: "request" },
      { tag: "decision" },
      { requestId: "request", tag: "error" },
    ];

    for (const query of queries) {
      assert.true((await client.logs.find(query)).ok);
    }

    assert.deepEqual(received, queries);
    await client.shutDown();
  });

  QUnit.test("maps public tags to generated log index values", async (assert) => {
    const tags: string[] = [];
    const engine = createGeneratedEngineFixture({
      get_logs_by_tag(tag: string) {
        tags.push(tag);
        return [];
      },
      get_logs_by_request_id_and_tag(_requestId: string, tag: string) {
        tags.push(tag);
        return [];
      },
    });

    await engine.findLogs({ tag: "decision" });
    await engine.findLogs({ tag: "warn" });
    await engine.findLogs({ requestId: "request", tag: "error" });

    assert.deepEqual(tags, ["Decision", "WARN", "ERROR"]);
    await engine.shutDown();
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

    for (
      const [name, raw, expectedKind] of [
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
      ] as const
    ) {
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

    assert.throws(
      () =>
        normalizeGeneratedLog({
          id: "future-id",
          log_kind: "Future",
          level: "WARN",
          message: "future log envelope",
          pdp_id: "pdp",
        }, "logs.find"),
      (error: unknown) =>
        (error as { code?: unknown }).code === "GENERATED_PROTOCOL_ERROR",
      "an explicit unknown kind cannot be relabeled as system",
    );
  });

  QUnit.test("normalizes raw failures without retaining secrets", async (assert) => {
    const secret = "raw-log-operation-secret"; // # gitleaks:allow
    const engine = recordingEngine([]);
    engine.findLogs = async () => {
      throw new Error(secret);
    };
    const client = createClientForEngine(engine, {
      memoryLogging: true,
    });

    const result = await client.logs.find();
    assertCedarlingError(assert, result, {
      code: "LOG_OPERATION_FAILED",
      operation: "logs.find",
    }, (error) => {
      assert.strictEqual(error.details, undefined);
      assert.false("cause" in error);
      assert.false(JSON.stringify(error).includes(secret));
    });
    await client.shutDown();
  });
}
