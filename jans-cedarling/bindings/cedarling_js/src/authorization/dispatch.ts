import type {
  AuthorizationRequest,
  MultiIssuerAuthorizationRequest,
  UnsignedAuthorizationRequest,
} from "./types.js";
import { InputValidationError } from "../errors/errors.js";
import {
  inspectOwnProperty,
  isPlainDataRecord,
  type DataRecord,
} from "../values/inspect.js";

/** Raises one envelope-validation issue without retaining rejected input. */
function invalid(
  code: "required" | "type" | "unsupported",
  path: readonly (string | number)[],
): never {
  throw new InputValidationError(
    code,
    "invalid authorization envelope",
    path,
  );
}

/** Requires a plain authorization envelope. */
function record(value: unknown): DataRecord {
  if (!isPlainDataRecord(value, true)) {
    return invalid("type", []);
  }
  return value;
}

/** Reads an own enumerable data field without invoking an accessor. */
function field(value: DataRecord, key: string): unknown {
  const property = inspectOwnProperty(value, key);
  if (property.kind === "missing" || !property.enumerable) {
    return undefined;
  }
  if (property.kind === "accessor") {
    return invalid("type", [key]);
  }
  return property.value;
}

/**
 * Selects one named authorization request without validating it a second time.
 *
 * The selected named client method remains the sole owner of request
 * validation, detachment, lifecycle accounting, and engine invocation.
 */
export function selectAuthorizationRequest(
  value: AuthorizationRequest,
): AuthorizationRequest {
  const envelope = record(value);
  const type = field(envelope, "type");

  if (type === undefined) {
    return invalid("required", ["type"]);
  }
  if (typeof type !== "string") {
    return invalid("type", ["type"]);
  }
  if (type !== "unsigned" && type !== "multiIssuer") {
    return invalid("unsupported", ["type"]);
  }

  const request = field(envelope, "request");
  if (request === undefined) {
    return invalid("required", ["request"]);
  }

  return type === "unsigned"
    ? {
        type,
        request: request as UnsignedAuthorizationRequest,
      }
    : {
        type,
        request: request as MultiIssuerAuthorizationRequest,
      };
}
