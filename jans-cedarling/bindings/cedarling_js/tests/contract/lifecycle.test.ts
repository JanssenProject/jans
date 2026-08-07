import type QUnitApi from "qunit";

import { createCedarling } from "@janssenproject/cedarling";
import { tracerPolicyStore } from "../fixtures/tracer-policy-store.js";

/**
 * Registers public client lifecycle contracts through real WASM.
 *
 * The failed-`shutDown` idempotency path is exercised only in
 * `tests/unit/lifecycle.test.ts` because the public contract API offers no
 * way to inject a shutdown failure into the generated WASM client. That
 * unit test owns the regression coverage for failure-state shutdown behavior.
 */
export default function registerLifecycleTests(QUnit: QUnitApi): void {
  QUnit.module("lifecycle");

  QUnit.test("authorization after shutDown returns CLIENT_CLOSED", async (assert) => {
    const created = await createCedarling({
      applicationName: "lifecycle-closed",
      policyStore: { type: "inline", document: tracerPolicyStore },
    });
    assert.true(created.ok);
    if (!created.ok) {
      return;
    }

    assert.false("close" in created.value, "the legacy close method is absent");
    const firstShutdown = created.value.shutDown();
    const secondShutdown = created.value.shutDown();
    assert.strictEqual(
      firstShutdown,
      secondShutdown,
      "public concurrent shutdown calls share one promise",
    );
    const shutDown = await firstShutdown;
    assert.strictEqual(
      created.value.shutDown(),
      firstShutdown,
      "repeated shutdown remains idempotent after settlement",
    );
    const authorized = await created.value.authorizeUnsigned({
      principal: { type: "Tracer::User", id: "alice" },
      action: 'Tracer::Action::"Read"',
      resource: { type: "Tracer::Resource", id: "document" },
    });

    assert.true(shutDown.ok);
    assert.false(authorized.ok);
    if (!authorized.ok) {
      assert.strictEqual(authorized.error.code, "CLIENT_CLOSED");
      assert.strictEqual(authorized.error.operation, "authorizeUnsigned");
    }
  });
}
