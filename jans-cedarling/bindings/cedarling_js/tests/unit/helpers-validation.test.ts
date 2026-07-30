import type QUnitApi from "qunit";

import {
  createInputValidator,
  isSafeIntegerInRange,
} from "../../dist/helpers/validation.js";

/** Registers direct tests for the shared input-validation Interface. */
export default function registerHelperValidationTests(QUnit: QUnitApi): void {
  QUnit.module("helpers-validation");

  QUnit.test("record policy remains explicit per feature", (assert) => {
    const strict = createInputValidator("strict");
    const permissive = createInputValidator("permissive", {
      allowNullPrototype: true,
    });
    const nullPrototype = Object.create(null) as Record<string, unknown>;

    assert.throws(() => strict.record(nullPrototype, []), TypeError);
    assert.strictEqual(permissive.record(nullPrototype, []), nullPrototype);
  });

  QUnit.test("field inspection never invokes accessors", (assert) => {
    const input = createInputValidator("field");
    let invoked = false;
    const source = {};
    Object.defineProperty(source, "value", {
      enumerable: true,
      get() {
        invoked = true;
        return "secret";
      },
    });

    assert.throws(() => input.field(source, "value", []), TypeError);
    assert.false(invoked);
  });

  QUnit.test("exact fields report the rejected field path", (assert) => {
    const input = createInputValidator("fields");

    assert.throws(
      () => input.exactFields({ known: true, extra: true }, ["known"], ["root"]),
      (error: unknown) =>
        JSON.stringify(
          (error as { issues?: readonly [{ path?: unknown }] }).issues?.[0]?.path,
        ) === JSON.stringify(["root", "extra"]),
    );
  });

  QUnit.test("string normalization preserves each feature contract", (assert) => {
    const preserving = createInputValidator("preserve");
    const trimming = createInputValidator("trim", {
      stringNormalization: "trim",
    });

    assert.strictEqual(preserving.requiredString(" value ", []), " value ");
    assert.strictEqual(trimming.requiredString(" value ", []), "value");
    assert.strictEqual(
      preserving.requiredString("   ", [], { empty: "empty" }),
      "   ",
      "non-empty log identifiers preserve the existing whitespace behavior",
    );
    assert.throws(() => preserving.requiredString("   ", []), TypeError);
  });

  QUnit.test("safe integer checks share inclusive range behavior", (assert) => {
    assert.true(isSafeIntegerInRange(1, 1, 3));
    assert.true(isSafeIntegerInRange(3, 1, 3));
    assert.false(isSafeIntegerInRange(0, 1, 3));
    assert.false(isSafeIntegerInRange(1.5, 1, 3));
    assert.false(isSafeIntegerInRange(Number.MAX_SAFE_INTEGER + 1, 1, Infinity));
  });
}
