/**
 * SDK-owned validation and snapshot helpers for every JSON-shaped public value.
 *
 * Traversal uses property descriptors instead of normal property access so
 * accessors are rejected without executing application code.
 */
import type {
  CedarEntityReference,
  CedarExtensionFunction,
  CedarExtensionValue,
  CedarObject,
  CedarValue,
  JsonObject,
  JsonValue,
} from "./types.js";
import { createInputError } from "../errors/errors.js";
import { errorCode, type CedarlingOperation } from "../errors/types.js";
import { CEDAR_EXTENSION_FUNCTION_SET } from "../helpers/constants.js";
import {
  inspectDenseArray,
  inspectPropertyDescriptor,
  isPlainDataRecord,
} from "../helpers/records.js";

/** Raises the single public error shape without retaining the rejected value. */
function invalidValue(operation: CedarlingOperation): never {
  throw createInputError(errorCode.inputInvalidType, operation);
}

/** Returns an enumerable data-property value without invoking accessors. */
function dataPropertyValue(
  descriptor: PropertyDescriptor | undefined,
  operation: CedarlingOperation,
): unknown {
  const property = inspectPropertyDescriptor(descriptor);
  if (property.kind !== "data" || !property.enumerable) {
    return invalidValue(operation);
  }

  return property.value;
}

/** Recursive snapshot function shared by array and object traversal. */
type SnapshotValue<T> = (
  value: unknown,
  ancestors: WeakSet<object>,
  operation: CedarlingOperation,
) => T;

/**
 * Copies a dense array using descriptors.
 *
 * Sparse arrays, symbol properties, accessors, and extra own properties are
 * rejected because they do not have a lossless Cedar/JSON representation.
 */
function snapshotArray<T>(
  value: readonly unknown[],
  ancestors: WeakSet<object>,
  snapshotValue: SnapshotValue<T>,
  operation: CedarlingOperation,
): readonly T[] {
  const inspected = inspectDenseArray(value);
  if (inspected.kind === "invalid") {
    return invalidValue(operation);
  }

  const snapshot: T[] = [];
  for (const item of inspected.values) {
    snapshot.push(snapshotValue(item, ancestors, operation));
  }

  return snapshot;
}

/**
 * Enumerates a plain data object without evaluating getters.
 *
 * Only ordinary or null prototypes are accepted; class instances and other
 * behavior-bearing objects must be converted by the caller first.
 */
function plainObjectEntries(
  value: object,
  operation: CedarlingOperation,
): readonly (readonly [string, unknown])[] {
  if (!isPlainDataRecord(value, true)) {
    return invalidValue(operation);
  }

  const ownKeys = Reflect.ownKeys(value);

  if (ownKeys.some((key) => typeof key === "symbol")) {
    return invalidValue(operation);
  }

  const descriptors = Object.getOwnPropertyDescriptors(value);
  const entries: [string, unknown][] = [];

  for (const key of ownKeys) {
    if (typeof key !== "string") {
      return invalidValue(operation);
    }

    entries.push([key, dataPropertyValue(descriptors[key], operation)]);
  }

  return entries;
}

/** Recursively copies entries from a previously validated plain object. */
function snapshotObject<T>(
  value: object,
  ancestors: WeakSet<object>,
  snapshotValue: SnapshotValue<T>,
  operation: CedarlingOperation,
): Readonly<Record<string, T>> {
  const snapshot: Record<string, T> = Object.create(null) as Record<string, T>;

  for (const [key, item] of plainObjectEntries(value, operation)) {
    snapshot[key] = snapshotValue(item, ancestors, operation);
  }

  return snapshot;
}

/** Validates a root object and starts cycle tracking for recursive traversal. */
function snapshotRootObject<T>(
  value: unknown,
  snapshotValue: SnapshotValue<T>,
  operation: CedarlingOperation,
): Readonly<Record<string, T>> {
  if (value === null || typeof value !== "object" || Array.isArray(value)) {
    return invalidValue(operation);
  }

  const ancestors = new WeakSet<object>();
  ancestors.add(value);

  try {
    return snapshotObject(value, ancestors, snapshotValue, operation);
  } finally {
    ancestors.delete(value);
  }
}

/**
 * Canonically validates and copies one Cedar-compatible value.
 *
 * Entity attributes, request context, and retained context data share one
 * representation and traversal policy.
 */
const snapshotCedarValueInner: SnapshotValue<CedarValue> =
  (value, ancestors, operation) => {
    if (typeof value === "boolean" || typeof value === "string") {
      return value;
    }

    if (typeof value === "number") {
      if (!Number.isSafeInteger(value)) {
        return invalidValue(operation);
      }

      return value;
    }

    if (value === null || typeof value !== "object") {
      return invalidValue(operation);
    }

    if (ancestors.has(value)) {
      return invalidValue(operation);
    }

    ancestors.add(value);

    try {
      if (Array.isArray(value)) {
        return snapshotArray(
          value,
          ancestors,
          snapshotCedarValueInner,
          operation,
        );
      }

      if (Reflect.ownKeys(value).includes("__entity")) {
        return snapshotCedarEntityReference(value, operation);
      }

      if (Reflect.ownKeys(value).includes("__extn")) {
        return snapshotCedarExtension(value, operation);
      }

      if (Reflect.ownKeys(value).includes("cedar_entity_mapping")) {
        return invalidValue(operation);
      }

      return snapshotObject(
        value,
        ancestors,
        snapshotCedarValueInner,
        operation,
      );
    } finally {
      ancestors.delete(value);
    }
  };

