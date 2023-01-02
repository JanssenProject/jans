---
tags:
- administration
- auth-server
- token-revocation
- endpoint
---

# Overview

Janssen Server supports token revocation endpoint to enable invalidation of refresh or access token. Implementation
conforms with [token revocation specification](https://datatracker.ietf.org/doc/html/rfc7009).

URL to access revocation endpoint on Janssen Server is listed in the response of Janssen Server's well-known
[configuration endpoint](./configuration.md) given below.

```text
https://<jans-server-host>/jans-auth/.well-known/openid-configuration
```

`revocation_endpoint` claim in the response specifies the URL for revocation endpoint. By default, revocation endpoint
looks like below:

```
https://jans-dynamic-ldap/jans-auth/restv1/revoke
```

More information about request and response of the revocation endpoint can be found in
the OpenAPI specification of [jans-auth-server module](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans/replace-janssen-version/jans-auth-server/docs/swagger.yaml#/Token/revoke).



## Disabling The Endpoint Using Feature Flag

`Token revocation` endpoint can be enabled or disable using [REVOKE_TOKEN feature flag](../../reference/json/feature-flags/janssenauthserver-feature-flags.md#revoketoken).
Use [Janssen Text-based UI(TUI)](../../config-guide/tui.md) or [Janssen command-line interface](../../config-guide/jans-cli/README.md) to perform this task.

When using TUI, navigate via `Auth Server`->`Properties`->`enabledFeatureFlags` to screen below. From here, enable or
disable `REVOKE_TOKEN` flag as required.

![](../../../assets/image-tui-enable-components.png)

## Configuration Properties

Token revocation endpoint can be further configured using Janssen Server configuration properties listed below. When using
[Janssen Text-based UI(TUI)](../../config-guide/tui.md) to configure the properties,
navigate via `Auth Server`->`Properties`.

- [mtlstokenrevocationendpoint](https://docs.jans.io/replace-janssen-version/admin/reference/json/properties/janssenauthserver-properties/#mtlstokenrevocationendpoint)
- [tokenRevocationEndpoint](https://docs.jans.io/replace-janssen-version/admin/reference/json/properties/janssenauthserver-properties/#tokenrevocationendpoint)

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