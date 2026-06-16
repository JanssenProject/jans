import { before, after, describe, it } from "node:test";
import assert from "node:assert/strict";
import { spawn, ChildProcess } from "node:child_process";
import http from "node:http";
import { AddressInfo } from "node:net";
import { mkdtempSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";

// --- Helpers ----------------------------------------------------------------

let serverProc: ChildProcess;
let mockOidc: http.Server;
let baseUrl: string;
let issuer: string;
let tmpDir: string;

/** Posts a single JSON-RPC message to /mcp and returns the parsed response. */
async function mcp(method: string, params: unknown = {}, id = 1) {
  const res = await fetch(`${baseUrl}/mcp`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json, text/event-stream",
    },
    body: JSON.stringify({ jsonrpc: "2.0", id, method, params }),
  });
  return { status: res.status, body: await res.json() as any };
}

/** Minimal in-memory OIDC provider so tests need no external network. */
function startMockOidc(): Promise<{ server: http.Server; issuer: string }> {
  return new Promise((resolve) => {
    const server = http.createServer((req, res) => {
      const url = new URL(req.url || "/", `http://${req.headers.host}`);
      const send = (code: number, obj: unknown) => {
        res.writeHead(code, { "Content-Type": "application/json" });
        res.end(JSON.stringify(obj));
      };
      const iss = `http://${req.headers.host}`;

      if (url.pathname === "/.well-known/openid-configuration") {
        return send(200, {
          issuer: iss,
          registration_endpoint: `${iss}/register`,
          authorization_endpoint: `${iss}/authorize`,
          token_endpoint: `${iss}/token`,
          userinfo_endpoint: `${iss}/userinfo`,
        });
      }
      if (url.pathname === "/register") {
        return send(201, {
          client_id: "mock-client-id",
          client_secret: "mock-client-secret",
          registration_client_uri: `${iss}/register/mock-client-id`,
          registration_access_token: "mock-reg-token",
        });
      }
      if (url.pathname === "/token") {
        return send(200, {
          access_token: "mock-access-token",
          token_type: "Bearer",
          expires_in: 3600,
          id_token: "mock-id-token",
        });
      }
      if (url.pathname === "/userinfo") {
        return send(200, { sub: "user-123", email: "user@example.com" });
      }
      send(404, { error: "not_found" });
    });
    server.listen(0, "127.0.0.1", () => {
      const port = (server.address() as AddressInfo).port;
      resolve({ server, issuer: `http://127.0.0.1:${port}` });
    });
  });
}

async function waitForHealth(url: string, attempts = 50): Promise<void> {
  for (let i = 0; i < attempts; i++) {
    try {
      const res = await fetch(`${url}/health`);
      if (res.ok) return;
    } catch {
      /* not up yet */
    }
    await new Promise((r) => setTimeout(r, 100));
  }
  throw new Error("Server did not become healthy in time");
}

// --- Lifecycle --------------------------------------------------------------

before(async () => {
  ({ server: mockOidc, issuer } = await startMockOidc());

  tmpDir = mkdtempSync(path.join(tmpdir(), "mcp-test-"));
  const port = 3100 + Math.floor((mockOidc.address() as AddressInfo).port % 800);
  baseUrl = `http://127.0.0.1:${port}`;

  serverProc = spawn("node", ["dist/server.js"], {
    cwd: path.resolve(__dirname, ".."),
    env: {
      ...process.env,
      PORT: String(port),
      DB_FILE: path.join(tmpDir, "db.json"),
    },
    stdio: "ignore",
  });

  await waitForHealth(baseUrl);
});

after(() => {
  serverProc?.kill();
  mockOidc?.close();
  if (tmpDir) rmSync(tmpDir, { recursive: true, force: true });
});

// --- Tests ------------------------------------------------------------------

describe("health & metadata", () => {
  it("GET /health returns healthy", async () => {
    const res = await fetch(`${baseUrl}/health`);
    const body = (await res.json()) as any;
    assert.equal(res.status, 200);
    assert.equal(body.status, "healthy");
  });
});

describe("MCP transport", () => {
  it("lists the three registered tools", async () => {
    const { status, body } = await mcp("tools/list");
    assert.equal(status, 200);
    const names = body.result.tools.map((t: any) => t.name).sort();
    assert.deepEqual(names, ["exchangeToken", "registerOIDCClient", "startAuthFlow"]);
  });

  // Regression test: a single shared StreamableHTTPServerTransport made every
  // request after the first fail with HTTP 500. A fresh transport per request
  // must keep all of them succeeding.
  it("handles repeated requests on the stateless endpoint", async () => {
    for (let i = 0; i < 3; i++) {
      const { status, body } = await mcp("tools/list", {}, i + 1);
      assert.equal(status, 200, `request ${i + 1} should succeed`);
      assert.ok(Array.isArray(body.result.tools));
    }
  });
});

