---
tags:
  - architecture
  - use-cases
  - planning
---

## Browser Single Sign-On (B-SS0)

Web browser SSO is one of the main use cases for Janssen. The OpenID Connect
interfaces of the Auth Server provide the identity layer that manages
authentication and B-SSO. Auth Server tracks web browser sessions via a cookie.
An end user may actually have several active sessions, although only one session is
active. For example, below is a hypothetical session cookie:

```
session_id: c691e83d-eb1b-41f0-b453-fab905681b5b
current_sessions: ["de510ab6-b06c-4393-86d8-12a7c501aafe", "c691e83d-eb1b-41f0-b453-fab905681b5b"]
```

To switch sessions, an RP can redirect to the Jans Auth Server OpenID Connect Provider authorization endpoint with the parameters `prompt=select_account`.

## Native Single Sign-On (N-SS0)

On most mobile platforms, mobile apps signed by the same vendor certs can share information via the system "keychain" (Account Manager on Android). [OpenID Mobile SSO](https://openid.net/specs/openid-connect-native-sso-1_0.html) specifies a new scope, extends the token endpoint and profiles the [OAuth2 Token Exchange](https://www.rfc-editor.org/rfc/rfc8693.html) spec allowing mobile apps to share identity between apps produced and signed by the same vendor (i.e. signed with the same vendor certificate).

## Single Logout (SLO)

The bane of identity engineers everywhere, Auth Server does offer some endpoints
that enable SLO. Janssen Auth Server implements
[Back-Channel Logout](https://openid.net/specs/openid-connect-backchannel-1_0.html)
and
[Session Management](https://openid.net/specs/openid-connect-session-1_0.html).
But the most commonly used strategy is [Front-Channel Logout](https://openid.net/specs/openid-connect-frontchannel-1_0.html).
This works by displaying a web page with
numerous iFrames, each with the respective logout URL for each application.
The reason SLO is so hard is that logout is
inherently an asynchronous use case. When you need a bunch of websites to
receive a notification, some of those notifications may fail. Ideally, you'd
replay messages that aren't received. But end users want a fast logout response,
and don't want to wait for all the applications to confirm they received and
acted on the notification. So making logout synchronous is a round-hole /
square peg situation. But with that said, Auth Server supports it.

## API Access Management

Janssen Auth Server is frequently used to issue access tokens to authenticate
software clients that needs to call an API. The [OAuth WG](https://datatracker.ietf.org/group/oauth/documents/) has published many useful
specifications about how to do this securely, and Janssen implements [many](.) of
these RFCs and drafts. In OAuth parlance, Jans Auth Server is the Authorization
Server; and Jans Client API is a service that helps clients with advanced
features like MTLS and private key authentication. As OAuth scopes are frequently
used to control the extent of access of clients, many domains are using Jans
Auth Server to dynamically render scopes based on the identity of the client,
or the security context. The Jans Auth Server also implements
[OAuth](https://www.rfc-editor.org/rfc/rfc7591.html) and
[OpenID](https://openid.net/specs/openid-connect-registration-1_0.html)
client registration API. Finally, Jans Auth Server publishes an SSA endpoint,
for [OAuth software statement assertions](https://www.rfc-editor.org/rfc/rfc7591.html#section-2.3)
which enables trusted services to request an SSA that specifies what type of
clients can register. All of these features mean that Jans Auth Server is an
essential tool as part of your API access management infrastructure.

## Multifactor Authentication

OpenID Connect is a web identity layer. Auth Server displays and processes a
series of web pages to authenticate a person, and then shares user claims
with the applications that requested them. Out of the box, Auth Server supports
many MFA workflows, including FIDO, passkeys, OATH OTP (HOTP, TOTP), SMS (via
Twilio or SMPP) and many more. But you're not limited to the MFA that comes
out of the box. Using Person Authentication Scripts, you can implement any
authentication workflow which you can imagine. You can also use the Janssen
Project's Agama programming language to implement multi-step authenticaiton
workflows.

## Mobile Authentication

One of the initial requirements for OpenID was to support mobile apps (because
SAML is terrible for mobile apps). OpenID and OAuth have developed several
mitigations that make mobile authentication more secure, which are desribed
in [RFC 8252](https://datatracker.ietf.org/doc/rfc8252/). However, due to
the complexity of cookie sharing between the mobile phone browser SDK, and
the mobile browser (assuming you are using the system browser), your results
may vary if you actually want SSO.  Recently, OpenID has published a new
specification draft for [Native SSO for Mobile Apps](https://openid.net/specs/openid-connect-native-sso-1_0.html),
which is on the [roadmap](https://github.com/JanssenProject/jans/issues/2518).
There are some other hacky ways you can implement mobile authentication, for
example, using the OAuth `password` grant--but this is possible only for first
party applications, and is generally discouraged, because there is no way to
securely store a client secret in the mobile application, which hackers can
easily decompile.

## Open Banking

You'll notice that the [Gluu Open Banking Identity Platform](https://openid.net/certification/#FAPI_OPs)
is certified for Financial-grade API (FAPI) 1.0 Final--this is Gluu's distribution
of Janssen Auth Server profiled for banking. If you want to implement an OP for
open banking, Jans Auth Server is one of the few comprehensive open source
platforms that will enable you to do so.

## Social Sign-in

This could really be covered under the MFA section. But it's not uncommon that
as part of an authentication workflow, Auth Server may redirect you to an
external identity provider, which then sends you back to Auth Server with
some kind of reference id or assertion. Out of the box, Jans supports the top
three social sites: Google, Apple, Facebook (which account for 80% of social
login). But of course you can implement any social login using an Agama project.

## Registration

It's not that uncommon that an authentication workflow is used to create an
account for a person in an identity provider. In fact, it's so common, that
the OpenID working group recently created a spec to standardize a parameter
in the authentication request to signal to the OpenID Provider that this is
what's happening: [prompt=create](https://github.com/JanssenProject/jans/issues/2616).
As we're just displaying a series of web pages, we can display a registration
form, process the form (with some kind of remote identity proofing?), send email,
SMS, Telegram messages or whatever. Also, frequently social login is tied to
registration--the first time you login with an external IDP, Auth Server creates
an account of the fly with the user claims sent by the remote IDP.

## Authorization

Frequently people conflate the identity layer with the authorization layer. RBAC
is still an important security paradigm. So it makes sense to people that the
place where you store information about a person's roles, is where you define
policies about what resources a role can access (i.e. policies). Recently,
centralized authorization has had an active resurgence. Companies like
[Styra][https://www.styra.com/], [Oso](https://www.osohq.com/), and
[HashiCorp](https://www.boundaryproject.io/) have introduced innovative
centralized authorization software. These systems, acting as a "policy decision
point", enable you to define policies, and can evaluate these policies at
runtime. Note: they rely on "policy enforcement points" to call their endpoints,
supplying the data which is input to policies. With that said, there are places
in Auth Server that you can define or impact authorization (besides user claims).
In particular, [OAuth scopes](https://www.rfc-editor.org/rfc/rfc6749#section-3.3)
are used to convey the extent of access in an access token, that when presented
to an API by a software client, enables access control. However, scope is still
input to policy, and may in fact be just one more piece of data considered by
a policy decision point. And one more caveat... the "authorization" in an OAuth
"authorization server", was originally meant to convey the authorization of the
person who was trying to access something. Whether a person consented to share
information, or grant a client the ability to act on their behalf, is another
important consideration. So does "Auth Server" provide authorization? It depends!  

## Consent Gathering

Through the "front channel" (i.e. the browser), Auth Server can interact with
a person by displaying web pages. OpenID Connect and OAuth scopes trigger
the authorization phase (after authentication). By default, the scopes requested
by the client and their respective descriptions are displayed for approval.
However, like all pages in Auth Server, these pages can be customized to meet
your exact requirements.

Auth Server also implements the User Managed Access Protocol ("UMA"). This
profile of OAuth2 enables consent to be sought from a person even after
the initial authentication has taken place. This phase is also called
"claims gathering", but one of the claims could be whether a person consents
to something.

Note: Authorizations are stored relative to a person's entity in the database.
How a person views and revokes consent is outside the scope of Auth Server.
To view a person's consents, you need to use the config API, as this information
is not shared via OpenID Connect or SCIM.
