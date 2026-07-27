/**
 * SDK-owned validation and snapshot helpers for every JSON-shaped public value.
 *
 * Traversal uses property descriptors instead of normal property access so
 * accessors are rejected without executing application code.
 */
import type {
  CedarContextValue,
  CedarContextObject,
  CedarObject,
  CedarExtensionFunction,
  CedarExtensionValue,
  CedarValue,
  ContextDataValue,
  JsonObject,
  JsonValue,
} from "./types.js";
import { InputValidationError } from "../errors/errors.js";
import {
  inspectPropertyDescriptor,
  isPlainDataRecord,
} from "./inspect.js";

/** Raises a private validation failure without retaining the rejected value. */
function invalidValue(message: string): never {
  throw new InputValidationError("type", message);
}

/** Returns an enumerable data-property value without invoking accessors. */
function dataPropertyValue(
  descriptor: PropertyDescriptor | undefined,
): unknown {
  const property = inspectPropertyDescriptor(descriptor);
  if (property.kind !== "data" || !property.enumerable) {
    return invalidValue("Expected an enumerable data property.");
  }

  return property.value;
}

/** Recursive snapshot function shared by array and object traversal. */
type SnapshotValue<T> = (
  value: unknown,
  ancestors: WeakSet<object>,
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
): readonly T[] {
  const ownKeys = Reflect.ownKeys(value);

  if (
    ownKeys.some((key) => typeof key === "symbol") ||
    ownKeys.length !== value.length + 1
  ) {
    return invalidValue("Expected a dense Cedar array.");
  }

  const descriptors = Object.getOwnPropertyDescriptors(value);
  const snapshot: T[] = [];

  for (let index = 0; index < value.length; index += 1) {
    const item = dataPropertyValue(descriptors[String(index)]);
    snapshot.push(snapshotValue(item, ancestors));
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
): readonly (readonly [string, unknown])[] {
  if (!isPlainDataRecord(value, true)) {
    return invalidValue("Expected a plain data object.");
  }

  const ownKeys = Reflect.ownKeys(value);

  if (ownKeys.some((key) => typeof key === "symbol")) {
    return invalidValue("Symbol properties are not supported.");
  }

  const descriptors = Object.getOwnPropertyDescriptors(value);
  const entries: [string, unknown][] = [];

  for (const key of ownKeys) {
    if (typeof key !== "string") {
      return invalidValue("Expected a string property name.");
    }

    entries.push([key, dataPropertyValue(descriptors[key])]);
  }

  return entries;
}

/** Recursively copies entries from a previously validated plain object. */
function snapshotObject<T>(
  value: object,
  ancestors: WeakSet<object>,
  snapshotValue: SnapshotValue<T>,
): Readonly<Record<string, T>> {
  const snapshot: Record<string, T> = Object.create(null) as Record<string, T>;

  for (const [key, item] of plainObjectEntries(value)) {
    snapshot[key] = snapshotValue(item, ancestors);
  }

  return snapshot;
}

/** Validates a root object and starts cycle tracking for recursive traversal. */
function snapshotRootObject<T>(
  value: unknown,
  snapshotValue: SnapshotValue<T>,
  message: string,
): Readonly<Record<string, T>> {
  if (value === null || typeof value !== "object" || Array.isArray(value)) {
    return invalidValue(message);
  }

  const ancestors = new WeakSet<object>();
  ancestors.add(value);

  try {
    return snapshotObject(value, ancestors, snapshotValue);
  } finally {
    ancestors.delete(value);
  }
}

/**
 * Validates one Cedar entity-attribute value.
 *
 * Plain numbers must be safe integers. Fractional and specialized values use
 * the same explicit extension marker accepted by request context.
 */
function snapshotCedarValueInner(
  value: unknown,
  ancestors: WeakSet<object>,
): CedarValue {
  if (typeof value === "boolean" || typeof value === "string") {
    return value;
  }

  if (typeof value === "number") {
    if (!Number.isSafeInteger(value)) {
      return invalidValue("Expected a safe Cedar integer.");
    }

    return value;
  }

  if (value === null || typeof value !== "object") {
    return invalidValue("Expected a Cedar value.");
  }

  if (ancestors.has(value)) {
    return invalidValue("Cyclic Cedar values are not supported.");
  }

  ancestors.add(value);

  try {
    if (Array.isArray(value)) {
      return snapshotArray(value, ancestors, snapshotCedarValueInner);
    }

    if (Reflect.ownKeys(value).includes("__extn")) {
      return snapshotCedarExtension(value);
    }

    return snapshotObject(value, ancestors, snapshotCedarValueInner);
  } finally {
    ancestors.delete(value);
  }
}

/**
 * Validates and detaches one Cedar entity-attribute value.
 *
 * @param value - Untrusted JavaScript input.
 * @returns An SDK-owned Cedar value.
 * @throws {@link InputValidationError} when the value is not losslessly
 * representable.
 */
export function snapshotCedarValue(value: unknown): CedarValue {
  return snapshotCedarValueInner(value, new WeakSet<object>());
}

/**
 * Validates and detaches a root Cedar entity-attributes object.
 *
 * @param value - Untrusted JavaScript input.
 * @returns An SDK-owned Cedar object.
 */
export function snapshotCedarObject(value: unknown): CedarObject {
  return snapshotRootObject(
    value,
    snapshotCedarValueInner,
    "Expected a Cedar object.",
  );
}

/** Cedar extension functions accepted by the canonical context marker. */
const extensionFunctions: ReadonlySet<string> = new Set([
  "decimal",
  "ip",
  "datetime",
  "duration",
]);

/** Narrows a string to a supported Cedar extension function. */
function isCedarExtensionFunction(
  value: string,
): value is CedarExtensionFunction {
  return extensionFunctions.has(value);
}

/**
 * Validates an exact `{ __extn: { fn, arg } }` marker.
 *
 * Extra outer or inner fields are rejected so ordinary context objects cannot
 * be confused with extension values.
 */
function snapshotCedarExtension(value: object): CedarExtensionValue {
  const outerEntries = plainObjectEntries(value);

  if (
    outerEntries.length !== 1 ||
    outerEntries[0]?.[0] !== "__extn" ||
    outerEntries[0][1] === null ||
    typeof outerEntries[0][1] !== "object" ||
    Array.isArray(outerEntries[0][1])
  ) {
    return invalidValue("Expected an exact Cedar extension marker.");
  }

  const innerEntries = plainObjectEntries(outerEntries[0][1]);
  const inner = new Map(innerEntries);

  if (
    innerEntries.length !== 2 ||
    !inner.has("fn") ||
    !inner.has("arg")
  ) {
    return invalidValue("Expected Cedar extension fn and arg fields.");
  }

  const fn = inner.get("fn");
  const arg = inner.get("arg");

  if (
    typeof fn !== "string" ||
    !isCedarExtensionFunction(fn) ||
    typeof arg !== "string" ||
    arg.trim().length === 0
  ) {
    return invalidValue("Expected a supported Cedar extension and argument.");
  }

  return {
    __extn: {
      fn,
      arg,
    },
  };
}

/**
 * Validates one request-context value.
 *
 * Plain numbers are restricted to safe integers; fractional or specialized
 * Cedar values must use an explicit extension marker.
 */
function snapshotCedarContextValueInner(
  value: unknown,
  ancestors: WeakSet<object>,
): CedarContextValue {
  if (typeof value === "boolean" || typeof value === "string") {
    return value;
  }

  if (typeof value === "number") {
    if (!Number.isSafeInteger(value)) {
      return invalidValue("Expected a safe integer in Cedar context.");
    }

    return value;
  }

  if (value === null || typeof value !== "object") {
    return invalidValue("Expected a Cedar context value.");
  }

  if (ancestors.has(value)) {
    return invalidValue("Cyclic Cedar context values are not supported.");
  }

  ancestors.add(value);

  try {
    if (Array.isArray(value)) {
      return snapshotArray(value, ancestors, snapshotCedarContextValueInner);
    }

    if (Reflect.ownKeys(value).includes("__extn")) {
      return snapshotCedarExtension(value);
    }

    return snapshotObject(value, ancestors, snapshotCedarContextValueInner);
  } finally {
    ancestors.delete(value);
  }
}

/**
 * Validates and detaches one Cedar request-context value.
 *
 * @param value - Untrusted JavaScript input.
 * @returns An SDK-owned canonical Cedar context value.
 */
export function snapshotCedarContextValue(
  value: unknown,
): CedarContextValue {
  return snapshotCedarContextValueInner(value, new WeakSet<object>());
}

/**
 * Validates and detaches a root Cedar request-context object.
 *
 * @param value - Untrusted JavaScript input.
 * @returns An SDK-owned canonical Cedar context object.
 */
export function snapshotCedarContextObject(
  value: unknown,
): CedarContextObject {
  return snapshotRootObject(
    value,
    snapshotCedarContextValueInner,
    "Expected a Cedar context object.",
  );
}

/**
 * Validates one JSON value while preserving nested `null` and finite
 * fractional numbers.
 */
function snapshotJsonValueInner(
  value: unknown,
  ancestors: WeakSet<object>,
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
      return invalidValue("Expected a finite JSON number.");
    }

    if (Number.isInteger(value) && !Number.isSafeInteger(value)) {
      return invalidValue("Expected a safe JSON integer.");
    }

    return value;
  }

  if (typeof value !== "object") {
    return invalidValue("Expected a JSON value.");
  }

  if (ancestors.has(value)) {
    return invalidValue("Cyclic JSON values are not supported.");
  }

  ancestors.add(value);

  try {
    if (Array.isArray(value)) {
      return snapshotArray(value, ancestors, snapshotJsonValueInner);
    }

    return snapshotObject(value, ancestors, snapshotJsonValueInner);
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
export function snapshotJsonValue(value: unknown): JsonValue {
  return snapshotJsonValueInner(value, new WeakSet<object>());
}

/**
 * Validates and detaches a root JSON object.
 *
 * @param value - Untrusted JavaScript input.
 * @returns An SDK-owned JSON object.
 */
export function snapshotJsonObject(value: unknown): JsonObject {
  return snapshotRootObject(
    value,
    snapshotJsonValueInner,
    "Expected a JSON object.",
  );
}

/**
 * Validates and detaches one context-data value.
 *
 * Top-level `null` is rejected, while nested JSON `null` and finite
 * fractional numbers are preserved for storage round trips.
 *
 * @param value - Untrusted JavaScript input.
 * @returns An SDK-owned context-data value.
 */
export function snapshotContextDataValue(value: unknown): ContextDataValue {
  return snapshotCedarContextValue(value);
}
