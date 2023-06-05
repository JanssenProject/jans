---
tags:
  - administration
  - config-api
---

# config-api

## Overview
[Jans Config Api](https://github.com/JanssenProject/jans/tree/main/jans-config-api) provides a central place to manage and configure jans modules.
It helps in configuring auth-server, users, fido2 and scim modules.

Config API is a REST application that is developed using Weld 4.x (JSR-365) and JAX-RS. Its endpoints can be used to manage configuration and other properties of [Jans Auth Server](https://github.com/JanssenProject/jans/tree/vreplace-janssen-version/jans-auth-server), which is an open-source OpenID Connect Provider (OP) and UMA Authorization Server (AS)

![Config-API-Architecture](../../../assets/config-api-architecture.png)

If you want to learn more about Weld, please visit its website: https://weld.cdi-spec.org/

## Packaging and running the application
The application can be packaged using `./mvnw package`.
It produces the `jans-config-api.war` file in the `server/target` directory.
Be aware that all the dependencies are copied into the `server/target/jans-config-api/WEB-INF/lib` directory.

## Plugins
Jans Config API follow a flexible plugin architecture in which the new features can be added using extensions called plugins without altering the application itself. In this section, we will discuss the steps to develop and add plugins in Jans Config API.
[Refer here](./plugins.md) for more details.

## Endpoints protection
Config API endpoints are OAuth 2.0 protected. It supports simple bearer and JWT token.
Learn more API endpoint protection using [here](./authorization.md).

## Monitoring
Config API module monitoring related details [here](./authorization.md).

