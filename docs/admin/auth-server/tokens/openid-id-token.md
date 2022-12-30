---
tags:
  - administration
  - auth-server
  - token
---

## OpenID Connect ID Token

Defined in [Section 2](https://openid.net/specs/openid-connect-core-1_0.html#IDToken)
of `OpenID Connect Core 1.0`:

> The primary extension that OpenID Connect makes to OAuth 2.0 to enable End-Users to be Authenticated is the ID Token data structure. The ID Token is a security token that contains Claims about the Authentication of an End-User by an Authorization Server when using a Client, and potentially other requested Claims. The ID Token is represented as a JSON Web Token (JWT) [JWT].

The ID Token is similar to a
[SAML 2.0](http://docs.oasis-open.org/security/saml/Post2.0/sstc-saml-tech-overview-2.0.html)
Assertion, which must contain an Authentication statement, and may optionally
contain an Attribute statement, defined in the spec as:

> - Authentication statements: These are created by the party that successfully authenticated a user. At a minimum, they describe the particular means used to authenticate the user and the specific time at which the authentication took place.

> - Attribute statements: These contain specific identifying attributes about the subject (for example, that user “John Doe” has “Gold” card status).

The ID Token is a standard JWT, and can use signing and encryption depending on
the configuration preferences of the client. A sample payload for a Jans Auth
Server `id_token` is below:

```
{
  "iss": "https://idp.example.tld",                 <-- OP hostname
  "aud": "bd0469f7-f80a-4595-bd52-df9826f0a2f4",    <-- client_id  
  "sub": "0a9ca356-6130-4032-91c6-a2dfe4e19804",    <-- in this case, a pairwise identifer
  "iat": 1672373703,                                <-- issued at
  "auth_time": 1672373700,                          <-- when user authenticated
  "acr": "basic",                                   <-- how user authenticated
  "amr": ["10"],                                    <-- Extra authn details
  "nonce": "1u0y3ii",                               <-- client should verify
  "sid": "5f01565c-f2dc-4b4b-af8a-ab1578a5dbe3",    <-- session id handle
  "jansOpenIDConnectVersion": "openidconnect-1.0"   <-- OpenID version
}
```

A great tool if you want to decode a JWT is Auth0's [https://jwt.io/](https://jwt.io/).

Notice that a basic ID Token, like the one above, contains details about the
authentication event, not user claims. You can configure the client to "include
claims in id_token", or you can set a Auth Server configuration property,
`legacyIdTokenClaims` to `True` to set the behavior for all clients.

## Obtaining an ID Token

The client may obtain an ID Token in the authorization response, from the
token endpoint, or both. Note, if you intend to use the Code Flow, and you
don't intend to validate the `c_hash` or `s_hash` values in the ID Token
returned from the authorization endpoint, don't check `id_token` in the
response_type for the authorization endpoint, as it will waste compute and
storage generating a token you don't need.

## Validating an ID Token

At a minimum, client developers should always validate the following:
- Signature of the ID token
- `iss` make sure the ID Token was issued by the OP you trust
- `aud` make sure the ID Token was issued to your `client_id`
- `exp` make sure the token is not expired
- `nonce` make sure the id_token correlates to your original authn request

## Using the ID Token as an access token for API's

Don't do this. The ID Token is an identity assertion, not an access token. It
may be passed in the payload to an API, but it should not be used in lieu of
an OAuth access token! Fundamentally, the `id_token` details how a person
was authenticated, not which API's a client is authorized to call.
