import type {
  CedarlingOptions,
  JwtAlgorithm,
} from "./types.js";
import type { JsonObject } from "../values/types.js";
import {
  DEFAULTS,
  INPUT_FIELDS,
  JS_SAFE_U64_MAX,
  JWT_ALGORITHMS,
  JWT_ALGORITHM_SET,
  LIMITS,
  LOG_LEVEL_SET,
  UINT32_MAX,
} from "../helpers/constants.js";
import {
  createInputValidator,
  isSafeIntegerInRange,
} from "../helpers/validation.js";
import {
  snapshotJsonObject,
  snapshotJsonValue,
} from "../values/snapshot.js";

const {
  exactFields: rejectUnknown,
  field,
  invalid,
  record,
  requiredString,
} = createInputValidator("invalid option", {
  stringNormalization: "trim",
});

/** Detached policy source selected after public option validation. */
type PreparedPolicySource =
  | { readonly type: "inline"; readonly document: JsonObject }
  | { readonly type: "url"; readonly url: string }
  | { readonly type: "archive"; readonly bytes: Uint8Array }
  | { readonly type: "loader"; readonly load: () => Promise<Uint8Array> }
  | { readonly type: "bootstrap" };

/** Client behavior derived once from validated or detached configuration. */
export interface PreparedClientCapabilities {
  /** Whether retained-log queries have memory storage to inspect. */
  readonly memoryLogging: boolean;

  /** Largest explicit context-data lifetime supported by the core config. */
  readonly contextMaxTtlSeconds: number;
}

/** Private configuration passed to the runtime-independent engine factory. */
export interface PreparedEngineOptions {
  /** Frozen generated-binding bootstrap map containing no SDK field names. */
  readonly bootstrapConfig: Readonly<Record<string, unknown>>;

  /** Detached source retained for adapter-owned preparation and routing. */
  readonly policyStore: PreparedPolicySource;
}

/** Complete private preparation result consumed by the public factory. */
export interface PreparedCedarlingOptions extends PreparedEngineOptions {
  /** Immutable capabilities consumed only by the JavaScript client facade. */
  readonly clientCapabilities: PreparedClientCapabilities;
}

/** Converts a raw core TTL value into the JavaScript client's safe range. */
function contextMaxTtlSeconds(value: unknown): number {
  let candidate = value;

  if (typeof candidate === "string") {
    try {
      candidate = JSON.parse(candidate) as unknown;
    } catch {
      return DEFAULTS.contextStore.maxTtlSeconds;
    }
  }

  if (
    typeof candidate !== "number" ||
    !Number.isInteger(candidate) ||
    candidate < 0
  ) {
    return DEFAULTS.contextStore.maxTtlSeconds;
  }

  return Math.min(candidate, JS_SAFE_U64_MAX);
}

/** Derives immutable client behavior once from the generated bootstrap map. */
function prepareClientCapabilities(
  bootstrap: Readonly<Record<string, unknown>>,
): PreparedClientCapabilities {
  return Object.freeze({
    memoryLogging: bootstrap.CEDARLING_LOG_TYPE === "memory",
    contextMaxTtlSeconds: contextMaxTtlSeconds(
      bootstrap.CEDARLING_DATA_STORE_MAX_TTL,
    ),
  });
}

/** Returns an optional boolean or its versioned default. */
function optionalBoolean(
  value: unknown,
  fallback: boolean,
  path: readonly string[],
): boolean {
  if (value === undefined) {
    return fallback;
  }
  return typeof value === "boolean" ? value : invalid("type", path);
}

/** Validates one safe integer against an inclusive range. */
function integer(
  value: unknown,
  fallback: number,
  minimum: number,
  maximum: number,
  path: readonly string[],
): number {
  if (value === undefined) {
    return fallback;
  }
  if (!isSafeIntegerInRange(value, minimum, maximum)) {
    return invalid("range", path);
  }
  return value;
}

