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
: *Default: True* - Boolean value specifying whether to remove Refresh Tokens on logout. 
However if intention is to leave Refresh Token after logout, it's required to set it to `false`.

`skipRefreshTokenDuringRefreshing`
: *Default: False* - Boolean value specifying whether to skip refreshing tokens on refreshing. 
By default AS always creates new Refresh Token on refresh call to Token Endpoint. This property allows to avoid (skip) new Refresh Token creation.

`refreshTokenExtendLifetimeOnRotation`
: *Default: False* - Boolean value specifying whether to extend refresh tokens on rotation. By default lifetime is not extended, expiration date is set to date of previous Refresh Token. With this property it's possible to have all further Refresh Token have fixed (extended in relation to previuos Refresh Token) lifetime. 

`checkUserPresenceOnRefreshToken`
: *Default: False* - Check whether user exists and is active before creating RefreshToken. Set it to true if check is needed(Default value is false - don't check.

### Refresh Token Management

To revoke a token, a client can do so via the [revocation endpoint][../endpoints/token-revocation] (including revocation of all tokens by `client_id`).


