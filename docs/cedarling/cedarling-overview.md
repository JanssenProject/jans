---
tags:
  - administration
  - lock
  - authorization / authz
  - Cedar
  - Cedarling
---

# Cedarling Overview

## Cedar Policy Language

[Cedar](https://www.cedarpolicy.com/en) is an open-source policy language developed by Amazon to enable granular, scalable authorization for applications and services. 
It serves as the foundation for [Amazon Verified Permissions](https://aws.amazon.com/verified-permissions/), providing developers with a robust framework for externalized access control.

### Key Features
- **Fine-Grained Authorization**: Enables precise "allow/deny" decisions at the resource level  
- **Policy Externalization**: Separates authorization logic from application code  
- **High Performance**: Optimized for low-latency authorization decisions (<1ms in most cases)  
- **Formal Verification**: Mathematically proven safety properties prevent common security flaws  

### PARC Authorization Model
Cedar policies follow a structured syntax based on four core elements:

| Component | Description | Example |
|-----------|-------------|---------|
| **Principal** | Entity requesting access | `User`, `Role`,  `Workload` or `Service` (e.g., "Admin") |
| **Action**    | Operation being performed | CRUD operations (e.g., "Write", "Create") |
| **Resource**  | Protected asset being accessed | Application object (e.g., "/config folder") |
| **Context**   | Environmental factors | Time, IP address, MFA status |

### Example of Cedar Policy
```cedar
permit (
    principal == Role::"Admin",
    action == Action::"Write",
    resource == Folder::"/config"
);
```

## What is the Cedarling
[Cedarling](https://github.com/cedar-policy/cedarling) is a high-performance local authorization service written in [Rust](https://www.rust-lang.org/) 
that uses [AWS's Cedar policy language](https://www.cedar.io/) to make access control decisions, where it takes [JWT tokens](https://jwt.io/) 
and evaluates whether a user or service has permission to perform specific actions on resources based on predefined policies and schemas.

It essentially acts as a security checkpoint that answers the question "should this user/service be allowed to do this action?" while running within your own infrastructure rather than as a cloud service.

This makes it particularly useful for:
- API gateways
- Microservices  
- Applications requiring fine-grained access control

### Architectural Overview
- **Stateful PDP**: Implements in-memory caching for batched logging and performance optimizations
- **Cross-Platform Support**: Native Rust core with bindings for:
  - Web: WebAssembly (WASM)
  - Mobile: iOS & Android
  - Cloud: Python
- **Embeddable Design**: Deploys as lightweight component within application runtime environments
### Key Features

#### Multi-Environment Support
| Platform | Implementation | Use Case |
|----------|----------------|----------|
| **Frontend** | Browser WASM engine | Browser-based applications |
| **Mobile** | Native iOS/Android bindings | Mobile app authorization |
| **Backend** | Linux container/Docker | Cloud service authorization |

#### Authorization Data Flow
1. **Principal Identification**  
   Extracted from JWT claims (users, workloads, or hybrid identities)
2. **Request Context**  
   Receives `Action`, `Resource`, and `Context` parameters via authorization API calls
3. **Local Decision Engine**  
   Eliminates cloud dependencies through embedded policy evaluation

#### Performance Characteristics
- **Sub-Millisecond Response**: <1ms decision latency for UX-critical operations
- **Written in Rust**: providing memory safety and thread safety
- **Frontend Optimization**: Browser-compatible WASM engine avoids main thread blocking

#### Design Advantages
- **End-to-End Security**: Consistent policy enforcement across application layers, zero trust security models, Audit logging for security compliance
- **Flexible Configuration**: Environment variable support, file file based configuration
- **Developer-Friendly**: Simple API for authorization decisions, Comprehensive logging for debugging and auditing,etc.
### Sample deployment overview
![](../assets/lock-cedarling-diagram-1.jpg)

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


