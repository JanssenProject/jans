# FIDO2 Metrics – Complete Guide

**Understand, configure, and use the FIDO2 Metrics API for dashboards, monitoring, and reporting.**

This guide explains everything about FIDO2 metrics: what is tracked, how data is stored and aggregated, how to call the API, and how to interpret the results. Whether you are integrating a dashboard, setting up alerts, or troubleshooting, you’ll find the answers here.

---

## Table of contents

- [What is the FIDO2 Metrics feature?](#what-is-the-fido2-metrics-feature)
- [What is tracked (and what is not)](#what-is-tracked-and-what-is-not)
- [Key concepts at a glance](#key-concepts-at-a-glance)
- [Base URL, time format, and security](#base-url-time-format-and-security)
- [Getting started](#getting-started)
- [API reference – all endpoints](#api-reference--all-endpoints)
- [Raw data endpoints](#raw-data-endpoints)
- [Aggregation endpoints](#aggregation-endpoints)
- [Analytics endpoints](#analytics-endpoints)
- [Utility endpoints](#utility-endpoints)
- [Configuration reference](#configuration-reference)
- [How aggregations work (schedule and period IDs)](#how-aggregations-work-schedule-and-period-ids)
- [Understanding response fields](#understanding-response-fields)
- [Examples and use cases](#examples-and-use-cases)
- [Troubleshooting](#troubleshooting)
- [Metrics and formulas reference](#metrics-and-formulas-reference)
- [Additional resources](#additional-resources)

---

## What is the FIDO2 Metrics feature?

The FIDO2 Metrics feature adds a **full metrics and analytics API** for FIDO2/Passkey operations. Every registration and every authentication is recorded automatically with user, device, and outcome details. The system also **pre-computes aggregations** at hourly, daily, weekly, and monthly intervals, so you can query adoption, performance, devices, errors, and trends without scanning raw data.

**In practice you can:**

| Use case | What you get |
|----------|----------------|
| **Dashboards** | Real-time adoption, success rates, device distribution |
| **Monitoring & alerting** | Error rates and performance; trigger alerts when thresholds are exceeded |
| **Reports** | Periodic reports on user adoption, engagement, and failure analysis |
| **Troubleshooting** | Error categories, top errors, and performance metrics to resolve issues quickly |

All of this is exposed through **REST APIs** with clear query parameters and JSON responses.

---

## What is tracked (and what is not)

### In scope (what FIDO2 metrics track)

- **FIDO2 registration** – Passkey enrollment: attempt, success, and failure (with error reason and category).
- **FIDO2 authentication** – Passkey sign-in: attempt, success, and failure (with error reason and category).
- **Fallback** – When the user skips passkey and uses another method (e.g. password) during the 2FA step: we record a fallback event with method (e.g. `PASSWORD`) and reason.

### Out of scope (not tracked by FIDO2 metrics)

- **Username/password (first-factor) authentication** – Success or failure of the initial login with username and password is **not** part of the FIDO2 server. That step is handled by the authorization server (e.g. jans-auth) or another IdP. FIDO2 metrics only see requests that reach the FIDO2 endpoints (passkey registration or passkey authentication). For username/password success and failure counts, use metrics or logs from the component that performs first-factor authentication (e.g. jans-auth).

---

## Key concepts at a glance

| Concept | Meaning |
|--------|---------|
| **Raw entries** | One record per registration or authentication event. Stored in the database and returned by `/metrics/entries` (and variants). Use for auditing or custom analysis. |
| **Aggregations** | Pre-computed summaries for a **period** (one hour, one day, one week, or one month). Stored in the database; the API returns aggregations that overlap your `startTime`/`endTime`. Use for dashboards and reports without scanning raw data. |
| **Aggregation types** | `HOURLY`, `DAILY`, `WEEKLY`, `MONTHLY`. Each type has its own schedule (e.g. hourly at :05 past the hour; daily at 01:10 UTC). |
| **Period ID** | Identifies one aggregation period. Examples: `2026-01-01-12` (hour 12 on 2026-01-01), `WEEKLY_2026-W07` (ISO week 7 of 2026). Durations in responses are in **milliseconds**. |
| **Device types** | Derived from authenticator type: typically **platform** (built-in) and **cross-platform** (roaming), and optionally **security-key**. |
| **Cluster vs single node** | In a cluster, aggregation jobs use a distributed lock. If the lock is unavailable (e.g. `ou=node` not configured), the job still runs in a **single-node fallback** mode and logs that behaviour. |

---

## Base URL, time format, and security

**Base URL for all metrics endpoints:**

```
https://your-jans-server/jans-fido2/restv1/metrics
```

Replace `your-jans-server` with your actual Janssen server host.

**Time parameters:** Most endpoints accept `startTime` and `endTime` in **ISO 8601** format, interpreted as **UTC**. Supported forms:

- `yyyy-MM-ddTHH:mm:ss` (e.g. `2026-01-01T00:00:00`) – treated as UTC  
- With timezone: `2026-01-01T12:00:00Z` or `2026-01-01T12:00:00+00:00`

**Authentication and security:** The metrics API does not enforce authentication by itself. **Protection should be applied at the infrastructure level** (e.g. API gateway with OAuth 2.0 or API keys, reverse proxy with auth, or network/firewall rules). Endpoints that return data for a **specific user** (e.g. `/metrics/entries/user/{userId}`) are sensitive and should be restricted to authorized administrators or the user themselves.

---

## Getting started

### Prerequisites

1. **Janssen FIDO2 server** must be installed and running.  
2. **Metrics** must be enabled in the FIDO2 configuration.  
3. A **database** (MySQL, PostgreSQL, or LDAP) must be configured for the FIDO2 server.

### Enabling metrics

Metrics are controlled via FIDO2 configuration. Ensure at least:

```properties
fido2.metrics.enabled=true
fido2.metrics.aggregation.enabled=true
```

Exact property names may depend on your deployment (e.g. JSON/LDAP config). The **current effective configuration** can always be checked with `GET /metrics/config`.

### First API call: health check

A simple way to verify the metrics service is available:

```bash
curl -X GET "https://your-jans-server/jans-fido2/restv1/metrics/health" \
  -H "Accept: application/json"
```

**When healthy:** HTTP 200 and a body like:

```json
{
  "metricsEnabled": true,
  "aggregationEnabled": true,
  "status": "UP",
  "timestamp": "2026-01-01T12:00:00"
}
```

**When unhealthy:** HTTP 503 and `"status": "DOWN"`. In that case, check database connectivity and application logs.

---

## API reference – all endpoints

The metrics API has **13 GET endpoints**, grouped as follows.

| # | Method | Endpoint | Description |
|---|--------|----------|-------------|
| 1 | GET | `/metrics/entries` | All metric entries in a time range |
| 2 | GET | `/metrics/entries/user/{userId}` | Entries for one user (by user ID / inum) |
| 3 | GET | `/metrics/entries/operation/{operationType}` | Entries filtered by REGISTRATION or AUTHENTICATION |
| 4 | GET | `/metrics/aggregations/{aggregationType}` | Pre-computed aggregations (HOURLY, DAILY, WEEKLY, MONTHLY) |
| 5 | GET | `/metrics/aggregations/{aggregationType}/summary` | Summary across aggregations in a time range |
| 6 | GET | `/metrics/analytics/adoption` | User adoption: new users, returning users, adoption rate |
| 7 | GET | `/metrics/analytics/performance` | Performance: average, min, max durations for registration and authentication |
| 8 | GET | `/metrics/analytics/devices` | Device analytics: device types, OS, browsers, authenticator types |
| 9 | GET | `/metrics/analytics/errors` | Error analysis: categories, top errors, success/failure rates |
| 10 | GET | `/metrics/analytics/trends/{aggregationType}` | Trend analysis over time with insights |
| 11 | GET | `/metrics/analytics/comparison/{aggregationType}` | Period-over-period comparison (e.g. this month vs last) |
| 12 | GET | `/metrics/config` | Current metrics configuration (enabled, retention, etc.) |
| 13 | GET | `/metrics/health` | Health check (200 = UP, 503 = DOWN) |

**Common parameters:**

- **Endpoints 1–3, 4–5, 6–10:** Require query parameters `startTime` and `endTime` (ISO format, UTC).  
- **Endpoint 11:** Optionally accepts `periods` (2–12, default 2) to compare that many consecutive periods.  
- **Path parameters:**  
  - `aggregationType`: one of `HOURLY`, `DAILY`, `WEEKLY`, `MONTHLY`  
  - `operationType`: one of `REGISTRATION`, `AUTHENTICATION`  
  - `userId`: the user’s internal ID (inum), not the username.

---

## Raw data endpoints

These return **individual metric entries** (one record per registration or authentication event).

### GET /metrics/entries

Returns all metric entries between `startTime` and `endTime`. Use when you need event-level detail (e.g. for auditing or custom analysis).

**Example request:**

```bash
curl -X GET "https://your-jans-server/jans-fido2/restv1/metrics/entries?startTime=2026-01-01T00:00:00&endTime=2026-01-01T23:59:59" \
  -H "Accept: application/json"
```

**Example response:** An array of entry objects. Fields that are null are omitted. Typical fields include:

- `id` – Unique identifier for the entry  
- `timestamp` – Event time in **milliseconds since epoch** (UTC)  
- `userId` – User’s internal ID (inum)  
- `username` – Username at time of the operation  
- `operationType` – `REGISTRATION`, `AUTHENTICATION`, or `FALLBACK`  
- `status` – `SUCCESS`, `FAILURE`, or `ATTEMPT`  
- `durationMs` – Operation duration in **milliseconds**  
- `authenticatorType` – e.g. `cross-platform`, `platform`, `security-key`  
- `nodeId` – Cluster node identifier (when running in a cluster)  
- When available: `ipAddress`, `userAgent`, `deviceInfo`, `errorReason`, `errorCategory`, `sessionId`, `applicationType`

```json
[
  {
    "id": "8092ad93-6d13-46aa-a288-c828741a0941",
    "timestamp": 1766767235681,
    "userId": "4a8f6f63-1306-4e2f-82bb-1c85da0284cc",
    "username": "admin",
    "operationType": "REGISTRATION",
    "status": "SUCCESS",
    "durationMs": 442,
    "authenticatorType": "cross-platform",
    "nodeId": "AA-A9-49-E1-A1-3E"
  }
]
```

### GET /metrics/entries/user/{userId}

Returns entries for a **single user** (by `userId`, i.e. inum). Useful for user-specific dashboards or support. Requires `startTime` and `endTime`.

### GET /metrics/entries/operation/{operationType}

Returns entries filtered by **operation type**: `REGISTRATION` or `AUTHENTICATION`. Requires `startTime` and `endTime`.

---

## Aggregation endpoints

Aggregations are **pre-computed** by the server (hourly, daily, weekly, monthly). They reduce the amount of data you need to process for dashboards and reports.

### GET /metrics/aggregations/{aggregationType}

Returns aggregation records that overlap the given time range. Each record summarizes one period (e.g. one hour or one day).

**Example request:**

```bash
curl -X GET "https://your-jans-server/jans-fido2/restv1/metrics/aggregations/HOURLY?startTime=2026-01-01T00:00:00&endTime=2026-01-01T23:59:59" \
  -H "Accept: application/json"
```

**Example response:** An array of aggregation objects. Each includes counts (attempts, successes, failures for registration and authentication), success rates, unique users, device type distribution, error counts, and a `period` identifier (e.g. `2026-01-01-12` for the 12:00 hour). Aggregation IDs for weekly data look like `WEEKLY_2026-W07` (ISO week 7 of 2026). **Durations in aggregations are in milliseconds.**

```json
[
  {
    "id": "HOURLY_2026-01-01-12",
    "aggregationType": "HOURLY",
    "startTime": 1767268800000,
    "endTime": 1767272400000,
    "uniqueUsers": 1,
    "registrationAttempts": 6,
    "registrationSuccesses": 2,
    "registrationFailures": 4,
    "authenticationAttempts": 0,
    "registrationSuccessRate": 0.3333333333333333,
    "deviceTypes": { "cross-platform": 2 },
    "errorCounts": {},
    "period": "2026-01-01-12"
  }
]
```

### GET /metrics/aggregations/{aggregationType}/summary

Returns a **single summary** over all aggregations in the time range: total registrations, total authentications, total operations, total fallbacks, and average success rates. Useful for “last 24 hours” or “last month” KPIs.

**Example response:**

```json
{
  "totalRegistrations": 6,
  "totalOperations": 6,
  "totalFallbacks": 0,
  "totalAuthentications": 0,
  "avgRegistrationSuccessRate": 0.3333333333333333,
  "avgAuthenticationSuccessRate": 0.0
}
```

---

## Analytics endpoints

These endpoints provide **analyzed insights** rather than raw or pre-aggregated data. They answer questions like “How is adoption?” or “What is the error rate?”

### GET /metrics/analytics/adoption

Returns **user adoption** metrics: how many new users (first registration in the period), how many returning users, total unique users, and adoption rate (new users / total unique users). Helps measure rollout and engagement.

**Example response:**

```json
{
  "newUsers": 1,
  "returningUsers": 0,
  "adoptionRate": 1.0,
  "totalUniqueUsers": 1
}
```

### GET /metrics/analytics/performance

Returns **performance statistics**: average, minimum, and maximum duration (**in milliseconds**) for registration and authentication. Use this to monitor latency and spot slow operations.

### GET /metrics/analytics/devices

Returns **device analytics**: counts or distributions by device type, operating system, browser, and authenticator type. Device types are derived from authenticator type (e.g. **platform**, **cross-platform**, and optionally **security-key**). Helps understand which platforms and clients are in use.

### GET /metrics/analytics/errors

Returns **error analysis**: error counts by category, top error reasons, overall success rate, and failure rate. Use for troubleshooting and alerting on high failure rates.

### GET /metrics/analytics/trends/{aggregationType}

Returns **trend data** over time for the chosen aggregation granularity (HOURLY, DAILY, etc.), including data points, trend direction (e.g. INCREASING, STABLE), growth rate, and insights (e.g. peak usage period).

### GET /metrics/analytics/comparison/{aggregationType}

Compares the **current period** with previous periods (e.g. this month vs last month). Optional query parameter `periods` (2–12, default 2) controls how many periods to compare. Response includes current and previous period metrics and percentage changes.

---

## Utility endpoints

### GET /metrics/config

Returns the **current metrics configuration** as seen by the server: whether metrics and aggregation are enabled, retention days, device info collection, error categorization, performance metrics, and the list of supported aggregation types. Use this to verify configuration without checking config files.

**Example response:**

```json
{
  "metricsEnabled": true,
  "aggregationEnabled": true,
  "retentionDays": 90,
  "deviceInfoCollection": true,
  "errorCategorization": true,
  "performanceMetrics": true,
  "supportedAggregationTypes": ["HOURLY", "DAILY", "WEEKLY", "MONTHLY"]
}
```

### GET /metrics/health

Returns the **health status** of the metrics service. HTTP 200 when the service is UP, HTTP 503 when it is DOWN (e.g. DB unavailable). Response body includes `status`, `metricsEnabled`, `aggregationEnabled`, and a timestamp.

---

## Configuration reference

Metrics behaviour is controlled by FIDO2 configuration. The following gives an overview; the exact property names may depend on your deployment. Use `GET /metrics/config` to see the effective values.

| Property | Default | Description |
|----------|---------|-------------|
| `fido2.metrics.enabled` | true | Master switch for metrics collection. If false, no entries are stored. |
| `fido2.metrics.aggregation.enabled` | true | Enables automatic computation of hourly/daily/weekly/monthly aggregations. |
| `fido2.metrics.retention.days` | 90 | Number of days to retain metrics data. Older entries and aggregations are removed. |
| `fido2.metrics.device.info.collection` | true | Whether to collect and store device information (browser, OS, device type, etc.). |
| `fido2.metrics.error.categorization` | true | Whether to categorize errors for the error analysis endpoint. |
| `fido2.metrics.performance.enabled` | true | Whether to collect performance metrics (durations). |

**Data retention:** Once retention is configured, the server automatically cleans up metrics entries and aggregations older than the retention period.

---

## How aggregations work (schedule and period IDs)

- **Raw entries** are written as each registration or authentication happens.  
- **Aggregations** are computed on a schedule and stored. The API returns stored aggregations whose time range overlaps your `startTime` and `endTime`; it does not compute aggregations on the fly from raw data.

**Typical schedule (can be overridden via cron-style config):**

| Type | When it runs | What the period covers |
|------|----------------|------------------------|
| HOURLY | At :05 past each hour | Previous full hour (e.g. 12:00–13:00) |
| DAILY | 01:10 UTC | Previous full day |
| WEEKLY | Monday 01:15 UTC | Previous ISO week (Monday–Sunday) |
| MONTHLY | 1st of month 01:20 UTC | Previous full month |

**Period ID examples:**

- **HOURLY:** `2026-01-01-12` = hour 12 (noon) on 2026-01-01.  
- **DAILY:** date-based (e.g. `2026-01-01`).  
- **WEEKLY:** `WEEKLY_2026-W07` = ISO week 7 of 2026.  
- **MONTHLY:** month-based (e.g. `2026-01`).

Data for the **current** hour may appear only a few minutes after the hour, once the hourly aggregation job has run.

**Cluster behaviour:** In a cluster, aggregation jobs use a distributed lock. If the cluster lock is unavailable (e.g. `ou=node` not configured), the job still runs aggregation in a **single-node fallback** mode and logs that behaviour. So aggregation continues to work even when the cluster is not fully configured.

---

## Understanding response fields

- **Durations** (e.g. `durationMs`, `registrationAvgDuration`, `authenticationAvgDuration`) are always in **milliseconds**.  
- **Device types** in aggregations and device analytics come from authenticator type: **platform** (built-in), **cross-platform** (roaming), and optionally **security-key**.  
- **Success rates** are in the range 0.0–1.0 (e.g. 0.33 = 33%).  
- **Adoption rate** = `newUsers / totalUniqueUsers` (0.0–1.0).  
- **Timestamps** in raw entries are milliseconds since epoch (UTC). Aggregation `startTime`/`endTime` are also epoch milliseconds.

---

## Examples and use cases

### Dashboard integration

You can call the analytics endpoints from a web dashboard to show adoption and performance. Example in JavaScript:

```javascript
const adoptionResponse = await fetch(
  'https://your-jans-server/jans-fido2/restv1/metrics/analytics/adoption?' +
  'startTime=2026-01-01T00:00:00&endTime=2026-01-01T23:59:59'
);
const adoption = await adoptionResponse.json();

console.log('New users:', adoption.newUsers);
console.log('Adoption rate:', (adoption.adoptionRate * 100).toFixed(2) + '%');
console.log('Total unique users:', adoption.totalUniqueUsers);
```

### Monitoring and alerting

Use the error analysis endpoint to monitor failure rate and trigger alerts when it exceeds a threshold. Example in Bash (requires `jq` and `bc`):

```bash
ERROR_RESPONSE=$(curl -s "https://your-jans-server/jans-fido2/restv1/metrics/analytics/errors?startTime=2026-01-01T00:00:00&endTime=2026-01-01T23:59:59")
FAILURE_RATE=$(echo "$ERROR_RESPONSE" | jq -r '.failureRate')

if (( $(echo "$FAILURE_RATE > 0.1" | bc -l) )); then
  echo "ALERT: High failure rate detected: $FAILURE_RATE"
  # Send notification (email, Slack, etc.)
fi
```

### Reporting

Use the aggregation summary to build monthly or weekly reports. Example in Python:

```python
import requests
from datetime import datetime, timedelta

end_date = datetime.now()
start_date = end_date - timedelta(days=30)
start_time = start_date.strftime('%Y-%m-%dT00:00:00')
end_time = end_date.strftime('%Y-%m-%dT23:59:59')

response = requests.get(
    'https://your-jans-server/jans-fido2/restv1/metrics/aggregations/MONTHLY/summary',
    params={'startTime': start_time, 'endTime': end_time}
)
summary = response.json()

print('Total registrations:', summary['totalRegistrations'])
print('Total authentications:', summary['totalAuthentications'])
print('Avg registration success rate:', f"{summary['avgRegistrationSuccessRate'] * 100:.2f}%")
```

---

## Troubleshooting

### Empty results ([])

**Symptom:** Endpoints return an empty array or empty-looking response.

**What to check:**

- Call `GET /metrics/config` and confirm `metricsEnabled` (and `aggregationEnabled` if using aggregation endpoints) is true.  
- Ensure there is actually data in the requested time range (e.g. registrations or authentications occurred).  
- For aggregation endpoints: aggregations run on a schedule; data for the current hour may appear only a few minutes after the hour (e.g. after the hourly job runs).

### 503 Service Unavailable

**Symptom:** Health check or other endpoints return HTTP 503.

**What to check:**

- Database connectivity: the metrics service depends on the same persistence layer as the FIDO2 server.  
- Application logs for the FIDO2 service (e.g. jans-fido2 logs) for errors during startup or when handling requests.

### 400 Bad Request (time format)

**Symptom:** Request returns 400 with a message about time or parameters.

**What to check:**

- Use ISO 8601 format for `startTime` and `endTime`, e.g. `2026-01-01T00:00:00` or `2026-01-01T12:00:00Z`.  
- Ensure the time range is valid (e.g. `startTime` before `endTime`).

### Aggregations not updating

**Symptom:** Aggregation or analytics endpoints show no new data or outdated data.

**What to check:**

- Application logs for a message like “FIDO2 metrics aggregation scheduler initialized” to confirm the scheduler is running.  
- Configuration for aggregation cron expressions if you have customized them.  
- If running in a cluster, verify that the distributed lock or cluster configuration for the aggregation jobs is correct. If the lock is unavailable, the job should still run in single-node fallback mode (check logs).

### HTTP status codes summary

| Code | Meaning | Typical action |
|------|---------|----------------|
| 200 | Success | Response body contains the requested data. |
| 400 | Bad Request | Check parameters (e.g. time format, required query/path params). |
| 403 | Forbidden | Metrics may be disabled or access restricted; check config and infrastructure. |
| 500 | Internal Server Error | Check server logs for details. |
| 503 | Service Unavailable | Service or dependency (e.g. DB) unavailable; check health and connectivity. |

---

## Metrics and formulas reference

**Registration:**  
`registrationAttempts`, `registrationSuccesses`, `registrationFailures`, `registrationSuccessRate` (0.0–1.0), `registrationAvgDuration` (milliseconds).

**Authentication:**  
`authenticationAttempts`, `authenticationSuccesses`, `authenticationFailures`, `authenticationSuccessRate` (0.0–1.0), `authenticationAvgDuration` (milliseconds).

**User:**  
`totalUniqueUsers`, `newUsers` (first registration in period), `returningUsers`, `adoptionRate` (new users / total unique users).

**Device:**  
`deviceTypes` (e.g. platform, cross-platform), `authenticatorTypes`, `operatingSystems`, `browsers`.

**Error:**  
`errorCategories`, `topErrors`, `successRate`, `failureRate`.

**Formulas:**

- **Success rate:** `successfulOperations / totalOperations`  
- **Adoption rate:** `newUsers / totalUniqueUsers`  
- **Average duration:** `sum(durations) / count(operations)` (durations in milliseconds)

---

## Additional resources

- **[OpenAPI (Swagger) specification](jansFido2Swagger.yaml)** – Full API spec (paths, parameters, response schemas) for the FIDO2 Metrics endpoints.

**Last updated:** February 2026 · Version 1.0.0
