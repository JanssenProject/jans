---
tags:
  - administration
  - keycloak
  - SAML
  - SSO
---

## Overview

The Janssen Server acts as a SAML Identity Provider to support outbound SAML
single sign-on (SSO). It does this by leveraging the SAML features available
in [Keycloak](https://www.keycloak.org/), while leveraging the Janssen Server's
authentication module to handle the user
authentication part of SSO. Once authentication is successful, the user is
redirected to the SP with personal attributes and an active SSO session.

## Installation

During installation of the Janssen Server, simply select the option
`Install Jans KC`   to install and setup SAML SSO for Keycloak. Instructions
on how to setup SAML SSO with Keycloak post-install will eventually be provided.


## Managing SAML Service Providers Through the Jans-Cli

To act as an IDP to various SAML SPs (Service Providers), the latter
need to be added to the Janssen Server. This can be done via the
[Jans TUI](../config-guide/config-tools/jans-tui/README.md) which is what we
will cover in this section.

###  Adding a SAML SP

1. Open Jans-TUI
1. Select the menu item `Jans SAML` > `Service Providers`
1. Then navigate and select `<Add Service Provider>`
1. Input the following:
    1. `Display Name` : An identifiable name for the Service Provider
    1. `Enable TR`: Whether or not the Service Provider should be enabled
    1. `Metadata Location`: The location of the metadata. The supported options
      so far are `file` and `manual`.
    1. `Released Attributes`: The user attributes to be released via the SAML
      response if authentication is successful

The configurable options are kept to the bare functional minimum but will be
expanded gradually.

The `manual` metadata option for `Metadata Location`, allows the possibility
to specify SP metadata information manually.

## IDP Metadata Location

For SAML authentication to work, there is a need for the SPs to
trust the IDP, which usually is done by using an IDP metadata file that will
be used on the SP side. The metadata can be found at
`https://<server-hostname>/kc/realms/jans/protocol/saml/descriptor`  where
`<server-hostname>` is the hostname of the Janssen server specified during
installation.

## IDP Initiated Flows

_This content is a work in progress_

## IDP Key Management

_This content is a work in progress_

## Have questions in the meantime?

You can ask questions through [GitHub Discussions](https://github.com/JanssenProject/jans/discussions) or the [community chat on Gitter](https://gitter.im/JanssenProject/Lobby). Any questions you have will help determine what information our documentation should cover.

## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).