/** Validates an optional safe integer without materializing a default. */
function optionalInteger(
  value: unknown,
  minimum: number,
  maximum: number,
  path: readonly string[],
): number | undefined {
  return value === undefined
    ? undefined
    : integer(value, minimum, minimum, maximum, path);
}

/** Enforces the zero-or-at-least-five refresh interval convention. */
function refreshInterval(
  value: unknown,
  fallback: number | undefined,
  path: readonly string[],
): number | undefined {
  const parsed = optionalInteger(
    value,
    LIMITS.unsignedInteger.minimum,
    JS_SAFE_U64_MAX,
    path,
  );
  const result = parsed ?? fallback;
  if (
    result !== undefined &&
    result !== 0 &&
    result < LIMITS.refreshIntervalSeconds.minimumEnabled
  ) {
    return invalid("range", path);
  }
  return result;
}

/** Reports whether a normalized URL hostname is an exact loopback address. */
function isLoopbackHostname(hostname: string): boolean {
  if (hostname === "localhost" || hostname === "[::1]") {
    return true;
  }

  const octets = hostname.split(".");
  return (
    octets.length === 4 &&
    octets[0] === "127" &&
    octets.every((octet) => /^(?:0|[1-9]\d{0,2})$/u.test(octet)) &&
    octets.every((octet) => Number(octet) <= 255)
  );
}

/** Returns an absolute credential-free HTTPS or loopback HTTP URL string. */
function httpUrl(value: unknown, path: readonly string[]): string {
  if (!(typeof value === "string" || value instanceof URL)) {
    return invalid("type", path);
  }
  let url: URL;
  try {
    url = new URL(value);
  } catch {
    return invalid("format", path);
  }
  if (
    (
      url.protocol !== "https:" &&
      !(url.protocol === "http:" && isLoopbackHostname(url.hostname))
    ) ||
    url.username !== "" ||
    url.password !== ""
  ) {
    return invalid("format", path);
  }
  return url.toString();
}

/** Detaches and validates the configured JWT algorithm allowlist once. */
function jwtAlgorithms(
  value: unknown,
  path: readonly string[],
): readonly JwtAlgorithm[] {
  let snapshot: unknown;
  try {
    snapshot = snapshotJsonValue(value);
  } catch {
    return invalid("format", path);
  }

  if (
    !Array.isArray(snapshot) ||
    snapshot.length === 0 ||
    !snapshot.every(
      (algorithm) =>
        typeof algorithm === "string" &&
        JWT_ALGORITHM_SET.has(algorithm),
    ) ||
    new Set(snapshot).size !== snapshot.length
  ) {
    return invalid("format", path);
  }

  return snapshot as readonly JwtAlgorithm[];
}

/**
 * Validates and detaches exactly one policy source.
 *
 * Inline and URL sources populate Cedarling-owned bootstrap keys. Archive and
 * loader sources retain only the data needed for generated archive loading.
 */
