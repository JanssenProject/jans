---
tags:
  - administration
  - lock
  - authorization / authz
  - Cedar
  - Cedarling
---

# Jans Lock Overview

Lock provides a centralized control plane for domains to use [Cedar](https://www.cedarpolicy.com/en)
to secure a network of distributed applications and audit the activity of both people and software. 

Using a declarative syntax like Cedar for authorization policies is a best practice for enterprise
application security. Using a policy engine enables developers to define security in their 
application without resorting to writing policies in the code. Cedar supports traditional access management strategies like RBAC, and more adaptive policies that consider a more nuanced model 
of the context. The Cedar Engine does this without sacrificing the security benefit of a 
deterministic policy engine.

There are three key components in the Lock architecture: (1) Cedarling--a WebAssembly ("WASM") 
component that runs the [Amazon Rust Cedar Engine](https://github.com/cedar-policy/cedar) and
performs JWT token validation; (2) Lock Master--a web service deployed by domains to manage a 
network of distributed ephemeral Cedarlings; (3) [Agama Lab](https://cloud.gluu.org/agama-lab)
policy authoring tool for developers to easily design policies and publish their policy store
in Github.

The Cedarling makes sense to secure even a single browser-based application. A developer could 
specify a local Cedarling policy store or retreive one from a Web URI (e.g. Github).

Jans Lock offers organizational control over a network of Cedarlings used for different
applications, each with their own policy store. It does this by providing central endpoints that
publish configuration, collect audit logs, and push real time updates to Cedarlings. In addition to
publishing the Policy Store, the Lock Master server enables Cedarlings to subscibe to real time 
event notifications. A notification example is the latest OAuth Token Status List JWT from Jans 
Auth Server, or an updated Policy Store.

## Background

For years, security architects have conceptualized distributed authorization model in line with
[RFC 2409](https://datatracker.ietf.org/doc/html/rfc2904#section-4.4)
and [XACML](https://docs.oasis-open.org/xacml/3.0/xacml-3.0-core-spec-cos01-en.html),
which describe several common roles:

| Role	| Acronym | Description |
| ----- | :--: | ----------- |
| Policy Decision Point	| PDP |  Service which evaluates access requests against authorization policies before issuing access decisions |
| Policy Information Point	| PIP | The source of "data", e.g. about people, clients and resources |
| Policy Enforcement Point	| PEP | Service, website or API which queries the PDP for authorization |
| Policy Administration Point	| PAP |  Where admins manage the authorization infrastructure |
| Policy Retrieval Point	| PRP | Repository where policies are stored |

Jans Lock aligns with this model:

| Role	| Lock | Description |
| ----- | :--: | ----------- |
| PDP	| Cedarling	| Evaluates policies versus input data |
| PIP	| JWT tokens | Contain data to instantiate entities |
| PEP	| Application | Must rely on Cedarling for decision |
| PAP	| Jans Config API | Endpoints for Lock admin configuration |
| PRP	| Lock Master | Endpoints to publish Policy Store and other PDP configuration |

## Lock Design Goals

Following are a list of goals that informed the design of Jans Lock and the Cedarling:

* Move the PDP to the edge of the network--even into the browser itself.
* Make the PDP performant and deterministic (i.e. milliseond statup time and always return a 
  PERMIT/DENY response).
* Empower application developers to author policies appropriate for the resources and actions 
they need to protect.
* Centralilze audit and health data collection
* Publish centralized updates enabling the network to adapt in real time to attacks
* Push updates to enable near-realtime responses to attacks

# More information

* Lock Master configuration and operation [docs](./lock-master.md) 
* Cedarling [Readme](.)
* RBAC Use Case
* API Access Management Use Case
* Real time token revocation Use Case
* Cedarling [Training](.) (coming soon!)
