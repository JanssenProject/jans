import type {
  CedarlingError,
  CedarlingErrorCode,
  CedarlingOperation,
} from "./types.js";
import { errorCode } from "./types.js";
import type { JsonObject, JsonValue } from "../values/types.js";
import {
  isPlainDataRecord,
  ownEnumerableDataProperty,
} from "../helpers/records.js";

const SAFE_DETAIL_FIELDS = [
  "runtimeCapability",
  "sourceType",
  "requestId",
  "wasmMessage",
] as const;
const SAFE_DETAIL_STRING_PATTERN = /^[A-Za-z][A-Za-z0-9._-]{0,79}$/u;
const SAFE_PATH_SEGMENT_PATTERN = /^[A-Za-z0-9_-]{1,80}$/u;

const ERROR_MESSAGES: Readonly<Record<CedarlingErrorCode, string>> = {
  [errorCode.inputRequired]: "A required Cedarling input value is missing.",
  [errorCode.inputInvalidType]:
    "A Cedarling input value has an invalid type.",
  [errorCode.inputInvalidFormat]:
    "A Cedarling input value has an invalid format.",
  [errorCode.inputOutOfRange]:
    "A Cedarling input value is outside the supported range.",
  [errorCode.inputUnknownField]: "A Cedarling input field is not supported.",
  [errorCode.inputConflict]:
    "A Cedarling input value conflicts with another field.",
  [errorCode.inputUnsupported]: "A Cedarling input value is not supported.",
  [errorCode.unsupportedRuntimeCapability]:
    "The runtime does not support a required Cedarling capability.",
  [errorCode.wasmLoadFailed]:
    "The Cedarling WebAssembly module could not be loaded.",
  [errorCode.initializationFailed]: "Cedarling initialization failed.",
  [errorCode.authorizationFailed]: "Cedarling authorization failed.",
  [errorCode.policyEvaluationFailed]: "A Cedar policy evaluation failed.",
  [errorCode.clientClosed]: "The Cedarling client is closed.",
  [errorCode.lifecycleFailed]: "The Cedarling lifecycle operation failed.",
  [errorCode.resultConversionFailed]:
    "A Cedarling result could not be converted.",
  [errorCode.generatedProtocolError]:
    "The Cedarling generated protocol is incompatible.",
};

const safeErrors = new WeakSet<object>();
const rawErrorCauses = new WeakMap<object, unknown>();
const exposedRawErrors = new WeakSet<object>();

function sanitizeDetails(
  details: Readonly<Record<string, unknown>> | undefined,
): JsonObject | undefined {
  if (details === undefined || !isPlainDataRecord(details, false)) {
    return undefined;
  }
  const sanitized: Record<string, JsonValue> = {};
  for (const key of SAFE_DETAIL_FIELDS) {
    const value = ownEnumerableDataProperty(details, key);
    if (typeof value === "string" && SAFE_DETAIL_STRING_PATTERN.test(value)) {
      sanitized[key] = value;
    }
  }
  return Object.keys(sanitized).length === 0
    ? undefined
    : Object.freeze(sanitized);
}

