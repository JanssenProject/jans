---
tags:
  - administration
  - auth-server
  - session
---

## IDP versus RP session

Applications generally have their own session cookie (the "RP Session").
This makes sense, because the RP only redirects to the IDP for authentication
if it cannot find its own local cookie. If a user has a session with many
RP's, achieving simultaneous logout across all sites is a challenge--something
which your business leaders may not appreciate. While OpenID proposes several
solutions to logout, none are ideal. Fundamentally, logout is an asynchronous
challenge. A given RP may be disconnected from the network. Thus logout
messages sent to RP's that are not received must be replayed. The
[IETF Sec Events Workgroup](https://datatracker.ietf.org/doc/charter-ietf-secevent/01/) has been working on standards to handle logout (and other asynchronous
requirements). But adoption of this architecture is not common.

Another challenge of RP sessions is that they may have a different timeout
for inactivity. See the Janssen Planning Guide page on
[Timeout Management](../../planning/timeout-management.md) for more details.