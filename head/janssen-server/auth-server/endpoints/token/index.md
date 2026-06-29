# Token Endpoint

Token endpoint is an OAuth2 protected endpoint that is used to grant tokens to client in response to valid request. Token endpoint is defined in the [OAuth 2.0 framework](https://datatracker.ietf.org/doc/html/rfc6749), [OpenID Connect](https://openid.net/specs/openid-connect-core-1_0.html) specification and other specifications relevant to them.

Tokens granted by this endpoint depends on grant type and scopes that are specified in the token request. The token endpoint is used with every authorization grant type except for the implicit grant type (since an access token is issued directly).

Based on request, this endpoint can grant following types of tokens:

- [Access Token](https://datatracker.ietf.org/doc/html/rfc6749#section-1.4)
- [Refresh Token](https://datatracker.ietf.org/doc/html/rfc6749#section-1.5)
- [ID Token](https://openid.net/specs/openid-connect-core-1_0.html#IDToken)

URL to access token endpoint on Janssen Server is listed in the response of Janssen Server's well-known [configuration endpoint](https://docs.jans.io/head/janssen-server/auth-server/endpoints/configuration/index.md) given below.

```
https://janssen.server.host/jans-auth/.well-known/openid-configuration
```

`token_endpoint` claim in the response specifies the URL for userinfo endpoint. By default, userinfo endpoint looks like below:

```
https://janssen.server.host/jans-auth/restv1/token
```

In response to a valid request, the token endpoint returns token/s in JSON format similar to below. This is just a sample response. Actual response can greatly vary in its contents based on request:

```
  HTTP/1.1 200 OK
  Content-Type: application/json
  Cache-Control: no-store
  Pragma: no-cache

  {
   "access_token": "SlAV32hkKG",
   "token_type": "Bearer",
   "refresh_token": "8xLOxBtZp8",
   "expires_in": 3600,
   "id_token": "eyJhbGciOiJSUzI1NiIsImtpZCI6IjFlOWdkazcifQ.ewogImlzc
     yI6ICJodHRwOi8vc2VydmVyLmV4YW1wbGUuY29tIiwKICJzdWIiOiAiMjQ4Mjg5
     NzYxMDAxIiwKICJhdWQiOiAiczZCaGRSa3F0MyIsCiAibm9uY2UiOiAibi0wUzZ
     fV3pBMk1qIiwKICJleHAiOiAxMzExMjgxOTcwLAogImlhdCI6IDEzMTEyODA5Nz
     AKfQ.ggW8hZ1EuVLuxNuuIJKX_V8a_OMXzR0EHR9R6jgdqrOOF4daGU96Sr_P6q
     Jp6IcmD3HP99Obi1PRs-cwh3LO-p146waJ8IhehcwL7F09JdijmBqkvPeB2T9CJ
     NqeGpe-gccMg4vfKjkM8FcGvnzZUN4_KSP0aAp1tOJ1zZwgjxqGByKHiOtX7Tpd
     QyHE5lcMiKPXfEIQILVq0pc_E2DzL7emopWoaoZTF_m0_N0YzFC6g6EJbOEoRoS
     K5hoDalrcvRYLSrQAZZKflyuVCyixEoV9GfNQC3_osjzw2PAithfubEEBLuVVk4
     XUVrWOLrLl0nx7RkKU8NXNHq-rvKMzqg"
  }
```

More information about request and response of the token endpoint can be found in the OpenAPI specification of [jans-auth-server module](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans/vreplace-janssen-version/jans-auth-server/docs/swagger.yaml#/Token/post-token).

## Configuration Properties

Token endpoint and tokens issued by token endpoint can be further configured using Janssen Server configuration properties listed below. When using [Janssen Text-based UI(TUI)](https://docs.jans.io/head/janssen-server/config-guide/config-tools/jans-tui/index.md) to configure the properties, navigate via `Auth Server`->`Properties`.

- [tokenEndpoint](https://docs.jans.io/head/janssen-server/reference/json/properties/janssenauthserver-properties/#tokenendpoint)
- [tokenEndpointAuthMethodsSupported](https://docs.jans.io/head/janssen-server/reference/json/properties/janssenauthserver-properties/#tokenendpointauthmethodssupported)
- [tokenEndpointAuthSigningAlgValuesSupported](https://docs.jans.io/head/janssen-server/reference/json/properties/janssenauthserver-properties/#tokenendpointauthsigningalgvaluessupported)
- [accessTokenLifetime](https://docs.jans.io/head/janssen-server/reference/json/properties/janssenauthserver-properties/#accesstokenlifetime)
- [checkUserPresenceOnRefreshToken](https://docs.jans.io/head/janssen-server/reference/json/properties/janssenauthserver-properties/#checkuserpresenceonrefreshtoken)
- [defaultSignatureAlgorithm](https://docs.jans.io/head/janssen-server/reference/json/properties/janssenauthserver-properties/#defaultsignaturealgorithm)
- [forceOfflineAccessScopeToEnableRefreshToken](https://docs.jans.io/head/janssen-server/reference/json/properties/janssenauthserver-properties/#forceofflineaccessscopetoenablerefreshtoken)
- [grantTypesSupported](https://docs.jans.io/head/janssen-server/reference/json/properties/janssenauthserver-properties/#granttypessupported)
- [accessTokenSigningAlgValuesSupported](https://docs.jans.io/head/janssen-server/reference/json/properties/janssenauthserver-properties/#accesstokensigningalgvaluessupported)
- [idTokenEncryptionAlgValuesSupported](https://docs.jans.io/head/janssen-server/reference/json/properties/janssenauthserver-properties/#idtokenencryptionalgvaluessupported)
- [idTokenEncryptionEncValuesSupported](https://docs.jans.io/head/janssen-server/reference/json/properties/janssenauthserver-properties/#idtokenencryptionencvaluessupported)
- [idTokenFilterClaimsBasedOnAccessToken](https://docs.jans.io/head/janssen-server/reference/json/properties/janssenauthserver-properties/#idtokenfilterclaimsbasedonaccesstoken)
- [idTokenLifetime](https://docs.jans.io/head/janssen-server/reference/json/properties/janssenauthserver-properties/#idtokenlifetime)
- [idTokenSigningAlgValuesSupported](https://docs.jans.io/head/janssen-server/reference/json/properties/janssenauthserver-properties/#idtokensigningalgvaluessupported)
- [accessTokenSigningAlgValuesSupported](https://docs.jans.io/head/janssen-server/reference/json/properties/janssenauthserver-properties/#accesstokensigningalgvaluessupported)
- [legacyIdTokenClaims](https://docs.jans.io/head/janssen-server/reference/json/properties/janssenauthserver-properties/#legacyidtokenclaims)
- [mtlsTokenEndpoint](https://docs.jans.io/head/janssen-server/reference/json/properties/janssenauthserver-properties/#mtlstokenendpoint)
- [openidScopeBackwardCompatibility](https://docs.jans.io/head/janssen-server/reference/json/properties/janssenauthserver-properties/#openidscopebackwardcompatibility)
- [persistIdToken](https://docs.jans.io/head/janssen-server/reference/json/properties/janssenauthserver-properties/#persistidtoken)
- [persistRefreshToken](https://docs.jans.io/head/janssen-server/reference/json/properties/janssenauthserver-properties/#persistrefreshtoken)
- [refreshTokenExtendLifetimeOnRotation](https://docs.jans.io/head/janssen-server/reference/json/properties/janssenauthserver-properties/#refreshtokenextendlifetimeonrotation)
- [refreshTokenLifetime](https://docs.jans.io/head/janssen-server/reference/json/properties/janssenauthserver-properties/#refreshtokenlifetime)
- [responseTypesSupported](https://docs.jans.io/head/janssen-server/reference/json/properties/janssenauthserver-properties/#responsetypessupported)
- [skipRefreshTokenDuringRefreshing](https://docs.jans.io/head/janssen-server/reference/json/properties/janssenauthserver-properties/#skiprefreshtokenduringrefreshing)
- [refreshTokenLifetime](https://docs.jans.io/head/janssen-server/reference/json/properties/janssenauthserver-properties/#refreshtokenlifetime)

## Client Authentication

Janssen Server Token Endpoint requires confidential clients to authenticate using one of the supported client authentication method listed below:

- client_secret_basic
- client_secret_post
- client_secret_jwt
- private_key_jwt

Refer to [Client Authentication](https://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication) section of OpenID Connect core specification for more details on these authentication methods.

AS provides ability to customize Client Authentication behavior via [Client Authentication custom script](https://docs.jans.io/head/script-catalog/client_authn/client-authn/index.md)

Client can specify the default authentication method. To set default authentication method using [Janssen Text-based UI(TUI)](https://docs.jans.io/head/janssen-server/config-guide/config-tools/jans-tui/index.md), navigate via `Auth Server`->`Clients`->`Add Client`->`Basic`-> `Authn Method Token Endpoint`.

## Supported Grant Types

Token endpoint supports below mentioned grant types.

- [Authorization Code](https://docs.jans.io/head/janssen-server/auth-server/oauth-features/auth-code-grant/index.md)
- [Implicit](https://docs.jans.io/head/janssen-server/auth-server/oauth-features/implicit-grant/index.md)
- [Refresh Token](https://docs.jans.io/head/janssen-server/auth-server/oauth-features/index.md)
- [Client Credentials](https://docs.jans.io/head/janssen-server/auth-server/oauth-features/client-credential-grant/index.md)
- [Password](https://docs.jans.io/head/janssen-server/auth-server/oauth-features/password-grant/index.md)
- [Token Exchange](https://docs.jans.io/head/janssen-server/auth-server/oauth-features/token-exchange/index.md)
- [Transaction Tokens](https://docs.jans.io/head/janssen-server/auth-server/tokens/oauth-tx-tokens/index.md)
- [Device Grant](https://docs.jans.io/head/janssen-server/auth-server/oauth-features/device-grant/index.md)
- [JWT Grant](https://docs.jans.io/head/janssen-server/auth-server/oauth-features/jwt-grant/index.md)
- [CIBA](https://docs.jans.io/head/janssen-server/auth-server/endpoints/backchannel-authentication/index.md)

Client can configure all the possible grant types it can request from token endpoint during client configuration. To select the available grant types using [Janssen Text-based UI(TUI)](https://docs.jans.io/head/janssen-server/config-guide/config-tools/jans-tui/index.md), navigate via `Auth Server`->`Clients`->`Add Client`/`search client`->`Basic`-> `Grant`.

## Interception Scripts

Token endpoint response can be further customized using [interception scripts](https://docs.jans.io/head/janssen-server/developer/scripts/index.md). Following interception scripts are relevant to token endpoint:

- [Update Token](https://docs.jans.io/head/script-catalog/update_token/update-token/index.md)

Client can configure a particular script to be executed using client configuration. To update configuration using [Janssen Text-based UI(TUI)](https://docs.jans.io/head/janssen-server/config-guide/config-tools/jans-tui/index.md) navigate via `Auth Server`->`Clients`->`Add Client`/`search`-> `Client Scripts`
