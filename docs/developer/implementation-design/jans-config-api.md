---
tags:
  - developer
  - config-api
---

# Overview

## Janssen Config API
Jans Config API is an application programming interface (API) gateway managing client access to various Janssen backend services.

[Diagram reference](../../assets/jans-config-api-components.xml)


### Jans Config API features:
l  Jans Config API uses REST endpoints to communicate.
2. Jans Config API endpoint are OAuth 2.0 protected. More details [here](./authorization.md).
3. Jans Config API flexible plugin architecture in which the new features can be added using extensions. More details [here](./plugins.md).
4. Config API endpoint can be used to create new user, clients, scopes, etc. This data is stores into the same persistence store as the Jans-Auth server.


### Jans Config API Flow

