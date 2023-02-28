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
https://janssen.server.host/jans-auth/.well-known/openid-configuration
```

`authorization_endpoint` claim in the response specifies the URL for authorization endpoint. By default, authorization 
endpoint looks like below:

```
https://janssen.server.host/jans-auth/restv1/authorize
```

More information about request and response of the authorization endpoint can be found in the OpenAPI specification 
of [jans-auth-server module](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans/vreplace-janssen-version/jans-auth-server/docs/swagger.yaml#/Authorization).

## Disabling The Endpoint Using Feature Flag

TODO: It seems this endpoint can't be disabled using featureflags. Confirm this.


## Configuration Properties

Authorization endpoint can be further configured using Janssen Server configuration properties listed below. When using
[Janssen Text-based UI(TUI)](../../config-guide/tui.md) to configure the properties,
navigate via `Auth Server`->`Properties`.

- [issuer](../../reference/json/properties/janssenauthserver-properties.md#issuer)
- [requirePkce](../../reference/json/properties/janssenauthserver-properties.md#requirepkce)
- [fapiCompatibility](../../reference/json/properties/janssenauthserver-properties.md#fapicompatibility)
- [forceSignedRequestObject](../../reference/json/properties/janssenauthserver-properties.md#forcesignedrequestobject)
- [authorizationCodeLifetime](../../reference/json/properties/janssenauthserver-properties.md#authorizationcodelifetime)
- [returnDeviceSecretFromAuthzEndpoint](../../reference/json/properties/janssenauthserver-properties.md#returndevicesecretfromauthzendpoint)
- [requestUriBlockList](../../reference/json/properties/janssenauthserver-properties.md#requesturiblocklist)
- [requireRequestObjectEncryption](../../reference/json/properties/janssenauthserver-properties.md#requirerequestobjectencryption)
- [staticDecryptionKid](../../reference/json/properties/janssenauthserver-properties.md#staticdecryptionkid)
- [requestUriHashVerificationEnabled](../../reference/json/properties/janssenauthserver-properties.md#requesturihashverificationenabled)
- [legacyIdTokenClaims](../../reference/json/properties/janssenauthserver-properties.md#legacyidtokenclaims)
- [customHeadersWithAuthorizationResponse](../../reference/json/properties/janssenauthserver-properties.md#customheaderswithauthorizationresponse)
- [includeSidInResponse](../../reference/json/properties/janssenauthserver-properties.md#includesidinresponse)
- [sessionIdRequestParameterEnabled](../../reference/json/properties/janssenauthserver-properties.md#sessionidrequestparameterenabled)
- [returnDeviceSecretFromAuthzEndpoint](../../reference/json/properties/janssenauthserver-properties.md#returndevicesecretfromauthzendpoint)
- [requirePar](../../reference/json/properties/janssenauthserver-properties.md#requirepar)
- [cibaMaxExpirationTimeAllowedSec](../../reference/json/properties/janssenauthserver-properties.md#cibamaxexpirationtimeallowedsec)

## Required Client Configuration

Clients must be registered with Janssen Server as using [code](https://www.rfc-editor.org/rfc/rfc6749#section-4.1) 
and/or [implicit](https://www.rfc-editor.org/rfc/rfc6749#section-4.2) grant types in order to use authorization endpoint.

Using [Janssen Text-based UI(TUI)](../../config-guide/tui.md), client can be registered for appropriate grant type by
navigating to `Auth-Server`->`Clients`->`Add Client`

## Using PKCE

Janssen Server [supports PKCE](../oauth-features/pkce.md), which recommended and more secure method for using `code`
grant. 

PKCE can be enabled/disable by setting [requirePkce](../../reference/json/properties/janssenauthserver-properties.md#requirepkce)
property. Janssen server supports `plain` as well as `s256` code challenge methods. 

## Using PAR

As a separate endpoint, Janssen Server [supports PAR](../oauth-features/par.md)(Pushed Authorization Requests) to enable authorization using 
more complex authorization requests and making it more secure at the same time. 

Use Janssen Server configuration property [requirePar](../../reference/json/properties/janssenauthserver-properties.md#requirepar)
to accept only PAR requests. 

## Using JARM

Authorization endpoint supports JWT Secured Authorization Response Mode, or [JARM](../openid-features/jarm.md). Using 
JARM makes authorization responses more secure and compliant to be used in FAPI deployments. 

Janssen Server supports all response modes as defined in [JARM specification](https://openid.net//specs/openid-financial-api-jarm.html#response-encoding) 

## Using Prompt Parameter

`prompt` request parameter is an ASCII string value that specifies whether the Authorization Server prompts the End-User
for re-authentication and consent. Janssen Server supports `none`, `login`, `consent` and `select_account` values for
`prompt` parameter. Multiple values can be specified by separating them with single space. Based on value/s of this 
request parameter Authorization Server prompts the End-User for re-authentication and consent. 

### none

`none` value will instruct Janssen Server NOT to display any authentication or consent user interface pages. 
An error is returned if the End-User is not already authenticated or the Client does not have pre-configured consent for
the requested scopes. This can be used as a method to check for existing authentication and/or consent.

### login

`login` value will instruct Janssen Server to prompt the End-User for re-authentication.

### consent

`consent` value will instruct Janssen Server to prompt the End-User for consent before returning information to the 
Client.

### select_account

`select_account` value will instruct Janssen Server to prompt the End-User to select a user account. This allows a user
who has multiple accounts at the Authorization Server to select amongst the multiple accounts that they may have current
sessions for.

## Configuring Authentication Methods

`acr_values` request parameter is used to specify authentication methods to be used by Janssen Server to authenticate
the end user. Multiple acr values can be specified by separating them with a space.

In order to use a particular acr value, the client needs to be authorized to use all the acr values in the list. If no
the request doesn't specify any acr value then the default acr value configured for respective client is used by Janssen
server for end user authentication.

## Customizing using Interception Scripts

[Interception scripts](../../developer/interception-scripts.md) allows
flexibility to configure and customize multiple aspects in Janssen Server. For example, see this documentation to learn
how [person authentication](../../developer/scripts/person-authentication.md)
and [consent gathering](../../developer/scripts/consent-gathering.md) can be 
customized using interception scripts.

## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).