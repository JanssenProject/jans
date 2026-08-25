import type QUnitApi from "qunit";
import { assertCedarlingError } from "../run.js";

import { createClientForEngine } from "../../.build/client/client.js";
import { createTestEngine } from "./engine-fixture.js";

export default function registerCapabilityTests(QUnit: QUnitApi): void {
  QUnit.module("client capabilities");

  QUnit.test("context, logs, and issuer operations use the Engine Seam", async (assert) => {
    let retained: unknown;
    const client = createClientForEngine(createTestEngine({
      async setContext(_key, value) {
        retained = value;
      },
      async getContext() {
        return retained as never;
      },
      async logIds() {
        return ["log-1"];
      },
      async isIssuerLoaded(issuer) {
        return "id" in issuer && issuer.id === "issuer";
      },
    }), {
      memoryLogging: true,
      contextMaxTtlSeconds: 60,
    });

    const value = { enabled: true };
    assert.true((await client.context.set("feature", value, {
      ttlSeconds: 30,
    })).ok);
    value.enabled = false;
    assert.deepEqual(await client.context.get("feature"), {
      ok: true,
      value: { enabled: true },
    });
    assert.deepEqual(await client.logs.ids(), {
      ok: true,
      value: ["log-1"],
    });
    assert.deepEqual(await client.issuers.isLoaded({ id: "issuer" }), {
      ok: true,
      value: true,
    });
    assert.true((await client.shutDown()).ok);
  });

  QUnit.test("disabled memory logging fails before the Engine", async (assert) => {
    const calls: string[] = [];
    const client = createClientForEngine(createTestEngine({
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
    }));

    for (const [operation, work] of [
      ["logs.ids", () => client.logs.ids()],
      ["logs.find", () => client.logs.find()],
      ["logs.drain", () => client.logs.drain()],
    ] as const) {
      assertCedarlingError<unknown>(assert, await work(), {
        code: "LOG_STORAGE_UNAVAILABLE",
        operation,
      });
    }

    assert.deepEqual(calls, []);
    assert.true((await client.shutDown()).ok);
  });
}
