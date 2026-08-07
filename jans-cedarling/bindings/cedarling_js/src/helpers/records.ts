/** Host-neutral structural inspection of caller- and adapter-owned values. */

/** Plain record shape used after prototype inspection. */
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
  const keys: string[] = [];

  for (const key of Reflect.ownKeys(value)) {
    if (typeof key !== "string") {
      continue;
    }
    const property = inspectOwnProperty(value, key);
    if (property.kind !== "missing" && property.enumerable) {
      keys.push(key);
    }
  }

  return keys;
}

/** Reads one own data property, including a non-enumerable property. */
export function ownDataProperty(value: object, key: PropertyKey): unknown {
  const property = inspectOwnProperty(value, key);
  return property.kind === "data" ? property.value : undefined;
}

/** Reads one own enumerable data property. */
export function ownEnumerableDataProperty(
  value: object,
  key: PropertyKey,
): unknown {
  const property = inspectOwnProperty(value, key);
  return property.kind === "data" && property.enumerable
    ? property.value
    : undefined;
}
