import type QUnitApi from "qunit";

import { createClientForEngine } from "../../dist/client/client.js";
import type { CedarlingEngine } from "../../dist/engine/engine.js";
import {
  createGeneratedEngineFixture,
  createTestEngine,
} from "./engine-fixture.js";

const decision = {
  decision: true,
  requestId: "multi-issuer-unit",
  diagnostics: { reasons: ["allow"], errors: [] },
};
const target = {
  action: 'Authorization::Action::"Read"',
  resource: { type: "Authorization::Resource", id: "document" },
};

export default function registerMultiIssuerAuthorizationUnitTests(
  QUnit: QUnitApi,
): void {
  QUnit.module("authorize-multi-issuer input boundary");

  QUnit.test("rejects unknown fields without invoking accessors or the engine", async (assert) => {
    let engineCalls = 0;
    let accessorCalls = 0;
    const client = createClientForEngine(createTestEngine({
      async authorizeMultiIssuer() {
        engineCalls += 1;
        return decision;
      },
    }));
    const token = {
      mapping: "Authorization::AccessToken",
      payload: "header.payload.signature",
    };
    const accessor = Object.defineProperty({ ...target, tokens: [token] }, "extra", {
      enumerable: true,
      get() {
        accessorCalls += 1;
        return "untrusted";
      },
    });

    for (const [input, path] of [
      [accessor, ["extra"]],
      [{ ...target, tokens: [{ ...token, issuer: "issuer.example" }] }, ["tokens", 0, "issuer"]],
    ] as const) {
      const result = await client.authorizeMultiIssuer(input as never);
      assert.false(result.ok);
      if (!result.ok) {
        assert.strictEqual(result.error.code, "INPUT_UNKNOWN_FIELD");
        assert.deepEqual(result.error.path, path);
      }
    }
    assert.strictEqual(accessorCalls, 0, "unknown accessors are never invoked");
    assert.strictEqual(engineCalls, 0, "invalid requests never reach the engine");
    assert.true((await client.shutDown()).ok);
  });

  QUnit.test("detaches token input while preserving order", async (assert) => {
    let accepted: Parameters<CedarlingEngine["authorizeMultiIssuer"]>[0] | undefined;
    const client = createClientForEngine(createTestEngine({
      async authorizeMultiIssuer(request) {
        accepted = request;
        return decision;
      },
    }));
    const tokens = [
      { mapping: "Authorization::First", payload: "first.payload.signature" },
      { mapping: "Authorization::Second", payload: "second.payload.signature" },
    ];
    const pending = client.authorizeMultiIssuer({ ...target, tokens });
    tokens.reverse();
    tokens[0].payload = "mutated";

    assert.true((await pending).ok);
    assert.deepEqual(accepted?.tokens, [
      { mapping: "Authorization::First", payload: "first.payload.signature" },
      { mapping: "Authorization::Second", payload: "second.payload.signature" },
    ]);
    assert.true((await client.shutDown()).ok);
  });

  QUnit.test("uses only the generated multi-issuer operation", async (assert) => {
    let unsignedCalls = 0;
    let requestJson = "";
    let disposals = 0;
    const engine = createGeneratedEngineFixture({
      async authorize_unsigned() {
        unsignedCalls += 1;
      },
      async authorize_multi_issuer(request) {
        requestJson = request;
        return {
          json_string: () => JSON.stringify({
            decision: true,
            request_id: "generated-multi-issuer",
            response: { diagnostics: { reason: ["token_present"], errors: [] } },
          }),
          free() { disposals += 1; },
        };
      },
    });

    const result = await engine.authorizeMultiIssuer({
      ...target,
      tokens: [{
        mapping: "Authorization::AccessToken",
        payload: "header.payload.signature",
      }],
    });
    assert.true(result.decision);
    assert.strictEqual(unsignedCalls, 0);
    assert.strictEqual(disposals, 1);
    assert.strictEqual(
      (JSON.parse(requestJson) as { tokens: unknown[] }).tokens.length,
      1,
    );
    await engine.shutDown();
  });
}
