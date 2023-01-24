---
tags:
  - administration
  - auth-server
  - token
---

## OAuth Access Tokens

### Background

When a software client calls an OAuth protected API, an access token is presented
in the HTTP Authorization header of the request. Following is an example of a
bearer token:

```
Authorization: Bearer 8pkjyj0gxhwh6ux2
```

OAuth access tokens come in two main types: bearer tokens and MAC tokens.

Bearer tokens are the most commonly used type of access token. They are simply
a string that is included in the Authorization header of an HTTP request. The
client presents the token to the resource server, which can trust the token,
by validating it was issued by a trusted authorization server. Bearer tokens
can be sent either by value or reference. A value token is a JWT, signed
by the authorization server. By calling Auth Server's [introspection endpoint],
the client can trade the reference token for the plain JSON object.

MAC tokens, are cryptographically signed tokens that include a nonce, timestamp,
and other information to prevent replay attacks. They are typically used in
situations where the client and the resource server are not able to securely
share a secret. They are not commonly used and Janssen Auth Server does
not support MAC tokens.

There are several security considerations that both mobile developers and API
developers should be aware of when working with OAuth access tokens. For mobile
developers, it is important to ensure that the access token is stored securely
on the device, and that it is not exposed to other apps or processes. API
developers should ensure that their API is only accessible to clients that have
a valid access token, and that the API properly validates the token before
allowing access to the protected resource.

Scopes are used in OAuth access tokens to limit the extent of access of a token,
i.e. the specific resources and actions that the token is valid for. Scopes are
typically defined by the API developer, and the client can request a specific
scope when requesting an access token from the authorization server. This allows
the API developer to fine-tune the level of access that different clients have
to the API.

Other security considerations for OAuth access tokens include the need to
protect against replay attacks by including a nonce and timestamp in the token
and using TLS for all communication between the client, the authorization
server and the resource server.

It is also important to rotate the access token. This can be done by using
short lived tokens and refresh tokens which will allow the client to obtain a
new access token without re-prompting the user for their credentials. By
default, Jans Auth Server access tokens expire after 5 minutes.

### Access Token Schema

| claim         | Description           |
| ------------- | ----------------------|
| `active`      | `true` or `false`.    |  
| `iss`         | The URI of the issuer authorization server |
| `aud`         | The audience, used by the client to verify it is the correct recipient. During registration, the client can specify `additional_audience` values |
| `iat`         | When the client was issued, in seconds, e.g. *1514797822* |
| `exp`         | When the token expires, in seconds, e.g. *1514797942* |
| `scope`       | A space delimited list of scopes |
| `client_id`   | Recipient of the token |
| `nbf`         | Not before, which insures the token is only valid within a certain time window |
| `cnf`         | Confirmation, used for TLS client certificate bound tokens |

It is possible to add additional claims to an access token via the
Auth Server interception scripts. The preferred script is the
[update token script](../../developer/scripts/update-token.md). You can
also use the [introspection script](../../developer/scripts/introspection.md).

### Access Token Crypto (JWT)

JWT access tokens are signed by Jans Auth Server using
algorithms specified in the `access_token_signing_alg_values_supported`
claim of the OpenID configuration endpoint response. Access tokens are
signed with the standard OpenID signing key.

Jans Auth Server supports TLS client certificate bound access tokens. After
a successful mutual TLS client authentication, Jans Auth Server encodes the
client certificate thumbprint (hash) in `x5t#S256` confirmation method of the JWT or introspection
JSON. Assuming the client uses the same certificate to establish a mutual TLS
session with the API, the thumbprint in the access token can verify that this
is the same client that obtained the access token. This feature is typically
used in high security environments, as the operational cost of mutual TLS is
material.

Decoded JWT example
```json
{
  "iss": "https://server.example.com",
  "sub": "ty.webb@example.com",
  "exp": 1493726400,
  "nbf": 1493722800,
  "cnf":{
    "x5t#S256": "bwcK0esc3ACC3DB2Y5_lESsXE8o9ltc05O89jdN-dg2"
  }
}
```

Sample introspection response
```json
HTTP/1.1 200 OK
Content-Type: application/json

{
  "active": true,
  "iss": "https://server.example.com",
  "sub": "ty.webb@example.com",
  "exp": 1493726400,
  "nbf": 1493722800,
  "cnf":{
    "x5t#S256": "bwcK0esc3ACC3DB2Y5_lESsXE8o9ltc05O89jdN-dg2"
  }
}
```

### Server and Client configurations

Access token lifetime is configurable at the server level via the
`accessTokenLifetime` property. However, a client can override this value
during client registration with the `access_token_lifetime` request
parameter.

### Revoke Access Token

Access token can be revoked via [Revoke Endpoint](../endpoints/token-revocation.md)
