---
tags:
  - administration
  - fido2
---

# FIDO2 Server Configuration

This document contains a detailed reference of configuration properties. To learn how to safely update these dynamic configuration parameters, please follow the [Janssen FIDO2 Configuration Guide](../config-guide/fido2-config/janssen-fido2-configuration.md) first.

## FIDO2 Server Configuration Parameters

The following properties represent the dynamic configuration for the Janssen FIDO2 Server. They are stored in the configuration storage layer and can be updated at runtime.

| Field Name | Type / Example | Description |
| :--- | :--- | :--- |
| `issuer` | `https://my-jans-server.jans.io` | URL using the HTTPS scheme with no query or fragment component. The OP asserts this as its Issuer Identifier. |
| `baseEndpoint` | `https://my-jans-server/jans-fido2/restv1` | Base URL of the FIDO2 server endpoints. |
| `webAuthnEndpoint` | `https://my-jans-server/jans-fido2/restv1/webauthn/configuration` | Base URL of the FIDO2 WebAuthn Server Endpoint which returns Relying Party (RP) Origins. |
| `cleanServiceInterval` | `60` | Time interval for the Clean Service daemon in seconds. |
| `cleanServiceBatchChunkSize` | `10000` | Number of expired records fetched and deleted per iteration batch from the persistence store. |
| `useLocalCache` | `true` | Boolean value specifying whether to enable local in-memory caching for performance. |
| `disableJdkLogger` | `true` | Boolean value specifying whether to disable standard JDK loggers. |
| `loggingLevel` | `INFO`, `DEBUG`, or `TRACE` | The logging verbosity level for the FIDO2 server diagnostics. |
| `loggingLayout` | `text` or `json` | Format of output log lines (plain text layout or structured JSON). |
| `externalLoggerConfiguration` | String | Path to an external Log4j2 XML configuration file. |
| `metricReporterInterval` | `300` | The interval for the legacy jans-core metric reporter daemon in seconds. |
| `metricReporterKeepDataDays` | `15` | The retention period in days for legacy metric reporter records stored in persistence. |
| `metricReporterEnabled` | `true` | Boolean value specifying whether to enable the legacy jans-core metric reporter. |
| `fido2MetricsEnabled` | `true` | Master switch for passkey telemetry collection. If `false`, no metric entries are stored. See [Passkey Telemetry & Metrics](passkey-telemetry.md). |
| `fido2MetricsAggregationEnabled` | `true` | Enables the scheduled hourly/daily/weekly/monthly aggregation jobs for passkey telemetry. The times those jobs run are fixed by cron expressions packaged with the server, not by dynamic configuration — see [Aggregation schedule](passkey-telemetry.md#aggregation-schedule). |
| `fido2MetricsRetentionDays` | `90` | Retention period in days for passkey metric entries and aggregations before automatic cleanup. |
| `fido2DeviceInfoCollection` | `true` | Whether device info (browser, OS, device type) is collected and stored with passkey metrics. |
| `fido2ErrorCategorization` | `true` | Whether passkey operation failures are categorized for the error-analysis endpoint. |
| `fido2PerformanceMetrics` | `true` | Whether passkey operation durations are tracked for performance analytics. |
| `fido2Configuration` | Object | Nested object containing FIDO2 protocol-specific details (see structure below). |

---

## FIDO2 Configuration Object (`fido2Configuration`)

This nested block defines WebAuthn and FIDO2 attestation and assertion policy behavior.

| Field | Type | Default / Example | Description |
| :--- | :--- | :--- | :--- |
| `authenticatorCertsFolder` | String | `"/etc/jans/conf/fido2/authenticator_cert"` | Folder where verified authenticator certificates (e.g., Apple roots) are stored. |
| `mdsCertsFolder` | String | `"/etc/jans/conf/fido2/mds/cert"` | Folder where FIDO Metadata Service (MDS) TOC root certificates are stored. |
| `mdsTocsFolder` | String | `"/etc/jans/conf/fido2/mds/toc"` | Folder where downloaded MDS TOC files are cached. |
| `userAutoEnrollment` | Boolean | `false` | Specifies whether to automatically enroll unknown users during WebAuthn cycles (normally disabled). |
| `unfinishedRequestExpiration` | Integer | `120` | Expiration time in seconds for incomplete registration/authentication requests. |
| `metadataRefreshInterval` | Integer | `1296000` | Expiration time in seconds (e.g., 15 days) before checking and reloading the FIDO Alliance MDS TOC. |
| <span id="servermetadatafolder">`serverMetadataFolder`</span> | String | `"/etc/jans/conf/fido2/server_metadata"` | Folder where local vendor metadata statement JSON files are placed manually. |
| `enabledFidoAlgorithms` | Array of Strings | `["RS256", "ES256"]` | Enabled cryptographic signing algorithms allowed for credentials. Accepted names: `RS256`, `RS384`, `RS512`, `RS65535`, `PS256`, `PS384`, `PS512`, `ES256`, `ES384`, `ES512`, `ESP256`, `ESP384`, `EdDSA`, `Ed25519`, `Ed448`, `ML-DSA-44`, `ML-DSA-65`, `ML-DSA-87` — the algorithms the server can both advertise and complete a registration with. When unset, the server advertises `RS256`, `ES256` and `EdDSA`. An unrecognised name is ignored. A recognised name the deployment cannot actually complete a registration with is logged at `ERROR` and left out of `pubKeyCredParams` — see [Advertised algorithms](#advertised-algorithms). |
| `rp` | Array of Objects | `[ { "id": "https://jans.io", "origins": ["jans.io"] } ]` | Relying Party (RP) configuration mapping expected IDs to valid origins. |
| `metadataServers` | Array of Objects | `[ { "url": "https://mds.fidoalliance.org/" } ]` | External FIDO Metadata Service endpoints to download statement catalogs. |
| `disableMetadataService` | Boolean | `false` | If set to `true`, the FIDO2 server skips validating authenticators against the MDS3 service. |
| `mdsDownloadStartupRetries` | Integer | `3` | Number of times the MDS TOC download is *retried* at server startup when the TOC blob is missing (a missing TOC prevents attestation validation). This is in addition to the initial attempt, so the default of `3` means up to 4 downloads. `0` disables retries. Retries stop early once the blob is present, and are skipped when the metadata server answers HTTP 429, since it has explicitly asked the server to back off. |
| `mdsDownloadStartupRetryInterval` | Integer | `30` | Delay in seconds between MDS TOC download retries at server startup when the TOC blob is missing. |
| `hints` | Array of Strings | `["security-key", "client-device", "hybrid"]` | Preferred authenticator type hints presented to the Relying Party. |
| `enterpriseAttestation` | Boolean | `false` | Enables support for enterprise-specific hardware attestation profiles. |
| `attestationMode` | String | `"monitor"` | Options are: `disabled` (skip attestation checks), `monitor` (log/validate but allow credentials if attestation is absent/unknown), and `enforced` (fail credential creation if attestation check fails). |
| `allowedTopOrigins` | Array of Strings | `[]` | Full origins permitted to frame a cross-origin ceremony, each written as scheme, host and optional port (for example `https://portal.example.com`). Empty — the default — denies every framed ceremony. See [Cross-origin ceremonies](#cross-origin-ceremonies). |

### Advertised algorithms

The algorithms offered to the authenticator in `pubKeyCredParams` are not taken from `enabledFidoAlgorithms`
directly. An algorithm is advertised only when this server can complete a registration with it end-to-end:
decode the credential public key and verify a signature made with it, using the crypto provider the
deployment is actually running. Anything else is dropped: a configured name that does not survive the
check is logged at `ERROR`, and a default that does not survive it is logged at `WARN`.

This matters most on the FIPS build, whose provider supports strictly fewer algorithms than the standard
one. Deriving the advertised set from real capability means a FIPS deployment simply offers less, rather
than offering an algorithm and then failing the ceremony once the authenticator picks it.

If no configured algorithm survives the check, the server logs an error and falls back to whichever of the
defaults it does support. In a deployment that supports none of them that fallback is itself empty, and
`pubKeyCredParams` is sent empty — a state worth alerting on, since the log will already carry the reason.

### Fully-specified ECDSA algorithms

`ESP256` and `ESP384` name their elliptic curve in the COSE code point itself rather than leaving it to the
credential: `ESP256` is P-256 only and `ESP384` is P-384 only. A credential that pairs one of them with any
other curve is rejected during registration. `ES256`, `ES384` and `ES512` are not fully specified and take
whichever curve the key carries.

### Fully-specified EdDSA algorithms

`EdDSA` is the original COSE code point and takes whichever Edwards curve the credential carries — both
Ed25519 and Ed448 keys are accepted under it. `Ed25519` and `Ed448` are separate, fully-specified code
points that name their curve: a credential pairing `Ed25519` with an Ed448 key, or the reverse, is rejected
during registration.

Existing credentials are unaffected — they were registered under `EdDSA`, whose behaviour is unchanged.

### Post-quantum algorithms (ML-DSA)

`ML-DSA-44`, `ML-DSA-65` and `ML-DSA-87` are supported on the standard build. They are **not** available on
the FIPS build, because no released `bc-fips` provider implements them yet.

This needs no special handling from an administrator. Because the advertised set is derived from real
provider capability (see [Advertised algorithms](#advertised-algorithms)), a FIPS deployment that lists an
ML-DSA name simply logs it at `ERROR` and leaves it out of `pubKeyCredParams` — it never offers an
algorithm it would then fail to verify. The same configuration is therefore safe to share between the two
build variants.

Both the IANA spelling (`ML-DSA-44`) and the underscore form (`ML_DSA_44`) are accepted.

### Cross-origin ceremonies

As WebAuthn Level 3 requires, the server reads the `crossOrigin` member of `CollectedClientData`. An absent
member is treated as `false`; both a non-boolean value and an explicit `null` fail with `invalid_request`.

When `crossOrigin` is `true`, the ceremony is allowed only if its `topOrigin` — the origin of the page that
framed it — appears in `allowedTopOrigins`. The request fails with `cross_origin_not_allowed` when:

- `allowedTopOrigins` is empty, which is the default and denies every framed ceremony
- `topOrigin` is absent, `null`, not a string, or blank
- `topOrigin` is not listed

Entries are compared against the whole origin, ignoring case and surrounding whitespace. A different scheme
or port is a different origin, so `https://portal.example.com` does not permit `http://portal.example.com`.

`allowedTopOrigins` is deliberately separate from the `origins` under `rp`. Those say which origin may
*serve* a ceremony; this says which origin may *frame* one. Reusing the former would silently widen the
framing policy of every existing deployment.


