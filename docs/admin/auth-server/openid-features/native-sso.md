---
tags:
  - administration
  - auth-server
  - openidc
  - feature
  - native sso
  - mobile
  - single sign-on
---

# Native Single Sign-On

Increasing number of mobile phone users are using multiple mobile applications
from the same software vendor. For instance, it is fairly common to see people
using multiple mobile apps from software vendors like Google and Microsoft etc.

Software vendors need a way to allow users to sign-in in one of the apps 
provided by software vendor and user should be able to use all other apps from 
same vendor without having to sign-in again. In short, **single sign-on** for 
applications belonging to same vendor.

Janssen Server supports [OpenID Connect native SSO mechanism](https://openid.net/specs/openid-connect-native-sso-1_0.html) 
to enable SSO for mobile applications.

## Scope Support

Janssen Server supports `device_sso` scope as defined by the specification. 
Software vendor wanting to leverage native SSO feature should build the apps
so during initial user authentication the app would send `device_sso`
scope in the authorization request.

Presence of `device_sso` scope in authorization request would enable AS to 
return `device_secret` in token response from token endpoint.

## Device Secret

`device_secret` is an opaque value returned to the application from token 
endpoint as a response to token exchange request. Janssen Server will return
`device_secret` only if the code provided by the application in token exchange
request is having `device_sso` scope.

Janssen Server also checks if the client has the token exchange grant type 
enabled. To enable the grant type, use [Janssen Text-based UI(TUI)](../../config-guide/config-tools/jans-tui/README.md)
and enable token exchange grant 
(`urn:ietf:params:oauth:grant-type:token-exchange`). 

`device_token` claim in returned token response contains the device secret.
Janssen Server stores the device secretes issued to a client in corresponding
session-id.

## Processing Token Exchange Request

Janssen Server carries out processing of token request as per rules and 
checks defined [in the specification](https://openid.net/specs/openid-connect-native-sso-1_0.html#name-native-sso-processing-rules).



## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).