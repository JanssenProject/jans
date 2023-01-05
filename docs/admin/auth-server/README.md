---
tags:
  - administration
  - auth-server
---

# Overview

Auth Server provides the federated identity functionality of the Janssen Platform.
The server is a fork of [oxAuth](https://github.com/GluuFederation/oxAuth/),
the engine of Gluu Server 4. The design goal of Auth Server was speed,
scalability and flexibility for large scale enterprise deployments. It is based
on the Java [Weld](https://weld.cdi-spec.org/) framework, a stable platform that
provides many features out of the box.

Auth Server is a fairly comprehensive implementation of OpenID Connect, which
itself is built on top of OAuth 2.0. See the latest [OpenID certifications](https://openid.net/certification/) for OpenID Providers, FAPI OpenID Providers, and
FAPI-CIBA OpenID Providers for the latest results.

## Supported Standards

**OpenID**

* [OpenID Connect Core 1.0](https://openid.net/specs/openid-connect-core-1_0.html)
* [OpenID Connect Discovery 1.0](https://openid.net/specs/openid-connect-discovery-1_0.html)
* [OpenID Connect Dynamic Registration](https://openid.net/specs/openid-connect-registration-1_0.html)
* [OpenID Connect Client-Initiated Backchannel Authentication Flow - Core 1.0](https://openid.net/specs/openid-client-initiated-backchannel-authentication-core-1_0.html)
* [Financial-grade API Security Profile 1.0 - Part 1: Baseline](https://openid.net/specs/openid-financial-api-part-1-1_0.html)
* [Financial-grade API Security Profile 1.0 - Part 2: Advanced](https://openid.net/specs/openid-financial-api-part-2-1_0.html)
* [OpenIDConnect Front-Channel Logout 1.0](https://openid.net/specs/openid-connect-frontchannel-1_0.html)
* [OpenID Connect Back-Channel Logout 1.0](https://openid.net/specs/openid-connect-backchannel-1_0.html)
* [OpenID Connect Session Management](https://openid.net/specs/openid-connect-session-1_0.html)
* [Draft - JWT Secured Authorization Response Mode for OAuth 2.0 (JARM)](https://bitbucket.org/openid/fapi/src/master/oauth-v2-jarm.md)
* [Draft - Financial-grade API: Client Initiated Backchannel Authentication Profile](https://bitbucket.org/openid/fapi/src/master/Financial_API_WD_CIBA.md)
* [Draft -  OpenID Connect Native SSO for Mobile Apps 1.0](https://openid.net/specs/openid-connect-native-sso-1_0.html#name-authorization-request)

** OAuth **

* [RFC 6749 The OAuth 2.0 Authorization Framework](https://www.rfc-editor.org/rfc/rfc6749.html)
* [OAuth 2.0 Multiple Response Type Encoding Practices](https://openid.net/specs/oauth-v2-multiple-response-types-1_0.html)
* [OAuth 2.0 Form Post Response Mode](https://openid.net/specs/oauth-v2-form-post-response-mode-1_0.html)
* [RFC 6750 The OAuth 2.0 Authorization Framework: Bearer Token Usage](https://www.rfc-editor.org/rfc/rfc6750.html)
* [RFC 7009 OAuth 2.0 Token Revocation](https://www.rfc-editor.org/rfc/rfc7009.html)
* [RFC 7519 JSON Web Token (JWT)](https://www.rfc-editor.org/rfc/rfc7519.html)
* [RFC 7591 OAuth 2.0 Dynamic Client Registration Protocol](https://www.rfc-editor.org/rfc/rfc7591.html)
* [RFC 7592 OAuth 2.0 Dynamic Client Registration Management Protocol](https://www.rfc-editor.org/rfc/rfc7592.html)
* [RFC 7636 Proof Key for Code Exchange by OAuth Public Clients](https://www.rfc-editor.org/rfc/rfc7636.html)
* [RFC 7662 OAuth 2.0 Token Introspection](https://www.rfc-editor.org/rfc/rfc7662.html)
* [RFC 8252 OAuth 2.0 for Native Apps](https://www.rfc-editor.org/rfc/rfc8252.html)
* [RFC 8414 OAuth 2.0 Authorization Server Metadata](https://www.rfc-editor.org/rfc/rfc8414.html)
* [RFC 8628 OAuth 2.0 Device Authorization Grant](https://www.rfc-editor.org/rfc/rfc8628.html)
* [RFC 8693 OAuth 2.0 Token Exchange](https://www.rfc-editor.org/rfc/rfc8693.html)
* [RFC 8705 OAuth 2.0 Mutual-TLS Client Authentication and Certificate-Bound Access Tokens](https://www.rfc-editor.org/rfc/rfc8705.html)
* [RFC 9068 JSON Web Token (JWT) Profile for OAuth 2.0 Access Tokens](https://www.rfc-editor.org/rfc/rfc9068.html)
* [RFC 9126 OAuth 2.0 Pushed Authorization Requests](https://www.rfc-editor.org/rfc/rfc9126.html)
* [Draft - OAuth 2.0 Demonstrating Proof-of-Possession at the Application Layer (DPoP)](https://www.ietf.org/archive/id/draft-ietf-oauth-dpop-11.html)
* [Draft - JWT Response for OAuth Token Introspection](https://www.ietf.org/archive/id/draft-ietf-oauth-jwt-introspection-response-12.html)

** User Managed Access (UMA) **

* [Federated Authorization for User-Managed Access (UMA) 2.0](https://docs.kantarainitiative.org/uma/wg/rec-oauth-uma-federated-authz-2.0.html)
* [User-Managed Access (UMA) 2.0 Grant for OAuth 2.0 Authorization](https://docs.kantarainitiative.org/uma/wg/rec-oauth-uma-grant-2.0.htmlFederated Authorization for User-Managed Access (UMA) 2.0)

For the above specifications, Auth Server implements many features--all of the
MUST's, but also many of the SHOULD's and MAY's. If you find any discrepancies,
please raise an [issue](https://github.com/JanssenProject/jans/issues).
