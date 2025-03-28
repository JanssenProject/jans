---
tags:
  - administration
  - auth-server
  - uma
---

# UMA Features

As a profile of OAuth 2.0 that is complementary to OpenID Connect, UMA 2 defines RESTful, JSON-based, standardized flows and constructs for coordinating the protection of APIs and web resources.

UMA 2 defines interfaces between authorization servers (AS), like Gluu, and resource servers (RS) that enable centralized policy decision-making for improved service delivery, auditing, policy administration, and accountability, even in a very loosely coupled "public API" environment. 

UMA 2 does not standardize a policy expression language, enabling flexibility in policy expression and evaluation through XACML, other declarative policy languages, or procedural code as warranted by conditions. 

UMA 2 inherits authentication agnosticism from OAuth. It concentrates on authorization, not authentication. It has been profiled to work with OpenID Connect to gather identity claims from whoever is attempting access, and enables attribute-based ("claims" in OAuth2) authorization (with group-based or role-based policies a natural subset).
 
## Terminology

UMA 2 introduces new terms and enhancements of OAuth term definitions. A few important terms include:

Resource Owner (RO): An entity capable of granting access to a protected resource--the "user" in User-Managed Access. This is typically an end-user, but it can also be non-human entity that is treated as a person for limited legal purposes, such as a corporation.

Resource Server (RS): A server that hosts resources on a resource owner's behalf, registers resources for protection at an authorization server, and is capable of accepting and responding to requests for protected resources.

Authorization Server (AS)_*_: A server that protects, on a resource owner's behalf, resources managed at a resource server.

_*_ _The Janssen Authentication Server acts as an UMA AS_.

Learn more in the UMA 2 [Core](https://docs.kantarainitiative.org/uma/wg/uma-core-2.0-20.html), [Federated Authorization](https://docs.kantarainitiative.org/uma/ed/oauth-uma-federated-authz-2.0-07.html) and [Grant](https://docs.kantarainitiative.org/uma/ed/oauth-uma-grant-2.0-06.html) specifications.
