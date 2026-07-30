import type QUnitApi from "qunit";
import { createCedarling } from "@janssenproject/cedarling";
import { tracerPolicyStore } from "../fixtures/tracer-policy-store.js";

/** Detects generated ownership and representation values recursively. */
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
    containsGeneratedSurface(Reflect.get(value, key), visited),
  );
}

/** Registers the first public real-WASM authorization contract. */
export default function registerWebTracerTests(QUnit: QUnitApi): void {
  QUnit.module("web-tracer");

  QUnit.test("an unsigned decision reaches the real WASM engine", async (assert) => {
    const created = await createCedarling({
      applicationName: "cedarling-js-tracer",
      policyStore: {
        type: "inline",
        document: tracerPolicyStore,
      },
    });

    assert.true(created.ok, "the client initializes");
    if (!created.ok) {
      return;
    }

    try {
      const authorized = await created.value.authorizeUnsigned({
        principal: {
          type: "Tracer::User",
          id: "alice",
        },
        action: 'Tracer::Action::"Read"',
        resource: {
          type: "Tracer::Resource",
          id: "document",
        },
      });

      assert.true(authorized.ok, "authorization succeeds");
      if (authorized.ok) {
        assert.true(authorized.value.decision, "the policy allows the request");
        assert.true(
          authorized.value.requestId.length > 0,
          "a request ID is returned",
        );
        assert.deepEqual(authorized.value.diagnostics.reasons, ["allow"]);
        assert.deepEqual(authorized.value.diagnostics.errors, []);
        assert.propEqual(
          structuredClone(authorized.value),
          authorized.value,
          "the decision is ordinary structured-cloneable data",
        );
        assert.strictEqual(
          Object.getPrototypeOf(authorized.value),
          Object.prototype,
          "the decision has a plain object prototype",
        );
        assert.false(
          containsGeneratedSurface(authorized.value),
          "no wrapper, disposal hook, memory, Map, or bigint escapes",
        );
        assert.strictEqual(
          typeof JSON.stringify(authorized.value),
          "string",
          "the decision uses normal JSON-compatible data",
        );
      }
    } finally {
      const closed = await created.value.shutDown();
      assert.true(closed.ok, "the client closes");
    }
  });
}
