/**
 * Public entry point for the Cedarling JavaScript SDK.
 *
 * The package exports {@link createCedarling} as its only runtime value. Client
 * implementations, Runtime Adapters, and generated WebAssembly bindings remain
 * private; all other exports are TypeScript types.
 *
 * @packageDocumentation
 */

import { createCedarlingForEngine } from "./client/client.js";
import { createEmbeddedEngine } from "./engine/embedded.js";

/**
 * Creates one isolated Cedarling client.
 *
 * Private package conditions select the Runtime Adapter; generated JavaScript
 * and WebAssembly assets never enter the public Interface.
 */
export const createCedarling =
  createCedarlingForEngine(createEmbeddedEngine);

export type {
  AuthorizationOptions,
  CedarlingBaseOptions,
  CedarlingDebugOptions,
  CedarlingOptions,
  RawBootstrapCedarlingOptions,
  WebNativeCedarlingOptions,
  ContextStoreOptions,
  HttpOptions,
  IssuerLoadingOptions,
  JwtAlgorithm,
  JwtValidationOptions,
  LockOptions,
  LoggingOptions,
  LogLevel,
  PolicyStoreSource,
  TokenCacheOptions,
  UrlPolicyStoreSource,
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
  CedarlingLogEntry,
  CedarlingLogKind,
  CedarlingLogs,
  CedarlingLogTag,
  LogQuery,
} from "./logs/types.js";

export type {
  CedarDataType,
  CedarlingContext,
  ContextDataEntry,
  ContextDataStats,
  ContextSetOptions,
} from "./context/types.js";

export type {
  CedarlingIssuers,
  IssuerReference,
} from "./issuers/types.js";

export type {
  CedarlingOperation,
  Result,
  CedarlingError,
  CedarlingErrorCode,
} from "./errors/types.js";
