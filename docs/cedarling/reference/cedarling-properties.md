---
tags:
  - administration
  - lock
  - authorization / authz
  - Cedar
  - Cedarling
  - properties
---

# Cedarling Properties

These Bootstrap Properties control default application-level behavior. Properties are grouped below by which authorization method they apply to, so you can quickly find what you need for your chosen approach.

Not sure which authorization method to use? See the [decision guide](../quick-start/cedarling-quick-start.md#which-authorization-method-should-i-use).

## Required properties (both methods)

These properties are required regardless of which authorization method you use.

- **`CEDARLING_APPLICATION_NAME`** : Human friendly identifier for this Cedarling instance.

To load policy store one of the following keys must be provided:

- **`CEDARLING_POLICY_STORE_LOCAL`** : JSON object as string with policy store. You can use [this](https://jsontostring.com/) converter.

- **`CEDARLING_POLICY_STORE_URI`** : URL to fetch policy store from. Cedarling automatically detects the format:
  - URLs ending in `.cjar` → loads as Cedar Archive
  - Other URLs → loads as legacy JSON from Lock Server

- **`CEDARLING_POLICY_STORE_LOCAL_FN`** : Path to local policy store. Cedarling automatically detects the format:
  - Directories → loads as directory-based policy store
  - `.cjar` files → loads as Cedar Archive
  - `.json` files → loads as JSON
  - `.yaml`/`.yml` files → loads as YAML

**New Directory-Based Format** (Native platforms only):

Cedarling now supports a directory-based policy store format with human-readable Cedar files. See [Policy Store Formats](./cedarling-policy-store.md#policy-store-formats) for details.

**Note:** In WASM environments, only `CEDARLING_POLICY_STORE_URI` and `CEDARLING_POLICY_STORE_LOCAL` are available. File and directory sources (`CEDARLING_POLICY_STORE_LOCAL_FN`) are not supported in WASM due to lack of filesystem access.

!!! NOTE
All other fields are optional and can be omitted. If a field is not provided, Cedarling will use the default value specified in the property definition.

## Properties for `authorize_multi_issuer` (JWT-based / TBAC)

These properties are relevant when using `authorize_multi_issuer` with signed JWT tokens from trusted identity providers. This is the recommended authorization method for most production deployments.

**JWT and cryptographic behavior:**

- **`CEDARLING_JWT_SIG_VALIDATION`** : `enabled` | `disabled` -- Whether to check the signature of all JWT tokens. When enabled, this requires the `iss` claim to be present in all tokens and the issuer URL must use the `https` scheme. Default is `disabled`.
- **`CEDARLING_JWT_STATUS_VALIDATION`** : `enabled` | `disabled` -- Whether to check the status of the JWT. On startup, the Cedarling should fetch and retrieve the latest Status List JWT from the `.well-known/openid-configuration` via the `status_list_endpoint` claim and cache it. See the [IETF Draft](https://datatracker.ietf.org/doc/draft-ietf-oauth-status-list/) for more info. Default is `disabled`.
- **`CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED`** : Only tokens signed with these algorithms are acceptable to the Cedarling. If not specified, all algorithms supported by the underlying library are allowed.
- **`CEDARLING_LOCAL_JWKS`** : Path to a local file containing a JWKS. Keys from this file are loaded at startup and added to the key store before fetching remote issuer keys. Useful for development, testing, or air-gapped environments. Only used when `CEDARLING_JWT_SIG_VALIDATION` is `enabled`.

**Token cache:**

- **`CEDARLING_TOKEN_CACHE_MAX_TTL`** : Maximum token cache TTL in seconds. The token cache avoids decoding and validating the same token twice. Default is `0`, which disables the maximum TTL — in that case, the token's `exp` claim is used to compute the cache entry TTL. If the token has no `exp` claim and this is `0`, the token is not cached at all. If the token has no `exp` claim and this is > 0, this value is used as the cache TTL fallback.
- **`CEDARLING_TOKEN_CACHE_CAPACITY`** : Maximum number of tokens the cache can store. Default value is 100. 0 means no limit.
- **`CEDARLING_TOKEN_CACHE_EARLIEST_EXPIRATION_EVICTION`** : Enables eviction policy based on the earliest expiration time. When the cache reaches its capacity, the entry with the nearest expiration timestamp will be removed to make room for a new one. Default value is `true`.

**Trusted issuer loading:**

- **`CEDARLING_TRUSTED_ISSUER_LOADER_TYPE`** : `SYNC` | `ASYNC` -- Type of trusted issuer loader. If not set, synchronous loader is used. Sync loader means that trusted issuers will be loaded on initialization. Async loader means that trusted issuers will be loaded in background. Default is `SYNC`. When using `ASYNC`, see [Trusted Issuer Loading Info](./cedarling-interfaces.md#trusted-issuer-loading-info) to check loading status.
- **`CEDARLING_TRUSTED_ISSUER_LOADER_WORKERS`** : Number of concurrent workers to use when loading trusted issuers. Applies to both `SYNC` (parallel loading during initialization) and `ASYNC` (parallel background loading) modes. Default is 10 for native targets (max 1000) or 2 for WASM targets (max 6). Values are clamped between 1 and the target-specific maximum. Zero becomes 1.

**Decision logging for tokens:**

- **`CEDARLING_DECISION_LOG_DEFAULT_JWT_ID`** : JWT claim name used to identify tokens in decision logs. Default is `jti`. Override with any claim name (e.g., `sub`, `sid`) if your tokens lack a `jti` claim or you need a different identifier.

## Properties for `authorize_unsigned` (application-asserted identity)

These properties are specifically relevant when using `authorize_unsigned` with raw entity data (no JWT validation).

- **`CEDARLING_PRINCIPAL_BOOLEAN_OPERATION`** : JSON Logic rule that combines per-principal authorization decisions into a final result. Variable names must match the full Cedar principal type (e.g., `MyApp::User`, `MyApp::Workload`). Only applies to `authorize_unsigned`.
  Use `{"===": [{"var": "MyApp::User"}, "ALLOW"]}` if you only want user authorization. Use `{"===": [{"var": "MyApp::Workload"}, "ALLOW"]}` if you only want workload authorization. [See here](./cedarling-principal-boolean-operations.md) for combining multiple principals.

- **`CEDARLING_UNSIGNED_ROLE_ID_SRC`** : The attribute that will be used to create the Role entity when using the unsigned interface. Defaults to `"role"`.

- **`CEDARLING_MAPPING_ROLE`** : Name of Cedar Role schema entity used when creating Role entities for principals. Default value: `Jans::Role`. Not used by `authorize_multi_issuer`.

## Properties for both methods

These properties apply to both `authorize_multi_issuer` and `authorize_unsigned`.

**Log behavior:**

- **`CEDARLING_LOG_TYPE`** : `off`, `memory`, `std_out`. Default is `off`.
- **`CEDARLING_LOG_LEVEL`** : System Log Level [See here](./cedarling-logs.md). Default to `WARN`
- **`CEDARLING_LOG_TTL`** : in case of `memory` store, TTL (time to live) of log entities in seconds.
- **`CEDARLING_LOG_MAX_ITEMS`** : Maximum number of log entities that can be stored using Memory logger. If used `0` value means no limit. And If missed or None, default value is applied.
- **`CEDARLING_LOG_MAX_ITEM_SIZE`** : Maximum size of a single log entity in bytes using Memory logger. If used `0` value means no limit. And If missed or None, default value is applied.
- **`CEDARLING_STDOUT_MODE`** : Logging mode for stdout logger: `async` or `immediate` (default). Only applicable for native targets (not WASM). Defaults to `immediate`.
- **`CEDARLING_STDOUT_TIMEOUT_MILLIS`** : Flush timeout in milliseconds for async stdout logging. Only applicable for native targets (not WASM). Defaults to 100 ms.
- **`CEDARLING_STDOUT_BUFFER_LIMIT`** : Buffer size limit in bytes for async stdout logging. Only applicable for native targets (not WASM). Defaults to 1 MB (2^20 bytes).

**Context Data API:**

- **`CEDARLING_DATA_STORE_MAX_ENTRIES`** : Maximum number of entries that can be stored in the data store. Default value is `10000`. Set to `0` for unlimited entries.

- **`CEDARLING_DATA_STORE_MAX_ENTRY_SIZE`** : Maximum size per entry in bytes. Default value is `1048576` (1 MB). Set to `0` for unlimited size.

- **`CEDARLING_DATA_STORE_DEFAULT_TTL`** : Default TTL (Time To Live) in seconds for entries that don't specify a TTL. Default value is `None` (entries will not expire). When set, entries without an explicit TTL will use this value.

- **`CEDARLING_DATA_STORE_MAX_TTL`** : Maximum allowed TTL in seconds. Default value is `3600` (1 hour). Entries with TTL exceeding this value will be rejected. Note: setting to `0` results in a zero-second max TTL (immediate expiry), not unlimited. There is currently no way to configure unlimited max TTL — omitting the property uses the default (1 hour).

- **`CEDARLING_DATA_STORE_ENABLE_METRICS`** : Whether to enable metrics tracking for data entries (access counts, etc.). Default value is `true`.

- **`CEDARLING_DATA_STORE_MEMORY_ALERT_THRESHOLD`** : Memory usage threshold percentage (0.0-100.0) for triggering alerts. Default value is `80.0`. When capacity usage exceeds this threshold, `memory_alert_triggered` will be `true` in statistics.

**Advanced configuration:**

- **`CEDARLING_MAX_BASE64_SIZE`** : Maximum size in bytes for Base64-encoded content (policies, schema, etc.)
- **`CEDARLING_MAX_DEFAULT_ENTITIES`** : Maximum number of default entities that can be loaded from the policy store.

## Lock Server integration

The following bootstrap properties are only needed for the Lock Server Integration. These apply to both authorization methods.

- **`CEDARLING_LOCK`** : `enabled` | `disabled`. If `enabled`, the Cedarling will connect to the Lock Server for policies, and subscribe for SSE events. Default is `disabled`.
- **`CEDARLING_LOCK_SERVER_CONFIGURATION_URI`** : Required if `LOCK` == `enabled`. URI where Cedarling can get JSON file with all required metadata about the Lock Server, i.e. `.well-known/lock-master-configuration`.
- **`CEDARLING_LOCK_DYNAMIC_CONFIGURATION`** : `enabled` | `disabled`, controls whether Cedarling should listen for SSE config updates. Default is `disabled`.
- **`CEDARLING_LOCK_SSA_JWT`** : SSA for DCR in a Lock Server deployment. The Cedarling will validate this SSA JWT prior to DCR.
- **`CEDARLING_LOCK_LOG_INTERVAL`** : How often to send log messages to Lock Server (0 to turn off transmission).
- **`CEDARLING_LOCK_HEALTH_INTERVAL`** : How often to send health messages to Lock Server (0 to turn off transmission).
- **`CEDARLING_LOCK_TELEMETRY_INTERVAL`** : How often to send telemetry messages to Lock Server (0 to turn off transmission).
- **`CEDARLING_LOCK_LISTEN_SSE`** : `enabled` | `disabled`: controls whether Cedarling should listen for updates from the Lock Server. Default is `disabled`.
- **`CEDARLING_LOCK_ACCEPT_INVALID_CERTS`** : `enabled` | `disabled`: Allows interaction with a Lock server with invalid certificates. Mainly used for testing. Doesn't work for WASM builds. Default is `disabled`.
- **`CEDARLING_LOCK_TRANSPORT`** : `rest` | `grpc`: Controls the transport protocol used to communicate with the Lock server. The gRPC transport requires compiling Cedarling with the `grpc` feature enabled. Default value is `rest`.
- **`CEDARLING_LOCK_LOG_CHANNEL_CAPACITY`** : Channel capacity for buffering log entries before sending to the Lock Server. Higher values allow more buffering when the server is slow, but increase memory usage. Default is `100`.
- **`CEDARLING_LOCK_LOG_MAX_RETRIES`** : Maximum number of retry attempts for sending logs to the Lock Server. Uses exponential backoff strategy. Default is `5`.
