import type QUnitApi from "qunit";
import {
  createCedarling,
  type AuthorizationDecision,
  type JsonObject,
} from "@janssenproject/cedarling";
import { createMultiIssuerPolicyStore } from "../fixtures/multi-issuer-policy-store.js";
import { tracerPolicyStore } from "../fixtures/tracer-policy-store.js";

/** Encodes bytes with the URL-safe unpadded base64 form used by JWT. */
function base64Url(bytes: Uint8Array): string {
  let binary = "";
  for (const byte of bytes) {
    binary += String.fromCharCode(byte);
  }
  return btoa(binary)
    .replaceAll("+", "-")
    .replaceAll("/", "_")
    .replace(/=+$/u, "");
}

/** Produces one synthetic signed JWT using host-neutral Web APIs. */
async function signToken(claims: JsonObject): Promise<string> {
  const encoder = new TextEncoder();
  const header = base64Url(
    encoder.encode(JSON.stringify({ alg: "HS256", typ: "JWT" })),
  );
  const payload = base64Url(encoder.encode(JSON.stringify(claims)));
  const input = `${header}.${payload}`;
  const key = await crypto.subtle.importKey(
    "raw",
    encoder.encode("cedarling-js-contract-signing-key"),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const signature = await crypto.subtle.sign(
    "HMAC",
    key,
    encoder.encode(input),
  );
  return `${input}.${base64Url(new Uint8Array(signature))}`;
}

/** Omits the intentionally unique request ID before semantic comparison. */
function semantics(decision: AuthorizationDecision) {
  return {
    decision: decision.decision,
    diagnostics: decision.diagnostics,
  };
}

/** Registers public dispatch equivalence tests against real WASM. */
export default function registerAuthorizationContractTests(
  QUnit: QUnitApi,
): void {
  QUnit.module("authorization-dispatch");

  QUnit.test("unsigned named and overloaded calls have identical semantics", async (assert) => {
    const created = await createCedarling({
      applicationName: "cedarling-js-dispatch-unsigned",
      policyStore: {
        type: "inline",
        document: tracerPolicyStore,
      },
    });

    assert.true(created.ok, "the real WASM client initializes");
    if (!created.ok) {
      return;
    }

    try {
      const request = {
        principal: { type: "Tracer::User", id: "alice" },
        action: 'Tracer::Action::"Read"',
        resource: { type: "Tracer::Resource", id: "document" },
      } as const;
      const named = await created.value.authorizeUnsigned(request);
      const dispatched = await created.value.authorize({
        type: "unsigned",
        request,
      });

      assert.true(named.ok);
      assert.true(dispatched.ok);
      if (named.ok && dispatched.ok) {
        assert.deepEqual(
          semantics(dispatched.value),
          semantics(named.value),
        );
        assert.strictEqual(dispatched.allowed, named.allowed);
        assert.strictEqual(dispatched.denied, named.denied);
      }
    } finally {
      assert.true((await created.value.close()).ok);
    }
  });

  QUnit.test("multi-issuer named and overloaded calls have identical semantics", async (assert) => {
    const created = await createCedarling({
      applicationName: "cedarling-js-dispatch-multi-issuer",
      authorization: {
        dangerouslyDisableSchemaValidation: true,
      },
      jwt: {
        dangerouslyDisableSignatureValidation: true,
        dangerouslyDisableStatusValidation: true,
      },
      policyStore: {
        type: "inline",
        document: createMultiIssuerPolicyStore(),
      },
    });

    assert.true(created.ok, "the real WASM client initializes");
    if (!created.ok) {
      return;
    }

    try {
      const request = {
        tokens: [
          {
            mapping: "Authorization::AccessToken",
            payload: await signToken({
              iss: "https://issuer.example",
              sub: "alice",
              jti: "dispatch-token",
              iat: 1_700_000_000,
              exp: 4_000_000_000,
            }),
          },
        ],
        action: 'Authorization::Action::"Read"',
        resource: {
          type: "Authorization::Resource",
          id: "document",
        },
      } as const;
      const named = await created.value.authorizeMultiIssuer(request);
      const dispatched = await created.value.authorize({
        type: "multiIssuer",
        request,
      });

      assert.true(named.ok);
      assert.true(dispatched.ok);
      if (named.ok && dispatched.ok) {
        assert.deepEqual(
          semantics(dispatched.value),
          semantics(named.value),
        );
        assert.strictEqual(dispatched.allowed, named.allowed);
        assert.strictEqual(dispatched.denied, named.denied);
      }
    } finally {
      assert.true((await created.value.close()).ok);
    }
  });
}
