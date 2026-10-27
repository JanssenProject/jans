---
tags:
  - administration
  - auth-server
  - oauth
  - feature
---

# OAuth 2.0 and OAuth 2.1

The [Janssen Authentication Server](https://github.com/JanssenProject/jans/tree/main/jans-auth-server) supports OAuth 2.0 and OAuth 2.1-aligned authorization flows, along with related specifications and extensions.

Janssen uses OAuth 2.1 security practices by default, including requiring Proof Key for Code Exchange (PKCE) with the `S256` challenge method for the Authorization Code Grant. The Implicit Grant and Resource Owner Password Credentials (ROPC) Grant are disabled by default, as these grant types are not included in OAuth 2.1.

For backward compatibility, Janssen retains configuration options for legacy OAuth 2.0 functionality. Administrators can configure supported grant types and security settings according to their application requirements.

The Janssen Authentication Server supports the following OAuth specifications and related extensions:

- The OAuth 2.1 Authorization Framework [(spec draft)](https://datatracker.ietf.org/doc/draft-ietf-oauth-v2-1/)
- The OAuth 2.0 Authorization Framework [(spec)](https://datatracker.ietf.org/doc/html/rfc6749)
- The OAuth 2.0 Authorization Framework: Bearer Token Usage [(spec)](https://datatracker.ietf.org/doc/html/rfc6750)
- JSON Web Token (JWT) Profile for OAuth 2.0 Access Tokens [(spec)](https://datatracker.ietf.org/doc/html/rfc9068)
- JSON Web Token (JWT) [(spec)](https://datatracker.ietf.org/doc/html/rfc7519)
- OAuth 2.0 Token Introspection [(spec)](https://datatracker.ietf.org/doc/html/rfc7662)
- OAuth 2.0 Device Authorization Grant [(spec)](https://datatracker.ietf.org/doc/html/rfc8628)
- OAuth 2.0 Token Revocation [(spec)](https://datatracker.ietf.org/doc/html/rfc7009)
- Proof Key for Code Exchange by OAuth Public Clients (PKCE) [(spec)](https://datatracker.ietf.org/doc/html/rfc7636)
- OAuth 2.0 for Native Apps [(spec)](https://datatracker.ietf.org/doc/html/rfc8252)
- OAuth 2.0 Token Exchange [(spec)](https://datatracker.ietf.org/doc/html/rfc8693)
- OAuth 2.0 Authorization Server Metadata [(spec)](https://datatracker.ietf.org/doc/html/rfc8414)
- OAuth 2.0 Dynamic Client Registration Protocol [(spec)](https://datatracker.ietf.org/doc/html/rfc7591)
- OAuth 2.0 Pushed Authorization Requests [(spec)](https://datatracker.ietf.org/doc/html/rfc9126)
- OAuth 2.0 Demonstrating Proof-of-Possession (DPoP) [(spec)](https://datatracker.ietf.org/doc/html/rfc9449)
- OAuth 2.0 Mutual-TLS Client Authentication and Certificate-Bound Access Tokens [(spec)](https://datatracker.ietf.org/doc/html/rfc8705)
- Assertion Framework for OAuth 2.0 Client Authentication and Authorization Grants [(spec)](https://www.rfc-editor.org/rfc/rfc7521.html)
- JWT Secured Authorization Response Mode for OAuth 2.0 (JARM) [(spec)](https://openid.net/specs/oauth-v2-jarm.html)
- OAuth 2.0 for First-Party Applications [(spec draft)](https://www.ietf.org/archive/id/draft-parecki-oauth-first-party-apps-02.html)
- The Use of Attestation in OAuth 2.0 Dynamic Client Registration [(spec draft)](https://www.ietf.org/id/draft-tschofenig-oauth-attested-dclient-reg-00.html)
- OpenID Connect Core Error Code unmet_authentication_requirements [(spec)](https://openid.net/specs/openid-connect-unmet-authentication-requirements-1_0.html)
- Transaction Tokens [(spec)](https://drafts.oauth.net/oauth-transaction-tokens/draft-ietf-oauth-transaction-tokens.html)
- Global Token Revocation [(spec)](https://www.ietf.org/archive/id/draft-parecki-oauth-global-token-revocation-03.html)
- JSON Web Token (JWT) Profile for OAuth 2.0 Client Authentication and Authorization Grants [(spec)](https://datatracker.ietf.org/doc/html/rfc7523)
- OAuth 2.0 Client ID Metadata Document [(spec draft)](https://www.ietf.org/archive/id/draft-ietf-oauth-client-id-metadata-document-01.html)
- OAuth 2.0 Authorization Server Issuer Identification [(spec)](https://datatracker.ietf.org/doc/html/rfc9207) [(doc)](./authorization-server-issuer-identification.md)
- OpenID Connect Ephemeral Subject Identifier 1.0 [(spec)](https://openid.net/specs/openid-connect-ephemeral-subject-identifier-1_0.html) [(doc)](../openid-features/subject-identifiers.md#ephemeral-subject-identifiers)

## Protocol Overview

OAuth 2.0 is an authorization framework that enables applications to obtain limited access to protected resources. OAuth 2.1 builds on OAuth 2.0 by incorporating updated security requirements and recommended practices for securing authorization flows.

The Janssen Authentication Server supports OAuth 2.1-aligned authorization flows while retaining configurable legacy OAuth 2.0 functionality for backward compatibility. For new applications, the Authorization Code Grant with Proof Key for Code Exchange (PKCE) is recommended. Legacy grant types, such as the Implicit Grant and Resource Owner Password Credentials Grant, are disabled by default.

OAuth defines the following four primary roles:

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

You can ask questions through [GitHub Discussions](https://github.com/JanssenProject/jans/discussions) or the [community chat on Zulip](https://chat.gluu.org/join/wnsm743ho6byd57r4he2yihn/). Any questions you have will help determine what information our documentation should cover.

## Want to contribute?

If you have content you'd like to contribute to this page, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).
