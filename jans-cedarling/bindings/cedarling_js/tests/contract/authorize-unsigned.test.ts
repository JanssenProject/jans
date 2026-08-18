import type QUnitApi from "qunit";
import type { CedarlingClient } from "@janssenproject/cedarling";
import {
  tracerExtensionPolicyStore,
  tracerPolicyStore,
} from "../fixtures/tracer-policy-store.js";
import { withCedarling } from "../run.js";

function containsGeneratedSurface(
  value: unknown,
  visited = new Set<object>(),
): boolean {
  if (typeof value === "bigint" || value instanceof Map) {
    return true;
  }
  if (typeof value !== "object" || value === null || visited.has(value)) {
    return false;
  }

  visited.add(value);
  if (
    "free" in value ||
    "memory" in value ||
    (Symbol.dispose !== undefined && Symbol.dispose in value)
  ) {
    return true;
  }

  return Reflect.ownKeys(value).some((key) =>
    containsGeneratedSurface(Reflect.get(value, key), visited)
  );
}

export default function registerUnsignedAuthorizationTests(
  QUnit: QUnitApi,
): void {
  QUnit.module("authorize-unsigned");

  async function withClient(
    assert: Assert,
    applicationName: string,
    work: (client: CedarlingClient) => Promise<void>,
  ): Promise<void> {
    await withCedarling(
      assert,
      {
        applicationName,
        policyStore: {
          type: "inline",
          document: tracerPolicyStore,
        },
      },
      work,
    );
  }

  QUnit.test("returns allow and deny as successful decisions", async (assert) => {
    await withClient(
      assert,
      "cedarling-js-unsigned-decisions",
      async (client) => {
        const principal = {
          type: "Tracer::User",
          id: "alice",
        };
        const resource = {
          type: "Tracer::Resource",
          id: "document",
        };

        const allowed = await client.authorizeUnsigned({
          principal,
          action: 'Tracer::Action::"Read"',
          resource,
        });
        const denied = await client.authorizeUnsigned({
          principal,
          action: 'Tracer::Action::"Deny"',
          resource,
        });

        assert.true(allowed.ok, "allow is a successful authorization");
        if (allowed.ok) {
          assert.true(allowed.value.decision);
          assert.false(
            "allowed" in allowed,
            "the canonical result has no flat allow shortcut",
          );
          assert.false(
            "denied" in allowed,
            "the canonical result has no flat deny shortcut",
          );
          assert.false(
            "decision" in allowed,
            "the decision remains under result.value",
          );
          assert.false(
            "err" in allowed,
            "errors use the canonical error field",
          );
          assert.deepEqual(allowed.value.diagnostics.reasons, ["allow"]);
          assert.propEqual(
            structuredClone(allowed.value),
            allowed.value,
            "the decision is ordinary structured-cloneable data",
          );
          assert.strictEqual(
            Object.getPrototypeOf(allowed.value),
            Object.prototype,
            "the decision has a plain object prototype",
          );
          assert.false(
            containsGeneratedSurface(allowed.value),
            "no wrapper, disposal hook, memory, Map, or bigint escapes",
          );
        }
        assert.true(denied.ok, "deny is not an operational failure");
        if (denied.ok) {
          assert.false(denied.value.decision);
          assert.deepEqual(denied.value.diagnostics.reasons, []);
        }
      },
    );
  });

  QUnit.test("accepts a structured Cedar action without exposing UID syntax", async (assert) => {
    await withClient(
      assert,
      "cedarling-js-structured-action",
      async (client) => {
        const authorized = await client.authorizeUnsigned({
          principal: {
            type: "Tracer::User",
            id: "alice",
          },
          action: {
            namespace: "Tracer",
            id: "Read",
          },
          resource: {
            type: "Tracer::Resource",
            id: "document",
          },
        });

        assert.true(authorized.ok, "the SDK constructs the Cedar action UID");
        if (authorized.ok) {
          assert.true(authorized.value.decision);
          assert.deepEqual(authorized.value.diagnostics.reasons, ["allow"]);
        }
      },
    );
  });

  QUnit.test("omitted principals preserve partial evaluation and fail closed", async (assert) => {
    await withClient(
      assert,
      "cedarling-js-unsigned-partial",
      async (client) => {
        const resource = {
          type: "Tracer::Resource",
          id: "document",
        };

        const publicResult = await client.authorizeUnsigned({
          action: 'Tracer::Action::"Public"',
          resource,
        });
        const protectedResult = await client.authorizeUnsigned({
          action: 'Tracer::Action::"Protected"',
          resource,
        });

        assert.true(publicResult.ok, "principal-independent policy resolves");
        if (publicResult.ok) {
          assert.true(publicResult.value.decision);
          assert.deepEqual(publicResult.value.diagnostics.reasons, ["public"]);
        }
        assert.true(
          protectedResult.ok,
          "a residual is a decision, not an error",
        );
        if (protectedResult.ok) {
          assert.false(protectedResult.value.decision);
          assert.true(
            protectedResult.value.diagnostics.reasons.includes("residual"),
            "the residual policy ID is preserved",
          );
        }
      },
    );
  });

  QUnit.test("entity attributes are converted from a detached request snapshot", async (assert) => {
    await withClient(
      assert,
      "cedarling-js-unsigned-entities",
      async (client) => {
        const principal = {
          type: "Tracer::User",
          id: "alice",
          attributes: {
            role: "editor",
            labels: ["stable"],
          },
        };
        const resource = {
          type: "Tracer::Resource",
          id: "document",
          attributes: {
            owner: "alice",
          },
        };

        const pending = client.authorizeUnsigned({
          principal,
          action: 'Tracer::Action::"Edit"',
          resource,
        });

        principal.attributes.role = "viewer";
        principal.attributes.labels[0] = "mutated";
        resource.attributes.owner = "mallory";

        const authorized = await pending;
        assert.true(authorized.ok, "authorization uses the accepted snapshot");
        if (authorized.ok) {
          assert.true(authorized.value.decision);
          assert.deepEqual(authorized.value.diagnostics.reasons, [
            "entity_attributes",
          ]);
        }
      },
    );
  });

  QUnit.test("canonical Cedar extension markers reach the engine unchanged", async (assert) => {
    await withCedarling(assert, {
      applicationName: "cedarling-js-unsigned-extension",
      authorization: {
        dangerouslyDisableSchemaValidation: true,
      },
      policyStore: {
        type: "inline",
        document: tracerExtensionPolicyStore,
      },
    }, async (client) => {
      const authorized = await client.authorizeUnsigned({
        principal: {
          type: "Tracer::User",
          id: "alice",
        },
        action: 'Tracer::Action::"Connect"',
        resource: {
          type: "Tracer::Resource",
          id: "gateway",
        },
        context: {
          network: {
            __extn: {
              fn: "ip",
              arg: "192.0.2.42",
            },
          },
        },
      });

      assert.true(authorized.ok, "the extension is accepted by Cedarling");
      if (authorized.ok) {
        assert.true(authorized.value.decision);
        assert.deepEqual(authorized.value.diagnostics.reasons, ["network"]);
      }
    });
  });

  QUnit.test("an attribute-free resource resolves its policy-store default entity", async (assert) => {
    await withClient(
      assert,
      "cedarling-js-unsigned-default-entity",
      async (client) => {
        const authorized = await client.authorizeUnsigned({
          principal: {
            type: "Tracer::User",
            id: "alice",
          },
          action: 'Tracer::Action::"UseDefault"',
          resource: {
            type: "Tracer::Resource",
            id: "default",
          },
        });

        assert.true(authorized.ok, "the default entity is usable");
        if (authorized.ok) {
          assert.true(authorized.value.decision);
          assert.deepEqual(authorized.value.diagnostics.reasons, [
            "default_entity",
          ]);
        }
      },
    );
  });

  QUnit.test("rejects a whitespace-only entity type before authorization", async (assert) => {
    await withClient(
      assert,
      "cedarling-js-unsigned-validation",
      async (client) => {
        const authorized = await client.authorizeUnsigned({
          principal: {
            type: " ",
            id: "alice",
          },
          action: 'Tracer::Action::"Read"',
          resource: {
            type: "Tracer::Resource",
            id: "document",
          },
        });

        assert.false(authorized.ok, "the SDK rejects the invalid entity");
        if (!authorized.ok) {
          assert.strictEqual(authorized.error.code, "INPUT_REQUIRED");
          assert.strictEqual(
            authorized.error.operation,
            "authorizeUnsigned",
          );
          assert.deepEqual(authorized.error.path, [
            "principal",
            "type",
          ]);
        }
      },
    );
  });
}