describe("MCP tools", () => {
  it("registerOIDCClient returns client credentials", async () => {
    const { status, body } = await mcp("tools/call", {
      name: "registerOIDCClient",
      arguments: {
        issuer,
        redirect_uris: ["https://app.example.com/cb"],
        scopes: ["openid", "profile"],
        response_types: ["code"],
        token_endpoint_auth_method: "client_secret_basic",
        userinfo_signed_response_alg: "RS256",
        jansInclClaimsInIdTkn: "true",
      },
    });
    assert.equal(status, 200);
    assert.equal(body.result.structuredContent.client_id, "mock-client-id");
    assert.equal(body.result.isError, undefined);
  });

  it("startAuthFlow builds an authorization URL with PKCE + state", async () => {
    const { status, body } = await mcp("tools/call", {
      name: "startAuthFlow",
      arguments: {
        issuer,
        client_id: "mock-client-id",
        scope: "openid",
        redirect_uri: "https://app.example.com/cb",
        code_challenge: "0123456789abcdefghijklmnopqrstuvwxyzABCDEFG",
        nonce: "0123456789abcdef0123",
      },
    });
    assert.equal(status, 200);
    const sc = body.result.structuredContent;
    const authUrl = new URL(sc.authorization_url);
    assert.equal(authUrl.searchParams.get("client_id"), "mock-client-id");
    assert.equal(authUrl.searchParams.get("code_challenge_method"), "S256");
    assert.ok(sc.state.length > 0);
    assert.equal(sc.expires_in, 600);
  });

  it("exchangeToken returns tokens and userinfo", async () => {
    const { status, body } = await mcp("tools/call", {
      name: "exchangeToken",
      arguments: {
        issuer,
        code: "auth-code-123",
        client_id: "mock-client-id",
        client_secret: "mock-client-secret",
        code_verifier: "verifier-value",
        redirect_uri: "https://app.example.com/cb",
      },
    });
    assert.equal(status, 200);
    const sc = body.result.structuredContent;
    assert.equal(sc.tokens.access_token, "mock-access-token");
    assert.equal(sc.userinfo.sub, "user-123");
  });

  it("returns a tool error for an invalid issuer URL", async () => {
    const { body } = await mcp("tools/call", {
      name: "startAuthFlow",
      arguments: {
        issuer: "not-a-url",
        client_id: "x",
        scope: "openid",
        redirect_uri: "https://app.example.com/cb",
        code_challenge: "0123456789abcdefghijklmnopqrstuvwxyzABCDEFG",
        nonce: "0123456789abcdef0123",
      },
    });
    // Invalid input is rejected (either schema validation or isError result).
    assert.ok(body.result?.isError || body.error, "should report an error");
  });
});

describe("API key management", () => {
  let createdId: string;

  it("POST /api/keys creates a key and hides the full value", async () => {
    const res = await fetch(`${baseUrl}/api/keys`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ provider: "openai", model: "gpt-4", key: "sk-secret-1234" }),
    });
    const body = (await res.json()) as any;
    assert.equal(res.status, 201);
    assert.equal(body.provider, "openai");
    assert.equal(body.keyPreview, "...1234");
    assert.equal(body.key, undefined, "full key must not be returned");
    createdId = body.id;
  });

  it("POST /api/keys rejects a missing key", async () => {
    const res = await fetch(`${baseUrl}/api/keys`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ provider: "openai", model: "gpt-4" }),
    });
    assert.equal(res.status, 400);
  });

  it("POST /api/keys is idempotent on duplicates (409)", async () => {
    const res = await fetch(`${baseUrl}/api/keys`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ provider: "openai", model: "gpt-4", key: "sk-secret-1234" }),
    });
    assert.equal(res.status, 409);
  });

  it("GET /api/keys lists keys without full values", async () => {
    const res = await fetch(`${baseUrl}/api/keys`);
    const body = (await res.json()) as any;
    assert.equal(res.status, 200);
    assert.ok(body.count >= 1);
    assert.ok(body.keys.every((k: any) => k.key === undefined));
  });

  it("GET /api/keys/:id returns the full key and stamps lastUsed", async () => {
    const res = await fetch(`${baseUrl}/api/keys/${createdId}`);
    const body = (await res.json()) as any;
    assert.equal(res.status, 200);
    assert.equal(body.key, "sk-secret-1234");
    assert.ok(body.lastUsed, "lastUsed should be set");
  });

  it("PUT /api/keys updates an existing key", async () => {
    const res = await fetch(`${baseUrl}/api/keys`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ provider: "openai", model: "gpt-4", key: "sk-updated-5678" }),
    });
    const body = (await res.json()) as any;
    assert.equal(res.status, 200);
    assert.equal(body.keyPreview, "...5678");
  });

  it("GET /api/keys/:id returns 404 for an unknown id", async () => {
    const res = await fetch(`${baseUrl}/api/keys/does-not-exist`);
    assert.equal(res.status, 404);
  });

  it("DELETE /api/keys/:id removes the key", async () => {
    const res = await fetch(`${baseUrl}/api/keys/${createdId}`, { method: "DELETE" });
    const body = (await res.json()) as any;
    assert.equal(res.status, 200);
    assert.equal(body.success, true);

    const after = await fetch(`${baseUrl}/api/keys/${createdId}`);
    assert.equal(after.status, 404);
  });

  it("DELETE /api/keys/provider/:provider removes all keys for a provider", async () => {
    await fetch(`${baseUrl}/api/keys`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ provider: "anthropic", model: "claude", key: "sk-a-1" }),
    });
    const res = await fetch(`${baseUrl}/api/keys/provider/anthropic`, { method: "DELETE" });
    const body = (await res.json()) as any;
    assert.equal(res.status, 200);
    assert.ok(body.deletedCount >= 1);
  });
});
