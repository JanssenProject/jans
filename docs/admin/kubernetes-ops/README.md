---
tags:
  - administration
  - kubernetes
  - operations
---

# Overview

This Operation Guide helps you learn about the common operations for Janssen on Kubernetes.


## Prerequisite

You should complete the janssen [installation](../install/helm-install/local.md) on your desired Kubernetes provider.

## Janssen components

- **auth-server**: The OAuth Authorization Server, the OpenID Connect Provider, the UMA Authorization Server--this is the main Internet facing component of Janssen. It's the service that returns tokens, JWT's and identity assertions. This service must be Internet facing.
- **auth-key-rotation**: Responsible for regenerating auth-keys per x hours.
- **config-api**: The API to configure the auth-server and other components is consolidated in this component. This service should not be Internet-facing.
- **OpenDJ**: A directory server which implements a wide range of Lightweight Directory Access Protocol and related standards, including full compliance with LDAPv3 but also support  for Directory Service Markup Language (DSMLv2).Written in Java, OpenDJ offers multi-master replication, access control, and many extensions.
- **Fido**: Provides the server side endpoints to enroll and validate devices that use FIDO. It provides both FIDO U2F (register, authenticate) and FIDO 2 (attestation, assertion) endpoints. This service must be internet facing.
- **SCIM**: a JSON/REST API to manage user data. Use it to add, edit and update user information. This service should not be Internet facing.

## Architectural diagram of Janssen

![svg](../../assets/jans-arch-diagram.svg)
## Common Operations

- [Scaling](scaling.md)
- [Backup and Restore](backup-restore.md)  
- [Certificate Management](cert-management.md)  
- [Customization](customization.md)  
- [Start Order](start-order.md)  
- [Logs](logs.md)
- [Health Check](health-check.md)
- [FAQ](faq.md)
