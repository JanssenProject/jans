---
tags:
  - administration
  - planning
---

# Project Goals

Online trust is a fundamental challenge to our digital society. The Internet has connected us. But at the same time, it has undermined trust. Digital identity starts with a connection between a person and a digital device. Identity software conveys the integrity of that connection from the user’s device to a complex web of backend services. Solving the challenge of digital identity is foundational to achieving trustworthy online security.

Nothing builds trust like source code. At the Janssen Project, we believe that
the software that powers the world's digital identity services must be open
source. And that the open source project behind digital identity must be
community governed. And that the community must have a mechanism to extend the
software to solve any digital identity challenge.

# Functional Goals

Janssen focuses on open standards for *run-time* digital identity, specifically federated digital identity protocols such as OpenID Connect and modern
client-side passwordless authentication like FIDO. The project may take on new digital identity standards as they are invented.

Organizations can make Janssen authoritative for identity, or can configure
Janssen as a consumer of identity.

Janssen is not designed to act as a workforce identity management or identity
governance platform.

# Security Goals

Janssen project seeks to tack the most challenging security requirements. This means keeping current with OpenID certifications, enabling cryptographic conformance with FIPS 140-2 and implementing best practices for software development lifecycle, including a transparent build process to enable
government and non-government organizations to trust the software.

Janssen Project is unapologetically Java. The reason is simple: Java has the
most cryptographic implementations. Java Cryptographic Engines (JCE) are
written by many companies and open source projects. It takes a long time
to trust a cryptographic implementation. Not only does Java have the most
options for cryptography, it also has some of the most trusted implementations,
and when new algorithms are announced, it frequently has code first.

# Community Goals

Open source gives you the freedom to modify the code. But having your own fork
of the code might make it hard to upgrade--you'll have to merge changes. Janssen provides standard interfaces that make it possible to implement custom business logic in an upgrade-friendly manner.

Janssen is also the home of the Agama programming language--the world's first
code specifically designed to enable developers to write secure web
authentication workflows. By lowering the bar for customization, Janssen seeks to expand the community of developers and content creators far and wide.

# Deployment goals

* Developers should have distributions to enable rapid testing
for non-production instances.

* Linux system administrators should have packages for easy installation
and update for smaller deployments.

* Cloud native deployments should be available for high end deployments,
including high concurrency and high availability. Janssen should take
advantage of auto-scaling horizontally--enabling hypothetically any concurrency
by adding more compute and memory.
