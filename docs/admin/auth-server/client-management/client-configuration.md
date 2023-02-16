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

### Available Algorithms for Encryption and Signing

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
 
### Recommendations

- RSA keys with a minimum 2048 bits if using RSA cryptography
- Elliptic Curve keys with a minimum of 160 bits if using Elliptic Curve cryptography
- Client secret should have a minimum of 128 bits if using symmetric key cryptography
- Sign with PS256 (RSASSA-PSS using SHA-256 and MGF1 with SHA-256) or ES256 (ECDSA using P-256 and SHA-256)

## Grants

### Supported Grant Types
Grant defines how a client interacts with the token endpoint to get the tokens. Janssen Server supports grant types
defined by OAuth 2.0, OAuth 2.1, and extension grants defined by other RFCs. A complete
list of supported grant types can be found in the response of the Janssen Server's well-known
[configuration endpoint](./configuration.md) given below.

```text
https://janssen.server.host/jans-auth/.well-known/openid-configuration
```

Claim `grant_types_supported` lists all the supported grant types in the response.

### Configuring Grant Type For Client

Janssen Server will allow requests from a client with grant types that the client is configured to use. Client can be
configured to use or not use certain grant types using [CLI](../../config-guide/jans-cli/README.md) or [TUI](../../config-guide/tui.md) tools.

### Recommendations For Using Grant Types and Flows

Developers should use the grant types based on the ability of the client to protect the client credentials as well as
the security profile of the deployment. If the client software is a server-side component that can securely store the
client credentials, such a client is called a `confidential` client. As opposed to that, if the application requesting
access token is entirely running on a browser, where it is not possible to store client credentials securely, such a client
is called a `public` client.

Along with the grant type to be used, developers also need to choose which flow should be used to get the required
tokens. The table below shows grant types and flows that should be used for various use-cases.

| Client Type                                                                                     | Recommended Grant Type                         | Flow                         | 
|-------------------------------------------------------------------------------------------------|------------------------------------------------|------------------------------|
| Backend App (Example: batch processes) that need to access its own resources                    | `client_credentials`                           | Client Credentials           |
| Server backend of a web-application needs access token                                          | `authorization_code`                           | Authorization Code           |
| Web-application that needs user information via id_token on browser and access token on backend | `authorization_code`                           | Hybrid Flow                  |
| Browser based single page applications or Mobile applications                                   | `authorization_code`                           | Authorization Code with PKCE |
| Browser based single page applications or Mobile applications that only intend to get id_token  | -                                              | Implicit Flow with Form Post |
| Input constrained devices (Example: TV)                                                         | `urn:ietf:params:oauth:grant-type:device_code` | Device Flow                  |
| Highly trusted applications where redirect based flows are not feasible to implement            | `password`                                     | Resource Owner Password Flow |


## Pre-authorization

If the OAuth authorization prompt should not be displayed to end users, set this field to True. This is useful for SSO
to internal clients (not a third party) where there is no need to prompt the person to approve the release of information.

## Response Types

TODO: add details to this section

!!! Contribute
If you’d like to contribute to this document, get started with the [Contribution Guide](https://docs.jans.io/head/CONTRIBUTING/#contributing-to-the-documentation)
