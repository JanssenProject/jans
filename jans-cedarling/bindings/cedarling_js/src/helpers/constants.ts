/**
 * Canonical fixed values used across the Cedarling JavaScript SDK.
 *
 * Maintainers should update allowlists, defaults, numeric limits, fixed field
 * sets, and stable validation/error metadata here so runtime validation and
 * public types cannot silently drift across feature Modules.
 */
import type {
  CedarlingErrorCode,
  ValidationIssueCode,
} from "../errors/types.js";

/** Largest integer representable by generated unsigned 32-bit fields. */
export const UINT32_MAX = 4_294_967_295;

/** Largest integer the JavaScript number Interface represents exactly. */
export const JS_SAFE_U64_MAX = Number.MAX_SAFE_INTEGER;

/** Typed configuration defaults mirrored from Cedarling core behavior. */
export const DEFAULTS = Object.freeze({
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
  validation: {
    fallbackIssueCode: "type",
    undefinedStringCode: "required",
  },
} as const);

/** Descriptor handling policies shared by feature input validators. */
export const FIELD_BEHAVIORS = Object.freeze({
  rejectAccessors: {
    accessor: "alwaysInvalid",
  },
  strictEnumerableData: {
    accessor: "alwaysInvalid",
    nonEnumerableData: "invalid",
  },
} as const);

/** Shared inclusive validation limits for configurable numeric fields. */
export const LIMITS = Object.freeze({
  httpMaxRetries: 31,
  httpStatus: { minimum: 100, maximum: 599 },
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

/** Generated log categories exposed by the stable SDK Interface. */
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
export const INPUT_FIELDS = Object.freeze({
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
  policyArchive: ["type", "bytes"],
  policyInline: ["type", "document"],
  policyLoader: ["type", "load"],
  policyRefresh: ["intervalSeconds"],
  policyUrl: ["type", "url", "refresh"],
  rawBootstrap: ["bootstrapProperties"],
  tokenCache: ["maxTtlSeconds", "capacity", "evictEarliestExpiration"],
  webNativeOptions: [
    "applicationName",
    "policyStore",
    "logging",
    "authorization",
    "contextStore",
    "jwt",
    "tokenCache",
    "issuerLoading",
    "http",
    "lock",
  ],
} as const);

/** Cedar action grammar shared by both accepted public representations. */
export const CEDAR_IDENTIFIER_PATTERN = /^[A-Za-z_][A-Za-z0-9_]*$/u;
export const FORMAL_ACTION_PATTERN =
  /^(?:[A-Za-z_][A-Za-z0-9_]*::)*Action::("(?:[^"\\\u0000-\u001F]|\\(?:["\\/bfnrt]|u[0-9A-Fa-f]{4}))*")$/u;

/** Generated envelope fields excluded from normalized public log payloads. */
export const LOG_ENVELOPE_FIELD_SET: ReadonlySet<string> = new Set([
  "id",
  "request_id",
  "timestamp",
  "log_kind",
  "level",
  "pdp_id",
  "application_id",
]);

/** Stable public messages assigned during SDK error normalization. */
export const ERROR_MESSAGES: Readonly<Record<CedarlingErrorCode, string>> = {
  INVALID_INPUT: "Cedarling input validation failed.",
  UNSUPPORTED_RUNTIME_CAPABILITY:
    "The runtime does not support a required Cedarling capability.",
  WASM_LOAD_FAILED: "The Cedarling WebAssembly module could not be loaded.",
  POLICY_LOADER_FAILED: "The application policy loader failed.",
  INITIALIZATION_FAILED: "Cedarling initialization failed.",
  AUTHORIZATION_FAILED: "Cedarling authorization failed.",
  LOG_STORAGE_UNAVAILABLE: "Cedarling log storage is unavailable.",
  LOG_OPERATION_FAILED: "The Cedarling log operation failed.",
  CONTEXT_OPERATION_FAILED: "The Cedarling context operation failed.",
  ISSUER_OPERATION_FAILED: "The Cedarling issuer operation failed.",
  CLIENT_CLOSED: "The Cedarling client is closed.",
  LIFECYCLE_FAILED: "The Cedarling lifecycle operation failed.",
  RESULT_CONVERSION_FAILED: "A Cedarling result could not be converted.",
  GENERATED_PROTOCOL_ERROR: "The Cedarling generated protocol is incompatible.",
};

export const VALIDATION_ISSUE_MESSAGES: Readonly<
  Record<ValidationIssueCode, string>
> = {
  required: "A required value is missing.",
  type: "The value has an invalid type.",
  format: "The value has an invalid format.",
  range: "The value is outside the supported range.",
  unknownField: "The field is not supported.",
  conflict: "The value conflicts with another field.",
  unsupported: "The value is not supported.",
};

/** Fixed allowlist and patterns for safely serialized diagnostic metadata. */
export const SAFE_DETAIL_FIELDS = [
  "runtimeCapability",
  "sourceType",
  "requestId",
  "wasmMessage",
] as const;
export const SAFE_DETAIL_STRING_PATTERN = /^[A-Za-z][A-Za-z0-9._-]{0,79}$/u;
export const SAFE_PATH_SEGMENT_PATTERN = /^[A-Za-z0-9_-]{1,80}$/u;

/** Engine failures that may already be normalized below the client facade. */
export const GENERATED_OPERATION_ERROR_CODES = [
  "RESULT_CONVERSION_FAILED",
  "GENERATED_PROTOCOL_ERROR",
] as const;

export type OperationErrorPolicy = readonly [
  CedarlingErrorCode,
  ...CedarlingErrorCode[],
];

export const OPERATION_ERROR_POLICIES = Object.freeze({
  authorization: [
    "AUTHORIZATION_FAILED",
    "CLIENT_CLOSED",
    ...GENERATED_OPERATION_ERROR_CODES,
  ],
  context: ["CONTEXT_OPERATION_FAILED", ...GENERATED_OPERATION_ERROR_CODES],
  initialization: [
    "INITIALIZATION_FAILED",
    "UNSUPPORTED_RUNTIME_CAPABILITY",
    "WASM_LOAD_FAILED",
    "POLICY_LOADER_FAILED",
    ...GENERATED_OPERATION_ERROR_CODES,
  ],
  issuer: ["ISSUER_OPERATION_FAILED", ...GENERATED_OPERATION_ERROR_CODES],
  lifecycle: ["LIFECYCLE_FAILED", ...GENERATED_OPERATION_ERROR_CODES],
  log: [
    "LOG_OPERATION_FAILED",
    "LOG_STORAGE_UNAVAILABLE",
    ...GENERATED_OPERATION_ERROR_CODES,
  ],
} as const satisfies Readonly<Record<string, OperationErrorPolicy>>);
