<h1 align="center"><img src="https://github.com/JanssenProject/jans/blob/main/docs/assets/logo/janssen_project_transparent_630px_182px.png" alt="Janssen Project - Open Source Digital Identity Infrastructure Software"></h1>

## Welcome to the Janssen Project

[![The Linux Foundation](https://img.shields.io/badge/Member-The%20Linux%20Foundation-blue?style=flat-square)](https://www.linuxfoundation.org/press/press-release/the-janssen-project-takes-on-worlds-most-demanding-digital-trust-challenges-at-linux-foundation)
[![DPGA](https://img.shields.io/badge/DPGA-Digital%20Public%20Good-green?style=flat-square)](https://app.digitalpublicgoods.net/a/10470)

An open source digital identity platforms that scales, Janssen is a software
distribution of standards-based, developer-friendly components that are
engineered to work together in any cloud.
* Develop your identity solution using low code on
  [Agama Lab](https://agama-lab.gluu.org)
* Use Helm, Rancher, or OpenTofu to deploy your solution in a Kubernetes
  cluster.
* Connect your solution to mobile, web, and API software clients.

Digital identity has a huge technical surface area. As you can see from
the commits on this projects, we write a lot of code. But we don't have to
write everything! Where it's synergistic, the project leverages third party
security components, like [Keycloak](https://www.keycloak.org/) and [Open Policy Agent](https://www.openpolicyagent.org/). We favor security
software that lives under the Linux Foundation umbrella, but other community
governed open source components are ok too.

The Janssen Project is the home of
[Agama](https://docs.jans.io/head/agama/introduction/), a programming language
for web login flows. Agama also defines the `.gama` file extension, an archive
format to standardize deployment of Agama code on any IDP.

Janssen is a self-funded project chartered directly under the
Linux Foundation. It is recognized as a
[Digital Public Good](https://app.digitalpublicgoods.net/a/10470) by the
[DPGA](https://digitalpublicgoods.net/). Currently, a lot of contributions and 
many core 
contributors of the Janssen
Project are from the [Gluu team](https://gluu.org), who provide a 
commercial 
distribution called [Gluu Flex](https://gluu.org/flex).

----

**Releases**: [Latest](https://github.com/JanssenProject/jans/releases/latest) | [All](https://github.com/JanssenProject/jans/releases)

**Get Help**: [Discussions](https://github.com/JanssenProject/jans/discussions) | [Chat](https://gitter.im/JanssenProject/Lobby)

**Docs**: [Documentation](https://docs.jans.io/)

**Contribute**: [Contribution Guide](https://docs.jans.io/head/CONTRIBUTING/) | [Community Docs](https://docs.jans.io/head/governance/charter/) | [Developer Guides](https://docs.jans.io/head/CODE_OF_CONDUCT/)

**Social**: [Linkedin](https://www.linkedin.com/company/janssen-project)

[![Artifact Hub](https://img.shields.io/endpoint?url=https://artifacthub.io/badge/repository/janssen-auth-server)](https://artifacthub.io/packages/search?repo=janssen-auth-server)
[![CII Best Practices](https://bestpractices.coreinfrastructure.org/projects/4353/badge)](https://bestpractices.coreinfrastructure.org/projects/4353)
[![Hex.pm](https://img.shields.io/hexpm/l/plug)](./LICENSE)
[![GitHub contributors](https://img.shields.io/github/contributors/janssenproject/jans)](#users-and-community)
[![Conventional Commits](https://img.shields.io/badge/Conventional%20Commits-1.0.0-%23FE5196?logo=conventionalcommits&logoColor=white)](https://conventionalcommits.org)

----

## Janssen Components

| Component                                    | Description                                                                                                                                                                                                                                 | Lifecycle Stage                                                  |
|----------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:-----------------------------------------------------------------|
| **[Jans Auth Server](jans-auth-server)**     | A very complete Java OAuth Authorization Server and a [certified](https://openid.net/certification/) OpenID Connect Provider. It's the upstream open-source core of [Gluu Flex](https://gluu.org/flex).                                     | ![Graduated](https://img.shields.io/badge/Graduated-%2301ba77)   |
| **[Agama](agama)**                           | Agama offers an interoperable way to design authentication flows, coded in a DSL purpose-built for writing identity journeys.                                                                                                               | ![Graduated](https://img.shields.io/badge/Graduated-%2301ba77)   |
| **[Jans FIDO](jans-fido2)**                  | Enables end-users to enroll and authenticate with passkeys and other FIDO authenticators.                                                                                                                                                   | ![Graduated](https://img.shields.io/badge/Graduated-%2301ba77)   |
| **[Jans SCIM](jans-scim)**                   | [SCIM](http://www.simplecloud.info/) JSON/REST [API](https://docs.jans.io/head/admin/reference/openapi/) for user management, including associated FIDO devices.                                                                            | ![Graduated](https://img.shields.io/badge/Graduated-%2301ba77)   |
| **[Jans Config API](jans-config-api)**       | RESTful control plane for all Janssen components.                                                                                                                                                                                           | ![Graduated](https://img.shields.io/badge/Graduated-%2301ba77)   |
| **[Text UI ("TUI")](jans-cli-tui)**          | Command line and interactive configuration tools to help you correctly call the Config API.                                                                                                                                                 | ![Graduated](https://img.shields.io/badge/Graduated-%2301ba77)   |
| **[Jans Casa](jans-casa)**                   | Jans Casa is a self-service web portal for end-users to manage authentication and authorization preferences for their account in the Janssen Server                                                                                         | ![Graduated](https://img.shields.io/badge/Graduated-%2301ba77)   |
| **[Jans KC](jans-keycloak-integration)**     | provides an array of out of the box IAM services in a single lightweight container image. It's handy for many workforce requirements like SAML. The Janssen authenticator module (SPI) simplifies SSO across Janssen and Keycloak websites. | ![Incubating](https://img.shields.io/badge/Incubating-%23f79307) |
| **[Jans LDAP Link](jans-link)**              | a group of components that provide synchronization services to update the Janssen User Store from an external authoritative LDAP data sources                                                                                               | ![Incubating](https://img.shields.io/badge/Incubating-%23f79307) |
| **[Jans Keycloak Link](jans-keycloak-link)** | a group of components that provide synchronization services to update the Janssen User Store from an external authoritative Keycloak data sources                                                                                           | ![Incubating](https://img.shields.io/badge/Incubating-%23f79307) |
| **[Jans Lock](jans-lock)**                   | A Pub/Sub client that retrieves the latest data about OAuth access and transaction tokens and updates OPA.                                                                                                                                  | ![Incubating](https://img.shields.io/badge/Incubating-%23f79307) |
| **[Jans Tarp](demos/jans-tarp)**             | An OpenID Connect RP test website that runs as a browser plugin in Chrome or Firefox.                                                                                                                                                       | ![Incubating](https://img.shields.io/badge/Incubating-%23f79307) |
| **[Jans Chip](demos/jans-chip)**             | Sample iOS and Android mobile applications that implement the full OAuth and FIDO security stack for app integrity, client constrained access tokens, and user presence.                                                                    | ![Demo](https://img.shields.io/badge/Demo-%23368af7)             |
| **[Jans Tent](demos/jans-tent)**             | A test Relying Party ("RP") built using Python and Flask. Enables you to send different requests by quickly modifying just one configuration file.                                                                                          | ![Demo](https://img.shields.io/badge/Demo-%23368af7)             |

## Installation

You can install Janssen in a Kubernetes cluster or as a single VM. Check out the
[Janssen Documentation](https://docs.jans.io/head/admin/install/) for all
the details.

## Community

A BIG thanks to all the amazing contributors!! 👏 👏

Building a diverse community is our number one goal. Please let us know what we
can do to make you feel more welcome, no matter what you want to contribute.

There are many ways you can contribute. Join this amazing team!

<a href="https://github.com/JanssenProject/jans/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=JanssenProject/jans" />
</a>

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

Below is the list of current mega releases that hold information about every single release of our services and modules:
- [v1.1.3](https://github.com/JanssenProject/jans/releases/tag/v1.1.3)
- [v1.1.2](https://github.com/JanssenProject/jans/releases/tag/v1.1.2)
- [v1.1.1](https://github.com/JanssenProject/jans/releases/tag/v1.1.1)
- [v1.1.0](https://github.com/JanssenProject/jans/releases/tag/v1.1.0)
- [v1.0.22](https://github.com/JanssenProject/jans/releases/tag/v1.0.22)
- [v1.0.21](https://github.com/JanssenProject/jans/releases/tag/v1.0.21)
- [v1.0.20](https://github.com/JanssenProject/jans/releases/tag/v1.0.20)
- [v1.0.19](https://github.com/JanssenProject/jans/releases/tag/v1.0.19)
- [v1.0.18](https://github.com/JanssenProject/jans/releases/tag/v1.0.18)
- [v1.0.17](https://github.com/JanssenProject/jans/releases/tag/v1.0.17)
- [v1.0.16](https://github.com/JanssenProject/jans/releases/tag/v1.0.16)
- [v1.0.15](https://github.com/JanssenProject/jans/releases/tag/v1.0.15)
- [v1.0.14](https://github.com/JanssenProject/jans/releases/tag/v1.0.14)
- [v1.0.13](https://github.com/JanssenProject/jans/releases/tag/v1.0.13)
- [v1.0.12](https://github.com/JanssenProject/jans/releases/tag/v1.0.12)
- [v1.0.11](https://github.com/JanssenProject/jans/releases/tag/v1.0.11)
- [v1.0.10](https://github.com/JanssenProject/jans/releases/tag/v1.0.10)
- [v1.0.9](https://github.com/JanssenProject/jans/releases/tag/v1.0.9)
- [v1.0.8](https://github.com/JanssenProject/jans/releases/tag/v1.0.8)
- [v1.0.7](https://github.com/JanssenProject/jans/releases/tag/v1.0.7)
- [v1.0.6](https://github.com/JanssenProject/jans/releases/tag/v1.0.6)
- [v1.0.5](https://github.com/JanssenProject/jans/releases/tag/v1.0.5)
- [v1.0.4](https://github.com/JanssenProject/jans/releases/tag/v1.0.4)
- [v1.0.3](https://github.com/JanssenProject/jans/releases/tag/v1.0.3)
- [v1.0.2](https://github.com/JanssenProject/jans/releases/tag/v1.0.2)
- [v1.0.1](https://github.com/JanssenProject/jans/releases/tag/v1.0.1)
- [v1.0.0](https://github.com/JanssenProject/jans/releases/tag/v1.0.0)
- [v1.0.0-beta.16](https://github.com/JanssenProject/jans/releases/tag/v1.0.0-beta.16)
- [v1.0.0-beta.15](https://github.com/JanssenProject/jans/releases/tag/v1.0.0-beta.15)

### Janssen History

In 2020, [Gluu](https://gluu.org) decided to give contributors a role in the
governance and collaborated with the Linux Foundation to charter the Janssen
Project. The initial software contribution for the Janssen Project was a fork of
the "Gluu Server". Subsequently, the Janssen Project developers added a new
configuration control plane, tools, demos, documentation, packaging and
deployment assets.

### Why the name Janssen?

Pigeons (or doves...) are universally regarded as a symbol of peace--which I
think everyone can agree we need more of today. But pigeons are also really fast,
capable of flying 1000 kilometers in a single day, powered by
a handful of seeds. The **Janssen brothers of Arendonk** in Belgium bred the
world's fastest family of racing pigeons. Janssen racing pigeons revolutionized
the sport. The Janssen Project seeks to revolutionize how open-source
digital identity scales in the clouds.
