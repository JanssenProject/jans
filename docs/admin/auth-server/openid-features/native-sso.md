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

`device_sso`

## Changes to Token Endpoint

## Token Exchange SPEC profile

urn:ietf:params:oauth:grant-type:token-exchange

urn:x-oath:params:oauth:token-type:device-secret



## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).