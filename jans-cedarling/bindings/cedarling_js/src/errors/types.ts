import type { JsonObject } from "../values/types.js";

/**
 * Represents the outcome of an expected Cedarling SDK operation.
 *
 * Inspect {@link Result.ok} before reading either the successful `value` or
 * normalized {@link CedarlingError}. Expected validation, runtime, WASM, and
 * lifecycle failures are returned instead of thrown.
 */
export type Result<T> =
  | { readonly ok: true; readonly value: T }
  | { readonly ok: false; readonly error: CedarlingError };

export type CedarlingOperation =
  | "initialize"
  | "authorizeUnsigned"
  | "authorizeMultiIssuer"
  | "logs.ids"
  | "logs.find"
  | "logs.drain"
  | "context.set"
  | "context.get"
  | "context.getEntry"
  | "context.delete"
  | "context.clear"
  | "context.entries"
  | "context.stats"
  | "issuers.isLoaded"
  | "shutDown";

/**
 * Stable, machine-readable categories for every Cedarling SDK error.
 *
 * Input codes identify the rejected constraint directly. Other codes describe
 * SDK-controlled stages and public operation boundaries. Codes are never
 * derived from Rust, WebAssembly, or host error text.
 */
export const errorCode = Object.freeze({
  inputRequired: "INPUT_REQUIRED",
  inputInvalidType: "INPUT_INVALID_TYPE",
  inputInvalidFormat: "INPUT_INVALID_FORMAT",
  inputOutOfRange: "INPUT_OUT_OF_RANGE",
  inputUnknownField: "INPUT_UNKNOWN_FIELD",
  inputConflict: "INPUT_CONFLICT",
  inputUnsupported: "INPUT_UNSUPPORTED",

  unsupportedRuntimeCapability: "UNSUPPORTED_RUNTIME_CAPABILITY",
  wasmLoadFailed: "WASM_LOAD_FAILED",
  policyLoaderFailed: "POLICY_LOADER_FAILED",
  initializationFailed: "INITIALIZATION_FAILED",

  authorizationFailed: "AUTHORIZATION_FAILED",
  policyEvaluationFailed: "POLICY_EVALUATION_FAILED",

  logStorageUnavailable: "LOG_STORAGE_UNAVAILABLE",
  logOperationFailed: "LOG_OPERATION_FAILED",

  contextOperationFailed: "CONTEXT_OPERATION_FAILED",
  issuerOperationFailed: "ISSUER_OPERATION_FAILED",

  clientClosed: "CLIENT_CLOSED",
  lifecycleFailed: "LIFECYCLE_FAILED",

  resultConversionFailed: "RESULT_CONVERSION_FAILED",
  generatedProtocolError: "GENERATED_PROTOCOL_ERROR",
} as const);

export type CedarlingErrorCode =
  (typeof errorCode)[keyof typeof errorCode];

/**
 * Type-only shape of every SDK-created error.
 *
 * The package does not export an error constructor. Narrow errors through
 * {@link Result.ok}, `code`, and `operation` rather than `instanceof`.
 * Paths and details are sanitized. Causes are normalized unless raw error
 * exposure is explicitly enabled for local debugging.
 */
export interface CedarlingError extends Error {
  readonly name: "CedarlingError";
  readonly code: CedarlingErrorCode;
  readonly operation: CedarlingOperation;

  readonly path?: readonly (string | number)[];
  readonly details?: JsonObject;
  readonly cause?: unknown;
}
