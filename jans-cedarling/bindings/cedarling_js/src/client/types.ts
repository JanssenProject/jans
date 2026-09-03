import type {
  AuthorizationDecision,
  MultiIssuerAuthorizationRequest,
  UnsignedAuthorizationRequest,
} from "../authorization/types.js";
import type { Result } from "../errors/types.js";
import type { CedarlingLogs } from "../logs/types.js";
import type { CedarlingContext } from "../context/types.js";
import type { CedarlingIssuers } from "../issuers/types.js";

/**
 * Web-native Cedarling client returned by {@link createCedarling}.
 *
 * Create clients through the factory; no concrete client constructor is
 * exported. Always shut down a client when the application no longer needs it.
 */
export interface CedarlingClient {
  authorizeUnsigned(
    request: UnsignedAuthorizationRequest,
  ): Promise<Result<AuthorizationDecision>>;

  authorizeMultiIssuer(
    request: MultiIssuerAuthorizationRequest,
  ): Promise<Result<AuthorizationDecision>>;

  readonly issuers: CedarlingIssuers;
  readonly context: CedarlingContext;
  readonly logs: CedarlingLogs;

  shutDown(): Promise<Result<void>>;
}
