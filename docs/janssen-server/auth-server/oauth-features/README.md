---
tags:
  - administration
  - auth-server
  - oauth
  - feature
---

# OpenID Connect Provider (OP)
The [Janssen Authentication Server](https://github.com/JanssenProject/jans/tree/main/jans-auth-server) has core OAuth 2.0 support as well as many other related specs: 

- The OAuth 2.0 Authorization Framework [(spec)](https://datatracker.ietf.org/doc/html/rfc6749)
- The OAuth 2.0 Authorization Framework: Bearer Token Usage [(spec)](https://datatracker.ietf.org/doc/html/rfc6750)
- JSON Web Token (JWT) Profile for OAuth 2.0 Access Tokens [(spec)](https://datatracker.ietf.org/doc/html/rfc9068)
- JSON Web Token (JWT) [(spec)](https://datatracker.ietf.org/doc/html/rfc7519)
- OAuth 2.0 Token Introspection [(spec)](https://datatracker.ietf.org/doc/html/rfc7662)
- OAuth 2.0 Device Authorization Grant [(spec)](https://datatracker.ietf.org/doc/html/rfc8628)
- OAuth 2.0 Token Revocation [(spec)](https://datatracker.ietf.org/doc/html/rfc7009)
- Proof Key for Code Exchange by OAuth Public Clients (PKCE) [(spec)](https://datatracker.ietf.org/doc/html/rfc7636)
- OAuth 2.0 for Native Apps [(spec)](https://datatracker.ietf.org/doc/html/rfc8252)
- OAuth 2.0 Token Exchange [(spec)](https://datatracker.ietf.org/doc/html/rfc8252)
- OAuth 2.0 Authorization Server Metadata [(spec)](https://datatracker.ietf.org/doc/html/rfc8414)
- OAuth 2.0 Dynamic Client Registration Protocol [(spec)](https://datatracker.ietf.org/doc/html/rfc7591)
- OAuth 2.0 Pushed Authorization Requests [(spec)](https://datatracker.ietf.org/doc/html/rfc9126)
- OAuth 2.0 Demonstrating Proof-of-Possession at the Application Layer (DPoP) [(spec)](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-dpop)
- OAuth 2.0 Mutual-TLS Client Authentication and Certificate-Bound Access Tokens [(spec)](https://datatracker.ietf.org/doc/html/rfc8705)
- Assertion Framework for OAuth 2.0 Client Authentication and Authorization Grants [(spec)](https://www.rfc-editor.org/rfc/rfc7521.html)
- JWT Secured Authorization Response Mode for OAuth 2.0 (JARM) [(spec)](https://openid.net/specs/oauth-v2-jarm.html)
- OAuth 2.0 for First-Party Applications [(spec draft)](https://www.ietf.org/archive/id/draft-parecki-oauth-first-party-apps-02.html)
- The Use of Attestation in OAuth 2.0 Dynamic Client Registration [(spec draft)](https://www.ietf.org/id/draft-tschofenig-oauth-attested-dclient-reg-00.html)
- OpenID Connect Core Error Code unmet_authentication_requirements [(spec)](https://openid.net/specs/openid-connect-unmet-authentication-requirements-1_0.html)
- Transaction Tokens [(spec)](https://drafts.oauth.net/oauth-transaction-tokens/draft-ietf-oauth-transaction-tokens.html)
- Global Token Revocation [(spec)](https://www.ietf.org/archive/id/draft-parecki-oauth-global-token-revocation-03.html)


## Protocol Overview

OAuth 2.0 is the industry-standard protocol for authorization. OAuth 2.0 focuses on client developer simplicity providing specific authorization flows for different applications (e.g. web applications, desktop applications, mobile phones, and other devices). 

It's handy to know some OAuth 2.0 terminology:

- **resource owner** - An entity capable of granting access to a protected resource.
                       When the resource owner is a person, it is referred to as an end-user.
  
- **resource server** - The server hosting the protected resources, capable of accepting
                        and responding to protected resource requests using access tokens.
  
- **client** - An application making protected resource requests on behalf of the
               resource owner and with its authorization.  The term "client" does
               not imply any particular implementation characteristics (e.g.,
               whether the application executes on a server, a desktop, or other devices).
  
- **authorization server** - The server issuing access tokens to the client after successfully
                             authenticating the resource owner and obtaining authorization.

## Have questions in the meantime?

You can ask questions through [GitHub Discussions](https://github.com/JanssenProject/jans/discussions) or the [community chat on Gitter](https://gitter.im/JanssenProject/Lobby). Any questions you have will help determine what information our documentation should cover.

## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).