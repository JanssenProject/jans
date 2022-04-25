# Janssen Developer - Frequently Asked Questions

This documents captures frequenty asked questions by new community members and developers. Information here may not be exhaustive or complete in nature. Always refer to Janssen Project [README](https://github.com/JanssenProject/jans/blob/main/README.md) for latest updates.

## What is Janssen Project
Start by going through links below:
- [Janssen Project README](https://github.com/JanssenProject/jans/blob/main/README.md)
- [Linux Foundation Press Release](https://www.linuxfoundation.org/press-release/the-janssen-project-takes-on-worlds-most-demanding-digital-trust-challenges-at-linux-foundation/)

## How do I install Janssen server and see it in action
For setting up a development environment, it is recommended to install Janssen server on a local VM. VM can be created using tools like VMWare workstation Player or any other tool. Head over to [Janssen Installation Guide](https://github.com/JanssenProject/jans/wiki/Install-Jans-on-a-VM) for installation instructions.
Once it is confirmed that Janssen Server is installed properly, visit use-case guides to implement one of the usecase. Initially, try to setup a simpler use-case like implementing [authentication using mod-auth-openidc](https://github.com/JanssenProject/jans/blob/main/docs/user/how-to/authn-with-apache-reverse-proxy.md).

## How do I interact with Janssen Server?
Janssen Server provides interactive CLI base tool `jans-cli`. Read [quick start guide](https://github.com/JanssenProject/jans/tree/main/jans-cli#quick-start) and [detailed documentation](https://github.com/JanssenProject/jans/tree/main/jans-cli/docs) for complete list of available options.

## Where do I find Janssen server logs?
- Ubuntu
  - Installation and Setup logs: `/opt/jans/jans-setup/logs/`
  - Module logs: `/opt/jans/jetty/<module-name>/logs/`

## I want to contribute to Janssen Project, how do I do that?
[Contributing](https://github.com/JanssenProject/jans/blob/main/docs/community/CONTRIBUTING.md) guidelines has all the details required to start contributing to the Janssen Project. 

## How do I understand existing Janssen server code?
Leverage [technical documentation](https://github.com/JanssenProject/jans/tree/main/docs/technical) to understand high-level module design, interactions. Use [Code](https://github.com/JanssenProject/jans/tree/main/docs/code) documents to gain understanding of code at granular level.

## I have installed Janssen server, where can I see it installed in on file system?
On Ubuntu system, most of the Janssen server artifacts are placed under `/opt`, and `/opt/jans`.  