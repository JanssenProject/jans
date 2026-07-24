# Passkey Telemetry & Metrics

When metrics are enabled (the default), the Janssen FIDO2 server records **every passkey registration and authentication** and exposes the results through a built-in **metrics and analytics API**. This gives you adoption, success rates, performance, device mix, and error/drop-off analysis for your passkey rollout — without bolting on an external analytics stack.

This page explains

- What you can learn using the metrics API
- How the data is produced
- How to consume the API

For the exact request/response schemas of every endpoint, refer to the [OpenAPI (Swagger) specification](#api-reference).

## Why it matters

Metrics API can answer following critical questions related to usage and roll-out of passkeys within your organization.

| Question                                                       | Where the answer comes from                         |
| -------------------------------------------------------------- | --------------------------------------------------- |
| Is adoption growing? How many users are new vs. returning?     | `analytics/adoption`, `analytics/trends`            |
| Are registrations and sign-ins actually succeeding?            | aggregation `summary`, `analytics/errors`           |
| How many users start a passkey flow but drop off?              | `analytics/errors` (`dropOffRate`)                  |
| Why are users failing — cancels, timeouts, bad credentials?    | `analytics/errors` (`errorCategories`, `topErrors`) |
| Which platforms, browsers, and authenticator types are in use? | `analytics/devices`                                 |
| Is passkey latency healthy, or getting worse?                  | `analytics/performance`                             |
| How does this month compare to last?                           | `analytics/comparison`                              |

Since the API serves this data as plain JSON, it can be easily used by a dashboard, an alerting rule, or a periodic report.

## Event tracking

Following events are tracked and they are sent to the FIDO2 endpoints:

- Registration (passkey enrollment)
  - Attempt
  - Success
  - Failure (with error reason and category).
- Authentication (passkey sign-in)
  - Attempt
  - Success
  - Failure (with error reason and category).
- Fallback (when a user skips the passkey during a 2FA step and uses another method (e.g. password), recorded with method and reason).

Note

First-factor username/password authentication is not handled by FIDO2 server. You can find those metrics in the Authorization server that handles the first-factor authentication.

## How it works

Two kinds of data are produced:

- **Raw entries** — one record per event, written as each registration or authentication happens. Each entry carries user, outcome (`ATTEMPT` / `SUCCESS` / `FAILURE`), duration, authenticator type, and (optionally) device info. Use these for auditing or custom analysis.
- **Aggregations** — pre-computed summaries for a period (`HOURLY`, `DAILY`, `WEEKLY`, `MONTHLY`), produced on a schedule and stored. Dashboards read these instead of scanning raw data.

ATTEMPT vs. completion

Each operation produces a separate `ATTEMPT` entry when the user starts and a `SUCCESS`/`FAILURE` entry when it completes. An `ATTEMPT` with no matching completion means the user **dropped off** — which is exactly what `dropOffRate` measures.

### Aggregation schedule and retention

A scheduler computes aggregations on a cadence (hourly aggregations shortly after each hour, then daily/weekly/monthly). Data older than the configured retention window is cleaned up automatically. In a cluster the aggregation job uses a distributed lock; if the lock is unavailable it falls back to single-node mode and logs that it did so, so aggregation keeps working.

## Configuration

Telemetry is controlled by properties in the FIDO2 **dynamic configuration** (see the [FIDO2 Server Properties](https://docs.jans.io/nightly/janssen-server/fido/fido2-server-properties-config/index.md) reference for how to read and update dynamic configuration). Out of the box metrics use the default values for these properties as listed below:

| Property                          | Default | Description                                                                        |
| --------------------------------- | ------- | ---------------------------------------------------------------------------------- |
| `fido2MetricsEnabled`             | `true`  | Master switch for metrics collection. If `false`, no entries are stored.           |
| `fido2MetricsAggregationEnabled`  | `true`  | Enables the scheduled hourly/daily/weekly/monthly aggregation jobs.                |
| `fido2MetricsAggregationInterval` | `60`    | Interval in **minutes** driving the aggregation scheduler (default `60` = hourly). |
| `fido2MetricsRetentionDays`       | `90`    | Days to retain entries and aggregations before automatic cleanup.                  |
| `fido2DeviceInfoCollection`       | `true`  | Whether device info (browser, OS, device type) is collected and stored.            |
| `fido2ErrorCategorization`        | `true`  | Whether failures are categorized for the error-analysis endpoint.                  |
| `fido2PerformanceMetrics`         | `true`  | Whether operation durations are tracked.                                           |

Don't confuse these with `metricReporter*`

The `metricReporterEnabled` / `metricReporterInterval` / `metricReporterKeepDataDays` properties belong to the legacy jans-core metric reporter and are **separate** from the passkey telemetry feature above. Passkey telemetry is governed by the `fido2Metrics*` properties.

You can always check the currently effective configuration at runtime using the command below.

Command

```
curl -X GET "https://<your-jans-server>/jans-fido2/restv1/metrics/config" \
  -H "Accept: application/json"
```

## Security

Secure these endpoints at the infrastructure level. The metrics API **does not enforce authentication on its own**, and some responses can contain PII (userId, username, IP address, user-agent, session ID). Protection must be applied in front of the FIDO2 server — an API gateway with OAuth 2.0 / API keys, a reverse proxy with auth, or network/firewall rules. Per-user endpoints such as `entries/user/{userId}` are especially sensitive and should be restricted to administrators or the user themselves.

## Healthcheck

Use `health` endpoint to check the current status of metrics API.

```
curl -X GET "https://<your-jans-server>/jans-fido2/restv1/metrics/health" \
  -H "Accept: application/json"
```

A healthy service returns HTTP 200 with `"status": "UP"` while `503` / `"DOWN"` indicates a database or configuration problem (check the FIDO2 server logs).

## API reference

The telemetry API is a set of read-only `GET` endpoints grouped as raw entries, aggregations, analytics, and utility (`config`, `health`). For the complete list of paths, parameters, and response schemas, use the Swagger spec:

- **[FIDO2 Metrics API — OpenAPI/Swagger](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans/nightly/jans-fido2/docs/jansFido2Swagger.yaml)**

| Group        | Endpoints                                                                                                                                        |
| ------------ | ------------------------------------------------------------------------------------------------------------------------------------------------ |
| Raw entries  | `entries`, `entries/user/{userId}`, `entries/operation/{operationType}`                                                                          |
| Aggregations | `aggregations/{type}`, `aggregations/{type}/summary`                                                                                             |
| Analytics    | `analytics/adoption`, `analytics/performance`, `analytics/devices`, `analytics/errors`, `analytics/trends/{type}`, `analytics/comparison/{type}` |
| Utility      | `config`, `health`                                                                                                                               |

`{type}` is one of `HOURLY`, `DAILY`, `WEEKLY`, `MONTHLY`; `{operationType}` is `REGISTRATION` or `AUTHENTICATION`.

## Sample dashboard

You can build a passkey rollout dashboard using the data provided by metrics API.

Most metrics API endpoints take `startTime` and `endTime` in ISO-8601 format, interpreted as UTC. For example: `2026-01-01T00:00:00` or `2026-01-01T12:00:00Z`.

To build a minimal dashboard you would typically need three calls per dashboard refresh — a KPI summary, adoption, and errors — over your chosen timeframe. For instance:

```
BASE="https://<your-jans-server>/jans-fido2/restv1/metrics"
RANGE="startTime=2026-01-01T00:00:00&endTime=2026-01-31T23:59:59"

curl -s "$BASE/aggregations/DAILY/summary?$RANGE"   # totals + avg success rates
curl -s "$BASE/analytics/adoption?$RANGE"           # new vs returning users
curl -s "$BASE/analytics/errors?$RANGE"             # failure + drop-off breakdown
```

You can build daily and monthly trend reports for passkey adoption and performance from the response data.

Though the interpretation of various KPIs differ per implementation, a sample interpretation is given below.

- **Registration success rate** is healthy above ~0.80
- **authentication success rate** above ~0.90 (sign-in is usually higher, since no key generation is involved).
- A high **`dropOffRate`** or high **`USER_CANCELLED`** count usually points at UX friction in the passkey prompt.
- During rollout, expect a high **`adoptionRate`** (many new users); as the base matures it falls and **`returningUsers`** dominates — that's the healthy direction.
- Rising **average durations** (`analytics/performance`) is an early warning of infrastructure or authenticator problems.

## Troubleshooting

| Symptom                          | What to check                                                                                                                                                                                                   |
| -------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Empty array `[]` in API response | Confirm `metricsEnabled` (and `aggregationEnabled` for aggregation endpoints) via `GET /metrics/config`; confirm activity occurred in the range; current-hour aggregations appear a few minutes after the hour. |
| `403 Forbidden`                  | Metrics disabled in config, or access blocked by your gateway/proxy.                                                                                                                                            |
| `400 Bad Request`                | Fix the `startTime`/`endTime` ISO format and ensure `startTime` ≤ `endTime`.                                                                                                                                    |
| `503` on `health`                | Database/persistence unreachable; check FIDO2 server logs (see [FIDO Logs](https://docs.jans.io/nightly/janssen-server/fido/logs/index.md)).                                                                    |
| Aggregations not updating        | Look for "aggregation scheduler initialized" in the logs; in a cluster verify the distributed lock, or confirm single-node fallback is logged.                                                                  |

## Related documentation

- [FIDO2 Server Properties](https://docs.jans.io/nightly/janssen-server/fido/fido2-server-properties-config/index.md) — reading/updating the `fido2Metrics*` properties
- [Passkeys Implementation Guide](https://docs.jans.io/nightly/janssen-server/recipes/passkey-impl-guide/index.md) — deploying the passkey experience these metrics measure
- [FIDO Logs](https://docs.jans.io/nightly/janssen-server/fido/logs/index.md) — server-side logging and diagnostics
