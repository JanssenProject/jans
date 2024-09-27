---
tags:
  - administration
  - auth-server
  - openidc
  - feature
---

# OpenID Connect Provider (OP)
The Janssen Authentication Server is a [fully certified OpenID Provider (OP)](http://openid.net/certification/) that supports the following OpenID Connect specifications: 

- Core [(spec)](http://openid.net/specs/openid-connect-core-1_0.html)
- Dynamic Client Registration [(spec)](https://openid.net/specs/openid-connect-registration-1_0.html)
- Discovery [(spec)](https://openid.net/specs/openid-connect-discovery-1_0.html)
- Form Post Response Mode [(spec)](https://openid.net/specs/oauth-v2-form-post-response-mode-1_0.html)
- Session Management [(spec)](http://openid.net/specs/openid-connect-session-1_0.html)
- Front Channel Logout [(draft)](http://openid.net/specs/openid-connect-frontchannel-1_0.html)

## Protocol Overview
OpenID Connect is an identity layer that profiles OAuth 2.0 to define a sign-in flow for applications (clients) to authenticate a person and obtain authorization to gather information (or "claims") about that person. For more information, see [OpenID Connect](http://openid.net/connect)

It's handy to know some OpenID Connect terminology:

- The *end user* or *subject* is the person being authenticated.

- The *OpenID Provider (OP)* is the equivalent of a SAML Identity Provider (IDP). It holds end user credentials (like a username/ password) and personally identifiable information. During a single sign-on (SSO) login flow, end users are redirected to the OP for authentication. 

- The *Relying Party* or  *RP*  or *client* is software, like a mobile application or website, which needs to authenticate the subject. The RP is an OAuth client.
