import assert from "node:assert/strict";
import { after, before, test } from "node:test";

import { createTaskApp, parseBooleanFlag } from "./server.js";

const calls = [];
const cedarling = {
  async authorizeUnsigned(request) {
    calls.push({ kind: "unsigned", request });
    return { ok: true, value: { decision: request.context.userId === request.resource.attributes.owner } };
  },
  async authorizeMultiIssuer(request) {
    calls.push({ kind: "signed", request });
    return { ok: true, value: { decision: request.tokens[0].payload === "valid" } };
  },
};

async function verifyTokenSub(token) {
  if (token === "valid" || token.startsWith("error-")) return "bob";
  throw new Error("Invalid token");
}

let baseUrl;
let server;

before(async () => {
  server = createTaskApp({
    cedarling,
    verifyTokenSub,
    allowUnsignedDemoIdentity: true,
  }).listen(0, "127.0.0.1");
  await new Promise((resolve) => server.once("listening", resolve));
  const address = server.address();
  baseUrl = `http://127.0.0.1:${address.port}`;
});

after(async () => {
  await new Promise((resolve, reject) =>
    server.close((error) => (error ? reject(error) : resolve())),
  );
});

test("requires an explicit known identity", async () => {
  assert.equal((await fetch(`${baseUrl}/tasks`)).status, 401);
  assert.equal(
    (await fetch(`${baseUrl}/tasks`, { headers: { "x-user-id": "mallory" } })).status,
    401,
  );
});

test("accepts a lowercase Bearer scheme without exposing the token", async () => {
  const response = await fetch(`${baseUrl}/tasks`, {
    headers: { "x-user-id": "bob", authorization: "bearer valid" },
  });
  assert.equal(response.status, 200);
  assert.equal(calls.at(-1).kind, "signed");
});

test("validates bodies and resolves missing tasks before authorization", async () => {
  assert.equal(
    (
      await fetch(`${baseUrl}/tasks`, {
        method: "POST",
        headers: { "content-type": "application/json", "x-user-id": "bob" },
        body: JSON.stringify({ title: "" }),
      })
    ).status,
    400,
  );
  const beforeCalls = calls.length;
  assert.equal(
    (
      await fetch(`${baseUrl}/tasks/missing`, {
        method: "DELETE",
        headers: { "x-user-id": "bob" },
      })
    ).status,
    404,
  );
  assert.equal(calls.length, beforeCalls);
});

test("fails closed on an authorization operation error", async () => {
  const failingApp = createTaskApp({
    allowUnsignedDemoIdentity: true,
    cedarling: {
      async authorizeUnsigned() {
        return { ok: false, error: { code: "AUTHORIZATION_FAILED" } };
      },
    },
  });
  const failingServer = failingApp.listen(0, "127.0.0.1");
  await new Promise((resolve) => failingServer.once("listening", resolve));
  const address = failingServer.address();
  const response = await fetch(`http://127.0.0.1:${address.port}/tasks`, {
    headers: { "x-user-id": "bob" },
  });
  assert.equal(response.status, 503);
  await new Promise((resolve) => failingServer.close(resolve));
});

test("maps signed identity failures to 401 and signed operational failures to 503", async () => {
  for (const [token, code, expected] of [
    ["error-auth", "AUTHORIZATION_FAILED", 401],
    ["error-client", "CLIENT_CLOSED", 503],
  ]) {
    const failingApp = createTaskApp({
      cedarling: {
        async authorizeMultiIssuer() {
          return { ok: false, error: { code } };
        },
      },
      verifyTokenSub,
    });
    const failingServer = failingApp.listen(0, "127.0.0.1");
    await new Promise((resolve) => failingServer.once("listening", resolve));
    const address = failingServer.address();
    const response = await fetch(`http://127.0.0.1:${address.port}/tasks`, {
      headers: { "x-user-id": "bob", authorization: `Bearer ${token}` },
    });
    assert.equal(response.status, expected);
    await new Promise((resolve) => failingServer.close(resolve));
  }
});

test("returns 403 when the user does not own the task", async () => {
  const response = await fetch(`${baseUrl}/tasks/task-1`, {
    method: "DELETE",
    headers: { "x-user-id": "alice" },
  });
  assert.equal(response.status, 403);
  assert.equal(calls.at(-1).kind, "unsigned");
});

test("rejects an invalid signed token", async () => {
  const response = await fetch(`${baseUrl}/tasks`, {
    headers: { "x-user-id": "bob", authorization: "Bearer garbage" },
  });
  assert.equal(response.status, 401);
});

