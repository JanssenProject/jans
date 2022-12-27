---
tags:
  - administration
  - auth-server
  - userinfo
  - endpoint
---

# Overview

Userinfo endpoint is an OAuth2 protected endpoint that is used to retrieve claims about an authenticated end-user.
Userinfo endpoint is defined in the [OpenID Connect specification](https://openid.net/specs/openid-connect-core-1_0.html#UserInfo).

URL to access userinfo endpoint on Janssen Server is listed in the response of Janssen Server's well-known 
[configuration endpoint](./configuration.md) given below. 

```text
https://<jans-server-host>/jans-auth/.well-known/openid-configuration
```

`userinfo_endpoint` claim in the response specifies the URL for userinfo endpoint.

Since userinfo endpoint is an OAuth2 protected resource, a valid access token with appropriate scope is required to 
access the endpoint. More information about request and response of the userinfo endpoint can be found in 
the OpenAPI specification of [jans-auth-server module](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans/replace-janssen-version/jans-auth-server/docs/swagger.yaml#/User_Info).

## Disabling The Endpoint Using Feature Flag

Using [USERINFO feature flag](../../reference/json/feature-flags/janssenauthserver-feature-flags.md#userinfo) 

## Configuration Properties

Userinfo endpoint can be further configured using Janssen Server configuration properties listed below. [Set property 
values](link to CURL command that will be used to set properties) according to your need. 

- [mtlsUserInfoEndpoint](../../reference/json/properties/janssenauthserver-properties.md#mtlsuserinfoendpoint)
- [userInfoConfiguration](../../reference/json/properties/janssenauthserver-properties.md#userinfoconfiguration)
- [userInfoEncryptionAlgValuesSupported](../../reference/json/properties/janssenauthserver-properties.md#userinfoencryptionalgvaluessupported)
- [userInfoEncryptionEncValuesSupported](../../reference/json/properties/janssenauthserver-properties.md#userinfoencryptionencvaluessupported)
- [userInfoEndpoint](../../reference/json/properties/janssenauthserver-properties.md#userinfoendpoint)
- [userInfoSigningAlgValuesSupported](../../reference/json/properties/janssenauthserver-properties.md#userinfosigningalgvaluessupported)

## Interception Scripts

Response from userinfo can be further customized using [dynamic scope](../../developer/scripts/dynamic-scope.md) interception script. 

## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).