---
tags:
  - administration
  - auth-server
  - oauth
  - feature
  - cimd
  - client-id-metadata-document
---

# Client ID Metadata Document (CIMD)

The Client ID Metadata Document feature allows a URL to serve as a `client_id` in OAuth 2.0 flows. When a URL is used as a `client_id`, the Janssen Server fetches client metadata from that URL (an RFC 7591-compatible JSON document), automatically registering the client without requiring prior Dynamic Client Registration (DCR).

This is based on the IETF draft specification: [OAuth 2.0 Client ID Metadata Document](https://www.ietf.org/archive/id/draft-ietf-oauth-client-id-metadata-document-01.html)

## Overview

In traditional OAuth 2.0, a `client_id` is an opaque string assigned during client registration. With CIMD, the `client_id` is a URL that also serves as the location of the client's metadata document:

```
GET https://example.com/client-metadata HTTP/1.1
Accept: application/json

HTTP/1.1 200 OK
Content-Type: application/json
Cache-Control: max-age=3600

{
  "redirect_uris": ["https://app.example.com/callback"],
  "grant_types": ["authorization_code"],
  "response_types": ["code"],
  "client_name": "My Application",
  "token_endpoint_auth_method": "private_key_jwt",
  "jwks_uri": "https://example.com/jwks"
}
```

When the Janssen Server receives an authorization request with `client_id=https://example.com/client-metadata`, it:

1. Fetches the metadata document from that URL
2. Validates the metadata (redirect URIs, grant types, etc.)
3. Persists the client to the database with a TTL
4. Processes the authorization request using the fetched metadata

On subsequent requests within the TTL window, the persisted client is used directly without re-fetching.

## Enabling CIMD

CIMD is disabled by default. To enable it, add `CLIENT_ID_METADATA_DOCUMENT` to the `featureFlags` configuration property:

```json
{
  "featureFlags": ["CLIENT_ID_METADATA_DOCUMENT"]
}
```

## Client Metadata Document Format

The metadata document must be valid JSON and follow the RFC 7591 client metadata format. The document is fetched with an `Accept: application/json` header.

Example document:

```json
{
  "redirect_uris": ["https://app.example.com/callback"],
  "grant_types": ["authorization_code", "refresh_token"],
  "response_types": ["code"],
  "application_type": "web",
  "client_name": "My Application",
  "token_endpoint_auth_method": "private_key_jwt",
  "jwks_uri": "https://app.example.com/jwks",
  "scope": "openid profile email"
}
```

All standard RFC 7591 client metadata fields are supported.

### Forbidden Authentication Methods

Per [CIMD spec Section 4.1](https://www.ietf.org/archive/id/draft-ietf-oauth-client-id-metadata-document-01.html), the following `token_endpoint_auth_method` values **MUST NOT** be used in a client metadata document:

| Method | Reason |
|---|---|
| `client_secret_basic` | Requires a shared symmetric secret |
| `client_secret_post` | Requires a shared symmetric secret |
| `client_secret_jwt` | Requires a shared symmetric secret |

Since no shared secret can be pre-established between the metadata document and the authorization server, all symmetric-secret-based methods are prohibited. Requests using any of these methods will be rejected with `400 Bad Request`.

Use `private_key_jwt`, `tls_client_auth`, `self_signed_tls_client_auth`, or `none` instead.

## TTL and Caching

The Janssen Server persists fetched client metadata to the database and caches it for a configurable duration (TTL). During the TTL window, the server uses the persisted client without re-fetching the document. When the TTL expires, the document is re-fetched on the next request.

The TTL is determined as follows:

1. If the metadata URL returns a `Cache-Control: max-age=<seconds>` header, that value is used (bounded by `cimdMaxTtlMinutes`).
2. Otherwise, the default `cimdTtlMinutes` value is used.

## Configuration Properties

The following properties control CIMD behavior in the Janssen Server configuration:

| Property | Default | Description |
|---|---|---|
| `cimdSchemeAllowlist` | `["https"]` | URL schemes allowed for `client_id` URLs. Only `https` is recommended for production. |
| `cimdDomainAllowlist` | _(empty — all allowed)_ | If set, only these domains are permitted as `client_id` hosts. |
| `cimdDomainBlocklist` | _(empty)_ | Domains explicitly blocked from use as `client_id` hosts. |
| `cimdBlockPrivateIp` | `true` | When `true`, blocks `client_id` URLs that resolve to private/loopback IP addresses (SSRF protection). |
| `cimdMaxResponseSize` | `65536` | Maximum size in bytes of a fetched metadata document. Documents exceeding this limit are rejected. |
| `cimdConnectTimeoutMs` | `5000` | Connection timeout in milliseconds when fetching a metadata document. |
| `cimdReadTimeoutMs` | `10000` | Read timeout in milliseconds when fetching a metadata document. |
| `cimdTtlMinutes` | `60` | Default TTL (in minutes) for persisted client metadata when no `Cache-Control` header is present. |
| `cimdMaxTtlMinutes` | `1440` | Maximum allowed TTL (in minutes), regardless of the `Cache-Control: max-age` value in the response. |

## Security Considerations

### SSRF Protection

By default, the server blocks `client_id` URLs that resolve to private or loopback IP addresses (e.g., `127.0.0.1`, `10.0.0.0/8`, `192.168.0.0/16`). This prevents Server-Side Request Forgery (SSRF) attacks. This behavior is controlled by `cimdBlockPrivateIp`.

### Domain Controls

Use `cimdDomainAllowlist` to restrict which domains may serve client metadata. This is recommended in production environments to prevent arbitrary URLs from being used as `client_id` values.

Use `cimdDomainBlocklist` to explicitly block specific domains.

### Scheme Restriction

Only HTTPS should be used in production. The `cimdSchemeAllowlist` defaults to `["https"]` to prevent metadata from being fetched over unencrypted connections.

### Custom Script Integration

Before a fetched client is persisted, all registered Dynamic Client Registration (DCR) custom scripts are executed. This allows server operators to inspect, modify, or reject CIMD clients using the same script interface as regular DCR. A script returning `false` causes the request to be rejected with a `400 Bad Request` error.

## Using CIMD in Authorization Requests

Send the metadata URL as the `client_id` parameter in a standard authorization request:

```
GET /authorize
  ?response_type=code
  &client_id=https%3A%2F%2Fapp.example.com%2Fclient-metadata
  &redirect_uri=https%3A%2F%2Fapp.example.com%2Fcallback
  &scope=openid
  &state=abc123
```

No prior registration is required. The server resolves the metadata on the first request.

## Have questions in the meantime?

You can ask questions through [GitHub Discussions](https://github.com/JanssenProject/jans/discussions) or the [community chat on Zulip](https://chat.gluu.org/join/wnsm743ho6byd57r4he2yihn/). Any questions you have will help determine what information our documentation should cover.

## Want to contribute?

If you have content you'd like to contribute to this page, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).
