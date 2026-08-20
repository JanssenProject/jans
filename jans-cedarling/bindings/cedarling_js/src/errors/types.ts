import type { JsonObject } from "../values/types.js";

export type Result<T> =
  | { readonly ok: true; readonly value: T }
  | { readonly ok: false; readonly error: CedarlingError };

export type CedarlingOperation =
  | "initialize"
  | "authorizeUnsigned"
  | "authorizeMultiIssuer"
  | "shutDown";

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
  initializationFailed: "INITIALIZATION_FAILED",

  authorizationFailed: "AUTHORIZATION_FAILED",
  policyEvaluationFailed: "POLICY_EVALUATION_FAILED",

  clientClosed: "CLIENT_CLOSED",
  lifecycleFailed: "LIFECYCLE_FAILED",

  resultConversionFailed: "RESULT_CONVERSION_FAILED",
  generatedProtocolError: "GENERATED_PROTOCOL_ERROR",
} as const);

export type CedarlingErrorCode =
  (typeof errorCode)[keyof typeof errorCode];

export interface CedarlingError extends Error {
  readonly name: "CedarlingError";
  readonly code: CedarlingErrorCode;
  readonly operation: CedarlingOperation;
  readonly path?: readonly (string | number)[];
  readonly details?: JsonObject;
  readonly cause?: unknown;
}
