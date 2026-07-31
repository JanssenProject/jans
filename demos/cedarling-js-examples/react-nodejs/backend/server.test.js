import assert from "node:assert/strict";
import { after, before, test } from "node:test";

import { createTaskApp } from "./server.js";

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
let baseUrl;
let server;

before(async () => {
  server = createTaskApp({ cedarling }).listen(0, "127.0.0.1");
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
