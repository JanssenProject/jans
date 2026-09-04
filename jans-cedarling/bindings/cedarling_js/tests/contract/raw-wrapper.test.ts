import { init } from "@janssenproject/cedarling";
import type QUnitApi from "qunit";

import { tracerPolicyStore } from "../fixtures/tracer-policy-store.js";

const properties = {
  CEDARLING_APPLICATION_NAME: "raw-wrapper-contract",
  CEDARLING_POLICY_STORE_LOCAL: JSON.stringify(tracerPolicyStore),
  CEDARLING_LOG_TYPE: "memory",
  CEDARLING_LOG_TTL: 120,
  CEDARLING_LOG_LEVEL: "INFO",
  CEDARLING_JWT_SIG_VALIDATION: "disabled",
  CEDARLING_JWT_STATUS_VALIDATION: "disabled",
};

const request = JSON.stringify({
  principal: {
    cedar_entity_mapping: { entity_type: "Tracer::User", id: "alice" },
  },
  action: 'Tracer::Action::"Read"',
  resource: {
    cedar_entity_mapping: { entity_type: "Tracer::Resource", id: "document" },
  },
  context: {},
});

function containsGeneratedValue(value: unknown, visited = new Set<object>()): boolean {
  if (typeof value !== "object" || value === null || visited.has(value)) return false;
  visited.add(value);
  if ("free" in value || Symbol.dispose in value) return true;
  return Reflect.ownKeys(value).some((key) =>
    containsGeneratedValue(Reflect.get(value, key), visited)
  );
}

export default function registerRawWrapperContractTests(QUnit: QUnitApi): void {
  QUnit.module("raw wrapper contract");

  QUnit.test("initializes from raw properties and returns plain generated data", async (assert) => {
    const cedarling = await init(properties);
    try {
      const result = await cedarling.authorizeUnsigned(request);
      assert.true(result.decision, "the raw generated request authorizes");
      assert.strictEqual(result.request_id.length > 0, true, "a generated request id is retained");
      assert.deepEqual(result.response.diagnostics.reason, ["allow"]);
      assert.false(containsGeneratedValue(result), "authorization data contains no generated resource");

      cedarling.pushDataCtx("session", { role: "editor" }, 60n);
      const value = cedarling.getDataCtx("session") as { role?: string };
      assert.strictEqual(value.role, "editor", "raw context input reaches the generated store");
      const entry = cedarling.getDataEntryCtx("session");
      assert.ok(entry, "entry metadata is returned as plain data");
      assert.false(containsGeneratedValue(entry), "entry data contains no generated resource");
      const stats = cedarling.getStatsCtx();
      assert.false(containsGeneratedValue(stats), "stats contain no generated resource");
      assert.true(cedarling.removeDataCtx("session"));
    } finally {
      await cedarling.shutDown();
    }
  });
}
