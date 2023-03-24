---
tags:
  - administration
  - planning
---

Using roles to control access to resources makes sense for lots of security use
cases. One benefit of RBAC is that it's deterministic-- from an audit
perspective, you can tell who had access to what at any point in time. This is
harder to achieve when contextual variables play a role in determining access.

An OpenID Provider / OAuth Authorization Server like Jans Auth Server certainly
has a role to play in the implementation of an RBAC infrastructure, but is not
capable of delivering all the functionality your organization will need for a
complete solution. For example, you may need a platform to define enterprise
roles or to perform role consolidation. This is normally handled by an
[identity governance](./identity-access-governance.md) tool.
Also, Jans Auth Server is not a policy management platform, like
* [Styra OPA](https://styra.com), [OSO](https://www.osohq.com)
* [Zanzibar](https://zanzibar.academy/), or
* [Hashicorp Boundary](https://www.boundaryproject.io/) or
* [Apache Fortress](https://www.symas.com/syms-rbac-abac-apache-fortress)

The tools above are just a few open source policy management frameworks. There
are more commercial products in this space.

Although Jans Auth Server may not be a complete RBAC solution, there are still
some RBAC capabilities to consider. When a person authenticates using a web
browser, the client can obtain user claims via OpenID Connect. It may make sense  
to send the `role` role claim to the client. You may also send the `memberOf`
claim, if your organization uses group membership to manage roles.

But what if you need to dynamically compute roles? Or if you don't want to
over-share by sending all the roles and groups associated for a person? One
strategy is to use the Jans Auth Server Update Token Interception script to
render the role claim, either in the OAuth access token, the `id_token` or
the Userinfo JWT.
