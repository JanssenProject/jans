# Cedarling Properties

Cedarling bootstrap properties control the application's behavior.
Properties common and specific to different authorization methods are described below.

!!! tip
    Not sure which authorization method to use? See the [decision guide](./cedarling-authz.md#which-authorization-method-should-i-use).

## Common properties

These properties are effective regardless of which authorization method is in use.

### Required properties

- **`CEDARLING_APPLICATION_NAME`** : Human-friendly identifier for this Cedarling instance.

### Loading the policy store

To load the policy store, one of the following properties must be set:

- **`CEDARLING_POLICY_STORE_LOCAL_FN`** : Path to local policy store. Cedarling automatically detects the format as one of the following. This property is not supported in WASM due to lack of file-system access. 
      - Directories → loads as directory-based policy store
      - `.cjar` files → loads as Cedar Archive
      - `.yaml`/`.yml` files → loads as YAML (supported for test suites)
      *(Note: Legacy `.json` files are no longer supported. Please migrate to the folder-based format or `.cjar` archive.)*

- **`CEDARLING_POLICY_STORE_URI`** : URL to fetch policy store archive from. The URL must point to a Cedar Archive (`.cjar` or zip archive). Legacy JSON endpoints are no longer supported.

- **`CEDARLING_POLICY_STORE_LOCAL`** : Inline YAML policy store string (primarily supported for test suites and inline configurations). Inline JSON format is deprecated and rejected; migrate to folder-based `.cjar` archives, directories, or inline YAML.

!!! note "Folder-Based Policy Store Format"
    Cedarling uses a folder-based policy store format with human-readable Cedar files (either as a directory or a `.cjar` archive). 
    See [Policy Store Formats](./cedarling-policy-store.md#policy-store-formats) for details.

### Refreshing the policy store

- **`CEDARLING_POLICY_STORE_REFRESH_INTERVAL`** : Background refresh interval in seconds for URL-based policy store sources (`CEDARLING_POLICY_STORE_URI` pointing at a Cedar Archive URL). When set to a non-zero value, Cedarling spawns a worker that periodically re-fetches the policy store and atomically swaps the in-memory `Authz` instance when the upstream changes. A server-side `Cache-Control: max-age` / `Expires` hint may *shorten* the next interval but never extends it. Default is `0` (refresh disabled — load-once-at-startup behavior). Non-zero values below `5` seconds are clamped to `5`. Ignored for local sources (`CEDARLING_POLICY_STORE_LOCAL_FN`, `CEDARLING_POLICY_STORE_LOCAL`). See [Background refresh](./cedarling-policy-store.md#background-refresh) for the per-request consistency model, the strategy ladder, and the emitted metric keys.

### Limiting policy store size

- **`CEDARLING_POLICY_STORE_MAX_FILE_SIZE`** : Maximum decompressed size, in bytes, of a single file inside a Cedar Archive (`.cjar`). Archives are ZIP files, so a small download can expand into a very large buffer in memory (a "zip bomb"); an archive whose entry exceeds this limit is rejected with an error rather than being decompressed. The whole-archive decompressed size is capped at ten times this value, and an archive may hold at most 10000 entries. Set to `0` to disable the size caps. Default is `10485760` (10 MB).

    This limit is independent of `CEDARLING_HTTP_MAX_RESPONSE_SIZE_BYTES`, which bounds the compressed archive while it is downloaded. Serving a `.cjar` over HTTP whose compressed size exceeds `CEDARLING_HTTP_MAX_RESPONSE_SIZE_BYTES` (10 MB by default) requires raising that property as well.

### Verifying policy store signatures

- **`CEDARLING_POLICY_STORE_SIGSTORE_MODE`** : `disabled` (default) or `enforce`. With `enforce`, Cedarling loads a `.cjar` policy store only if its Sigstore (cosign keyless) bundle verifies against the signer below; otherwise loading fails and, on refresh, the current store is kept. Only `.cjar` sources can be signed, so `enforce` with a directory or YAML policy store fails at startup. Each verified load and refresh is logged at `INFO` with the signer identity, issuer, archive SHA-256 and Rekor timestamp.
- **`CEDARLING_POLICY_STORE_SIGSTORE_CERT_IDENTITY`** : Expected signer identity (the certificate SAN), matched exactly. Required with `enforce`.
- **`CEDARLING_POLICY_STORE_SIGSTORE_CERT_OIDC_ISSUER`** : Expected OIDC issuer of the signing certificate, matched exactly. Required with `enforce`.
- **`CEDARLING_POLICY_STORE_SIGSTORE_BUNDLE_URI`** : Location of the bundle, as a URL for remote stores or a path for a local `.cjar`. Defaults to the policy store location with `.sigstore.json` appended. Required for archives passed as bytes (`initFromArchiveBytes`, `load_from_json_with_archive_bytes`), which have no location of their own.

These match the `cosign verify-blob` flags. For a policy store published by Agama Lab:

```json
"CEDARLING_POLICY_STORE_SIGSTORE_MODE": "enforce",
"CEDARLING_POLICY_STORE_SIGSTORE_CERT_IDENTITY": "asset-signer@prod-gluu-cloud-platform.iam.gserviceaccount.com",
"CEDARLING_POLICY_STORE_SIGSTORE_CERT_OIDC_ISSUER": "https://accounts.google.com"
```

### Optional properties

Properties listed here are optional. If a property value is not set, 
the Cedarling will use the default value as specified in the property definition below.

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

**Telemetry metrics:**

- **`CEDARLING_METRICS_COLLECTION`** : `enabled` | `disabled`. Whether to enable local collection of telemetry metrics, exposed via the `drain_metrics` API. `drain_metrics` is a destructive read: it returns a snapshot of the metrics and resets the interval window, so use a single consumer. It takes effect only when no Lock telemetry ticker is active; setting `CEDARLING_LOCK_TELEMETRY_INTERVAL` to a non-zero value makes the Lock ticker own the collector, even if the Lock server has no telemetry endpoint in which case the local metrics snapshot fails with `LockTelemetry`. Default is `disabled`.

**HTTP client:**

- **`CEDARLING_HTTP_REQUEST_TIMEOUT`** : Per-request timeout in seconds. Only applicable for native targets (not WASM). Default is `10` (10 seconds).
- **`CEDARLING_HTTP_REQUEST_MAX_RETRIES`** : Maximum number of retry attempts per request. Only applicable for native targets (not WASM). Default is `3`.
- **`CEDARLING_HTTP_REQUEST_RETRY_DELAY`** : Base delay between retries in seconds. Only applicable for native targets (not WASM). Default is `3` (3 seconds).
- **`CEDARLING_HTTP_MAX_RESPONSE_SIZE_BYTES`** : Maximum bytes Cedarling will buffer from any HTTP response (JWKS, OIDC discovery, status list, policy store, Lock Server). Oversized responses are rejected before they exhaust memory. Set to `0` to disable. Default is `10485760` (10 MB).

**Advanced configuration:**

- **`CEDARLING_MAX_BASE64_SIZE`** : Maximum size in bytes for Base64-encoded content (policies, schema, etc.)
- **`CEDARLING_MAX_DEFAULT_ENTITIES`** : Maximum number of default entities that can be loaded from the policy store.

### Lock Server integration properties

The following bootstrap properties are only needed for the Lock Server Integration. These apply to both authorization methods.

- **`CEDARLING_LOCK`** : `enabled` | `disabled`. If `enabled`, the Cedarling will connect to the Lock Server for policies, and subscribe for SSE events. Default is `disabled`.
- **`CEDARLING_LOCK_SERVER_CONFIGURATION_URI`** : Required if `LOCK` == `enabled`. URI where Cedarling can get JSON file with all required metadata about the Lock Server, i.e. `.well-known/lock-server-configuration`.
- **`CEDARLING_LOCK_DYNAMIC_CONFIGURATION`** : `enabled` | `disabled`, controls whether Cedarling should listen for SSE config updates. Default is `disabled`.
- **`CEDARLING_LOCK_SSA_JWT`** : SSA for DCR in a Lock Server deployment. The Cedarling will validate this SSA JWT prior to DCR.
- **`CEDARLING_LOCK_ACCESS_TOKEN_JWT`** : Pre-issued access token for Lock Server authentication. When set, Cedarling skips the SSA → DCR → access token flow entirely and uses this token directly to authenticate with Lock Server endpoints (log, health, telemetry). **Primarily intended for testing and local development** to simplify the bootstrap flow when a full Auth Server / DCR setup is not desired. This token may also be used when token issuance is managed externally or DCR is not available. If both `CEDARLING_LOCK_ACCESS_TOKEN_JWT` and `CEDARLING_LOCK_SSA_JWT` are set, `CEDARLING_LOCK_ACCESS_TOKEN_JWT` takes precedence and a warning is emitted. **Not available on WASM builds.**
- **`CEDARLING_LOCK_LOG_INTERVAL`** : How often to send log messages to Lock Server (0 to turn off transmission).
- **`CEDARLING_LOCK_HEALTH_INTERVAL`** : How often to send health messages to Lock Server (0 to turn off transmission).
- **`CEDARLING_LOCK_TELEMETRY_INTERVAL`** : How often to send telemetry messages to Lock Server (0 to turn off transmission).
- **`CEDARLING_LOCK_LISTEN_SSE`** : `enabled` | `disabled`: controls whether Cedarling should listen for updates from the Lock Server. Default is `disabled`.
- **`CEDARLING_LOCK_ACCEPT_INVALID_CERTS`** : `enabled` | `disabled`: Allows interaction with a Lock server with invalid certificates. Mainly used for testing. Doesn't work for WASM builds. Default is `disabled`.
- **`CEDARLING_LOCK_TRANSPORT`** : `rest` | `grpc`: Controls the transport protocol used to communicate with the Lock server. The gRPC transport requires compiling Cedarling with the `grpc` feature enabled. Default value is `rest`.
- **`CEDARLING_LOCK_LOG_CHANNEL_CAPACITY`** : Channel capacity for buffering log entries before sending to the Lock Server. Higher values allow more buffering when the server is slow, but increase memory usage. Default is `100`.
- **`CEDARLING_LOCK_LOG_MAX_RETRIES`** : Maximum number of retry attempts for sending logs to the Lock Server. Uses exponential backoff: the delay doubles from 10 seconds up to 5 minutes. Default is `5`.

### TRACE properties

With TRACE enabled, Cedarling signs an `AUTHORIZATION_DECISION` TRACE record for each authorization decision that carries a TRACE context (see [Authorize options and TRACE context](./cedarling-authz.md#authorize-options-and-trace-context)) and sends it to Lock Server. TRACE never changes the decision. An invalid TRACE configuration stops Cedarling at startup.

TRACE requires:

- `CEDARLING_LOCK` set to `enabled`: records are sent to Lock Server.
- A `.cjar` policy store (local `.cjar` file, `.cjar` URL, Lock Server or policy store URI). Each record carries `bundle_hash`, the SHA-256 of the `.cjar` bytes Cedarling loaded, so a directory or YAML policy store cannot be used.
- A signing key (`CEDARLING_TRACE_SIGNING_KEY` or `CEDARLING_TRACE_SIGNING_KEY_PEM`).
- A native build. TRACE is not supported on WASM.

Properties:

- **`CEDARLING_TRACE`** : `enabled` | `disabled`. Default is `disabled`.
- **`CEDARLING_TRACE_PRODUCER_ID`** : Stable producer id signed into every record and registered in Lock. 1 to 255 ASCII characters: a letter or digit, then letters, digits or `._:/-`. Default is the value of `CEDARLING_APPLICATION_NAME`, which must then satisfy the same rule (the default `My App` does not, because of the space).
- **`CEDARLING_TRACE_SIGNING_KEY`** : Path to the Ed25519 private key as an unencrypted PKCS#8 PEM file, for example created with `openssl genpkey -algorithm ed25519 -out trace-key.pem`. Prefer a file (for example a mounted Kubernetes or Docker secret with restricted permissions): it keeps the secret out of the bootstrap configuration. If `CEDARLING_TRACE` == `enabled`, this property or `CEDARLING_TRACE_SIGNING_KEY_PEM` is required; when both are set, this one is used.
- **`CEDARLING_TRACE_SIGNING_KEY_PEM`** : The same key as PEM text in the property value, for hosts that inject secrets as values rather than files. Used only when `CEDARLING_TRACE_SIGNING_KEY` is not set. Prefer `CEDARLING_TRACE_SIGNING_KEY` where the host can provide a file: a key inlined in the configuration can leak through environment dumps, process listings or configuration files kept under version control. Treat a configuration that contains this property as a secret.
- **`CEDARLING_TRACE_SIGNING_KID`** : `kid` the key is registered under in Lock, at most 255 UTF-16 code units (Lock counts Java string length). If unset or blank, the `kid` is the RFC 7638 thumbprint of the public key (`sha256:<hex>`). Cedarling logs the producer id, `kid` and thumbprint at startup, so the admin can register the key in Lock.
- **`CEDARLING_TRACE_ENDPOINT`** : `discovered` or a URL of the TRACE bulk endpoint. `discovered` (the default) takes `trace_bulk_endpoint` from the Lock Server well-known configuration. An empty or blank value also means `discovered`. The endpoint, explicit or discovered, must use `https`, or `http` only to a loopback host (`localhost`, `127.0.0.1`, `[::1]`), because every bulk request carries the Lock access token.
- **`CEDARLING_TRACE_BULK_MAX_RECORDS`** : Maximum number of records in one bulk request. Must be greater than 0. Default is `100`, the Lock Server default.
- **`CEDARLING_TRACE_BULK_MAX_BYTES`** : Maximum size in bytes of one bulk request. Must be at least `262146`: the largest record Lock Server accepts by default (`maxRequestBytes`, `262144` bytes) plus the brackets of the array, so any record fits in a request. Default is `4194304` (4 MiB), the Lock Server default.

Two properties from the TRACE design are intentionally absent, because in this release each has a single possible value: `CEDARLING_TRACE_CHAIN_GENESIS` (a new producer chain always starts as `first_observed` at each start) and `CEDARLING_TRACE_EMISSION` (every decision with a TRACE context is emitted).

**Tokens in a record.** A record lists the tokens of an `authorize_multi_issuer` call by issuer, token type and `jti` (or a fingerprint when there is no `jti`). The token type is the request `mapping` as is, for example `Jans::Access_Token`. A JWT is listed with its `iss` claim; Lock Server accepts only an `https` issuer, or `http` with host `localhost`, so a JWT whose `iss` Lock Server would reject means no record for that decision. A custom (non-JWT) token handled by a [custom token processor](./cedarling-multi-issuer.md#custom-non-jwt-token-processing) has no issuer URI, so it is listed with the issuer `urn:jans:cedarling:custom-issuer:<issuer_id>`, whatever the processor returns as `iss`. `<issuer_id>` is the name of its custom issuer in the policy store as Cedarling normalizes it: lower case, with `.`, `-` and spaces turned into `_` (`CustomKeys` becomes `customkeys`). Lock Server accepts this issuer in a token reference.

#### How records are sent

The authorization call only hands its record to a queue and returns; it never waits for Lock Server. A background emitter takes whatever the queue holds, up to `CEDARLING_TRACE_BULK_MAX_RECORDS` and `CEDARLING_TRACE_BULK_MAX_BYTES`, signs it and sends it as one bulk request. While a request is in flight, the next batch gathers in the queue, so under load requests fill up and when traffic is light each record goes out on its own.

TRACE reuses two Lock log properties:

- `CEDARLING_LOCK_LOG_CHANNEL_CAPACITY` is the size of the queue. When it is full, for example while Lock Server is down, the decision is still returned and its record is not emitted (`not_emitted_queue_full`). A record is signed only when the emitter takes it from the queue, so a record that never got into the queue leaves no gap in the producer chain. Cedarling logs a `WARN` when the queue fills and, when it has room again, an `INFO` with the number of records not emitted meanwhile.
- `CEDARLING_LOCK_LOG_MAX_RETRIES` is how many times a failed bulk request is retried, with exponential backoff doubling from 10 seconds up to 5 minutes, and how many times one record is resent.

`CEDARLING_LOCK_LOG_INTERVAL` does not apply to TRACE: records are sent as soon as the emitter takes them.

What is retried:

- A request that does not reach Lock Server, or gets an HTTP `5xx`, `429` or `408`, is retried with the same bytes. `429` and `408` often come from a proxy before Lock Server; both ask to repeat the request later. After the last retry its records are dropped.
- A `Retry-After` header on such an answer, in seconds or as an HTTP date, makes the wait before the next attempt at least that long, never shorter than the emitter's own backoff, and at most 5 minutes, so a misconfigured proxy cannot stop TRACE for hours. Without the header, or with a value that cannot be read or is in the past, the emitter's own backoff applies.
- HTTP `401`: when Cedarling obtained its token through DCR, it requests a new token and sends the same request once more; a second `401` is final. A token set with `CEDARLING_LOCK_ACCESS_TOKEN_JWT` cannot be renewed, so its `401` is final at once.
- Any other refusal of the whole request is final: its records are logged at `ERROR` and dropped. This includes HTTP `403`: for a token without the `trace.write` scope, or for a client Lock Server has no evidence domain for (see [Lock Server setup for TRACE](#lock-server-setup-for-trace)), and `400 invalid_request`.
- Lock Server refuses a request over its `traceConfiguration.maxBulkRecords` or `maxBulkRequestBytes` with `400 invalid_request` and the reason `max_bulk_records_exceeded` or `bulk_body_too_large`. The `ERROR` then names the Cedarling setting to lower, `CEDARLING_TRACE_BULK_MAX_RECORDS` or `CEDARLING_TRACE_BULK_MAX_BYTES`, with its current value. Lock Server sends the reason only when its `errorReasonEnabled` is `true` (default `false`); without it, the `ERROR` names the causes that fit a request Cedarling builds: TRACE disabled in Lock Server (`traceConfiguration.enabled`), or a bulk limit, with both Cedarling settings and their values.
- Per record, Lock Server answers in a `207` response. A record refused with `storage_failure` or `crypto_unavailable` is resent, after a 10-second pause, together with the next request. A record refused with `chain_not_registered` is never resent. When Lock Server refuses the first record (genesis) of the chain with `chain_not_registered`, it registers chains only through its admin API: Cedarling needs `chainRegistrationMode: FIRST_OBSERVED` (see [Lock Server setup for TRACE](#lock-server-setup-for-trace)). Every other refusal is final. The records of one response refused for the same error and reason are logged with one `ERROR` that gives their count; each record is logged at `DEBUG`.

Lock Server registers a producer chain from its genesis record and refuses every other record of the chain until it does. So Cedarling sends the genesis record alone and signs no other record into the chain until Lock Server accepts it; decisions made meanwhile wait in the queue. If the genesis record is lost (refused or dropped), Cedarling opens a new chain, which starts with its own genesis record. If Lock Server refuses records with `chain_not_registered` after it accepted the genesis record (for example, its database was restored), Cedarling logs one `WARN`, drops the records of that chain it still holds, counts them in `trace.dropped` and opens a new chain. It does so without a pause when Lock Server had accepted more of the chain than its genesis record; a chain Lock Server forgot right after its genesis record is replaced after the wait described below, since the next one would likely fare the same.

When Lock Server does not take a request (it is lost after its retries, refused as a whole, answered with every record refused, or Lock Server refuses the genesis record), the emitter waits before the next request: 10 seconds, doubling with each failure in a row up to 5 minutes; a longer `Retry-After` on the last answer stretches the wait, also to at most 5 minutes. Each wait is logged at `WARN` with its length. So a Lock Server that is down or refuses every record gets a request every few minutes, not one per decision. Meanwhile decisions wait in the queue; when it is full they are reported as `not_emitted_queue_full`, and the authorization path never waits. The first record Lock Server accepts ends the waits, which is logged at `INFO`. A chain Lock Server forgot after taking more than its genesis record is not such a failure and is replaced at once.

`Cedarling::shut_down` stops the emitter before the log. On shut down the emitter sends every record it still holds once, without retries or pauses, so stopping Cedarling does not wait for Lock Server to come back. After the first request Lock Server does not take (it fails, or Lock Server refuses the genesis record), the remaining records are not sent: one `WARN` reports how many are dropped, and they are counted in `trace.dropped`.

When Lock Server publishes no `trace_bulk_endpoint` and `CEDARLING_TRACE_ENDPOINT` is not set, or the endpoint is plain `http` to a non-loopback host, Cedarling starts, logs a `WARN` once and reports `not_emitted_no_endpoint` for every decision.

#### Record status and metrics

With TRACE enabled, every decision log entry has a `trace_record_status` field (see [Authorize options and TRACE context](./cedarling-authz.md#authorize-options-and-trace-context)):

| Status | Meaning |
|---|---|
| `emitted` | The record was handed to the emitter. Whether Lock Server accepted it shows in the emitter logs and the `trace.accepted` metric. |
| `not_emitted_missing_context` | The call had no TRACE context, or a required context field was empty. |
| `not_emitted_over_limit` | A value is longer, or a list larger, than Lock Server accepts. Cedarling does not truncate signed facts. The first one is logged at `WARN` with the field, later ones at `DEBUG`. |
| `not_emitted_invalid_field` | A value Lock Server rejects, such as a JWT issuer that is not `https`, or a policy store that cannot back a record: a `.cjar` whose `metadata.json` has no `policy_store.id` or `policy_store.version`. A store like this is reported with one `WARN` when it is loaded. Any other value is logged at `WARN` with the field the first time, later ones at `DEBUG`. |
| `not_emitted_queue_full` | The queue to the emitter is full. |
| `not_emitted_stopped` | The emitter has stopped: Cedarling was shut down, or the emitter ended unexpectedly, which is logged at `ERROR` when it happens. This instance emits no more records. Reported with one `WARN`. |
| `not_emitted_no_endpoint` | There is no TRACE endpoint to send to. |

When TRACE is disabled, the field is absent and no `trace.*` metric is added.

With metrics enabled, `operational_stats` counts each status under `trace.<status>` (for example `trace.emitted`), and what the emitter did afterwards:

- `trace.accepted`: records Lock Server accepted.
- `trace.rejected`: records Lock Server refused for good, alone or with their whole request.
- `trace.dropped`: records given up after their retries, or dropped with a chain that was replaced.
- `trace.sign_failed`: records that could not be signed.
- `trace.chains_opened`: chains opened to replace one whose genesis record was lost or that Lock Server no longer knows.

A counter appears only after it is first incremented.

#### Lock Server setup for TRACE

Lock Server accepts TRACE records only with an access token that has the `https://jans.io/oauth/lock/trace.write` scope; `log.write` is not enough. With TRACE enabled, Cedarling asks for this scope in addition to the usual Lock scopes. When Cedarling registers through DCR (see [Lock Server integration](./cedarling-lock-server.md)):

- In the Auth Server, add `https://jans.io/oauth/lock/trace.write` to the software roles of the SSA and to the `scope_list` of the interception script that adds the Cedarling scopes. The scope is created when Lock Server is installed.
- In Lock Server, enable TRACE, so that it publishes `trace_bulk_endpoint`, and register the producer key: Cedarling logs the producer id, `kid` and thumbprint at startup.

In the Lock Server `traceConfiguration` (see [Lock Server TRACE configuration](../../janssen-server/lock/trace.md#configuration)), Cedarling also needs:

| Setting | Set it to | Without it |
|---|---|---|
| `chainRegistrationMode` | `FIRST_OBSERVED` | every record is refused: `chain_not_registered` |
| `defaultEvidenceDomainId` | a domain id, for example `default` | every request is refused: HTTP `403` `client_not_bound` |
| `defaultAllowedProducerIds` | a list that contains `CEDARLING_TRACE_PRODUCER_ID` | every record is refused: `producer_not_allowed` |

Cedarling opens a new producer chain at every start and registers it with its first record, which the default `PRE_REGISTERED` mode does not allow (see [Chain registration modes](../../janssen-server/lock/trace.md#chain-registration-modes)). A per-client entry in `clientDomainBindings` also works instead of the two defaults, but Cedarling registers a new client through DCR at every start, so the defaults are simpler.

To see why Lock Server refused a record, set `errorReasonEnabled: true` in the Lock Server configuration: Cedarling then logs Lock Server's `reason` with the error, for example `event_kind_not_authorized`. Lock Server reads this setting only at startup.

A token set with `CEDARLING_LOCK_ACCESS_TOKEN_JWT` must already carry the `trace.write` scope.


## authorize_multi_issuer method properties

These properties are relevant only when using `authorize_multi_issuer` with signed JWT tokens from trusted identity providers. 
Also called Token-based Access Control (TBAC). This is the recommended authorization method for most production deployments.

**JWT and cryptographic behavior:**

- **`CEDARLING_JWT_SIG_VALIDATION`** : `enabled` | `disabled` -- Whether to check the signature of all JWT tokens. When enabled, this requires the `iss` claim to be present in all tokens and the issuer URL must use the `https` scheme. Loopback hosts (`localhost`, `127.0.0.1`, `::1`) are allowed over plain HTTP so local development is not blocked. Default is `enabled`. **Disabling this is strongly discouraged outside of testing**: a JWT without signature validation is just plain JSON and is trivially spoofable.
- **`CEDARLING_JWT_STATUS_VALIDATION`** : `enabled` | `disabled` -- Whether to check the status of the JWT. On startup, the Cedarling should fetch and retrieve the latest Status List JWT from the `.well-known/openid-configuration` via the `status_list_endpoint` claim and cache it. See the [IETF Draft](https://datatracker.ietf.org/doc/draft-ietf-oauth-status-list/) for more info. Default is `enabled`. If the issuer does not publish a `status_list_endpoint`, status checks are skipped gracefully for that issuer.
- **`CEDARLING_STRICT_SCHEMA_VALIDATION`** : `enabled` | `disabled` -- When `enabled` (default), a Cedar schema is required and policies/entities are validated against it (current behavior). When `disabled`, Cedarling runs without schema-based validation, allowing quick-start and prototyping without maintaining a schema. If disabled and a schema is present, a warning is logged that the schema is loaded but not enforced. Default is `enabled`. **Disabling this removes the load-time guard on attribute names and types.** Typos in policies will silently fail to match and claim/attribute types are not validated. **Keep enabled outside of prototyping**.
- **`CEDARLING_CUSTOM_TOKEN_PROCESSOR_TIMEOUT_MILLIS`** : Timeout in milliseconds applied to the custom token processor call for **non-JWT** custom tokens. `0` (default) disables the timeout.
- **`CEDARLING_JWT_STATUS_LIST_REFRESH_INTERVAL_MAX`** : Upper bound on the Status List JWT refresh interval, in seconds. When the Status List JWT fetched from the issuer carries a `ttl` claim (per the IETF `oauth-status-list` spec), the effective refresh interval is `min(jwt_ttl, CEDARLING_JWT_STATUS_LIST_REFRESH_INTERVAL_MAX)` — the issuer can always request a *more frequent* refresh, but never a less frequent one. When the JWT omits `ttl`, this value is used directly. A value of `0` or an unset variable resolves to the built-in default (`300` seconds) so the status list cannot silently go stale forever. Non-zero values below `5` are clamped to `5`. **Fail-closed semantics**: if a background refresh fails (network error, 5xx, invalid status list body) the cached status list is dropped and all tokens that depend on it are rejected until the next successful refresh. This ensures a revoked token is never accepted based on stale data at the cost of temporarily denying valid tokens when the status endpoint is unreachable.
- **`CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED`** : Only tokens signed with these algorithms are acceptable to the Cedarling. If not specified, all algorithms supported by the underlying library are allowed.
- **`CEDARLING_LOCAL_JWKS`** : Path to a local file containing a JWKS. Keys from this file are loaded at startup and added to the key store before fetching remote issuer keys. Useful for development, testing, or air-gapped environments. Only used when `CEDARLING_JWT_SIG_VALIDATION` is `enabled`.
- **`CEDARLING_JWKS_REFRESH_INTERVAL`** : Optional override for JWKS periodic refresh interval in seconds. When set, overrides the `Cache-Control: max-age` from the JWKS endpoint. If omitted, the server-driven interval or a 1-hour fallback is used.
- **`CEDARLING_JWKS_REFRESH_MIN_INTERVAL`** : Minimum interval in seconds between on-demand JWKS re-fetches per issuer (those triggered by tokens with an unknown `kid`). Prevents abuse from invalid JWT floods triggering excessive re-fetches. Does not affect the periodic refresh schedule, which is driven by `CEDARLING_JWKS_REFRESH_INTERVAL` / `Cache-Control: max-age`. Default is `30` seconds.

**Token cache:**

- **`CEDARLING_TOKEN_CACHE_MAX_TTL`** : Maximum token cache TTL in seconds. The token cache avoids decoding and validating the same token twice. Default is `5` seconds — small enough that revocation and status-list changes are picked up quickly, large enough to amortise repeated requests for the same token. Effective TTL is `min(time-until-exp, max_ttl)` when both apply. Setting this to `0` disables the token cache entirely.
- **`CEDARLING_TOKEN_CACHE_CAPACITY`** : Maximum number of tokens the cache can store. Default value is 100. 0 means no limit.
- **`CEDARLING_TOKEN_CACHE_EARLIEST_EXPIRATION_EVICTION`** : Enables eviction policy based on the earliest expiration time. When the cache reaches its capacity, the entry with the nearest expiration timestamp will be removed to make room for a new one. Default value is `true`.

**Trusted issuer loading:**

- **`CEDARLING_TRUSTED_ISSUER_LOADER_TYPE`** : `SYNC` | `ASYNC` -- Type of trusted issuer loader. If not set, synchronous loader is used. Sync loader means that trusted issuers will be loaded on initialization. `ASYNC` loader means that trusted issuers will be loaded in background. Default is `SYNC`. When using `ASYNC`, see [Trusted Issuer Loading Info](./cedarling-interfaces.md#trusted-issuer-loading-info) to check loading status.
- **`CEDARLING_TRUSTED_ISSUER_LOADER_WORKERS`** : Number of concurrent workers to use when loading trusted issuers. Applies to both `SYNC` (parallel loading during initialization) and `ASYNC` (parallel background loading) modes. Default is 10 for native targets (max 1000) or 2 for WASM targets (max 6). Values are clamped between 1 and the target-specific maximum. Zero becomes 1.

**Decision logging for tokens:**

- **`CEDARLING_DECISION_LOG_DEFAULT_JWT_ID`** : JWT claim name used to identify tokens in decision logs. Default is `jti`. Override with any claim name (e.g., `sub`, `sid`) if your tokens lack a `jti` claim, or you need a different identifier.
