import type { CedarObject } from "../values/types.js";
import type { CedarlingError } from "../errors/types.js";

export interface CedarEntity {
  readonly type: string;
  readonly id: string;
  readonly attributes?: CedarObject;
}

/**
 * Structured Cedar action accepted as an alternative to a formal action UID.
 *
 * The SDK converts `{ namespace: "Task", id: "Read" }` to
 * `Task::Action::"Read"`. Omit `namespace` for the root `Action` entity type.
 */
export interface CedarAction {
  readonly namespace?: string;
  readonly id: string;
}

/**
 * Application-asserted authorization request that does not require tokens.
 *
 * The principal is optional to support Cedar partial evaluation. Request data
 * is validated and detached before reaching WebAssembly.
 */
export interface UnsignedAuthorizationRequest {
  readonly principal?: CedarEntity;
  readonly action: string | CedarAction;
  readonly resource: CedarEntity;
  readonly context?: CedarObject;
}

export interface TokenInput {
  readonly mapping: string;
  readonly payload: string;
}

/**
 * Token-validating authorization request. The method name distinguishes this
 * flow from unsigned authorization; it does not require multiple tokens or
 * issuers.
 */
export interface MultiIssuerAuthorizationRequest {
  readonly tokens: readonly TokenInput[];
  readonly action: string | CedarAction;
  readonly resource: CedarEntity;
  readonly context?: CedarObject;
}

/**
 * Detached result of one authorization evaluation.
 *
 * A `false` decision is a normal successful result, not an operational error.
 */
export interface AuthorizationDecision {
  readonly decision: boolean;
  readonly requestId: string;
  readonly diagnostics: {
    readonly reasons: readonly string[];
    readonly errors: readonly CedarlingError[];
  };
}
