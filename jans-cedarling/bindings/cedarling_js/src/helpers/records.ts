/** Host-neutral structural inspection of caller- and adapter-owned values. */

export type DataRecord = Readonly<Record<string, unknown>>;

/** Neutral classification of one own JavaScript property descriptor. */
export type OwnPropertyInspection =
  | { readonly kind: "missing" }
  | { readonly kind: "accessor"; readonly enumerable: boolean }
  | {
      readonly kind: "data";
      readonly enumerable: boolean;
      readonly value: unknown;
    };

/** Reports whether a value is a non-null, non-array object record. */
export function isObjectRecord(
  value: unknown,
): value is Readonly<Record<string, unknown>> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

/** Reports whether a value is an ordinary data record. */
export function isPlainDataRecord(
  value: unknown,
  allowNullPrototype: boolean,
): value is DataRecord {
  if (!isObjectRecord(value)) {
    return false;
  }

  const prototype: unknown = Object.getPrototypeOf(value);
  return (
    prototype === Object.prototype ||
    (allowNullPrototype && prototype === null)
  );
}

/** Classifies a descriptor without evaluating an accessor. */
export function inspectPropertyDescriptor(
  descriptor: PropertyDescriptor | undefined,
): OwnPropertyInspection {
  if (descriptor === undefined) {
    return { kind: "missing" };
  }
  if (!("value" in descriptor)) {
    return {
      kind: "accessor",
      enumerable: descriptor.enumerable === true,
    };
  }
  return {
    kind: "data",
    enumerable: descriptor.enumerable === true,
    value: descriptor.value,
  };
}

/** Reads and classifies one own property without evaluating an accessor. */
export function inspectOwnProperty(
  value: object,
  key: PropertyKey,
): OwnPropertyInspection {
  return inspectPropertyDescriptor(Object.getOwnPropertyDescriptor(value, key));
}

/** Lists own enumerable string keys without evaluating accessors. */
export function ownEnumerableStringKeys(value: object): readonly string[] {
  return Object.keys(value);
}

/** Reads one own data property, including a non-enumerable property. */
export function ownDataProperty(value: object, key: PropertyKey): unknown {
  const property = inspectOwnProperty(value, key);
  return property.kind === "data" ? property.value : undefined;
}

/** Result of descriptor-safe dense-array inspection. */
export type DenseArrayInspection =
  | { readonly kind: "values"; readonly values: readonly unknown[] }
  | { readonly kind: "invalid"; readonly index?: number };

/** Reads a dense array without invoking accessors. */
export function inspectDenseArray(
  value: readonly unknown[],
): DenseArrayInspection {
  const ownKeys = Reflect.ownKeys(value);
  if (
    ownKeys.some((key) => typeof key === "symbol") ||
    ownKeys.length !== value.length + 1
  ) {
    return { kind: "invalid" };
  }

  const descriptors = Object.getOwnPropertyDescriptors(value);
  const values: unknown[] = [];
  for (let index = 0; index < value.length; index += 1) {
    const property = inspectPropertyDescriptor(descriptors[String(index)]);
    if (property.kind !== "data" || !property.enumerable) {
      return { kind: "invalid", index };
    }
    values.push(property.value);
  }
  return { kind: "values", values };
}

/** Reads one own enumerable data property without invoking accessors. */
export function ownEnumerableDataProperty(
  value: object,
  key: PropertyKey,
): unknown {
  const property = inspectOwnProperty(value, key);
  return property.kind === "data" && property.enumerable
    ? property.value
    : undefined;
}
