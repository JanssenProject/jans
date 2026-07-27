/**
 * Host-neutral inspection primitives for caller-owned JavaScript values.
 *
 * This module reports structure only. Feature modules remain responsible for
 * deciding which prototypes, property states, validation codes, and paths
 * their interfaces accept.
 */

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

/**
 * Tests whether a value is an ordinary data record.
 *
 * @param value - Unknown JavaScript value to inspect.
 * @param allowNullPrototype - Whether a null-prototype record is accepted.
 */
export function isPlainDataRecord(
  value: unknown,
  allowNullPrototype: boolean,
): value is DataRecord {
  if (typeof value !== "object" || value === null || Array.isArray(value)) {
    return false;
  }

  const prototype: unknown = Object.getPrototypeOf(value);
  return (
    prototype === Object.prototype ||
    (allowNullPrototype && prototype === null)
  );
}

/**
 * Classifies an already-read property descriptor without evaluating accessors.
 *
 * @param descriptor - Own descriptor or `undefined` when the key is absent.
 */
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

/**
 * Reads and classifies one own property without evaluating an accessor.
 *
 * @param value - Previously accepted object.
 * @param key - Own property key to inspect.
 */
export function inspectOwnProperty(
  value: object,
  key: PropertyKey,
): OwnPropertyInspection {
  return inspectPropertyDescriptor(
    Object.getOwnPropertyDescriptor(value, key),
  );
}
