---
tags:
- administration
- auth-server
- logout-status-jwt
---

# Logout Status JWT

## Overview

The Logout Status JWT is a compact, self-contained, purpose-built token that represents the state and validity of a user "logged-in" state.

Logout Status JWT is returned from Authorization Endpoint and Authorization Challenge Endpoint by sending `logout_status_jwt=true` parameter in request. It is returned back upon successful authn&authz (when `code` is returned).

Janssen Server provides Token Status List endpoint to enable the client to query token status.
Logout Status JWT state can be queries via Token Status List endpoint.

## Usage

Logout Status JWT can be requested at Authorization Endpoint or Authorization Challenge Endpoint by providing additional parameter `logout_status_jwt=true`.

Sample request (that requests Logout Status JWT)

```curl
https://janssen.server.host/jans-auth/restv1/authorize?response_type=code&client_id=435636e1-c8cf-4b27-8ee3-d6121286f3dc&scope=openid+profile+address+email+phone+user_name+revoke_any_token+global_token_revocation&redirect_uri=https%3A%2F%2Fjanssen.server.host%2Fjans-auth-rp%2Fhome.htm&state=172645ff-4887-4e97-9152-197efb1591d0&nonce=85b2161e-7502-4619-8eaf-1caa4cf7bec0&prompt=&ui_locales=&claims_locales=&acr_values=&request_session_id=false&logout_status_jwt=true
```

After successful authentication and authorization AS returns back also Logout Status JWT.

Sample response
```text
HTTP/1.1 302 Found
Location: https://janssen.server.host/jans-auth-rp/home.htm?code=03cb499b-a6fb-4ac1-b406-051baf7278cc&scope=openid+revoke_any_token+global_token_revocation&logout_status_jwt=eyJraWQiOiJjb25uZWN0XzA3ZjQ3ZmRkLTg0NTYtNDMzOC1iYTRmLWNhNmU0NDBmYjljOV9zaWdfcnMyNTYiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJhdWQiOiI0MzU2MzZlMS1jOGNmLTRiMjctOGVlMy1kNjEyMTI4NmYzZGMiLCJuYmYiOjE3NDkyMjUwMzgsInN0YXR1c19saXN0Ijp7ImlkeCI6ODEsInVyaSI6Imh0dHBzOi8veXVyaXl6LWludGVybmFsLWphZ3Vhci5nbHV1LmluZm8vamFucy1hdXRoL3Jlc3R2MS9zdGF0dXNfbGlzdCJ9LCJpc3MiOiJodHRwczovL3l1cml5ei1pbnRlcm5hbC1qYWd1YXIuZ2x1dS5pbmZvIiwiZXhwIjoxNzQ5MzExNDM4LCJpYXQiOjE3NDkyMjUwMzgsImp0aSI6ImZmOWFkMzEyLWRmODgtNGJkOS04ODhlLWJhNTk1NDNlNmE4MCJ9.eIHpiFoc7tp4Yin52cje7u8vNsnojcpurKWel2pGOKcWkjZw2f3Ioyhpe0nacNiA8tJDV11B6i_S49q4X4TTFjoY-oR7bfbXMhKe64L1Km71qP240rfsppcxbpVccRjvwFpX3ZSrvq1yWaxY4E8UoiRS168JtXGBlvZ1Hr89GoUVEdsTmowITRbSCQK2s7ENsD53P9M0--mag3YFAA9LTvQqfWtD1fhqoqpC9Hm4rEnO6VgTbI0tCuKIB1ncnMU30gH_PrKY53Z_n3NEYF_BdwkejcHE6mGybAtlTEXGqVSt5FQFUKIvsJM8kW6mK_XGsIhFNu49T70oPEs5ZSD2Zg&state=172645ff-4887-4e97-9152-197efb1591d0&session_state=f71764d3f7f2280ecbfe170a5c0dd51b3e20769c7deadaee1bda8eb8d2df0f8f.3281229a-0118-4037-895b-43aed75ad2f8
```

