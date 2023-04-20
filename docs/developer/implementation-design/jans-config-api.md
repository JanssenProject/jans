---
tags:
  - developer
  - config-api
---

# Overview

## Janssen Config API
Janssen(Jans) Config API is an application programming interface (API) gateway managing configuring of various Janssen modules.

[Diagram reference](../../assets/config-api-components.png)


### Jans Config API features:

 1. Jans Config API uses REST endpoints to communicate. 
 2. Jans Config API endpoint are OAuth 2.0 protected. More details [here](../../admin/config-guide/config-api/authorization.md)
 3. Jans Config API plugin architecture can be used to add new features. More details [here](../../admin/config-guide/config-api/plugins.md).
 4. Config API endpoint can be used to create new user, clients, scopes, etc. This data is stores into the same persistence store as the Jans-Auth server.


### Jans Config API Flow
[Diagram reference](../../assets/sequence-config-api-flow.png)

```mermaid

sequenceDiagram
title Jans Config API request flow
autonumber 1

participant Admin
participant Admin-ui
participant Config-api
participant Auth-Server
participant Jans Persistence

Admin->>Admin-ui: Create Client
note over Admin-ui: Click on (+) Create Client and Save button to create new client
Config-api->>Auth-Server: Introspect Token
Auth-Server->>Config-api: Returns Introspection Response
Config-api->>Config-api: Successful validation of token claim with Introspection Response
Config-api->>Jans Persistence: Validate and persist client data
Config-api->>Admin-ui: Returns persistence status
```


 1. **Admin**: Administrator of the application. Will use Admin-ui to configure application. </li>
 2. **Admin-ui**: Gluu graphical user interface for the administrators to manage configuration and other properties of Jans Auth Server via Jans Config API.</li>
 3. **Config-api**: Jans API gateway for configuring Janssen modules like Jans Auth Server, fido2, SCIM, etc. </li>
 4. **Auth-Server**: Janssen federated identity with comprehensive implementation of OpenID Connect. Used for introspection of access token in this flow.</li>
 5. **Jans Persistence**: Jans Persistence layer to persist data in backend.</li>

