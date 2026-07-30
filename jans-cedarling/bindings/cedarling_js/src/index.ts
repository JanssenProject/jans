/**
 * Web-native public entry point for the Cedarling JavaScript SDK.
 *
 * The package exports {@link createCedarling} as its only runtime value. Client
 * implementations and generated WebAssembly bindings remain private; all
 * other exports are TypeScript types.
 *
 * @packageDocumentation
 */

import { createCedarlingForEngine } from "./client/client.js";
import { createWebEngine } from "./engine/web.js";

/**
 * Creates Cedarling through the generated binding's standard Web initializer.
 *
 * The generated JavaScript module and its adjacent WebAssembly asset remain
 * private implementation details. The host must deliver the dependency-owned
 * asset through the standard Web URL and fetch contract.
 *
 * @example
 * ```ts
 * import { createCedarling } from "@janssenproject/cedarling";
 *
 * const created = await createCedarling({
 *   applicationName: "task-manager",
 *   policyStore: {
 *     type: "inline",
 *     document: policyStoreDocument,
 *   },
 * });
 * ```
 */
export const createCedarling =
  createCedarlingForEngine(createWebEngine);

export type {
  AuthorizationOptions,
  CedarlingBaseOptions,
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
  PolicyRefreshOptions,
  PolicyStoreSource,
  TokenCacheOptions,
  UrlPolicyStoreSource,
} from "./configuration/types.js";

export type {
  CedarObject,
  CedarPrimitive,
  CedarExtensionFunction,
  CedarExtensionValue,
  JsonObject,
  JsonPrimitive,
  JsonValue,
  PolicyStoreDocument,
  CedarValue,
  CedarContextObject,
  CedarContextValue,
  ContextDataValue,
} from "./values/types.js";

export type {
  CedarAction,
  CedarEntity,
  TokenInput,
  UnsignedAuthorizationRequest,
  MultiIssuerAuthorizationRequest,
  AuthorizationDecision,
  AuthorizationDiagnostics,
  PolicyEvaluationError,
} from "./authorization/types.js";

export type {
  CedarlingAuthorizer,
  CedarlingClient,
} from "./client/types.js";

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
  AuthorizationResult,
  ValidationIssue,
  ValidationIssueCode,
  CedarlingContextError,
  CedarlingError,
  CedarlingErrorCode,
  CedarlingInitializationError,
  CedarlingIssuerError,
  CedarlingLifecycleError,
  CedarlingLogError,
  CedarlingAuthorizationError,
} from "./errors/types.js";
