import type {
  JsonObject,
  PolicyStoreDocument,
} from "../values/types.js";
import type {
  JWT_ALGORITHMS,
  LOG_LEVELS,
} from "../helpers/constants.js";

/**
 * Background refresh behavior owned by a URL policy source.
 *
 * @example
 * ```ts
 * const refresh: PolicyRefreshOptions = { intervalSeconds: 30 };
 * ```
 */
export interface PolicyRefreshOptions {
  /** Zero disables refresh; non-zero values must be at least five seconds. */
  readonly intervalSeconds: number;
}

/**
 * Cedarling-managed remote policy-store source.
 *
 * Cedarling owns retrieval, response-size enforcement, JSON/archive format
 * detection, retry behavior, and optional background refresh.
 *
 * @example
 * ```ts
 * const source: UrlPolicyStoreSource = {
 *   type: "url",
 *   url: "https://policy.example/task-manager.cjar",
 * };
 * ```
 */
export interface UrlPolicyStoreSource {
  /** Selects Cedarling-managed HTTPS or loopback HTTP policy loading. */
  readonly type: "url";

  /** Absolute HTTPS URL, or loopback HTTP URL, without credentials. */
  readonly url: string | URL;

  /** Optional Cedarling-managed refresh for this URL source. */
  readonly refresh?: PolicyRefreshOptions;
}

/**
 * Policy source accepted by {@link createCedarling}.
 *
 * @example
 * ```ts
 * const source: PolicyStoreSource = {
 *   type: "archive",
 *   bytes: new Uint8Array([80, 75]),
 * };
 * ```
 */
export type PolicyStoreSource =
  | UrlPolicyStoreSource
  | {
      /** Selects an in-memory JSON policy document. */
      readonly type: "inline";
      /** Complete policy-store document copied before initialization. */
      readonly document: PolicyStoreDocument;
    }
  | {
      /** Selects in-memory Cedar archive bytes. */
      readonly type: "archive";
      /** Non-empty `.cjar` bytes copied before initialization. */
      readonly bytes: Uint8Array;
    }
  | {
      /** Selects an application-owned asynchronous archive loader. */
      readonly type: "loader";
      /** Returns non-empty `.cjar` bytes and is invoked at most once. */
      readonly load: () => Promise<Uint8Array>;
    };

/**
 * Cedarling log severity.
 *
 * @example
 * ```ts
 * const level: LogLevel = "warn";
 * ```
 */
export type LogLevel = (typeof LOG_LEVELS)[number];

/**
 * Logging destination and memory-store limits.
 *
 * @example
 * ```ts
 * const logging: LoggingOptions = {
 *   type: "memory",
 *   ttlSeconds: 60,
 * };
 * ```
 */
export type LoggingOptions =
  | {
      /** Disables Cedarling logging. */
      readonly type: "off";
    }
  | {
      /** Retains logs in Cedarling memory storage. */
      readonly type: "memory";
      /** Minimum retained log severity. */
      readonly level?: LogLevel;
      /** Positive retention lifetime in seconds. */
      readonly ttlSeconds?: number;
      /** Maximum retained item count; zero removes the count limit. */
      readonly maxItems?: number;
      /** Maximum bytes per retained item; zero removes the size limit. */
      readonly maxItemSizeBytes?: number;
    }
  | {
      /** Writes logs through the generated console logger. */
      readonly type: "console";
      /** Minimum emitted log severity. */
      readonly level?: LogLevel;
    };

/**
 * Authorization-engine configuration.
 *
 * @example
 * ```ts
 * const authorization: AuthorizationOptions = {
 *   decisionLogTokenIdClaim: "jti",
 * };
 * ```
 */
export interface AuthorizationOptions {
  /** Disables Cedar schema validation only when explicitly true. */
  readonly dangerouslyDisableSchemaValidation?: boolean;

  /** Non-empty token claim recorded as the decision identifier. */
  readonly decisionLogTokenIdClaim?: string;
}

/**
 * In-memory context-store limits.
 *
 * @example
 * ```ts
 * const contextStore: ContextStoreOptions = { maxEntries: 10_000 };
 * ```
 */
export interface ContextStoreOptions {
  /** Maximum entry count; zero removes the count limit. */
  readonly maxEntries?: number;
  /** Maximum bytes per entry; zero removes the size limit. */
  readonly maxEntrySizeBytes?: number;
  /** Positive default entry lifetime in seconds. */
  readonly defaultTtlSeconds?: number;
  /** Positive maximum effective entry lifetime in seconds. */
  readonly maxTtlSeconds?: number;
  /** Enables context access counters and statistics. */
  readonly metrics?: boolean;
  /** Percentage at which memory-capacity warnings begin. */
  readonly memoryAlertThresholdPercent?: number;
}

/**
 * JWT signature algorithm accepted by Cedarling.
 *
 * @example
 * ```ts
 * const algorithm: JwtAlgorithm = "ES256";
 * ```
 */
export type JwtAlgorithm = (typeof JWT_ALGORITHMS)[number];

/**
 * JWT validation and refresh behavior.
 *
 * @example
 * ```ts
 * const jwt: JwtValidationOptions = {
 *   allowedAlgorithms: ["ES256", "EdDSA"],
 * };
 * ```
 */
export interface JwtValidationOptions {
  /** Disables JWT signature verification only when explicitly true. */
  readonly dangerouslyDisableSignatureValidation?: boolean;
  /** Disables JWT status verification only when explicitly true. */
  readonly dangerouslyDisableStatusValidation?: boolean;
  /** Non-empty, unique set of accepted signature algorithms. */
  readonly allowedAlgorithms?: readonly JwtAlgorithm[];
  /** Optional periodic JWKS refresh interval in seconds. */
  readonly jwksRefreshIntervalSeconds?: number;
  /** Positive minimum interval between on-demand JWKS fetches. */
  readonly jwksRefreshMinIntervalSeconds?: number;
  /** Positive upper bound for status-list refresh intervals. */
  readonly statusListRefreshMaxSeconds?: number;
}

