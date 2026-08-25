import type QUnitApi from "qunit";

import {
  inspectDenseArray,
  inspectOwnProperty,
  isPlainDataRecord,
  ownDataProperty,
  ownEnumerableDataProperty,
  ownEnumerableStringKeys,
} from "../../.build/helpers/records.js";
import {
  snapshotCedarObject,
  snapshotCedarValue,
  snapshotJsonObject,
  snapshotJsonValue,
} from "../../.build/values/snapshot.js";

function rejectionCode(work: () => unknown): unknown {
  try {
    work();
  } catch (error: unknown) {
    return typeof error === "object" && error !== null
      ? ownDataProperty(error, "code")
      : error;
  }
  return "NO_ERROR";
}

export default async function registerCommonValueTests(QUnit: QUnitApi): Promise<void> {
  await Promise.resolve();
  QUnit.module("common-values");

  QUnit.test("plain-record inspection honors null prototypes", (assert) => {
    const nullPrototype = Object.create(null) as Record<string, unknown>;
    nullPrototype.value = 1;

    assert.false(isPlainDataRecord(nullPrototype, false));
    assert.true(isPlainDataRecord(nullPrototype, true));
  });

  QUnit.test("property inspection never invokes accessors", (assert) => {
    let getterInvoked = false;
    const value: Record<PropertyKey, unknown> = {
      first: 1,
      [Symbol("symbol")]: "symbol",
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
      value: 2,
    });

    assert.deepEqual(inspectOwnProperty(value, "first"), {
      kind: "data",
      enumerable: true,
      value: 1,
    });
    assert.deepEqual(inspectOwnProperty(value, "computed"), {
      kind: "accessor",
      enumerable: true,
    });
    assert.deepEqual(ownEnumerableStringKeys(value), ["first", "computed"]);
    assert.strictEqual(ownDataProperty(value, "hidden"), 2);
    assert.strictEqual(ownEnumerableDataProperty(value, "hidden"), undefined);
    assert.false(getterInvoked);
  });

  QUnit.test("dense-array inspection rejects sparse and accessor elements", (assert) => {
    const sparse = new Array<unknown>(1);
    let reads = 0;
    const accessor = ["placeholder"];
    Object.defineProperty(accessor, "0", {
      enumerable: true,
      get() {
        reads += 1;
        return "secret";
      },
    });

    assert.deepEqual(inspectDenseArray(["a", "b"]), {
      kind: "values",
      values: ["a", "b"],
    });
    assert.deepEqual(inspectDenseArray(sparse), { kind: "invalid" });
    assert.deepEqual(inspectDenseArray(accessor), {
      kind: "invalid",
      index: 0,
    });
    assert.strictEqual(reads, 0);
  });

  QUnit.test("JSON and Cedar root snapshots preserve their contracts", (assert) => {
    assert.deepEqual(snapshotJsonValue({ empty: null, ratio: 1.5 }, "initialize"), { empty: null, ratio: 1.5 });
    assert.deepEqual(snapshotJsonObject({ ratio: 1.5 }, "initialize"), { ratio: 1.5 });
    assert.deepEqual(snapshotCedarObject({ enabled: true }, "authorizeUnsigned"), { enabled: true });
    for (const invalid of [Number.NaN, Infinity, Number.MAX_SAFE_INTEGER + 1])
      assert.strictEqual(rejectionCode(() => snapshotJsonValue(invalid, "initialize")), "INPUT_INVALID_TYPE");
    for (const snapshot of [snapshotJsonObject, snapshotCedarObject])
      for (const invalid of [null, [], "not an object"])
        assert.strictEqual(rejectionCode(() => snapshot(invalid, "initialize")), "INPUT_INVALID_TYPE");
  });
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

    const snapshot = snapshotCedarValue(source, "authorizeUnsigned");

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
      assert.strictEqual(
        rejectionCode(() => snapshotCedarValue(invalid, "authorizeUnsigned")),
        "INPUT_INVALID_TYPE",
        `invalid value ${index + 1} rejects with an input error`,
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

    const snapshot = snapshotCedarValue(
      { count: 4, price: extension },
      "authorizeUnsigned",
    );

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
      assert.strictEqual(
        rejectionCode(() => snapshotCedarValue(invalid, "authorizeUnsigned")),
        "INPUT_INVALID_TYPE",
        `invalid context value ${index + 1} rejects with an input error`,
      );
    }
  });

  QUnit.test("extension marker fields are independent of insertion order", (assert) => {
    const snapshot = snapshotCedarValue(
      {
        __extn: {
          arg: "192.0.2.1",
          fn: "ip",
        },
      },
      "authorizeUnsigned",
    );

    assert.deepEqual(snapshot, {
      __extn: {
        fn: "ip",
        arg: "192.0.2.1",
      },
    });
  });

  QUnit.test("entity reference markers are detached and preserved", (assert) => {
    const source = {
      owner: {
        __entity: {
          type: "Jans::User",
          id: "alice",
        },
      },
    };

    const snapshot = snapshotCedarValue(source, "authorizeUnsigned");
    source.owner.__entity.type = "Mutated::User";

    assert.deepEqual(snapshot, {
      owner: {
        __entity: {
          type: "Jans::User",
          id: "alice",
        },
      },
    });
  });

  QUnit.test("entity reference markers require an exact shape", (assert) => {
    const invalidValues: readonly unknown[] = [
      { __entity: { type: "Jans::User" } },
      { __entity: { id: "alice" } },
      { __entity: { type: "Jans::User", id: "alice", extra: true } },
      { __entity: { type: "", id: "alice" } },
      { __entity: { type: "Jans::User", id: "" } },
      { __entity: { type: 1, id: "alice" } },
      { __entity: { type: "Jans::User", id: 1 } },
      { __entity: "Jans::User" },
      { __entity: ["Jans::User", "alice"] },
      { __entity: { type: "Jans::User", id: "alice" }, extra: true },
      {
        __entity: { type: "Jans::User", id: "alice" },
        __extn: { fn: "ip", arg: "192.0.2.1" },
      },
    ];

    for (const [index, invalid] of invalidValues.entries()) {
      assert.strictEqual(
        rejectionCode(() => snapshotCedarValue(invalid, "authorizeUnsigned")),
        "INPUT_INVALID_TYPE",
        `invalid entity reference ${index + 1} rejects with an input error`,
      );
    }
  });

  QUnit.test("reserved cedar_entity_mapping key is rejected in Cedar values", (assert) => {
    const invalidValues: readonly unknown[] = [
      { cedar_entity_mapping: { entity_type: "Jans::User", id: "alice" } },
      {
        nested: {
          cedar_entity_mapping: { entity_type: "Jans::User", id: "alice" },
        },
      },
      [{ cedar_entity_mapping: "attacker-controlled" }],
    ];

    for (const [index, invalid] of invalidValues.entries()) {
      assert.strictEqual(
        rejectionCode(() => snapshotCedarValue(invalid, "authorizeUnsigned")),
        "INPUT_INVALID_TYPE",
        `reserved key ${index + 1} rejects with an input error`,
      );
    }
  });

}
