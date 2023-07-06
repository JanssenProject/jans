<h1 align="center"><img src="https://github.com/JanssenProject/jans/blob/main/docs/assets/logo/janssen_project_transparent_630px_182px.png" alt="Janssen Project - Open Source Digital Identity Infrastructure Software"></h1>

## Welcome to the Janssen Project

Linux Foundation Janssen Project is a collaboration hub for digital identity
infrastructure software. It is also the home of Agama, a domain specific
language and archive format for interoperable identity orchestration.

The Janssen server distribution is used by domains who need to self-host
a modern digital authentication service. It enables domains to pick the services
they want to run ala carte, and it plays well with existing and legacy IAM
infrastructure.  In addition to the Janssen server software, the project
includes Agama, relying party software, cloud native assets and some useful
demos. We've proud to say that we've been recognized as a
[Digital Public Good](https://app.digitalpublicgoods.net/a/10470).

[![The Linux Foundation](https://img.shields.io/badge/Member-The%20Linux%20Foundation-blue?style=flat-square)](https://www.linuxfoundation.org/press/press-release/the-janssen-project-takes-on-worlds-most-demanding-digital-trust-challenges-at-linux-foundation)
[![DPGA](https://img.shields.io/badge/DPGA-digital%20public%20good-green?style=flat-square)](https://app.digitalpublicgoods.net/a/10470)

----

**Releases**: [Latest](https://github.com/JanssenProject/jans/releases/latest) | [All](https://github.com/JanssenProject/jans/releases)

**Get Help**: [Discussions](https://github.com/JanssenProject/jans/discussions) | [Chat](https://gitter.im/JanssenProject/Lobby)

**Get Started**: [Documentation](https://docs.jans.io/) | [Quick Start](#quick-start) | [User Guides](https://docs.jans.io/head/admin/recipes/)

**Contribute**: [Contribution Guide](https://docs.jans.io/head/CONTRIBUTING/) | [Community Docs](https://docs.jans.io/head/governance/charter/) | [Developer Guides](https://docs.jans.io/head/CODE_OF_CONDUCT/)

**Social**: [Twitter](https://twitter.com/janssen_project) | [Linkedin](https://www.linkedin.com/company/janssen-project)



[![Artifact Hub](https://img.shields.io/endpoint?url=https://artifacthub.io/badge/repository/janssen-auth-server)](https://artifacthub.io/packages/search?repo=janssen-auth-server)
[![CII Best Practices](https://bestpractices.coreinfrastructure.org/projects/4353/badge)](https://bestpractices.coreinfrastructure.org/projects/4353)
[![Hex.pm](https://img.shields.io/hexpm/l/plug)](./LICENSE)
[![GitHub contributors](https://img.shields.io/github/contributors/janssenproject/jans)](#users-and-community)
[![Conventional Commits](https://img.shields.io/badge/Conventional%20Commits-1.0.0-%23FE5196?logo=conventionalcommits&logoColor=white)](https://conventionalcommits.org)

----

**Table of Contents**

- [Janssen Components](#janssen-components)
- [Getting Started](#getting-started)
  - [Installation](#installation)
- [Users and Community](#users-and-community)
- [Contributing](#contributing)
  - [Code of Conduct](#code-of-conduct)
  - [Contribution Guidelines](#contribution-guidelines)
- [Security](#security)
- [Documentation](#documentation)
- [Design](#design)
  - [Design Goals](#design-goals)
- [Governance](#governance)
- [Support](#support)
- [More about Janssen Project](#more-about-janssen-project)
  - [History](#history)
  - [why the name Janssen](#why-the-name-janssen)

## Janssen Components

A partial list of software in this project:

1. **[jans-auth-server](jans-auth-server)**: A fairly comprehensive Java OAuth
Authorization Server and an OIDF certified OpenID Connect Provider. It's the
upstream open source used in [Gluu Flex](https://gluu.org).

1. **[jans-fido2](jans-fido2)**: A FIDO Server that enables people to
authenticate with USB, NFC, BT or platform FIDO devices.

1. **[jans-scim](jans-scim)**: [SCIM](http://www.simplecloud.info/) JSON/REST
[API](https://datatracker.ietf.org/doc/html/rfc7644#section-3.2) for identity
provisioning automation for users and fido devices persisted in the Janssen
database.

1. **[jans-config-api](jans-config-api)**: Java Config API Server: service
provides single control plane for all Janssen services.

1. **[jans-cli-tui](jans-cli-tui)**: Text-only tools for interactive and
single-line configuration for those who don't like long curl commands.

1. **[Agama](agama)**: Language reference and Java implementation. Agama
offers an interoperable way to design authentication flows, coded in a DSL
purpose built for writing identity journeys.

1. **[Jans Tarp](demos/jans-tarp)**: A fun test browser plugin for invoking
OpenID Connect authentication flows (i.e. a "test RP").

## Getting Started

### Quick Start

For development and testing purposes, the Janssen Server can be quickly installed on an Ubuntu 20.04 VM by running the command below:

```
wget https://raw.githubusercontent.com/JanssenProject/jans/main/automation/startjanssenmonolithdemo.sh && chmod u+x startjanssenmonolithdemo.sh && sudo bash startjanssenmonolithdemo.sh demoexample.jans.io MYSQL
```

The fully featured Janssen Server is now installed and ready to be used. Start configuring as needed using
[Text-based User Interface (TUI)](https://docs.jans.io/head/admin/config-guide/tui/) or
[command-line](https://docs.jans.io/head/admin/config-guide/jans-cli/)

### Installation

For the production environment, Janssen can be installed as cloud-native in a Kubernetes cluster or as a server on a single VM. Go to the [Janssen Documentation](https://docs.jans.io/head/admin/install/) to know all the installation options

## Users and Community

A BIG thanks to all the amazing contributors!! 👏 👏

There are many ways you can contribute. Of course, you can contribute code. But we also need people to write documentation and guides, to help us with testing, to answer questions on the forums and chat, to review PRs, to help us with devops and CI/CD, to provide feedback on usability, and to promote the project through outreach. Also, by sharing metrics with us, we can gain valuable insights into how the software performs in the wild.

<a href="https://digitalpublicgoods.net/">
  <img src="https://github.com/JanssenProject/jans/blob/main/docs/assets/DPGA_color_logo.png" alt="DPGA" width="113"/>
</a>

The Janssen Project community takes immense pride in the fact that Janssen Project is [recognized as a digital public good (DPG)](https://app.digitalpublicgoods.net/a/10470) by [Digital Public Good Alliance](https://digitalpublicgoods.net/). This is a validation of our social and global impact on millions of people. Today, countries and communities globally use Janssen Project to create affordable and accessible digital identity infrastructure. Please read the [announcement](https://www.linkedin.com/pulse/linux-foundation-janssen-project-recognized-digital-public/) to know more.

Building a large community is our number one goal. Please let us know what we can do to make you feel more welcome, no matter what you want to contribute.

<a href="https://github.com/JanssenProject/jans/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=JanssenProject/jans" />
</a>

## Contributing

### Code of Conduct

[Janssen code of conduct](https://docs.jans.io/head/CODE_OF_CONDUCT/) ensures that the Janssen community is a welcoming place for everyone.

### Contribution Guidelines

[Contribution guide](https://docs.jans.io/head/CONTRIBUTING/) will give you all the necessary information and `howto` to get started. Janssen community welcomes all types of contributions. Be it an interesting comment on an open issue or implementing a feature.  Welcome aboard! ✈️

## Security

### Disclosing vulnerabilities
If you think you found a security vulnerability, please refrain from posting it publicly on the forums, the chat, or GitHub. Instead, email us at security@jans.io.

Refer to [Janssen Security Policy](.github/SECURITY.md)

## Documentation

Visit [Janssen Documentation Site](https://docs.jans.io/) for documentation around current as well as previous versions.

## Design

### Design Goals

The Janssen Project is aligned with the goals of cloud native infrastructure to enable:

1. High Concurrency: For digital identity infrastructure, the number of users is not necessarily related to performance. If you have a billion users who never login, you can do this with a monolithic platform. Concurrency is hard. Janssen is designed to scale horizontally--enabling hypothetically any concurrency by adding more compute and memory.

2. Highly Available: Digital identity infrastructure is mission critical. For many applications, if you can't login, you're dead in the water. Robustness is a fundamental consideration.

3. Flexible while Upgradable: Open source gives you the freedom to modify the code. But having your own fork of the code might make it hard to upgrade--you'll have to merge changes. Janssen provides standard interfaces that make it possible to implement custom business logic in an upgrade-friendly manner.

## Governance

Janssen is a Linux Foundation project, governed according to the [charter](https://docs.jans.io/head/governance/charter/). Technical oversight of the project is the responsibility of the Technical Steering Committee ("TSC"). Day to day decision-making is in the hands of the Contributors. The TSC helps to guide the direction of the project and to improve the quality and security of the development process.

## Support

Documentation currently is a work in progress and published on [Documentation site](https://docs.jans.io/). You may want to also check Gluu Server [docs](https://gluu.org/docs), which have a lot in common with Janssen.

We prefer to have all our discussions through [GitHub Discussions](https://github.com/JanssenProject/jans/discussion) to better facilitate faster responses. However, other means are available such as the [community chat on Gitter](https://gitter.im/JanssenProject/Lobby). You can register for free there with your GitHub identity.

If you find a bug in a Janssen project, or you would like to suggest a new feature, try the [GitHub Discussions](https://github.com/JanssenProject/jans/discussion) first. If you have a "howto" or "usage" question, [raise the question or usage](https://github.com/JanssenProject/jans/discussion)!

## Releases

Below is the list of current mega releases that hold information about every single release of our services and modules:
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

## More about Janssen Project

### History

The initial code was ported by [Gluu](https://gluu.org), based on version 4.2 of its identity and access management (IAM) platform. Gluu launched in 2009 with the goal of creating an enterprise-grade open source distribution of IAM components. In 2012, Gluu started work on an OAuth Authorization Server to implement OpenID Connect, which they saw as a promising next-generation replacement for SAML. This project was called [oxAuth](https://github.com/GluuFederation/oxauth), and over time, became the core component of the Gluu Server.  Gluu has submitted many [self-certifications](https://openid.net/certification/) at the OpenID Foundation. Today, it is one of the most comprehensive OpenID Connect Providers.

In 2020, Gluu decided to democratize the governance of the oxAuth project by moving it to the Linux Foundation. The name of the project was changed from oxAuth to Janssen, to avoid any potential trademark issues. Gluu felt that a collaboration with the Linux Foundation would help to build a larger ecosystem.

### Why the name Janssen?

Pigeons (or doves if you like...) are universally regarded as a symbol of peace. But they are also fast. Powered by a handful of seeds, a well-trained racing pigeon can fly 1000 kilometers in a day. The Janssen brothers of Arendonk in Belgium bred the world's fastest family of racing pigeons. Complex open source infrastructure, like competitive animal husbandry, requires incremental improvement. Janssen racing pigeons revolutionized the sport. The Janssen Project seeks to revolutionize identity and access management.