**Encoded Logout Status JWT example**
```java
eyJraWQiOiJjb25uZWN0XzA3ZjQ3ZmRkLTg0NTYtNDMzOC1iYTRmLWNhNmU0NDBmYjljOV9zaWdfcnMyNTYiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJhdWQiOiI0MzU2MzZlMS1jOGNmLTRiMjctOGVlMy1kNjEyMTI4NmYzZGMiLCJuYmYiOjE3NDkyMjUwMzgsInN0YXR1c19saXN0Ijp7ImlkeCI6ODEsInVyaSI6Imh0dHBzOi8veXVyaXl6LWludGVybmFsLWphZ3Vhci5nbHV1LmluZm8vamFucy1hdXRoL3Jlc3R2MS9zdGF0dXNfbGlzdCJ9LCJpc3MiOiJodHRwczovL3l1cml5ei1pbnRlcm5hbC1qYWd1YXIuZ2x1dS5pbmZvIiwiZXhwIjoxNzQ5MzExNDM4LCJpYXQiOjE3NDkyMjUwMzgsImp0aSI6ImZmOWFkMzEyLWRmODgtNGJkOS04ODhlLWJhNTk1NDNlNmE4MCJ9.eIHpiFoc7tp4Yin52cje7u8vNsnojcpurKWel2pGOKcWkjZw2f3Ioyhpe0nacNiA8tJDV11B6i_S49q4X4TTFjoY-oR7bfbXMhKe64L1Km71qP240rfsppcxbpVccRjvwFpX3ZSrvq1yWaxY4E8UoiRS168JtXGBlvZ1Hr89GoUVEdsTmowITRbSCQK2s7ENsD53P9M0--mag3YFAA9LTvQqfWtD1fhqoqpC9Hm4rEnO6VgTbI0tCuKIB1ncnMU30gH_PrKY53Z_n3NEYF_BdwkejcHE6mGybAtlTEXGqVSt5FQFUKIvsJM8kW6mK_XGsIhFNu49T70oPEs5ZSD2Zg
```

**Decoded payload of Logout Status JWT**

```json
{
  "aud": "435636e1-c8cf-4b27-8ee3-d6121286f3dc",
  "nbf": 1749225038,
  "status_list": {
    "idx": 81,
    "uri": "https://janssen.server.host/jans-auth/restv1/status_list"
  },
  "iss": "https://janssen.server.host",
  "exp": 1749311438,
  "iat": 1749225038,
  "jti": "ff9ad312-df88-4bd9-888e-ba59543e6a80"
}
```

**Claims**
- idx - index in token status list
- exp - expiration date of the Logout Status Token

When RP receives Logout Status JWT and it can query Token Status List to get Logout Status JWT status at any time.

**Sample status list request**
```java
POST /jans-auth/restv1/status_list
Accept: application/statuslist+json
```

**Sample status list response**
```json
{
  "sub": "https://janssen.server.host/jans-auth/restv1/status_list",
  "nbf": 1747049562,
  "status_list": {
    "bits": 2,
    "lst": "eNpjZRipgBEABeMABw"
  },
  "iss": "https://janssen.server.host",
  "exp": 1747050162,
  "iat": 1747049562,
  "ttl": 600
}
```

**lst** contains encoded status list. Status list has encoded statuses based on [Token Status List spec](https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-02.html).

**Important**
Logout Status JWT specifies `exp` which means that token must be considered expired when this time is reached independently from Token Status List response.
If Token Status List has `INVALID` (`1` - at `idx` index for 2 bits) then RP must consider Logout Status JWT as invalid. 


## Disabling Logout Status JWT Using Feature Flag

`Logout Status JWT` can be enabled or disable using [logout_status_jwt feature flag](../../reference/json/feature-flags/janssenauthserver-feature-flags.md#logout_status_jwt).
Use [Janssen Text-based UI(TUI)](../../config-guide/config-tools/jans-tui/README.md) or [Janssen command-line interface](../../config-guide/config-tools/jans-cli/README.md) to perform this task.

When using TUI, navigate via `Auth Server`->`Properties`->`enabledFeatureFlags` to screen below. From here, enable or
disable `logout_status_jwt` flag as required.

![](../../../assets/image-tui-enable-components.png)

## Revoking

Logout Status JWT can be revoked:
- via standard Revocation Endpoint by providing full token (`/revoke` and form parameters `token_type_hint=logout_status_jwt&token=<logout status jwt>`)
- via standard Revocation Endpoint by providing jti (`/revoke` and form parameters `token_type_hint=jti&token=<jti from logout status jwt>`)
- via Global Token Revocation Endpoint

After revocation token status is changed in Token Status List (`VALID` -> `INVALID`)

## Configuration

- `logoutStatusJwtLifetime` - The lifetime of Logout Status JWT. If not set falls back to 1 day
- `logoutStatusJwtSigningAlgValuesSupported` - This JSON Array lists which JWS signing algorithms (alg values) [JWA] can be used by for the Logout Status JWT at Authorization Endpoint to encode the claims in a JWT 
- `logout_status_jwt` value within `featureFlags` to enable/disable Logout Status JWT.

## References

- [Token Status List spec](https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-02.html)

