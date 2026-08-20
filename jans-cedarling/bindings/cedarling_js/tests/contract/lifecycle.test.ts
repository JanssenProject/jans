import type QUnitApi from "qunit";

import { createCedarling } from "@janssenproject/cedarling";
import { tracerPolicyStore } from "../fixtures/tracer-policy-store.js";

export default function registerLifecycleTests(QUnit: QUnitApi): void {
  QUnit.module("lifecycle");

  QUnit.test("shutdown is idempotent and closes every service", async (assert) => {
    const created = await createCedarling({
      applicationName: "lifecycle-closed",
      logging: { type: "memory", level: "trace" },
      policyStore: { type: "inline", document: tracerPolicyStore },
    });
    assert.true(created.ok);
    if (!created.ok) return;

    const first = created.value.shutDown();
    assert.strictEqual(created.value.shutDown(), first);
    assert.true((await first).ok);
    assert.strictEqual(created.value.shutDown(), first);

    let inspections = 0;
    const hostile = new Proxy({}, {
      get() { inspections += 1; },
      getPrototypeOf(target) {
        inspections += 1;
        return Reflect.getPrototypeOf(target);
      },
      ownKeys() { inspections += 1; return []; },
      getOwnPropertyDescriptor() { inspections += 1; return undefined; },
    });
    const operations = [
      ["authorizeUnsigned", () => created.value.authorizeUnsigned(hostile as never)],
      ["authorizeMultiIssuer", () => created.value.authorizeMultiIssuer(hostile as never)],
      ["context.set", () => created.value.context.set("fact", hostile as never)],
      ["context.get", () => created.value.context.get(hostile as never)],
      ["context.getEntry", () => created.value.context.getEntry(hostile as never)],
      ["context.delete", () => created.value.context.delete(hostile as never)],
      ["context.clear", () => created.value.context.clear()],
      ["context.entries", () => created.value.context.entries()],
      ["context.stats", () => created.value.context.stats()],
      ["issuers.isLoaded", () => created.value.issuers.isLoaded(hostile as never)],
      ["logs.find", () => created.value.logs.find(hostile as never)],
      ["logs.ids", () => created.value.logs.ids()],
      ["logs.drain", () => created.value.logs.drain()],
    ] as const;

    for (const [operation, run] of operations) {
      const result = await run();
      assert.false(result.ok, operation);
      if (!result.ok) {
        assert.strictEqual(result.error.code, "CLIENT_CLOSED", operation);
        assert.strictEqual(result.error.operation, operation, operation);
      }
    }
    assert.strictEqual(inspections, 0, "closed work never inspects caller input");
  });
}
