---
tags:
  - administration
  - auth-server
  - oauth
  - feature
---

# OAuth 2.1 and OAuth 2.0

The [Janssen Authentication Server](https://github.com/JanssenProject/jans/tree/main/jans-auth-server) follows OAuth 2.1 by default, along with related specifications and extensions. Out of the box:

- The Authorization Code Grant requires Proof Key for Code Exchange (PKCE) with the `S256` challenge method. The `plain` method is not accepted.
- The Implicit Grant and the Resource Owner Password Credentials (ROPC) Grant are not in the default supported grant types.
- The UserInfo endpoint does not accept access tokens in the URL query.
- Public clients receive a new refresh token on every refresh.
- Authorization responses include the `iss` parameter.

For backward compatibility, Janssen still supports OAuth 2.0 features that OAuth 2.1 removes or discourages. See [Enabling Legacy OAuth 2.0 Features](#enabling-legacy-oauth-20-features).

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

OAuth 2.0 is the industry-standard framework for authorization. OAuth 2.1 builds on
OAuth 2.0 with updated security recommendations and safer defaults for authorization
flows across different types of applications.

For new applications, use the Authorization Code Grant with PKCE. OAuth 2.0 grants removed by OAuth 2.1 do remain available in Janssen when explicitly enabled.

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

## Enabling Legacy OAuth 2.0 Features

Janssen supports OAuth 2.0 features that OAuth 2.1 removes or discourages, so that existing
applications keep working. Each feature has its own setting; there is no single switch.
These options are not recommended for new integrations.

| Legacy feature | Setting |
| --- | --- |
| [Implicit Grant](./implicit-grant.md) (`response_type=token`) | Add `implicit` to [`grantTypesSupported`](../../reference/json/properties/janssenauthserver-properties.md#granttypessupported) and to the client's grant types |
| OpenID Connect `id_token` and hybrid response types, such as `code id_token` | Same as the Implicit Grant, or set [`allowIdTokenWithoutImplicitGrantType`](../../reference/json/properties/janssenauthserver-properties.md#allowidtokenwithoutimplicitgranttype) to `true` to allow `id_token` without the `implicit` grant |
| [Password Grant](./password-grant.md) (Resource Owner Password Credentials) | Add `password` to [`grantTypesSupported`](../../reference/json/properties/janssenauthserver-properties.md#granttypessupported) and to the client's grant types |
| Authorization Code without PKCE | Set [`requirePkce`](../../reference/json/properties/janssenauthserver-properties.md#requirepkce) to `false`. PKCE is still required for clients that have `requirePkce` enabled in their own attributes |
| Authorization responses without the `iss` parameter | Set [`authorizationResponseIssParameterSupported`](../../reference/json/properties/janssenauthserver-properties.md#authorizationresponseissparametersupported) to `false`. See [Issuer Identification](./authorization-server-issuer-identification.md) |

Server properties are set using
[Janssen Text-based UI(TUI)](../../config-guide/config-tools/jans-tui/README.md) via
`Auth Server`->`Properties`. A client's grant types are set via
`Auth Server`->`Clients`->select the client->`Basic`->`Grant`.

For clients registered using dynamic client registration, the requested grant types must also be in
[`grantTypesSupportedByDynamicRegistration`](../../reference/json/properties/janssenauthserver-properties.md#granttypessupportedbydynamicregistration).
The default value already includes `implicit` and `password`.

The following OAuth 2.0 behaviors cannot be re-enabled:

- The PKCE `plain` code challenge method. Only `S256` is accepted.
- Keeping the same refresh token for public clients. A public client always receives a new
  refresh token on refresh, even when `skipRefreshTokenDuringRefreshing` is `true`.

## Have questions in the meantime?

You can ask questions through [GitHub Discussions](https://github.com/JanssenProject/jans/discussions) or the [community chat on Zulip](https://chat.gluu.org/join/wnsm743ho6byd57r4he2yihn/). Any questions you have will help determine what information our documentation should cover.

## Want to contribute?

If you have content you'd like to contribute to this page, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).