function preparePolicyStore(
  sourceValue: unknown,
  bootstrap: Record<string, unknown>,
): PreparedPolicySource {
  const source = record(sourceValue, ["policyStore"]);
  const type = field(source, "type", ["policyStore"]);
  if (typeof type !== "string") {
    return invalid(type === undefined ? "required" : "type", [
      "policyStore",
      "type",
    ]);
  }

  switch (type) {
    case "inline": {
      rejectUnknown(source, INPUT_FIELDS.policyInline, ["policyStore"]);
      const document = field(source, "document", ["policyStore"]);
      if (document === undefined) {
        invalid("required", ["policyStore", "document"]);
      }
      let snapshot: JsonObject;
      try {
        snapshot = snapshotJsonObject(document);
      } catch {
        return invalid("type", ["policyStore", "document"]);
      }
      bootstrap.CEDARLING_POLICY_STORE_LOCAL = JSON.stringify(snapshot);
      return { type, document: snapshot };
    }
    case "url": {
      rejectUnknown(source, INPUT_FIELDS.policyUrl, ["policyStore"]);
      const url = httpUrl(field(source, "url", ["policyStore"]), [
        "policyStore",
        "url",
      ]);
      const refreshValue = field(source, "refresh", ["policyStore"]);
      const refresh =
        refreshValue === undefined
          ? DEFAULTS.policyRefreshIntervalSeconds
          : (() => {
              const options = record(refreshValue, [
                "policyStore",
                "refresh",
              ]);
              rejectUnknown(
                options,
                INPUT_FIELDS.policyRefresh,
                ["policyStore", "refresh"],
              );
              return refreshInterval(
                field(options, "intervalSeconds", [
                  "policyStore",
                  "refresh",
                ]),
                DEFAULTS.policyRefreshIntervalSeconds,
                ["policyStore", "refresh", "intervalSeconds"],
              ) as number;
            })();
      bootstrap.CEDARLING_POLICY_STORE_URI = url;
      bootstrap.CEDARLING_POLICY_STORE_REFRESH_INTERVAL = refresh;
      return { type, url };
    }
    case "archive": {
      rejectUnknown(source, INPUT_FIELDS.policyArchive, ["policyStore"]);
      const bytes = field(source, "bytes", ["policyStore"]);
      if (!(bytes instanceof Uint8Array)) {
        return invalid("type", ["policyStore", "bytes"]);
      }
      if (bytes.byteLength === 0) {
        return invalid("range", ["policyStore", "bytes"]);
      }
      return { type, bytes: new Uint8Array(bytes) };
    }
    case "loader": {
      rejectUnknown(source, INPUT_FIELDS.policyLoader, ["policyStore"]);
      const load = field(source, "load", ["policyStore"]);
      if (typeof load !== "function") {
        return invalid("type", ["policyStore", "load"]);
      }
      return { type, load: load as () => Promise<Uint8Array> };
    }
    default:
      return invalid("unsupported", ["policyStore", "type"]);
  }
}

/** Applies logging defaults and maps the SDK union to raw bootstrap fields. */
function applyLogging(value: unknown, bootstrap: Record<string, unknown>): void {
  if (value === undefined) {
    bootstrap.CEDARLING_LOG_TYPE = DEFAULTS.logging.type;
    bootstrap.CEDARLING_LOG_LEVEL = DEFAULTS.logging.level.toUpperCase();
    return;
  }
  const options = record(value, ["logging"]);
  const type = field(options, "type", ["logging"]);
  if (type === "off") {
    rejectUnknown(options, INPUT_FIELDS.loggingOff, ["logging"]);
    bootstrap.CEDARLING_LOG_TYPE = DEFAULTS.logging.type;
    bootstrap.CEDARLING_LOG_LEVEL = DEFAULTS.logging.level.toUpperCase();
    return;
  }
  if (type === "console") {
    rejectUnknown(options, INPUT_FIELDS.loggingConsole, ["logging"]);
    bootstrap.CEDARLING_LOG_TYPE = "std_out";
  } else if (type === "memory") {
    rejectUnknown(
      options,
      INPUT_FIELDS.loggingMemory,
      ["logging"],
    );
    bootstrap.CEDARLING_LOG_TYPE = "memory";
    bootstrap.CEDARLING_LOG_TTL = integer(
      field(options, "ttlSeconds", ["logging"]),
      DEFAULTS.logging.ttlSeconds,
      LIMITS.loggingTtlSeconds.minimum,
      LIMITS.loggingTtlSeconds.maximum,
      ["logging", "ttlSeconds"],
    );
    bootstrap.CEDARLING_LOG_MAX_ITEMS = integer(
      field(options, "maxItems", ["logging"]),
      DEFAULTS.logging.maxItems,
      LIMITS.unsignedInteger.minimum,
      UINT32_MAX,
      ["logging", "maxItems"],
    );
    bootstrap.CEDARLING_LOG_MAX_ITEM_SIZE = integer(
      field(options, "maxItemSizeBytes", ["logging"]),
      DEFAULTS.logging.maxItemSizeBytes,
      LIMITS.unsignedInteger.minimum,
      UINT32_MAX,
      ["logging", "maxItemSizeBytes"],
    );
  } else {
    invalid(type === undefined ? "required" : "unsupported", [
      "logging",
      "type",
    ]);
  }
  const levelValue = field(options, "level", ["logging"]) ??
    DEFAULTS.logging.level;
  const level = typeof levelValue === "string"
    ? levelValue
    : invalid("unsupported", ["logging", "level"]);
  if (!LOG_LEVEL_SET.has(level)) {
    invalid("unsupported", ["logging", "level"]);
  }
  bootstrap.CEDARLING_LOG_LEVEL = level.toUpperCase();
}

