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
use multiple mobile apps from software vendors like Google and Microsoft etc.

Software vendors need a way to allow users to sign-in in one of the apps 
provided by software vendor and user should be able to use all other apps from 
same vendor without having to sign-in again. In short, single sign-on for 
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

`device_secret`

- device secret is opaque to the client. How to exactly construct this device token is not specified in the 
spec and it is decided by AS.
- device secret identifies a unique device and it is same for all apps on that device from a particular software vendor
- what data does device secret contain? and construction of device secret? is device secret encrypted?
- does AS maintain state on backend and if not then how does AS protect device secret

`Session ID`
- how is it created? and used?

## Changes to Token Endpoint

- changes to id_token
  - how does AS manage binding between device_secret and ds_hash
  - how does AS protect the relationship between the id_token and device_secret?
  - The device_secret is returned in the device_token claim of the returned JSON data.

## Token Exchange SPEC profile

urn:ietf:params:oauth:grant-type:token-exchange

urn:x-oath:params:oauth:token-type:device-secret



## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).