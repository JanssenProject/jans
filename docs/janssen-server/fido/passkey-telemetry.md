---
tags:
  - administration
  - fido
  - passkeys
  - metrics
  - telemetry
---

# Passkey Telemetry & Metrics

When metrics are enabled (the default), the Janssen FIDO2 server records **every passkey
registration and authentication** and exposes the results through a built-in **metrics and
analytics API**. This gives you adoption, success rates, performance, device mix, and
error/drop-off analysis for your passkey rollout — without bolting on an external analytics
stack.

This page explains 

- What you can learn using the metrics API
- How the data is produced
- How to consume the API

For the exact request/response schemas of every endpoint, refer to the
[OpenAPI (Swagger) specification](#api-reference).

## Why it matters

Metrics API can answer following critical questions related to usage and roll-out of passkeys 
within your organization. 

| Question | Where the answer comes from |
|---|---|
| Is adoption growing? How many users are new vs. returning? | `analytics/adoption`, `analytics/trends` |
| Are registrations and sign-ins actually succeeding? | aggregation `summary`, `analytics/errors` |
| How many users start a passkey flow but drop off? | `analytics/errors` (`dropOffRate`) |
| Why are users failing — cancels, timeouts, bad credentials? | `analytics/errors` (`errorCategories`, `topErrors`) |
| Which platforms, browsers, and authenticator types are in use? | `analytics/devices` |
| Is passkey latency healthy, or getting worse? | `analytics/performance` |
| How does this month compare to last? | `analytics/comparison` |

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

!!! note 
    First-factor username/password authentication is not handled by FIDO2 server.
    You can find those metrics in the Authorization server that handles the first-factor
    authentication.

## How it works

Two kinds of data are produced:

- **Raw entries** — one record per event, written as each registration or authentication
  happens. Each entry carries user, outcome (`ATTEMPT` / `SUCCESS` / `FAILURE`), duration,
  authenticator type, and the request context described below. Use these for auditing or
  custom analysis.
- **Aggregations** — pre-computed summaries for a period (`HOURLY`, `DAILY`, `WEEKLY`,
  `MONTHLY`), produced on a schedule and stored. Dashboards read these instead of scanning
  raw data.

!!! note "ATTEMPT vs. completion"
    Each operation produces a separate `ATTEMPT` entry when the user starts and a
    `SUCCESS`/`FAILURE` entry when it completes. An `ATTEMPT` with no matching completion
    means the user **dropped off** — which is exactly what `dropOffRate` measures.

### Request context on raw entries

Beyond the outcome itself, each raw entry records where the operation came from:

| Field | Source |
|---|---|
| `ipAddress` | First valid address from `X-Forwarded-For` and the other common proxy headers, otherwise the socket remote address. |
| `userAgent` | The `User-Agent` request header, up to 512 characters. |
| `deviceInfo` | Browser, OS and device type parsed from the user agent, plus a copy of the user agent itself. The only field `fido2DeviceInfoCollection` suppresses — every other field here is written regardless. |
| `sessionId` | The `session_id` cookie set by the Authorization Server, falling back to the servlet session when one exists. Empty for requests that carry neither. |
| `metricType` | The metric name of the event, e.g. `fido2_registration_success`. |
| `nodeId` | Identifier of the cluster node that served the request. |

!!! note "Oversized values are shortened, not dropped"
    Free-form fields — `userAgent`, `sessionId`, `username`, `errorReason` and
    `fallbackReason` — are shortened to the width of their database column before being
    stored, so a single unusually long value cannot fail the write and lose the whole
    entry. Real-world values fit comfortably; when a value is actually shortened the FIDO2
    server logs one `WARN` naming the field and its original length. The value itself is
    never logged, since these fields are personal data.

!!! warning "Behind a reverse proxy"
    `ipAddress` is only as trustworthy as the proxy headers reaching the FIDO2 server. If
    your deployment terminates TLS at a proxy, make sure it sets `X-Forwarded-For` and
    strips any client-supplied value; otherwise the recorded address can be spoofed by the
    caller.

### Aggregation schedule and retention

A scheduler computes aggregations on a cadence
(hourly aggregations shortly after each hour, then daily/weekly/monthly). Data older than
the configured retention window is cleaned up automatically. In a cluster the aggregation
job uses a distributed lock; if the lock is unavailable it falls back to single-node mode
and logs that it did so, so aggregation keeps working.

## Configuration

Telemetry is controlled by properties in the FIDO2 **dynamic configuration** (see the
[FIDO2 Server Properties](fido2-server-properties-config.md) reference for how to read and
update dynamic configuration). Out of the box metrics use the default values for these properties
as listed below:

| Property | Default | Description |
|---|---|---|
| `fido2MetricsEnabled` | `true` | Master switch for metrics collection. If `false`, no entries are stored. |
| `fido2MetricsAggregationEnabled` | `true` | Enables the scheduled hourly/daily/weekly/monthly aggregation jobs. |
| `fido2MetricsAggregationInterval` | `60` | Interval in **minutes** driving the aggregation scheduler (default `60` = hourly). |
| `fido2MetricsRetentionDays` | `90` | Days to retain entries and aggregations before automatic cleanup. |
| `fido2DeviceInfoCollection` | `true` | Whether device info (browser, OS, device type) is collected and stored. Entries are still written when this is `false` — only the `deviceInfo` field is omitted. Use `fido2MetricsEnabled` to stop writing entries altogether. |
| `fido2ErrorCategorization` | `true` | Whether failures are categorized for the error-analysis endpoint. |
| `fido2PerformanceMetrics` | `true` | Whether operation durations are tracked. |

!!! warning "Don't confuse these with `metricReporter*`"
    The `metricReporterEnabled` / `metricReporterInterval` / `metricReporterKeepDataDays`
    properties belong to the legacy jans-core metric reporter and are **separate** from the
    passkey telemetry feature above. Passkey telemetry is governed by the `fido2Metrics*`
    properties.

You can always check the currently effective configuration at runtime using the command below.

```bash title="Command"
curl -X GET "https://<your-jans-server>/jans-fido2/restv1/metrics/config" \
  -H "Accept: application/json"
```

## Security

Secure these endpoints at the infrastructure level. The metrics API **does not enforce authentication on its own**, and some responses can contain PII (userId, username, IP address, user-agent, session ID). Protection must be applied in front of the FIDO2 server — an API gateway with OAuth 2.0 / API keys, a reverse proxy with auth, or network/firewall rules. Per-user endpoints such as `entries/user/{userId}` are especially sensitive and should be restricted to administrators or the user themselves.


## Healthcheck

Use `health` endpoint to check the current status of metrics API.

```bash
curl -X GET "https://<your-jans-server>/jans-fido2/restv1/metrics/health" \
  -H "Accept: application/json"
```

A healthy service returns HTTP 200 with `"status": "UP"` while `503` / `"DOWN"` indicates a
database or configuration problem (check the FIDO2 server logs).

## API reference

The telemetry API is a set of read-only `GET` endpoints grouped as raw entries,
aggregations, analytics, and utility (`config`, `health`). For the complete list of paths,
parameters, and response schemas, use the Swagger spec:

- **[FIDO2 Metrics API — OpenAPI/Swagger](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans/vreplace-janssen-version/jans-fido2/docs/jansFido2Swagger.yaml)**

| Group | Endpoints |
|---|---|
| Raw entries | `entries`, `entries/user/{userId}`, `entries/operation/{operationType}` |
| Aggregations | `aggregations/{type}`, `aggregations/{type}/summary` |
| Analytics | `analytics/adoption`, `analytics/performance`, `analytics/devices`, `analytics/errors`, `analytics/trends/{type}`, `analytics/comparison/{type}` |
| Utility | `config`, `health` |

`{type}` is one of `HOURLY`, `DAILY`, `WEEKLY`, `MONTHLY`; `{operationType}` is
`REGISTRATION` or `AUTHENTICATION`.

## Sample dashboard

You can build a passkey rollout dashboard using the data provided by metrics API. 

Most metrics API endpoints take `startTime` and `endTime` in ISO-8601 format, interpreted
as UTC. For example: `2026-01-01T00:00:00` or `2026-01-01T12:00:00Z`.

To build a minimal dashboard you would typically need three calls per dashboard refresh — a KPI summary, adoption,
and errors — over your chosen timeframe. For instance:

```bash
BASE="https://<your-jans-server>/jans-fido2/restv1/metrics"
RANGE="startTime=2026-01-01T00:00:00&endTime=2026-01-31T23:59:59"

curl -s "$BASE/aggregations/DAILY/summary?$RANGE"   # totals + avg success rates
curl -s "$BASE/analytics/adoption?$RANGE"           # new vs returning users
curl -s "$BASE/analytics/errors?$RANGE"             # failure + drop-off breakdown
```

You can build daily and monthly trend reports for passkey adoption and performance from the
response data.

Though the interpretation of various KPIs differ per implementation, a sample interpretation 
is given below.

- **Registration success rate** is healthy above ~0.80
- **authentication success rate** above ~0.90 (sign-in is usually higher, since no key generation is involved).
- A high **`dropOffRate`** or high **`USER_CANCELLED`** count usually points at UX friction
  in the passkey prompt.
- During rollout, expect a high **`adoptionRate`** (many new users); as the base matures it
  falls and **`returningUsers`** dominates — that's the healthy direction.
- Rising **average durations** (`analytics/performance`) is an early warning of
  infrastructure or authenticator problems.



## Troubleshooting

| Symptom | What to check |
|---|---|
| Empty array `[]` in API response | Confirm `metricsEnabled` (and `aggregationEnabled` for aggregation endpoints) via `GET /metrics/config`; confirm activity occurred in the range; current-hour aggregations appear a few minutes after the hour. |
| `403 Forbidden` | Metrics disabled in config, or access blocked by your gateway/proxy. |
| `400 Bad Request` | Fix the `startTime`/`endTime` ISO format and ensure `startTime` ≤ `endTime`. |
| `503` on `health` | Database/persistence unreachable; check FIDO2 server logs (see [FIDO Logs](logs.md)). |
| Aggregations not updating | Look for "aggregation scheduler initialized" in the logs; in a cluster verify the distributed lock, or confirm single-node fallback is logged. |
| Nothing is collected at all, and the log shows `Failed to store FIDO2 metrics entry` caused by `value too long for type character varying` (PostgreSQL) or `Data too long for column` (MySQL) | The metrics columns predate the widened schema. New installs and container deployments correct themselves; an in-place VM upgrade needs the one-time migration below. |

### Widening the metrics columns on an existing VM install

Deployments created before the column widths were corrected store the metrics tables with
64-character columns, which is too small for a browser user agent. Every write is then
rejected and the tables stay empty — with the aggregation job still running normally and
reporting success, since it has nothing to summarise.

New VM installs and container/Kubernetes deployments are handled automatically: the
persistence loader compares the declared schema against the live one and widens the columns
on its next run. An existing VM install needs the change applied once, by hand.

The statements below only widen columns — no stored value is truncated or removed. They do,
however, take locks, so plan when you run them:

- **PostgreSQL** takes an `ACCESS EXCLUSIVE` lock on each table for the duration of the
  statement, blocking reads and writes. Increasing a `varchar` length and converting
  `varchar` to `text` do not rewrite the table, so the lock is normally held only briefly.
- **MySQL** can widen a `VARCHAR` in place only while the length-prefix size is unchanged.
  The conversions to `TEXT` require `ALGORITHM=COPY`, which rebuilds the table and blocks
  writes for the duration.

Run these in a maintenance window, or confirm that your MySQL version supports an online DDL
algorithm for these specific changes before applying them to a busy table. In practice the
cost is small on an affected deployment, because the metrics tables are empty — that is the
symptom being fixed.

=== "PostgreSQL"

    ```sql
    ALTER TABLE "jansFido2MetricsEntry" ALTER COLUMN "jansFido2MetricsUserAgent"      TYPE VARCHAR(512);
    ALTER TABLE "jansFido2MetricsEntry" ALTER COLUMN "jansFido2MetricsErrorReason"    TYPE VARCHAR(1024);
    ALTER TABLE "jansFido2MetricsEntry" ALTER COLUMN "jansFido2MetricsFallbackReason" TYPE VARCHAR(512);
    ALTER TABLE "jansFido2MetricsEntry" ALTER COLUMN "jansFido2MetricsSessionId"      TYPE VARCHAR(128);
    ALTER TABLE "jansFido2MetricsEntry" ALTER COLUMN "jansFido2MetricsUsername"       TYPE VARCHAR(256);
    ALTER TABLE "jansFido2MetricsEntry" ALTER COLUMN "jansFido2MetricsUserId"         TYPE VARCHAR(128);
    ALTER TABLE "jansFido2MetricsEntry" ALTER COLUMN "jansFido2MetricsDeviceInfo"     TYPE TEXT;
    ALTER TABLE "jansFido2MetricsEntry" ALTER COLUMN "jansFido2MetricsAdditionalData" TYPE TEXT;

    ALTER TABLE "jansFido2UserMetrics" ALTER COLUMN "jansLastUserAgent"    TYPE VARCHAR(512);
    ALTER TABLE "jansFido2UserMetrics" ALTER COLUMN "jansUsername"         TYPE VARCHAR(256);
    ALTER TABLE "jansFido2UserMetrics" ALTER COLUMN "jansUserId"           TYPE VARCHAR(128);
    ALTER TABLE "jansFido2UserMetrics" ALTER COLUMN "jansUserSegments"     TYPE TEXT;
    ALTER TABLE "jansFido2UserMetrics" ALTER COLUMN "jansBehaviorPatterns" TYPE TEXT;
    ```

=== "MySQL"

    ```sql
    ALTER TABLE jansFido2MetricsEntry MODIFY COLUMN jansFido2MetricsUserAgent      VARCHAR(512);
    ALTER TABLE jansFido2MetricsEntry MODIFY COLUMN jansFido2MetricsErrorReason    VARCHAR(1024);
    ALTER TABLE jansFido2MetricsEntry MODIFY COLUMN jansFido2MetricsFallbackReason VARCHAR(512);
    ALTER TABLE jansFido2MetricsEntry MODIFY COLUMN jansFido2MetricsSessionId      VARCHAR(128);
    ALTER TABLE jansFido2MetricsEntry MODIFY COLUMN jansFido2MetricsUsername       VARCHAR(256);
    ALTER TABLE jansFido2MetricsEntry MODIFY COLUMN jansFido2MetricsUserId         VARCHAR(128);
    ALTER TABLE jansFido2MetricsEntry MODIFY COLUMN jansFido2MetricsDeviceInfo     TEXT;
    ALTER TABLE jansFido2MetricsEntry MODIFY COLUMN jansFido2MetricsAdditionalData TEXT;

    ALTER TABLE jansFido2UserMetrics MODIFY COLUMN jansLastUserAgent    VARCHAR(512);
    ALTER TABLE jansFido2UserMetrics MODIFY COLUMN jansUsername         VARCHAR(256);
    ALTER TABLE jansFido2UserMetrics MODIFY COLUMN jansUserId           VARCHAR(128);
    ALTER TABLE jansFido2UserMetrics MODIFY COLUMN jansUserSegments     TEXT;
    ALTER TABLE jansFido2UserMetrics MODIFY COLUMN jansBehaviorPatterns TEXT;
    ```

To confirm the change took effect:

```sql
SELECT column_name, data_type, character_maximum_length
FROM information_schema.columns
WHERE table_name IN ('jansFido2MetricsEntry', 'jansFido2UserMetrics')
  AND data_type IN ('character varying', 'varchar', 'text')
ORDER BY table_name, column_name;
```

PostgreSQL reports the type as `character varying`, MySQL as `varchar`, so the filter covers
both.

Register a passkey from a browser afterwards and confirm a row appears; the next scheduled
aggregation will then have something to summarise.

## Related documentation

- [FIDO2 Server Properties](fido2-server-properties-config.md) — reading/updating the `fido2Metrics*` properties
- [Passkeys Implementation Guide](../recipes/passkey-impl-guide.md) — deploying the passkey experience these metrics measure
- [FIDO Logs](logs.md) — server-side logging and diagnostics
