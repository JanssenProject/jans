# FIDO2 Metrics – Complete Guide

**Understand, configure, and use the FIDO2 Metrics API for dashboards, monitoring, and reporting.**

This guide explains everything about FIDO2 metrics: what is tracked, how data is stored and aggregated, how to call the API, and how to interpret the results. Whether you are integrating a dashboard, setting up alerts, or troubleshooting, you'll find the answers here.

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
  - `userId`: the user's internal ID (inum), not the username.

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

**Example response:** An array of entry objects. Fields that are null are omitted.

**Response fields explained:**

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Unique identifier (UUID) for this specific metric entry. Use this to reference individual events. |
| `timestamp` | integer | Event time in **milliseconds since epoch** (UTC). Convert to date: `new Date(timestamp)` in JavaScript or similar. Example: `1766767235681` = 2026-01-26T12:00:35.681Z. |
| `userId` | string | User's internal ID (inum). This is a stable identifier for correlating user activity across entries. Not the username. |
| `username` | string | Human-readable username at time of the operation. May change if user renames account; use `userId` for stable tracking. |
| `operationType` | string | Type of operation: `REGISTRATION` (passkey enrollment), `AUTHENTICATION` (passkey sign-in), or `FALLBACK` (user chose alternative method). |
| `status` | string | Operation outcome: `ATTEMPT` (user started but hasn't finished yet), `SUCCESS` (completed successfully), or `FAILURE` (completed with an error). See note below about how these relate. |
| `durationMs` | integer | Time from operation start to completion in **milliseconds**. Typical values: 200–800ms for authentication, 400–2000ms for registration. Only set for SUCCESS/FAILURE entries, not ATTEMPT. High values (>3000ms) may indicate network issues. |
| `authenticatorType` | string | Type of authenticator used: `platform` (built-in like TouchID, FaceID, Windows Hello), `cross-platform` (external like YubiKey, USB security key), or `security-key`. |
| `nodeId` | string | Identifier of the cluster node that processed this request (e.g. MAC address). Useful for debugging in multi-node deployments. |
| `ipAddress` | string | Client's IP address. Useful for geo-analysis or detecting suspicious patterns. Present when available from request headers. |
| `userAgent` | string | Full browser user-agent string. Used to derive `deviceInfo`. Example: `Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0...` |
| `deviceInfo` | object | Parsed device details containing `browser` (e.g. "Chrome"), `os` (e.g. "Windows"), `deviceType` (e.g. "desktop"). Only present when `deviceInfoCollection` is enabled. |
| `errorReason` | string | Human-readable error message explaining why the operation failed. Only present when `status` is `FAILURE`. Example: "User cancelled the operation". |
| `errorCategory` | string | Categorized error type for grouping failures. Common values: `USER_CANCELLED`, `TIMEOUT`, `INVALID_CREDENTIAL`, `NETWORK_ERROR`, `NOT_ALLOWED`, `SECURITY_ERROR`. Only present when `status` is `FAILURE`. |
| `sessionId` | string | Session identifier linking this operation to a user session. Useful for correlating multiple operations in the same session. |
| `applicationType` | string | Application or relying party identifier. Useful when multiple applications share the same FIDO2 server. |

**Note on status:** Each FIDO2 operation generates **separate entries**:
1. An `ATTEMPT` entry is created when the user **starts** the operation (clicks "Register" or "Sign in")
2. A `SUCCESS` or `FAILURE` entry is created when the operation **completes**

This means: if you see an `ATTEMPT` without a matching `SUCCESS`/`FAILURE` for the same user and session, the user **dropped off** (started but never finished). You can calculate drop-off by comparing ATTEMPT count vs (SUCCESS + FAILURE) count.

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

**Example request:**

```bash
curl -X GET "https://your-jans-server/jans-fido2/restv1/metrics/entries/user/4a8f6f63-1306-4e2f-82bb-1c85da0284cc?startTime=2026-01-01T00:00:00&endTime=2026-01-31T23:59:59" \
  -H "Accept: application/json"
```

**Response format:** Same as `/metrics/entries` (array of entry objects), filtered to only include entries for the specified user.

### GET /metrics/entries/operation/{operationType}

Returns entries filtered by **operation type**: `REGISTRATION` or `AUTHENTICATION`. Requires `startTime` and `endTime`.

**Example request:**

```bash
curl -X GET "https://your-jans-server/jans-fido2/restv1/metrics/entries/operation/REGISTRATION?startTime=2026-01-01T00:00:00&endTime=2026-01-31T23:59:59" \
  -H "Accept: application/json"
```

**Response format:** Same as `/metrics/entries` (array of entry objects), filtered to only include entries matching the specified operation type.

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

**Response fields explained:**

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Unique identifier combining type and period. Format: `{TYPE}_{PERIOD}`. Examples: `HOURLY_2026-01-01-12`, `DAILY_2026-01-01`, `WEEKLY_2026-W07`, `MONTHLY_2026-01`. Use this to reference specific aggregations. |
| `aggregationType` | string | Granularity of this aggregation: `HOURLY` (1 hour), `DAILY` (24 hours), `WEEKLY` (7 days, ISO week Mon-Sun), or `MONTHLY` (calendar month). |
| `startTime` | integer | Period start time in **milliseconds since epoch** (UTC). Inclusive. Example: `1767268800000` = 2026-01-01T12:00:00Z. |
| `endTime` | integer | Period end time in **milliseconds since epoch** (UTC). Exclusive (end of period). Duration = endTime - startTime. |
| `period` | string | Human-readable period identifier. Format varies by type: `2026-01-01-12` (hour 12 on Jan 1), `2026-01-01` (day), `2026-W07` (ISO week 7), `2026-01` (January). |
| `uniqueUsers` | integer | Count of **distinct users** (by userId) who had any FIDO2 activity in this period. Use this to track active user counts over time. |
| `registrationAttempts` | integer | Total number of registration operations that reached SUCCESS or FAILURE status. Does not include in-progress ATTEMPTs. |
| `registrationSuccesses` | integer | Number of registrations that completed successfully. New passkeys created. |
| `registrationFailures` | integer | Number of registrations that failed. Calculated as `registrationAttempts - registrationSuccesses`. |
| `registrationSuccessRate` | number | Success rate for registrations (0.0–1.0). Calculated as `registrationSuccesses / registrationAttempts`. **Healthy range:** >0.80. Example: 0.85 = 85% of registration attempts succeeded. |
| `authenticationAttempts` | integer | Total number of authentication operations that reached SUCCESS or FAILURE status. |
| `authenticationSuccesses` | integer | Number of authentications that completed successfully. Successful passkey sign-ins. |
| `authenticationFailures` | integer | Number of authentications that failed. |
| `authenticationSuccessRate` | number | Success rate for authentications (0.0–1.0). **Healthy range:** >0.90. Authentication typically has higher success rates than registration. |
| `deviceTypes` | object | Count of operations by authenticator type. Keys: `platform` (built-in authenticators), `cross-platform` (external security keys). Example: `{"platform": 45, "cross-platform": 12}`. |
| `errorCounts` | object | Count of errors by category. Keys are error categories (e.g. `USER_CANCELLED`, `TIMEOUT`), values are counts. Empty `{}` means no errors—good! |

### GET /metrics/aggregations/{aggregationType}/summary

Returns a **single summary** over all aggregations in the time range: total registrations, total authentications, total operations, total fallbacks, and average success rates. Useful for "last 24 hours" or "last month" KPIs.

**Example request:**

```bash
curl -X GET "https://your-jans-server/jans-fido2/restv1/metrics/aggregations/DAILY/summary?startTime=2026-01-01T00:00:00&endTime=2026-01-31T23:59:59" \
  -H "Accept: application/json"
```

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

**Response fields explained:**

| Field | Type | Description |
|-------|------|-------------|
| `totalRegistrations` | integer | Total count of registration operations across all aggregation periods in the time range. Use this for "X passkeys registered this month" metrics. |
| `totalAuthentications` | integer | Total count of authentication operations. Use this for "X passkey sign-ins this month" metrics. Typically much higher than registrations in mature deployments. |
| `totalOperations` | integer | Sum of `totalRegistrations + totalAuthentications`. Use this as your overall "FIDO2 activity" metric. |
| `totalFallbacks` | integer | Count of times users chose an alternative authentication method instead of using their passkey. High numbers may indicate passkey UX issues or users forgetting they have passkeys. |
| `avgRegistrationSuccessRate` | number | Average of registration success rates across all periods (0.0–1.0). **Healthy range:** >0.80. Note: This is an average of rates, not a single rate across all operations. |
| `avgAuthenticationSuccessRate` | number | Average of authentication success rates across all periods (0.0–1.0). **Healthy range:** >0.90. Authentication typically has higher success rates than registration. |

**Interpreting the summary:**
- **Mature deployment:** High `totalAuthentications` relative to `totalRegistrations` (10:1 or higher ratio). Users are actively using their passkeys.
- **Rollout phase:** `totalRegistrations` may equal or exceed `totalAuthentications` as users are onboarding.
- **Concerning signals:** High `totalFallbacks` or low success rates (<0.70) warrant investigation.

---

## Analytics endpoints

These endpoints provide **analyzed insights** rather than raw or pre-aggregated data. They answer questions like "How is adoption?" or "What is the error rate?"

### GET /metrics/analytics/adoption

Returns **user adoption** metrics: how many new users (first registration in the period), how many returning users, total unique users, and adoption rate (new users / total unique users). Helps measure rollout and engagement.

**Example request:**

```bash
curl -X GET "https://your-jans-server/jans-fido2/restv1/metrics/analytics/adoption?startTime=2026-01-01T00:00:00&endTime=2026-01-31T23:59:59" \
  -H "Accept: application/json"
```

**Example response:**

```json
{
  "newUsers": 1,
  "returningUsers": 0,
  "adoptionRate": 1.0,
  "totalUniqueUsers": 1
}
```

**Response fields explained:**

| Field | Type | Description |
|-------|------|-------------|
| `newUsers` | integer | Count of users who registered a passkey **for the first time ever** during this period. These are users who had never used FIDO2 before. High numbers indicate successful onboarding/rollout. |
| `returningUsers` | integer | Count of users who **already had** a passkey before this period and used FIDO2 again (authentication or additional registration). High numbers indicate good retention and continued passkey usage. |
| `totalUniqueUsers` | integer | Total distinct users with **any** FIDO2 activity in the period. Equals the unique count of userIds across all entries. Use this as your "active FIDO2 users" metric. |
| `adoptionRate` | number | Ratio of new users to total unique users (0.0–1.0). **Interpretation:** 1.0 = all users are new (initial rollout phase); 0.0 = all users are returning (mature deployment); 0.3 = 30% are first-time users. **Typical progression:** High during rollout (0.6–0.9), decreasing over time as user base matures (0.1–0.3). |

**Understanding adoption metrics:**
- **During rollout:** Expect high `newUsers` and high `adoptionRate` (0.7+). This is good—users are onboarding.
- **Mature deployment:** Expect high `returningUsers`, low `adoptionRate` (0.1–0.3). Most activity is from existing passkey users.
- **Stalled adoption:** If `newUsers` drops to near zero but `totalUniqueUsers` is low, your rollout may have stalled.

### GET /metrics/analytics/performance

Returns **performance statistics**: average, minimum, and maximum duration (**in milliseconds**) for registration and authentication. Use this to monitor latency and spot slow operations.

**Example request:**

```bash
curl -X GET "https://your-jans-server/jans-fido2/restv1/metrics/analytics/performance?startTime=2026-01-01T00:00:00&endTime=2026-01-31T23:59:59" \
  -H "Accept: application/json"
```

**Example response:**

```json
{
  "registrationAvgDuration": 523.4,
  "registrationMinDuration": 312,
  "registrationMaxDuration": 1842,
  "authenticationAvgDuration": 287.6,
  "authenticationMinDuration": 156,
  "authenticationMaxDuration": 892
}
```

**Response fields explained:**

| Field | Type | Description |
|-------|------|-------------|
| `registrationAvgDuration` | number | Average time (ms) to complete a passkey registration. **Typical range: 400–1500ms.** Values >2000ms may indicate slow authenticators or network latency. Lower is better for user experience. |
| `registrationMinDuration` | integer | Fastest registration observed (ms). Represents best-case performance. **Typical: 300–600ms.** Use as baseline for what's achievable. |
| `registrationMaxDuration` | integer | Slowest registration observed (ms). **Alert threshold suggestion: >5000ms.** High values may indicate: slow authenticators, network timeouts, or users taking time to complete biometric prompts. |
| `authenticationAvgDuration` | number | Average time (ms) to complete a passkey authentication. **Typical range: 150–500ms.** Authentication is usually faster than registration since no key generation is needed. |
| `authenticationMinDuration` | integer | Fastest authentication observed (ms). **Typical: 100–250ms.** Platform authenticators (TouchID, Windows Hello) are usually fastest. |
| `authenticationMaxDuration` | integer | Slowest authentication observed (ms). **Alert threshold suggestion: >3000ms.** High values may indicate network issues or users struggling with their authenticator. |

**Notes:**
- All durations are in **milliseconds** (1000ms = 1 second).
- Fields are **omitted entirely** if there are no operations of that type in the time range. For example, if no registrations occurred, all `registration*` fields will be absent from the response.
- **Baseline guidance:** Monitor average durations over time. A sudden increase (e.g., avg jumps from 400ms to 1200ms) may indicate infrastructure problems.
- **Alerting suggestion:** Set alerts when `maxDuration` exceeds 5000ms or when `avgDuration` increases by more than 50% from baseline.

### GET /metrics/analytics/devices

Returns **device analytics**: counts or distributions by device type, operating system, browser, and authenticator type. Device types are derived from authenticator type (e.g. **platform**, **cross-platform**, and optionally **security-key**). Helps understand which platforms and clients are in use.

**Example request:**

```bash
curl -X GET "https://your-jans-server/jans-fido2/restv1/metrics/analytics/devices?startTime=2026-01-01T00:00:00&endTime=2026-01-31T23:59:59" \
  -H "Accept: application/json"
```

**Example response:**

```json
{
  "deviceTypes": {
    "desktop": 45,
    "mobile": 32,
    "tablet": 8
  },
  "authenticatorTypes": {
    "platform": 52,
    "cross-platform": 33
  },
  "browsers": {
    "Chrome": 48,
    "Safari": 22,
    "Firefox": 10,
    "Edge": 5
  },
  "operatingSystems": {
    "Windows": 38,
    "macOS": 25,
    "iOS": 12,
    "Android": 10
  }
}
```

**Response fields explained:**

| Field | Type | Description |
|-------|------|-------------|
| `deviceTypes` | object | Count of operations by device type (desktop, mobile, tablet). Based on user-agent parsing. |
| `authenticatorTypes` | object | Count by authenticator type: **platform** (built-in like TouchID, Windows Hello) vs **cross-platform** (roaming like USB security keys). |
| `browsers` | object | Count by browser (Chrome, Safari, Firefox, Edge, etc.). |
| `operatingSystems` | object | Count by OS (Windows, macOS, iOS, Android, Linux, etc.). |

**Notes:**
- Fields with no data are returned as empty objects `{}`.
- Device info depends on `deviceInfoCollection` being enabled in configuration.

### GET /metrics/analytics/errors

Returns **error analysis**: error counts by category, top error reasons, overall success rate, and failure rate. Use for troubleshooting and alerting on high failure rates.

**Example request:**

```bash
curl -X GET "https://your-jans-server/jans-fido2/restv1/metrics/analytics/errors?startTime=2026-01-01T00:00:00&endTime=2026-01-31T23:59:59" \
  -H "Accept: application/json"
```

**Example response:**

```json
{
  "errorCategories": {
    "USER_CANCELLED": 12,
    "TIMEOUT": 8,
    "INVALID_CREDENTIAL": 5,
    "NETWORK_ERROR": 3
  },
  "topErrors": {
    "User cancelled the operation": 12,
    "Operation timed out": 8,
    "Credential not recognized": 5
  },
  "successRate": 0.72,
  "failureRate": 0.28
}
```

**Response fields explained:**

| Field | Type | Description |
|-------|------|-------------|
| `errorCategories` | object | Count of errors grouped by category. Keys are category codes, values are counts. **Common categories:** `USER_CANCELLED` (user clicked cancel), `TIMEOUT` (operation timed out), `INVALID_CREDENTIAL` (credential not recognized), `NETWORK_ERROR` (connectivity issues), `NOT_ALLOWED` (browser/security policy blocked), `SECURITY_ERROR` (security constraint violation). |
| `topErrors` | object | Count of errors by specific error message. Keys are the actual error messages, values are counts. Use this to identify the most frequent specific issues users encounter. Example: `"User cancelled the operation": 12` means 12 users clicked cancel. |
| `successRate` | number | Ratio of successful operations to total completed operations (0.0–1.0). **Interpretation:** 0.72 = 72% of operations succeeded. **Healthy range:** >0.85 (85%) is typical for mature deployments. <0.70 may indicate UX issues or technical problems. |
| `failureRate` | number | Ratio of failed operations to total (0.0–1.0). Always equals `1 - successRate`. **Alert threshold suggestion:** Set alerts if failureRate exceeds 0.15 (15%). |

**Notes:**
- `successRate + failureRate = 1.0` (100% of completed operations). These rates are calculated from operations that reached a final state (SUCCESS or FAILURE), not from ATTEMPT entries.
- Empty `errorCategories` and `topErrors` (`{}`) indicate no failures occurred in the time range—this is good!
- **Actionable insights:** High `USER_CANCELLED` counts may indicate confusing UX. High `TIMEOUT` counts may indicate slow network or authenticator issues. High `INVALID_CREDENTIAL` may indicate users trying wrong passkeys.
- Error categorization depends on `errorCategorization` being enabled in configuration.

### GET /metrics/analytics/trends/{aggregationType}

Returns **trend data** over time for the chosen aggregation granularity (HOURLY, DAILY, etc.), including data points, trend direction (e.g. INCREASING, STABLE), growth rate, and insights (e.g. peak usage period).

**Example request:**

```bash
curl -X GET "https://your-jans-server/jans-fido2/restv1/metrics/analytics/trends/DAILY?startTime=2026-01-01T00:00:00&endTime=2026-01-31T23:59:59" \
  -H "Accept: application/json"
```

**Example response:**

```json
{
  "dataPoints": [
    {
      "timestamp": 1735689600000,
      "period": "2026-01-01",
      "metrics": {
        "registrationAttempts": 15,
        "registrationSuccesses": 12,
        "authenticationAttempts": 45,
        "authenticationSuccesses": 42
      }
    },
    {
      "timestamp": 1735776000000,
      "period": "2026-01-02",
      "metrics": {
        "registrationAttempts": 18,
        "registrationSuccesses": 16,
        "authenticationAttempts": 52,
        "authenticationSuccesses": 50
      }
    }
  ],
  "growthRate": 0.15,
  "trendDirection": "INCREASING",
  "insights": {
    "peakPeriod": "2026-01-15",
    "peakOperations": 95,
    "averageOperations": 62
  }
}
```

**Response fields explained:**

| Field | Type | Description |
|-------|------|-------------|
| `dataPoints` | array | Array of data points, **one per aggregation period** in chronological order. Each contains: `timestamp` (period start in epoch ms), `period` (human-readable ID like "2026-01-01"), and `metrics` (object with registrationAttempts, registrationSuccesses, authenticationAttempts, authenticationSuccesses, etc.). Use this to plot charts over time. |
| `growthRate` | number | Overall growth rate comparing first period to last period. **Interpretation:** 0.15 = 15% growth, -0.10 = 10% decline, 0.0 = flat. Calculated as `(last - first) / first`. Use this for "month-over-month growth" metrics. |
| `trendDirection` | string | Simplified trend indicator: `INCREASING` (sustained growth), `DECREASING` (sustained decline), or `STABLE` (relatively flat, <5% change). Use this for quick dashboard indicators (green/red/yellow). |
| `insights` | object | Computed insights including: `peakPeriod` (period ID with highest activity—useful for identifying busy times), `peakOperations` (count at peak), `averageOperations` (mean across all periods—use as baseline). |

**Using trend data:**
- **Dashboard charts:** Plot `dataPoints` over time to visualize adoption curves.
- **Quick status:** Use `trendDirection` for at-a-glance health indicators.
- **Capacity planning:** Use `peakOperations` to understand maximum load and `averageOperations` for typical load.

### GET /metrics/analytics/comparison/{aggregationType}

Compares the **current period** with previous periods (e.g. this month vs last month). Optional query parameter `periods` (2–12, default 2) controls how many periods to compare. Response includes current and previous period metrics and percentage changes.

**Example request:**

```bash
curl -X GET "https://your-jans-server/jans-fido2/restv1/metrics/analytics/comparison/MONTHLY?periods=2" \
  -H "Accept: application/json"
```

**Example response:**

```json
{
  "currentPeriod": {
    "totalRegistrations": 156,
    "totalAuthentications": 892,
    "totalFallbacks": 12,
    "totalOperations": 1048,
    "avgRegistrationSuccessRate": 0.85,
    "avgAuthenticationSuccessRate": 0.94
  },
  "previousPeriod": {
    "totalRegistrations": 134,
    "totalAuthentications": 756,
    "totalFallbacks": 18,
    "totalOperations": 890,
    "avgRegistrationSuccessRate": 0.82,
    "avgAuthenticationSuccessRate": 0.91
  },
  "comparison": {
    "totalOperationsChange": 17.75
  }
}
```

**Response fields explained:**

| Field | Type | Description |
|-------|------|-------------|
| `currentPeriod` | object | Summary metrics for the **most recent** period(s). Contains: `totalRegistrations`, `totalAuthentications`, `totalFallbacks`, `totalOperations`, `avgRegistrationSuccessRate`, `avgAuthenticationSuccessRate`. This represents your "current" performance. |
| `previousPeriod` | object | Summary metrics for the **preceding** period(s), same structure as `currentPeriod`. This is your comparison baseline. Use to answer "how are we doing compared to last month?" |
| `comparison` | object | Calculated percentage changes between periods. `totalOperationsChange` shows growth/decline as a percentage. **Interpretation:** 17.75 = 17.75% more operations than previous period (growth). Negative values indicate decline. |

**Using comparison data:**
- **Executive reporting:** "FIDO2 usage grew 17.75% month-over-month"
- **Alerting:** Set alerts if `totalOperationsChange` is negative for consecutive periods (declining usage)
- **Success tracking:** Compare `avgRegistrationSuccessRate` between periods to see if UX improvements are working

**Notes:**
- The `periods` parameter controls how many consecutive periods to include in each summary (default 2). Higher values give broader comparisons but may dilute recent trends.
- Time ranges are **automatically aligned** to period boundaries (e.g. start of month for MONTHLY, start of week for WEEKLY). You don't need to calculate exact boundaries.

---

## Utility endpoints

### GET /metrics/config

Returns the **current metrics configuration** as seen by the server: whether metrics and aggregation are enabled, retention days, device info collection, error categorization, performance metrics, and the list of supported aggregation types. Use this to verify configuration without checking config files.

**Example request:**

```bash
curl -X GET "https://your-jans-server/jans-fido2/restv1/metrics/config" \
  -H "Accept: application/json"
```

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

**Response fields explained:**

| Field | Type | Description |
|-------|------|-------------|
| `metricsEnabled` | boolean | Whether metrics collection is enabled. |
| `aggregationEnabled` | boolean | Whether automatic aggregation jobs are enabled. |
| `retentionDays` | integer | Number of days metrics data is retained before cleanup. |
| `deviceInfoCollection` | boolean | Whether device info (browser, OS) is collected. |
| `errorCategorization` | boolean | Whether errors are categorized for analytics. |
| `performanceMetrics` | boolean | Whether durations are tracked. |
| `supportedAggregationTypes` | array | List of available aggregation granularities. |

### GET /metrics/health

Returns the **health status** of the metrics service. HTTP 200 when the service is UP, HTTP 503 when it is DOWN (e.g. DB unavailable). Response body includes `status`, `metricsEnabled`, `aggregationEnabled`, and a timestamp.

**Example request:**

```bash
curl -X GET "https://your-jans-server/jans-fido2/restv1/metrics/health" \
  -H "Accept: application/json"
```

**Example response (healthy):**

```json
{
  "status": "UP",
  "metricsEnabled": true,
  "aggregationEnabled": true,
  "serviceAvailable": true,
  "timestamp": "2026-01-15T10:30:00"
}
```

**Example response (unhealthy - HTTP 503):**

```json
{
  "status": "DOWN",
  "metricsEnabled": true,
  "aggregationEnabled": true,
  "serviceAvailable": false,
  "timestamp": "2026-01-15T10:30:00"
}
```

**Response fields explained:**

| Field | Type | Description |
|-------|------|-------------|
| `status` | string | Overall health: `UP` (HTTP 200) or `DOWN` (HTTP 503). |
| `metricsEnabled` | boolean | Whether metrics collection is enabled in configuration. |
| `aggregationEnabled` | boolean | Whether automatic aggregation is enabled. |
| `serviceAvailable` | boolean | Whether the metrics backend (database) is reachable. False triggers `DOWN` status. |
| `timestamp` | string | Current server time (ISO format, UTC). |

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

- Application logs for a message like "FIDO2 metrics aggregation scheduler initialized" to confirm the scheduler is running.  
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
