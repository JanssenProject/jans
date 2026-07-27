import type QUnitApi from "qunit";
import { createCedarling } from "@janssenproject/cedarling";
import {
  tracerExtensionPolicyStore,
  tracerPolicyStore,
} from "../fixtures/tracer-policy-store.js";

/** Registers the public unsigned-authorization contract against real WASM. */
export default function registerUnsignedAuthorizationTests(
  QUnit: QUnitApi,
): void {
  QUnit.module("authorize-unsigned");

  /** Creates one isolated real-WASM client for an unsigned behavior probe. */
  async function createClient(applicationName: string) {
    return await createCedarling({
      applicationName,
      policyStore: {
        type: "inline",
        document: tracerPolicyStore,
      },
    });
  }

  QUnit.test("returns allow and deny as successful decisions", async (assert) => {
    const created = await createClient("cedarling-js-unsigned-decisions");

    assert.true(created.ok, "the real WASM client initializes");
    if (!created.ok) {
      return;
    }

    const principal = {
      type: "Tracer::User",
      id: "alice",
    };
    const resource = {
      type: "Tracer::Resource",
      id: "document",
    };

    try {
      const allowed = await created.value.authorizeUnsigned({
        principal,
        action: 'Tracer::Action::"Read"',
        resource,
      });
      const denied = await created.value.authorizeUnsigned({
        principal,
        action: 'Tracer::Action::"Deny"',
        resource,
      });

      assert.true(allowed.ok, "allow is a successful authorization");
      if (allowed.ok) {
        assert.true(allowed.value.decision);
        assert.true(allowed.allowed, "the flat allow shortcut is true");
        assert.false(allowed.denied, "an allowed decision is not denied");
        assert.strictEqual(allowed.error, undefined, "successful results have no error");
        assert.deepEqual(allowed.value.diagnostics.reasons, ["allow"]);
      }
      assert.true(denied.ok, "deny is not an operational failure");
      if (denied.ok) {
        assert.false(denied.value.decision);
        assert.false(denied.allowed, "the flat allow shortcut is false");
        assert.true(denied.denied, "the flat deny shortcut is true");
        assert.deepEqual(denied.value.diagnostics.reasons, []);
      }
    } finally {
      assert.true((await created.value.close()).ok);
    }
  });

  QUnit.test("accepts a structured Cedar action without exposing UID syntax", async (assert) => {
    const created = await createClient("cedarling-js-structured-action");

    assert.true(created.ok, "the real WASM client initializes");
    if (!created.ok) {
      return;
    }

    try {
      const authorized = await created.value.authorizeUnsigned({
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
    } finally {
      assert.true((await created.value.close()).ok);
    }
  });

  QUnit.test("omitted principals preserve partial evaluation and fail closed", async (assert) => {
    const created = await createClient("cedarling-js-unsigned-partial");

    assert.true(created.ok, "the real WASM client initializes");
    if (!created.ok) {
      return;
    }

    const resource = {
      type: "Tracer::Resource",
      id: "document",
    };

    try {
      const publicResult = await created.value.authorizeUnsigned({
        action: 'Tracer::Action::"Public"',
        resource,
      });
      const protectedResult = await created.value.authorizeUnsigned({
        action: 'Tracer::Action::"Protected"',
        resource,
      });

      assert.true(publicResult.ok, "principal-independent policy resolves");
      if (publicResult.ok) {
        assert.true(publicResult.value.decision);
        assert.deepEqual(publicResult.value.diagnostics.reasons, ["public"]);
      }
      assert.true(protectedResult.ok, "a residual is a decision, not an error");
      if (protectedResult.ok) {
        assert.false(protectedResult.value.decision);
        assert.true(
          protectedResult.value.diagnostics.reasons.includes("residual"),
          "the residual policy ID is preserved",
        );
      }
    } finally {
      assert.true((await created.value.close()).ok);
    }
  });

  QUnit.test("entity attributes are converted from a detached request snapshot", async (assert) => {
    const created = await createClient("cedarling-js-unsigned-entities");

    assert.true(created.ok, "the real WASM client initializes");
    if (!created.ok) {
      return;
    }

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

    try {
      const pending = created.value.authorizeUnsigned({
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
    } finally {
      assert.true((await created.value.close()).ok);
    }
  });

  QUnit.test("canonical Cedar extension markers reach the engine unchanged", async (assert) => {
    const created = await createCedarling({
      applicationName: "cedarling-js-unsigned-extension",
      authorization: {
        dangerouslyDisableSchemaValidation: true,
      },
      policyStore: {
        type: "inline",
        document: tracerExtensionPolicyStore,
      },
    });

    assert.true(created.ok, "the schemaless real-WASM client initializes");
    if (!created.ok) {
      return;
    }

    try {
      const authorized = await created.value.authorizeUnsigned({
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
    } finally {
      assert.true((await created.value.close()).ok);
    }
  });

  QUnit.test("an attribute-free resource resolves its policy-store default entity", async (assert) => {
    const created = await createClient("cedarling-js-unsigned-default-entity");

    assert.true(created.ok, "the real WASM client initializes");
    if (!created.ok) {
      return;
    }

    try {
      const authorized = await created.value.authorizeUnsigned({
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
    } finally {
      assert.true((await created.value.close()).ok);
    }
  });

  QUnit.test("rejects a whitespace-only entity type before authorization", async (assert) => {
    const created = await createClient("cedarling-js-unsigned-validation");

    assert.true(created.ok, "the real WASM client initializes");
    if (!created.ok) {
      return;
    }

    try {
      const authorized = await created.value.authorizeUnsigned({
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
        assert.strictEqual(authorized.error.code, "INVALID_INPUT");
        assert.strictEqual(
          authorized.error.operation,
          "authorizeUnsigned",
        );
        assert.deepEqual(authorized.error.issues?.[0]?.path, [
          "principal",
          "type",
        ]);
        assert.strictEqual(
          authorized.error.issues?.[0]?.code,
          "required",
        );
      }
    } finally {
      const closed = await created.value.close();
      assert.true(closed.ok, "the client closes");
    }
  });

  QUnit.test("a closed client rejects unsigned work without inspecting it", async (assert) => {
    const created = await createClient("cedarling-js-unsigned-closed");

    assert.true(created.ok, "the real WASM client initializes");
    if (!created.ok) {
      return;
    }

    assert.true((await created.value.close()).ok, "the client closes");

    let reads = 0;
    const request = Object.defineProperty({}, "action", {
      enumerable: true,
      get() {
        reads += 1;
        return 'Tracer::Action::"Read"';
      },
    });
    const authorized = await created.value.authorizeUnsigned(
      request as never,
    );

    assert.false(authorized.ok);
    if (!authorized.ok) {
      assert.strictEqual(authorized.error.code, "CLIENT_CLOSED");
      assert.strictEqual(
        authorized.error.operation,
        "authorizeUnsigned",
      );
    }
    assert.strictEqual(reads, 0, "closed work does not inspect caller input");
  });
}
