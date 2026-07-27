import type { LogLevel } from "../configuration/types.js";
import { createSdkError } from "../errors/errors.js";
import type { CedarlingOperation } from "../errors/types.js";
import type { JsonObject, JsonValue } from "../values/types.js";
import { snapshotJsonObject } from "../values/snapshot.js";
import type {
  CedarlingLogEntry,
  CedarlingLogKind,
} from "./types.js";

/** Generated envelope fields excluded from the public payload. */
const envelopeFields = new Set([
  "id",
  "request_id",
  "timestamp",
  "log_kind",
  "level",
  "pdp_id",
  "application_id",
]);

/** Supported generated log levels after case normalization. */
const logLevels: ReadonlySet<string> = new Set([
  "trace",
  "debug",
  "info",
  "warn",
  "error",
  "fatal",
]);

/** Reads an own value from a detached null-prototype JSON object. */
function field(value: JsonObject, key: string): JsonValue | undefined {
  return Object.prototype.hasOwnProperty.call(value, key)
    ? value[key]
    : undefined;
}

/** Copies optional generated string fields while accepting explicit null. */
function optionalString(
  value: JsonValue | undefined,
): string | undefined | false {
  if (value === undefined || value === null) {
    return undefined;
  }
  return typeof value === "string" ? value : false;
}

/** Normalizes one generated log category. */
function logKind(value: JsonValue | undefined): CedarlingLogKind | undefined {
  if (typeof value !== "string") {
    return undefined;
  }
  const normalized = value.toLowerCase();
  return normalized === "decision" ||
    normalized === "system" ||
    normalized === "metric"
    ? normalized
    : undefined;
}

/** Normalizes one optional generated severity. */
function logLevel(
  value: JsonValue | undefined,
): LogLevel | undefined | false {
  if (value === undefined || value === null) {
    return undefined;
  }
  if (typeof value !== "string") {
    return false;
  }
  const normalized = value.toLowerCase();
  return logLevels.has(normalized) ? (normalized as LogLevel) : false;
}

/** Copies non-envelope fields into an SDK-owned JSON payload. */
function payloadFrom(value: JsonObject): JsonObject {
  const payload: Record<string, JsonValue> = Object.create(null) as Record<
    string,
    JsonValue
  >;
  for (const key of Object.keys(value)) {
    if (!envelopeFields.has(key)) {
      const item = value[key];
      if (item !== undefined) {
        payload[key] = item;
      }
    }
  }
  return payload;
}

/** Converts one unknown generated entry into the stable public log shape. */
export function normalizeGeneratedLog(
  value: unknown,
  operation: Extract<CedarlingOperation, "logs.find" | "logs.drain">,
): CedarlingLogEntry {
  let snapshot: JsonObject;
  try {
    snapshot = snapshotJsonObject(value);
  } catch {
    throw createSdkError("GENERATED_PROTOCOL_ERROR", operation);
  }

  const id = field(snapshot, "id");
  const pdpId = field(snapshot, "pdp_id");
  const requestId = optionalString(field(snapshot, "request_id"));
  const timestamp = optionalString(field(snapshot, "timestamp"));
  const applicationId = optionalString(field(snapshot, "application_id"));
  const level = logLevel(field(snapshot, "level"));
  const explicitKind = logKind(field(snapshot, "log_kind"));
  const kind =
    explicitKind ??
    (typeof field(snapshot, "message") === "string" && level !== false
      ? "system"
      : undefined);

  if (
    typeof id !== "string" ||
    id.length === 0 ||
    typeof pdpId !== "string" ||
    pdpId.length === 0 ||
    requestId === false ||
    timestamp === false ||
    applicationId === false ||
    level === false ||
    kind === undefined
  ) {
    throw createSdkError("GENERATED_PROTOCOL_ERROR", operation);
  }

  return {
    id,
    ...(requestId === undefined ? {} : { requestId }),
    ...(timestamp === undefined ? {} : { timestamp }),
    kind,
    ...(level === undefined ? {} : { level }),
    pdpId,
    ...(applicationId === undefined ? {} : { applicationId }),
    payload: payloadFrom(snapshot),
  };
}
