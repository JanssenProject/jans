---
tags:
  - administration
  - auth-server
  - token
---

## Background

Refresh tokens are an optimization that allows a client to renew an access token
without having to re-authenticate. You should not issue a refresh token to
untrusted clients, i.e. public clients.

# Server properties.

`refreshTokenLifetime`
: *Default: 14400 minutes* - With this property, you can specify a longer lifetime
then 5 days. Using the Jans Config API, you can set a client-specific refresh
token lifetime.

`forceOfflineAccessScopeToEnableRefreshToken`
: *Default: True* - A good practice is for Auth Server to explicitly ask the
subject to consent to the use of refresh tokens. To encourage this practice,
by default, Auth Server requires that the client have the `offline_access` scope
to issue a refresh token.

`persistRefreshTokenInLdap`
: *Default: True* - Otherwise they are written to the cache

`clientRegDefaultToCodeFlowWithRefresh`
: *Default: True* - Set to False if you don't want web clients to get a refresh
token.

`removeRefreshTokensForClientOnLogout`
: *Default: True* - Explicit logout is different then timeout, so if the
subject has initiated a logout, you may want to also remove all the refresh
tokens for that client. **todo**

`skipRefreshTokenDuringRefreshing`
: *Default: False* - **todo**

`refreshTokenExtendLifetimeOnRotation`
: *Default: False* - **todo**

`checkUserPresenceOnRefreshToken`
: *Default: False* - **todo**

### Refresh Token Management

To revoke a token, a client can do so via the [revocation endpoint][../endpoints/token-revocation]. In order to get a list of refresh tokens outstanding by the AS:
**todo**.
