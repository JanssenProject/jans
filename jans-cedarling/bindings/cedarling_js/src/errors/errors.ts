/**
 * Private error normalization and redaction Module.
 *
 * Only values created and branded here may become public `CedarlingError`
 * objects or retained causes.
 */
import type {
  CedarlingError,
  CedarlingErrorCode,
  CedarlingOperation,
  ValidationIssue,
  ValidationIssueCode,
} from "./types.js";
import type { JsonObject, JsonValue } from "../values/types.js";
import {
  inspectOwnProperty,
  isPlainDataRecord,
} from "../values/inspect.js";

/** Generic developer-safe message assigned to each stable error code. */
const errorMessages: Readonly<Record<CedarlingErrorCode, string>> = {
  INVALID_INPUT: "Cedarling input validation failed.",
  UNSUPPORTED_RUNTIME_CAPABILITY:
    "The runtime does not support a required Cedarling capability.",
  WASM_LOAD_FAILED: "The Cedarling WebAssembly module could not be loaded.",
  POLICY_LOADER_FAILED: "The application policy loader failed.",
  INITIALIZATION_FAILED: "Cedarling initialization failed.",
  AUTHORIZATION_FAILED: "Cedarling authorization failed.",
  LOG_STORAGE_UNAVAILABLE: "Cedarling log storage is unavailable.",
  LOG_OPERATION_FAILED: "The Cedarling log operation failed.",
  CONTEXT_OPERATION_FAILED: "The Cedarling context operation failed.",
  ISSUER_OPERATION_FAILED: "The Cedarling issuer operation failed.",
  CLIENT_CLOSED: "The Cedarling client is closed.",
  LIFECYCLE_FAILED: "The Cedarling lifecycle operation failed.",
  RESULT_CONVERSION_FAILED: "A Cedarling result could not be converted.",
  GENERATED_PROTOCOL_ERROR: "The Cedarling generated protocol is incompatible.",
};

/** Generic developer-safe message assigned to each validation category. */
const issueMessages: Readonly<Record<ValidationIssueCode, string>> = {
  required: "A required value is missing.",
  type: "The value has an invalid type.",
  format: "The value has an invalid format.",
  range: "The value is outside the supported range.",
  unknownField: "The field is not supported.",
  conflict: "The value conflicts with another field.",
  unsupported: "The value is not supported.",
};

/** Brands immutable errors that are safe enough to retain as nested causes. */
const safeErrors = new WeakSet<object>();

/** Conservative character sets accepted in allowlisted diagnostic metadata. */
const safeDetailString = /^[A-Za-z][A-Za-z0-9._-]{0,79}$/u;

/** Conservative character set accepted in validation paths. */
const safePathSegment = /^[A-Za-z0-9_-]{1,80}$/u;

/** Optional safe inputs accepted by the private SDK error factory. */
export interface SdkErrorOptions {
  /** Validation issues whose messages and paths will be sanitized. */
  readonly issues?: readonly ValidationIssue[];

  /** Diagnostic metadata filtered through the fixed safe-detail allowlist. */
  readonly details?: Readonly<Record<string, unknown>>;

  /** Cause retained only when it is an immutable error branded by this module. */
  readonly cause?: unknown;
}

/**
 * Private failure raised by value validators before operation-level
 * normalization assigns an SDK code and public input path.
 */
export class InputValidationError extends TypeError {
  /** Structured validation issues carried to the public boundary. */
  readonly issues: readonly ValidationIssue[];

  /**
   * Creates one private validation failure.
   *
   * @param code - Stable validation category.
   * @param message - Internal explanation; public normalization replaces it
   * with the SDK-controlled message for `code`.
   * @param path - Path relative to the validator's current root.
   */
  constructor(
    code: ValidationIssueCode,
    message: string,
    path: readonly (string | number)[] = [],
  ) {
    super(message);
    this.name = "InputValidationError";
    this.issues = [{ path, code, message }];
  }
}

/**
 * Removes credentials, query, and fragment components from an HTTP diagnostic
 * URL. Invalid URL strings are omitted rather than echoed.
 */
function sanitizeUrl(value: unknown): string | undefined {
  if (typeof value !== "string") {
    return undefined;
  }

  try {
    const url = new URL(value);
    url.username = "";
    url.password = "";
    url.search = "";
    url.hash = "";
    return url.toString();
  } catch {
    return undefined;
  }
}

/** Reads an own enumerable data property without invoking an accessor. */
function dataProperty(
  value: Readonly<Record<string, unknown>>,
  key: string,
): unknown {
  const property = inspectOwnProperty(value, key);
  return property.kind === "data" && property.enumerable
    ? property.value
    : undefined;
}