function sanitizePath(
  path: readonly (string | number)[] | undefined,
): readonly (string | number)[] | undefined {
  if (path === undefined) return undefined;
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

const createErrorEnvelope = (
  message: string,
  code: CedarlingErrorCode,
  operation: CedarlingOperation,
) => Object.assign(new Error(message), {
  name: "CedarlingError" as const,
  code,
  operation,
});

function buildSdkError(
  code: CedarlingErrorCode,
  operation: CedarlingOperation,
  message: string,
  options: {
    readonly path?: readonly (string | number)[];
    readonly details?: JsonObject;
    readonly cause?: unknown;
    readonly rawCause?: unknown;
  } = {},
): CedarlingError {
  const error = createErrorEnvelope(message, code, operation);
  if (options.path !== undefined) Object.assign(error, { path: options.path });
  if (options.details !== undefined) {
    Object.assign(error, { details: options.details });
  }
  if (
    typeof options.cause === "object" &&
    options.cause !== null &&
    safeErrors.has(options.cause)
  ) {
    Object.assign(error, { cause: options.cause });
  }
  if ("rawCause" in options) rawErrorCauses.set(error, options.rawCause);
  safeErrors.add(error);
  return Object.freeze(error);
}

export function createSdkError(
  code: CedarlingErrorCode,
  operation: CedarlingOperation,
  options: {
    readonly path?: readonly (string | number)[];
    readonly details?: Readonly<Record<string, unknown>>;
    readonly cause?: unknown;
    readonly rawCause?: unknown;
  } = {},
): CedarlingError {
  return buildSdkError(code, operation, ERROR_MESSAGES[code], {
    ...options,
    path: sanitizePath(options.path),
    details: sanitizeDetails(options.details),
  });
}

export function createPolicyEvaluationError(
  policyId: string,
  message: string,
  operation: Extract<
    CedarlingOperation,
    "authorizeUnsigned" | "authorizeMultiIssuer"
  >,
): CedarlingError {
  return buildSdkError(
    errorCode.policyEvaluationFailed,
    operation,
    message,
    { details: Object.freeze({ policyId }) },
  );
}

export function exposeSdkErrorCause(
  error: CedarlingError,
): CedarlingError {
  if (exposedRawErrors.has(error) || !rawErrorCauses.has(error)) {
    return error;
  }
  const exposed = createErrorEnvelope(
    error.message,
    error.code,
    error.operation,
  );
  if (error.path !== undefined) Object.assign(exposed, { path: error.path });
  if (error.details !== undefined) {
    Object.assign(exposed, { details: error.details });
  }
  Object.defineProperty(exposed, "cause", {
    configurable: false,
    enumerable: false,
    value: rawErrorCauses.get(error),
    writable: false,
  });
  safeErrors.add(exposed);
  exposedRawErrors.add(exposed);
  return Object.freeze(exposed);
}

function isSdkError(error: unknown): error is CedarlingError {
  return typeof error === "object" && error !== null && safeErrors.has(error);
}

export function isSdkErrorCode(
  error: unknown,
  codes: readonly CedarlingErrorCode[],
): error is CedarlingError {
  return isSdkError(error) && codes.some((code) => code === error.code);
}

const isInputErrorCode = (code: CedarlingErrorCode): boolean =>
  code.startsWith("INPUT_");

export function createInputError(
  code: CedarlingErrorCode,
  operation: CedarlingOperation,
  path: readonly (string | number)[] = [],
): CedarlingError {
  return createSdkError(
    isInputErrorCode(code) ? code : errorCode.inputInvalidType,
    operation,
    { path },
  );
}

export function normalizeInputError(
  error: unknown,
  operation: CedarlingOperation,
  path: readonly (string | number)[] = [],
): CedarlingError {
  return isSdkError(error) && isInputErrorCode(error.code)
    ? createInputError(error.code, operation, [...path, ...(error.path ?? [])])
    : createInputError(errorCode.inputInvalidType, operation, path);
}

function operationErrorPolicy(operation: CedarlingOperation) {
  const shared = [
    errorCode.resultConversionFailed,
    errorCode.generatedProtocolError,
  ] as const;
  if (operation.startsWith("authorize")) {
    return {
      fallback: errorCode.authorizationFailed,
      allowed: [errorCode.authorizationFailed, ...shared],
    } as const;
  }
  if (operation === "shutDown") {
    return {
      fallback: errorCode.lifecycleFailed,
      allowed: [errorCode.lifecycleFailed, ...shared],
    } as const;
  }
  return {
    fallback: errorCode.initializationFailed,
    allowed: [
      errorCode.initializationFailed,
      errorCode.unsupportedRuntimeCapability,
      errorCode.wasmLoadFailed,
      ...shared,
    ],
  } as const;
}

export function normalizeOperationError(
  error: unknown,
  operation: CedarlingOperation,
  exposeRawErrors: boolean,
): CedarlingError {
  const policy = operationErrorPolicy(operation);
  const normalized = isSdkErrorCode(error, policy.allowed)
    ? error
    : createSdkError(policy.fallback, operation, { rawCause: error });
  return exposeRawErrors ? exposeSdkErrorCause(normalized) : normalized;
}
