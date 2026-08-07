import type QUnitApi from "qunit";
import * as cedarling from "@janssenproject/cedarling";

/** Registers package-root runtime export contract tests. */
export default function registerExportTests(QUnit: QUnitApi): void {
  QUnit.module("exports");

  QUnit.test("only the public factory is exported as a runtime value", (assert) => {
    assert.deepEqual(Object.keys(cedarling), ["createCedarling"]);
    assert.strictEqual(
      typeof cedarling.createCedarling,
      "function",
      "the public factory is callable",
    );
    for (const generatedName of [
      "AuthorizeResult",
      "Cedarling",
      "InitOutput",
      "default",
      "init",
      "initSync",
      "memory",
    ]) {
      assert.false(
        Object.hasOwn(cedarling, generatedName),
        `${generatedName} remains private`,
      );
    }
  });
}
