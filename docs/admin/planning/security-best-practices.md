---
tags:
  - security recommendations
---

Janssen Project distributions are designed to be easy to deploy. Its default
security settings may not be strict enough for certain organizations or use
cases. This document highlights important security controls and offers best
practices for increasing security related to your deployment.

## Scripts

Make sure only required scripts are enabled. It's extremely important to disable
any person authentication or other scripts not in use.

## Keys

Use RSA keys with a minimum strength of 2048 bits and Elliptic Curve keys with a
minimum of 160 bits. Also, check the key rotation policy of your Auth Server.
This differs based on your deployment model. A single server can handle key
rotation locally, but if you have deployed a cluster, you must manage key
rotation centrally.

## Admin Web Services

Make sure SCIM and the config API are not Internet facing. If you do expose
SCIM, you should network restrict access as much as possible. Although SCIM
is protected via OAuth or UMA access tokens, because a breach of SCIM would
undermine the integrity of authentications, a multi-layer security approach
is warranted. There is no use case for an Internet facing Config API--you should
tightly control access to this service.

Make sure no services are listening on external interfaces except for those that
are absolutely required--`tcp/443` for the OpenID and FIDO endpoints.

On Linux servers, a list of current listeners can be obtained with `netstat -nlpt`
(for TCP) and `netstat -nlpu` (for UDP). In particular, make sure the internal
databases used by Gluu to store all its configuration are not Internet facing.

## OpenID

Review the [OAuth 2.0 Security Best Current Practice](OAuth 2.0 Security Best Current Practice) IETF draft.

Don't use the implicit flow. In the implicit flow, the client is not
authenticated, and a token is returned from the Authorization endpoint. If the
`response_type` in your OpenID Authentication contains `token`, you are using
the implicit flow. Section
[2.1.2](https://www.ietf.org/archive/id/draft-ietf-oauth-security-topics-21.html#section-2.1.2)
of the current OAuth Security Best Practices OAuth working group draft
specifically warns against using the implicit flow. If you must use the implicit
flow, Review the CORS filter configuration for Jans Auth Server. CORS restricts
access to trusted domains to execute browser application requests to Auth Server
endpoints. By default, the filter allows any RP to call OpenID endpoints.

Review Auth Server propoerties chosen for `sessionIdUnusedLifetime` and
`sessionIdLifetime`. Long sessions present higher risks of session hijacking and
unauthorized access from shared devices.

### OAuth Client Security

The table below summarizes the default values for dynamically registered
clients.

| Attribute      | Default Value |
| -------------   |:-------------:|
| applicationType | web |
| subjectType | public|
|idTokenSignedResponseAlg | RS512 |
| userInfoSignedResponseAlg | RS512 |
| userInfoEncryptedResponseAlg | RSA-OAEP |
| userInfoEncryptedResponseEnc | RSA-OAEP |
| accessTokenLifetime | 3600 |

Consider whether support of OpenID Connect Dynamic Client Registration extension
is required. If not, disable it by setting `dynamicRegistrationEnabled` to
`False`. If Dynamic Client Registration is enabled, consider using
[software statements](https://www.rfc-editor.org/rfc/rfc7591.html#section-3.1),
and writing a Client Registration interception script to implement extra rules
for software statement validation.

Check the list of scopes to quickly assess which scopes the clients can
potentially add to their registration entry without consent of OP's administrator.
Make sure sensitive scopes are `dynamicRegistrationScopesParamEnabled` set to
False.

The Auth Server property `dynamicRegistrationScopesParamEnabled` controls
whether default scopes are globally enabled; scopes defined as default
will be automatically added to any dynamically registered client entry.

For client authentication at the token endpoint, move away from shared secrets--
use either private key authentication or [MTLS](https://datatracker.ietf.org/doc/html/rfc8705). A client secret is basically a password for your software application. Utilize
asymmetric client authentication.

Make sure your OpenID client applications check the state value, and verify that
it is the same state posted in the original request. This is necessary to
prevent Cross Site Request Forgery (CSRF). Also make sure that clients use a
non-static value for state (i.e. a one time non-guessable value). You can also
verify the state in the `s_hash` claim of the id_token in the authentication
response.

This may sound obvious, but make sure your clients verify the signature of the
id_token. The location of the current public keys of the OpenID Provider can be
found in the configuration endpoint (`jwks_uri`). It is also recommended that
clients implement a unique `redirect_uri` per OP.

This may also sound obvious, but make sure you trust the OpenID Connect client
software library that your developers are using to authenticate users. Also
make sure OpenID Connect client libraries are updated expeditiously.

Suggest to web developers that they read the [OpenID Connect Basic Client
Implementer's Guide](https://openid.net/specs/openid-connect-basic-1_0.html),
which provides a more reader-friendly narrative for how to use the OpenID
code flow--the main flow for relying parties.

Client developers should consider using a Request Object JWT, which prevents
hackers from tampering with the request parameters. Request objects can mitigate
the Malicious Endpoint Attack and the IDP confusion attack.

Mobile developers should read [RFC 8252 OAuth 2.0 for Native Apps](https://www.rfc-editor.org/rfc/rfc8252.html) and follow the recommendations there. It's really important not to
store any client secret or private key in the application, because hackers
can decompile applications to extract these secrets. That's why RFC 8252
recommends using [PKCE](https://datatracker.ietf.org/doc/html/rfc7636)
to prevent Authorization Code Interception Attack, and use a SHA256 as the
code challenge method. Use App Auth if possible for
[Android](https://github.com/openid/AppAuth-Android),
[iOS](https://github.com/openid/AppAuth-iOS),
or [JavaScript](https://github.com/openid/AppAuth-JS).

If additional client security is needed, consider using the
[FAPI](https://openid.net/wg/fapi/) profile of OpenID
Connect, which adds additional signing, encryption and security
mitigations.

## UMA

The UMA protocol enables post-authentication authorization flows. Consider
the following system-level properties:

* `umaGrantAccessIfNoPolicies` allows access to a resource even if no policies
are defined for the related scopes; though it simplifies initial testing, we
recommend disabling this feature in production setups

* `umaRestrictResourceToAssociatedClient` won't allow any other client except
the one which registered the resource initially to acquire a RPT for it; it's
recommended to have it enabled for production setups
