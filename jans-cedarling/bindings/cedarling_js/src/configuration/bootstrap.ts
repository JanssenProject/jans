import {
  DEFAULTS,
  INPUT_FIELDS,
  JS_SAFE_U64_MAX,
  JWT_ALGORITHMS,
  LIMITS,
  LOG_LEVEL_SET,
  UINT32_MAX,
} from "../helpers/constants.js";
import { errorCode } from "../errors/types.js";
import {
  field,
  httpUrl,
  integer,
  invalid,
  jwtAlgorithms,
  optionalBoolean,
  optionalInteger,
  record,
  refreshInterval,
  rejectUnknown,
  requiredString,
} from "./validation.js";

/** Validates one configuration section and owns its field paths. */
function createSectionReader<Field extends string>(
  value: unknown,
  section: string,
  allowed?: readonly Field[],
) {
  const options = value === undefined ? {} : record(value, [section]);
  if (allowed !== undefined) {
    rejectUnknown(options, allowed, [section]);
  }
  const path = (name: Field) => [section, name] as const;
  const read = (name: Field) => field(options, name, [section]);

  return Object.freeze({
    options,
    read,
    integer(
      name: Field,
      fallback: number,
      minimum: number,
      maximum: number,
    ): number {
      return integer(read(name), fallback, minimum, maximum, path(name));
    },
    optionalInteger(
      name: Field,
      minimum: number,
      maximum: number,
    ): number | undefined {
      return optionalInteger(read(name), minimum, maximum, path(name));
    },
    boolean(name: Field, fallback: boolean): boolean {
      return optionalBoolean(read(name), fallback, path(name));
    },
  });
}

function applyLogging(value: unknown, bootstrap: Record<string, unknown>): void {
  if (value === undefined) {
    bootstrap.CEDARLING_LOG_TYPE = DEFAULTS.logging.type;
    bootstrap.CEDARLING_LOG_LEVEL = DEFAULTS.logging.level.toUpperCase();
    return;
  }

  const section = createSectionReader(value, "logging");
  const type = section.read("type");
  if (type === "off") {
    rejectUnknown(section.options, INPUT_FIELDS.loggingOff, ["logging"]);
    bootstrap.CEDARLING_LOG_TYPE = DEFAULTS.logging.type;
    bootstrap.CEDARLING_LOG_LEVEL = DEFAULTS.logging.level.toUpperCase();
    return;
  }

  if (type === "console") {
    rejectUnknown(section.options, INPUT_FIELDS.loggingConsole, ["logging"]);
    bootstrap.CEDARLING_LOG_TYPE = "std_out";
  } else if (type === "memory") {
    rejectUnknown(section.options, INPUT_FIELDS.loggingMemory, ["logging"]);
    bootstrap.CEDARLING_LOG_TYPE = "memory";
    bootstrap.CEDARLING_LOG_TTL = section.integer(
      "ttlSeconds",
      DEFAULTS.logging.ttlSeconds,
      LIMITS.loggingTtlSeconds.minimum,
      LIMITS.loggingTtlSeconds.maximum,
    );
    bootstrap.CEDARLING_LOG_MAX_ITEMS = section.integer(
      "maxItems",
      DEFAULTS.logging.maxItems,
      LIMITS.unsignedInteger.minimum,
      UINT32_MAX,
    );
    bootstrap.CEDARLING_LOG_MAX_ITEM_SIZE = section.integer(
      "maxItemSizeBytes",
      DEFAULTS.logging.maxItemSizeBytes,
      LIMITS.unsignedInteger.minimum,
      UINT32_MAX,
    );
  } else {
    invalid(
      type === undefined
        ? errorCode.inputRequired
        : errorCode.inputUnsupported,
      ["logging", "type"],
    );
  }

  const configuredLevel = section.read("level");
  const levelValue = configuredLevel === undefined
    ? DEFAULTS.logging.level
    : configuredLevel;
  const level = typeof levelValue === "string"
    ? levelValue
    : invalid(errorCode.inputUnsupported, ["logging", "level"]);
  if (!LOG_LEVEL_SET.has(level)) {
    invalid(errorCode.inputUnsupported, ["logging", "level"]);
  }
  bootstrap.CEDARLING_LOG_LEVEL = level.toUpperCase();
}

