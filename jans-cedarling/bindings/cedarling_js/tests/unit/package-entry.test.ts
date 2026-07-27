import type QUnitApi from "qunit";
import * as cedarling from "@janssenproject/cedarling";

/** Registers the built package-entry smoke test. */
export default function registerPackageEntryTests(QUnit: QUnitApi): void {
  QUnit.module("package entry");

  QUnit.test("the built ESM package root can be imported", (assert) => {
    assert.strictEqual(typeof cedarling, "object");
    assert.strictEqual(
      typeof cedarling.createCedarling,
      "function",
      "the public factory is callable",
    );
  });
}
