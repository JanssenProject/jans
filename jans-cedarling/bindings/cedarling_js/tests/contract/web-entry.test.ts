import type QUnitApi from "qunit";

import { createCedarling } from "../../dist/index.js";
import { tracerPolicyStore } from "../fixtures/tracer-policy-store.js";

/**
 * Focused coverage for the browser/web entry (`dist/index.js`) inside
 * Node-family runners.
 *
 * The package root resolves to the Node entry in these hosts, so the
 * contract group would otherwise never exercise the web engine. The runners
 * install a `fetch(file:)` shim, modeling the host asset-delivery contract
 * the web engine requires outside a bundler (import maps, static servers).
 */
export default function registerWebEntryTests(QUnit: QUnitApi): void {
  QUnit.module("web-entry");

  QUnit.test("the web entry initializes and authorizes through the generated Web loader", async (assert) => {
    const created = await createCedarling({
      applicationName: "web-entry-contract",
      policyStore: { type: "inline", document: tracerPolicyStore },
    });

    assert.true(created.ok);
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
        assert.true(authorized.value.requestId.length > 0);
      }
    } finally {
      const closed = await created.value.shutDown();
      assert.true(closed.ok);
    }
  });
}
