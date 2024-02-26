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

  During installation of `Janssen`, simply select the option `Install KC SAML` to install and setup SAML SSO for keycloak.
Instructions on how to setup SAML SSO with keycloak post-install will eventually be provided.


## Managing SAML Service Providers Through the Jans-Cli 

  In order to act as an IDP to various SAML SPs (Service Providers), the latter need to be added to janssen. This can be done via 
the `jans-cli` which is what we will cover in this section.

###  Adding a SAML SP 

  This assumed jans-cli is open. Select the menu item `Jans SAML` > `Service Providers`. Then navigate and select `<Add Service Provider>`.
The configurable options are kept to the bare functional minimum but will be expanded gradually.
Input the folowing:
- `Display Name` : An identifiable name for the Service Provider 
- `Enable TR`: Whether or not the Service Provider should be enabled
- `Metadata Location`: The location of the metadata. The supported options so far are `file` and `manual`.
- `Released Attributes`: The user attributes to be released via the SAML response if authentication is succesful

The `manual` metadata option for `Metadata Location` , allows the possibility to specify SP metadata information manually.

## IDP Metadata Location 

  In order for SAML authentication to work , there is a need for the SPs to trust the IDP, which usually is done by using an IDP metadata file 
that will be used on the SP side. The metadata can be found at `https://<server-hostname>/kc/realms/jans/protocol/saml/descriptor`  where 
`<server-hostname>` is the hostname of the Janssen server specified during installation.

## IDP Initiated Flows
TBD

## IDP Key Management 
TBD 

## Have questions in the meantime?

While this documentation is in progress, you can ask questions through [GitHub Discussions](https://github.com/JanssenProject/jans/discussions) or the [community chat on Gitter](https://gitter.im/JanssenProject/Lobby). Any questions you have will help determine what information our documentation should cover.

## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).
