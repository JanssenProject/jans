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
| `fido2MetricsAggregationEnabled` | `true` | Enables the scheduled hourly/daily/weekly/monthly aggregation jobs for passkey telemetry. |
| `fido2MetricsAggregationInterval` | `60` | Interval in seconds driving the passkey metrics aggregation scheduler. |
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
| `serverMetadataFolder` | String | `"/etc/jans/conf/fido2/server_metadata"` | Folder where local vendor metadata statement JSON files are placed manually. |
| `enabledFidoAlgorithms` | Array of Strings | `["RS256", "ES256"]` | Enabled cryptographic signing algorithms allowed for credentials. |
| `rp` | Array of Objects | `[ { "id": "https://jans.io", "origins": ["jans.io"] } ]` | Relying Party (RP) configuration mapping expected IDs to valid origins. |
| `metadataServers` | Array of Objects | `[ { "url": "https://mds.fidoalliance.org/" } ]` | External FIDO Metadata Service endpoints to download statement catalogs. |
| `disableMetadataService` | Boolean | `false` | If set to `true`, the FIDO2 server skips validating authenticators against the MDS3 service. |
| `hints` | Array of Strings | `["security-key", "client-device", "hybrid"]` | Preferred authenticator type hints presented to the Relying Party. |
| `enterpriseAttestation` | Boolean | `false` | Enables support for enterprise-specific hardware attestation profiles. |
| `attestationMode` | String | `"monitor"` | Options are: `disabled` (skip attestation checks), `monitor` (log/validate but allow credentials if attestation is absent/unknown), and `enforced` (fail credential creation if attestation check fails). |


