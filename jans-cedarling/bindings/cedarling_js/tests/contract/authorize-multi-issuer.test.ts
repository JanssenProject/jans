import type QUnitApi from "qunit";
import {
  type CedarlingClient,
  type JsonObject,
  type MultiIssuerAuthorizationRequest,
} from "@janssenproject/cedarling";
import { withCedarling } from "../run.js";
import { createMultiIssuerPolicyStore } from "../fixtures/multi-issuer-policy-store.js";

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

/**
 * Produces one well-formed HMAC-signed JWT for token-processing contracts.
 *
 * This suite deliberately disables signature verification; cryptographic
 * verification is covered by the dedicated signature-validation E2E suite.
 */
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

export default function registerMultiIssuerAuthorizationTests(
  QUnit: QUnitApi,
): void {
  QUnit.module("authorize-multi-issuer");

  async function withClient(
    assert: Assert,
    applicationName: string,
    work: (client: CedarlingClient) => Promise<void>,
  ): Promise<void> {
    await withCedarling(assert, {
      applicationName,
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
    }, work);
  }

  async function accessToken(
    id: string,
    claims: JsonObject = {},
  ): Promise<string> {
    return await signToken({
      iss: "https://issuer.example",
      sub: "alice",
      jti: id,
      iat: 1_700_000_000,
      exp: 4_000_000_000,
      ...claims,
    });
  }

  function request(
    payloads: readonly string[],
    action = 'Authorization::Action::"Read"',
  ): MultiIssuerAuthorizationRequest {
    return {
      tokens: payloads.map((payload) => ({
        mapping: "Authorization::AccessToken",
        payload,
      })),
      action,
      resource: {
        type: "Authorization::Resource",
        id: "document",
      },
    };
  }

  QUnit.test("authorizes one well-formed token set", async (assert) => {
    await withClient(
      assert,
      "cedarling-js-multi-issuer-valid",
      async (client) => {
        const authorized = await client.authorizeMultiIssuer(
          request([await accessToken("valid-token")]),
        );

        assert.true(authorized.ok, "the well-formed token set is processed");
        if (authorized.ok) {
          assert.true(authorized.value.decision);
          assert.deepEqual(authorized.value.diagnostics.reasons, [
            "token_present",
          ]);
        }
      },
    );
  });

  QUnit.test("expired and not-yet-valid token sets fail closed", async (assert) => {
    await withClient(
      assert,
      "cedarling-js-multi-issuer-time",
      async (client) => {
        const expired = await accessToken("expired-token", { exp: 1 });
        const future = await accessToken("future-token", {
          nbf: 4_000_000_000,
        });

        for (
          const [name, token] of [
            ["expired", expired],
            ["not-yet-valid", future],
          ] as const
        ) {
          const authorized = await client.authorizeMultiIssuer(
            request([token]),
          );
          assert.false(authorized.ok, `${name} token is rejected`);
          if (!authorized.ok) {
            assert.strictEqual(
              authorized.error.code,
              "AUTHORIZATION_FAILED",
            );
            assert.strictEqual(
              authorized.error.operation,
              "authorizeMultiIssuer",
            );
            assert.false(
              JSON.stringify(authorized.error).includes(token),
              "the rejected token is not disclosed",
            );
          }
        }
      },
    );
  });

  QUnit.test("malformed and unknown-issuer token sets fail closed", async (assert) => {
    await withClient(
      assert,
      "cedarling-js-multi-issuer-invalid",
      async (client) => {
        const unknownIssuer = await signToken({
          iss: "https://unknown.example",
          sub: "mallory",
          jti: "unknown-issuer",
          exp: 4_000_000_000,
        });

        for (
          const [name, token] of [
            ["malformed", "not-a-jwt"],
            ["unknown issuer", unknownIssuer],
          ] as const
        ) {
          const authorized = await client.authorizeMultiIssuer(
            request([token]),
          );
          assert.false(authorized.ok, `${name} token is rejected`);
          if (!authorized.ok) {
            assert.strictEqual(
              authorized.error.code,
              "AUTHORIZATION_FAILED",
            );
            assert.false(
              JSON.stringify(authorized.error).includes(token),
              "the rejected token is not disclosed",
            );
          }
        }
      },
    );
  });

  QUnit.test("a well-formed token survives a partially invalid token set", async (assert) => {
    await withClient(
      assert,
      "cedarling-js-multi-issuer-partial",
      async (client) => {
        const authorized = await client.authorizeMultiIssuer(
          request([await accessToken("valid-among-invalid"), "not-a-jwt"]),
        );

        assert.true(authorized.ok, "Cedarling ignores the malformed token");
        if (authorized.ok) {
          assert.true(authorized.value.decision);
          assert.deepEqual(authorized.value.diagnostics.reasons, [
            "token_present",
          ]);
        }
      },
    );
  });

  QUnit.test("duplicate mapped tokens are handled gracefully", async (assert) => {
    await withClient(
      assert,
      "cedarling-js-multi-issuer-duplicate",
      async (client) => {
        const first = await accessToken("duplicate-first", {
          scope: ["allow"],
        });
        const second = await accessToken("duplicate-second", {
          scope: ["deny"],
        });
        const authorized = await client.authorizeMultiIssuer(
          request([first, second]),
        );

        assert.true(authorized.ok, "duplicates are handled by Cedarling");
        if (authorized.ok) {
          assert.true(
            authorized.value.decision,
            "a duplicate does not erase the valid mapped token",
          );
          assert.deepEqual(authorized.value.diagnostics.reasons, [
            "token_present",
          ]);
        }
      },
    );
  });

  QUnit.test("policy-store defaults override a colliding request resource", async (assert) => {
    await withClient(
      assert,
      "cedarling-js-multi-issuer-default",
      async (client) => {
        const input = request(
          [await accessToken("default-entity-token")],
          'Authorization::Action::"Default"',
        );
        const authorized = await client.authorizeMultiIssuer({
          ...input,
          resource: {
            type: "Authorization::Resource",
            id: "default",
            attributes: {
              owner: "attacker",
            },
          },
        });

        assert.true(authorized.ok, "authorization completes");
        if (authorized.ok) {
          assert.true(
            authorized.value.decision,
            "policy-store entity data takes precedence",
          );
          assert.deepEqual(authorized.value.diagnostics.reasons, [
            "default_entity",
          ]);
        }
      },
    );
  });
}
