import { InputValidationError } from "../errors/errors.js";
import {
  inspectOwnProperty,
  isPlainDataRecord,
} from "../values/inspect.js";
import { snapshotContextDataValue } from "../values/snapshot.js";
import type { ContextDataValue } from "../values/types.js";
import type { ContextSetOptions } from "./types.js";

/** Requires one non-empty context-data key without changing its identity. */
export function snapshotContextKey(value: string): string {
  if (typeof value !== "string" || value.trim().length === 0) {
    throw new InputValidationError(
      typeof value === "string" ? "required" : "type",
      "Expected a non-empty context key.",
    );
  }
  return value;
}

/** Validates and detaches one context-data write. */
export function snapshotContextSet(
  key: string,
  value: ContextDataValue,
  options: ContextSetOptions | undefined,
  maxTtlSeconds: number,
): {
  readonly key: string;
  readonly value: ContextDataValue;
  readonly ttlSeconds?: number;
} {
  const snapshotKey = snapshotContextKey(key);
  const snapshotValue = snapshotContextDataValue(value);
  if (options === undefined) {
    return { key: snapshotKey, value: snapshotValue };
  }
  if (!isPlainDataRecord(options, false)) {
    throw new InputValidationError("type", "Expected context set options.", [
      "options",
    ]);
  }
  for (const optionKey of Object.keys(options)) {
    if (optionKey !== "ttlSeconds") {
      throw new InputValidationError("unknownField", "Unknown set option.", [
        "options",
        optionKey,
      ]);
    }
  }
  const ttlProperty = inspectOwnProperty(options, "ttlSeconds");
  if (ttlProperty.kind === "accessor") {
    throw new InputValidationError("type", "Expected a data property.", [
      "options",
      "ttlSeconds",
    ]);
  }
  const ttlSeconds =
    ttlProperty.kind === "data" && ttlProperty.enumerable
      ? ttlProperty.value
      : undefined;
  if (
    ttlSeconds !== undefined &&
    (typeof ttlSeconds !== "number" ||
      !Number.isSafeInteger(ttlSeconds) ||
      ttlSeconds < 1 ||
      ttlSeconds > maxTtlSeconds)
  ) {
    throw new InputValidationError("range", "Expected a safe TTL.", [
      "options",
      "ttlSeconds",
    ]);
  }
  return {
    key: snapshotKey,
    value: snapshotValue,
    ...(ttlSeconds === undefined ? {} : { ttlSeconds }),
  };
}
