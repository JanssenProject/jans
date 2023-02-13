---
tags:
- administration
- client
- configuration
---

# Client Configuration

This document covers some important configuration elements of client configuration. How these elements are configured
for a client has an impact on aspects like client security.

## Redirect URI

Redirect URI is the most basic and at times the only parameter needed for registering a client. It is defined in [OAuth
framework](https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.1) and
[OpenId Connect specification](https://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication).

The client can register a list of URIs as a value for redirect URI parameter. Redirect URI can be any
[valid URI](https://www.ietf.org/rfc/rfc2396.txt).

- Redirect URI should be an **absolute URI**. For instance, URI should not have wildcard characters. As recommended
  [here](https://www.rfc-editor.org/rfc/rfc6749#section-3.1.2)
- **Redirect Regex**: Janssen Server enables clients to allow redirect URIs that conform to a regular expression(regex)
  pattern. While validating redirect URIs received in an incoming request, the Janssen Server first looks at the list
  of statically registered URI as part of `Redirect URIs` and then checks if any of the redirect URI from the request
  matches with the regex provided by the client. Great care should be exercised when using regex to ensure that it
  accurately matches with the intended patterns only. This feature can be turned on or off via
  [feature flag](../../reference/json/feature-flags/janssenauthserver-feature-flags.md)
- When using client type as `web`, the redirect URI generally takes the form of `<schema>://<host-name>:<port>/<path>`.
  It must use `https` schema. The exception to **schema rule** is made when  
  `localhost` or loopback ip `127.0.0.1` is used as the hostname. 
- Prefer to use the loopback IP instead of `localhost` as hostname to facilitate **local testing** as
  recommended in [OAuth 2.0 native app specification section 8.3](https://www.rfc-editor.org/rfc/rfc8252#section-8.3)
- Janssen Server supports all [methods of redirection](https://datatracker.ietf.org/doc/html/rfc8252#section-7) used by
  **native apps**. Use of Private-Use URI (or custom URL) is
  supported by allowing redirect URI to take the form of reverse DNS name, for example, ` com.example.app`. URLs for
  loopback interface redirection are also supported.
- When the client registers **multiple redirect URIs** (Janssen Server accepts a list of URIs separated by space), be aware
  that Janssen Server will use these, one by one for validation purposes and the validation stops at the first match.
- If there are multiple registered redirect_uris, and the client is using `pairwise` subject
  identifiers, the Client MUST also register a **sector_identifier_uri**. This is required to keep the pairwise subject
  identifiers consistent across various domains under the same administrative control. Refer to [pairwise algorithm
  section](https://openid.net/specs/openid-connect-core-1_0.html#PairwiseAlg) of OpenId Connect specification for more
  details.

## Cryptography

Janssen Server allows clients to configure static set of keys using `JWKS` or specify a URI as `JWKS URI` where client
is exposing its key set. For client who can host keys and expose a URI, it is **recommended** to use `JWKS URI` instead of
static `JWKS` key set. Using `JWKS URI` enables client to rotate its cryptographic keys without having to change the
client configuration on Janssen Server.

### Selecting Algorithms for Encryption and Signing

The client can select algorithms for cryptographic and encryption during client configuration. Janssen 
Server supports a list of algorithms as listed in response of Janssen Server's well-known
[configuration endpoint](./configuration.md) given below.

```text
https://janssen.server.host/jans-auth/.well-known/openid-configuration
```

Claims that list supported algorithms:

- id_token_encryption_alg_values_supported
- id_token_signing_alg_values_supported
- userinfo_encryption_enc_values_supported
- userinfo_signing_alg_values_supported
- userinfo_encryption_alg_values_supported
- access_token_signing_alg_values_supported
- request_object_signing_alg_values_supported
- request_object_encryption_alg_values_supported
- request_object_encryption_alg_values_supported
 
## Grants

## Pre-authorization

If the OAuth authorization prompt should not be displayed to end users, set this field to True. This is useful for SSO
to internal clients (not a third party) where there is no need to prompt the person to approve the release of information.

## Response Types



Please use the left navigation menu to browse the content of this section while we are still working on developing content for `Overview` page.

!!! Contribute
If you’d like to contribute to this document, get started with the [Contribution Guide](https://docs.jans.io/head/CONTRIBUTING/#contributing-to-the-documentation)
