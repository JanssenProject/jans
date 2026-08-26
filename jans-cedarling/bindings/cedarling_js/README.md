![Cedarling PARC authorization inputs flow into local policy evaluation and produce allow or deny](https://raw.githubusercontent.com/JanssenProject/jans/main/jans-cedarling/bindings/cedarling_js/docs/assets/cedarling-parc-boundary.png)

# Cedarling JavaScript SDK

[![npm](https://img.shields.io/npm/v/%40janssenproject%2Fcedarling)](https://www.npmjs.com/package/@janssenproject/cedarling)
[![license](https://img.shields.io/npm/l/%40janssenproject%2Fcedarling)](https://github.com/JanssenProject/jans/blob/main/LICENSE)

Add fast, local Cedar policy authorization to JavaScript and TypeScript
applications. `@janssenproject/cedarling` evaluates access decisions in your
application process, validates OAuth/OIDC tokens when configured, and returns
ordinary JavaScript data.

## What Cedarling evaluates

Cedarling evaluates a principal, action, resource, and context against Cedar
policies. Your application supplies the request and bootstrap properties; the
SDK runs policy evaluation locally and returns a plain allow-or-deny decision.

Use `init` with Cedarling bootstrap properties, or `initFromArchiveBytes`
when your application has already retrieved a Cedar Archive.

## Install

~~~bash
npm install @janssenproject/cedarling
~~~

The package also installs with pnpm, Yarn, Bun, and Deno's npm compatibility.

## Choose the runtime entry

| Environment                       | Import                         | Notes                                                                                           |
| --------------------------------- | ------------------------------ | ----------------------------------------------------------------------------------------------- |
| Node.js 22, 24, or 26             | @janssenproject/cedarling      | Supports ESM and CommonJS.                                                                      |
| Bun current stable                | @janssenproject/cedarling      | ESM.                                                                                            |
| Deno LTS or current stable        | @janssenproject/cedarling      | ESM; grant read access to the installed WASM asset when required by the Deno dependency layout. |
| Modern browsers                   | @janssenproject/cedarling      | Bundle with a modern bundler such as Vite, webpack, esbuild, or Rolldown.                       |
| Cloudflare Workers or Vercel Edge | @janssenproject/cedarling/edge | Explicit ESM-only edge entry.                                                                   |

~~~ts
import { init, initFromArchiveBytes } from "@janssenproject/cedarling";
~~~

~~~js
const { init, initFromArchiveBytes } = require("@janssenproject/cedarling");
~~~

Edge hosts use:

~~~ts
import { init, initFromArchiveBytes } from "@janssenproject/cedarling/edge";
~~~

The edge entry does not support CommonJS require. Browser consumers must bundle
the package; the package manages its internal WASM asset, so consumers should
not copy, serve, or configure a separate WASM file.

## Quick start

Initialize with Cedarling's bootstrap-property object. Use the
[bootstrap-property reference](https://docs.jans.io/stable/cedarling/reference/cedarling-properties/)
and the expandable reference below for supported names, values, defaults, and
security requirements.

~~~ts
import { init } from "@janssenproject/cedarling";

const cedarling = await init({
  CEDARLING_APPLICATION_NAME: "task-api",
  CEDARLING_POLICY_STORE_LOCAL: JSON.stringify(policyStoreDocument),
  CEDARLING_LOG_TYPE: "std_out",
  CEDARLING_LOG_LEVEL: "INFO",
  CEDARLING_JWT_SIG_VALIDATION: "disabled",
  CEDARLING_JWT_STATUS_VALIDATION: "disabled",
});
~~~

The signature and status settings above are suitable only for an unsigned local
demonstration. Enable the appropriate validation settings in production.

When `CEDARLING_LOG_TYPE` is `"memory"`, also set the memory-log properties
required by your retention policy, including `CEDARLING_LOG_TTL`.

## Initialize from an archive you retrieve

Fetch policy-store bytes with your application's authentication, authorization,
cache, and retry policy, then pass those bytes to `initFromArchiveBytes`.
This works in browsers, Node.js, and edge deployments.

~~~ts
// Inside your application
const response = await fetch("/api/policy-store", {
  headers: { Authorization: "Bearer " + policyStoreToken },
});
if (!response.ok) throw new Error("Policy-store download failed");

// Then pass it to Cedarling
const cedarling = await initFromArchiveBytes(
  {
    CEDARLING_APPLICATION_NAME: "task-api",
    CEDARLING_LOG_TYPE: "std_out",
    CEDARLING_LOG_LEVEL: "INFO",
  },
  new Uint8Array(await response.arrayBuffer()),
);
~~~

Do not put archive bytes or functions inside the bootstrap-property object.
Use `initFromArchiveBytes` for an archive held in memory. Its properties object
must omit `CEDARLING_POLICY_STORE_LOCAL`, `CEDARLING_POLICY_STORE_URI`,
`CEDARLING_POLICY_STORE_CJAR_URL`, and `CEDARLING_POLICY_STORE_LOCAL_FN`; the
archive argument is the policy-store source.

## Choose an authorization method

### Application-asserted authorization

Authorization methods accept a JSON-string request. For unsigned authorization,
the application asserts the principal; for multi-issuer authorization, Cedarling
validates mapped token inputs.

~~~ts
const result = await cedarling.authorizeUnsigned(JSON.stringify({
  principal: {
    cedar_entity_mapping: { entity_type: "Task::User", id: "alice" },
    role: "editor",
  },
  action: 'Task::Action::"Read"',
  resource: {
    cedar_entity_mapping: { entity_type: "Task::Document", id: "document-1" },
  },
  context: { request: { method: "GET" } },
}));

if (result.decision) {
  console.log("Allowed", result.request_id);
} else {
  console.log("Denied", result.response.diagnostics.reason);
}
~~~

The decision contains `decision`, `request_id`, and
`response.diagnostics`. See the API reference below for the complete shape.

### Token-validating authorization

Use `authorizeMultiIssuer` when Cedarling must validate mapped token inputs.
Pass the token-oriented request JSON required by your Cedarling configuration;
property names, issuer rules, and validation requirements remain defined by
[Cedarling core documentation](https://docs.jans.io/stable/cedarling/).

~~~ts
const result = await cedarling.authorizeMultiIssuer(multiIssuerRequestJson);
if (!result.decision) {
  console.log(result.response.diagnostics.reason);
}
~~~

## Store reusable context data

Store application data that Cedarling policies can use during later decisions.

~~~ts
cedarling.pushDataCtx("request:123", { department: "sales" }, 60n);
const entry = cedarling.getDataEntryCtx("request:123");
const stats = cedarling.getStatsCtx();
cedarling.removeDataCtx("request:123");
~~~

## Query retained logs

When Cedarling is configured to retain logs, query or drain stored entries.

~~~ts
const matching = cedarling.getLogsByTag("Decision");
const requestLogs = cedarling.getLogsByRequestId(result.request_id);
const drained = cedarling.popLogs();
~~~

## Observe issuer readiness and annotations

Use issuer methods to observe configured issuers and annotation methods to read
policy metadata.

~~~ts
const loaded = cedarling.isTrustedIssuerLoadedByName("example-issuer");
const unavailable = cedarling.failedTrustedIssuerIds();
const annotations = cedarling.annotationsMap(["policy-id"]);
~~~

The API reference below documents every method's input and return shape,
including batch results.

## Configuration reference


<details>
<summary>Full bootstrap-property reference</summary>

Pass a JSON-compatible object to `init` or `initFromArchiveBytes`. Keys are
case-sensitive Cedarling property names. Values can be native JavaScript strings,
numbers, booleans, arrays, and objects when the listed property accepts that
JSON type. The core validates required values and combinations during
initialization.

### Required property and policy source

Set `CEDARLING_APPLICATION_NAME` and exactly one policy-store source when using
`init`. With `initFromArchiveBytes`, omit every policy-store source property;
its `archiveBytes` argument supplies the policy store.

| Property                                  | Accepted value                                 | Meaning                                                                                                                |
| ----------------------------------------- | ---------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------- |
| `CEDARLING_APPLICATION_NAME`              | Non-empty string                               | Identifies this Cedarling instance.                                                                                    |
| `CEDARLING_POLICY_STORE_LOCAL`            | String containing a policy-store JSON document | Loads an inline policy store.                                                                                          |
| `CEDARLING_POLICY_STORE_URI`              | URL string                                     | Fetches a policy store; Cedar Archive URLs load `.cjar` content and other URLs use the configured policy-store format. |
| `CEDARLING_POLICY_STORE_CJAR_URL`         | URL string                                     | Fetches a Cedar Archive policy store.                                                                                  |
| `CEDARLING_POLICY_STORE_REFRESH_INTERVAL` | Non-negative seconds; `0` is the default       | Enables background refresh for a remote source. Values from `1` to `4` are raised to `5`; local sources ignore it.     |

### Logging and decision-log identity

| Property                                | Accepted value                                                    | Default and meaning                                                         |
| --------------------------------------- | ----------------------------------------------------------------- | --------------------------------------------------------------------------- |
| `CEDARLING_LOG_TYPE`                    | `"off"`, `"memory"`, or `"std_out"`                               | `"off"`. Selects no logs, retained in-memory logs, or standard-output logs. |
| `CEDARLING_LOG_LEVEL`                   | `"TRACE"`, `"DEBUG"`, `"INFO"`, `"WARN"`, `"ERROR"`, or `"FATAL"` | `"WARN"`. Minimum recorded log level.                                       |
| `CEDARLING_LOG_TTL`                     | Non-negative seconds                                              | Retention period for memory logs.                                           |
| `CEDARLING_LOG_MAX_ITEMS`               | Non-negative integer                                              | Maximum retained memory logs; `0` removes the limit.                        |
| `CEDARLING_LOG_MAX_ITEM_SIZE`           | Non-negative bytes                                                | Maximum retained memory-log size; `0` removes the limit.                    |
| `CEDARLING_DECISION_LOG_DEFAULT_JWT_ID` | JWT claim name                                                    | `"jti"`. Claim recorded as the token identifier in decision logs.           |

Memory-log query methods require `CEDARLING_LOG_TYPE: "memory"`.

### Context-data storage

| Property                                      | Accepted value           | Default and meaning                                                                |
| --------------------------------------------- | ------------------------ | ---------------------------------------------------------------------------------- |
| `CEDARLING_DATA_STORE_MAX_ENTRIES`            | Non-negative integer     | `10000`. Maximum context entries; `0` is unlimited.                                |
| `CEDARLING_DATA_STORE_MAX_ENTRY_SIZE`         | Non-negative bytes       | `1048576`. Maximum serialized entry size; `0` is unlimited.                        |
| `CEDARLING_DATA_STORE_DEFAULT_TTL`            | Non-negative seconds     | Unset means entries without an explicit TTL do not expire.                         |
| `CEDARLING_DATA_STORE_MAX_TTL`                | Non-negative seconds     | `3600`. Rejects a larger per-entry TTL. `0` means immediate expiry, not unlimited. |
| `CEDARLING_DATA_STORE_ENABLE_METRICS`         | Boolean                  | `true`. Enables access and capacity metrics.                                       |
| `CEDARLING_DATA_STORE_MEMORY_ALERT_THRESHOLD` | Number from `0` to `100` | `80`. Capacity percentage that marks the memory alert as triggered.                |

### Token validation, caching, and issuer loading

| Property                                             | Accepted value               | Default and meaning                                                                                |
| ---------------------------------------------------- | ---------------------------- | -------------------------------------------------------------------------------------------------- |
| `CEDARLING_JWT_SIG_VALIDATION`                       | `"enabled"` or `"disabled"`  | `"enabled"`. Verifies token signatures. Keep enabled outside local testing.                        |
| `CEDARLING_JWT_STATUS_VALIDATION`                    | `"enabled"` or `"disabled"`  | `"enabled"`. Checks issuer status lists when available.                                            |
| `CEDARLING_STRICT_SCHEMA_VALIDATION`                 | `"enabled"` or `"disabled"`  | `"enabled"`. Requires and enforces a Cedar schema.                                                 |
| `CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED`       | Array of `HS256`, `HS384`, `HS512`, `ES256`, `ES384`, `RS256`, `RS384`, `RS512`, `PS256`, `PS384`, `PS512`, or `EdDSA` | Limits accepted token signature algorithms; omit to use Cedarling's supported set. |
| `CEDARLING_JWKS_REFRESH_INTERVAL`                    | Seconds                      | Overrides issuer JWKS cache timing; values below `5` become `5`.                                   |
| `CEDARLING_JWKS_REFRESH_MIN_INTERVAL`                | Seconds                      | `30`. Minimum interval between on-demand JWKS refreshes; values below `5` become `5`.              |
| `CEDARLING_JWT_STATUS_LIST_REFRESH_INTERVAL_MAX`     | Seconds                      | `300`. Maximum status-list refresh interval; `0` uses the default and values below `5` become `5`. |
| `CEDARLING_TOKEN_CACHE_MAX_TTL`                      | Non-negative seconds         | `5`. Maximum cached-token lifetime; `0` disables the cache.                                        |
| `CEDARLING_TOKEN_CACHE_CAPACITY`                     | Non-negative integer         | `100`. Maximum cached tokens; `0` removes the limit.                                               |
| `CEDARLING_TOKEN_CACHE_EARLIEST_EXPIRATION_EVICTION` | Boolean                      | `true`. Evicts the entry nearest expiry when capacity is reached.                                  |
| `CEDARLING_TRUSTED_ISSUER_LOADER_TYPE`               | `"SYNC"` or `"ASYNC"`        | `"SYNC"`. Loads issuers during initialization or in the background.                                |
| `CEDARLING_TRUSTED_ISSUER_LOADER_WORKERS`            | Integer                      | WebAssembly default `2`, clamped from `1` to `6`. Number of concurrent issuer loaders.             |

### Network and resource limits

| Property                                 | Accepted value       | Default and meaning                                                         |
| ---------------------------------------- | -------------------- | --------------------------------------------------------------------------- |
| `CEDARLING_HTTP_REQUEST_MAX_RETRIES`     | Non-negative integer | `3`. Retry count for Cedarling HTTP requests.                               |
| `CEDARLING_HTTP_REQUEST_RETRY_DELAY`     | Non-negative seconds | `3`. Base delay between retries.                                            |
| `CEDARLING_HTTP_MAX_RESPONSE_SIZE_BYTES` | Non-negative bytes   | `10485760` (10 MB). Maximum buffered HTTP response; `0` disables the limit. |
| `CEDARLING_MAX_BASE64_SIZE`              | Non-negative bytes   | Limits Base64-encoded policy-store content; `0` removes the limit.          |
| `CEDARLING_MAX_DEFAULT_ENTITIES`         | Non-negative integer | Limits policy-store default entities; `0` removes the limit.                |

### Lock Server integration

These properties apply only to a Lock Server deployment.

| Property                                  | Accepted value              | Default and meaning                                            |
| ----------------------------------------- | --------------------------- | -------------------------------------------------------------- |
| `CEDARLING_LOCK`                          | `"enabled"` or `"disabled"` | `"disabled"`. Enables Lock Server integration.                 |
| `CEDARLING_LOCK_SERVER_CONFIGURATION_URI` | URL string                  | Required when Lock integration is enabled.                     |
| `CEDARLING_LOCK_DYNAMIC_CONFIGURATION`    | `"enabled"` or `"disabled"` | `"disabled"`. Enables dynamic configuration updates.           |
| `CEDARLING_LOCK_SSA_JWT`                  | JWT string                  | Software statement assertion for dynamic client registration.  |
| `CEDARLING_LOCK_LOG_INTERVAL`             | Non-negative seconds        | `0` disables Lock log transmission.                            |
| `CEDARLING_LOCK_HEALTH_INTERVAL`          | Non-negative seconds        | `0` disables Lock health transmission.                         |
| `CEDARLING_LOCK_TELEMETRY_INTERVAL`       | Non-negative seconds        | `0` disables Lock telemetry transmission.                      |
| `CEDARLING_LOCK_LISTEN_SSE`               | `"enabled"` or `"disabled"` | `"disabled"`. Listens for Lock events.                         |
| `CEDARLING_LOCK_ACCEPT_INVALID_CERTS`     | `"enabled"` or `"disabled"` | `"disabled"`. Testing-only certificate override.               |
| `CEDARLING_LOCK_TRANSPORT`                | `"rest"` or `"grpc"`        | `"rest"`. Lock transport; gRPC requires a matching core build. |
| `CEDARLING_LOCK_LOG_CHANNEL_CAPACITY`     | Positive integer            | `100`. Buffered Lock log capacity.                             |
| `CEDARLING_LOCK_LOG_MAX_RETRIES`          | Non-negative integer        | `5`. Retry count for Lock log delivery.                        |

### Properties unavailable to this WebAssembly SDK

| Property                                                                                    | Why it is unavailable                                                          |
| ------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------ |
| `CEDARLING_POLICY_STORE_LOCAL_FN`                                                           | Requires filesystem access. Use a URL, inline JSON, or `initFromArchiveBytes`. |
| `CEDARLING_LOCAL_JWKS`                                                                      | Requires a local file path. Configure remote trusted issuers instead.          |
| `CEDARLING_STDOUT_MODE`, `CEDARLING_STDOUT_TIMEOUT_MILLIS`, `CEDARLING_STDOUT_BUFFER_LIMIT` | Standard-output controls apply to native targets, not WebAssembly.             |
| `CEDARLING_HTTP_REQUEST_TIMEOUT`                                                            | Native-target HTTP timeout setting.                                            |
| `CEDARLING_LOCK_ACCESS_TOKEN_JWT`                                                           | Not available in the WebAssembly build.                                        |

See the [Cedarling bootstrap-property reference](https://docs.jans.io/stable/cedarling/reference/cedarling-properties/)
for core behavior, policy-store formats, and Lock Server deployment guidance.
</details>

## Handle decisions and failures

Initialization and client operations reject on errors. A `false` authorization
`decision` is a successful policy denial, not an operation error.

~~~ts
try {
  const cedarling = await init(properties);
  try {
    await cedarling.authorizeUnsigned(requestJson);
  } finally {
    await cedarling.shutDown();
  }
} catch (error) {
  console.error("Cedarling failed", error);
}
~~~

## Client API reference


<details>
<summary>Full API details</summary>

All methods use camel-case JavaScript names. Authorization methods return a
promise; context, log, issuer, and annotation methods return their value
synchronously. A policy denial resolves with `decision: false`; invalid input,
initialization failures, and runtime failures reject.

### Initialization

| Function                                         | Input                                                  | Resolves to                              |
| ------------------------------------------------ | ------------------------------------------------------ | ---------------------------------------- |
| `init(properties)`                               | A bootstrap-property object from the reference above   | A `Cedarling` client.                    |
| `initFromArchiveBytes(properties, archiveBytes)` | Bootstrap properties plus a `Uint8Array` Cedar Archive | A `Cedarling` client using that archive. |

### Authorization

Pass a JSON string to each authorization method.

| Method                               | Request JSON                                                                                                              | Resolves to                 |
| ------------------------------------ | ------------------------------------------------------------------------------------------------------------------------- | --------------------------- |
| `authorizeUnsigned(request)`         | `{ principal?: EntityData \| null, action: string, resource: EntityData, context?: object }`                              | `AuthorizationResult`.      |
| `authorizeMultiIssuer(request)`      | `{ tokens: [{ mapping: string, payload: string }], action: string, resource: EntityData, context?: object }`              | `AuthorizationResult`.      |
| `authorizeUnsignedBatch(request)`    | `{ principal?: EntityData \| null, items: [{ action: string, resource: EntityData, context?: object }] }`                 | `BatchAuthorizationResult`. |
| `authorizeMultiIssuerBatch(request)` | `{ tokens: [{ mapping: string, payload: string }], items: [{ action: string, resource: EntityData, context?: object }] }` | `BatchAuthorizationResult`. |

`EntityData` is the Cedarling entity JSON shape, including
`cedar_entity_mapping: { entity_type, id }` and optional attributes. The token
`mapping` must exist in the policy store.

A single decision has this shape:

~~~ts
type AuthorizationResult = {
  decision: boolean;
  request_id: string;
  response: {
    decision: boolean;
    diagnostics: {
      reason: readonly string[];
      errors: readonly { id: string; error: string }[];
    };
  };
};
~~~

A batch preserves input-item order:

~~~ts
type BatchAuthorizationResult = {
  batch_id: string;
  results: readonly (
    | { is_ok: true; result: AuthorizationResult }
    | {
        is_ok: false;
        error?: { category: string; item_index: number; message: string };
      }
  )[];
};
~~~

### Context data

| Method                              | Input                                                               | Returns                                                                                                                                       |
| ----------------------------------- | ------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------- |
| `pushDataCtx(key, value, ttlSecs?)` | String key, JSON-compatible value, optional `bigint` TTL in seconds | `void`. Replaces an existing key.                                                                                                             |
| `getDataCtx(key)`                   | String key                                                          | Stored JSON value, or no value when missing or expired.                                                                                       |
| `getDataEntryCtx(key)`              | String key                                                          | A context entry object, `null`, or `undefined`. Entry metadata includes the stored value, key, Cedar data type, timestamps, and access count. |
| `removeDataCtx(key)`                | String key                                                          | `true` when an entry was removed; otherwise `false`.                                                                                          |
| `clearDataCtx()`                    | —                                                                   | `void`. Removes every stored context entry.                                                                                                   |
| `listDataCtx()`                     | —                                                                   | Context entry objects with the same metadata as `getDataEntryCtx`.                                                                            |
| `getStatsCtx()`                     | —                                                                   | Statistics including entry count, configured limits, byte totals, capacity percentage, metrics state, and memory-alert state.                 |

### Retained logs

Set `CEDARLING_LOG_TYPE: "memory"` before using retained logs.

| Method                                     | Input                           | Returns                                                         |
| ------------------------------------------ | ------------------------------- | --------------------------------------------------------------- |
| `getLogIds()`                              | —                               | Retained log IDs.                                               |
| `getLogById(id)`                           | Log ID                          | One log object or no value.                                     |
| `getLogsByRequestId(requestId)`            | Decision request ID             | Matching log objects.                                           |
| `getLogsByRequestIdAndTag(requestId, tag)` | Decision request ID and log tag | Matching log objects.                                           |
| `getLogsByTag(tag)`                        | Log tag or level                | Matching log objects.                                           |
| `popLogs()`                                | —                               | All retained log objects and clears them from retained storage. |

Log objects are returned as JSON data because their fields depend on the log
kind and Cedarling configuration.

### Issuer readiness

| Method                                  | Input              | Returns                                        |
| --------------------------------------- | ------------------ | ---------------------------------------------- |
| `isTrustedIssuerLoadedByIss(issClaim)`  | Issuer `iss` claim | Whether that trusted issuer is loaded.         |
| `isTrustedIssuerLoadedByName(issuerId)` | Trusted issuer ID  | Whether that trusted issuer is loaded.         |
| `totalIssuers()`                        | —                  | Number of discovered trusted issuer entries.   |
| `loadedTrustedIssuersCount()`           | —                  | Number of successfully loaded trusted issuers. |
| `loadedTrustedIssuerIds()`              | —                  | Successfully loaded issuer IDs.                |
| `failedTrustedIssuerIds()`              | —                  | Issuer IDs that failed to load.                |

A readiness value of `false` means the issuer is unknown, pending, or failed.

### Policy annotations

| Method                             | Input                         | Returns                                                 |
| ---------------------------------- | ----------------------------- | ------------------------------------------------------- |
| `annotationValues(policyIds, key)` | Policy IDs and annotation key | Every matching annotation value, preserving duplicates. |
| `annotationsByPolicy(policyIds)`   | Policy IDs                    | Annotation objects grouped by policy ID.                |
| `annotationsMap(policyIds)`        | Policy IDs                    | One merged annotation object. Duplicate keys are lossy. |

Use `result.response.diagnostics.reason` as the policy-ID input after an
authorization decision. Unknown policy IDs are skipped.

### Lifecycle

| Method       | Input | Resolves to                                                                                          |
| ------------ | ----- | ---------------------------------------------------------------------------------------------------- |
| `shutDown()` | —     | `void` after in-flight authorization calls finish. Repeated calls share the same shutdown operation. |

</details>

## Shut down the client

Always call `shutDown()` when the client is no longer needed. It waits for active
authorization calls to finish, then closes the client. Do not use a client after
shutdown begins.

## Security

- Treat bootstrap properties, policy stores, archive bytes, authorization JSON,
and tokens as untrusted input.
- Use HTTPS outside local development and validate policy-store responses before
passing their bytes to initFromArchiveBytes.
- Keep signature, status, schema, and algorithm validation enabled in production.
- Never log tokens, policy material, or raw error data indiscriminately.
- Ensure browser Content Security Policy permits WebAssembly execution.

## Troubleshooting and support

If initialization fails, verify the raw properties against the Cedarling
reference. For browser initialization failures, verify that the application
bundles the package and that its Content Security Policy permits WebAssembly.

- [Cedarling documentation](https://docs.jans.io/stable/cedarling/)
- [Bootstrap-property reference](https://docs.jans.io/stable/cedarling/reference/cedarling-properties/)
- [SDK source](https://github.com/JanssenProject/jans/tree/main/jans-cedarling/bindings/cedarling_js)
- [Issue tracker](https://github.com/JanssenProject/jans/issues)
