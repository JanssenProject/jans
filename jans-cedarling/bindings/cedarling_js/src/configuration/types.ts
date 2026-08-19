import type { JsonObject } from "../values/types.js";
import type { JWT_ALGORITHMS } from "../helpers/constants.js";

export type JwtAlgorithm = (typeof JWT_ALGORITHMS)[number];

export interface AuthorizationOptions {
  readonly dangerouslyDisableSchemaValidation?: boolean;
  readonly decisionLogTokenIdClaim?: string;
}

export interface JwtValidationOptions {
  readonly dangerouslyDisableSignatureValidation?: boolean;
  readonly dangerouslyDisableStatusValidation?: boolean;
  readonly allowedAlgorithms?: readonly JwtAlgorithm[];
}

export interface CedarlingDebugOptions {
  readonly dangerouslyExposeRawErrors?: boolean;
}

export interface CedarlingOptions {
  readonly applicationName: string;
  readonly policyStore: {
    readonly type: "inline";
    readonly document: JsonObject;
  };
  readonly authorization?: AuthorizationOptions;
  readonly jwt?: JwtValidationOptions;
  readonly debug?: CedarlingDebugOptions;
}
