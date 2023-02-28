---
tags:
  - administration
  - planning
---

An application portal ("portal") is a website which displays all the
protected applications available for an end user to access. The Janssen Project
does not provide a portal. But you can build your own.

Each application that you want in your portal should be configured as an
OpenID Connect RP. Build a simple website (any CMS will do) that is also
an RP. The OP can provide the roles of the user, or another claim, which will
enable your portal to determine which websites to display. Iterate and
display the websites (or icons) with the respective URLs.
