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
  isPlainDataRecord,
  ownEnumerableDataProperty,
} from "../helpers/records.js";
import {
  ERROR_MESSAGES,
  LIMITS,
  SAFE_DETAIL_FIELDS,
  SAFE_DETAIL_STRING_PATTERN,
  SAFE_PATH_SEGMENT_PATTERN,
  VALIDATION_ISSUE_MESSAGES,
} from "../helpers/constants.js";

/** Brands immutable errors that are safe enough to retain as nested causes. */
const safeErrors = new WeakSet<object>();

/** Optional safe inputs accepted by the private SDK error factory. */
interface SdkErrorOptions {
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

  for (const key of SAFE_DETAIL_FIELDS) {
    const value = ownEnumerableDataProperty(details, key);
    if (typeof value === "string" && SAFE_DETAIL_STRING_PATTERN.test(value)) {
      sanitized[key] = value;
    }
  }

  const httpStatus = ownEnumerableDataProperty(details, "httpStatus");
  if (
    typeof httpStatus === "number" &&
    Number.isInteger(httpStatus) &&
    httpStatus >= LIMITS.httpStatus.minimum &&
    httpStatus <= LIMITS.httpStatus.maximum
  ) {
    sanitized.httpStatus = httpStatus;
  }

  const url = sanitizeUrl(ownEnumerableDataProperty(details, "url"));
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
      (typeof segment === "string" && SAFE_PATH_SEGMENT_PATTERN.test(segment))
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
        message: VALIDATION_ISSUE_MESSAGES[issue.code],
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
  const error = Object.assign(new Error(ERROR_MESSAGES[code]), {
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
function isSdkError(error: unknown): error is CedarlingError {
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
      message: VALIDATION_ISSUE_MESSAGES.type,
    },
  ];
}
