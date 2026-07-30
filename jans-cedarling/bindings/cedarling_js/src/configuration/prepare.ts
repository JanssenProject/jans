import type {
  CedarlingOptions,
  JwtAlgorithm,
} from "./types.js";
import type {
  ValidationIssueCode,
} from "../errors/types.js";
import type { JsonObject } from "../values/types.js";
import { InputValidationError } from "../errors/errors.js";
import {
  snapshotJsonObject,
  snapshotJsonValue,
} from "../values/snapshot.js";
import {
  inspectOwnProperty,
  isPlainDataRecord,
  type DataRecord,
} from "../values/inspect.js";

/** Largest integer representable by the generated unsigned 32-bit fields. */
const UINT32_MAX = 4_294_967_295;

/**
 * Largest unsigned 64-bit value the JavaScript number API can represent
 * without losing integer precision.
 */
const JS_SAFE_U64_MAX = Number.MAX_SAFE_INTEGER;

/** Complete algorithm allowlist supported by the selected Cedarling branch. */
const ALL_ALGORITHMS: readonly JwtAlgorithm[] = [
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
];

/** Detached policy source selected after public option validation. */
export type PreparedPolicySource =
  | { readonly type: "inline"; readonly document: JsonObject }
  | { readonly type: "url"; readonly url: string }
  | { readonly type: "archive"; readonly bytes: Uint8Array }
  | { readonly type: "loader"; readonly load: () => Promise<Uint8Array> }
  | { readonly type: "bootstrap" };

/** Private configuration passed from the public factory to the Web engine. */
export interface PreparedCedarlingOptions {
  /** Frozen generated-binding bootstrap map containing no SDK field names. */
  readonly bootstrapConfig: Readonly<Record<string, unknown>>;

  /** Detached source retained for adapter-owned preparation and routing. */
  readonly policyStore: PreparedPolicySource;
}

/** Raises one SDK-controlled validation issue without retaining input values. */
function invalid(
  code: ValidationIssueCode,
  path: readonly (string | number)[],
): never {
  throw new InputValidationError(code, "invalid option", path);
}

/** Requires a plain object whose enumerable properties are data properties. */
function record(value: unknown, path: readonly string[]): DataRecord {
  if (!isPlainDataRecord(value, false)) {
    return invalid("type", path);
  }
  return value;
}

/** Reads an own enumerable data property without invoking an accessor. */
function field(
  value: DataRecord,
  key: string,
  path: readonly string[],
): unknown {
  const property = inspectOwnProperty(value, key);
  if (property.kind === "missing" || !property.enumerable) {
    return undefined;
  }
  if (property.kind === "accessor") {
    return invalid("type", [...path, key]);
  }
  return property.value;
}

/** Rejects misspelled SDK-owned fields deterministically. */
function rejectUnknown(
  value: DataRecord,
  allowed: readonly string[],
  path: readonly string[],
): void {
  const allowedFields = new Set(allowed);
  for (const key of Object.keys(value)) {
    if (!allowedFields.has(key)) {
      invalid("unknownField", [...path, key]);
    }
  }
}

