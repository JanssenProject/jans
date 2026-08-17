import type {
  AuthorizationDecision,
  MultiIssuerAuthorizationRequest,
  UnsignedAuthorizationRequest,
} from "../authorization/types.js";
import type { PreparedEngineOptions } from "../configuration/prepare.js";

export interface CedarlingEngine {
  authorizeUnsigned(
    request: UnsignedAuthorizationRequest,
  ): Promise<AuthorizationDecision>;
  authorizeMultiIssuer(
    request: MultiIssuerAuthorizationRequest,
  ): Promise<AuthorizationDecision>;
  shutDown(): Promise<void>;
}

export type EngineFactory = (
  options: PreparedEngineOptions,
) => Promise<CedarlingEngine>;
