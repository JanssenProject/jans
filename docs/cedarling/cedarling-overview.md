---
tags:
  - administration
  - lock
  - authorization / authz
  - Cedar
  - Cedarling
---

# Cedarling Overview

## What is Cedar

[Cedar](https://www.cedarpolicy.com/en) is a policy syntax invented by Amazon and used by their 
[Verified Permission](https://aws.amazon.com/verified-permissions/) service. Cedar policies
enable developers to implement fine-grain access control and externalize policies. To learn more
about why the design of Cedar is **intuitive**, **fast** and **safe**, read this 
[article](https://aws.amazon.com/blogs/security/how-we-designed-cedar-to-be-intuitive-to-use-fast-and-safe/) 
or watch this [video](https://www.youtube.com/watch?v=k6pPcnLuOXY&t=1779s)

Cedar uses the **PARC** syntax: 

* **P**rincipal
* **A**ction
* **R**esource
* **C**ontext 

For example, you may have a policy that says *Admins* can *write* to the */config* folder. The *Admin* role 
is the Principal, *write* is the Action, and the */config* folder is the Resource. The Context is used to 
specify information about the enivironment, like the time of day or network address.

![](../assets/lock-cedarling-diagram-3.jpg)

Fine grain access control makes sense in both the frontend and backend. In the frontend, mastery of 
authz can help developers build better UX. For example, why display form fields a user is not 
authorized to see? In the backend, fine grain policies are necessary for a zero trust architecture.

## What is the Cedarling

![](../assets/lock-cedarling-diagram-1.jpg)

Architecturally, the Cedarling is an embeddable stateful Policy Decision Point, or "PDP". It is 
stateful because it implements an in-memory cache which makes it possible to batch logs and 
implement other performance optimizations. The Cedarling is written in Rust with bindings 
to WASM, iOS, Android, and Python--this makes it possible for web, mobile, and cloud developers
to incorporate the Cedarling into their applications.

The Cedarling is used for both frontend and backend security. Because the frontend is more 
constrained with regard to memory and compute, this requirement was critical to the design.
For example, in the backend, a PDP could run in a Linux container. But in the frontend, the 
Cedarling must run in a browser, using the browser WASM engine. 

How does the Cedarling get the data to calculate a decision? The Principal data is contained in 
the JWTs--a person, a workload, or both. The Action, Resource and Context are sent by the 
application as arguments in the authz request. The Cedarling is fast because it has all the data it 
needs to make a local decision. No cloud round-trips are needed to return an authz decision--a 
cloud roundtrip may kill the performance of a frontend application. The Cedarling can execute many 
requests in less then 1ms--this is critical for UX fine grain authorization.

Below is a conceptual diagram showing how you can archiect the Cedarling for frontend and backend
security. 

![](../assets/lock-cedarling-mobile-generic.jpg)

1. The Cedarling is used to determine if the Mobile Application should be allowed to register. 
For example, perhaps the IDP wants to execute a policy that restricts registration to mobile
applications that present a Google Integrity API attestation to indicate the checksum of the binary 
has not changed, and that the phone is not rooted. 
1. The Cedarling is used to authenticate the person using the mobile application, i.e. the "User". 
For example, perhaps the IDP wants to execute a policy that says that 2FA is required from a non
trusted network. 
1. The Cedarling is used to determine which scopes to add to the OAuth access token. For example, 
perhaps if the mobile application presents a software statement assertion JWT (i.e. and "SSA")
issued by the IDP, the application may request the `financial` scope. 
1. Once JWT tokens are issued by the IDP, the frontend can use these tokens to evaluate local policies. 
For example, perhaps the mobile application only allows access to certain features if the Userinfo JWT
contains a role claim for a "Manager". 
1. The mobile application may send an OAuth access token to call an API. This API Gateway may route this
request to a backend service, but only after evaluating certain security policies. For example, perhaps
the Action POST is only allowed on a Resource (e.g. "URI") when the access token JWT contains a certain 
scope.
1. Finally, the Backend API can use the Cedarling to perform its own fine grain authorization. For example, 
perhaps the Backend only allows transaction greater than $10,000 if the access token contains scope value
`high-net-worth`. 

This is just a hypothetical example, but hopefully you can see how the Cedarling is used to achieve 
multilayer security. Each Cedarling has it's own specific policy store. The API Gateway Cedarling instance
does not need to know the policies or schema for the mobile application. By layering security, you can 
implement a zero trust architecture.

### Cedarling Interfaces

The developer using the Cedarling to build an application uses three easy interfaces: ("`init`"), 
authorization ("`authz`") and logging ("`log`"). 

Developers call the `init` interface on startup of their application, causing the Cedarling to read
its [bootstrap properties](./cedarling-properties) and load its [policy store](./cedarling-policy-store).
If configured, the Cedarling will also retrieve the most  recent IDP public keys and request JWT 
status updates.

The `authz` interface provides the main functionality of the Cedarling: to authorize a PARC 
request from the application by mapping the data sent in the request, and evaluating it with 
the embedded [Rust Cedar Engine](https://github.com/cedar-policy/cedar). The authz interface 
answers the question: "Is this action, on this resource, given this context, allowed with 
these JWTs?". The Cedarling returns the decion--*allow* or *deny*. If denied, the Cedarling
returns "diagnostics"--additional context if the decision is denied.  During `authz`, the 
Cedarling can perform two more important jobs: (1) validate JWT tokens; (2) log the resulting
decision. 

The `log` interface enables developers to retrieve decision and system logs from the Cedarling's 
in-memory cache. See the Cedarling [log](./cedarling-logs) documentation for more information. 

### Cedarling Components

As a developer, you don't really need to understand how the Cedarling is constructed. But this 
section is meant to give you an idea to help you get a better understand of what it's actually
doing. The following diagram is a very high level picture:

![](../assets/lock-cedarling-rust-core-components.jpg)

* **Cedar Engine** is the latest code released from the open source Rust Cedar project. Thanks
Amazon for supporting this fabulous technology! 
* **SparKV** is an in-memory key-value store that support automatic expiration of data. For example,
we don't want to store logs for more then a few minutes. The Cedarling is a *stateful* PDP, but 
of course it doesn't write anything to disk. The state is stored entirely in Memory, and SparKV
provides an easy way to do this. 
* **Init, Authz, and Log Engines** perform actions similar to those described in the interfaces 
above.
* **JWT Engine** is used to validate JWT signatures and to check the status of a JWT token. 
* **Lock Engine** is used for enterprise deployments, where the Cedarling is one of many instances,
and it needs to pick up its Policy Store from a trusted source and send to store its logs centrally, 
for example in a SIEM.

So you can see that the Cedar Engine is central to the functionality of the Cedarling. However, the 
other helper engines make it easier for developer to use Cedar for application security when they 
are using JWT tokens as the source of Person and Workload identity.


