function freezeValues<T extends Record<string, unknown>>(record: T): Readonly<T> {
  for (const value of Object.values(record)) {
    if (typeof value === "object" && value !== null) Object.freeze(value);
  }
  return Object.freeze(record);
}

/**
 * Canonical fixed values used across the Cedarling JavaScript SDK.
 *
 * Maintainers should update allowlists, defaults, numeric limits, fixed field
 * sets, and validation metadata here so runtime validation and public types
 * cannot silently drift across feature Modules. Error codes live exclusively
 * in the error Module.
 */
/** Largest integer representable by generated unsigned 32-bit fields. */
export const UINT32_MAX = 4_294_967_295;

/** Largest integer the JavaScript number Interface represents exactly. */
export const JS_SAFE_U64_MAX = Number.MAX_SAFE_INTEGER;

/** Typed configuration defaults mirrored from Cedarling core behavior. */
export const DEFAULTS = freezeValues({
  authorization: {
    decisionLogTokenIdClaim: "jti",
    disableSchemaValidation: false,
  },
  contextStore: {
    maxEntries: 10_000,
    maxEntrySizeBytes: 1_048_576,
    maxTtlSeconds: 3_600,
    metrics: true,
    memoryAlertThresholdPercent: 80,
  },
  client: {
    exposeRawErrors: false,
    memoryLogging: false,
  },
  http: {
    maxRetries: 3,
    retryDelaySeconds: 3,
    maxResponseSizeBytes: 10_485_760,
  },
  issuerLoading: {
    mode: "sync",
    workers: 2,
  },
  jwt: {
    disableSignatureValidation: false,
    disableStatusValidation: false,
    jwksRefreshMinIntervalSeconds: 30,
    statusListRefreshMaxSeconds: 300,
  },
  lock: {
    logIntervalSeconds: 0,
    healthIntervalSeconds: 0,
    telemetryIntervalSeconds: 0,
    logChannelCapacity: 100,
    logMaxRetries: 5,
  },
  logging: {
    type: "off",
    level: "warn",
    ttlSeconds: 60,
    maxItems: 10_000,
    maxItemSizeBytes: 500_000,
  },
  policyRefreshIntervalSeconds: 0,
  tokenCache: {
    maxTtlSeconds: 5,
    capacity: 100,
    evictEarliestExpiration: true,
  },
} as const);

/** Shared inclusive validation limits for configurable numeric fields. */
export const LIMITS = freezeValues({
  httpMaxRetries: 31,
  issuerLoadingWorkers: { minimum: 1, maximum: 6 },
  lockMaxRetries: 31,
  loggingTtlSeconds: { minimum: 1, maximum: 3_600 },
  memoryAlertThresholdPercent: { minimum: 0, maximum: 100 },
  positiveInteger: { minimum: 1 },
  refreshIntervalSeconds: { minimumEnabled: 5 },
  unsignedInteger: { minimum: 0 },
} as const);

/** JWT signature algorithms accepted by typed configuration. */
export const JWT_ALGORITHMS = [
  "HS256",
  "HS384",
  "HS512",
  "ES256",
  "ES384",
  "RS256",
  "RS384",
  "RS512",
  "PS256",
  "PS384",
  "PS512",
  "EdDSA",
] as const;

/** Log severities accepted by configuration and generated log parsing. */
export const LOG_LEVELS = [
  "trace",
  "debug",
  "info",
  "warn",
  "error",
  "fatal",
] as const;

/** Known generated log categories accepted as retained-log query tags. */
export const LOG_KINDS = ["decision", "system", "metric"] as const;

/** Cedar extension functions accepted by canonical extension markers. */
export const CEDAR_EXTENSION_FUNCTIONS = [
  "decimal",
  "ip",
  "datetime",
  "duration",
] as const;

/** Cedar data types reported by generated context metadata. */
export const CEDAR_DATA_TYPES = [
  "string",
  "long",
  "bool",
  "set",
  "record",
  "entity",
  "ip",
  "decimal",
  "datetime",
  "duration",
] as const;

/** Lookup sets derived from canonical literal lists. */
export const JWT_ALGORITHM_SET: ReadonlySet<string> = new Set(JWT_ALGORITHMS);
export const LOG_LEVEL_SET: ReadonlySet<string> = new Set(LOG_LEVELS);
export const LOG_KIND_SET: ReadonlySet<string> = new Set(LOG_KINDS);
export const LOG_TAG_SET: ReadonlySet<string> = new Set([
  ...LOG_KINDS,
  ...LOG_LEVELS,
]);
export const CEDAR_EXTENSION_FUNCTION_SET: ReadonlySet<string> = new Set(
  CEDAR_EXTENSION_FUNCTIONS,
);
export const CEDAR_DATA_TYPE_SET: ReadonlySet<string> = new Set(
  CEDAR_DATA_TYPES,
);

/** Fixed input fields accepted by feature validators. */
export const INPUT_FIELDS = freezeValues({
  action: ["namespace", "id"],
  authorization: [
    "dangerouslyDisableSchemaValidation",
    "decisionLogTokenIdClaim",
  ],
  contextSet: ["ttlSeconds"],
  contextStore: [
    "maxEntries",
    "maxEntrySizeBytes",
    "defaultTtlSeconds",
    "maxTtlSeconds",
    "metrics",
    "memoryAlertThresholdPercent",
  ],
  http: ["maxRetries", "retryDelaySeconds", "maxResponseSizeBytes"],
  debug: ["dangerouslyExposeRawErrors"],
  entity: ["type", "id", "attributes"],
  issuerLoading: ["mode", "workers"],
  issuerReference: ["id", "iss"],
  jwt: [
    "dangerouslyDisableSignatureValidation",
    "dangerouslyDisableStatusValidation",
    "allowedAlgorithms",
    "jwksRefreshIntervalSeconds",
    "jwksRefreshMinIntervalSeconds",
    "statusListRefreshMaxSeconds",
  ],
  lock: [
    "configurationUrl",
    "ssaJwt",
    "logIntervalSeconds",
    "healthIntervalSeconds",
    "telemetryIntervalSeconds",
    "logChannelCapacity",
    "logMaxRetries",
  ],
  loggingConsole: ["type", "level"],
  loggingMemory: [
    "type",
    "level",
    "ttlSeconds",
    "maxItems",
    "maxItemSizeBytes",
  ],
  loggingOff: ["type"],
  logQuery: ["id", "requestId", "tag"],
  multiIssuerAuthorizationRequest: ["tokens", "action", "resource", "context"],
  policyArchive: ["type", "bytes"],
  policyInline: ["type", "document"],
  policyLoader: ["type", "load"],
  policyRefresh: ["intervalSeconds"],
  policyUrl: ["type", "url", "refresh"],
  rawBootstrap: ["bootstrapProperties", "debug"],
  token: ["mapping", "payload"],
  tokenCache: ["maxTtlSeconds", "capacity", "evictEarliestExpiration"],
  webNativeOptions: [
    "applicationName",
    "policyStore",
    "debug",
    "logging",
    "authorization",
    "contextStore",
    "jwt",
    "tokenCache",
    "issuerLoading",
    "http",
    "lock",
  ],
  unsignedAuthorizationRequest: ["principal", "action", "resource", "context"],
} as const);
