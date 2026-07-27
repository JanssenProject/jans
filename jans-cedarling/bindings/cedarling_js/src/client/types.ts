import type {
  AuthorizationRequest,
  MultiIssuerAuthorization,
  MultiIssuerAuthorizationRequest,
  UnsignedAuthorization,
  UnsignedAuthorizationRequest,
} from "../authorization/types.js";
import type {
  CedarlingAuthorizationError,
  CedarlingLifecycleError,
  AuthorizationResult,
  Result,
} from "../errors/types.js";
import type { CedarlingLogs } from "../logs/types.js";
import type { CedarlingContext } from "../context/types.js";
import type { CedarlingIssuers } from "../issuers/types.js";

/**
 * Narrow authorization-only capability implemented by a Cedarling client.
 *
 * Named operations expose the selected trust model at the call site.
 * `authorize()` is only a discriminated convenience dispatcher.
 *
 * @example
 * ```ts
 * const authorizer: CedarlingAuthorizer = client;
 * const result = await authorizer.authorize({
 *   type: "unsigned",
 *   request,
 * });
 * ```
 */
export interface CedarlingAuthorizer {
  /**
   * Authorizes application-asserted Cedar entities and context.
   *
   * @param request - Unsigned request to validate, detach, and evaluate.
   * @returns A decision or a normalized authorization error.
   *
   * @example
   * ```ts
   * const authorized = await client.authorizeUnsigned({
   *   principal: { type: "Task::User", id: "alice" },
   *   action: 'Task::Action::"Read"',
   *   resource: { type: "Task::Document", id: "roadmap" },
   * });
   * ```
   */
  authorizeUnsigned(
    request: UnsignedAuthorizationRequest,
  ): Promise<AuthorizationResult<CedarlingAuthorizationError>>;

  /**
   * Validates mapped JWTs from trusted issuers and authorizes their entities.
   *
   * @param request - Multi-issuer token request to validate and evaluate.
   * @returns A decision or a normalized authorization error.
   */
  authorizeMultiIssuer(
    request: MultiIssuerAuthorizationRequest,
  ): Promise<AuthorizationResult<CedarlingAuthorizationError>>;

  /**
   * Dispatches an unsigned authorization envelope to `authorizeUnsigned()`.
   *
   * @param request - Explicit unsigned authorization envelope.
   * @returns The named operation's decision or an error labeled `authorize`.
   */
  authorize(
    request: UnsignedAuthorization,
  ): Promise<AuthorizationResult<CedarlingAuthorizationError>>;

  /**
   * Dispatches a multi-issuer envelope to `authorizeMultiIssuer()`.
   *
   * @param request - Explicit token-validating authorization envelope.
   * @returns The named operation's decision or an error labeled `authorize`.
   */
  authorize(
    request: MultiIssuerAuthorization,
  ): Promise<AuthorizationResult<CedarlingAuthorizationError>>;

  /**
   * Dispatches a discriminated authorization union to one named operation.
   *
   * @param request - Unsigned or multi-issuer authorization envelope.
   * @returns The selected named operation's decision with dispatcher errors.
   */
  authorize(
    request: AuthorizationRequest,
  ): Promise<AuthorizationResult<CedarlingAuthorizationError>>;
}

/**
 * Web-native Cedarling client returned by {@link createCedarling}.
 *
 * Create clients through the factory; no concrete client constructor is
 * exported. Always close a client when the application no longer needs it.
 *
 * @example
 * ```ts
 * const created = await createCedarling(options);
 * if (created.ok) {
 *   try {
 *     const result = await created.value.authorizeUnsigned(request);
 *     console.log(result);
 *   } finally {
 *     await created.value.close();
 *   }
 * }
 * ```
 */
export interface CedarlingClient extends CedarlingAuthorizer {
  /** Configured trusted-issuer readiness observations. */
  readonly issuers: CedarlingIssuers;

  /** Per-client context data injected into subsequent authorization calls. */
  readonly context: CedarlingContext;

  /** Retained decision, system, and metric log queries. */
  readonly logs: CedarlingLogs;

  /**
   * Shuts down and releases the Cedarling client.
   *
   * @returns Success after shutdown, or a normalized lifecycle error.
   *
   * @example
   * ```ts
   * const closed = await client.close();
   * if (!closed.ok) console.error(closed.error.code);
   * ```
   */
  close(): Promise<Result<void, CedarlingLifecycleError>>;
}
