<h1 align="center"><img src="https://github.com/JanssenProject/jans/blob/main/docs/assets/logo/janssen_project_transparent_630px_182px.png" alt="Janssen Project - Open Source Digital Identity Infrastructure Software"></h1>

## Welcome to the Janssen Project

[![The Linux Foundation](https://img.shields.io/badge/Member-The%20Linux%20Foundation-blue?style=flat-square)](https://www.linuxfoundation.org/press/press-release/the-janssen-project-takes-on-worlds-most-demanding-digital-trust-challenges-at-linux-foundation)
[![DPG Badge](https://img.shields.io/badge/Verified-DPG-3333AB?logo=data:image/svg%2bxml;base64,PHN2ZyB3aWR0aD0iMzEiIGhlaWdodD0iMzMiIHZpZXdCb3g9IjAgMCAzMSAzMyIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHBhdGggZD0iTTE0LjIwMDggMjEuMzY3OEwxMC4xNzM2IDE4LjAxMjRMMTEuNTIxOSAxNi40MDAzTDEzLjk5MjggMTguNDU5TDE5LjYyNjkgMTIuMjExMUwyMS4xOTA5IDEzLjYxNkwxNC4yMDA4IDIxLjM2NzhaTTI0LjYyNDEgOS4zNTEyN0wyNC44MDcxIDMuMDcyOTdMMTguODgxIDUuMTg2NjJMMTUuMzMxNCAtMi4zMzA4MmUtMDVMMTEuNzgyMSA1LjE4NjYyTDUuODU2MDEgMy4wNzI5N0w2LjAzOTA2IDkuMzUxMjdMMCAxMS4xMTc3TDMuODQ1MjEgMTYuMDg5NUwwIDIxLjA2MTJMNi4wMzkwNiAyMi44Mjc3TDUuODU2MDEgMjkuMTA2TDExLjc4MjEgMjYuOTkyM0wxNS4zMzE0IDMyLjE3OUwxOC44ODEgMjYuOTkyM0wyNC44MDcxIDI5LjEwNkwyNC42MjQxIDIyLjgyNzdMMzAuNjYzMSAyMS4wNjEyTDI2LjgxNzYgMTYuMDg5NUwzMC42NjMxIDExLjExNzdMMjQuNjI0MSA5LjM1MTI3WiIgZmlsbD0id2hpdGUiLz4KPC9zdmc+Cg==)](https://digitalpublicgoods.net/r/janssen-project)

Janssen is a self-funded project chartered directly under the Linux Foundation
to foster the development of enterprise digital identity and access management 
infrastructure. As the lead Contributors, the [Gluu team](https://gluu.org) drives the 
priorities on a day-to-day basis, governed and guided by the Janssen community 
Technical Steering Commitee.

There are several Janssen Components in different stages of development, from demos 
to stable releases. Janssen Project software has batteries included. You 
will find binaries, cloud native deployment assets, documentation and more-- 
enabling you to build a product or mission critical cybersecurity service with 
Janssen software.

If your enteprise needs Janssen for a production deployment, Gluu offers a 
commercial distribution of Janssen Project Components called 
[Gluu Flex](https://gluu.org/flex) and [Gluu Solo](https://gluu.org/solo).

----

**Releases**: [Latest](https://github.com/JanssenProject/jans/releases/latest) | [All](https://github.com/JanssenProject/jans/releases)

**Get Help**: [Discussions](https://github.com/JanssenProject/jans/discussions) | [Chat](https://gitter.im/JanssenProject/Lobby)

**Docs**: [Documentation](https://docs.jans.io/)

**Contribute**: [Contribution Guide](https://docs.jans.io/head/CONTRIBUTING/) | [Community Docs](https://docs.jans.io/head/governance/charter/) | [Developer Guides](https://docs.jans.io/head/CODE_OF_CONDUCT/)

**Social**: [Linkedin](https://www.linkedin.com/company/janssen-project) | [YouTube](https://www.youtube.com/@JanssenProject)

**Resources to learn more**: [Gluu Academy](https://learn.gluu.org/)

[![Artifact Hub](https://img.shields.io/endpoint?url=https://artifacthub.io/badge/repository/janssen-auth-server)](https://artifacthub.io/packages/search?repo=janssen-auth-server)
[![OpenSSF Scorecard](https://api.scorecard.dev/projects/github.com/JanssenProject/jans/badge)](https://scorecard.dev/viewer/?uri=github.com/JanssenProject/jans)
[![OpenSSF Best Practices](https://www.bestpractices.dev/projects/4353/badge)](https://www.bestpractices.dev/projects/4353)
[![Hex.pm](https://img.shields.io/hexpm/l/plug)](./LICENSE)
[![GitHub contributors](https://img.shields.io/github/contributors/janssenproject/jans)](#community)
[![Conventional Commits](https://img.shields.io/badge/Conventional%20Commits-1.0.0-%23FE5196?logo=conventionalcommits&logoColor=white)](https://conventionalcommits.org)

----

## Janssen Components

The table below lists components of the Janssen Project and their 
[maturity stages](https://docs.jans.io/stable/CONTRIBUTING/#project-maturity-stages).

| Component                                    | Description                                                                                                                                                                                                                                 | Lifecycle Stage                                                  |
|:---------------------------------------------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:-----------------------------------------------------------------|
| **[Jans Auth Server](jans-auth-server)**     | A complete OAuth Authorization Server and a [certified](https://openid.net/certification/) OpenID Connect Provider written in Java. It's the upstream open-source core of [Gluu Flex](https://gluu.org/flex).                                     | ![Graduated](https://img.shields.io/badge/Graduated-%2301ba77)   |
| **[Agama](agama)**                           | Agama offers an interoperable way to design authentication flows, coded in a DSL purpose-built for writing identity journeys.                                                                                                               | ![Graduated](https://img.shields.io/badge/Graduated-%2301ba77)   |
| **[Jans FIDO](jans-fido2)**                  | Enables end-users to enroll and authenticate with passkeys and other FIDO authenticators.                                                                                                                                                   | ![Graduated](https://img.shields.io/badge/Graduated-%2301ba77)   |
| **[Jans SCIM](jans-scim)**                   | [SCIM](http://www.simplecloud.info/) JSON/REST [API](https://docs.jans.io/head/admin/reference/openapi/) for user management, including associated FIDO devices.                                                                            | ![Graduated](https://img.shields.io/badge/Graduated-%2301ba77)   |
| **[Jans Config API](jans-config-api)**       | RESTful APIs manage configuration for all Janssen components.                                                                                                                                                                                           | ![Graduated](https://img.shields.io/badge/Graduated-%2301ba77)   |
| **[Text UI ("TUI")](jans-cli-tui)**          | User interface accessible from command line. TUI is text-based interactive configuration tool that leverages config-API to configure Janssen Server modules                                                                                                                                                 | ![Graduated](https://img.shields.io/badge/Graduated-%2301ba77)   |
| **[Jans CLI](jans-cli-tui)**                 | Command line configuration tools to help you correctly call the Config API.                                                                                                                                                 | ![Graduated](https://img.shields.io/badge/Graduated-%2301ba77)   |
| **[Jans Casa](jans-casa)**                   | Jans Casa is a self-service web portal for end-users to manage authentication and authorization preferences for their account in the Janssen Server                                                                                         | ![Graduated](https://img.shields.io/badge/Graduated-%2301ba77)   |
| **[Jans KC](jans-keycloak-integration)**     | provides an array of out of the box IAM services in a single lightweight container image. It's handy for many workforce requirements like SAML. The Janssen authenticator module (SPI) simplifies SSO across Janssen and Keycloak websites. | ![Incubating](https://img.shields.io/badge/Incubating-%23f79307) |
| **[Jans LDAP Link](jans-link)**              | a group of components that provide synchronization services to update the Janssen User Store from an external authoritative LDAP data sources                                                                                               | ![Incubating](https://img.shields.io/badge/Incubating-%23f79307) |
| **[Jans Keycloak Link](jans-keycloak-link)** | a group of components that provide synchronization services to update the Janssen User Store from an external authoritative Keycloak data sources                                                                                           | ![Experimental](https://img.shields.io/badge/Experimental-%23708090) |
| **[Jans Cedarling](jans-cedarling)**         | Cedarling is an embeddable stateful Policy Decision Point for authorization requests. In simple terms, the Cedarling returns the answer: should the application allow this action on this resource given these JWT tokens. It is written in Rust with bindings to WASM, iOS, Android, and Python.                                                                | ![Incubating](https://img.shields.io/badge/Incubating-%23f79307) |
| **[Jans Lock](jans-lock)**                   | An enterprise authorization solution featuring the Cedarling, a stateless PDP and the Lock Server which centralizes audit logs and configuration.                                                                                           | ![Incubating](https://img.shields.io/badge/Incubating-%23f79307) |
| **[Janssen Tarp](demos/janssen-tarp)**       | An OpenID Connect RP test website that runs as a browser plugin in Chrome or Firefox.                                                                                                                                                       | ![Incubating](https://img.shields.io/badge/Incubating-%23f79307) |
| **[Jans Chip](demos/jans-chip)**             | Sample iOS and Android mobile applications that implement the full OAuth and FIDO security stack for app integrity, client constrained access tokens, and user presence.                                                                    | ![Demo](https://img.shields.io/badge/Demo-%23368af7)             |

## Installation

You can install the Janssen federation stack in a Kubernetes cluster or as a 
single VM. 

Development environments: [Quick Start](https://docs.jans.io/head/janssen-server/install/docker-install/quick-start/) 
| [Local K8S](https://docs.jans.io/head/janssen-server/install/helm-install/local/) | [VM install](https://docs.jans.io/head/janssen-server/install/vm-install/vm-requirements/)


Production deployments: [Amazon EKS](https://docs.jans.io/head/janssen-server/install/helm-install/amazon-eks/) | [Microsoft Azure AKS](https://docs.jans.io/head/janssen-server/install/helm-install/microsoft-azure/) | [Google GKE](https://docs.jans.io/head/janssen-server/install/helm-install/google-gke/) | [Rancher](https://docs.jans.io/head/janssen-server/install/helm-install/rancher/)

Check out the 
[Janssen Documentation](https://docs.jans.io/head/janssen-server/install/) 
for details.

## Community

A BIG thanks to all the amazing contributors!! 👏 👏

Building a diverse and inclusive community is an important goal. Please let us know what we
can do to make you feel more welcome, no matter what you want to contribute.

### Code of Conduct / Contribution Guidelines / Security

* [Janssen code of conduct](https://docs.jans.io/head/CODE_OF_CONDUCT/) ensures
that the Janssen community is a welcoming place for everyone.

* Start with the [Contribution Guide](https://docs.jans.io/head/CONTRIBUTING/)
for an introduction on the Janssen development lifecycle.

* If you think you found a security vulnerability, please refrain from posting
it publicly on the forums, the chat, or GitHub. Instead, email us at
`security@jans.io`. Refer to [Janssen Security Policy](.github/SECURITY.md)

### Governance

Janssen is a self-funded Linux Foundation project, governed according to the
[charter](https://docs.jans.io/head/governance/charter/). Technical oversight
of the project is the responsibility of the Technical Steering Committee ("TSC").
Day-to-day decision-making is in the hands of the Contributors. The TSC helps to
guide the direction of the project and to improve the quality and security of
the development process.

### Support

If you find a bug in the Janssen project, would like to suggest a new feature, or
have a "howto" question, please post on
[GitHub Discussions](https://github.com/JanssenProject/jans/discussions), which
is the main channel for community support. There is also a
[community chat on Gitter](https://app.gitter.im/#/room/#JanssenProject_Lobby:gitter.im).

### Releases

Check out the [latest release]((https://github.com/JanssenProject/jans/releases/latest)) of the Janssen Project for new features and updates.

### Janssen History

In 2020, [Gluu](https://gluu.org) decided to give contributors a role in the
governance and collaborated with the Linux Foundation to charter the Janssen
Project. The initial software contribution for the Janssen Project was a fork of
the Gluu Server version 4. Subsequently, the Janssen Project developers added a new
configuration control plane, tools, demos, documentation, packaging and
deployment assets.

### Why the name Janssen?

Pigeons (or doves...) are universally regarded as a symbol of peace--which 
we need more of today. But pigeons are also really fast,
capable of flying 1000 kilometers in a single day, powered by
a handful of seeds. The **Janssen brothers of Arendonk** in Belgium bred the
world's fastest family of racing pigeons. Janssen racing pigeons revolutionized
the sport. The Janssen Project seeks to revolutionize how open-source
digital identity scales in the clouds.
