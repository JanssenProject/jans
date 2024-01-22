# Janssen Project Documentation

## Introduction

Janssen is a distribution of open source identity components which
organizations can use to build a scalable federated authentication and
authorization service. This documentation is always work in progress. Please
help to make it better by submitting a PR if you can think of any way to
improve it!

## Administration Guide

Read the [Administration guide](admin/README.md) to learn how to deploy, operate
and maintain the Janssen components. Planning your solution using the
[Deployment Guide](admin/planning/platform-goal.md). An easy way to get started
is to try Janssen on an VM, see [Installation](admin/install/vm-install/README.md)
or check the other docs for Kubernetes based installation.

## Contribution Guide

There are many ways the community can contribute to the Janssen Project. Of course, you can contribute code. But we also need people to write documentation and guides, to help us with testing, to answer questions on the forums and chat, to review PRs, to help us with devops and CI/CD, to provide feedback on usability, and to promote the project through outreach. Also, by sharing metrics with us, we can gain valuable insights into how the software performs in the wild. Resources to get started are available [here](CONTRIBUTING.md).

## Governance Guide

The Janssen Project is chartered under the Linux Foundation. Information about
the project's governance can be found [here](governance/charter.md).

## Script Catalog

Interception scripts (or custom scripts) allow you to define custom business
logic for various features in Janssen without forking the Jans Server core
project. Interceptions scripts are available for many components, including
Auth Server, SCIM, FIDO, and Link. The definitive location for scripts and their
documentation is the [Script Catalog](admin/developer/scripts/README.md).

## Agama

[Agama](agama/introduction/README.md) is a domain specific language ("DSL")
designed for writing web flows, and a project archive format (".gama") which
stores all the code and web assets required for deployment of an Agama project
by an identity provider. Although invented by Janssen, we envision many IDP's
using Agama as a cross-vendor standard for identity orchestration.

## Support

If you have any questions about usage, post on [Jans Discussions](https://github.com/JanssenProject/jans/discussions) or try [community chat on Gitter](https://gitter.im/JanssenProject/Lobby).

If you find a bug in a Janssen project, or you would like to suggest a new
feature, you should also post on [GitHub Discussions](https://github.com/JanssenProject/jans/discussions) first. The Jans team will try to replicate the bug, or weigh the feature request versus current priorities.

## License

Most Janssen Project components are licensed under the [Apache License 2.0](https://github.com/JanssenProject/jans/blob/main/LICENSE). Janssen has some third party components,
so it's always best to check the license for each component. We won't include
any component in the distribution that has a non-[OSI](https://opensource.org/)
approved license, or a commercial trademark.

## Looking for older documentation versions?

The Janssen Project posts the last five versions of the documentation. If you are looking for older versions, you can find them unprocessed in the [docs](https://github.com/JanssenProject/jans/tree/main/docs) folder. Select the version of choice from the tag dropdown in GitHub. If you want to process them you may do so following the steps [here](contribute/testing.md#testing-documentation-changes-locally).