/** Trims and returns a required non-empty string option. */
function requiredString(
  value: unknown,
  path: readonly string[],
): string {
  if (typeof value !== "string") {
    return invalid(value === undefined ? "required" : "type", path);
  }
  const normalized = value.trim();
  if (normalized.length === 0) {
    return invalid("required", path);
  }
  return normalized;
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
  if (
    typeof value !== "number" ||
    !Number.isSafeInteger(value) ||
    value < minimum ||
    value > maximum
  ) {
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
  const parsed = optionalInteger(value, 0, JS_SAFE_U64_MAX, path);
  const result = parsed ?? fallback;
  if (result !== undefined && result !== 0 && result < 5) {
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
        ALL_ALGORITHMS.includes(algorithm as JwtAlgorithm),
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
      rejectUnknown(source, ["type", "document"], ["policyStore"]);
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
      rejectUnknown(source, ["type", "url", "refresh"], ["policyStore"]);
      const url = httpUrl(field(source, "url", ["policyStore"]), [
        "policyStore",
        "url",
      ]);
      const refreshValue = field(source, "refresh", ["policyStore"]);
      const refresh =
        refreshValue === undefined
          ? 0
          : (() => {
              const options = record(refreshValue, [
                "policyStore",
                "refresh",
              ]);
              rejectUnknown(
                options,
                ["intervalSeconds"],
                ["policyStore", "refresh"],
              );
              return refreshInterval(
                field(options, "intervalSeconds", [
                  "policyStore",
                  "refresh",
                ]),
                0,
                ["policyStore", "refresh", "intervalSeconds"],
              ) as number;
            })();
      bootstrap.CEDARLING_POLICY_STORE_URI = url;
      bootstrap.CEDARLING_POLICY_STORE_REFRESH_INTERVAL = refresh;
      return { type, url };
    }
    case "archive": {
      rejectUnknown(source, ["type", "bytes"], ["policyStore"]);
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
      rejectUnknown(source, ["type", "load"], ["policyStore"]);
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
    bootstrap.CEDARLING_LOG_TYPE = "off";
    bootstrap.CEDARLING_LOG_LEVEL = "WARN";
    return;
  }
  const options = record(value, ["logging"]);
  const type = field(options, "type", ["logging"]);
  if (type === "off") {
    rejectUnknown(options, ["type"], ["logging"]);
    bootstrap.CEDARLING_LOG_TYPE = "off";
    bootstrap.CEDARLING_LOG_LEVEL = "WARN";
    return;
  }
  if (type === "console") {
    rejectUnknown(options, ["type", "level"], ["logging"]);
    bootstrap.CEDARLING_LOG_TYPE = "std_out";
  } else if (type === "memory") {
    rejectUnknown(
      options,
      ["type", "level", "ttlSeconds", "maxItems", "maxItemSizeBytes"],
      ["logging"],
    );
    bootstrap.CEDARLING_LOG_TYPE = "memory";
    bootstrap.CEDARLING_LOG_TTL = integer(
      field(options, "ttlSeconds", ["logging"]),
      60,
      1,
      3_600,
      ["logging", "ttlSeconds"],
    );
    bootstrap.CEDARLING_LOG_MAX_ITEMS = integer(
      field(options, "maxItems", ["logging"]),
      10_000,
      0,
      UINT32_MAX,
      ["logging", "maxItems"],
    );
    bootstrap.CEDARLING_LOG_MAX_ITEM_SIZE = integer(
      field(options, "maxItemSizeBytes", ["logging"]),
      500_000,
      0,
      UINT32_MAX,
      ["logging", "maxItemSizeBytes"],
    );
  } else {
    invalid(type === undefined ? "required" : "unsupported", [
      "logging",
      "type",
    ]);
  }
  const level = field(options, "level", ["logging"]) ?? "warn";
  if (
    typeof level !== "string" ||
    !["trace", "debug", "info", "warn", "error", "fatal"].includes(level)
  ) {
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
    ["dangerouslyDisableSchemaValidation", "decisionLogTokenIdClaim"],
    ["authorization"],
  );
  bootstrap.CEDARLING_STRICT_SCHEMA_VALIDATION = optionalBoolean(
    field(options, "dangerouslyDisableSchemaValidation", ["authorization"]),
    false,
    ["authorization", "dangerouslyDisableSchemaValidation"],
  )
    ? "disabled"
    : "enabled";
  bootstrap.CEDARLING_DECISION_LOG_DEFAULT_JWT_ID = requiredString(
    field(options, "decisionLogTokenIdClaim", ["authorization"]) ?? "jti",
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
    [
      "maxEntries",
      "maxEntrySizeBytes",
      "defaultTtlSeconds",
      "maxTtlSeconds",
      "metrics",
      "memoryAlertThresholdPercent",
    ],
    ["contextStore"],
  );
  bootstrap.CEDARLING_DATA_STORE_MAX_ENTRIES = integer(
    field(options, "maxEntries", ["contextStore"]),
    10_000,
    0,
    UINT32_MAX,
    ["contextStore", "maxEntries"],
  );
  bootstrap.CEDARLING_DATA_STORE_MAX_ENTRY_SIZE = integer(
    field(options, "maxEntrySizeBytes", ["contextStore"]),
    1_048_576,
    0,
    UINT32_MAX,
    ["contextStore", "maxEntrySizeBytes"],
  );
  const defaultTtl = optionalInteger(
    field(options, "defaultTtlSeconds", ["contextStore"]),
    1,
    JS_SAFE_U64_MAX,
    ["contextStore", "defaultTtlSeconds"],
  );
  if (defaultTtl !== undefined) {
    bootstrap.CEDARLING_DATA_STORE_DEFAULT_TTL = defaultTtl;
  }
  bootstrap.CEDARLING_DATA_STORE_MAX_TTL = integer(
    field(options, "maxTtlSeconds", ["contextStore"]),
    3_600,
    1,
    JS_SAFE_U64_MAX,
    ["contextStore", "maxTtlSeconds"],
  );
  bootstrap.CEDARLING_DATA_STORE_ENABLE_METRICS = optionalBoolean(
    field(options, "metrics", ["contextStore"]),
    true,
    ["contextStore", "metrics"],
  );
  const threshold =
    field(options, "memoryAlertThresholdPercent", ["contextStore"]) ?? 80;
  if (
    typeof threshold !== "number" ||
    !Number.isFinite(threshold) ||
    threshold < 0 ||
    threshold > 100
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
    [
      "dangerouslyDisableSignatureValidation",
      "dangerouslyDisableStatusValidation",
      "allowedAlgorithms",
      "jwksRefreshIntervalSeconds",
      "jwksRefreshMinIntervalSeconds",
      "statusListRefreshMaxSeconds",
    ],
    ["jwt"],
  );
  bootstrap.CEDARLING_JWT_SIG_VALIDATION = optionalBoolean(
    field(options, "dangerouslyDisableSignatureValidation", ["jwt"]),
    false,
    ["jwt", "dangerouslyDisableSignatureValidation"],
  )
    ? "disabled"
    : "enabled";
  bootstrap.CEDARLING_JWT_STATUS_VALIDATION = optionalBoolean(
    field(options, "dangerouslyDisableStatusValidation", ["jwt"]),
    false,
    ["jwt", "dangerouslyDisableStatusValidation"],
  )
    ? "disabled"
    : "enabled";

  const algorithms = field(options, "allowedAlgorithms", ["jwt"]);
  if (algorithms === undefined) {
    bootstrap.CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED = [
      ...ALL_ALGORITHMS,
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
    30,
    ["jwt", "jwksRefreshMinIntervalSeconds"],
  );
  if (minimumRefresh === 0) {
    invalid("range", ["jwt", "jwksRefreshMinIntervalSeconds"]);
  }
  bootstrap.CEDARLING_JWKS_REFRESH_MIN_INTERVAL = minimumRefresh;
  const status = refreshInterval(
    field(options, "statusListRefreshMaxSeconds", ["jwt"]),
    300,
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
    ["maxTtlSeconds", "capacity", "evictEarliestExpiration"],
    ["tokenCache"],
  );
  bootstrap.CEDARLING_TOKEN_CACHE_MAX_TTL = integer(
    field(options, "maxTtlSeconds", ["tokenCache"]),
    5,
    0,
    UINT32_MAX,
    ["tokenCache", "maxTtlSeconds"],
  );
  bootstrap.CEDARLING_TOKEN_CACHE_CAPACITY = integer(
    field(options, "capacity", ["tokenCache"]),
    100,
    0,
    UINT32_MAX,
    ["tokenCache", "capacity"],
  );
  bootstrap.CEDARLING_TOKEN_CACHE_EARLIEST_EXPIRATION_EVICTION =
    optionalBoolean(
      field(options, "evictEarliestExpiration", ["tokenCache"]),
      true,
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
  rejectUnknown(options, ["mode", "workers"], ["issuerLoading"]);
  const mode = field(options, "mode", ["issuerLoading"]) ?? "sync";
  if (mode !== "sync" && mode !== "async") {
    invalid("unsupported", ["issuerLoading", "mode"]);
  }
  bootstrap.CEDARLING_TRUSTED_ISSUER_LOADER_TYPE = mode.toUpperCase();
  bootstrap.CEDARLING_TRUSTED_ISSUER_LOADER_WORKERS = integer(
    field(options, "workers", ["issuerLoading"]),
    2,
    1,
    6,
    ["issuerLoading", "workers"],
  );
}

/** Applies shared outbound retry, delay, and response-size limits. */
function applyHttp(value: unknown, bootstrap: Record<string, unknown>): void {
  const options = value === undefined ? {} : record(value, ["http"]);
  rejectUnknown(
    options,
    ["maxRetries", "retryDelaySeconds", "maxResponseSizeBytes"],
    ["http"],
  );
  bootstrap.CEDARLING_HTTP_REQUEST_MAX_RETRIES = integer(
    field(options, "maxRetries", ["http"]),
    3,
    0,
    31,
    ["http", "maxRetries"],
  );
  bootstrap.CEDARLING_HTTP_REQUEST_RETRY_DELAY = integer(
    field(options, "retryDelaySeconds", ["http"]),
    3,
    0,
    UINT32_MAX,
    ["http", "retryDelaySeconds"],
  );
  bootstrap.CEDARLING_HTTP_MAX_RESPONSE_SIZE_BYTES = integer(
    field(options, "maxResponseSizeBytes", ["http"]),
    10_485_760,
    0,
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
    [
      "configurationUrl",
      "ssaJwt",
      "logIntervalSeconds",
      "healthIntervalSeconds",
      "telemetryIntervalSeconds",
      "logChannelCapacity",
      "logMaxRetries",
    ],
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
    0,
    0,
    JS_SAFE_U64_MAX,
    ["lock", "logIntervalSeconds"],
  );
  bootstrap.CEDARLING_LOCK_HEALTH_INTERVAL = integer(
    field(options, "healthIntervalSeconds", ["lock"]),
    0,
    0,
    JS_SAFE_U64_MAX,
    ["lock", "healthIntervalSeconds"],
  );
  bootstrap.CEDARLING_LOCK_TELEMETRY_INTERVAL = integer(
    field(options, "telemetryIntervalSeconds", ["lock"]),
    0,
    0,
    JS_SAFE_U64_MAX,
    ["lock", "telemetryIntervalSeconds"],
  );
  bootstrap.CEDARLING_LOCK_LOG_CHANNEL_CAPACITY = integer(
    field(options, "logChannelCapacity", ["lock"]),
    100,
    1,
    UINT32_MAX,
    ["lock", "logChannelCapacity"],
  );
  bootstrap.CEDARLING_LOCK_LOG_MAX_RETRIES = integer(
    field(options, "logMaxRetries", ["lock"]),
    5,
    0,
    31,
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
    rejectUnknown(options, ["bootstrapProperties"], []);

    let bootstrapConfig: JsonObject;
    try {
      bootstrapConfig = snapshotJsonObject(rawBootstrap);
    } catch {
      return invalid("type", ["bootstrapProperties"]);
    }

    return {
      bootstrapConfig: Object.freeze(bootstrapConfig),
      policyStore: { type: "bootstrap" },
    };
  }

  rejectUnknown(
    options,
    [
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
  };
}
