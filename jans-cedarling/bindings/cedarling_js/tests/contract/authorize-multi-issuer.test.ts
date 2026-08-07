import type QUnitApi from "qunit";
import {
  createCedarling,
  type JsonObject,
  type MultiIssuerAuthorizationRequest,
} from "@janssenproject/cedarling";
import { createMultiIssuerPolicyStore } from "../fixtures/multi-issuer-policy-store.js";

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

/** Registers multi-issuer behavior against the public package and real WASM. */
export default function registerMultiIssuerAuthorizationTests(
  QUnit: QUnitApi,
): void {
  QUnit.module("authorize-multi-issuer");

  /** Creates one offline client with signature and status validation disabled. */
  async function createClient(applicationName: string) {
    return await createCedarling({
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
    });
  }

  /** Creates a valid synthetic access token with caller-selected claims. */
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

  /** Creates the common public request around one ordered token list. */
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
    const created = await createClient("cedarling-js-multi-issuer-valid");

    assert.true(created.ok, "the real WASM client initializes");
    if (!created.ok) {
      return;
    }

    try {
      const authorized = await created.value.authorizeMultiIssuer(
        request([await accessToken("valid-token")]),
      );

      assert.true(authorized.ok, "the well-formed token set is processed");
      if (authorized.ok) {
        assert.true(authorized.value.decision);
        assert.deepEqual(authorized.value.diagnostics.reasons, [
          "token_present",
        ]);
      }
    } finally {
      assert.true((await created.value.shutDown()).ok);
    }
  });

  QUnit.test("expired and not-yet-valid token sets fail closed", async (assert) => {
    const created = await createClient("cedarling-js-multi-issuer-time");

    assert.true(created.ok, "the real WASM client initializes");
    if (!created.ok) {
      return;
    }

    try {
      const expired = await accessToken("expired-token", { exp: 1 });
      const future = await accessToken("future-token", {
        nbf: 4_000_000_000,
      });

      for (const [name, token] of [
        ["expired", expired],
        ["not-yet-valid", future],
      ] as const) {
        const authorized = await created.value.authorizeMultiIssuer(
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
    } finally {
      assert.true((await created.value.shutDown()).ok);
    }
  });

  QUnit.test("malformed and unknown-issuer token sets fail closed", async (assert) => {
    const created = await createClient("cedarling-js-multi-issuer-invalid");

    assert.true(created.ok, "the real WASM client initializes");
    if (!created.ok) {
      return;
    }

    try {
      const unknownIssuer = await signToken({
        iss: "https://unknown.example",
        sub: "mallory",
        jti: "unknown-issuer",
        exp: 4_000_000_000,
      });

      for (const [name, token] of [
        ["malformed", "not-a-jwt"],
        ["unknown issuer", unknownIssuer],
      ] as const) {
        const authorized = await created.value.authorizeMultiIssuer(
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
    } finally {
      assert.true((await created.value.shutDown()).ok);
    }
  });

  QUnit.test("a well-formed token survives a partially invalid token set", async (assert) => {
    const created = await createClient("cedarling-js-multi-issuer-partial");

    assert.true(created.ok, "the real WASM client initializes");
    if (!created.ok) {
      return;
    }

    try {
      const authorized = await created.value.authorizeMultiIssuer(
        request([await accessToken("valid-among-invalid"), "not-a-jwt"]),
      );

      assert.true(authorized.ok, "Cedarling ignores the malformed token");
      if (authorized.ok) {
        assert.true(authorized.value.decision);
        assert.deepEqual(authorized.value.diagnostics.reasons, [
          "token_present",
        ]);
      }
    } finally {
      assert.true((await created.value.shutDown()).ok);
    }
  });

  QUnit.test("duplicate mapped tokens are handled gracefully", async (assert) => {
    const created = await createClient("cedarling-js-multi-issuer-duplicate");

    assert.true(created.ok, "the real WASM client initializes");
    if (!created.ok) {
      return;
    }

    try {
      const first = await accessToken("duplicate-first", {
        scope: ["allow"],
      });
      const second = await accessToken("duplicate-second", {
        scope: ["deny"],
      });
      const authorized = await created.value.authorizeMultiIssuer(
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
    } finally {
      assert.true((await created.value.shutDown()).ok);
    }
  });

  QUnit.test("policy-store defaults override a colliding request resource", async (assert) => {
    const created = await createClient("cedarling-js-multi-issuer-default");

    assert.true(created.ok, "the real WASM client initializes");
    if (!created.ok) {
      return;
    }

    try {
      const input = request(
        [await accessToken("default-entity-token")],
        'Authorization::Action::"Default"',
      );
      const authorized = await created.value.authorizeMultiIssuer({
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
    } finally {
      assert.true((await created.value.shutDown()).ok);
    }
  });

  QUnit.test("a closed client rejects multi-issuer work without inspection", async (assert) => {
    const created = await createClient("cedarling-js-multi-issuer-closed");

    assert.true(created.ok, "the real WASM client initializes");
    if (!created.ok) {
      return;
    }

    assert.true((await created.value.shutDown()).ok, "the client closes");

    let reads = 0;
    const malicious = Object.defineProperty({}, "tokens", {
      enumerable: true,
      get() {
        reads += 1;
        return [];
      },
    });
    const authorized = await created.value.authorizeMultiIssuer(
      malicious as never,
    );

    assert.false(authorized.ok);
    if (!authorized.ok) {
      assert.strictEqual(authorized.error.code, "CLIENT_CLOSED");
      assert.strictEqual(
        authorized.error.operation,
        "authorizeMultiIssuer",
      );
    }
    assert.strictEqual(reads, 0, "closed work does not inspect caller input");
  });
}