/** Applies schema-validation and decision-log configuration. */
function applyAuthorization(
  value: unknown,
  bootstrap: Record<string, unknown>,
): void {
  const options =
    value === undefined ? {} : record(value, ["authorization"]);
  rejectUnknown(
    options,
    INPUT_FIELDS.authorization,
    ["authorization"],
  );
  bootstrap.CEDARLING_STRICT_SCHEMA_VALIDATION = optionalBoolean(
    field(options, "dangerouslyDisableSchemaValidation", ["authorization"]),
    DEFAULTS.authorization.disableSchemaValidation,
    ["authorization", "dangerouslyDisableSchemaValidation"],
  )
    ? "disabled"
    : "enabled";
  bootstrap.CEDARLING_DECISION_LOG_DEFAULT_JWT_ID = requiredString(
    field(options, "decisionLogTokenIdClaim", ["authorization"]) ??
      DEFAULTS.authorization.decisionLogTokenIdClaim,
    ["authorization", "decisionLogTokenIdClaim"],
  );
}

/** Applies context-store capacity, TTL, metrics, and alert configuration. */
function applyContextStore(
  value: unknown,
  bootstrap: Record<string, unknown>,
): void {
  const options = value === undefined ? {} : record(value, ["contextStore"]);
  rejectUnknown(
    options,
    INPUT_FIELDS.contextStore,
    ["contextStore"],
  );
  bootstrap.CEDARLING_DATA_STORE_MAX_ENTRIES = integer(
    field(options, "maxEntries", ["contextStore"]),
    DEFAULTS.contextStore.maxEntries,
    LIMITS.unsignedInteger.minimum,
    UINT32_MAX,
    ["contextStore", "maxEntries"],
  );
  bootstrap.CEDARLING_DATA_STORE_MAX_ENTRY_SIZE = integer(
    field(options, "maxEntrySizeBytes", ["contextStore"]),
    DEFAULTS.contextStore.maxEntrySizeBytes,
    LIMITS.unsignedInteger.minimum,
    UINT32_MAX,
    ["contextStore", "maxEntrySizeBytes"],
  );
  const defaultTtl = optionalInteger(
    field(options, "defaultTtlSeconds", ["contextStore"]),
    LIMITS.positiveInteger.minimum,
    JS_SAFE_U64_MAX,
    ["contextStore", "defaultTtlSeconds"],
  );
  if (defaultTtl !== undefined) {
    bootstrap.CEDARLING_DATA_STORE_DEFAULT_TTL = defaultTtl;
  }
  bootstrap.CEDARLING_DATA_STORE_MAX_TTL = integer(
    field(options, "maxTtlSeconds", ["contextStore"]),
    DEFAULTS.contextStore.maxTtlSeconds,
    LIMITS.positiveInteger.minimum,
    JS_SAFE_U64_MAX,
    ["contextStore", "maxTtlSeconds"],
  );
  bootstrap.CEDARLING_DATA_STORE_ENABLE_METRICS = optionalBoolean(
    field(options, "metrics", ["contextStore"]),
    DEFAULTS.contextStore.metrics,
    ["contextStore", "metrics"],
  );
  const threshold =
    field(options, "memoryAlertThresholdPercent", ["contextStore"]) ??
      DEFAULTS.contextStore.memoryAlertThresholdPercent;
  if (
    typeof threshold !== "number" ||
    !Number.isFinite(threshold) ||
    threshold < LIMITS.memoryAlertThresholdPercent.minimum ||
    threshold > LIMITS.memoryAlertThresholdPercent.maximum
  ) {
    invalid("range", ["contextStore", "memoryAlertThresholdPercent"]);
  }
  bootstrap.CEDARLING_DATA_STORE_MEMORY_ALERT_THRESHOLD = threshold;
}

