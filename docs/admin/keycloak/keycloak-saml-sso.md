---
tags:
  - administration
  - keycloak
  - SAML
  - SSO
---

## Overview

Janssen acts as a SAML Identity Provider to support outbound SAML single sign-on (SSO).
It does this by leveraging the feature of the same name available in keycloak, while leveraging `jans-auth` to handle the 
user authentication part of SSO. Once authentication is successful, the user is redirected to the SP with personnal attributes 
and an active SSO session.

## Installation 

## Have questions in the meantime?

While this documentation is in progress, you can ask questions through [GitHub Discussions](https://github.com/JanssenProject/jans/discussions) or the [community chat on Gitter](https://gitter.im/JanssenProject/Lobby). Any questions you have will help determine what information our documentation should cover.

## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).