function applyAuthorization(
  value: unknown,
  bootstrap: Record<string, unknown>,
): void {
  const section = createSectionReader(
    value,
    "authorization",
    INPUT_FIELDS.authorization,
  );
  bootstrap.CEDARLING_STRICT_SCHEMA_VALIDATION = section.boolean(
    "dangerouslyDisableSchemaValidation",
    DEFAULTS.authorization.disableSchemaValidation,
  )
    ? "disabled"
    : "enabled";
  const decisionLogTokenIdClaim = section.read("decisionLogTokenIdClaim");
  bootstrap.CEDARLING_DECISION_LOG_DEFAULT_JWT_ID = requiredString(
    decisionLogTokenIdClaim === undefined
      ? DEFAULTS.authorization.decisionLogTokenIdClaim
      : decisionLogTokenIdClaim,
    ["authorization", "decisionLogTokenIdClaim"],
  );
}

function applyContextStore(
  value: unknown,
  bootstrap: Record<string, unknown>,
): void {
  const section = createSectionReader(
    value,
    "contextStore",
    INPUT_FIELDS.contextStore,
  );
  bootstrap.CEDARLING_DATA_STORE_MAX_ENTRIES = section.integer(
    "maxEntries",
    DEFAULTS.contextStore.maxEntries,
    LIMITS.unsignedInteger.minimum,
    UINT32_MAX,
  );
  bootstrap.CEDARLING_DATA_STORE_MAX_ENTRY_SIZE = section.integer(
    "maxEntrySizeBytes",
    DEFAULTS.contextStore.maxEntrySizeBytes,
    LIMITS.unsignedInteger.minimum,
    UINT32_MAX,
  );

  const maxTtl = section.integer(
    "maxTtlSeconds",
    DEFAULTS.contextStore.maxTtlSeconds,
    LIMITS.positiveInteger.minimum,
    JS_SAFE_U64_MAX,
  );
  bootstrap.CEDARLING_DATA_STORE_MAX_TTL = maxTtl;

  const defaultTtl = section.optionalInteger(
    "defaultTtlSeconds",
    LIMITS.positiveInteger.minimum,
    JS_SAFE_U64_MAX,
  );
  if (defaultTtl !== undefined) {
    if (defaultTtl > maxTtl) {
      invalid(errorCode.inputConflict, [
        "contextStore",
        "defaultTtlSeconds",
      ]);
    }
    bootstrap.CEDARLING_DATA_STORE_DEFAULT_TTL = defaultTtl;
  }
  bootstrap.CEDARLING_DATA_STORE_ENABLE_METRICS = section.boolean(
    "metrics",
    DEFAULTS.contextStore.metrics,
  );

  const configuredThreshold = section.read("memoryAlertThresholdPercent");
  const threshold = configuredThreshold === undefined
    ? DEFAULTS.contextStore.memoryAlertThresholdPercent
    : configuredThreshold;
  if (
    typeof threshold !== "number" ||
    !Number.isFinite(threshold) ||
    threshold < LIMITS.memoryAlertThresholdPercent.minimum ||
    threshold > LIMITS.memoryAlertThresholdPercent.maximum
  ) {
    invalid(errorCode.inputOutOfRange, [
      "contextStore",
      "memoryAlertThresholdPercent",
    ]);
  }
  bootstrap.CEDARLING_DATA_STORE_MEMORY_ALERT_THRESHOLD = threshold;
}