/** Applies JWT verification, algorithm, and refresh configuration. */
function applyJwt(value: unknown, bootstrap: Record<string, unknown>): void {
  const options = value === undefined ? {} : record(value, ["jwt"]);
  rejectUnknown(
    options,
    INPUT_FIELDS.jwt,
    ["jwt"],
  );
  bootstrap.CEDARLING_JWT_SIG_VALIDATION = optionalBoolean(
    field(options, "dangerouslyDisableSignatureValidation", ["jwt"]),
    DEFAULTS.jwt.disableSignatureValidation,
    ["jwt", "dangerouslyDisableSignatureValidation"],
  )
    ? "disabled"
    : "enabled";
  bootstrap.CEDARLING_JWT_STATUS_VALIDATION = optionalBoolean(
    field(options, "dangerouslyDisableStatusValidation", ["jwt"]),
    DEFAULTS.jwt.disableStatusValidation,
    ["jwt", "dangerouslyDisableStatusValidation"],
  )
    ? "disabled"
    : "enabled";

  const algorithms = field(options, "allowedAlgorithms", ["jwt"]);
  if (algorithms === undefined) {
    bootstrap.CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED = [
      ...JWT_ALGORITHMS,
    ];
  } else {
    bootstrap.CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED = [
      ...jwtAlgorithms(algorithms, ["jwt", "allowedAlgorithms"]),
    ];
  }
  const refresh = refreshInterval(
    field(options, "jwksRefreshIntervalSeconds", ["jwt"]),
    undefined,
    ["jwt", "jwksRefreshIntervalSeconds"],
  );
  if (refresh !== undefined) {
    if (refresh === 0) {
      invalid("range", ["jwt", "jwksRefreshIntervalSeconds"]);
    }
    bootstrap.CEDARLING_JWKS_REFRESH_INTERVAL = refresh;
  }
  const minimumRefresh = refreshInterval(
    field(options, "jwksRefreshMinIntervalSeconds", ["jwt"]),
    DEFAULTS.jwt.jwksRefreshMinIntervalSeconds,
    ["jwt", "jwksRefreshMinIntervalSeconds"],
  );
  if (minimumRefresh === 0) {
    invalid("range", ["jwt", "jwksRefreshMinIntervalSeconds"]);
  }
  bootstrap.CEDARLING_JWKS_REFRESH_MIN_INTERVAL = minimumRefresh;
  const status = refreshInterval(
    field(options, "statusListRefreshMaxSeconds", ["jwt"]),
    DEFAULTS.jwt.statusListRefreshMaxSeconds,
    ["jwt", "statusListRefreshMaxSeconds"],
  );
  if (status === 0) {
    invalid("range", ["jwt", "statusListRefreshMaxSeconds"]);
  }
  bootstrap.CEDARLING_JWT_STATUS_LIST_REFRESH_INTERVAL_MAX = status;
}

/** Applies validated-token cache limits and eviction behavior. */
function applyTokenCache(
  value: unknown,
  bootstrap: Record<string, unknown>,
): void {
  const options = value === undefined ? {} : record(value, ["tokenCache"]);
  rejectUnknown(
    options,
    INPUT_FIELDS.tokenCache,
    ["tokenCache"],
  );
  bootstrap.CEDARLING_TOKEN_CACHE_MAX_TTL = integer(
    field(options, "maxTtlSeconds", ["tokenCache"]),
    DEFAULTS.tokenCache.maxTtlSeconds,
    LIMITS.unsignedInteger.minimum,
    UINT32_MAX,
    ["tokenCache", "maxTtlSeconds"],
  );
  bootstrap.CEDARLING_TOKEN_CACHE_CAPACITY = integer(
    field(options, "capacity", ["tokenCache"]),
    DEFAULTS.tokenCache.capacity,
    LIMITS.unsignedInteger.minimum,
    UINT32_MAX,
    ["tokenCache", "capacity"],
  );
  bootstrap.CEDARLING_TOKEN_CACHE_EARLIEST_EXPIRATION_EVICTION =
    optionalBoolean(
      field(options, "evictEarliestExpiration", ["tokenCache"]),
      DEFAULTS.tokenCache.evictEarliestExpiration,
      ["tokenCache", "evictEarliestExpiration"],
    );
}

