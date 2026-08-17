import type QUnitApi from "qunit";
import { createClientForEngine } from "../../dist/client/client.js";
import type { CedarlingEngine } from "../../dist/engine/engine.js";

export default function registerAuthorizationKernelTests(
  QUnit: QUnitApi,
): void {
  QUnit.module("authorization kernel");

  QUnit.test("snapshots inputs and preserves the result contract", async (assert) => {
    let receivedId = "";
    const engine: CedarlingEngine = {
      async authorizeUnsigned(request) {
        receivedId = request.resource.id;
        return {
          decision: true,
          requestId: "request-1",
          diagnostics: { reasons: ["allow"], errors: [] },
        };
      },
      async authorizeMultiIssuer() {
        return {
          decision: true,
          requestId: "request-2",
          diagnostics: { reasons: ["token"], errors: [] },
        };
      },
      async shutDown() {},
    };
    const client = createClientForEngine(engine);
    const resource = { type: "Task", id: "original" };
    const pending = client.authorizeUnsigned({
      action: 'Action::"Read"',
      resource,
    });
    resource.id = "mutated";

    const result = await pending;
    assert.true(result.ok);
    assert.strictEqual(receivedId, "original");
    if (result.ok) {
      assert.true(result.value.decision);
      assert.strictEqual(result.value.requestId, "request-1");
    }
    assert.true((await client.shutDown()).ok);
  });

  QUnit.test("returns validation and closed-client failures", async (assert) => {
    const engine: CedarlingEngine = {
      async authorizeUnsigned() {
        throw new Error("must not run");
      },
      async authorizeMultiIssuer() {
        throw new Error("must not run");
      },
      async shutDown() {},
    };
    const client = createClientForEngine(engine);
    const invalid = await client.authorizeMultiIssuer({
      tokens: [],
      action: 'Action::"Read"',
      resource: { type: "Task", id: "one" },
    });
    assert.false(invalid.ok);
    if (!invalid.ok) {
      assert.strictEqual(invalid.error.code, "INPUT_OUT_OF_RANGE");
      assert.deepEqual(invalid.error.path, ["tokens"]);
    }

    assert.true((await client.shutDown()).ok);
    const closed = await client.authorizeUnsigned(
      new Proxy({} as never, {
        get() {
          assert.true(false, "closed clients must not inspect input");
        },
      }),
    );
    assert.false(closed.ok);
    if (!closed.ok) {
      assert.strictEqual(closed.error.code, "CLIENT_CLOSED");
    }
  });

  QUnit.test("shutdown waits for admitted authorization", async (assert) => {
    let release = () => {};
    const blocked = new Promise<void>((resolve) => {
      release = resolve;
    });
    const engine: CedarlingEngine = {
      async authorizeUnsigned() {
        await blocked;
        return {
          decision: true,
          requestId: "request-3",
          diagnostics: { reasons: [], errors: [] },
        };
      },
      async authorizeMultiIssuer() {
        throw new Error("unused");
      },
      async shutDown() {},
    };
    const client = createClientForEngine(engine);
    const authorization = client.authorizeUnsigned({
      action: 'Action::"Read"',
      resource: { type: "Task", id: "one" },
    });
    const shutdown = client.shutDown();
    release();
    assert.true((await authorization).ok);
    assert.true((await shutdown).ok);
    assert.strictEqual(await client.shutDown(), await shutdown);
  });
}
