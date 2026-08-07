import { createServer, type Server } from "node:http";

import type { JsonObject } from "@janssenproject/cedarling";

const keyId = "cedarling-js-signature-test";
const encoder = new TextEncoder();

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

/** Starts one server after its listener and address are both available. */
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
    throw new Error("The signed-issuer fixture has no TCP address.");
  }
  return address.port;
}

/** Closes one listening fixture server. */
async function close(server: Server): Promise<void> {
  await new Promise<void>((resolve, reject) => {
    server.close((error) => {
      if (error === undefined) {
        resolve();
      } else {
        reject(error);
      }
    });
  });
}

/** Isolated offline OIDC issuer used by the real-WASM signature contract. */
export interface SignedIssuerFixture {
  /** Issuer identifier written into signed token claims. */
  readonly issuer: string;

  /** Discovery URL embedded in the inline trusted-issuer policy store. */
  readonly openidConfigurationEndpoint: string;

  /** Produces one RS256 JWT using the fixture's in-memory private key. */
  signToken(claims?: JsonObject): Promise<string>;

  /** Stops the loopback discovery and JWKS server. */
  close(): Promise<void>;
}

/** Creates an in-memory RSA key and loopback OIDC discovery/JWKS server. */
export async function createSignedIssuer(): Promise<SignedIssuerFixture> {
  const keys = await crypto.subtle.generateKey(
    {
      name: "RSASSA-PKCS1-v1_5",
      modulusLength: 2_048,
      publicExponent: new Uint8Array([1, 0, 1]),
      hash: "SHA-256",
    },
    true,
    ["sign", "verify"],
  );
  const publicKey = await crypto.subtle.exportKey("jwk", keys.publicKey);
  let issuer = "";
  const server = createServer((request, response) => {
    response.setHeader("content-type", "application/json");

    if (request.url === "/.well-known/openid-configuration") {
      response.end(JSON.stringify({
        issuer,
        jwks_uri: `${issuer}/jwks`,
      }));
      return;
    }
    if (request.url === "/jwks") {
      response.end(JSON.stringify({
        keys: [{
          ...publicKey,
          alg: "RS256",
          kid: keyId,
          use: "sig",
        }],
      }));
      return;
    }

    response.statusCode = 404;
    response.end(JSON.stringify({ error: "not_found" }));
  });
  const port = await listen(server);
  issuer = `http://127.0.0.1:${port}`;

  return {
    issuer,
    openidConfigurationEndpoint:
      `${issuer}/.well-known/openid-configuration`,
    async signToken(claims: JsonObject = {}) {
      const now = Math.floor(Date.now() / 1_000);
      const header = base64Url(encoder.encode(JSON.stringify({
        alg: "RS256",
        kid: keyId,
        typ: "JWT",
      })));
      const payload = base64Url(encoder.encode(JSON.stringify({
        iss: issuer,
        sub: "alice",
        jti: "signed-token",
        iat: now - 60,
        exp: now + 3_600,
        ...claims,
      })));
      const input = `${header}.${payload}`;
      const signature = await crypto.subtle.sign(
        "RSASSA-PKCS1-v1_5",
        keys.privateKey,
        encoder.encode(input),
      );
      return `${input}.${base64Url(new Uint8Array(signature))}`;
    },
    close: () => close(server),
  };
}
