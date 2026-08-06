import type { LogLevel } from "../configuration/types.js";
import { createSdkError } from "../errors/errors.js";
import type { CedarlingOperation } from "../errors/types.js";
import {
  LOG_ENVELOPE_FIELD_SET,
  LOG_KIND_SET,
  LOG_LEVEL_SET,
} from "../helpers/constants.js";
import type { JsonObject, JsonValue } from "../values/types.js";
import { snapshotJsonObject } from "../values/snapshot.js";
import type {
  CedarlingLogEntry,
  CedarlingLogKind,
} from "./types.js";

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
  return LOG_KIND_SET.has(normalized)
    ? (normalized as CedarlingLogKind)
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
  return LOG_LEVEL_SET.has(normalized) ? (normalized as LogLevel) : false;
}

/** Copies non-envelope fields into an SDK-owned JSON payload. */
function payloadFrom(value: JsonObject): JsonObject {
  const payload: Record<string, JsonValue> = Object.create(null) as Record<
    string,
    JsonValue
  >;
  for (const key of Object.keys(value)) {
    if (!LOG_ENVELOPE_FIELD_SET.has(key)) {
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

  const id = snapshot.id;
  const pdpId = snapshot.pdp_id;
  const requestId = optionalString(snapshot.request_id);
  const timestamp = optionalString(snapshot.timestamp);
  const applicationId = optionalString(snapshot.application_id);
  const level = logLevel(snapshot.level);
  const explicitKind = logKind(snapshot.log_kind);
  const kind =
    explicitKind ??
    (typeof snapshot.message === "string" && level !== false
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
