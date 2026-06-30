# Identity Assertion Authorization Grant (Cross-App Access)

## Overview

The Janssen Authorization Server implements the [Identity Assertion Authorization Grant (draft-ietf-oauth-identity-assertion-authz-grant-04)](https://www.ietf.org/archive/id/draft-ietf-oauth-identity-assertion-authz-grant-04.html), also known as **Cross-App Access (XAA)**.

This mechanism enables a client already authenticated with one Identity Provider (IdP) to obtain access tokens from a *different* Resource Authorization Server (Resource AS) that trusts the same IdP, **without launching a new browser-based SSO flow**. It combines:

- [OAuth 2.0 Token Exchange (RFC 8693)](https://www.rfc-editor.org/rfc/rfc8693) to issue an Identity Assertion JWT (ID-JAG), and
- [JWT Bearer Token Grant (RFC 7523)](https://www.rfc-editor.org/rfc/rfc7523) to exchange the ID-JAG for an access token.

## Protocol Flow

```
Client                     IdP Janssen-AS              Resource Janssen-AS
  |                             |                            |
  |-- 1. OIDC login ----------->|                            |
  |<-- ID token / RT -----------|                            |
  |                             |                            |
  |-- 2. Token Exchange ------->|                            |
  |   grant_type=token-exchange |                            |
  |   requested_token_type=     |                            |
  |     oauth:token-type:id-jag |                            |
  |   subject_token=<ID token>  |                            |
  |   audience=<Resource AS>    |                            |
  |<-- 3. ID-JAG JWT -----------|                            |
  |   typ: "oauth-id-jag+jwt"   |                            |
  |                             |                            |
  |-- 4. JWT Bearer grant ------------------------------>   |
  |   grant_type=jwt-bearer                                  |
  |   assertion=<ID-JAG>                                     |
  |<-- access_token -----------------------------------------|
```

Janssen AS can play **two roles** simultaneously:

| Role            | What Janssen AS does                                        |
| --------------- | ----------------------------------------------------------- |
| **IdP AS**      | Receives token exchange requests and issues ID-JAG JWTs     |
| **Resource AS** | Validates ID-JAG bearer assertions and issues access tokens |

## Enabling the Feature

The entire feature is controlled by the `identity_assertion_authz_grant` feature flag.

Add it to `featureFlags` in the Janssen authorization server configuration:

```
{
  "featureFlags": ["identity_assertion_authz_grant"]
}
```

When the flag is present, both roles are activated and the discovery document is updated automatically.

## Configuration Fields

| Field                    | Type                               | Default | Description                                                                                                           |
| ------------------------ | ---------------------------------- | ------- | --------------------------------------------------------------------------------------------------------------------- |
| `idJagTrustedIdpIssuers` | `Map<String, TrustedIssuerConfig>` | `{}`    | **(Resource AS role)** Trusted IdP issuers whose ID-JAGs this AS will accept. Empty map means any issuer is accepted. |
| `idJagLifetime`          | `int` (seconds)                    | `300`   | **(IdP role)** Lifetime for issued ID-JAGs.                                                                           |
| `idJagIssueRefreshToken` | `Boolean`                          | `false` | **(Resource AS role)** Whether to issue refresh tokens after accepting an ID-JAG. The spec recommends `false`.        |

### Restricting trusted IdP issuers

To restrict which IdPs can issue ID-JAGs accepted by this AS (Resource AS role):

```
{
  "idJagTrustedIdpIssuers": {
    "https://idp.example.com": {
      "automaticallyGrantedScopes": ["openid", "profile"]
    }
  }
}
```

## Step 2: Token Exchange Request (IdP Role)

The client sends a token exchange request to the IdP's `/token` endpoint:

```
POST /jans-auth/restv1/token HTTP/1.1
Content-Type: application/x-www-form-urlencoded
Authorization: Basic <client_credentials>

grant_type=urn:ietf:params:oauth:grant-type:token-exchange
&requested_token_type=urn:ietf:params:oauth:token-type:id-jag
&subject_token=<ID_TOKEN>
&subject_token_type=urn:ietf:params:oauth:token-type:id_token
&audience=https://resource-as.example.com
&scope=openid profile
```

**Supported `subject_token_type` values:**

| Value                                            | Description                                |
| ------------------------------------------------ | ------------------------------------------ |
| `urn:ietf:params:oauth:token-type:id_token`      | OIDC ID token (most common)                |
| `urn:ietf:params:oauth:token-type:saml2`         | SAML 2.0 assertion                         |
| `urn:ietf:params:oauth:token-type:refresh_token` | Refresh token (for SAML-first deployments) |

**Response:**

```
{
  "access_token": "<signed_ID-JAG_JWT>",
  "issued_token_type": "urn:ietf:params:oauth:token-type:id-jag",
  "token_type": "N_A"
}
```

## ID-JAG JWT Structure

The issued ID-JAG is a signed JWT with:

**Header:**

```
{
  "typ": "oauth-id-jag+jwt",
  "alg": "RS256",
  "kid": "<key_id>"
}
```

**Claims:**

```
{
  "iss": "https://idp.example.com",
  "sub": "alice",
  "aud": "https://resource-as.example.com",
  "client_id": "my-app",
  "jti": "550e8400-e29b-41d4-a716-446655440000",
  "exp": 1716648300,
  "iat": 1716648000,
  "scope": "openid profile",
  "email": "alice@example.com"
}
```

Optional claims propagated from the subject token: `email`, `auth_time`, `acr`, `amr`.

## Step 4: JWT Bearer Request (Resource AS Role)

The client presents the ID-JAG to the Resource AS `/token` endpoint:

```
POST /jans-auth/restv1/token HTTP/1.1
Content-Type: application/x-www-form-urlencoded
Authorization: Basic <client_credentials>

grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer
&assertion=<ID-JAG_JWT>
&scope=read write
```

**Validation performed by Resource AS:**

1. JWT `typ` header = `"oauth-id-jag+jwt"`
1. Signature verification
1. `aud` matches this AS's issuer
1. `client_id` matches the authenticated client
1. `exp` has not passed
1. `iss` is in `idJagTrustedIdpIssuers` (if configured)

**Subject resolution order:** `sub` → `email` → `aud_sub` → empty user

**Response:**

```
{
  "access_token": "<access_token>",
  "token_type": "Bearer",
  "expires_in": 300,
  "issued_token_type": "urn:ietf:params:oauth:token-type:access_token"
}
```

Note: no `refresh_token` is issued by default (controlled by `idJagIssueRefreshToken`).

## Discovery Metadata

When the feature flag is enabled, the following fields appear in the [OpenID Configuration](https://docs.jans.io/nightly/janssen-server/auth-server/endpoints/configuration/index.md) (`/.well-known/openid-configuration`):

```
{
  "identity_chaining_requested_token_types_supported": [
    "urn:ietf:params:oauth:token-type:id-jag"
  ],
  "authorization_grant_profiles_supported": [
    "urn:ietf:params:oauth:grant-profile:id-jag"
  ]
}
```

## Security Considerations

- **Confidential clients only.** Public clients should use the authorization code flow. The IdP AS should reject token exchange requests from non-confidential clients.
- **No refresh tokens.** The Resource AS should not issue refresh tokens for ID-JAG exchanges. When the access token expires, the client should re-present the ID-JAG or obtain a new one.
- **Audience binding.** The ID-JAG `aud` must exactly match the Resource AS issuer URI. This prevents replay across different Resource Servers.
- **Client ID binding.** The `client_id` in the ID-JAG must match the authenticated client at the Resource AS. This prevents impersonation.
- **Trusted issuer list.** Configure `idJagTrustedIdpIssuers` to restrict which IdPs can issue ID-JAGs your Resource AS accepts.
- **Short lifetime.** Keep `idJagLifetime` short (default 5 minutes). ID-JAGs are single-use delegation tokens.

## Related Documentation

- [Token endpoint](https://docs.jans.io/nightly/janssen-server/auth-server/endpoints/token/index.md)
- [Feature flags](https://docs.jans.io/nightly/janssen-server/auth-server/endpoints/oauth-features/feature-flags.md)
- [OAuth transaction tokens](https://docs.jans.io/nightly/janssen-server/auth-server/tokens/oauth-tx-tokens/index.md)
