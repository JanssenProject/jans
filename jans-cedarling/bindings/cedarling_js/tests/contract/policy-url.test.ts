import type QUnitApi from "qunit";

import { createCedarling } from "@janssenproject/cedarling";
import type { CedarlingClient } from "@janssenproject/cedarling";
import type { RuntimeFixtures } from "../run.js";

const denyTracerPolicyStore = {
  cedar_version: "v4.0.0",
  policy_stores: {
    tracer: {
      cedar_version: "v4.0.0",
      name: "Tracer",
      policies: {
        deny: {
          description: "deny the public tracer",
          creation_date: "2026-07-23T00:00:00Z",
          policy_content: {
            encoding: "none",
            content_type: "cedar",
            body:
              'forbid(principal, action == Tracer::Action::"Read", resource);',
          },
        },
      },
      schema: {
        encoding: "none",
        content_type: "cedar",
        body:
          "namespace Tracer {\n" +
          "entity User;\n" +
          "entity Resource;\n" +
          'action "Read" appliesTo { principal: [User], resource: [Resource], context: {} };\n' +
          "}",
      },
    },
  },
} as const;

/** Evaluates the shared tracer request and returns its public decision. */
async function tracerDecision(
  client: CedarlingClient,
): Promise<boolean> {
  const result = await client.authorizeUnsigned({
    principal: { type: "Tracer::User", id: "alice" },
    action: 'Tracer::Action::"Read"',
    resource: { type: "Tracer::Resource", id: "document" },
  });
  if (!result.ok) {
    throw result.error;
  }
  return result.value.decision;
}

/** Waits until public authorization observes an expected refreshed decision. */
async function waitForTracerDecision(
  client: CedarlingClient,
  expected: boolean,
): Promise<boolean> {
  const deadline = Date.now() + 2_000;
  let observed = await tracerDecision(client);
  while (observed !== expected && Date.now() < deadline) {
    await new Promise<void>((resolveWait) => {
      setTimeout(resolveWait, 25);
    });
    observed = await tracerDecision(client);
  }
  return observed;
}

/** Registers public URL policy-source contracts against real WASM. */
export default function registerPolicyUrlTests(
  QUnit: QUnitApi,
  fixtures: RuntimeFixtures,
): void {
  QUnit.module("policy-url");

  QUnit.test("Cedarling fetches and evaluates an extensionless JSON URL", async (assert) => {
    await fixtures.withPolicyServer(async (server) => {
      const created = await createCedarling({
        applicationName: "url-json-policy",
        policyStore: {
          type: "url",
          url: server.jsonUrl,
        },
      });

      assert.true(created.ok, "the URL policy initializes");
      if (!created.ok) {
        return;
      }

      try {
        const authorized = await created.value.authorizeUnsigned({
          principal: { type: "Tracer::User", id: "alice" },
          action: 'Tracer::Action::"Read"',
          resource: { type: "Tracer::Resource", id: "document" },
        });
        assert.true(authorized.ok);
        if (authorized.ok) {
          assert.true(authorized.value.decision);
        }
        assert.strictEqual(server.requestCount(), 1);
      } finally {
        const closed = await created.value.close();
        assert.true(closed.ok);
      }
    });
  });

  QUnit.test("Cedarling detects archive bytes without a cjar suffix", async (assert) => {
    await fixtures.withPolicyServer(async (server) => {
      const created = await createCedarling({
        applicationName: "url-archive-policy",
        policyStore: {
          type: "url",
          url: server.archiveUrl,
        },
      });

      assert.true(created.ok, "archive magic bytes select the cjar parser");
      if (!created.ok) {
        return;
      }

      try {
        const authorized = await created.value.authorizeUnsigned({
          principal: { type: "Tracer::User", id: "alice" },
          action: 'Tracer::Action::"Read"',
          resource: { type: "Tracer::Resource", id: "document" },
        });
        assert.true(authorized.ok);
        if (authorized.ok) {
          assert.true(authorized.value.decision);
          assert.deepEqual(authorized.value.diagnostics.reasons, ["allow"]);
        }
      } finally {
        const closed = await created.value.close();
        assert.true(closed.ok);
      }
    });
  });

  QUnit.test("refresh swaps valid policies atomically and retains the last good store", async (assert) => {
    assert.timeout(25_000);
    await fixtures.withPolicyServer(async (server) => {
      const created = await createCedarling({
        applicationName: "url-refresh-policy",
        policyStore: {
          type: "url",
          url: server.jsonUrl,
          refresh: { intervalSeconds: 5 },
        },
      });

      assert.true(created.ok);
      if (!created.ok) {
        return;
      }

      assert.true(await tracerDecision(created.value), "initial store allows");
      server.setJsonResponse(200, JSON.stringify(denyTracerPolicyStore));
      await server.waitForRequestCount(2);
      assert.false(
        await waitForTracerDecision(created.value, false),
        "one complete valid refresh replaces the store",
      );

      server.setJsonResponse(200, '{"privatePolicyMaterial":');
      await server.waitForRequestCount(3);
      assert.false(
        await tracerDecision(created.value),
        "a malformed refresh retains the last valid store",
      );

      const closed = await created.value.close();
      assert.true(closed.ok);
      const requestsAfterClose = server.requestCount();
      // Wait 7.5s (1.5× the configured 5s refresh interval) so a wrongly
      // scheduled refresh would have a comfortable margin to arrive.
      await new Promise<void>((resolveWait) => {
        setTimeout(resolveWait, 7_500);
      });
      assert.strictEqual(
        server.requestCount(),
        requestsAfterClose,
        "close stops background policy refresh",
      );
    });
  });

  QUnit.test("URL initialization failures redact response and URL secrets", async (assert) => {
    await fixtures.withPolicyServer(async (server) => {
      const secret = "url-policy-secret";
      server.setJsonResponse(500, secret);
      const result = await createCedarling({
        applicationName: "url-failure-redaction",
        policyStore: {
          type: "url",
          url: `${server.jsonUrl}?token=${secret}#${secret}`,
        },
        http: { maxRetries: 0 },
      });

      assert.false(result.ok);
      if (!result.ok) {
        assert.strictEqual(result.error.code, "INITIALIZATION_FAILED");
        assert.strictEqual(result.error.operation, "initialize");
        assert.false(JSON.stringify(result.error).includes(secret));
      }
    });
  });
}
