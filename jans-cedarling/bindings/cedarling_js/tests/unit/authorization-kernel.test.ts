import type QUnitApi from "qunit";
import { createClientForEngine } from "../../dist/client/client.js";
import type { CedarlingEngine } from "../../dist/engine/engine.js";
import { prepareCedarlingOptions } from "../../dist/configuration/prepare.js";
import { createGeneratedEngine } from "../../dist/engine/generated.js";
import { parseGeneratedResult } from "../../dist/engine/generated-authorization.js";
import { withGeneratedWrapper } from "../../dist/engine/generated-wrapper.js";

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

  QUnit.test("configuration rejects null decision-log claims", (assert) => {
    assert.throws(
      () => prepareCedarlingOptions({
        applicationName: "review-test",
        policyStore: { type: "inline", document: {} },
        authorization: { decisionLogTokenIdClaim: null as never },
      }),
      (error: unknown) =>
        (error as { code?: unknown }).code === "INPUT_INVALID_TYPE",
    );
  });

  QUnit.test("wrapper disposal preserves conversion failures", (assert) => {
    const conversionFailure = new Error("conversion");
    let disposals = 0;
    assert.throws(
      () => withGeneratedWrapper(
        { dispose() { disposals += 1; throw new Error("dispose"); } },
        "authorizeUnsigned",
        () => { throw conversionFailure; },
      ),
      (error: unknown) => error === conversionFailure,
    );
    assert.strictEqual(disposals, 1);
  });

  QUnit.test("generated failures redact raw diagnostics by default", async (assert) => {
    const rawFailure = new Error("raw-wasm-secret");
    const engine = createGeneratedEngine({
      authorize_unsigned() { throw rawFailure; },
      authorize_multi_issuer() { throw rawFailure; },
      shut_down() {},
      free() {},
    });
    assert.ok(engine);
    if (engine === undefined) return;
    const request = {
      action: 'Action::"Read"',
      resource: { type: "Task", id: "one" },
    };
    const hidden = await createClientForEngine(engine).authorizeUnsigned(request);
    assert.false(hidden.ok);
    if (!hidden.ok) {
      assert.strictEqual(hidden.error.details, undefined);
      assert.false("cause" in hidden.error);
    }
    const exposed = await createClientForEngine(
      engine,
      { exposeRawErrors: true },
    ).authorizeUnsigned(request);
    assert.false(exposed.ok);
    if (!exposed.ok) assert.strictEqual(exposed.error.cause, rawFailure);

    const policyError = parseGeneratedResult(
      JSON.stringify({
        decision: false,
        request_id: "request",
        response: { diagnostics: { reason: [], errors: [{ id: "unsafe policy id", error: "secret" }] } },
      }),
      "authorizeUnsigned",
    ).errors[0];
    assert.strictEqual(policyError?.message, "A Cedar policy evaluation failed.");
    assert.strictEqual(policyError?.details, undefined);
  });
}
