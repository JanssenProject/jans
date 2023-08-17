<h1 align="center"><img src="https://github.com/JanssenProject/jans/blob/main/docs/assets/logo/janssen_project_transparent_630px_182px.png" alt="Janssen Project - Open Source Digital Identity Infrastructure Software"></h1>

## Welcome to the Janssen Project

Linux Foundation Janssen Project is a collaboration hub for digital identity
infrastructure software. It is also the home of Agama, a domain-specific
language and archive format for interoperable identity orchestration.

Janssen software is used by domains to self-host a modern digital authentication
service. It provides a common control plane for infrastructure that is comprised
of many open-source software components. It plays well with existing IAM
infrastructure like KeyCloak and Microsoft Active Directory, as well as cloud
identity solutions like Okta. By design, Janssen also enables domains to select
*a la carte* which identity services to run.

We're proud to say that we've been recognized as a test-fail
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

1. **[jans-auth-server](jans-auth-server)**: A very complete Java OAuth
Authorization Server and an OIDF certified OpenID Connect Provider. It's the
upstream open-source used in [Gluu Flex](https://gluu.org).

1. **[jans-fido2](jans-fido2)**: A FIDO Server that enables people to
authenticate with USB, NFC, BT or platform FIDO devices.

1. **[jans-scim](jans-scim)**: [SCIM](http://www.simplecloud.info/) JSON/REST
[API](https://datatracker.ietf.org/doc/html/rfc7644#section-3.2) for identity
provisioning automation for users and Fido devices persisted in the Janssen
database.

1. **[jans-config-api](jans-config-api)**: Java Config API Server: service
provides a single control plane for all Janssen services.

1. **[jans-cli-tui](jans-cli-tui)**: Text-only tools for interactive and
single-line configuration for those who don't like long curl commands.

1. **[Agama](https://docs.jans.io/head/agama/introduction/)**: Language
reference and Java implementation. Agama offers an interoperable way to design
authentication flows, coded in a DSL purpose-built for writing identity journeys.

1. **[Jans Tarp](demos/jans-tarp)**: A fun test browser plugin for invoking
OpenID Connect authentication flows (i.e. a "test RP").

### Installation

You can install Janssen in a Kubernetes cluster or as a single VM. Check out the
[Janssen Documentation](https://docs.jans.io/head/admin/install/) for all
the details.

## Users and Community

A BIG thanks to all the amazing contributors!! 👏 👏

There are many ways you can contribute. Of course, you can contribute code. But we also need people to write documentation and guides, to help us with testing, to answer questions on the forums and chat, to review PRs, to help us with DevOps and CI/CD, to provide feedback on usability, and to promote the project through outreach. Also, by sharing metrics with us, we can gain valuable insights into how the software performs in the wild.

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

The Janssen Project is aligned with the goals of cloud-native infrastructure to enable:

1. High Concurrency: For digital identity infrastructure, the number of users is not necessarily related to performance. If you have a billion users who never login, you can do this with a monolithic platform. Concurrency is hard. Janssen is designed to scale horizontally--enabling hypothetically any concurrency by adding more compute and memory.

2. Highly Available: Digital identity infrastructure is mission-critical. For many applications, if you can't login, you're dead in the water. Robustness is a fundamental consideration.

3. Flexible while Upgradable: Open-source gives you the freedom to modify the code. But having your own fork of the code might make it hard to upgrade--you'll have to merge changes. Janssen provides standard interfaces that make it possible to implement custom business logic in an upgrade-friendly manner.

## Governance

Janssen is a Linux Foundation project, governed according to the [charter](https://docs.jans.io/head/governance/charter/). Technical oversight of the project is the responsibility of the Technical Steering Committee ("TSC"). Day-to-day decision-making is in the hands of the Contributors. The TSC helps to guide the direction of the project and to improve the quality and security of the development process.

## Support

If you find a bug in the Janssen project, would like to suggest a new feature, or
have a "howto" or "usage" question,
[GitHub Discussions](https://github.com/JanssenProject/jans/discussion) is the
main channel for community support. There is also a [community chat on Gitter](https://app.gitter.im/#/room/#JanssenProject_Lobby:gitter.im).

## Releases

Below is the list of current mega releases that hold information about every single release of our services and modules:
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

## More about Janssen Project

### History

In 2020, [Gluu](https://gluu.org) decided to give contributors a role in the
governance of its core open-source software, and collaborated with the
Linux Foundation to charter a new project, called the Janssen Project. The
Gluu team believed the Janssen Project would provide a vendor-neutral home for
the code which would help us build a bigger community.

Much of the initial software for the Janssen Project is a fork of Gluu
Server 4, which has many [OpenID self-certifications](https://openid.net/certification/), going back around ten years.

After this initial contribution, the Janssen Project developers added a new
configuration control plane, tools, documentation, packaging and deployment
assets.

### Why the name Janssen?

Pigeons (or doves if you like...) are universally regarded as a symbol of peace. But they are also fast. Powered by a handful of seeds, a well-trained racing pigeon can fly 1000 kilometers in a day. The Janssen brothers of Arendonk in Belgium bred the world's fastest family of racing pigeons. Like competitive animal husbandry, building a complex open-source infrastructure requires tenacity, and a long-term commitment to incremental improvement. Janssen racing pigeons revolutionized the sport. The Janssen Project seeks to revolutionize open-source identity and access management infrastructure.
