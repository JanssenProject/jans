import type QUnitApi from "qunit";
import { createClientForEngine } from "../../dist/client/client.js";
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
    const client = createClientForEngine(createTestEngine({
      async logIds() {
        throw new Error("must not run");
      },
    }));
    const logs = await client.logs.ids();
    assert.false(logs.ok);
    if (!logs.ok) {
      assert.strictEqual(logs.error.code, "LOG_STORAGE_UNAVAILABLE");
    }
    assert.true((await client.shutDown()).ok);
  });
}
