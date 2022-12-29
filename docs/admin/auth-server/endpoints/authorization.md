---
tags:
  - administration
  - auth-server
  - authorization
  - endpoint
---

# Overview

Janssen Server exposes authorization endpoint compliant with [OAuth2 framework](https://www.rfc-editor.org/rfc/rfc6749#section-3.1).
A client uses authorization endpoint to obtain an authorization grant. Based on response type requested by the client, 
the authorization endpoint issues an authorization code or an access token. Authorization endpoint is a protected endpoint
which will require end-user authentication before issuing authorization code or access token.

URL to access authorization endpoint on Janssen Server is listed in the response of Janssen Server's well-known
[configuration endpoint](./configuration.md) given below.

```text
https://<jans-server-host>/jans-auth/.well-known/openid-configuration
```

`authorization_endpoint` claim in the response specifies the URL for authorization endpoint. By default, authorization 
endpoint looks like below:

```
https://janssen.server.host/jans-auth/restv1/authorize
```

More information about request and response of the authorization endpoint can be found in the OpenAPI specification 
of [jans-auth-server module](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans/replace-janssen-version/jans-auth-server/docs/swagger.yaml#/Authorization).

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