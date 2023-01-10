---
tags:
- administration
- auth-server
- token
- endpoint
---

# Overview

Token endpoint is an OAuth2 protected endpoint that is used to grant tokens to client in response to valid request.
Token endpoint is defined in the [OAuth 2.0 framework](https://datatracker.ietf.org/doc/html/rfc6749), 
[OpenID Connect](https://openid.net/specs/openid-connect-core-1_0.html) specification and other specifications relevant
to them. 

Tokens granted by this endpoint depends on grant type and scopes that are specified in the token request. The token
endpoint is used with every authorization grant except for the implicit grant type (since an access token is issued 
directly). 

Based on request, this endpoint can grant following types of tokens:

- [Access Token](https://datatracker.ietf.org/doc/html/rfc6749#section-1.4)
- [Refresh Token](https://datatracker.ietf.org/doc/html/rfc6749#section-1.5)
- [ID Token](https://openid.net/specs/openid-connect-core-1_0.html#IDToken)


URL to access token endpoint on Janssen Server is listed in the response of Janssen Server's well-known
[configuration endpoint](./configuration.md) given below.

```text
https://janssen.server.host/jans-auth/.well-known/openid-configuration
```

`token_endpoint` claim in the response specifies the URL for userinfo endpoint. By default, userinfo endpoint looks
like below:

```
https://janssen.server.host/jans-auth/restv1/token
```

In response to a valid request, the token endpoint returns token/s in JSON format similar to below. This is just a 
sample response. Actual response can greatly vary in its contents based on request:

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

## Disabling The Endpoint Using Feature Flag

`userinfo` endpoint can be enabled or disable using [USERINFO feature flag](../../reference/json/feature-flags/janssenauthserver-feature-flags.md#userinfo).
Use [Janssen Text-based UI(TUI)](../../config-guide/tui.md) or [Janssen command-line interface](../../config-guide/jans-cli/README.md) to perform this task.

When using TUI, navigate via `Auth Server`->`Properties`->`enabledFeatureFlags` to screen below. From here, enable or
disable `USERINFO` flag as required.

![](../../../assets/image-tui-enable-components.png)

## Configuration Properties

Userinfo endpoint can be further configured using Janssen Server configuration properties listed below. When using
[Janssen Text-based UI(TUI)](../../config-guide/tui.md) to configure the properties,
navigate via `Auth Server`->`Properties`.

- [mtlsUserInfoEndpoint](../../reference/json/properties/janssenauthserver-properties.md#mtlsuserinfoendpoint)
- [userInfoConfiguration](../../reference/json/properties/janssenauthserver-properties.md#userinfoconfiguration)
- [userInfoEncryptionAlgValuesSupported](../../reference/json/properties/janssenauthserver-properties.md#userinfoencryptionalgvaluessupported)
- [userInfoEncryptionEncValuesSupported](../../reference/json/properties/janssenauthserver-properties.md#userinfoencryptionencvaluessupported)
- [userInfoEndpoint](../../reference/json/properties/janssenauthserver-properties.md#userinfoendpoint)
- [userInfoSigningAlgValuesSupported](../../reference/json/properties/janssenauthserver-properties.md#userinfosigningalgvaluessupported)

## Using Scopes To Control Claim Release

### Standard Scopes

In context of OpenID Connect specification, claim information released by userinfo endpoint can be controlled using
scopes. Janssen Server supports all [standard scopes](https://openid.net/specs/openid-connect-core-1_0.html#ScopeClaims)
and releases corresponding claims as per OpenID Connect specification. Administrator can customise standard scopes and
define claims to be linked to each standard scope.

When using [Janssen Text-based UI(TUI)](../../config-guide/tui.md) to configure the scopes, navigate via
`Auth Server`->`Scopes`->`Add Scopes`->`Scope Type` as `OpenID`->search for a standard scope like `address`

### Dynamic Scopes

In addition to standard scopes, Janssen server allows defining custom scopes which can be associated to user-defined
list of claims. This allows administrators to create custom groupings of claims.

When using [Janssen Text-based UI(TUI)](../../config-guide/tui.md), navigate via
`Auth Server`->`Scopes`->`Add Scopes`->`Scope Type` as `Dynamic`

### Interception Scripts

Response from userinfo can be further customized using [dynamic scope](../../developer/scripts/dynamic-scope.md) interception script.

Administrator can attach a dynamic scope script to a dynamic scope using [Janssen Text-based UI(TUI)](../../config-guide/tui.md).
Navigate to `Auth Server`->`Scopes`->`Add Scopes`->`Scope Type` as `Dynamic`->`Dynamic Scope Script`

## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).