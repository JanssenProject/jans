import type {
  AuthorizationDecision,
  MultiIssuerAuthorizationRequest,
  UnsignedAuthorizationRequest,
} from "../authorization/types.js";
import type { Result } from "../errors/types.js";

export interface CedarlingClient {
  authorizeUnsigned(
    request: UnsignedAuthorizationRequest,
  ): Promise<Result<AuthorizationDecision>>;
  authorizeMultiIssuer(
    request: MultiIssuerAuthorizationRequest,
  ): Promise<Result<AuthorizationDecision>>;
  shutDown(): Promise<Result<void>>;
}
