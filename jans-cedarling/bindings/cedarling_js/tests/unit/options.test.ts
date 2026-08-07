import type QUnitApi from "qunit";

import { prepareCedarlingOptions } from "../../dist/configuration/prepare.js";

const inlinePolicy = {
  type: "inline" as const,
  document: {},
};

/** Registers focused option normalization and raw bootstrap mapping tests. */
export default function registerOptionsTests(QUnit: QUnitApi): void {
  QUnit.module("options");

  QUnit.test("safe defaults map to the private generated bootstrap keys", (assert) => {
    const prepared = prepareCedarlingOptions({
      applicationName: "  mapping-contract  ",
      policyStore: inlinePolicy,
    });

    assert.deepEqual(prepared.bootstrapConfig, {
      CEDARLING_APPLICATION_NAME: "mapping-contract",
      CEDARLING_POLICY_STORE_LOCAL: "{}",
      CEDARLING_LOG_TYPE: "off",
      CEDARLING_LOG_LEVEL: "WARN",
      CEDARLING_STRICT_SCHEMA_VALIDATION: "enabled",
      CEDARLING_DECISION_LOG_DEFAULT_JWT_ID: "jti",
      CEDARLING_DATA_STORE_MAX_ENTRIES: 10_000,
      CEDARLING_DATA_STORE_MAX_ENTRY_SIZE: 1_048_576,
      CEDARLING_DATA_STORE_MAX_TTL: 3_600,
      CEDARLING_DATA_STORE_ENABLE_METRICS: true,
      CEDARLING_DATA_STORE_MEMORY_ALERT_THRESHOLD: 80,
      CEDARLING_JWT_SIG_VALIDATION: "enabled",
      CEDARLING_JWT_STATUS_VALIDATION: "enabled",
      CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED: [
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
      ],
      CEDARLING_JWKS_REFRESH_MIN_INTERVAL: 30,
      CEDARLING_JWT_STATUS_LIST_REFRESH_INTERVAL_MAX: 300,
      CEDARLING_TOKEN_CACHE_MAX_TTL: 5,
      CEDARLING_TOKEN_CACHE_CAPACITY: 100,
      CEDARLING_TOKEN_CACHE_EARLIEST_EXPIRATION_EVICTION: true,
      CEDARLING_TRUSTED_ISSUER_LOADER_TYPE: "SYNC",
      CEDARLING_TRUSTED_ISSUER_LOADER_WORKERS: 2,
      CEDARLING_HTTP_REQUEST_MAX_RETRIES: 3,
      CEDARLING_HTTP_REQUEST_RETRY_DELAY: 3,
      CEDARLING_HTTP_MAX_RESPONSE_SIZE_BYTES: 10_485_760,
      CEDARLING_LOCK: "disabled",
    });

    assert.deepEqual(prepared.clientCapabilities, {
      exposeRawErrors: false,
      memoryLogging: false,
      contextMaxTtlSeconds: 3_600,
    });
  });

  QUnit.test("explicit options map without leaking SDK field names", (assert) => {
    const prepared = prepareCedarlingOptions({
      applicationName: "explicit-mapping",
      policyStore: {
        type: "url",
        url: new URL("https://policy.example/store.cjar"),
        refresh: { intervalSeconds: 30 },
      },
      logging: {
        type: "memory",
        level: "debug",
        ttlSeconds: 1,
        maxItems: 0,
        maxItemSizeBytes: 0,
      },
      authorization: {
        dangerouslyDisableSchemaValidation: true,
        decisionLogTokenIdClaim: "sid",
      },
      contextStore: {
        defaultTtlSeconds: 10,
        maxTtlSeconds: 20,
        metrics: false,
        memoryAlertThresholdPercent: 25.5,
      },
      jwt: {
        dangerouslyDisableSignatureValidation: true,
        dangerouslyDisableStatusValidation: true,
        allowedAlgorithms: ["ES256"],
        jwksRefreshIntervalSeconds: 60,
        jwksRefreshMinIntervalSeconds: 5,
        statusListRefreshMaxSeconds: 5,
      },
      tokenCache: {
        maxTtlSeconds: 0,
        capacity: 0,
        evictEarliestExpiration: false,
      },
      issuerLoading: { mode: "async", workers: 6 },
      http: {
        maxRetries: 31,
        retryDelaySeconds: 0,
        maxResponseSizeBytes: 0,
      },
      lock: {
        configurationUrl: "https://lock.example/config",
        ssaJwt: "synthetic-ssa",
        logIntervalSeconds: 1,
        healthIntervalSeconds: 2,
        telemetryIntervalSeconds: 3,
        logChannelCapacity: 1,
        logMaxRetries: 31,
      },
    });

    assert.strictEqual(
      prepared.bootstrapConfig.CEDARLING_POLICY_STORE_URI,
      "https://policy.example/store.cjar",
    );
    assert.strictEqual(
      prepared.bootstrapConfig.CEDARLING_POLICY_STORE_REFRESH_INTERVAL,
      30,
    );
    assert.strictEqual(prepared.bootstrapConfig.CEDARLING_LOG_TYPE, "memory");
    assert.strictEqual(prepared.bootstrapConfig.CEDARLING_LOG_LEVEL, "DEBUG");
    assert.strictEqual(
      prepared.bootstrapConfig.CEDARLING_STRICT_SCHEMA_VALIDATION,
      "disabled",
    );
    assert.strictEqual(
      prepared.bootstrapConfig.CEDARLING_TRUSTED_ISSUER_LOADER_TYPE,
      "ASYNC",
    );
    assert.strictEqual(
      prepared.bootstrapConfig.CEDARLING_LOCK_SERVER_CONFIGURATION_URI,
      "https://lock.example/config",
    );
    assert.deepEqual(prepared.clientCapabilities, {
      exposeRawErrors: false,
      memoryLogging: true,
      contextMaxTtlSeconds: 20,
    });

    assert.notOk(
      Object.hasOwn(prepared.bootstrapConfig, "logging"),
      "public fields never cross the binding boundary",
    );
  });

  QUnit.test("raw bootstrap properties pass through without SDK mapping", (assert) => {
    const bootstrapProperties = {
      CEDARLING_APPLICATION_NAME: "raw-bootstrap",
      CEDARLING_POLICY_STORE_LOCAL: "{}",
      CEDARLING_LOG_TYPE: "std_out",
      FUTURE_CORE_PROPERTY: {
        enabled: true,
      },
    };
    const prepared = prepareCedarlingOptions({ bootstrapProperties });

    bootstrapProperties.CEDARLING_APPLICATION_NAME = "mutated";
    bootstrapProperties.FUTURE_CORE_PROPERTY.enabled = false;

    assert.deepEqual(
      Object.keys(prepared.bootstrapConfig),
      [
        "CEDARLING_APPLICATION_NAME",
        "CEDARLING_POLICY_STORE_LOCAL",
        "CEDARLING_LOG_TYPE",
        "FUTURE_CORE_PROPERTY",
      ],
      "the SDK neither inserts nor removes bootstrap properties",
    );
    assert.strictEqual(
      prepared.bootstrapConfig.CEDARLING_APPLICATION_NAME,
      "raw-bootstrap",
    );
    assert.deepEqual(prepared.bootstrapConfig.FUTURE_CORE_PROPERTY, {
      enabled: true,
    });
    assert.deepEqual(prepared.policyStore, { type: "bootstrap" });
    assert.true(Object.isFrozen(prepared.bootstrapConfig));

    assert.deepEqual(prepared.clientCapabilities, {
      exposeRawErrors: false,
      memoryLogging: false,
      contextMaxTtlSeconds: 3_600,
    });
  });

  QUnit.test("raw bootstrap client capabilities are normalized once", (assert) => {
    const prepared = prepareCedarlingOptions({
      bootstrapProperties: {
        CEDARLING_APPLICATION_NAME: "raw-capabilities",
        CEDARLING_POLICY_STORE_LOCAL: "{}",
        CEDARLING_LOG_TYPE: "memory",
        CEDARLING_DATA_STORE_MAX_TTL: "10",
      },
    });

    assert.deepEqual(prepared.clientCapabilities, {
      exposeRawErrors: false,
      memoryLogging: true,
      contextMaxTtlSeconds: 10,
    });
    assert.strictEqual(
      prepared.bootstrapConfig.CEDARLING_DATA_STORE_MAX_TTL,
      "10",
      "capability normalization does not rewrite raw bootstrap properties",
    );
    assert.true(Object.isFrozen(prepared.clientCapabilities));
  });

  QUnit.test("debug diagnostics are client-only and opt in", (assert) => {
    const webNative = prepareCedarlingOptions({
      applicationName: "debug-web-native",
      policyStore: inlinePolicy,
      debug: { dangerouslyExposeRawErrors: true },
    });
    const raw = prepareCedarlingOptions({
      bootstrapProperties: {
        CEDARLING_APPLICATION_NAME: "debug-raw",
        CEDARLING_POLICY_STORE_LOCAL: "{}",
      },
      debug: { dangerouslyExposeRawErrors: true },
    });

    assert.true(webNative.clientCapabilities.exposeRawErrors);
    assert.true(raw.clientCapabilities.exposeRawErrors);
    assert.notOk(Object.hasOwn(webNative.bootstrapConfig, "debug"));
    assert.notOk(Object.hasOwn(raw.bootstrapConfig, "debug"));
  });

  QUnit.test("debug diagnostics reject invalid and unknown fields", (assert) => {
    for (const testCase of [
      {
        options: {
          applicationName: "invalid-debug-value",
          policyStore: inlinePolicy,
          debug: { dangerouslyExposeRawErrors: "yes" },
        },
        path: ["debug", "dangerouslyExposeRawErrors"],
        code: "type",
      },
      {
        options: {
          applicationName: "invalid-debug-field",
          policyStore: inlinePolicy,
          debug: { exposeRawErrors: true },
        },
        path: ["debug", "exposeRawErrors"],
        code: "unknownField",
      },
    ] as const) {
      assert.throws(
        () => prepareCedarlingOptions(testCase.options as never),
        (error: unknown) => {
          const issue = (
            error as {
              issues?: readonly [{
                readonly code?: unknown;
                readonly path?: unknown;
              }];
            }
          ).issues?.[0];
          return issue?.code === testCase.code &&
            JSON.stringify(issue.path) === JSON.stringify(testCase.path);
        },
        testCase.path.join("."),
      );
    }
  });

  QUnit.test("u64-backed options accept the JavaScript safe-integer ceiling", (assert) => {
    const maximum = Number.MAX_SAFE_INTEGER;
    const prepared = prepareCedarlingOptions({
      applicationName: "safe-u64-ceiling",
      policyStore: {
        type: "url",
        url: "https://policy.example/store.cjar",
        refresh: { intervalSeconds: maximum },
      },
      contextStore: {
        defaultTtlSeconds: maximum,
        maxTtlSeconds: maximum,
      },
      jwt: {
        jwksRefreshIntervalSeconds: maximum,
        jwksRefreshMinIntervalSeconds: maximum,
        statusListRefreshMaxSeconds: maximum,
      },
      http: { maxResponseSizeBytes: maximum },
      lock: {
        configurationUrl: "https://lock.example/config",
        logIntervalSeconds: maximum,
        healthIntervalSeconds: maximum,
        telemetryIntervalSeconds: maximum,
      },
    });

    for (const key of [
      "CEDARLING_POLICY_STORE_REFRESH_INTERVAL",
      "CEDARLING_DATA_STORE_DEFAULT_TTL",
      "CEDARLING_DATA_STORE_MAX_TTL",
      "CEDARLING_JWKS_REFRESH_INTERVAL",
      "CEDARLING_JWKS_REFRESH_MIN_INTERVAL",
      "CEDARLING_JWT_STATUS_LIST_REFRESH_INTERVAL_MAX",
      "CEDARLING_HTTP_MAX_RESPONSE_SIZE_BYTES",
      "CEDARLING_LOCK_LOG_INTERVAL",
      "CEDARLING_LOCK_HEALTH_INTERVAL",
      "CEDARLING_LOCK_TELEMETRY_INTERVAL",
    ]) {
      assert.strictEqual(prepared.bootstrapConfig[key], maximum, key);
    }
  });

  QUnit.test("u64-backed options reject unsafe JavaScript integers", (assert) => {
    const unsafe = Number.MAX_SAFE_INTEGER + 1;
    const cases = [
      {
        path: ["contextStore", "maxTtlSeconds"],
        options: {
          applicationName: "unsafe-context-u64",
          policyStore: inlinePolicy,
          contextStore: { maxTtlSeconds: unsafe },
        },
      },
      {
        path: ["http", "maxResponseSizeBytes"],
        options: {
          applicationName: "unsafe-http-u64",
          policyStore: inlinePolicy,
          http: { maxResponseSizeBytes: unsafe },
        },
      },
      {
        path: ["lock", "logIntervalSeconds"],
        options: {
          applicationName: "unsafe-lock-u64",
          policyStore: inlinePolicy,
          lock: {
            configurationUrl: "https://lock.example/config",
            logIntervalSeconds: unsafe,
          },
        },
      },
    ] as const;

    for (const testCase of cases) {
      assert.throws(
        () => prepareCedarlingOptions(testCase.options),
        (error: unknown) => {
          const issue = (
            error as {
              issues?: readonly [{
                readonly code?: unknown;
                readonly path?: unknown;
              }];
            }
          ).issues?.[0];
          return (
            issue?.code === "range" &&
            JSON.stringify(issue.path) === JSON.stringify(testCase.path)
          );
        },
        testCase.path.join("."),
      );
    }
  });

  QUnit.test("value-bearing policy inputs are detached", (assert) => {
    const document = { nested: { enabled: true } };
    const archive = new Uint8Array([1, 2, 3]);
    const inline = prepareCedarlingOptions({
      applicationName: "inline-copy",
      policyStore: { type: "inline", document },
    });
    const archived = prepareCedarlingOptions({
      applicationName: "archive-copy",
      policyStore: { type: "archive", bytes: archive },
    });

    document.nested.enabled = false;
    archive[0] = 9;
    assert.deepEqual(inline.policyStore, {
      type: "inline",
      document: { nested: { enabled: true } },
    });
    assert.deepEqual(archived.policyStore, {
      type: "archive",
      bytes: new Uint8Array([1, 2, 3]),
    });
  });
}
