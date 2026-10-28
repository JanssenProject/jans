---
tags:
  - administration
  - fido2
  - passkeys
  - native apps
  - diagnostics
---

# Native App Readiness

Passkeys in an Android or iOS app only work when the relying party's domain vouches for the app. The domain
does that by serving two files under `/.well-known/`:

- `assetlinks.json` for Android
- `apple-app-site-association` for iOS

When one of them is missing, unreachable or out of date, the app fails with a generic error and nothing says
why. The readiness check compares the files an RP has actually deployed with the `androidApps` and `iosApps`
recorded for it under `rp` (see [Native applications of a relying
party](fido2-server-properties-config.md#native-applications-of-a-relying-party)) and reports what is wrong.

The check is read-only. It never changes configuration and never changes how a ceremony is decided.

## Enabling the check

`nativeReadinessEnabled` must be `true` to run the check at all. `nativeReadinessAllowExternalFetch` is needed
only for checking the files an RP has deployed; leave it `false` to get the configuration-only report. Both are
`false` by default, so an upgrade changes nothing until an administrator turns them on.

| Property | Effect |
| :--- | :--- |
| `nativeReadinessEnabled` | Master switch. While `false` the endpoint does no work and answers `{"enabled": false}`. |
| `nativeReadinessAllowExternalFetch` | Second, separate opt-in for contacting the RP's `/.well-known` files. With only `nativeReadinessEnabled` set, the check reports what is configured and does not fetch. |

The size, time and cache properties (`nativeReadinessMaxResponseBytes`, `nativeReadinessTimeoutMs`,
`nativeReadinessCacheTtlSeconds`) are described in [Native app
readiness](fido2-server-properties-config.md#native-app-readiness).

## Running a check

=== "FIDO2 server"

    ```
    POST /jans-fido2/restv1/rp/{rpConfigId}/readiness
    ```

    The FIDO2 server does not authenticate this call itself. Like the other diagnostic endpoints it relies on
    the infrastructure in front of it, so prefer the Config API route.

=== "Config API (fido2 plugin)"

    ```
    POST /fido2/rp/{rpConfigId}/readiness
    ```

    Served under the Config API base path. Requires the `https://jans.io/oauth/config/fido2.write` scope, or
    `https://jans.io/oauth/config/fido2.admin` or `https://jans.io/oauth/config/write-all`. A read scope is
    not enough, because the call makes the FIDO2 server contact an external host.

    The caller's `Authorization` header is forwarded to the FIDO2 server. The Config API identifies the caller
    by introspecting that token (the user's `inum`, else the user name, else the client id) and passes it on as
    the requester for the [audit](#audit). A value sent by the caller in a header is never used.

`rpConfigId` is the RP `id` exactly as configured, URL-encoded. The body is empty, or:

```json
{ "recheck": true }
```

Any other field is ignored. In particular, no URL, host, port or path can be supplied.

| Response | Meaning |
| :--- | :--- |
| `200` | The result below. |
| `400` | The body is not a JSON object. |
| `404` | No relying party is configured with that id. Nothing was fetched. |
| `429` | A [rate limit](#caching-and-rate-limits) refused the check. The `Retry-After` header gives the wait in seconds. Nothing was fetched. |
| `500` | The Config API route only: the FIDO2 server could not be reached or answered something else. |

### Result

```json
{
  "enabled": true,
  "externalFetch": true,
  "platforms": {
    "android": [
      { "check": "reachability.status", "status": "pass", "reason": "HTTP 200" },
      { "check": "content.android.fingerprint", "status": "fail",
        "reason": "fingerprint AB:CD:... for package com.example.app not listed" }
    ],
    "ios": [
      { "check": "content.ios.app", "status": "pass", "reason": "app T9A667JL6T.com.example.app listed in webcredentials.apps" },
      { "check": "content.ios.cdn", "status": "warn", "reason": "iOS reads this file through app-site-association.cdn-apple.com, which caches aggressively; a correct file may not take effect immediately" }
    ]
  },
  "checkedAt": "2026-10-21T09:30:00Z",
  "fromCache": false,
  "nextRecheckAllowedAt": "2026-10-21T09:31:00Z"
}
```

Each check has a `status` and a short `reason`:

| `status` | Meaning |
| :--- | :--- |
| `pass` | The check holds. |
| `warn` | Worth a look, but the file can still work. |
| `fail` | The platform will reject the file, or the file does not match the configuration. |

The `reason` names only configured values (a package name, a fingerprint, `TeamID.BundleID`), HTTP status
codes and counts. **The result never contains the content of a fetched file**, a redirect target, a resolved
address or a parser error message.

A platform with no configured apps is not fetched. Its list holds one `pass` saying so.

## Checks

### Reachability

Run once per file, for every platform that has apps configured.

| Check | `pass` | `warn` | `fail` |
| :--- | :--- | :--- | :--- |
| `reachability.status` | HTTP 200 | | Any other status (the code is stated), a redirect, or no response: the name does not resolve, resolves to an address that is not allowed, the TLS connection failed, or the host could not be reached. |
| `reachability.redirect` | | | Present only for a 3xx answer. Says whether the redirect goes to a path on the same host, to `www.` plus the same name, or to a different host. Neither Android nor iOS follows redirects. |
| `reachability.host` | Present for any non-redirect answer, naming the RP ID host that answered. | | |
| `reachability.content-type` | `application/json` (parameters such as `charset` are ignored). Present for HTTP 200 only. | A different type, for `apple-app-site-association` only. | No `Content-Type` header, or a different type for `assetlinks.json`. |
| `reachability.limits` | The response is within the size limit. | | The response is larger than the limit, or nothing arrived in time. |

### Content

Run only when the file answered HTTP 200 and was not cut off at the size limit.

| Check | `pass` | `warn` | `fail` |
| :--- | :--- | :--- | :--- |
| `content.parse` | | | The body is empty, is not valid JSON, is too deeply nested, or is not the expected shape (a JSON array for `assetlinks.json`, an object for `apple-app-site-association`). Nothing else is reported for that file. |
| `content.android.package` | A statement lists the configured package. | | The package is not listed. |
| `content.android.fingerprint` | Every configured fingerprint is listed. Comparison ignores case and colons. | The file lists fingerprints for the package that are not configured. The count is stated. | A configured fingerprint is missing, or an app has no fingerprint configured. |
| `content.android.relation` | A statement for the package that lists each configured fingerprint carries `delegate_permission/common.get_login_creds`. | | No such statement does. The relation on a different statement than the one holding the fingerprints is not enough. |
| `content.ios.app` | `webcredentials.apps` contains `TeamID.BundleID` (case-insensitive). | | It does not, or an app is configured without a team ID or bundle ID. |
| `content.drift` | The file matches what Jans would generate from the configuration. | The deployed file differs, for example after a signing key rotation. Counts of missing and unexpected entries are stated. Order, whitespace and unrelated extra content are ignored. | Never. |
| `content.ios.cdn` | | Always, for the Apple file, whether the other checks pass or fail. | Never. |

`content.drift` and `content.ios.cdn` are advice, not verdicts, and never fail a check on their own.

When nothing is fetched, the check reports `config.android` and `config.ios` instead (a `pass` when apps are
configured, a `warn` when none are), `fetch.skipped` when external fetching is off, and `config.rp-id` as a
`fail` when the configured RP ID is not a public host name that can be checked.

### Apple CDN caching

iOS does not read `apple-app-site-association` from your server when the app is installed. It reads a copy held by
Apple's CDN, `app-site-association.cdn-apple.com`, which caches aggressively. The check cannot see that copy
and does not contact the CDN, so a correct file can still take a while to take effect after you deploy it. This
is why `content.ios.cdn` is always a `warn`.

### CMS-signed `apple-app-site-association`

An `apple-app-site-association` file can be served CMS-signed, which is not JSON. The check reads the file as
JSON only, so a signed file is reported as `content.parse` `fail` and its contents are not checked. This is a
limitation of the check, not necessarily a fault of the file: if you serve a signed file, treat that `fail` as
expected and verify the file another way.

## What the server will and will not contact

Fetching is the risky part of this feature, so it is deliberately narrow:

- **Only the configured RP ID is contacted.** The caller selects a configured entry; it cannot supply a host,
  URL, port or path. An id that is not configured gets `404` and no outbound request.
- **The RP ID must be a public host name.** An IP address, `localhost`, a name with a port and similar values
  are refused before any lookup.
- **HTTPS on port 443 only**, at the two fixed paths `/.well-known/assetlinks.json` and
  `/.well-known/apple-app-site-association`.
- **Redirects are never followed.** A 3xx answer is reported and nothing more.
- **Blocked address ranges.** After the name is resolved, the connection is made only to an address the
  server has checked. Loopback, private, link-local (which includes the `169.254.169.254` cloud metadata
  address), unique-local IPv6, carrier-grade NAT, multicast, documentation, benchmarking, unspecified and
  reserved ranges are refused, and so are IPv6 forms that wrap one of those IPv4 addresses.
- **Bounded.** The response size is capped at 256 KiB and the HTTP fetch at 10 seconds, whatever the properties
  say. The name lookup happens before that deadline starts and is bounded only by the server's DNS resolver, so
  a slow lookup can make a whole check take longer.
- **Nothing sent.** No cookies, credentials, proxy or server tokens. Only `Accept` and `User-Agent` are set.

## Caching and rate limits

A result is kept in memory, per server node, for `nativeReadinessCacheTtlSeconds` and never for less than 60
seconds. A request served from the cache has `"fromCache": true` and makes no outbound request.

The cache is keyed by the RP id only, so changing an RP's `androidApps` or `iosApps` does not invalidate it. After
changing them, send `{"recheck": true}` to see a result for the new configuration.

Sending `{"recheck": true}` (the "Recheck now" action) skips the cache, but not the rate limits. The limits are
per server node, so in a cluster the installation-wide ceiling is the number of nodes times these values:

| Limit | Value |
| :--- | :--- |
| Fetches per relying party | 1 per 60 seconds |
| Files fetched, all relying parties together | 20 per 60 seconds |
| Checks running at the same time | 3 |

A request that would exceed one of them gets `429` with a `Retry-After` header and causes no fetch.
`nextRecheckAllowedAt` in the result says when the same relying party can be fetched again.

## Audit

Each executed check writes one line to the FIDO2 server log, starting with `native-readiness-audit`:

| Field | Value |
| :--- | :--- |
| `rpId` | The relying party id. |
| `check` | The check id, or `cache.hit` when a cached result was served. |
| `requestedBy` | Who asked: the caller the Config API identified from the token, sent in `X-Jans-Requested-By`, or whatever a direct call put in that header. `unknown` when absent. |
| `timestamp` | When the check ran, in UTC. |
| `result` | `pass`, `warn` or `fail`. |
| `resolvedIpClass` | A coarse label for the address that was reached, never the address itself: `public`, `blocked`, `unresolved`, `unknown`, or `none` when nothing was fetched. |

`requestedBy` is informational. The FIDO2 server does not authenticate callers, so a request that reaches it
directly can set any value.
