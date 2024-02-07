---
tags:
  - administration
  - link
  - keycloak
---

# Jans Keycloak Link

The Jans Keycloak Link is a [Jans Link](README.md) module that provide 
synchronization services to update the Janssen User Store from an external 
Keycloak instance.

Jans Keycloak Link integration with external Keycloak involves following 
configuration steps. 

## Configure Client on Keycloak

Jans Keycloak Link accesses Keycloak data via Keycloak API. A new `confidential`
client needs to be created on Keycloak in order to authorise Jans Keycloak Link
for API access.

- Go to Keycloak administration console and create a new OpenId Connect client
- Enable this client as `confidential` access type and enable the 
  `Service Accounts Enabled` flag.
  ![](../../assets/jans-kc-link-client-2.png)
- 

The Janssen Project documentation is currently in development. Topic pages are being created in order of broadest relevance, and this page is coming in the near future.

## Have questions in the meantime?

While this documentation is in progress, you can ask questions through [GitHub Discussions](https://github.com/JanssenProject/jans/discussions) or the [community chat on Gitter](https://gitter.im/JanssenProject/Lobby). Any questions you have will help determine what information our documentation should cover.

## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).