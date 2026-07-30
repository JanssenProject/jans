import { InputValidationError } from "../errors/errors.js";
import {
  inspectOwnProperty,
  isPlainDataRecord,
} from "../values/inspect.js";
import type {
  CedarlingLogTag,
  LogQuery,
} from "./types.js";

/** Complete category and severity tag allowlist. */
const tags: ReadonlySet<string> = new Set([
  "decision",
  "system",
  "metric",
  "trace",
  "debug",
  "info",
  "warn",
  "error",
  "fatal",
]);

/** Reads one enumerable own data property without invoking accessors. */
function field(
  value: Readonly<Record<PropertyKey, unknown>>,
  key: string,
): unknown {
  const property = inspectOwnProperty(value, key);
  if (
    property.kind === "accessor" ||
    property.kind === "data" && !property.enumerable
  ) {
    throw new InputValidationError("type", "Expected a data property.", [
      key,
    ]);
  }
  return property.kind === "data" ? property.value : undefined;
}

/** Requires one non-empty string query field. */
function identifier(value: unknown, key: string): string {
  if (typeof value !== "string" || value.length === 0) {
    throw new InputValidationError(
      value === undefined ? "required" : "type",
      "Expected a non-empty identifier.",
      [key],
    );
  }
  return value;
}

/** Validates and detaches the request-correlated retained-log query. */
export function snapshotLogQuery(
  value: LogQuery | undefined,
): LogQuery | undefined {
  if (value === undefined) {
    return undefined;
  }
  if (!isPlainDataRecord(value, false)) {
    throw new InputValidationError("type", "Expected a log query.");
  }

  const keys = Object.keys(value);
  for (const key of keys) {
    if (key !== "id" && key !== "requestId" && key !== "tag") {
      throw new InputValidationError("unknownField", "Unknown log query field.", [
        key,
      ]);
    }
  }
  const id = field(value, "id");
  const requestId = field(value, "requestId");
  const tag = field(value, "tag");
  const present = [id, requestId, tag].filter(
    (item) => item !== undefined,
  ).length;

  if (present === 0) {
    throw new InputValidationError(
      "conflict",
      "Expected one supported log query combination.",
    );
  }
  if (id !== undefined) {
    if (present !== 1) {
      throw new InputValidationError(
        "conflict",
        "Expected one supported log query combination.",
      );
    }
    return { id: identifier(id, "id") };
  }
  if (
    tag !== undefined &&
    (typeof tag !== "string" || !tags.has(tag))
  ) {
    throw new InputValidationError("unsupported", "Unknown log tag.", [
      "tag",
    ]);
  }

  return requestId === undefined
    ? { tag: tag as CedarlingLogTag }
    : {
        requestId: identifier(requestId, "requestId"),
        ...(tag === undefined ? {} : { tag: tag as CedarlingLogTag }),
      };
}