/**
 * Validates and detaches one Cedar entity-attribute value.
 *
 * @param value - Untrusted JavaScript input.
 * @returns An SDK-owned Cedar value.
 * @throws A branded Cedarling error when the value is not losslessly
 * representable.
 */
export function snapshotCedarValue(
  value: unknown,
  operation: CedarlingOperation,
): CedarValue {
  return snapshotCedarValueInner(value, new WeakSet<object>(), operation);
}

/**
 * Validates and detaches a root Cedar entity-attributes object.
 *
 * @param value - Untrusted JavaScript input.
 * @returns An SDK-owned Cedar object.
 */
export function snapshotCedarObject(
  value: unknown,
  operation: CedarlingOperation,
): CedarObject {
  return snapshotRootObject(
    value,
    snapshotCedarValueInner,
    operation,
  );
}

/** Narrows a string to a supported Cedar extension function. */
function isCedarExtensionFunction(
  value: string,
): value is CedarExtensionFunction {
  return CEDAR_EXTENSION_FUNCTION_SET.has(value);
}

/**
 * Validates an exact `{ __extn: { fn, arg } }` marker.
 *
 * Extra outer or inner fields are rejected so ordinary context objects cannot
 * be confused with extension values.
 */
function snapshotCedarExtension(
  value: object,
  operation: CedarlingOperation,
): CedarExtensionValue {
  const outerEntries = plainObjectEntries(value, operation);

  if (
    outerEntries.length !== 1 ||
    outerEntries[0]?.[0] !== "__extn" ||
    outerEntries[0][1] === null ||
    typeof outerEntries[0][1] !== "object" ||
    Array.isArray(outerEntries[0][1])
  ) {
    return invalidValue(operation);
  }

  const innerEntries = plainObjectEntries(outerEntries[0][1], operation);
  const inner = new Map(innerEntries);

  if (
    innerEntries.length !== 2 ||
    !inner.has("fn") ||
    !inner.has("arg")
  ) {
    return invalidValue(operation);
  }

  const fn = inner.get("fn");
  const arg = inner.get("arg");

  if (
    typeof fn !== "string" ||
    !isCedarExtensionFunction(fn) ||
    typeof arg !== "string" ||
    arg.trim().length === 0
  ) {
    return invalidValue(operation);
  }

  return {
    __extn: {
      fn,
      arg,
    },
  };
}

/**
 * Validates an exact `{ __entity: { type, id } }` marker.
 *
 * Extra outer or inner fields are rejected so ordinary context objects cannot
 * be confused with entity references.
 */
function snapshotCedarEntityReference(
  value: object,
  operation: CedarlingOperation,
): CedarEntityReference {
  const outerEntries = plainObjectEntries(value, operation);

  if (
    outerEntries.length !== 1 ||
    outerEntries[0]?.[0] !== "__entity"
  ) {
    return invalidValue(operation);
  }

  const entityValue = outerEntries[0][1];
  if (
    entityValue === null ||
    typeof entityValue !== "object" ||
    Array.isArray(entityValue)
  ) {
    return invalidValue(operation);
  }

  const innerEntries = plainObjectEntries(entityValue, operation);
  const inner = new Map(innerEntries);

  if (
    innerEntries.length !== 2 ||
    !inner.has("type") ||
    !inner.has("id")
  ) {
    return invalidValue(operation);
  }

  const type = inner.get("type");
  const id = inner.get("id");

  if (
    typeof type !== "string" ||
    type.trim().length === 0 ||
    typeof id !== "string" ||
    id.trim().length === 0
  ) {
    return invalidValue(operation);
  }

  return {
    __entity: {
      type,
      id,
    },
  };
}

/**
 * Validates one JSON value while preserving nested `null` and finite
 * fractional numbers.
 */
function snapshotJsonValueInner(
  value: unknown,
  ancestors: WeakSet<object>,
  operation: CedarlingOperation,
): JsonValue {
  if (
    value === null ||
    typeof value === "boolean" ||
    typeof value === "string"
  ) {
    return value;
  }

  if (typeof value === "number") {
    if (!Number.isFinite(value)) {
      return invalidValue(operation);
    }

    if (Number.isInteger(value) && !Number.isSafeInteger(value)) {
      return invalidValue(operation);
    }

    return value;
  }

  if (typeof value !== "object") {
    return invalidValue(operation);
  }

  if (ancestors.has(value)) {
    return invalidValue(operation);
  }

  ancestors.add(value);

  try {
    if (Array.isArray(value)) {
      return snapshotArray(value, ancestors, snapshotJsonValueInner, operation);
    }

    return snapshotObject(value, ancestors, snapshotJsonValueInner, operation);
  } finally {
    ancestors.delete(value);
  }
}

/**
 * Validates and detaches one JSON-compatible value.
 *
 * @param value - Untrusted JavaScript input.
 * @returns An SDK-owned JSON value.
 */
export function snapshotJsonValue(
  value: unknown,
  operation: CedarlingOperation,
): JsonValue {
  return snapshotJsonValueInner(value, new WeakSet<object>(), operation);
}

/**
 * Validates and detaches a root JSON object.
 *
 * @param value - Untrusted JavaScript input.
 * @returns An SDK-owned JSON object.
 */
export function snapshotJsonObject(
  value: unknown,
  operation: CedarlingOperation,
): JsonObject {
  return snapshotRootObject(
    value,
    snapshotJsonValueInner,
    operation,
  );
}
