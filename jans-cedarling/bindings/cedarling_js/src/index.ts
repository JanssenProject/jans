import { createCedarlingForEngine } from "./client/client.js";
import { createWebEngine } from "./engine/web.js";

export const createCedarling = createCedarlingForEngine(createWebEngine);

export type {
  AuthorizationOptions,
  CedarlingDebugOptions,
  CedarlingOptions,
  JwtAlgorithm,
  JwtValidationOptions,
} from "./configuration/types.js";
export type {
  CedarObject,
  CedarExtensionFunction,
  CedarExtensionValue,
  CedarEntityReference,
  JsonObject,
  JsonValue,
  CedarValue,
  ContextDataValue,
} from "./values/types.js";
export type {
  CedarAction,
  CedarEntity,
  TokenInput,
  UnsignedAuthorizationRequest,
  MultiIssuerAuthorizationRequest,
  AuthorizationDecision,
} from "./authorization/types.js";
export type { CedarlingClient } from "./client/types.js";
export type {
  CedarlingOperation,
  Result,
  CedarlingError,
  CedarlingErrorCode,
} from "./errors/types.js";
