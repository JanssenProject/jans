import type {
  CedarContextObject,
  CedarObject,
} from "../values/types.js";

/**
 * Cedar entity used as a principal or resource in authorization.
 *
 * @example
 * ```ts
 * const document: CedarEntity = {
 *   type: "Task::Document",
 *   id: "roadmap",
 *   attributes: { owner: "alice", confidential: false },
 * };
 * ```
 */
export interface CedarEntity {
  /** Cedar entity type, such as `"Task::User"` or `"Task::Document"`. */
  readonly type: string;

  /** Application-defined entity identifier. */
  readonly id: string;

  /** Optional detached Cedar attributes. */
  readonly attributes?: CedarObject;
}

/**
 * Structured Cedar action accepted as an alternative to a formal action UID.
 *
 * The SDK converts `{ namespace: "Task", id: "Read" }` to
 * `Task::Action::"Read"`. Omit `namespace` for the root `Action` entity type.
 *
 * @example
 * ```ts
 * const action: CedarAction = {
 *   namespace: "Task",
 *   id: "Read",
 * };
 * ```
 */
export interface CedarAction {
  /** Optional Cedar namespace, including nested `::`-separated namespaces. */
  readonly namespace?: string;

  /** Non-empty Cedar action entity identifier. */
  readonly id: string;
}

/**
 * Application-asserted authorization request that does not require tokens.
 *
 * The principal is optional to support Cedar partial evaluation. Request data
 * is validated and detached before reaching WebAssembly.
 *
 * @example
 * ```ts
 * const request: UnsignedAuthorizationRequest = {
 *   principal: { type: "Task::User", id: "alice" },
 *   action: 'Task::Action::"Read"',
 *   resource: { type: "Task::Document", id: "roadmap" },
 *   context: { authenticated: true },
 * };
 * ```
 */
export interface UnsignedAuthorizationRequest {
  /** Optional principal asserted by the application. */
  readonly principal?: CedarEntity;

  /** Formal Cedar action UID or structured action evaluated by the policy store. */
  readonly action: string | CedarAction;

  /** Resource entity on which the action is requested. */
  readonly resource: CedarEntity;

  /** Optional canonical Cedar context for this decision. */
  readonly context?: CedarContextObject;
}

/**
 * One explicitly mapped JWT supplied to multi-issuer authorization.
 *
 * @example
 * ```ts
 * const token: TokenInput = {
 *   mapping: "Authorization::AccessToken",
 *   payload: signedJwt,
 * };
 * ```
 */
export interface TokenInput {
  /** Cedar entity type configured for this token kind. */
  readonly mapping: string;

  /** Compact JWT payload validated by Cedarling. */
  readonly payload: string;
}

/**
 * Token-validating authorization request for one or more trusted issuers.
 *
 * @example
 * ```ts
 * const request: MultiIssuerAuthorizationRequest = {
 *   tokens: [{ mapping: "Authorization::AccessToken", payload: signedJwt }],
 *   action: 'Authorization::Action::"Read"',
 *   resource: { type: "Authorization::Resource", id: "roadmap" },
 * };
 * ```
 */
export interface MultiIssuerAuthorizationRequest {
  /** Non-empty token set retained in caller order without deduplication. */
  readonly tokens: readonly TokenInput[];

  /** Formal Cedar action UID or structured action evaluated by the policy store. */
  readonly action: string | CedarAction;

  /** Resource entity on which the action is requested. */
  readonly resource: CedarEntity;

  /** Optional canonical Cedar context merged with token context. */
  readonly context?: CedarContextObject;
}

/**
 * One policy-evaluation diagnostic returned by Cedarling.
 *
 * @example
 * ```ts
 * const error: PolicyEvaluationError = {
 *   policyId: "allow-read",
 *   message: "Policy evaluation failed.",
 * };
 * ```
 */
export interface PolicyEvaluationError {
  /** Policy identifier associated with the evaluation error. */
  readonly policyId: string;

  /** Developer-facing policy evaluation message returned by Cedarling. */
  readonly message: string;
}

/**
 * Cedar diagnostics accompanying an authorization decision.
 *
 * @example
 * ```ts
 * const diagnostics: AuthorizationDiagnostics = {
 *   reasons: ["allow-read"],
 *   errors: [],
 * };
 * ```
 */
export interface AuthorizationDiagnostics {
  /** Policy identifiers that contributed to the decision. */
  readonly reasons: readonly string[];

  /** Policy evaluation errors observed while computing the decision. */
  readonly errors: readonly PolicyEvaluationError[];
}

/**
 * Detached result of one authorization evaluation.
 *
 * A `false` decision is a normal successful result, not a
 * {@link CedarlingAuthorizationError}.
 *
 * @example
 * ```ts
 * if (authorized.ok && authorized.value.decision) {
 *   console.log("Allowed", authorized.value.requestId);
 * }
 * ```
 */
export interface AuthorizationDecision {
  /** `true` when the request is allowed; otherwise `false`. */
  readonly decision: boolean;

  /** Cedarling-generated identifier used to correlate diagnostics and logs. */
  readonly requestId: string;

  /** Policy reasons and evaluation errors for the decision. */
  readonly diagnostics: AuthorizationDiagnostics;
}
