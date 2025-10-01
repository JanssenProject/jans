---
tags:
  - administration
  - planning
---

OAuth gives us a perfect infrastructure for one machine (i.e. software) to
authenticate itself while calling an API of another machine:
the [Client Credential Grant](https://www.rfc-editor.org/rfc/rfc6749#section-4.4).
The software receiving the token can validate the access token signature, or
if it's a reference token, can use [OAuth Introspection](https://datatracker.ietf.org/doc/html/rfc7662).

For extra security, the software in question can also use [mutual TLS](https://www.rfc-editor.org/rfc/rfc8705.html) to authenticate.

The OAuth Authorization Server can also send extra security context, like scopes
(which typically communicate the extent of access), or other information about
the software vendor or characteristics.

Of course this requires the devices in this machine-to-machine scenario to have
a tcp stack (some constrained IOT devices only have a UDP stack).
