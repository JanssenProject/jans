import assert from "node:assert/strict";
import { after, before, test } from "node:test";

import { createApp } from "./idp.js";

const issuer = "http://127.0.0.1:9090";
const frontendOrigin = "http://localhost:3000";
let baseUrl;
let server;

before(async () => {
  server = createApp(issuer, {
    frontendOrigin,
    logger: { error() {} },
  }).listen(0, "127.0.0.1");
  await new Promise((resolve, reject) => {
    server.once("listening", resolve);
    server.once("error", reject);
  });
  const address = server.address();
  baseUrl = `http://127.0.0.1:${address.port}`;
});

after(async () => {
  await new Promise((resolve, reject) => {
    server.close((error) => (error ? reject(error) : resolve()));
  });
});

test("publishes a code-flow-only RS256 provider", async () => {
  const discovery = await fetch(
    `${baseUrl}/.well-known/openid-configuration`,
  ).then((response) => response.json());
  assert.deepEqual(discovery.response_types_supported, ["code"]);
  assert.deepEqual(discovery.scopes_supported, ["openid", "profile", "role"]);

  const jwks = await fetch(`${baseUrl}/jwks`).then((response) =>
    response.json(),
  );
  assert.equal(jwks.keys.length, 1);
  assert.equal(jwks.keys[0].alg, "RS256");
  assert.equal(jwks.keys[0].d, undefined);
});

test("serves strict Cedarling config and an issuer-specific policy store", async () => {
  const config = await fetch(`${baseUrl}/config/cedarling`).then((response) =>
    response.json(),
  );
  assert.deepEqual(config, {
    applicationName: "TaskApp",
    jwt: { allowedAlgorithms: ["RS256"] },
  });

  const document = await fetch(`${baseUrl}/config/policy-store`).then(
    (response) => response.json(),
  );
  const store = document.policy_stores.TaskApp;
  assert.equal(
    store.trusted_issuers.LocalMockIdP.openid_configuration_endpoint,
    `${issuer}/.well-known/openid-configuration`,
  );
  assert.deepEqual(
    Object.keys(store.trusted_issuers.LocalMockIdP.token_metadata),
    ["userinfo_token"],
  );
  assert.match(
    store.policies["create-token"].policy_content.body,
    /resource\.owner/,
  );
});


test("allows only the configured browser origin", async () => {
  const allowed = await fetch(`${baseUrl}/config/cedarling`, {
    headers: { Origin: frontendOrigin },
  });
  assert.equal(
    allowed.headers.get("access-control-allow-origin"),
    frontendOrigin,
  );

  const rejected = await fetch(`${baseUrl}/config/cedarling`, {
    method: "OPTIONS",
    headers: { Origin: "https://attacker.example" },
  });
  assert.equal(rejected.status, 403);
  assert.equal(rejected.headers.get("access-control-allow-origin"), null);
});

test("rejects non-loopback HTTP issuer URLs", () => {
  assert.throws(
    () => createApp("http://idp.example", { frontendOrigin }),
    /HTTPS origin/,
  );
});
