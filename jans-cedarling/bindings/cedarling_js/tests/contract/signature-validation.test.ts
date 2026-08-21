import { createServer, type Server } from "node:http";
import type QUnitApi from "qunit";

import {
  createCedarling,
  type JsonObject,
} from "@janssenproject/cedarling";
import { createMultiIssuerPolicyStore } from "../fixtures/multi-issuer-policy-store.js";
import { assertCedarlingError } from "../run.js";

const encoder = new TextEncoder();
const keyId = "cedarling-js-signature-test";

function base64Url(bytes: Uint8Array): string {
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary)
    .replaceAll("+", "-")
    .replaceAll("/", "_")
    .replace(/=+$/u, "");
}

async function listen(server: Server): Promise<number> {
  await new Promise<void>((resolve, reject) => {
    server.once("error", reject);
    server.listen(0, "127.0.0.1", () => {
      server.off("error", reject);
      resolve();
    });
  });
  const address = server.address();
  if (address === null || typeof address === "string") {
    throw new Error("The test issuer has no TCP address.");
  }
  return address.port;
}

async function close(server: Server): Promise<void> {
  await new Promise<void>((resolve, reject) => {
    server.close((error) => error === undefined ? resolve() : reject(error));
  });
}

function tamperSignature(token: string): string {
  const [header, payload, signature] = token.split(".");
  if (header === undefined || payload === undefined || signature === undefined) {
    throw new Error("The test issuer returned an invalid JWT.");
  }
  const replacement = signature[0] === "A" ? "B" : "A";
  return `${header}.${payload}.${replacement}${signature.slice(1)}`;
}

export default function registerSignatureValidationTests(
  QUnit: QUnitApi,
): void {
  QUnit.module("signature validation");

  QUnit.test("accepts RS256 and rejects a tampered signature", async (assert) => {
    assert.timeout(20_000);
    const keys = await crypto.subtle.generateKey({
      name: "RSASSA-PKCS1-v1_5",
      modulusLength: 2_048,
      publicExponent: new Uint8Array([1, 0, 1]),
      hash: "SHA-256",
    }, true, ["sign", "verify"]);
    const publicKey = await crypto.subtle.exportKey("jwk", keys.publicKey);
    let issuer = "";
    const server = createServer((request, response) => {
      response.setHeader("content-type", "application/json");
      if (request.url === "/.well-known/openid-configuration") {
        response.end(JSON.stringify({ issuer, jwks_uri: `${issuer}/jwks` }));
      } else if (request.url === "/jwks") {
        response.end(JSON.stringify({
          keys: [{ ...publicKey, alg: "RS256", kid: keyId, use: "sig" }],
        }));
      } else {
        response.statusCode = 404;
        response.end(JSON.stringify({ error: "not_found" }));
      }
    });

    try {
      issuer = `http://127.0.0.1:${await listen(server)}`;
      const created = await createCedarling({
        applicationName: "cedarling-js-signature-validation",
        authorization: { dangerouslyDisableSchemaValidation: true },
        jwt: {
          allowedAlgorithms: ["RS256"],
          dangerouslyDisableStatusValidation: true,
        },
        issuerLoading: { mode: "sync", workers: 1 },
        policyStore: {
          type: "inline",
          document: createMultiIssuerPolicyStore(
            `${issuer}/.well-known/openid-configuration`,
          ),
        },
      });
      assert.true(created.ok, "the issuer keys load");
      if (!created.ok) return;

      try {
        const now = Math.floor(Date.now() / 1_000);
        const claims: JsonObject = {
          iss: issuer,
          sub: "alice",
          jti: "signed-token",
          iat: now - 60,
          exp: now + 3_600,
        };
        const header = base64Url(encoder.encode(JSON.stringify({
          alg: "RS256",
          kid: keyId,
          typ: "JWT",
        })));
        const payload = base64Url(encoder.encode(JSON.stringify(claims)));
        const input = `${header}.${payload}`;
        const signature = await crypto.subtle.sign(
          "RSASSA-PKCS1-v1_5",
          keys.privateKey,
          encoder.encode(input),
        );
        const token = `${input}.${base64Url(new Uint8Array(signature))}`;
        const authorize = (value: string) => created.value.authorizeMultiIssuer({
          tokens: [{ mapping: "Authorization::AccessToken", payload: value }],
          action: 'Authorization::Action::"Read"',
          resource: { type: "Authorization::Resource", id: "document" },
        });

        const valid = await authorize(token);
        assert.true(
          valid.ok,
          valid.ok
            ? "the valid token is processed"
            : `the valid token failed with ${valid.error.code}`,
        );
        if (!valid.ok) return;
        assert.true(valid.value.decision, "the valid token authorizes");
        const tampered = await authorize(tamperSignature(token));
        assertCedarlingError(assert, tampered, {
          code: "AUTHORIZATION_FAILED",
          operation: "authorizeMultiIssuer",
        });
      } finally {
        assert.true((await created.value.shutDown()).ok);
      }
    } finally {
      await close(server);
    }
  });
}
