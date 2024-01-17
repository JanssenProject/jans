---
tags:
  - administration
  - keycloak
  - SAML
  - inbound
---

## Overview
Janssen supports authentication against external SAML identity provider (IDP) namely inbound SAML/inbound identity.
This document provides instructions for configuring Janssen server


## Enable inbound SAML
Make sure you have Janssen server with SAML plugin installed and enabled. SAML plugin can be enabled using TUI (Jans Saml -> Contribution)

## Inbound SAML Authentication Flow

The following is a high-level diagram depicting a typical inbound identity user authentication and provisioning workflow:
![](../../../assets/inbound-saml-flow.png)

## Configure IDP for inbound SAML
1. Create new IDP in Keycloak using [Janssen Text-based UI(TUI)](../../config-guide/config-tools/jans-tui/README.md) or [Janssen command-line interface](../../config-guide/config-tools/jans-cli/README.md).
2. Use Agama Lab is an online visual editor to build authentication flows. Learn more about [Agama Lab](../../developer/agama/quick-start-using-agama-lab.md)
3. Fork existing agama-inbound-saml project from [Agama Lab Projects](https://agama-lab.gluu.org/landing-page/)
4. Deploying .gama package on Janssen Server 
5. Testing the authentication flow using [Jans Tarp](https://github.com/JanssenProject/jans/blob/main/demos/jans-tarp/README.md)

