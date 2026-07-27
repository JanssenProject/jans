import type { AuthorizationDecision } from "../authorization/types.js";
import type { JsonObject } from "../values/types.js";

/**
 * Represents the outcome of an expected Cedarling SDK operation.
 *
 * Inspect {@link Result.ok} before reading either the successful `value` or
 * normalized {@link CedarlingError}. Expected validation, runtime, WASM, and
 * lifecycle failures are returned instead of thrown.
 *
 * @typeParam T - Value returned when the operation succeeds.
 * @typeParam E - Operation-specific Cedarling error union.
 *
 * @example Narrowing an authorization result
 * ```ts
 * const result = await client.authorizeUnsigned(request);
 * if (result.ok) {
 *   console.log(result.value.decision);
 * } else {
 *   console.error(result.error.code, result.error.operation);
 * }
 * ```
 */
export type Result<T, E extends CedarlingError = CedarlingError> =
  | { readonly ok: true; readonly value: T }
  | { readonly ok: false; readonly error: E };

/** Flat authorization shortcuts layered onto the standard Result contract. */
export type AuthorizationResult<E extends CedarlingError = CedarlingError> =
  | {
      readonly ok: true;
      readonly value: AuthorizationDecision;
      readonly decision: boolean;
      readonly allowed: boolean;
      readonly denied: boolean;
      readonly error?: undefined;
      readonly err?: undefined;
    }
  | {
      readonly ok: false;
      readonly error: E;
      readonly err: E;
      readonly decision: false;
      readonly allowed: false;
      readonly denied: false;
    };

/**
 * Identifies the public SDK operation that produced an error.
 *
 * Dotted values identify methods on a public service. Errors returned by the
 * generic authorization overload use `"authorize"`; named authorization
 * methods use their own operation names.
 *
 * @example
 * ```ts
 * const operation: CedarlingOperation = "authorizeUnsigned";
 * ```
 */
export type CedarlingOperation =
  | "initialize"
  | "authorize"
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
  | "close";

/**
 * Stable, machine-readable categories for Cedarling SDK failures.
 *
 * Codes describe SDK-controlled stages and public operation boundaries. They
 * are intentionally not derived from Rust or WebAssembly error text.
 *
 * @example
 * ```ts
 * if (!result.ok && result.error.code === "INVALID_INPUT") {
 *   console.log(result.error.issues);
 * }
 * ```
 */
export type CedarlingErrorCode =
  | "INVALID_INPUT"
  | "UNSUPPORTED_RUNTIME_CAPABILITY"
  | "WASM_LOAD_FAILED"
  | "POLICY_LOADER_FAILED"
  | "INITIALIZATION_FAILED"
  | "AUTHORIZATION_FAILED"
  | "LOG_STORAGE_UNAVAILABLE"
  | "LOG_OPERATION_FAILED"
  | "CONTEXT_OPERATION_FAILED"
  | "ISSUER_OPERATION_FAILED"
  | "CLIENT_CLOSED"
  | "LIFECYCLE_FAILED"
  | "RESULT_CONVERSION_FAILED"
  | "GENERATED_PROTOCOL_ERROR";

/**
 * Stable category for one input-validation issue.
 *
 * @example
 * ```ts
 * const code: ValidationIssueCode = "range";
 * ```
 */
export type ValidationIssueCode =
  | "required"
  | "type"
  | "format"
  | "range"
  | "unknownField"
  | "conflict"
  | "unsupported";

/**
 * Describes one rejected input location without exposing the rejected value.
 *
 * @example
 * ```ts
 * const issue: ValidationIssue = {
 *   path: ["policyStore", "document"],
 *   code: "type",
 *   message: "The value has an invalid type.",
 * };
 * ```
 */
export interface ValidationIssue {
  /** Property names and array indexes from the public operation input. */
  readonly path: readonly (string | number)[];

  /** Stable validation category suitable for programmatic handling. */
  readonly code: ValidationIssueCode;

  /** Safe developer-facing explanation that never contains the rejected value. */
  readonly message: string;
}

/**
 * Type-only shape of a normalized SDK failure.
 *
 * The package does not export an error constructor. Narrow errors through
 * {@link Result.ok}, `code`, and `operation` rather than `instanceof`.
 *
 * @typeParam C - Error-code union valid for the operation returning the error.
 *
 * @example
 * ```ts
 * if (!result.ok) {
 *   const error: CedarlingError = result.error;
 *   console.error(error.code, error.operation, error.message);
 * }
 * ```
 */
