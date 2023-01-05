---
tags:
  - administration
  - planning
  - multitenancy
---

Multitenancy is a software architecture where several distinct customers can
utilize a single software instance. For a federated identity platform, this
presents a number of challenges:

1. Privacy: Customers must only see their own user information and data
1. Trust: Customers trust a distinct set of RP's
1. Authorization: Customers have specific police requirements
1. UX: Customers brand all user-facing web pages and messages
1. Logs: Customers see only their own log
1. Custom Business Logic: Customers have special rules, for example to
authenticate users, register clients, or issue access tokens.
1. Cryptographic Keys: Customers have their own keys
1. URLs: Customers host the IDP using their own domain name
1. Quality of Service: Customers control response time and concurrency
1. Access: Customers can access systems and data directly

One key decision to make with regard to multitenancy is which systems to
share. You could share nothing, which lowers deployment complexity but
increases operational complexity. Or you could share everything, which is
easier to operate, saves money on hardware, but requires a lot more code. Or you
could land somewhere between--share some systems, but not others.

If you want to address all the challenges of multitenancy, then a "shared
nothing" approach makes the most sense. And with advances in cloud native
deployment, gitops, and configuration as code, you can approach the cost savings
of "shared everything" multitenancy today, especially if you leverage serverless,
just in time auto-scaling. However, you will need some cloud natiuve devops
gurus to pull this off.

If having one top level domain is acceptable, it is possible to add a `tenant_id`
property to each entity (for example, client and person entities). Auth Server
and SCIM interceptions scripts enable you to filter results by adding this
`tenant_id` to queries behind the scenes. For example, you could also render
different login pages by using `default_acr_values` for clients.

It's possible in the future Jansen Project will implement some kind of support
to bundle a multitenancy approach that captures some of the properties described
above at the code level, but for the time being, it is not a priority.
