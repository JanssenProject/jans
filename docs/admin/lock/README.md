---
tags:
  - administration
  - lock
  - authorization / authz
  - Cedar
  - Cedarling
---

## Jans Lock Overview

Lock provides a centralized control plane for domains to use [Cedar](https://www.cedarpolicy.com/en)
to secure a network of distributed applications and audit the activity of both people and software. 
Lock makes it easy for Javascript developers to write access policies based on security tokens as 
input, for example OAuth access tokens, or OpenID Connect id_tokens. 

Using a declarative syntax like Cedar for authorization policies is a best practice for enterprise 
application security. A policy engine that executes declarative policies enables developers to define 
security rules without resorting to implementing such rules in their application code. Cedar supports 
traditional access management strategies like RBAC and more adaptive capabilities that offer fine 
grain decisions based on contextal data. The Cedar Engine does this without sacrificing performance 
or the security benefits of a deterministic policy engine.

There are three key components in a Lock topology: (1) Cedarling--a WebAssembly ("WASM") 
component that runs the [Amazon Rust Cedar Engine](https://github.com/cedar-policy/cedar) and
performs JWT token validation; (2) Lock Master--a web service deployed by domains to manage a 
network of distributed ephemeral Cedarlings; (3) [Agama Lab](https://cloud.gluu.org/agama-lab),
a policy authoring tool for developers to design policies and publish their policy store in Github.

You don't need to deploy a Jans Lock Topology to derive utility from the Cedarling. Javascript 
developers can use the Cedarling to secure even a single browser-based application, especially if 
they are using OAuth or OpenID. The Cedarling evaluates a request to perform an action on a 
resource by first validating the tokens, then instantiating a Person and Client entity based
on the JWT data payloads, and evaluating the request based on its existing policies. 

The Lock Master is designed for domains that want control over a **network of Cedarlings** used for different applications, each with their own policy store. Communication in this Lock topology is bi-directional. Cedarlings can send information to the Lock Master, and the Lock Master can push updates to the Cedarlings. Notifications from the Lock Master to the Cedarlings are connectionless--
a Cedarling subscibes to event notifications using 
[Server Sent Events](https://html.spec.whatwg.org/multipage/server-sent-events.html#server-sent-events) or "SSE". Requests from the Cedarling to the Lock Master are sent via HTTP Post to OAuth protected endpoints. 

Using the Cedarling with Lock Master can enable a temporal improvement in OAuth security: in addition
to validating the JWT signature, the Cedarling can check to ensure the token is not revoked. 
Using SSE, Lock Master sends an updated [OAuth Token Status List](https://www.ietf.org/archive/id/
draft-ietf-oauth-status-list-02.html) from Jans Auth Server. The Cedarling can thus checks the 
status of a token without a slow and potentially unreliable OAuth introspection request.

On startup, the Cedarling can make a POST request to fetch a Policy Store from Lock Master, load a
local Policy Store or retreive it from a TLS-protected Web URI. A Policy Store is a JSON document that
contains the Cedar policies, Cedar schema, and list of trusted issuers. How you write your policies is out of scope of the Janssen Project--you can do this manually or use the Gluu 
[Agama Lab](https://cloud.gluu.org/agama-lab) online authoring tool.

One of the challenges a network of distributed Cedarlings poses is the consolidation of audit logs. 
As Cedarlings are ephemeral, the logs need to be archived centrally. Jans Lock addresses this gap by
providing an audit endpoint. Other enterprise management features are available in the 
[Gluu Flex](https://gluu.org/flex) AdminUI.

## Authz Theoretical Background

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

* Move the PDP to the far edge of the network--the browser itself.
* Make the PDP performant and deterministic (i.e. milliseond statup time and always return a 
  PERMIT/DENY response).
* Empower application developers to author policies appropriate for the resources and actions 
they need to protect.
* Centralilze audit and health data collection
* Send updates to the Cedarlings from the Lock Master to enable realtime attack mitigation

## Policy Store

By convention the filename of the Cedarling Policy Store is `cedarling_store.json`. It is a JSON 
file that contains all the data the Cedarling needs to verify JWT tokens, evaluate policies, 
and instantiate the Cedar entities requied to evaluate the policies for a given resource and 
action. The policy store contains three things:

1. [Cedar Schema](https://docs.cedarpolicy.com/schema/schema.html) - JSON format Schema file. 
Lock comes with a common schema, but domains should extend this schema to fit their exact 
requirements. 
2. [Cedar Policies](https://docs.cedarpolicy.com/policies/syntax-policy.html) - JSON format Policy Set file. These policies need to be authored in Agama Lab developer tool or manually.
3. [Trusted Issuers](.) - List of which domains are authorized to issue tokens.

In JSON it looks like this: 

```
{
    "policies": {...},
    "schema": {...},
    "trusted_idps": []
}
```

### Trusted Issuer Schema

At initialization, the Cedarling iterates the list of Trusted IDPs and fetches the current public
keys. The trusted issuer schema provides guidance on how to uniquely identify a person, and how
to build the roles based on a user claim.

Here is a non-normative example: 

```
[
{"name": "Google", 
 "Description": "Consumer IDP", 
 "openid_configuration_endpoint": "https://accounts.google.com/.well-known/openid-configuration",
 "access_tokens": {"trusted": True}, 
 "id_tokens": {"trusted":True, "principal_identifier": "email"},
 "userinfo_tokens": {"trusted": True, "role_mapping": "role"},  
 "tx_tokens": {"trusted": True}
},
...
]
```

### Entity Mapping 

The Cedar schema is like the object defintion, and a Cedar entity is like an instance of the object.
Without entities, there is no data for the policies to evaluate. The Cedarling creates the entities
from the tokens provided as input to the requested authoriztaion in conjunction with its
configuration:

* **TrustedIssuer**: Created on startup from Policy Store
* **Client**: Created from access token 
* **Application**: Created if input supplies an Application name
* **Role**: Created for each `role` claim value in the joined id_token and userinfo token
* **User**: Created based on the joined id_token and userinfo token. `sub` is the entity identifier
* **Access_token**: 1:1 mapping from claims in token
* **id_token**: 1:1 mapping from claims in token
* **Userinfo_token**: 1:1 mapping from claims in token

## More information

* Lock Master configuration and operation [docs](./lock-master.md) 
* Cedarling [Readme](https://github.com/JanssenProject/jans/blob/main/jans-lock/cedarling/README.md)
* Cedarling [Training](.) (coming soon)