export interface CedarlingError<
  C extends CedarlingErrorCode = CedarlingErrorCode,
> extends Error {
  /** Constant discriminator for normalized Cedarling errors. */
  readonly name: "CedarlingError";

  /** Stable SDK error category. */
  readonly code: C;

  /** Public operation whose boundary produced the failure. */
  readonly operation: CedarlingOperation;

  /** Safe structured input issues, present for validation failures. */
  readonly issues?: readonly ValidationIssue[];

  /** Allowlisted, detached diagnostic metadata that is safe to serialize. */
  readonly details?: JsonObject;

  /**
   * Optional nested cause.
   *
   * A cause is retained only when the SDK can prove that the complete value is
   * already normalized and free of sensitive data.
   */
  readonly cause?: unknown;
}

/**
 * Errors returned by {@link createCedarling}.
 *
 * @example
 * ```ts
 * if (!created.ok) {
 *   const error: CedarlingInitializationError = created.error;
 *   console.error(error.code);
 * }
 * ```
 */
export type CedarlingInitializationError = CedarlingError<
  | "INVALID_INPUT"
  | "UNSUPPORTED_RUNTIME_CAPABILITY"
  | "WASM_LOAD_FAILED"
  | "POLICY_LOADER_FAILED"
  | "INITIALIZATION_FAILED"
  | "RESULT_CONVERSION_FAILED"
  | "GENERATED_PROTOCOL_ERROR"
>;

/**
 * Errors returned by authorization operations.
 *
 * A policy denial is not an error; it is a successful
 * {@link AuthorizationDecision} whose `decision` is `false`.
 *
 * @example
 * ```ts
 * if (!authorized.ok) {
 *   const error: CedarlingAuthorizationError = authorized.error;
 *   console.error(error.operation, error.code);
 * }
 * ```
 */
export type CedarlingAuthorizationError = CedarlingError<
  | "INVALID_INPUT"
  | "AUTHORIZATION_FAILED"
  | "CLIENT_CLOSED"
  | "RESULT_CONVERSION_FAILED"
  | "GENERATED_PROTOCOL_ERROR"
>;

/**
 * Errors returned by the Cedarling log service.
 *
 * @example
 * ```ts
 * declare const error: CedarlingLogError;
 * if (error.code === "LOG_STORAGE_UNAVAILABLE") {
 *   console.log("Enable memory logging before querying logs.");
 * }
 * ```
 */
export type CedarlingLogError = CedarlingError<
  | "INVALID_INPUT"
  | "LOG_STORAGE_UNAVAILABLE"
  | "LOG_OPERATION_FAILED"
  | "CLIENT_CLOSED"
  | "RESULT_CONVERSION_FAILED"
  | "GENERATED_PROTOCOL_ERROR"
>;

/**
 * Errors returned by context-data operations.
 *
 * @example
 * ```ts
 * declare const error: CedarlingContextError;
 * console.error(error.operation, error.code);
 * ```
 */
export type CedarlingContextError = CedarlingError<
  | "INVALID_INPUT"
  | "CONTEXT_OPERATION_FAILED"
  | "CLIENT_CLOSED"
  | "RESULT_CONVERSION_FAILED"
  | "GENERATED_PROTOCOL_ERROR"
>;

/**
 * Errors returned by issuer-readiness operations.
 *
 * @example
 * ```ts
 * declare const error: CedarlingIssuerError;
 * if (error.code === "ISSUER_OPERATION_FAILED") {
 *   console.error("Issuer readiness could not be read.");
 * }
 * ```
 */
export type CedarlingIssuerError = CedarlingError<
  | "INVALID_INPUT"
  | "ISSUER_OPERATION_FAILED"
  | "CLIENT_CLOSED"
  | "RESULT_CONVERSION_FAILED"
  | "GENERATED_PROTOCOL_ERROR"
>;

/**
 * Errors returned while closing a Cedarling client.
 *
 * @example
 * ```ts
 * const closed = await client.close();
 * if (!closed.ok) {
 *   const error: CedarlingLifecycleError = closed.error;
 *   console.error(error.code);
 * }
 * ```
 */
export type CedarlingLifecycleError = CedarlingError<
  "LIFECYCLE_FAILED" | "RESULT_CONVERSION_FAILED" | "GENERATED_PROTOCOL_ERROR"
>;
