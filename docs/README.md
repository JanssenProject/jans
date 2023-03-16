# Janssen Project Documentation

## Introduction

Janssen enables organizations to build a scalable centralized authentication and authorization service using free open source software. The components of the project include client and server implementations of the OAuth, OpenID Connect, SCIM and FIDO standards.

## Administration Guide

The Janssen Server is highly extensible and customizable. Resources for deployment, operation, and maintenance of the Janssen environment are available in the [Administration guide](admin/README.md). We recommend a holistic approach to identity, planning the environment thoroughly using the [Deployment Guide](admin/planning/platform-goal.md). For a quick start for testing, you can jump right into [Installation](admin/install/vm-install/).

## Developer Guide

There are many ways the community can contribute to the Janssen Project. Of course, you can contribute code. But we also need people to write documentation and guides, to help us with testing, to answer questions on the forums and chat, to review PRs, to help us with devops and CI/CD, to provide feedback on usability, and to promote the project through outreach. Also, by sharing metrics with us, we can gain valuable insights into how the software performs in the wild. Resources to get started are available [here](CONTRIBUTING.md).

## Governance Guide

The Janssen Project is an open source member of the Linux Foundation. Information about the project's governance can be found [here](governance/charter.md).

## Script Catalog

Interception scripts (or custom scripts) allow you to define custom business logic for various features offered by the OpenID Provider (Jans-auth server). Some examples of features which can be customized are - implementing a 2FA authentication method, consent gathering, client registration, adding business specific claims to ID token or Access token etc. Scripts can easily be upgraded and doesn't require forking the Jans Server code or re-building it.

The definitive location for scripts and their documentation is the [Script Catalog](script-catalog/README.md).

## Agama

[Agama](./admin/developer/agama/README.md) is a component of the Janssen authentication server that offers an alternative way to build web-based authentication flows. Typically, person authentication flows are defined in the server by means of jython scripts that adhere to a predefined API. With Agama, flows are coded using a DSL (domain specific language) designed for the sole purpose of writing web flows.

## Support

We prefer to have all our discussions through [GitHub Discussions](https://github.com/JanssenProject/jans/discussion) to better facilitate faster responses. However, other means are available such as the [community chat on Gitter](https://gitter.im/JanssenProject/Lobby). You can register for free there with your Github identity.

If you find a bug in a Janssen project, or you would like to suggest a new feature, try the [GitHub Discussions](https://github.com/JanssenProject/jans/discussion) first. If you have a "howto" or "usage" question, [raise the question or usage](https://github.com/JanssenProject/jans/discussion)! 

## License

The Janssen Project is licensed under the [Apache License 2.0](https://github.com/JanssenProject/jans/blob/main/LICENSE). The Janssen Server is highly extensible and can be used with a variety of other products and projects, which may fall under other licenses.

