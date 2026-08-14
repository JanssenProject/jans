# Passkey Telemetry & Metrics

When metrics are enabled (the default), the Janssen FIDO2 server records **every passkey registration and authentication** and exposes the results through a built-in **metrics and analytics API**. This gives you adoption, success rates, performance, device mix, and error/drop-off analysis for your passkey rollout — without bolting on an external analytics stack.

This page explains

- What you can learn using the metrics API
- How the data is produced
- How to consume the API

For the exact request/response schemas of every endpoint, refer to the [OpenAPI (Swagger) specification](#api-reference).

## Why it matters

Metrics API can answer following critical questions related to usage and roll-out of passkeys within your organization.

| Question                                                       | Where the answer comes from                                              |
| -------------------------------------------------------------- | ------------------------------------------------------------------------ |
| Is adoption growing? How many users are new vs. returning?     | `analytics/adoption`, `analytics/trends`                                 |
| Are registrations and sign-ins actually succeeding?            | aggregation `summary`, `analytics/errors`                                |
| How many users start a passkey flow but drop off?              | `analytics/errors` (`dropOffRate`, `abandonmentRate`)                    |
| Why are users failing — cancels, timeouts, bad credentials?    | `analytics/errors` (`errorCategories`, `topErrors`)                      |
| Why are authenticators being rejected at registration?         | `analytics/attestation-rejections` (`reasonCodes`, `topRejectedAaguids`) |
| Which platforms, browsers, and authenticator types are in use? | `analytics/devices`                                                      |
| Is passkey latency healthy, or getting worse?                  | `analytics/performance`                                                  |
| How does this month compare to last?                           | `analytics/comparison`                                                   |

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

- **Raw entries** — one record per event, written as each registration or authentication happens. Each entry carries user, outcome (`ATTEMPT` / `SUCCESS` / `FAILURE`), duration, authenticator type, and the request context described below. Use these for auditing or custom analysis.
- **Aggregations** — pre-computed summaries for a period (`HOURLY`, `DAILY`, `WEEKLY`, `MONTHLY`), produced on a schedule and stored. Dashboards read these instead of scanning raw data.

ATTEMPT vs. completion

Each operation produces a separate `ATTEMPT` entry when the user starts and a `SUCCESS`/`FAILURE`/`ABANDONED` entry if it resolves. An `ATTEMPT` with no matching entry is either a ceremony **still in flight** or one the user **dropped off** from — the two are not distinguishable at query time, which is why `dropOffRate`, computed as that residual, is an inference rather than a count.

### The outcomes of an authentication ceremony

A ceremony that is posted back — whether it passes verification or is rejected — always reaches one of the first two outcomes below. The third applies only to ceremonies issued for a named user, and only while `recordAbandonedAssertions` is enabled (the default); anything else that lapses stays `pending` and is deleted by normal cleanup, as it was before. Each outcome is recorded both as the `jansStatus` of the `jansFido2AuthnEntry` row and as a metrics entry status:

| Outcome          | `jansStatus`    | Metric status | Meaning                                                                                                                            |
| ---------------- | --------------- | ------------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| Verified success | `authenticated` | `SUCCESS`     | An assertion was posted and passed verification                                                                                    |
| Verified failure | `failed`        | `FAILURE`     | An assertion was posted and the server rejected it — bad signature, stale challenge, unknown credential, RP ID mismatch            |
| Abandonment      | `abandoned`     | `ABANDONED`   | A ceremony issued for a named user whose window elapsed with nothing posted back. Usernameless ceremonies are excluded — see below |

Abandonment is detected by a sweep that runs every `abandonedRequestSweepInterval` seconds and relabels named-user ceremonies still marked `pending` past `unfinishedRequestExpiration`. Abandoned rows are retained for `abandonedRequestExpiration`, deliberately much shorter than `authenticationHistoryExpiration`. Set `recordAbandonedAssertions` to `false` to disable the sweep.

Usernameless ceremonies are not counted as abandonment

A login page offering usernameless (conditional-UI) sign-in starts a ceremony on every page load, before it knows who is signing in. If the user then identifies themselves, a second, named ceremony is issued and that is the one they complete — the first is simply left untouched.

Those ceremonies are **not** swept. Counting them produced an abandonment for every successful sign-in, attributed to no user at all. They are skipped rather than recorded under a different label because the server cannot tell the two cases apart: a usernameless ceremony nobody looked at and one the user engaged with and gave up on are both just `pending` when the window elapses.

They keep the behaviour they had before abandonment recording existed — they stay `pending` and the cleaner removes them. `abandonmentRate` therefore covers ceremonies issued for a named user.

`abandonmentRate` and `dropOffRate` answer different questions and are reported side by side. `dropOffRate` is inferred as the residual of attempts minus completions, so it also absorbs ceremonies still in flight when the query runs; `abandonmentRate` counts ceremonies actually observed to have lapsed. In multi-node deployments the sweep is not coordinated across nodes, so `abandonmentRate` is approximate — an exact count is available by querying `jansStatus = 'abandoned'` directly within the retention window.

A failed fingerprint is never a `FAILURE`

With platform authenticators such as Touch ID, Face ID or Windows Hello, user verification happens **inside the authenticator**. A wrong fingerprint causes the operating system to retry locally and eventually fall back to the device passcode; the authenticator only emits an assertion once verification has already succeeded. None of those failed attempts reach the browser, let alone this server.

Consequently **the count of failed biometric attempts is not obtainable by any relying party**, and "the user failed their fingerprint" can never be recorded as an authentication failure. A user who fights with Touch ID and gives up is indistinguishable, at the protocol level, from one who cancelled immediately — both surface as `NotAllowedError` in the browser and as an `abandoned` ceremony here. A `FAILURE` means the server rejected an assertion it received, which in practice means a protocol-level problem rather than a user who could not verify.

### Request context on raw entries

Beyond the outcome itself, each raw entry records where the operation came from:

| Field        | Source                                                                                                                                                                                              |
| ------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `ipAddress`  | First valid address from `X-Forwarded-For` and the other common proxy headers, otherwise the socket remote address.                                                                                 |
| `userAgent`  | The `User-Agent` request header, up to 512 characters.                                                                                                                                              |
| `deviceInfo` | Browser, OS and device type parsed from the user agent, plus a copy of the user agent itself. The only field `fido2DeviceInfoCollection` suppresses — every other field here is written regardless. |
| `sessionId`  | The `session_id` cookie set by the Authorization Server, falling back to the servlet session when one exists. Empty for requests that carry neither.                                                |
| `metricType` | The metric name of the event, e.g. `fido2_registration_success`.                                                                                                                                    |
| `nodeId`     | Identifier of the cluster node that served the request.                                                                                                                                             |

Oversized values are shortened, not dropped

Free-form fields — `userAgent`, `sessionId`, `username`, `errorReason` and `fallbackReason` — are shortened to the width of their database column before being stored, so a single unusually long value cannot fail the write and lose the whole entry. Real-world values fit comfortably; when a value is actually shortened the FIDO2 server logs one `WARN` naming the field and its original length. The value itself is never logged, since these fields are personal data.

Behind a reverse proxy

`ipAddress` is only as trustworthy as the proxy headers reaching the FIDO2 server. If your deployment terminates TLS at a proxy, make sure it sets `X-Forwarded-For` and strips any client-supplied value; otherwise the recorded address can be spoofed by the caller.

### Aggregation schedule and retention

A scheduler computes aggregations on a cadence (hourly aggregations shortly after each hour, then daily/weekly/monthly). Data older than the configured retention window is cleaned up automatically. In a cluster the aggregation job uses a distributed lock; if the lock is unavailable it falls back to single-node mode and logs that it did so, so aggregation keeps working.

## Configuration

Telemetry is controlled by properties in the FIDO2 **dynamic configuration** (see the [FIDO2 Server Properties](https://docs.jans.io/nightly/janssen-server/fido/fido2-server-properties-config/index.md) reference for how to read and update dynamic configuration). Out of the box metrics use the default values for these properties as listed below:

| Property                          | Default | Description                                                                                                                                                                                                                    |
| --------------------------------- | ------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `fido2MetricsEnabled`             | `true`  | Master switch for metrics collection. If `false`, no entries are stored.                                                                                                                                                       |
| `fido2MetricsAggregationEnabled`  | `true`  | Enables the scheduled hourly/daily/weekly/monthly aggregation jobs.                                                                                                                                                            |
| `fido2MetricsAggregationInterval` | `60`    | Interval in **minutes** driving the aggregation scheduler (default `60` = hourly).                                                                                                                                             |
| `fido2MetricsRetentionDays`       | `90`    | Days to retain entries and aggregations before automatic cleanup.                                                                                                                                                              |
| `fido2DeviceInfoCollection`       | `true`  | Whether device info (browser, OS, device type) is collected and stored. Entries are still written when this is `false` — only the `deviceInfo` field is omitted. Use `fido2MetricsEnabled` to stop writing entries altogether. |
| `fido2ErrorCategorization`        | `true`  | Whether failures are categorized for the error-analysis endpoint.                                                                                                                                                              |
| `fido2PerformanceMetrics`         | `true`  | Whether operation durations are tracked.                                                                                                                                                                                       |

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

| Group        | Endpoints                                                                                                                                                                            |
| ------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Raw entries  | `entries`, `entries/user/{userId}`, `entries/operation/{operationType}`                                                                                                              |
| Aggregations | `aggregations/{type}`, `aggregations/{type}/summary`                                                                                                                                 |
| Analytics    | `analytics/adoption`, `analytics/performance`, `analytics/devices`, `analytics/errors`, `analytics/attestation-rejections`, `analytics/trends/{type}`, `analytics/comparison/{type}` |
| Utility      | `config`, `health`                                                                                                                                                                   |

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
- A high **`abandonmentRate`** points at the same friction but is the firmer signal, since it counts ceremonies observed to have lapsed rather than inferring them. Remember that it cannot separate a deliberate cancel from repeated biometric failures — see the warning above.
- During rollout, expect a high **`adoptionRate`** (many new users); as the base matures it falls and **`returningUsers`** dominates — that's the healthy direction.
- Rising **average durations** (`analytics/performance`) is an early warning of infrastructure or authenticator problems.

## Troubleshooting

| Symptom                                                                                                                                                                                       | What to check                                                                                                                                                                                                   |
| --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Empty array `[]` in API response                                                                                                                                                              | Confirm `metricsEnabled` (and `aggregationEnabled` for aggregation endpoints) via `GET /metrics/config`; confirm activity occurred in the range; current-hour aggregations appear a few minutes after the hour. |
| `403 Forbidden`                                                                                                                                                                               | Metrics disabled in config, or access blocked by your gateway/proxy.                                                                                                                                            |
| `400 Bad Request`                                                                                                                                                                             | Fix the `startTime`/`endTime` ISO format and ensure `startTime` ≤ `endTime`.                                                                                                                                    |
| `503` on `health`                                                                                                                                                                             | Database/persistence unreachable; check FIDO2 server logs (see [FIDO Logs](https://docs.jans.io/nightly/janssen-server/fido/logs/index.md)).                                                                    |
| Aggregations not updating                                                                                                                                                                     | Look for "aggregation scheduler initialized" in the logs; in a cluster verify the distributed lock, or confirm single-node fallback is logged.                                                                  |
| Nothing is collected at all, and the log shows `Failed to store FIDO2 metrics entry` caused by `value too long for type character varying` (PostgreSQL) or `Data too long for column` (MySQL) | The metrics columns predate the widened schema. New installs and container deployments correct themselves; an in-place VM upgrade needs the one-time migration below.                                           |

### Widening the metrics columns on an existing VM install

Deployments created before the column widths were corrected store the metrics tables with 64-character columns, which is too small for a browser user agent. Every write is then rejected and the tables stay empty — with the aggregation job still running normally and reporting success, since it has nothing to summarise.

New VM installs and container/Kubernetes deployments are handled automatically: the persistence loader compares the declared schema against the live one and widens the columns on its next run. An existing VM install needs the change applied once, by hand.

The statements below only widen columns — no stored value is truncated or removed. They do, however, take locks, so plan when you run them:

- **PostgreSQL** takes an `ACCESS EXCLUSIVE` lock on each table for the duration of the statement, blocking reads and writes. Increasing a `varchar` length and converting `varchar` to `text` do not rewrite the table, so the lock is normally held only briefly.
- **MySQL** can widen a `VARCHAR` in place only while the length-prefix size is unchanged. The conversions to `TEXT` require `ALGORITHM=COPY`, which rebuilds the table and blocks writes for the duration.

Run these in a maintenance window, or confirm that your MySQL version supports an online DDL algorithm for these specific changes before applying them to a busy table. In practice the cost is small on an affected deployment, because the metrics tables are empty — that is the symptom being fixed.

```
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

```
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

```
SELECT column_name, data_type, character_maximum_length
FROM information_schema.columns
WHERE table_name IN ('jansFido2MetricsEntry', 'jansFido2UserMetrics')
  AND data_type IN ('character varying', 'varchar', 'text')
ORDER BY table_name, column_name;
```

PostgreSQL reports the type as `character varying`, MySQL as `varchar`, so the filter covers both.

Then register a passkey from a browser — a real one, so a full-length `User-Agent` is sent.

Record the **username** you registered with and the **UTC timestamp** of the attempt. The queries below take both, as `<test-username>` and `<event-utc>`, plus a `<start-utc>`/`<end-utc>` window bracketing the attempt. Binding the checks to your own event is what stops a row left over from earlier traffic reading as a successful migration.

```
-- 1. raw events for the test account, inside the test window
SELECT "jansFido2MetricsTimestamp", "jansFido2MetricsOperationType", "jansFido2MetricsStatus",
       length("jansFido2MetricsUserAgent") AS ua_chars
FROM "jansFido2MetricsEntry"
WHERE "jansFido2MetricsUsername" = '<test-username>'
  AND "jansFido2MetricsTimestamp" BETWEEN TIMESTAMP '<start-utc>' AND TIMESTAMP '<end-utc>'
ORDER BY "jansFido2MetricsTimestamp" DESC;

-- 2. per-user rollup for the same account
SELECT "jansUsername", length("jansLastUserAgent") AS ua_chars
FROM "jansFido2UserMetrics"
WHERE "jansUsername" = '<test-username>';

-- 3. aggregation bucket for the hour containing the attempt
SELECT "jansId", "jansStartTime", "jansEndTime"
FROM "jansFido2MetricsAggregation"
WHERE "jansAggregationType" = 'HOURLY'
  AND "jansStartTime" = date_trunc('hour', TIMESTAMP '<event-utc>');
```

```
-- 1. raw events for the test account, inside the test window
SELECT jansFido2MetricsTimestamp, jansFido2MetricsOperationType, jansFido2MetricsStatus,
       CHAR_LENGTH(jansFido2MetricsUserAgent) AS ua_chars
FROM jansFido2MetricsEntry
WHERE jansFido2MetricsUsername = '<test-username>'
  AND jansFido2MetricsTimestamp BETWEEN '<start-utc>' AND '<end-utc>'
ORDER BY jansFido2MetricsTimestamp DESC;

-- 2. per-user rollup for the same account
SELECT jansUsername, CHAR_LENGTH(jansLastUserAgent) AS ua_chars
FROM jansFido2UserMetrics
WHERE jansUsername = '<test-username>';

-- 3. aggregation bucket for the hour containing the attempt
SELECT jansId, jansStartTime, jansEndTime
FROM jansFido2MetricsAggregation
WHERE jansAggregationType = 'HOURLY'
  AND jansStartTime = DATE_FORMAT('<event-utc>', '%Y-%m-%d %H:00:00');
```

Reading the results:

- **Query 1** must return the events you just performed. `ua_chars` should equal the character count of your browser's real user agent — typically 100–350, not 64. That is what separates a working migration from the truncation guard quietly trimming the value, and no `oversized field(s) shortened` warning should appear in the log for ordinary traffic. The MySQL variant uses `CHAR_LENGTH` because MySQL's `LENGTH` counts bytes rather than characters, which would inflate the figure for a non-ASCII user agent.
- **Query 2** returning nothing while query 1 returns rows means the column widths are correct and the per-user service is failing for a separate reason — check the FIDO2 log for `NoClassDefFoundError: Could not initialize class ...Fido2UserMetricsService`, which indicates the configuration keys are missing.
- **Query 3** must return exactly one row, and only after the scheduler has run for the hour *following* the one containing your attempt: it summarises the previous completed hour, a few minutes past each hour, in UTC. This table is the only proof that the aggregation ran — the job logs `Hourly aggregation completed` even when it finds nothing to summarise.

If you cannot reach the database directly, the raw entries are also available over the API. This substitutes for query 1 only; it reads raw entries and cannot confirm that the aggregation ran:

```
GET /jans-fido2/restv1/metrics/entries?startTime=<ISO-8601>&endTime=<ISO-8601>
```

## Related documentation

- [FIDO2 Server Properties](https://docs.jans.io/nightly/janssen-server/fido/fido2-server-properties-config/index.md) — reading/updating the `fido2Metrics*` properties
- [Passkeys Implementation Guide](https://docs.jans.io/nightly/janssen-server/recipes/passkey-impl-guide/index.md) — deploying the passkey experience these metrics measure
- [FIDO Logs](https://docs.jans.io/nightly/janssen-server/fido/logs/index.md) — server-side logging and diagnostics
