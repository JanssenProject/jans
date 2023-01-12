---
tags:
- administration
- auth-server
- par
- endpoint
---

# Overview

Userinfo endpoint is an OAuth2 protected endpoint that is used to retrieve claims about an authenticated end-user.
Userinfo endpoint is defined in the [OpenID Connect specification](https://openid.net/specs/openid-connect-core-1_0.html#UserInfo).

URL to access userinfo endpoint on Janssen Server is listed in the response of Janssen Server's well-known
[configuration endpoint](./configuration.md) given below.

```text
https://janssen.server.host/jans-auth/.well-known/openid-configuration
```

`userinfo_endpoint` claim in the response specifies the URL for userinfo endpoint. By default, userinfo endpoint looks
like below:

```
https://janssen.server.host/jans-auth/restv1/userinfo
```

In response to a valid request, the userinfo endpoint returns user information in JSON format similar to below:

```
  HTTP/1.1 200 OK
  Content-Type: application/json

  {
   "sub": "3482897610054",
   "name": "Chad Wick",
   "given_name": "Chad",
   "family_name": "Wick",
   "preferred_username": "c.wick",
   "email": "cwick@jans.com",
   "picture": "http://mysite.com/mypic.jpg"
  }
```

Since userinfo endpoint is an OAuth2 protected resource, a valid access token with appropriate scope is required to
access the endpoint. More information about request and response of the userinfo endpoint can be found in
the OpenAPI specification of [jans-auth-server module](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans/replace-janssen-version/jans-auth-server/docs/swagger.yaml#/User_Info).



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