[![CII Best Practices](https://bestpractices.coreinfrastructure.org/projects/4353/badge)](https://bestpractices.coreinfrastructure.org/projects/4353)

![Janssen Logo](https://github.com/JanssenProject/jans/blob/main/docs/logo/janssen_project_transparent_630px_182px.png)

## Welcome to the Janssen Project

Janssen enables organizations to build a scalable centralized authentication and authorization service using free open source software. The components of the project include client and server implementations of the OAuth, OpenID Connect, SCIM and FIDO standards. New digital identity components may be added as the technology evolves.

## Quick Start

Try first, ask questions later? Go to the [Janssen Project Wiki](https://github.com/JanssenProject/jans/wiki/) right now!

## Project Goal

Using the Janssen Project components, you can build a world class cloud native identity and access management ("IAM") platform. But why should you?
1. You have economies of scale, and outsourcing this critical infrastructure to a third party does not make sense
1. You need to embed this component in your product or solution
1. The privacy or security profile of a hosted solution is not acceptable
1. You need more freedom to customize

Through the Janssen project, we can coalesce a community. Open source development results in more innovation and better code. And ultimately, more trust in the code--*trust* is foundational to digital identity infrastructure.

## Project Structure

Janssen is a Linux Foundation project, governed according to the [charter](./docs/community/charter.md). Technical oversight of the project is the responsibility of the Technical Steering Committee ("TSC"). Day to day decision making is in the hands of the Contributors. The TSC helps to guide the direction of the project and to improve the quality and security of the development process.

## Design Goals

The Janssen Project is aligned with the goals of cloud native infrastructure to enable:

1. High Concurrency: For digital identity infrastructure, the number of users is not necessarily related to performance. If you have a billion users who never login, you can do this with a monolithic platform. Concurrency is hard. Janssen is designed to scale horizontally--enabling hypothetically any concurrency by adding more compute and memory.

2. Highly Available: Digital identity infrastructure is mission critical. For many applications, if you can't login, you're dead in the water. Robustness is a fundamental consideration.

3. Flexible while Upgradable: Open source gives you the freedom to modify the code. But having your own fork of the code might make it hard to upgrade--you'll have to merge changes. Janssen provides standard interfaces that make it possible to implement custom business logic in an upgrade-friendly manner.

## History

The initial code was ported by [Gluu](https://gluu.org), based on version 4.2 of it's identity and access management (IAM) platform. Gluu launched in 2009 with the goal of creating an enterprise-grade open source distribution of IAM components. In 2012, Gluu started work on an OAuth Authorization Server to implement OpenID Connect, which they saw as a promising next-generation replacement for SAML. This project was called [oxAuth](https://github.com/GluuFederation/oxauth), and over time, became the core component of the Gluu Server.  Gluu has submitted many [self-certifications](https://openid.net/certification/) at the OpenID Foundation. Today, it is  one of the most comprehensive OpenID Connect Providers.

In 2020, Gluu decided to democratize the governance of the oxAuth project by moving it to the Linux Foundation. The name of the project was changed from oxAuth to Janssen, to avoid any potential trademark issues. Gluu felt that a collaboration with the Linux Foundation would help to build a larger ecosystem.

## Why the name Janssen?

Pigeons (or doves if you like...) are universally regarded as a symbol of peace. But they are also fast. Powered by a handful of seeds, a well trained racing pigeon can fly 1000 kilometers in a day. The Janssen brothers of Arendonk in Belgium bred the world's fastest family of racing pigeons. Complex open source infrastructure, like competitive animal husbandry, requires incremental improvement. Janssen racing pigeons revolutionized the sport. The Janssen Project seeks to revolutionize identity and access management.

## Support

Documentation currently is a work in progress. Draft pages are currently available on [Janssen Project Wiki](https://github.com/JanssenProject/jans/wiki/). You may want to also check Gluu Server [docs](https://gluu.org/docs), which have a lot in common with Janssen.

We have setup a [community chat on Gitter](https://gitter.im/JanssenProject/Lobby). You can register for free their with your Github identity.

You can subscribe to the [Janssen Google Group](https://groups.google.com/u/2/g/janssen_project)
and post messages there.

If you find a bug in a Janssen project, or you would like to suggest a new feature, try the chat first. Then raise an issue on the respective repository. If you have a "howto" or "usage" question, [raise the issue](https://github.com/JanssenProject/jans/issues)! 

