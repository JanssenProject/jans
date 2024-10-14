---
tags:
  - administration
  - reference
  - kubernetes
  - architecture
  - components
---

# Overview

This Reference guide helps you learn about the components and architecture of Janssen.

## Janssen components

- **auth-server**: The OAuth Authorization Server, the OpenID Connect Provider, the UMA Authorization Server--this is the main Internet facing component of Janssen. It's the service that returns tokens, JWT's and identity assertions. This service must be Internet facing.
- **auth-key-rotation**: Responsible for regenerating auth-keys per x hours.
- **config-api**: The API to configure the auth-server and other components is consolidated in this component. This service should not be Internet-facing.
- **Fido**: Provides the server side endpoints to enroll and validate devices that use FIDO. It provides both FIDO U2F (register, authenticate) and FIDO 2 (attestation, assertion) endpoints. This service must be internet facing.
- **SCIM**: a JSON/REST API to manage user data. Use it to add, edit and update user information. This service should not be Internet facing.
- **Casa**: self-service web portal for end-users to manage authentication and authorization preferences for their account in a Gluu Server.

## Architectural diagram of Janssen

![svg](../../../assets/jans-arch-diagram.svg)
