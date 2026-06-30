# Request Objects

Request Objects let OpenID Connect authorization requests be passed as a signed and optionally encrypted JWT instead of only as URL query parameters. Protocol details are defined in [OpenID Connect Core — Passing Request Parameters as JWTs](https://openid.net/specs/openid-connect-core-1_0.html#JWTRequests) and [RFC 9101 (JAR)](https://www.rfc-editor.org/rfc/rfc9101.html).

Janssen Server supports Request Objects at the [authorization endpoint](https://docs.jans.io/nightly/janssen-server/auth-server/endpoints/authorization/index.md) using the `request` and `request_uri` parameters. The same mechanism is also used with [PAR](https://docs.jans.io/nightly/janssen-server/auth-server/oauth-features/par/index.md) and CIBA flows.

## Using JWTs for Passing Request Parameters

To enable authentication requests to be signed and optionally encrypted, the authorization request uses the parameters below. These requests are passed as Request Objects in two ways: **by value** and **by reference**.

| Parameter     | Description                                                                                                                                                                                                              | Required |
| ------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | -------- |
| `request`     | Enables OpenID Connect requests to be passed in a single, self-contained parameter and to be optionally signed and/or encrypted. The parameter value is a Request Object: a JWT whose claims are the request parameters. | Optional |
| `request_uri` | Enables OpenID Connect requests to be passed by reference, rather than by value. The `request_uri` value is a URL using the `https` scheme that references a resource containing a Request Object JWT.                   | Optional |

Janssen advertises support for these parameters in OpenID Provider Metadata (`request_parameter_supported`, `request_uri_parameter_supported`). If a parameter is disabled in server configuration, Janssen returns `request_not_supported` or `request_uri_not_supported` respectively.

## Request Object by Value

The `request` authorization request parameter lets the client send OpenID Connect request parameters in one JWT (the Request Object). The `request` parameter is optional.

When `request` is used, claim values inside the JWT **supersede** the same parameters passed using OAuth 2.0 request syntax. Parameters may still be passed in the URL alongside the Request Object. A common pattern is to use a cached, pre-signed (and possibly pre-encrypted) Request Object for fixed parameters, while per-request values such as `state` and `nonce` are passed as OAuth 2.0 parameters.

For a valid OAuth 2.0 authorization request, `response_type` and `client_id` **must** still be included using OAuth 2.0 request syntax because they are required by OAuth 2.0. Their values **must match** the values in the Request Object.

The `request` and `request_uri` parameters must not be included inside the Request Object JWT.

The following is a non-normative example of Request Object claims before base64url encoding and signing:

```
{
  "iss": "s6BhdRkqt3",
  "aud": "https://janssen.example.com",
  "response_type": "code id_token",
  "client_id": "s6BhdRkqt3",
  "redirect_uri": "https://client.example.org/cb",
  "scope": "openid",
  "state": "af0ifjsldkj",
  "nonce": "n-0S6_WzA2Mj",
  "max_age": 86400,
  "claims": {
    "userinfo": {
      "given_name": {"essential": true},
      "nickname": null,
      "email": {"essential": true},
      "email_verified": {"essential": true},
      "picture": null
    },
    "id_token": {
      "gender": null,
      "birthdate": {"essential": true},
      "acr": {"values": ["urn:mace:incommon:iap:silver"]}
    }
  }
}
```

### Using the `request` Request Parameter

The client sends the authorization request to the Janssen authorization endpoint:

```
https://<hostname>/jans-auth/restv1/authorize
```

Non-normative example (line breaks in values are for display only):

```
https://janssen.example.com/jans-auth/restv1/authorize?
  response_type=code%20id_token
  &client_id=s6BhdRkqt3
  &redirect_uri=https%3A%2F%2Fclient.example.org%2Fcb
  &scope=openid
  &state=af0ifjsldkj
  &nonce=n-0S6_WzA2Mj
  &request=eyJhbGciOiJSUzI1NiIsImtpZCI6ImsyYmRjIn0...
```

When the Request Object includes OIDC scopes, Janssen still requires the `scope` parameter in the authorization URL and it must include the `openid` value (per OpenID Connect rules enforced in `AuthzRequestService`).

## Request Object by Reference

The `request_uri` authorization request parameter passes the Request Object by reference. Janssen fetches the JWT from the HTTPS URL and processes it the same way as the `request` parameter, except the JWT is retrieved from the referenced resource.

The `request_uri_parameter_supported` discovery value indicates whether the OP supports this parameter. If Janssen has [requestUriParameterSupported](https://docs.jans.io/nightly/janssen-server/reference/json/properties/janssenauthserver-properties/#requesturiparametersupported) set to `false` and the RP sends `request_uri`, Janssen returns `request_uri_not_supported`.

When `request_uri` is used, claims in the fetched JWT supersede URL parameters. As with `request`, URL parameters may still be supplied for values that change per request (for example `state` and `nonce`).

For a valid OAuth 2.0 authorization request, `response_type` and `client_id` must be included in the URL and must match the Request Object.

### URL Referencing the Request Object

The client stores the Request Object at a URL Janssen can access. That URL is the Request URI (`request_uri`).

If the Request Object contains sensitive claims, it must not be exposed to parties other than the authorization server. The `request_uri` should have sufficient entropy for its lifetime. It is recommended to remove or expire the resource after use unless access controls protect it.

When [requestUriHashVerificationEnabled](https://docs.jans.io/nightly/janssen-server/reference/json/properties/janssenauthserver-properties/#requesturihashverificationenabled) is enabled, the URL may include a fragment with the base64url-encoded SHA-256 hash of the JWT content (per RFC 9101). Janssen verifies the hash after fetching the resource.

Non-normative example Request URI:

```
https://client.example.org/request.jwt#GkurKxf5T0Y-mnPFCHqWOMiZi4VS138cQO_V7PZHAdM
```

### Using the `request_uri` Request Parameter

Non-normative authorization request example:

```
https://janssen.example.com/jans-auth/restv1/authorize?
  response_type=code%20id_token
  &client_id=s6BhdRkqt3
  &request_uri=https%3A%2F%2Fclient.example.org%2Frequest.jwt
  %23GkurKxf5T0Y-mnPFCHqWOMiZi4VS138cQO_V7PZHAdM
  &state=af0ifjsldkj
  &nonce=n-0S6_WzA2Mj
  &scope=openid
```

Janssen additionally validates `request_uri` against the client's pre-registered `request_uris` (when configured) and against [requestUriBlockList](https://docs.jans.io/nightly/janssen-server/reference/json/properties/janssenauthserver-properties/#requesturiblocklist) (for example to block `localhost`).

## Signing and Encryption

If both signing and encryption are used, the Request Object is **signed first, then encrypted**, producing a nested JWT (JWE wrapping a nested JWS).

Janssen behavior:

- **JWS**: Signature is verified using the client's `request_object_signing_alg`, client JWKS, or client secret (for HMAC).
- **JWE**: The outer token is decrypted using the OP private key (RSA) or client secret (symmetric). The payload must contain a **nested signed JWT**; plain JSON inside JWE is rejected.
- When [requireRequestObjectEncryption](https://docs.jans.io/nightly/janssen-server/reference/json/properties/janssenauthserver-properties/#requirerequestobjectencryption) is `true`, the Request Object must be a five-segment JWE.
- When [forceSignedRequestObject](https://docs.jans.io/nightly/janssen-server/reference/json/properties/janssenauthserver-properties/#forcesignedrequestobject) is `true`, every authorization request must include `request` or `request_uri`, and unsigned Request Objects (including `none` or unrecognized algorithms) are rejected.

Supported algorithms are published in discovery as `request_object_signing_alg_values_supported`, `request_object_encryption_alg_values_supported`, and `request_object_encryption_enc_values_supported`.

## Error Response

Request Object-related errors are returned to the client's `redirect_uri` (or as JSON for direct API calls). Common errors:

| Error                       | Description                                                                                                                                                                                                                        |
| --------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `invalid_request_object`    | The `request` parameter contains an invalid Request Object (invalid JWT, signature, claims, or policy violation).                                                                                                                  |
| `invalid_request_uri`       | The `request_uri` returns an error, contains invalid data, is blocked, or is not allowed for the client.                                                                                                                           |
| `request_not_supported`     | The OP does not support the `request` parameter ([requestParameterSupported](https://docs.jans.io/nightly/janssen-server/reference/json/properties/janssenauthserver-properties/#requestparametersupported) is `false`).           |
| `request_uri_not_supported` | The OP does not support the `request_uri` parameter ([requestUriParameterSupported](https://docs.jans.io/nightly/janssen-server/reference/json/properties/janssenauthserver-properties/#requesturiparametersupported) is `false`). |
| `invalid_scope`             | For example, when OIDC scopes are requested from the JWT but `openid` is missing from the URL `scope` parameter.                                                                                                                   |

Other standard authorization errors (`interaction_required`, `login_required`, `consent_required`, etc.) apply as documented on the [authorization endpoint](https://docs.jans.io/nightly/janssen-server/auth-server/endpoints/authorization/index.md).

## Configure Request Objects in Janssen Server

### Authorization Server properties

Configure global Request Object settings using the [Janssen TUI](https://docs.jans.io/nightly/janssen-server/config-guide/config-tools/jans-tui/index.md) (`Auth Server` -> `Properties`) or Config API.

| Property                                                                                                                                                                                   | Description                                                      |
| ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ---------------------------------------------------------------- |
| [requestParameterSupported](https://docs.jans.io/nightly/janssen-server/reference/json/properties/janssenauthserver-properties/#requestparametersupported)                                 | Enable or disable the `request` parameter                        |
| [requestUriParameterSupported](https://docs.jans.io/nightly/janssen-server/reference/json/properties/janssenauthserver-properties/#requesturiparametersupported)                           | Enable or disable the `request_uri` parameter                    |
| [requestObjectSigningAlgValuesSupported](https://docs.jans.io/nightly/janssen-server/reference/json/properties/janssenauthserver-properties/#requestobjectsigningalgvaluessupported)       | JWS algorithms accepted for Request Objects                      |
| [requestObjectEncryptionAlgValuesSupported](https://docs.jans.io/nightly/janssen-server/reference/json/properties/janssenauthserver-properties/#requestobjectencryptionalgvaluessupported) | JWE `alg` values accepted for Request Objects                    |
| [requestObjectEncryptionEncValuesSupported](https://docs.jans.io/nightly/janssen-server/reference/json/properties/janssenauthserver-properties/#requestobjectencryptionencvaluessupported) | JWE `enc` values accepted for Request Objects                    |
| [forceSignedRequestObject](https://docs.jans.io/nightly/janssen-server/reference/json/properties/janssenauthserver-properties/#forcesignedrequestobject)                                   | Require a signed Request Object on every authorization request   |
| [requireRequestObjectEncryption](https://docs.jans.io/nightly/janssen-server/reference/json/properties/janssenauthserver-properties/#requirerequestobjectencryption)                       | Require Request Objects to be encrypted (JWE)                    |
| [requireRequestUriRegistration](https://docs.jans.io/nightly/janssen-server/reference/json/properties/janssenauthserver-properties/#requirerequesturiregistration)                         | Require `request_uri` values to be pre-registered on the client  |
| [requestUriBlockList](https://docs.jans.io/nightly/janssen-server/reference/json/properties/janssenauthserver-properties/#requesturiblocklist)                                             | Block patterns for disallowed `request_uri` hosts/paths          |
| [requestUriHashVerificationEnabled](https://docs.jans.io/nightly/janssen-server/reference/json/properties/janssenauthserver-properties/#requesturihashverificationenabled)                 | Verify SHA-256 hash in `request_uri` fragment                    |
| [staticDecryptionKid](https://docs.jans.io/nightly/janssen-server/reference/json/properties/janssenauthserver-properties/#staticdecryptionkid)                                             | Key ID for decrypting JWE when `kid` is absent in the JWE header |

**Recommendations:**

- Enable `forceSignedRequestObject` for high-assurance deployments.
- Prefer `PS256` or `ES256` over `none` for signing.
- Use `requestUriBlockList` to prevent SSRF-style fetches to internal endpoints.
- Combine with [fapiCompatibility](https://docs.jans.io/nightly/janssen-server/reference/json/properties/janssenauthserver-properties/#fapicompatibility) and [requirePar](https://docs.jans.io/nightly/janssen-server/reference/json/properties/janssenauthserver-properties/#requirepar) for FAPI-style deployments.

When [fapiCompatibility](https://docs.jans.io/nightly/janssen-server/reference/json/properties/janssenauthserver-properties/#fapicompatibility) is enabled, Janssen enforces additional checks on Request Objects (for example `aud`, `exp`, `nbf`, `nonce`, `scope`, and `redirect_uri` in the JWT, and restrictions on `RS256` / `none` for JWS).

### Client metadata

Configure per-client Request Object behavior via TUI (`Auth Server` -> `Clients`), [Dynamic Client Registration](https://docs.jans.io/nightly/janssen-server/auth-server/endpoints/client-registration/index.md), or client management APIs.

| Client metadata                 | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| ------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `request_object_signing_alg`    | JWS algorithm that must be used to sign Request Objects sent to the OP. Request Objects signed with another algorithm are rejected. Used for both `request` and `request_uri`. The value `none` may be used unless server policy forbids it.                                                                                                                                                                                                                                                                               |
| `request_object_encryption_alg` | JWE `alg` the RP may use to encrypt Request Objects. Include this when symmetric encryption is used so the OP can derive the key from `client_secret`. If both signing and encryption are used, the object is signed then encrypted (nested JWT).                                                                                                                                                                                                                                                                          |
| `request_object_encryption_enc` | JWE `enc` algorithm. If `request_object_encryption_alg` is set, `request_object_encryption_enc` should also be provided (default in spec: `A128CBC-HS256`).                                                                                                                                                                                                                                                                                                                                                                |
| `request_uris`                  | Pre-registered `request_uri` values. When the client defines this list, Janssen only accepts `request_uri` values that match. When discovery claim `require_request_uri_registration` is `true` (controlled by [requireRequestUriRegistration](https://docs.jans.io/nightly/janssen-server/reference/json/properties/janssenauthserver-properties/#requirerequesturiregistration)), pre-registration is required. If request file contents can change, include the base64url SHA-256 hash of the file as the URI fragment. |

Example client registration fragment:

```
{
  "request_object_signing_alg": "ES256",
  "request_object_encryption_alg": "RSA-OAEP",
  "request_object_encryption_enc": "A256GCM",
  "request_uris": [
    "https://client.example.com/oidc/request.jwt"
  ]
}
```

## OpenID Provider Metadata

Confirm runtime support from:

```
https://<hostname>/jans-auth/.well-known/openid-configuration
```

Relevant claims:

- `request_parameter_supported`
- `request_uri_parameter_supported`
- `require_request_uri_registration`
- `request_object_signing_alg_values_supported`
- `request_object_encryption_alg_values_supported`
- `request_object_encryption_enc_values_supported`

Janssen JWKS (for clients encrypting Request Objects to the OP):

```
https://<hostname>/jans-auth/restv1/jwks
```

## Related documentation

- [Authorization Endpoint](https://docs.jans.io/nightly/janssen-server/auth-server/endpoints/authorization/index.md)
- [PAR (Pushed Authorization Requests)](https://docs.jans.io/nightly/janssen-server/auth-server/oauth-features/par/index.md)
- [Client Configuration](https://docs.jans.io/nightly/janssen-server/auth-server/client-management/client-configuration/index.md)
- [Janssen Auth Server Configuration Properties](https://docs.jans.io/nightly/janssen-server/reference/json/properties/janssenauthserver-properties/index.md)
- [Security best practices](https://docs.jans.io/nightly/janssen-server/planning/security-best-practices/index.md)