function applyJwt(value: unknown, bootstrap: Record<string, unknown>): void {
  const section = createSectionReader(value, "jwt", INPUT_FIELDS.jwt);
  bootstrap.CEDARLING_JWT_SIG_VALIDATION = section.boolean(
    "dangerouslyDisableSignatureValidation",
    DEFAULTS.jwt.disableSignatureValidation,
  )
    ? "disabled"
    : "enabled";
  bootstrap.CEDARLING_JWT_STATUS_VALIDATION = section.boolean(
    "dangerouslyDisableStatusValidation",
    DEFAULTS.jwt.disableStatusValidation,
  )
    ? "disabled"
    : "enabled";

  const algorithms = section.read("allowedAlgorithms");
  bootstrap.CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED = Object.freeze(
    algorithms === undefined
      ? [...JWT_ALGORITHMS]
      : [...jwtAlgorithms(algorithms, ["jwt", "allowedAlgorithms"])],
  );

  const refresh = refreshInterval(
    section.read("jwksRefreshIntervalSeconds"),
    undefined,
    ["jwt", "jwksRefreshIntervalSeconds"],
  );
  if (refresh !== undefined) {
    if (refresh === 0) {
      invalid(errorCode.inputOutOfRange, [
        "jwt",
        "jwksRefreshIntervalSeconds",
      ]);
    }
    bootstrap.CEDARLING_JWKS_REFRESH_INTERVAL = refresh;
  }

  const minimumRefresh = refreshInterval(
    section.read("jwksRefreshMinIntervalSeconds"),
    DEFAULTS.jwt.jwksRefreshMinIntervalSeconds,
    ["jwt", "jwksRefreshMinIntervalSeconds"],
  );
  if (minimumRefresh === 0) {
    invalid(errorCode.inputOutOfRange, [
      "jwt",
      "jwksRefreshMinIntervalSeconds",
    ]);
  }
  bootstrap.CEDARLING_JWKS_REFRESH_MIN_INTERVAL = minimumRefresh;

  const status = refreshInterval(
    section.read("statusListRefreshMaxSeconds"),
    DEFAULTS.jwt.statusListRefreshMaxSeconds,
    ["jwt", "statusListRefreshMaxSeconds"],
  );
  if (status === 0) {
    invalid(errorCode.inputOutOfRange, [
      "jwt",
      "statusListRefreshMaxSeconds",
    ]);
  }
  bootstrap.CEDARLING_JWT_STATUS_LIST_REFRESH_INTERVAL_MAX = status;
}

function applyTokenCache(
  value: unknown,
  bootstrap: Record<string, unknown>,
): void {
  const section = createSectionReader(
    value,
    "tokenCache",
    INPUT_FIELDS.tokenCache,
  );
  bootstrap.CEDARLING_TOKEN_CACHE_MAX_TTL = section.integer(
    "maxTtlSeconds",
    DEFAULTS.tokenCache.maxTtlSeconds,
    LIMITS.unsignedInteger.minimum,
    UINT32_MAX,
  );
  bootstrap.CEDARLING_TOKEN_CACHE_CAPACITY = section.integer(
    "capacity",
    DEFAULTS.tokenCache.capacity,
    LIMITS.unsignedInteger.minimum,
    UINT32_MAX,
  );
  bootstrap.CEDARLING_TOKEN_CACHE_EARLIEST_EXPIRATION_EVICTION =
    section.boolean(
      "evictEarliestExpiration",
      DEFAULTS.tokenCache.evictEarliestExpiration,
    );
}

function applyIssuerLoading(
  value: unknown,
  bootstrap: Record<string, unknown>,
): void {
  const section = createSectionReader(
    value,
    "issuerLoading",
    INPUT_FIELDS.issuerLoading,
  );
  const configuredMode = section.read("mode");
  const modeValue = configuredMode === undefined
    ? DEFAULTS.issuerLoading.mode
    : configuredMode;
  const mode = typeof modeValue === "string"
    ? modeValue
    : invalid(errorCode.inputUnsupported, ["issuerLoading", "mode"]);
  if (mode !== "sync" && mode !== "async") {
    invalid(errorCode.inputUnsupported, ["issuerLoading", "mode"]);
  }
  bootstrap.CEDARLING_TRUSTED_ISSUER_LOADER_TYPE = mode.toUpperCase();
  bootstrap.CEDARLING_TRUSTED_ISSUER_LOADER_WORKERS = section.integer(
    "workers",
    DEFAULTS.issuerLoading.workers,
    LIMITS.issuerLoadingWorkers.minimum,
    LIMITS.issuerLoadingWorkers.maximum,
  );
}

