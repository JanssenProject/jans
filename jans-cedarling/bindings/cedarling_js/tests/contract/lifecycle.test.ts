import type QUnitApi from "qunit";

import { createCedarling } from "@janssenproject/cedarling";
import { tracerPolicyStore } from "../fixtures/tracer-policy-store.js";

/**
 * Registers public client lifecycle contracts through real WASM.
 *
 * The `close`-after-failed-`close` idempotency path is exercised only in
 * `tests/unit/lifecycle.test.ts` because the public contract API offers no
 * way to inject a shutdown failure into the generated WASM client. That
 * unit test owns the regression coverage for failure-state close behavior.
 */
export default function registerLifecycleTests(QUnit: QUnitApi): void {
  QUnit.module("lifecycle");

  QUnit.test("authorization after close returns CLIENT_CLOSED", async (assert) => {
    const created = await createCedarling({
      applicationName: "lifecycle-closed",
      policyStore: { type: "inline", document: tracerPolicyStore },
    });
    assert.true(created.ok);
    if (!created.ok) {
      return;
    }

    const firstClose = created.value.close();
    const secondClose = created.value.close();
    assert.strictEqual(
      firstClose,
      secondClose,
      "public concurrent close calls share one promise",
    );
    const closed = await firstClose;
    assert.strictEqual(
      created.value.close(),
      firstClose,
      "repeated close remains idempotent after settlement",
    );
    const authorized = await created.value.authorizeUnsigned({
      principal: { type: "Tracer::User", id: "alice" },
      action: 'Tracer::Action::"Read"',
      resource: { type: "Tracer::Resource", id: "document" },
    });

    assert.true(closed.ok);
    assert.false(authorized.ok);
    if (!authorized.ok) {
      assert.strictEqual(authorized.error.code, "CLIENT_CLOSED");
      assert.strictEqual(authorized.error.operation, "authorizeUnsigned");
    }
  });

  QUnit.test("closed dispatch rejects without inspecting its envelope", async (assert) => {
    const created = await createCedarling({
      applicationName: "lifecycle-closed-dispatch",
      policyStore: { type: "inline", document: tracerPolicyStore },
    });
    assert.true(created.ok);
    if (!created.ok) {
      return;
    }
    assert.true((await created.value.close()).ok);

    let inspections = 0;
    const envelope = new Proxy(
      {
        type: "unsigned",
        request: {
          action: 'Tracer::Action::"Read"',
          resource: { type: "Tracer::Resource", id: "document" },
        },
      } as const,
      {
        getPrototypeOf(target) {
          inspections += 1;
          return Reflect.getPrototypeOf(target);
        },
        getOwnPropertyDescriptor(target, key) {
          inspections += 1;
          return Reflect.getOwnPropertyDescriptor(target, key);
        },
      },
    );

    const authorized = await created.value.authorize(envelope);

    assert.false(authorized.ok);
    if (!authorized.ok) {
      assert.strictEqual(authorized.error.code, "CLIENT_CLOSED");
      assert.strictEqual(authorized.error.operation, "authorize");
    }
    assert.strictEqual(inspections, 0, "the envelope is not inspected");
  });
}
