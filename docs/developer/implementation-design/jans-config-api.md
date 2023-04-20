---
tags:
  - developer
  - config-api
---

# Overview

## Janssen Config API
Jans Config API is an application programming interface (API) gateway managing client access to various Janssen backend services.

[Diagram reference](../../assets/config-api-components.png)


### Jans Config API features:
<ol>
<li>l. Jans Config API uses REST endpoints to communicate.</li> 
<li>2. Jans Config API endpoint are OAuth 2.0 protected. More details [here](./authorization.md).</li> 
<li>3. Jans Config API plugin architecture can be used to add new features. More details [here](./plugins.md).</li> 
<li>4. Config API endpoint can be used to create new user, clients, scopes, etc. This data is stores into the same persistence store as the Jans-Auth server.</li> 
</ol>

### Jans Config API Flow
[Diagram reference](../../assets/config-api-flow.xml)
```mermaid
sequenceDiagram
autonumber 1
    title Jans Config API request flow

participant Admin
participant Admin-ui
participant Config-api
participant Auth-Server
participant Jans Persistence


Admin->Admin-ui: Create Client
note over Admin-ui: Click on (+) Create Client and Save button to create new client
Config-api->Auth-Server: Introspect Token
Config-api<--Auth-Server: Returns Introspection Response
Config-api<--Config-api: Successful validation of token claim with Introspection Response
Config-api<--Config-api: Validate token claim with Introspection Response
Config-api->Jans Persistence: Validate and persist client data
Admin-ui<--Config-api: Returns persistence status


```