function applyHttp(value: unknown, bootstrap: Record<string, unknown>): void {
  const section = createSectionReader(value, "http", INPUT_FIELDS.http);
  bootstrap.CEDARLING_HTTP_REQUEST_MAX_RETRIES = section.integer(
    "maxRetries",
    DEFAULTS.http.maxRetries,
    LIMITS.unsignedInteger.minimum,
    LIMITS.httpMaxRetries,
  );
  bootstrap.CEDARLING_HTTP_REQUEST_RETRY_DELAY = section.integer(
    "retryDelaySeconds",
    DEFAULTS.http.retryDelaySeconds,
    LIMITS.unsignedInteger.minimum,
    UINT32_MAX,
  );
  bootstrap.CEDARLING_HTTP_MAX_RESPONSE_SIZE_BYTES = section.integer(
    "maxResponseSizeBytes",
    DEFAULTS.http.maxResponseSizeBytes,
    LIMITS.unsignedInteger.minimum,
    JS_SAFE_U64_MAX,
  );
}

function applyLock(value: unknown, bootstrap: Record<string, unknown>): void {
  if (value === undefined) {
    bootstrap.CEDARLING_LOCK = "disabled";
    return;
  }

  const section = createSectionReader(value, "lock", INPUT_FIELDS.lock);
  bootstrap.CEDARLING_LOCK = "enabled";
  bootstrap.CEDARLING_LOCK_SERVER_CONFIGURATION_URI = httpUrl(
    section.read("configurationUrl"),
    ["lock", "configurationUrl"],
  );

  const ssaJwt = section.read("ssaJwt");
  if (ssaJwt !== undefined) {
    bootstrap.CEDARLING_LOCK_SSA_JWT = requiredString(ssaJwt, [
      "lock",
      "ssaJwt",
    ]);
  }

  bootstrap.CEDARLING_LOCK_LOG_INTERVAL = section.integer(
    "logIntervalSeconds",
    DEFAULTS.lock.logIntervalSeconds,
    LIMITS.unsignedInteger.minimum,
    JS_SAFE_U64_MAX,
  );
  bootstrap.CEDARLING_LOCK_HEALTH_INTERVAL = section.integer(
    "healthIntervalSeconds",
    DEFAULTS.lock.healthIntervalSeconds,
    LIMITS.unsignedInteger.minimum,
    JS_SAFE_U64_MAX,
  );
  bootstrap.CEDARLING_LOCK_TELEMETRY_INTERVAL = section.integer(
    "telemetryIntervalSeconds",
    DEFAULTS.lock.telemetryIntervalSeconds,
    LIMITS.unsignedInteger.minimum,
    JS_SAFE_U64_MAX,
  );
  bootstrap.CEDARLING_LOCK_LOG_CHANNEL_CAPACITY = section.integer(
    "logChannelCapacity",
    DEFAULTS.lock.logChannelCapacity,
    LIMITS.positiveInteger.minimum,
    UINT32_MAX,
  );
  bootstrap.CEDARLING_LOCK_LOG_MAX_RETRIES = section.integer(
    "logMaxRetries",
    DEFAULTS.lock.logMaxRetries,
    LIMITS.unsignedInteger.minimum,
    LIMITS.lockMaxRetries,
  );
}

export function applyTypedBootstrap(
  options: Record<string, unknown>,
  bootstrap: Record<string, unknown>,
): void {
  applyLogging(field(options, "logging", []), bootstrap);
  applyAuthorization(field(options, "authorization", []), bootstrap);
  applyContextStore(field(options, "contextStore", []), bootstrap);
  applyJwt(field(options, "jwt", []), bootstrap);
  applyTokenCache(field(options, "tokenCache", []), bootstrap);
  applyIssuerLoading(field(options, "issuerLoading", []), bootstrap);
  applyHttp(field(options, "http", []), bootstrap);
  applyLock(field(options, "lock", []), bootstrap);
}
