---
tags:
  - administration
  - auth-server
  - oauth
  - feature
  - mtls
  - spiffe
---

# SPIFFE-Based Client Authentication

Janssen supports SPIFFE-based OAuth client authentication, letting SPIFFE-enabled workloads
authenticate using their SPIFFE Verifiable Identity Documents (SVIDs) instead of a client secret.

This is based on the IETF draft specification:
[OAuth SPIFFE Client Authentication](https://datatracker.ietf.org/doc/draft-ietf-oauth-spiffe-client-auth/).
It is an active Internet-Draft, not yet an RFC, so this feature is disabled by default and its
behavior may change as the draft evolves.

Two credential types are supported:

- **X.509-SVID**: mutual TLS (building on [mTLS](./mtls.md) / RFC 8705), where the client's SPIFFE
  ID is carried in the certificate's URI Subject Alternative Name.
- **JWT-SVID**: a signed JWT presented as a `client_assertion` with
  `client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-spiffe`.

WIT-SVID (WIMSE Workload Identity Token) is defined by the draft but not supported yet, since it
depends on a separate, less mature draft (OAuth 2.0 Attestation-Based Client Authentication).

## Enabling the feature

Add `spiffe_client_auth` to `featureFlags` in the auth-server dynamic configuration.

## Trust configuration

Trust anchors are configured by the administrator, out-of-band, per SPIFFE trust domain - a
client-supplied `spiffe_bundle_endpoint` is never used to source trust material, since trusting it
would let a client vouch for itself. Configure `spiffeTrustDomains` in the dynamic configuration:

```json
{
  "spiffeTrustDomains": [
    {
      "trustDomain": "example.org",
      "bundleEndpointUrl": "https://spire-bundle.example.org/bundle.json",
      "bundleCacheLifetimeInMinutes": 60
    }
  ]
}
```

The bundle endpoint is expected to serve a JWKS (RFC 7517) where each key's `use` field indicates
its purpose: `x509-svid` (a CA certificate, in `x5c`, used as a trust anchor for X.509-SVID path
validation) or `jwt-svid` (a public key used to verify JWT-SVID signatures). The `bundleEndpointUrl`
must be an `https` URL; fetches to any other scheme are rejected.

The following properties control how the bundle endpoint is fetched:

| Property | Default | Description |
|---|---|---|
| `spiffeBundleMaxResponseSize` | `1048576` | Maximum size in bytes of a fetched bundle document. Responses exceeding this limit are rejected. |
| `spiffeBundleConnectTimeoutMs` | `5000` | Connection timeout in milliseconds when fetching a bundle. |
| `spiffeBundleReadTimeoutMs` | `10000` | Read timeout in milliseconds when fetching a bundle. |

> **Stale-bundle behavior on fetch failure:** if a refresh of a trust domain's bundle fails (network
> error, non-200 response, oversized or malformed body), the authorization server keeps serving the
> last successfully fetched bundle rather than failing closed, and retries the endpoint again after
> 30 seconds. This means previously-fetched trust material - including a CA certificate or JWT-SVID
> key that has since been revoked or rotated at the source - can remain in effect for as long as the
> bundle endpoint keeps failing, with no maximum staleness limit. If a bundle has never been
> successfully fetched, there is no stale copy to fall back on and validation fails closed (empty
> trust anchors / no JWKS) until a fetch succeeds.

> **Limitation:** the authorization server only validates the certificate that the reverse proxy
> forwards to it (see [mTLS](./mtls.md) for the supported forwarding headers), and most proxy
> configurations forward the leaf certificate only, not the full chain. X.509-SVID validation is
> therefore single-hop: the leaf certificate must be directly signed by one of the configured trust
> anchors. If your PKI uses an intermediate CA, either configure that intermediate certificate as
> the trust anchor, or configure the reverse proxy to forward the full certificate chain.

## Client metadata

Two new client metadata fields, from the draft:

- `spiffe_id` (required to use this feature for a client): the client's SPIFFE ID, e.g.
  `spiffe://example.org/my-workload`. May end with `/*` to match any concrete SVID under that
  path prefix (e.g. `spiffe://example.org/client/*` matches `spiffe://example.org/client/123`),
  useful when many workload instances share one client registration.
- `spiffe_bundle_endpoint` (informational only, not trusted as a source of trust anchors): the
  client's own declared SPIFFE Bundle Endpoint.

## X.509-SVID (mTLS) authentication

Register a client with `token_endpoint_auth_method=tls_client_auth` and a `spiffe_id`:

```bash
curl --insecure --location 'https://<YOUR_DOMAIN>/jans-auth/restv1/register' \
--header 'Content-Type: application/json' \
--data '{
  "client_name": "SPIFFE mTLS Client",
  "token_endpoint_auth_method": "tls_client_auth",
  "spiffe_id": "spiffe://example.org/my-workload",
  "grant_types": ["client_credentials"]
}'
```

At the token endpoint, present the client's X.509-SVID as the mTLS client certificate, forwarded
to Janssen via one of the headers described in [mTLS](./mtls.md). The authorization server:

1. Path-validates the certificate against the trust anchors configured for the SPIFFE ID's trust
   domain.
2. Verifies the certificate contains exactly one URI SAN, and that it is a valid SPIFFE ID.
3. Verifies the certificate is a leaf certificate (Basic Constraints `CA=FALSE`).
4. Verifies the certificate's Key Usage has the `digitalSignature` bit set.
5. Matches the presented SPIFFE ID against the client's registered `spiffe_id` (wildcard-aware).

Unlike `tls_client_auth`'s standard Subject DN check, X.509-SVIDs conventionally carry an
empty/absent Subject DN - identity lives solely in the URI SAN - so this validation path bypasses
the Subject DN check entirely for clients with a registered `spiffe_id`.

## JWT-SVID authentication

Register a client with `token_endpoint_auth_method=spiffe_jwt` and a `spiffe_id`:

```bash
curl --location 'https://<YOUR_DOMAIN>/jans-auth/restv1/register' \
--header 'Content-Type: application/json' \
--data '{
  "client_name": "SPIFFE JWT-SVID Client",
  "token_endpoint_auth_method": "spiffe_jwt",
  "spiffe_id": "spiffe://example.org/my-workload",
  "grant_types": ["client_credentials"]
}'
```

If `<YOUR_DOMAIN>` presents a certificate issued by a private/internal CA, add `--cacert
/path/to/ca.pem` rather than disabling certificate verification.

At the token endpoint, present:

- `client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-spiffe`
- `client_assertion=<the JWT-SVID>`
- `client_id=<the client's client_id>`

The JWT-SVID must have: a `sub` claim containing the presenting workload's SPIFFE ID; an `aud`
claim containing *only* the authorization server's issuer identifier as its sole value; and a
valid, unexpired `exp`. Its signature is verified against the `jwt-svid`-tagged keys from the
SPIFFE trust bundle for the SPIFFE ID's trust domain - not the client's own registered JWKS.

## Interaction with Client ID Metadata Documents (CIMD)

Per the draft, when the presenting `client_id` is a [CIMD](./cimd.md) URL, the SPIFFE ID from the
presented credential is matched against the `spiffe_id` declared in that metadata document -
`spiffe_id`/`spiffe_bundle_endpoint` are supported CIMD metadata fields for exactly this reason.