test("never downgrades a malformed Authorization header to unsigned identity", async () => {
  const beforeCalls = calls.length;
  const response = await fetch(`${baseUrl}/tasks`, {
    headers: { "x-user-id": "bob", authorization: "Basic forged" },
  });
  assert.equal(response.status, 401);
  assert.equal(calls.length, beforeCalls);
});

test("requires server opt-in before accepting a development identity", async () => {
  const guardedApp = createTaskApp({ cedarling, verifyTokenSub });
  const guardedServer = guardedApp.listen(0, "127.0.0.1");
  await new Promise((resolve) => guardedServer.once("listening", resolve));
  const address = guardedServer.address();
  const response = await fetch(`http://127.0.0.1:${address.port}/tasks`, {
    headers: { "x-user-id": "bob" },
  });
  assert.equal(response.status, 401);
  await new Promise((resolve) => guardedServer.close(resolve));
});

test("parses the unsigned development identity flag strictly", () => {
  assert.equal(parseBooleanFlag(undefined, "ALLOW_UNSIGNED_DEMO_IDENTITY"), false);
  assert.equal(parseBooleanFlag("true", "ALLOW_UNSIGNED_DEMO_IDENTITY"), true);
  assert.equal(parseBooleanFlag("false", "ALLOW_UNSIGNED_DEMO_IDENTITY"), false);
  assert.throws(
    () => parseBooleanFlag("yes", "ALLOW_UNSIGNED_DEMO_IDENTITY"),
    /ALLOW_UNSIGNED_DEMO_IDENTITY must be true or false/,
  );
});

test("rate limits every protected method through one shared limiter", async (t) => {
  const cases = [
    ["GET", "/tasks", undefined, 200],
    ["POST", "/tasks", { title: "Limited task" }, 201],
    ["PUT", "/tasks/task-1", { completed: true }, 200],
    ["DELETE", "/tasks/task-1", undefined, 204],
  ];

  for (const [method, pathname, body, expectedStatus] of cases) {
    await t.test(method, async () => {
      let authorizationCalls = 0;
      const limitedApp = createTaskApp({
        allowUnsignedDemoIdentity: true,
        cedarling: {
          async authorizeUnsigned() {
            authorizationCalls += 1;
            return { ok: true, value: { decision: true } };
          },
        },
        taskRateLimit: { limit: 1, windowMs: 60_000 },
      });
      const limitedServer = limitedApp.listen(0, "127.0.0.1");
      await new Promise((resolve) => limitedServer.once("listening", resolve));
      const address = limitedServer.address();
      const limitedBaseUrl = `http://127.0.0.1:${address.port}`;
      const headers = { "x-user-id": "bob" };
      if (body) headers["content-type"] = "application/json";
      const accepted = await fetch(`${limitedBaseUrl}${pathname}`, {
        method,
        headers,
        body: body ? JSON.stringify(body) : undefined,
      });
      assert.equal(accepted.status, expectedStatus);

      const rejected = await fetch(`${limitedBaseUrl}/tasks`, { headers });
      assert.equal(rejected.status, 429);
      assert.deepEqual(await rejected.json(), { error: "Too many requests" });
      assert.ok(rejected.headers.get("retry-after"));
      assert.ok(rejected.headers.get("ratelimit"));
      assert.equal(authorizationCalls, 1);
      await new Promise((resolve) => limitedServer.close(resolve));
    });
  }
});

test("validates requests before consuming authorization capacity", async () => {
  let authorizationCalls = 0;
  const limitedApp = createTaskApp({
    allowUnsignedDemoIdentity: true,
    cedarling: {
      async authorizeUnsigned() {
        authorizationCalls += 1;
        return { ok: true, value: { decision: true } };
      },
    },
    taskRateLimit: { limit: 1, windowMs: 60_000 },
  });
  const limitedServer = limitedApp.listen(0, "127.0.0.1");
  await new Promise((resolve) => limitedServer.once("listening", resolve));
  const address = limitedServer.address();
  const limitedBaseUrl = `http://127.0.0.1:${address.port}`;
  const invalid = await fetch(`${limitedBaseUrl}/tasks`, {
    method: "POST",
    headers: { "content-type": "application/json", "x-user-id": "bob" },
    body: JSON.stringify({ title: "" }),
  });
  assert.equal(invalid.status, 400);

  const accepted = await fetch(`${limitedBaseUrl}/tasks`, {
    headers: { "x-user-id": "bob" },
  });
  assert.equal(accepted.status, 200);
  const rejected = await fetch(`${limitedBaseUrl}/tasks`, {
    headers: { "x-user-id": "bob" },
  });
  assert.equal(rejected.status, 429);
  assert.equal(authorizationCalls, 1);
  await new Promise((resolve) => limitedServer.close(resolve));
});
