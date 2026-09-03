import type { JsonObject } from "../values/types.js";
import type {
  JWT_ALGORITHMS,
  LOG_LEVELS,
} from "../helpers/constants.js";

/**
 * Cedarling-managed remote policy-store source.
 *
 * Cedarling owns retrieval, response-size enforcement, JSON/archive format
 * detection, retry behavior, and optional background refresh. URLs must be
 * absolute HTTPS or loopback HTTP without credentials. A zero refresh interval
 * disables refresh; other values must be at least five seconds.
 */
export interface UrlPolicyStoreSource {
  readonly type: "url";
  readonly url: string | URL;
  readonly refresh?: {
    readonly intervalSeconds: number;
  };
}

/**
 * Policy source accepted by {@link createCedarling}: a managed URL, detached
 * inline document, copied archive bytes, or one-shot archive loader.
 */
export type PolicyStoreSource =
  | UrlPolicyStoreSource
  | {
      readonly type: "inline";
      readonly document: JsonObject;
    }
  | {
      readonly type: "archive";
      readonly bytes: Uint8Array;
    }
  | {
      readonly type: "loader";
      readonly load: () => Promise<Uint8Array>;
    };

export type LogLevel = (typeof LOG_LEVELS)[number];

/**
 * Logging destination and retention limits. Zero memory limits remove the
 * corresponding cap.
 */
export type LoggingOptions =
  | {
      readonly type: "off";
    }
  | {
      readonly type: "memory";
      readonly level?: LogLevel;
      readonly ttlSeconds?: number;
      readonly maxItems?: number;
      readonly maxItemSizeBytes?: number;
    }
  | {
      readonly type: "console";
      readonly level?: LogLevel;
    };

/** Authorization controls with an explicit, dangerous schema-validation bypass. */
export interface AuthorizationOptions {
  readonly dangerouslyDisableSchemaValidation?: boolean;
  readonly decisionLogTokenIdClaim?: string;
}

export interface ContextStoreOptions {
  readonly maxEntries?: number;
  readonly maxEntrySizeBytes?: number;
  readonly defaultTtlSeconds?: number;
  readonly maxTtlSeconds?: number;
  readonly metrics?: boolean;
  readonly memoryAlertThresholdPercent?: number;
}

export type JwtAlgorithm = (typeof JWT_ALGORITHMS)[number];

/**
 * JWT verification, algorithm, and refresh controls.
 *
 * The dangerous flags explicitly bypass signature or status verification.
 * Production applications should use the smallest algorithm allowlist required
 * by their trusted issuers.
 */
export interface JwtValidationOptions {
  readonly dangerouslyDisableSignatureValidation?: boolean;
  readonly dangerouslyDisableStatusValidation?: boolean;
  readonly allowedAlgorithms?: readonly JwtAlgorithm[];
  readonly jwksRefreshIntervalSeconds?: number;
  readonly jwksRefreshMinIntervalSeconds?: number;
  readonly statusListRefreshMaxSeconds?: number;
}

/**
 * Explicitly dangerous diagnostics intended only for local development.
 *
 * Raw causes can contain tokens, policy material, URLs, filesystem paths, or
 * other secrets produced by Cedarling, WebAssembly, loaders, and runtimes.
 * Enabling exposure places the original failure in non-enumerable
 * `error.cause`; never disclose it without redaction.
 */
export interface CedarlingDebugOptions {
  readonly dangerouslyExposeRawErrors?: boolean;
}

export interface TokenCacheOptions {
  readonly maxTtlSeconds?: number;
  readonly capacity?: number;
  readonly evictEarliestExpiration?: boolean;
}

export interface IssuerLoadingOptions {
  readonly mode?: "sync" | "async";
  readonly workers?: number;
}

export interface HttpOptions {
  readonly maxRetries?: number;
  readonly retryDelaySeconds?: number;
  readonly maxResponseSizeBytes?: number;
}

export interface LockOptions {
  readonly configurationUrl: string | URL;
  readonly ssaJwt?: string;
  readonly logIntervalSeconds?: number;
  readonly healthIntervalSeconds?: number;
  readonly telemetryIntervalSeconds?: number;
  readonly logChannelCapacity?: number;
  readonly logMaxRetries?: number;
}

export interface CedarlingBaseOptions {
  readonly applicationName: string;
  readonly logging?: LoggingOptions;
  readonly authorization?: AuthorizationOptions;
  readonly contextStore?: ContextStoreOptions;
  readonly jwt?: JwtValidationOptions;
  readonly tokenCache?: TokenCacheOptions;
  readonly issuerLoading?: IssuerLoadingOptions;
  readonly http?: HttpOptions;
  readonly lock?: LockOptions;
}

type WebNativeOptionFields = CedarlingBaseOptions & {
  readonly policyStore: PolicyStoreSource;
};

type CedarlingDebugOptionFields = {
  readonly debug?: CedarlingDebugOptions;
};

/** Curated, runtime-portable initialization options with one policy source. */
export type WebNativeCedarlingOptions = WebNativeOptionFields &
  CedarlingDebugOptionFields & {
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
export type RawBootstrapCedarlingOptions = CedarlingDebugOptionFields & {
  readonly bootstrapProperties: JsonObject;
} & {
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
