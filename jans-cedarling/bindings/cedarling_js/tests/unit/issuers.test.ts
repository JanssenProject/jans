import type QUnitApi from "qunit";

import { createClientForEngine } from "../../dist/client/client.js";
import { createTestEngine } from "./engine-fixture.js";

/** Registers issuer readiness validation and observation unit tests. */
export default function registerIssuerUnitTests(QUnit: QUnitApi): void {
  QUnit.module("issuers");

  QUnit.test("validates exact-one references before observing the engine", async (assert) => {
    let calls = 0;
    const client = createClientForEngine(createTestEngine({
      async isIssuerLoaded() {
        calls += 1;
        return false;
      },
    }));

    for (const reference of [
      {},
      { id: "issuer", iss: "https://issuer.example" },
      { id: "" },
      { iss: " " },
      { id: "issuer", extra: true },
    ]) {
      const result = await client.issuers.isLoaded(reference as never);
      assert.false(result.ok);
      if (!result.ok) {
        assert.strictEqual(result.error.code, "INVALID_INPUT");
      }
    }
    assert.strictEqual(calls, 0);
    await client.close();
  });

  QUnit.test("closed issuer observations do not inspect caller input", async (assert) => {
    const client = createClientForEngine(createTestEngine());
    await client.close();
    let inspections = 0;
    const reference = new Proxy({ id: "issuer" }, {
      getPrototypeOf(target) {
        inspections += 1;
        return Reflect.getPrototypeOf(target);
      },
    });

    const result = await client.issuers.isLoaded(reference);
    assert.false(result.ok);
    if (!result.ok) {
      assert.strictEqual(result.error.code, "CLIENT_CLOSED");
    }
    assert.strictEqual(inspections, 0);
  });

  QUnit.test("normalizes opaque issuer failures without retaining secrets", async (assert) => {
    const secret = "raw-issuer-observation-secret";
    const client = createClientForEngine(createTestEngine({
      async isIssuerLoaded() {
        throw new Error(secret);
      },
    }));

    const result = await client.issuers.isLoaded({ id: "issuer" });
    assert.false(result.ok);
    if (!result.ok) {
      assert.strictEqual(result.error.code, "ISSUER_OPERATION_FAILED");
      assert.false(JSON.stringify(result.error).includes(secret));
    }
    await client.close();
  });
}