/**
 * Validated-token cache behavior.
 *
 * @example
 * ```ts
 * const tokenCache: TokenCacheOptions = { maxTtlSeconds: 5 };
 * ```
 */
export interface TokenCacheOptions {
  /** Maximum validated-token lifetime; zero disables the cache. */
  readonly maxTtlSeconds?: number;
  /** Maximum cached token count; zero removes the count limit. */
  readonly capacity?: number;
  /** Evicts the token with the earliest expiration first. */
  readonly evictEarliestExpiration?: boolean;
}

/**
 * Trusted-issuer loading behavior.
 *
 * @example
 * ```ts
 * const issuerLoading: IssuerLoadingOptions = {
 *   mode: "sync",
 *   workers: 2,
 * };
 * ```
 */
export interface IssuerLoadingOptions {
  /** Loads issuers during initialization or in the background. */
  readonly mode?: "sync" | "async";
  /** Concurrent issuer workers in the WASM-supported range. */
  readonly workers?: number;
}

/**
 * Shared outbound HTTP retry and response limits.
 *
 * @example
 * ```ts
 * const http: HttpOptions = { maxRetries: 3 };
 * ```
 */
export interface HttpOptions {
  /** Maximum retry attempts for one outbound request. */
  readonly maxRetries?: number;
  /** Base delay in seconds for retry backoff. */
  readonly retryDelaySeconds?: number;
  /** Maximum response bytes; zero disables the cap. */
  readonly maxResponseSizeBytes?: number;
}

/**
 * Lock service configuration.
 *
 * @example
 * ```ts
 * const lock: LockOptions = {
 *   configurationUrl: "https://lock.example/.well-known/lock-master-configuration",
 * };
 * ```
 */
export interface LockOptions {
  /** Absolute HTTPS, or loopback HTTP, Lock URL without credentials. */
  readonly configurationUrl: string | URL;
  /** Application-provided software statement assertion. */
  readonly ssaJwt?: string;
  /** Lock log publication interval; zero disables publication. */
  readonly logIntervalSeconds?: number;
  /** Lock health publication interval; zero disables publication. */
  readonly healthIntervalSeconds?: number;
  /** Lock telemetry publication interval; zero disables publication. */
  readonly telemetryIntervalSeconds?: number;
  /** Non-zero capacity of the Lock log channel. */
  readonly logChannelCapacity?: number;
  /** Maximum retry attempts for Lock log publication. */
  readonly logMaxRetries?: number;
}

/**
 * Web-native configuration shared by every policy source.
 *
 * @example
 * ```ts
 * const base: CedarlingBaseOptions = {
 *   applicationName: "task-manager",
 * };
 * ```
 */
export interface CedarlingBaseOptions {
  /** Non-empty application identifier used by diagnostics and logs. */
  readonly applicationName: string;
  /** Logging destination and retention limits. */
  readonly logging?: LoggingOptions;
  /** Authorization validation and decision-log behavior. */
  readonly authorization?: AuthorizationOptions;
  /** Context-store capacity and lifetime behavior. */
  readonly contextStore?: ContextStoreOptions;
  /** JWT validation and refresh behavior. */
  readonly jwt?: JwtValidationOptions;
  /** Validated-token cache behavior. */
  readonly tokenCache?: TokenCacheOptions;
  /** Trusted-issuer loading behavior. */
  readonly issuerLoading?: IssuerLoadingOptions;
  /** Shared outbound HTTP limits. */
  readonly http?: HttpOptions;
  /** Optional Lock service configuration. */
  readonly lock?: LockOptions;
}

/**
 * Complete options used to initialize one isolated Cedarling client.
 *
 * @example
 * ```ts
 * const options: CedarlingOptions = {
 *   applicationName: "task-manager",
 *   policyStore: {
 *     type: "inline",
 *     document: policyStoreDocument,
 *   },
 * };
 * ```
 */
type WebNativeOptionFields = CedarlingBaseOptions & {
  /** Exactly one Web-native policy source. */
  readonly policyStore: PolicyStoreSource;
};

/** Curated, runtime-portable initialization options. */
export type WebNativeCedarlingOptions = WebNativeOptionFields & {
  /** Raw bootstrap properties cannot be mixed with Web-native options. */
  readonly bootstrapProperties?: never;
};

/**
 * Advanced initialization using the Cedarling core bootstrap-property
 * contract without SDK-owned property mapping.
 *
 * The Cedarling bootstrap-property documentation remains the source of truth
 * for supported keys and values. Runtime-specific capabilities, such as local
 * filesystem policy sources, are not made portable by this pass-through.
 */
export type RawBootstrapCedarlingOptions = {
  /** Detached JSON-compatible bootstrap properties passed unchanged to core. */
  readonly bootstrapProperties: JsonObject;
} & {
  /** Web-native options cannot be mixed with raw bootstrap properties. */
  readonly [Key in keyof WebNativeOptionFields]?: never;
};

/**
 * Complete input accepted by {@link createCedarling}.
 *
 * Use the Web-native shape for a curated, runtime-portable SDK contract, or
 * the explicit raw shape when cross-binding bootstrap parity is required.
 */
export type CedarlingOptions =
  | WebNativeCedarlingOptions
  | RawBootstrapCedarlingOptions;
