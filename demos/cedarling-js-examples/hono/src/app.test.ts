import assert from "node:assert/strict";
import { test } from "node:test";

import { createApp, type Bindings } from "./app";
import type { AuthorizationInput } from "./cedarling/authorize";

const bindings: Bindings = {
  FRONTEND_ORIGIN: "http://localhost:3000",
  OIDC_ISSUER: "http://localhost:9090",
};

test("requires identity and accepts lowercase Bearer", async () => {
  const calls: AuthorizationInput[] = [];
  const app = createApp({
    authorize: async (input) => {
      calls.push(input);
      return { kind: "allowed" };
    },
  });
  assert.equal((await app.request("/tasks", {}, bindings)).status, 401);
  const response = await app.request(
    "/tasks",
    { headers: { "x-user-id": "bob", authorization: "bearer signed" } },
    bindings,
  );
  assert.equal(response.status, 200);
  assert.equal(calls[0].token, "signed");
});

test("validates task input and resolves a missing task before authorization", async () => {
  let calls = 0;
  const app = createApp({
    authorize: async () => {
      calls += 1;
      return { kind: "allowed" };
    },
  });
  const invalid = await app.request(
    "/tasks",
    {
      method: "POST",
      headers: { "content-type": "application/json", "x-user-id": "bob" },
      body: JSON.stringify({ title: "" }),
    },
    bindings,
  );
  assert.equal(invalid.status, 400);
  const missing = await app.request(
    "/tasks/missing",
    { method: "DELETE", headers: { "x-user-id": "bob" } },
    bindings,
  );
  assert.equal(missing.status, 404);
  assert.equal(calls, 0);
});

test("distinguishes denial, invalid signed identity, and service failure", async () => {
  for (const [outcome, expected] of [
    [{ kind: "denied" as const }, 403],
    [{ kind: "error" as const, signed: true }, 401],
    [{ kind: "error" as const, signed: false }, 503],
  ] as const) {
    const app = createApp({ authorize: async () => outcome });
    const headers: Record<string, string> = { "x-user-id": "bob" };
    if (outcome.kind === "error" && outcome.signed) {
      headers.authorization = "Bearer invalid";
    }
    assert.equal((await app.request("/tasks", { headers }, bindings)).status, expected);
  }
});

test("uses exact-origin CORS", async () => {
  const app = createApp({ authorize: async () => ({ kind: "allowed" }) });
  const rejected = await app.request(
    "/tasks",
    { method: "OPTIONS", headers: { origin: "https://attacker.example" } },
    bindings,
  );
  assert.equal(rejected.status, 403);
  const allowed = await app.request(
    "/tasks",
    { method: "OPTIONS", headers: { origin: bindings.FRONTEND_ORIGIN! } },
    bindings,
  );
  assert.equal(allowed.status, 204);
  assert.equal(allowed.headers.get("access-control-allow-origin"), bindings.FRONTEND_ORIGIN);
});
