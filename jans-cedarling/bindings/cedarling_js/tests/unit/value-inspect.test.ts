import type QUnitApi from "qunit";

import {
  inspectOwnProperty,
  inspectPropertyDescriptor,
  isObjectRecord,
  isPlainDataRecord,
  ownDataProperty,
  ownEnumerableDataProperty,
  ownEnumerableStringKeys,
} from "../../dist/helpers/records.js";

/** Registers direct unit tests for the neutral value-inspection primitives. */
export default function registerValueInspectTests(QUnit: QUnitApi): void {
  QUnit.module("value-inspect");

  QUnit.test("isObjectRecord accepts adapter records without requiring a plain prototype", (assert) => {
    class AdapterRecord {
      public readonly value = 1;
    }

    assert.true(isObjectRecord({ value: 1 }));
    assert.true(isObjectRecord(new AdapterRecord()));
    assert.false(isObjectRecord([]));
    assert.false(isObjectRecord(null));
  });

  QUnit.test("isPlainDataRecord accepts ordinary and null-prototype records", (assert) => {
    assert.true(isPlainDataRecord({}, false));
    assert.true(isPlainDataRecord({ value: 1 }, false));
    assert.true(isPlainDataRecord(Object.create(null), true));
  });

  QUnit.test("isPlainDataRecord rejects non-object primitives", (assert) => {
    const nonObjects: readonly unknown[] = [
      null,
      undefined,
      0,
      1,
      Number.NaN,
      "",
      "value",
      false,
      true,
      0n,
      1n,
      Symbol("value"),
      () => undefined,
    ];

    for (const [index, candidate] of nonObjects.entries()) {
      assert.false(
        isPlainDataRecord(candidate, false),
        `non-object ${index + 1} is rejected`,
      );
      assert.false(
        isPlainDataRecord(candidate, true),
        `non-object ${index + 1} is rejected even with null-prototype allowance`,
      );
    }
  });

  QUnit.test("isPlainDataRecord rejects arrays, class instances, and built-ins", (assert) => {
    class Sample {
      public readonly value = 1;
    }

    const customPrototype = Object.create({ inherited: true }) as Record<
      string,
      unknown
    >;
    customPrototype.value = true;

    const rejected: readonly unknown[] = [
      [],
      [1, 2],
      new Map(),
      new Set(),
      new Date(),
      /regex/,
      new Uint8Array(0),
      new ArrayBuffer(0),
      new Sample(),
      Promise.resolve(),
      customPrototype,
    ];

    for (const [index, candidate] of rejected.entries()) {
      assert.false(
        isPlainDataRecord(candidate, false),
        `non-plain object ${index + 1} is rejected`,
      );
      assert.false(
        isPlainDataRecord(candidate, true),
        `non-plain object ${index + 1} is rejected even with null-prototype allowance`,
      );
    }
  });

  QUnit.test("isPlainDataRecord honors the null-prototype allowance flag", (assert) => {
    const nullPrototype = Object.create(null) as Record<string, unknown>;
    nullPrototype.value = 1;

    assert.false(
      isPlainDataRecord(nullPrototype, false),
      "null-prototype records require explicit allowance",
    );
    assert.true(
      isPlainDataRecord(nullPrototype, true),
      "null-prototype records are accepted only when allowed",
    );
  });

  QUnit.test("inspectPropertyDescriptor classifies missing descriptors", (assert) => {
    const inspection = inspectPropertyDescriptor(undefined);
    assert.deepEqual(inspection, { kind: "missing" });
  });

  QUnit.test("inspectPropertyDescriptor classifies data descriptors", (assert) => {
    const enumerable = inspectPropertyDescriptor({
      value: 42,
      writable: true,
      enumerable: true,
      configurable: true,
    });
    assert.deepEqual(enumerable, {
      kind: "data",
      enumerable: true,
      value: 42,
    });

    const hidden = inspectPropertyDescriptor({
      value: "hidden",
      writable: false,
      enumerable: false,
      configurable: false,
    });
    assert.deepEqual(hidden, {
      kind: "data",
      enumerable: false,
      value: "hidden",
    });
  });

  QUnit.test("inspectPropertyDescriptor classifies accessor descriptors without invoking them", (assert) => {
    let getterInvoked = false;
    let setterInvoked = false;

    const inspection = inspectPropertyDescriptor({
      get() {
        getterInvoked = true;
        return "value";
      },
      set() {
        setterInvoked = true;
      },
      enumerable: true,
      configurable: true,
    });

    assert.deepEqual(inspection, { kind: "accessor", enumerable: true });
    assert.false(getterInvoked, "getter is never invoked");
    assert.false(setterInvoked, "setter is never invoked");
  });

  QUnit.test("inspectPropertyDescriptor treats absent enumerable flag as non-enumerable", (assert) => {
    const inspection = inspectPropertyDescriptor({
      get() {
        return "value";
      },
      configurable: true,
    });
    assert.deepEqual(inspection, { kind: "accessor", enumerable: false });
  });

  QUnit.test("inspectOwnProperty reports missing keys without touching the value", (assert) => {
    const value: Record<string, unknown> = { present: 1 };
    const inspection = inspectOwnProperty(value, "absent");
    assert.deepEqual(inspection, { kind: "missing" });
  });

  QUnit.test("inspectOwnProperty reads data properties without invoking accessors", (assert) => {
    let getterInvoked = false;
    const value: Record<string, unknown> = {};
    Object.defineProperty(value, "data", {
      value: 7,
      enumerable: true,
    });
    Object.defineProperty(value, "secret", {
      enumerable: false,
      get() {
        getterInvoked = true;
        return "secret";
      },
    });

    const data = inspectOwnProperty(value, "data");
    assert.deepEqual(data, { kind: "data", enumerable: true, value: 7 });

    const accessor = inspectOwnProperty(value, "secret");
    assert.deepEqual(accessor, { kind: "accessor", enumerable: false });
    assert.false(getterInvoked, "accessor is never invoked while inspecting");
  });

  QUnit.test("ownEnumerableStringKeys follows descriptors without invoking accessors", (assert) => {
    let getterInvoked = false;
    const symbol = Symbol("symbol");
    const value: Record<PropertyKey, unknown> = {
      first: 1,
      [symbol]: "symbol",
    };
    Object.defineProperty(value, "computed", {
      enumerable: true,
      get() {
        getterInvoked = true;
        return "computed";
      },
    });
    Object.defineProperty(value, "hidden", {
      enumerable: false,
      value: "hidden",
    });

    assert.deepEqual(
      ownEnumerableStringKeys(value),
      ["first", "computed"],
      "only own enumerable string keys are returned in property order",
    );
    assert.false(
      getterInvoked,
      "enumerating descriptors does not invoke accessors",
    );
  });

  QUnit.test("own data-property readers preserve enumerability policy", (assert) => {
    const value = {};
    Object.defineProperty(value, "visible", {
      value: 1,
      enumerable: true,
    });
    Object.defineProperty(value, "hidden", {
      value: 2,
      enumerable: false,
    });

    assert.strictEqual(ownDataProperty(value, "visible"), 1);
    assert.strictEqual(ownDataProperty(value, "hidden"), 2);
    assert.strictEqual(ownEnumerableDataProperty(value, "visible"), 1);
    assert.strictEqual(ownEnumerableDataProperty(value, "hidden"), undefined);
  });
}
