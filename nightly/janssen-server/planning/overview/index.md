# Deployment and Planning Guide

This articles and documents in this section will help you plan how to launch a digital identity service for your organization. It will hopefully answer many of the "why" questions, not just the "how" questions, setting the stage for what challenges are in scope for Janssen--and where you may need to use other software. It addresses some common questions that don't quite fit into other parts of the Janssen docs.

By covering the key features and designs, hopefully, this guide will help you understand the different deployment options available, when to use each, and how to right-size your Janssen identity services.

The audience for this deployment guide includes technical architects, designers, developers, and Janssen administrators.

Documents in this section broadly cover the following areas of planning:

### Overview & Architecture

- [Platform goals and mission](https://docs.jans.io/nightly/janssen-server/planning/platform-goal/index.md)
- [Core identity use cases](https://docs.jans.io/nightly/janssen-server/planning/use-cases/index.md)
- [Overview of Janssen components](https://docs.jans.io/nightly/janssen-server/planning/components/index.md)

### Deployment & Infrastructure

- [Deploying Janssen with Kubernetes](https://docs.jans.io/nightly/janssen-server/planning/kubernetes/index.md)
- [Deploying with a VM cluster](https://docs.jans.io/nightly/janssen-server/planning/vm-cluster/index.md)
- [Deploying on a single VM instance](https://docs.jans.io/nightly/janssen-server/planning/vm-single-instance/index.md)
- [Load balancers and HTTP ingress](https://docs.jans.io/nightly/janssen-server/planning/load-balancers/index.md)
- [DNS security and hostnames](https://docs.jans.io/nightly/janssen-server/planning/dns/index.md)
- [Database persistence options](https://docs.jans.io/nightly/janssen-server/planning/persistence/index.md)
- [Caching options and session storage](https://docs.jans.io/nightly/janssen-server/planning/caching/index.md)

### Authentication & Access Control

- [Passwordless authentication and passkeys](https://docs.jans.io/nightly/janssen-server/planning/passwordless-auth/index.md)
- [Stepped-up authentication](https://docs.jans.io/nightly/janssen-server/planning/stepped-up-auth/index.md)
- [Machine-to-machine authentication](https://docs.jans.io/nightly/janssen-server/planning/machine-to-machine/index.md)
- [Identity provider discovery (WAYF)](https://docs.jans.io/nightly/janssen-server/planning/discovery/index.md)
- [Using Auth Server as a central authentication service](https://docs.jans.io/nightly/janssen-server/planning/central-auth-service/index.md)

### Identity Governance & Administration

- [Identity Management (IDM)](https://docs.jans.io/nightly/janssen-server/planning/identity-management/index.md)
- [Identity Access Governance (IAG)](https://docs.jans.io/nightly/janssen-server/planning/identity-access-governance/index.md)
- [Role-based access management (RBAC)](https://docs.jans.io/nightly/janssen-server/planning/role-based-access-management/index.md)
- [Delegated user administration](https://docs.jans.io/nightly/janssen-server/planning/delegated-user-admin/index.md)
- [Self-service password and 2FA credential management](https://docs.jans.io/nightly/janssen-server/planning/self-service-password-2fa/index.md)

### Security & Operational Management

- [Security best practices](https://docs.jans.io/nightly/janssen-server/planning/security-best-practices/index.md)
- [JSON signing, encryption, certificates, and keys](https://docs.jans.io/nightly/janssen-server/planning/certificates-keys/index.md)
- [Managing timeouts](https://docs.jans.io/nightly/janssen-server/planning/timeout-management/index.md)
- [Benchmarking performance and scalability](https://docs.jans.io/nightly/janssen-server/planning/benchmarking/index.md)

### Multi-tenancy & Application Customization

- [Multi-tenancy architecture](https://docs.jans.io/nightly/janssen-server/planning/multi-tenancy/index.md)
- [Customization and localization](https://docs.jans.io/nightly/janssen-server/planning/customization/index.md)
- [Building an application portal](https://docs.jans.io/nightly/janssen-server/planning/application-portal/index.md)
