import type QUnitApi from "qunit";

import {
  snapshotCedarContextValue,
  snapshotCedarValue,
  snapshotContextDataValue,
} from "../../dist/values/snapshot.js";

/** Registers common detached-value unit tests. */
export default function registerCommonValueTests(QUnit: QUnitApi): void {
  // Registration begins only after the host runner configures QUnit.
  QUnit.module("common-values");

  QUnit.test("entity values are detached and decimals stay explicit", (assert) => {
    const source = {
      score: {
        __extn: {
          fn: "decimal" as const,
          arg: "1.23456",
        },
      },
      labels: ["initial"],
    };

    const snapshot = snapshotCedarValue(source);

    source.score.__extn.arg = "9.0";
    source.labels[0] = "mutated";

    assert.deepEqual(snapshot, {
      score: {
        __extn: {
          fn: "decimal",
          arg: "1.23456",
        },
      },
      labels: ["initial"],
    });
  });

  QUnit.test("non-data and lossy JavaScript structures are rejected", (assert) => {
    const cycle: Record<string, unknown> = {};
    cycle.self = cycle;

    const sparse = new Array<unknown>(1);

    let getterInvoked = false;
    const accessor = {};
    Object.defineProperty(accessor, "secret", {
      enumerable: true,
      get() {
        getterInvoked = true;
        return "secret";
      },
    });

    const customPrototype = Object.create({ inherited: true }) as Record<
      string,
      unknown
    >;
    customPrototype.value = true;

    const symbolProperty = {
      value: true,
      [Symbol("hidden")]: "hidden",
    };

    const nonEnumerableProperty = { value: true };
    Object.defineProperty(nonEnumerableProperty, "hidden", {
      value: "hidden",
    });

    const invalidValues: readonly unknown[] = [
      null,
      undefined,
      () => undefined,
      Symbol("value"),
      1n,
      Number.NaN,
      Number.POSITIVE_INFINITY,
      Number.MAX_SAFE_INTEGER + 1,
      cycle,
      sparse,
      accessor,
      customPrototype,
      symbolProperty,
      nonEnumerableProperty,
      new Uint8Array(0),
      new ArrayBuffer(0),
      new Date(),
      /regex/,
      new Map(),
      new Set(),
      Promise.resolve(),
    ];

    for (const [index, invalid] of invalidValues.entries()) {
      let thrown: unknown;
      try {
        snapshotCedarValue(invalid);
      } catch (error: unknown) {
        thrown = error;
      }

      assert.true(
        thrown instanceof TypeError,
        `invalid value ${index + 1} rejects with a TypeError subclass`,
      );
      assert.true(
        thrown instanceof Error,
        `invalid value ${index + 1} rejects with an Error subclass`,
      );
    }

    assert.false(getterInvoked, "accessors are rejected without invocation");
  });

  QUnit.test("request context requires integers or exact extension markers", (assert) => {
    const extension = {
      __extn: {
        fn: "decimal",
        arg: "1.2346",
      },
    };

    const snapshot = snapshotCedarContextValue({
      count: 4,
      price: extension,
    });

    extension.__extn.arg = "mutated";

    assert.deepEqual(snapshot, {
      count: 4,
      price: {
        __extn: {
          fn: "decimal",
          arg: "1.2346",
        },
      },
    });

    const invalidValues: readonly unknown[] = [
      1.5,
      null,
      { __extn: { fn: "unknown", arg: "value" } },
      { __extn: { fn: "ip", arg: "" } },
      { __extn: { fn: "ip", arg: "192.0.2.1", extra: true } },
      { __extn: { fn: "ip", arg: "192.0.2.1" }, extra: true },
      { __extn: "192.0.2.1" },
    ];

    for (const [index, invalid] of invalidValues.entries()) {
      let thrown: unknown;
      try {
        snapshotCedarContextValue(invalid);
      } catch (error: unknown) {
        thrown = error;
      }

      assert.true(
        thrown instanceof TypeError,
        `invalid context value ${index + 1} rejects with a TypeError subclass`,
      );
      assert.true(
        thrown instanceof Error,
        `invalid context value ${index + 1} rejects with an Error subclass`,
      );
    }
  });

  QUnit.test("extension marker fields are independent of insertion order", (assert) => {
    const snapshot = snapshotCedarContextValue({
      __extn: {
        arg: "192.0.2.1",
        fn: "ip",
      },
    });

    assert.deepEqual(snapshot, {
      __extn: {
        fn: "ip",
        arg: "192.0.2.1",
      },
    });
  });

  QUnit.test("context data uses the same Cedar representation as authorization", (assert) => {
    const source = {
      values: [
        1,
        { __extn: { fn: "decimal" as const, arg: "1.25" } },
      ],
    };

    const snapshot = snapshotContextDataValue(source);
    source.values[0] = 9;

    assert.deepEqual(snapshot, {
      values: [1, { __extn: { fn: "decimal", arg: "1.25" } }],
    });
    assert.throws(() => snapshotContextDataValue(null), TypeError);
    assert.throws(
      () => snapshotContextDataValue({ values: [null] }),
      TypeError,
    );
    assert.throws(
      () => snapshotContextDataValue({ score: 1.25 }),
      TypeError,
    );
  });
  // The suite module performs no host startup or exit handling.
}