/** Applies the WASM-bounded trusted-issuer loading configuration. */
function applyIssuerLoading(
  value: unknown,
  bootstrap: Record<string, unknown>,
): void {
  const options =
    value === undefined ? {} : record(value, ["issuerLoading"]);
  rejectUnknown(options, INPUT_FIELDS.issuerLoading, ["issuerLoading"]);
  const modeValue = field(options, "mode", ["issuerLoading"]) ??
    DEFAULTS.issuerLoading.mode;
  const mode = typeof modeValue === "string"
    ? modeValue
    : invalid("unsupported", ["issuerLoading", "mode"]);
  if (mode !== "sync" && mode !== "async") {
    invalid("unsupported", ["issuerLoading", "mode"]);
  }
  bootstrap.CEDARLING_TRUSTED_ISSUER_LOADER_TYPE = mode.toUpperCase();
  bootstrap.CEDARLING_TRUSTED_ISSUER_LOADER_WORKERS = integer(
    field(options, "workers", ["issuerLoading"]),
    DEFAULTS.issuerLoading.workers,
    LIMITS.issuerLoadingWorkers.minimum,
    LIMITS.issuerLoadingWorkers.maximum,
    ["issuerLoading", "workers"],
  );
}

/** Applies shared outbound retry, delay, and response-size limits. */
function applyHttp(value: unknown, bootstrap: Record<string, unknown>): void {
  const options = value === undefined ? {} : record(value, ["http"]);
  rejectUnknown(
    options,
    INPUT_FIELDS.http,
    ["http"],
  );
  bootstrap.CEDARLING_HTTP_REQUEST_MAX_RETRIES = integer(
    field(options, "maxRetries", ["http"]),
    DEFAULTS.http.maxRetries,
    LIMITS.unsignedInteger.minimum,
    LIMITS.httpMaxRetries,
    ["http", "maxRetries"],
  );
  bootstrap.CEDARLING_HTTP_REQUEST_RETRY_DELAY = integer(
    field(options, "retryDelaySeconds", ["http"]),
    DEFAULTS.http.retryDelaySeconds,
    LIMITS.unsignedInteger.minimum,
    UINT32_MAX,
    ["http", "retryDelaySeconds"],
  );
  bootstrap.CEDARLING_HTTP_MAX_RESPONSE_SIZE_BYTES = integer(
    field(options, "maxResponseSizeBytes", ["http"]),
    DEFAULTS.http.maxResponseSizeBytes,
    LIMITS.unsignedInteger.minimum,
    JS_SAFE_U64_MAX,
    ["http", "maxResponseSizeBytes"],
  );
}