/**
 * Copies only diagnostic fields that have an explicit, non-secret public use.
 *
 * The fixed allowlist deliberately excludes tokens, policy material, archive
 * bytes, entity attributes, context values, authorization headers, and paths.
 */
function sanitizeDetails(
  details: Readonly<Record<string, unknown>> | undefined,
): JsonObject | undefined {
  if (
    details === undefined ||
    !isPlainDataRecord(details, false)
  ) {
    return undefined;
  }

  const sanitized: Record<string, JsonValue> = {};

  for (const key of ["runtimeCapability", "sourceType", "requestId", "wasmMessage"] as const) {
    const value = dataProperty(details, key);
    if (typeof value === "string" && safeDetailString.test(value)) {
      sanitized[key] = value;
    }
  }

  const httpStatus = dataProperty(details, "httpStatus");
  if (
    typeof httpStatus === "number" &&
    Number.isInteger(httpStatus) &&
    httpStatus >= 100 &&
    httpStatus <= 599
  ) {
    sanitized.httpStatus = httpStatus;
  }

  const url = sanitizeUrl(dataProperty(details, "url"));
  if (url !== undefined) {
    sanitized.url = url;
  }

  return Object.keys(sanitized).length === 0
    ? undefined
    : Object.freeze(sanitized);
}

/** Copies a validation path while rejecting unsafe or malformed segments. */
function sanitizeIssuePath(
  path: readonly (string | number)[],
): readonly (string | number)[] {
  const sanitized: (string | number)[] = [];

  for (const segment of path) {
    if (
      (typeof segment === "number" &&
        Number.isSafeInteger(segment) &&
        segment >= 0) ||
      (typeof segment === "string" && safePathSegment.test(segment))
    ) {
      sanitized.push(segment);
    }
  }

  return Object.freeze(sanitized);
}

/**
 * Rebuilds issues with SDK-controlled messages so rejected input cannot leak
 * through enumerable error serialization.
 */
function sanitizeIssues(
  issues: readonly ValidationIssue[] | undefined,
): readonly ValidationIssue[] | undefined {
  if (issues === undefined || issues.length === 0) {
    return undefined;
  }

  return Object.freeze(
    issues.map((issue) =>
      Object.freeze({
        path: sanitizeIssuePath(issue.path),
        code: issue.code,
        message: issueMessages[issue.code],
      }),
    ),
  );
}

/**
 * Creates an immutable, enumerable, safely serializable Cedarling SDK error.
 *
 * @param code - Stable wrapper-controlled failure stage.
 * @param operation - Public operation that produced the failure.
 * @param options - Structured diagnostics to sanitize and retain.
 */
export function createSdkError<C extends CedarlingErrorCode>(
  code: C,
  operation: CedarlingOperation,
  options: SdkErrorOptions = {},
): CedarlingError<C> {
  const error = Object.assign(new Error(errorMessages[code]), {
    name: "CedarlingError" as const,
    code,
    operation,
  });
  const issues = sanitizeIssues(options.issues);
  const details = sanitizeDetails(options.details);

  if (issues !== undefined) {
    Object.assign(error, { issues });
  }

  if (details !== undefined) {
    Object.assign(error, { details });
  }

  if (
    typeof options.cause === "object" &&
    options.cause !== null &&
    safeErrors.has(options.cause)
  ) {
    Object.assign(error, { cause: options.cause });
  }

  safeErrors.add(error);
  return Object.freeze(error);
}

/** Tests whether a value is an immutable SDK error branded by this module. */
export function isSdkError(error: unknown): error is CedarlingError {
  return typeof error === "object" && error !== null && safeErrors.has(error);
}

/**
 * Narrows a branded SDK error to the operation-specific code union accepted by
 * a public result boundary.
 */
export function isSdkErrorCode<C extends CedarlingErrorCode>(
  error: unknown,
  codes: readonly C[],
): error is CedarlingError<C> {
  return isSdkError(error) && codes.some((code) => code === error.code);
}

/**
 * Prefixes validator-relative issues with a public operation input path.
 *
 * Unknown thrown values become one generic type issue and are never inspected
 * or stringified.
 */
export function validationIssuesAt(
  error: unknown,
  path: readonly (string | number)[],
): readonly ValidationIssue[] {
  if (error instanceof InputValidationError) {
    return error.issues.map((issue) => ({
      ...issue,
      path: [...path, ...issue.path],
    }));
  }

  return [
    {
      path,
      code: "type",
      message: issueMessages.type,
    },
  ];
}
