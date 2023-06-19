---
tags:
  - administration
  - auth-server
  - oauth
  - feature
  - dpop
---

# DPoP (Demonstrating Proof-of-Possession at the Application Layer)

Janssen Server supports DPoP, Demonstrating Proof-of-Possession at the Application Layer, which is OAuth 2.0 feature
to enhance security of resources protected by access token.

When DPoP is being used, the Janssen Server checks whether the presenter of the access token is the one to whom the 
access token was actually issued. Hence, making sure that a stolen access token is not being used by someone
else to access the protected resource. OAuth 2.0 DPoP specification is available 
[here](https://www.ietf.org/archive/id/draft-ietf-oauth-dpop-16.html)

Janssen Server also supports OAuth [MTLS(Mutual TLS)](./mtls.md) as mechanism to ensure that the token presenting 
party is legitimate. While MTLS should be preferred whenever it is possible to use it, for other cases like single
page application(SPA), DPoP can be used.

## Using DPoP 

## DCR and client registration

Janssen Server client configuration does not need to enable anything specifically on Janssen Server to use DPoP.

TODO: When using dynamic client registration, do we support this `dpop_bound_access_tokens`? 

## Using DPoP Proof JWT

In order to use DPoP protection, client needs to create DPoP Proof JWT (or DPoP Proof) and send it using `DPoP`
request header to Janssen Server when 

1. Requesting for a new access token 
2. Accessing a protected resource using the access token

DPoP proofs are created differently for cases listed above. DPoP specification describes 
[how](https://www.ietf.org/archive/id/draft-ietf-oauth-dpop-16.html#name-dpop-proof-jwts).

When an access token is requested with DPoP header (1 above), the Janssen Server returns an access token (or refresh token) that
is bound to the public key attached with the DPoP proof. 

When client attaches DPoP proof along with the access token to access the protected resource (2 above), the RP checks
the validity of the request using steps laid out in the 
[specification](https://www.ietf.org/archive/id/draft-ietf-oauth-dpop-16.html#name-checking-dpop-proofs).

### Using Introspection Endpoint 



Do we support 

## Janssen Server Configuration for DPoP

Following properties of Janssen Server can be used to tailor the behavior with respect to DPoP.

- [dpopJtiCacheTime](https://docs.jans.io/head/admin/reference/json/properties/janssenauthserver-properties/#dpopjticachetime)
- [dpopSigningAlgValuesSupported](https://docs.jans.io/head/admin/reference/json/properties/janssenauthserver-properties/#dpopsigningalgvaluessupported)
- [dpopTimeframe](https://docs.jans.io/head/admin/reference/json/properties/janssenauthserver-properties/#dpoptimeframe)




## Have questions in the meantime?

While this documentation is in progress, you can ask questions through [GitHub Discussions](https://github.com/JanssenProject/jans/discussion) or the [community chat on Gitter](https://gitter.im/JanssenProject/Lobby). Any questions you have will help determine what information our documentation should cover.

## Want to contribute?

If you have content you'd 
like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).