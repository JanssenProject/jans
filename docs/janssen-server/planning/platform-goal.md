---
tags:
  - administration
  - planning
  - platform-goals
---

# Mission

Make it easier for organizations to innovate and scale open source,
standards-based identity services.

# Scale

Using Janssen Project software, organizations can build *runtime* identity
services to meet 100% of their requirements for enrollment, authentication,
credential management, and authorization. The Janssen Project includes software
tools and services for deploying, operating, testing, and developing identity
infrastructure at scale.

Scale is more than cloud-native design patterns. To truly scale, domains
have to consider the people and organizational challenges. To scale domains
need to:

* manage the complexity and cost of integrating multiple technologies,
  platforms, and vendors that may not be compatible or interoperable
* ensure the security and compliance of data and transactions
  across different systems, networks, and  jurisdictions, especially when
  dealing with sensitive or regulated information
* adapt to the changing and unpredictable demands of customers, markets, and
  deliver fast and reliable services; domains must develop and
  share the skills and talent needed to design, implement, and operate
  scalable identity infrastructure, and foster a culture of innovation and
  collaboration among teams
* balance the trade-offs between scaling up (adding more resources within a
  system) and scaling out (adding more systems across a network), and choosing
  the optimal architecture and configuration for different workloads and
  scenarios.

At the Janssen Project, we are doing our best to address all these challenges
with regard to scaling identity infrastructure.

# Security

Janssen Project tackles the most challenging security requirements. This means
keeping current with OpenID and FIDO self-certifications, creating distributions
that enables conformance with FIPS 140-2, and implementing best practices for
software development.

Janssen Project is unapologetically Java. The reason is simple: Java has the
most cryptographic implementations. Java Cryptographic Engines (JCE) are
written by many companies and open-source projects. It takes a long time
to trust a cryptographic implementation. Not only does Java have the most
options for cryptography, but it also has some of the most trusted
implementations. And when new algorithms are announced, Java SDKs are usually
first.

# Community

In 2020, Gluu contributed the code to the Linux Foundation Janssen Project.
One of the main reasons this was undertaken was to expand the size of the
community. Developers don't want to contribute to a project that might change
the license at any moment (e.g. Hashicorp, Elastic, MongoDB).

There is room for many companies to productize the Janssen Project software.
Governments can feel safe using the Janssen Project--it's been recognized as a
[Digital Public Good](https://app.digitalpublicgoods.net/a/10470). A healthy
ecosystem for infrastructure software results in long-term innovation velocity.

With the introduction of Agama, we're hoping that developers will have an
opportunity to build connectors to third-party systems and services. Gluu is
hosting the [Agama Lab Explore Catalog](https://agama-lab.gluu.org) to
help developers publish their Agama projects, making it easier for the community
to find ready-built projects that encourage the re-use of code.

# Deployment

* Developers should have distributions to enable rapid testing
for non-production instances.

* Linux system administrators should have packages for easy installation
and update of non-clustered deployments.

* Cloud-native engineers should have assets for high-end deployments,
including high concurrency and high availability.
