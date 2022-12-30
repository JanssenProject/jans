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

## Disabling The Endpoint Using Feature Flag

TODO: It seems this endpoint can't be disabled using featureflags. Confirm this.


## Configuration Properties

Authorization endpoint can be further configured using Janssen Server configuration properties listed below. When using
[Janssen Text-based UI(TUI)](../../config-guide/tui.md) to configure the properties,
navigate via `Auth Server`->`Properties`.

- [issuer](https://docs.jans.io/head/admin/reference/json/properties/janssenauthserver-properties/#issuer)
- [requirePar](https://docs.jans.io/head/admin/reference/json/properties/janssenauthserver-properties/#requirepar)
- [requirePkce](https://docs.jans.io/head/admin/reference/json/properties/janssenauthserver-properties/#requirepkce)
- [fapiCompatibility](https://docs.jans.io/head/admin/reference/json/properties/janssenauthserver-properties/#fapicompatibility)
- [forceSignedRequestObject](https://docs.jans.io/head/admin/reference/json/properties/janssenauthserver-properties/#forcesignedrequestobject)
- [authorizationCodeLifetime](https://docs.jans.io/head/admin/reference/json/properties/janssenauthserver-properties/#authorizationcodelifetime)
- [returnDeviceSecretFromAuthzEndpoint](https://docs.jans.io/head/admin/reference/json/properties/janssenauthserver-properties/#returndevicesecretfromauthzendpoint)
- [requestUriBlockList](https://docs.jans.io/head/admin/reference/json/properties/janssenauthserver-properties/#requesturiblocklist)
- [requireRequestObjectEncryption](https://docs.jans.io/head/admin/reference/json/properties/janssenauthserver-properties/#requirerequestobjectencryption)
- [staticDecryptionKid](https://docs.jans.io/head/admin/reference/json/properties/janssenauthserver-properties/#staticdecryptionkid)
- [requestUriHashVerificationEnabled](https://docs.jans.io/head/admin/reference/json/properties/janssenauthserver-properties/#requesturihashverificationenabled)
- [legacyIdTokenClaims](https://docs.jans.io/head/admin/reference/json/properties/janssenauthserver-properties/#legacyidtokenclaims)
- [customHeadersWithAuthorizationResponse](https://docs.jans.io/head/admin/reference/json/properties/janssenauthserver-properties/#customheaderswithauthorizationresponse)
- [includeSidInResponse](https://docs.jans.io/head/admin/reference/json/properties/janssenauthserver-properties/#includesidinresponse)
- [sessionIdRequestParameterEnabled](https://docs.jans.io/head/admin/reference/json/properties/janssenauthserver-properties/#sessionidrequestparameterenabled)
- [returnDeviceSecretFromAuthzEndpoint](https://docs.jans.io/head/admin/reference/json/properties/janssenauthserver-properties/#returndevicesecretfromauthzendpoint)
- [cibaMaxExpirationTimeAllowedSec](https://docs.jans.io/head/admin/reference/json/properties/janssenauthserver-properties/#cibamaxexpirationtimeallowedsec)

## Required Client Configuration

TODO: elaborate on client must be registered with appropriate grant type (code and implicit) 

## Configuring Authentication Methods

TODO: Elaborate on interception scripts

## Using PKCE

## Using PAR

## Using JARM

## Authorization Flows

TODO: Flows should be elaborated in separate documents if needed. Just add link here.

## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).