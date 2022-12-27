---
tags:
  - administration
  - auth-server
  - userinfo
  - endpoint
---

# Overview

Userinfo endpoint is used to retrieve claims about an authenticated end-user. It is a OAuth2 protected endpoint that
can be accessed using valid access token. Userinfo endpoint is part of the [OpenID Connect specification](https://openid.net/specs/openid-connect-core-1_0.html#UserInfo).

URL for userinfo endpoint can be obtained from the response of Janssen Server's well-known 
[configuration endpoint](./configuration.md) given below. `userinfo_endpoint` claim in the response specifies the URL.

```text
https://<jans-server-host>/jans-auth/.well-known/openid-configuration
```

More information about request and response of the userinfo endpoint can be found in the [OpenAPI specification](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans/replace-janssen-version/jans-auth-server/docs/swagger.yaml#/User_Info).

## Disabling The Endpoint Using Feature Flag

Using [USERINFO feature flag](../../reference/json/feature-flags/janssenauthserver-feature-flags.md#userinfo) 

## Relevant Properties

- [mtlsUserInfoEndpoint](../../reference/json/properties/janssenauthserver-properties.md#mtlsuserinfoendpoint)
- [userInfoConfiguration](../../reference/json/properties/janssenauthserver-properties.md#userinfoconfiguration)
- [userInfoEncryptionAlgValuesSupported](../../reference/json/properties/janssenauthserver-properties.md#userinfoencryptionalgvaluessupported)
- [userInfoEncryptionEncValuesSupported](../../reference/json/properties/janssenauthserver-properties.md#userinfoencryptionencvaluessupported)
- [userInfoEndpoint](../../reference/json/properties/janssenauthserver-properties.md#userinfoendpoint)
- [userInfoSigningAlgValuesSupported](../../reference/json/properties/janssenauthserver-properties.md#userinfosigningalgvaluessupported)

## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).