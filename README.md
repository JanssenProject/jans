<h1 align="center"><img src="https://github.com/JanssenProject/jans/blob/main/docs/assets/logo/janssen_project_transparent_630px_182px.png" alt="Janssen Project - cloud native identity and access management platform"></h1>

## Welcome to the Janssen Project

Janssen enables organizations to build a scalable centralized authentication and authorization service using free open source software. The components of the project include client and server implementations of the OAuth, OpenID Connect, SCIM and FIDO standards. 

**Releases**: [Latest](https://github.com/JanssenProject/jans/releases/latest) | [All](https://github.com/JanssenProject/jans/releases)

**Get Help**: [Discussions](https://github.com/JanssenProject/jans/discussions) | [Chat](https://gitter.im/JanssenProject/Lobby)

**Get Started**: [Documentation](https://docs.jans.io/) | [Quick Start](#quick-start) | [User Guides](https://docs.jans.io/head/admin/recipes/)

**Contribute**: [Contribution Guide](https://docs.jans.io/head/CONTRIBUTING/) | [Community Docs](https://docs.jans.io/head/governance/) | [Developer Guides](https://docs.jans.io/head/developer/)

[![Artifact Hub](https://img.shields.io/endpoint?url=https://artifacthub.io/badge/repository/janssen-auth-server)](https://artifacthub.io/packages/search?repo=janssen-auth-server)
[![CII Best Practices](https://bestpractices.coreinfrastructure.org/projects/4353/badge)](https://bestpractices.coreinfrastructure.org/projects/4353)
[![Hex.pm](https://img.shields.io/hexpm/l/plug)](./LICENSE)
[![GitHub contributors](https://img.shields.io/github/contributors/janssenproject/jans)](#users-and-community)
[![Conventional Commits](https://img.shields.io/badge/Conventional%20Commits-1.0.0-%23FE5196?logo=conventionalcommits&logoColor=white)](https://conventionalcommits.org)

**Table of Contents**


- [Janssen Modules](#janssen-modules)
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



## Janssen Modules

Janssen is not a big monolith--it's a lot of services working together. Whether you deploy Janssen to a Kubernetes cluster, or you are a developer running everything on one server, it's important to understand the different parts. 

1. **[jans-auth-server](jans-auth-server)**: This component is the OAuth Authorization Server, the OpenID Connect Provider, the UMA Authorization Server--this is the main Internet facing component of Janssen. It's the service that returns tokens, JWT's and identity assertions. This service must be Internet facing.

1. **[jans-fido2](jans-fido2)**:  This component provides the server side endpoints to enroll and validate devices that use FIDO. It provides both FIDO U2F (register, authenticate) and FIDO 2 (attestation, assertion) endpoints. This service must be internet facing.

1. **[jans-config-api](jans-config-api)**: The API to configure the auth-server and other components is consolidated in this component. This service should not be Internet-facing.

1. **[jans-scim](jans-scim)**: [SCIM](http://www.simplecloud.info/) is JSON/REST API to manage user data. Use it to add, edit and update user information. This service should not be Internet facing.

1. **[jans-cli](jans-cli)**: This module is a command line interface for configuring the Janssen software, providing both interactive and simple single line
   options for configuration.

1. **[jans-client-api](jans-client-api)**: Middleware API to help application developers call an OAuth, OpenID or UMA server. You may wonder why this is necessary. It makes it easier for client developers to use OpenID signing and encryption features, without becoming crypto experts. This API provides some high level endpoints to do some of the heavy lifting.

1. **[jans-core](jans-core)**: This library has code that is shared across several janssen projects. You will most likely need this project when you build other Janssen components.

1. **[jans-orm](jans-orm)**: This is the library for persistence and caching implementations in Janssen. Currently, LDAP and Couchbase are supported. RDBMS is coming soon.

1. **[Agama](agama)**: Agama module offers an alternative way to build authentication flows in Janssen Server. With Agama, flows are coded in a DSL (domain specific language) designed for the sole purpose of writing web flows.

## Getting Started

### Quick Start

For development and testing purposes, the Janssen Server can be quickly installed on an Ubuntu 22.04 VM by running the command below:

```
wget https://raw.githubusercontent.com/JanssenProject/jans/main/automation/startjanssenmonolithdemo.sh && chmod u+x startjanssenmonolithdemo.sh && sudo bash startjanssenmonolithdemo.sh demoexample.jans.io MYSQL
```

Full featured Janssen Server is now installed and ready to be used. See [Quick-Start Guide](https://docs.jans.io/head/admin/install/vm-install/quick-start-install/) to know how to access and start configuring.


### Installation

For production environment, Janssen can be installed as cloud-native in a Kubernetes cluster or as a server on a single VM. Go to the [Janssen Documentation](https://docs.jans.io/head/admin/install/) to know all the installation options



## Users and Community

A BIG thanks to all amazing contributors!! 👏 👏

There are many ways you can contribute. Of course, you can contribute code. But we also need people to write documentation and guides, to help us with testing, to answer questions on the forums and chat, to review PR's, to help us with devops and CI/CD, to provide feedback on usability, and to promote the project through outreach. Also, by sharing metrics with us, we can gain valuable insights into how the software performs in the wild. 

Building a large community is our number one goal. Please let us know what we can do to make you feel more welcome, no matter what you want to contribute.

<a href="https://github.com/JanssenProject/jans/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=JanssenProject/jans" />
</a>

## Contributing

### Code of Conduct

[Janssen code of conduct](https://docs.jans.io/head/CODE_OF_CONDUCT/) ensures that Janssen community is a welcoming place for everyone. 

### Contribution Guidelines

[Contribution guide](https://docs.jans.io/head/CONTRIBUTING/) will give you all necessary information and `howto` to get started. Janssen community welcomes all types of contributions. Be it an interesting comment on an open issue or implementing a feature.  Welcome aboard! ✈️ 

## Security

### Disclosing vulnerabilities
If you think you found a security vulnerability, please refrain from posting it publicly on the forums, the chat, or GitHub. Instead, email us on security@jans.io.

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

Below are the list of current mega releases that hold information about each single release of our services and modules:

- [v1.0.2](https://github.com/JanssenProject/jans/releases/tag/v1.0.2)
- [v1.0.1](https://github.com/JanssenProject/jans/releases/tag/v1.0.1)
- [v1.0.0](https://github.com/JanssenProject/jans/releases/tag/v1.0.0)
- [v1.0.0-beta.16](https://github.com/JanssenProject/jans/releases/tag/v1.0.0-beta.16)
- [v1.0.0-beta.15](https://github.com/JanssenProject/jans/releases/tag/v1.0.0-beta.15)

## More about Janssen Project

### History

The initial code was ported by [Gluu](https://gluu.org), based on version 4.2 of its identity and access management (IAM) platform. Gluu launched in 2009 with the goal of creating an enterprise-grade open source distribution of IAM components. In 2012, Gluu started work on an OAuth Authorization Server to implement OpenID Connect, which they saw as a promising next-generation replacement for SAML. This project was called [oxAuth](https://github.com/GluuFederation/oxauth), and over time, became the core component of the Gluu Server.  Gluu has submitted many [self-certifications](https://openid.net/certification/) at the OpenID Foundation. Today, it is  one of the most comprehensive OpenID Connect Providers.

In 2020, Gluu decided to democratize the governance of the oxAuth project by moving it to the Linux Foundation. The name of the project was changed from oxAuth to Janssen, to avoid any potential trademark issues. Gluu felt that a collaboration with the Linux Foundation would help to build a larger ecosystem.

### Why the name Janssen?

Pigeons (or doves if you like...) are universally regarded as a symbol of peace. But they are also fast. Powered by a handful of seeds, a well-trained racing pigeon can fly 1000 kilometers in a day. The Janssen brothers of Arendonk in Belgium bred the world's fastest family of racing pigeons. Complex open source infrastructure, like competitive animal husbandry, requires incremental improvement. Janssen racing pigeons revolutionized the sport. The Janssen Project seeks to revolutionize identity and access management.