/** Enables and maps the supported Lock service configuration subset. */
function applyLock(value: unknown, bootstrap: Record<string, unknown>): void {
  if (value === undefined) {
    bootstrap.CEDARLING_LOCK = "disabled";
    return;
  }
  const options = record(value, ["lock"]);
  rejectUnknown(
    options,
    INPUT_FIELDS.lock,
    ["lock"],
  );
  bootstrap.CEDARLING_LOCK = "enabled";
  bootstrap.CEDARLING_LOCK_SERVER_CONFIGURATION_URI = httpUrl(
    field(options, "configurationUrl", ["lock"]),
    ["lock", "configurationUrl"],
  );
  const ssaJwt = field(options, "ssaJwt", ["lock"]);
  if (ssaJwt !== undefined) {
    bootstrap.CEDARLING_LOCK_SSA_JWT = requiredString(ssaJwt, [
      "lock",
      "ssaJwt",
    ]);
  }
  bootstrap.CEDARLING_LOCK_LOG_INTERVAL = integer(
    field(options, "logIntervalSeconds", ["lock"]),
    DEFAULTS.lock.logIntervalSeconds,
    LIMITS.unsignedInteger.minimum,
    JS_SAFE_U64_MAX,
    ["lock", "logIntervalSeconds"],
  );
  bootstrap.CEDARLING_LOCK_HEALTH_INTERVAL = integer(
    field(options, "healthIntervalSeconds", ["lock"]),
    DEFAULTS.lock.healthIntervalSeconds,
    LIMITS.unsignedInteger.minimum,
    JS_SAFE_U64_MAX,
    ["lock", "healthIntervalSeconds"],
  );
  bootstrap.CEDARLING_LOCK_TELEMETRY_INTERVAL = integer(
    field(options, "telemetryIntervalSeconds", ["lock"]),
    DEFAULTS.lock.telemetryIntervalSeconds,
    LIMITS.unsignedInteger.minimum,
    JS_SAFE_U64_MAX,
    ["lock", "telemetryIntervalSeconds"],
  );
  bootstrap.CEDARLING_LOCK_LOG_CHANNEL_CAPACITY = integer(
    field(options, "logChannelCapacity", ["lock"]),
    DEFAULTS.lock.logChannelCapacity,
    LIMITS.positiveInteger.minimum,
    UINT32_MAX,
    ["lock", "logChannelCapacity"],
  );
  bootstrap.CEDARLING_LOCK_LOG_MAX_RETRIES = integer(
    field(options, "logMaxRetries", ["lock"]),
    DEFAULTS.lock.logMaxRetries,
    LIMITS.unsignedInteger.minimum,
    LIMITS.lockMaxRetries,
    ["lock", "logMaxRetries"],
  );
}

/**
 * Validates, normalizes, and detaches public options before asynchronous work.
 *
 * @param input - Untrusted runtime value typed as the public options contract.
 * @returns Frozen bootstrap data plus one detached adapter policy source.
 */
export function prepareCedarlingOptions(
  input: CedarlingOptions,
): PreparedCedarlingOptions {
  const options = record(input, []);
  const rawBootstrap = field(options, "bootstrapProperties", []);

  if (rawBootstrap !== undefined) {
    rejectUnknown(options, INPUT_FIELDS.rawBootstrap, []);

    let bootstrapConfig: JsonObject;
    try {
      bootstrapConfig = snapshotJsonObject(rawBootstrap);
    } catch {
      return invalid("type", ["bootstrapProperties"]);
    }

    return {
      bootstrapConfig: Object.freeze(bootstrapConfig),
      policyStore: { type: "bootstrap" },
      clientCapabilities: prepareClientCapabilities(bootstrapConfig),
    };
  }

  rejectUnknown(
    options,
    INPUT_FIELDS.webNativeOptions,
    [],
  );
  const bootstrap: Record<string, unknown> = {
    CEDARLING_APPLICATION_NAME: requiredString(
      field(options, "applicationName", []),
      ["applicationName"],
    ),
  };
  const policyStore = preparePolicyStore(
    field(options, "policyStore", []),
    bootstrap,
  );
  applyLogging(field(options, "logging", []), bootstrap);
  applyAuthorization(field(options, "authorization", []), bootstrap);
  applyContextStore(field(options, "contextStore", []), bootstrap);
  applyJwt(field(options, "jwt", []), bootstrap);
  applyTokenCache(field(options, "tokenCache", []), bootstrap);
  applyIssuerLoading(field(options, "issuerLoading", []), bootstrap);
  applyHttp(field(options, "http", []), bootstrap);
  applyLock(field(options, "lock", []), bootstrap);

  return {
    bootstrapConfig: Object.freeze(bootstrap),
    policyStore,
    clientCapabilities: prepareClientCapabilities(bootstrap),
  };
}
