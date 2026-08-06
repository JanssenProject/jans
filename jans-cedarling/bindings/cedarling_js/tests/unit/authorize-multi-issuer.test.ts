import type QUnitApi from "qunit";

import { createClientForEngine } from "../../dist/client/client.js";
import type { CedarlingEngine } from "../../dist/engine/engine.js";
import { createGeneratedEngine } from "../../dist/engine/generated.js";
import { createTestEngine } from "./engine-fixture.js";

const decision = {
  decision: true,
  requestId: "multi-issuer-unit",
  diagnostics: { reasons: ["allow"], errors: [] },
};

/** Registers deterministic multi-issuer request-boundary tests. */
export default function registerMultiIssuerAuthorizationUnitTests(
  QUnit: QUnitApi,
): void {
  QUnit.module("authorize-multi-issuer");

  QUnit.test("rejects an empty token set before the engine", async (assert) => {
    let calls = 0;
    const engine = createTestEngine({
      async authorizeMultiIssuer() {
        calls += 1;
        return decision;
      },
    });
    const client = createClientForEngine(engine);

    const result = await client.authorizeMultiIssuer({
      tokens: [],
      action: 'Authorization::Action::"Read"',
      resource: {
        type: "Authorization::Resource",
        id: "document",
      },
    });

    assert.false(result.ok);
    if (!result.ok) {
      assert.strictEqual(result.error.code, "INVALID_INPUT");
      assert.strictEqual(result.error.operation, "authorizeMultiIssuer");
      assert.deepEqual(result.error.issues?.[0]?.path, ["tokens"]);
      assert.strictEqual(result.error.issues?.[0]?.code, "range");
    }
    assert.strictEqual(calls, 0, "the engine is not called");
    await client.shutDown();
  });

  QUnit.test("rejects unknown request and token fields before the engine", async (assert) => {
    let calls = 0;
    const client = createClientForEngine(createTestEngine({
      async authorizeMultiIssuer() {
        calls += 1;
        return decision;
      },
    }));
    const baseRequest = {
      tokens: [{
        mapping: "Authorization::AccessToken",
        payload: "header.payload.signature",
      }],
      action: 'Authorization::Action::"Read"',
      resource: { type: "Authorization::Resource", id: "document" },
    };

    for (const testCase of [
      {
        name: "top-level field",
        value: { ...baseRequest, tokenz: baseRequest.tokens },
        path: ["tokenz"],
      },
      {
        name: "token field",
        value: {
          ...baseRequest,
          tokens: [{ ...baseRequest.tokens[0], issuer: "issuer.example" }],
        },
        path: ["tokens", 0, "issuer"],
      },
    ] as const) {
      const result = await client.authorizeMultiIssuer(testCase.value as never);
      assert.false(result.ok, testCase.name);
      if (!result.ok) {
        assert.deepEqual(result.error.issues?.[0], {
          path: testCase.path,
          code: "unknownField",
          message: "The field is not supported.",
        });
      }
    }

    assert.strictEqual(calls, 0, "no invalid request reaches the engine");
    await client.shutDown();
  });

  QUnit.test("validates token fields and preserves detached input order", async (assert) => {
    let accepted:
      | Parameters<CedarlingEngine["authorizeMultiIssuer"]>[0]
      | undefined;
    const engine: CedarlingEngine = createTestEngine({
      async authorizeMultiIssuer(request) {
        accepted = request;
        return decision;
      },
    });
    const client = createClientForEngine(engine);

    for (const [field, token] of [
      ["mapping", { mapping: " ", payload: "header.payload.signature" }],
      ["payload", { mapping: "Authorization::AccessToken", payload: "" }],
    ] as const) {
      const invalid = await client.authorizeMultiIssuer({
        tokens: [token],
        action: 'Authorization::Action::"Read"',
        resource: {
          type: "Authorization::Resource",
          id: "document",
        },
      });
      assert.false(invalid.ok, `${field} is required`);
      if (!invalid.ok) {
        assert.deepEqual(invalid.error.issues?.[0]?.path, [
          "tokens",
          0,
          field,
        ]);
      }
    }

    const tokens = [
      {
        mapping: "Authorization::First",
        payload: "first.payload.signature",
      },
      {
        mapping: "Authorization::Second",
        payload: "second.payload.signature",
      },
    ];
    const pending = client.authorizeMultiIssuer({
      tokens,
      action: 'Authorization::Action::"Read"',
      resource: {
        type: "Authorization::Resource",
        id: "document",
      },
    });
    tokens.reverse();
    tokens[0].payload = "mutated";
    tokens.push({
      mapping: "Authorization::LateArrival",
      payload: "late.payload.signature",
    });

    assert.true((await pending).ok);
    assert.deepEqual(accepted?.tokens, [
      {
        mapping: "Authorization::First",
        payload: "first.payload.signature",
      },
      {
        mapping: "Authorization::Second",
        payload: "second.payload.signature",
      },
    ]);
    await client.shutDown();
  });

  QUnit.test("disposes the generated result wrapper when authorization throws", async (assert) => {
    let resultDisposals = 0;
    const engine = createGeneratedEngine({
      async authorize_unsigned() {
        throw new Error("wrong generated operation");
      },
      async authorize_multi_issuer() {
        return {
          json_string(): string {
            throw new Error("generated json_string failure");
          },
          free() {
            resultDisposals += 1;
          },
        };
      },
      async shut_down() {},
      free() {},
    });

    assert.ok(engine, "the generated client is compatible");
    if (engine === undefined) {
      throw new Error("unreachable: assert.ok already failed");
    }

    try {
      await engine.authorizeMultiIssuer({
        tokens: [
          {
            mapping: "Authorization::AccessToken",
            payload: "header.payload.signature",
          },
        ],
        action: 'Authorization::Action::"Read"',
        resource: {
          type: "Authorization::Resource",
          id: "document",
        },
      });
      assert.pushResult({
        result: false,
        actual: "resolved",
        expected: "RESULT_CONVERSION_FAILED",
        message: "json_string failure must reject",
      });
    } catch (error: unknown) {
      assert.strictEqual(
        (error as { code?: unknown }).code,
        "RESULT_CONVERSION_FAILED",
      );
    }
    assert.strictEqual(
      resultDisposals,
      1,
      "the result wrapper is released exactly once after a throw",
    );
    await engine.shutDown();
  });

  QUnit.test("uses only the generated multi-issuer operation", async (assert) => {
    let unsignedCalls = 0;
    let multiIssuerRequest: string | undefined;
    let resultDisposals = 0;
    const engine = createGeneratedEngine({
      async authorize_unsigned() {
        unsignedCalls += 1;
        throw new Error("wrong generated operation");
      },
      async authorize_multi_issuer(request: string) {
        multiIssuerRequest = request;
        return {
          json_string() {
            return JSON.stringify({
              decision: true,
              request_id: "generated-multi-issuer",
              response: {
                diagnostics: {
                  reason: ["token_present"],
                  errors: [],
                },
              },
            });
          },
          free() {
            resultDisposals += 1;
          },
        };
      },
      async shut_down() {},
      free() {},
    });

    assert.ok(engine, "the generated client is compatible");
    if (engine === undefined) {
      throw new Error("unreachable: assert.ok already failed");
    }

    const result = await engine.authorizeMultiIssuer({
      tokens: [
        {
          mapping: "Authorization::AccessToken",
          payload: "header.payload.signature",
        },
      ],
      action: 'Authorization::Action::"Read"',
      resource: {
        type: "Authorization::Resource",
        id: "document",
      },
    });

    assert.true(result.decision);
    assert.strictEqual(unsignedCalls, 0);
    assert.strictEqual(resultDisposals, 1);
    assert.deepEqual(
      JSON.parse(multiIssuerRequest ?? "") as unknown,
      {
        tokens: [
          {
            mapping: "Authorization::AccessToken",
            payload: "header.payload.signature",
          },
        ],
        action: 'Authorization::Action::"Read"',
        resource: {
          cedar_entity_mapping: {
            entity_type: "Authorization::Resource",
            id: "document",
          },
        },
        context: {},
      },
    );
    await engine.shutDown();
  });
}
