---
tags:
  - administration
  - planning
  - deployment
---

# Deployment and Planning Guide

This articles and documents in this section will help you plan how to launch a digital identity
service for your organization. It will hopefully answer many of the "why"
questions, not just the "how" questions, setting the stage for what challenges
are in scope for Janssen--and where you may need to use other software.  It
addresses some common questions that don't quite fit into other parts of the
Janssen docs.

By covering the key features and designs, hopefully, this guide will help you understand the different deployment options available, when to use each, and how
to right-size your Janssen identity services.

The audience for this deployment guide includes technical architects, designers,
developers, and Janssen administrators.

Documents in this section broadly cover the following areas of planning:

### Overview & Architecture
- [Platform goals and mission](./platform-goal.md)
- [Core identity use cases](./use-cases.md)
- [Overview of Janssen components](./components.md)

### Deployment & Infrastructure
- [Deploying Janssen with Kubernetes](./kubernetes.md)
- [Deploying with a VM cluster](./vm-cluster.md)
- [Deploying on a single VM instance](./vm-single-instance.md)
- [Load balancers and HTTP ingress](./load-balancers.md)
- [DNS security and hostnames](./dns.md)
- [Database persistence options](./persistence.md)
- [Caching options and session storage](./caching.md)

### Authentication & Access Control
- [Passwordless authentication and passkeys](./passwordless-auth.md)
- [Stepped-up authentication](./stepped-up-auth.md)
- [Machine-to-machine authentication](./machine-to-machine.md)
- [Identity provider discovery (WAYF)](./discovery.md)
- [Using Auth Server as a central authentication service](./central-auth-service.md)

### Identity Governance & Administration
- [Identity Management (IDM)](./identity-management.md)
- [Identity Access Governance (IAG)](./identity-access-governance.md)
- [Role-based access management (RBAC)](./role-based-access-management.md)
- [Delegated user administration](./delegated-user-admin.md)
- [Self-service password and 2FA credential management](./self-service-password-2fa.md)

### Security & Operational Management
- [Security best practices](./security-best-practices.md)
- [JSON signing, encryption, certificates, and keys](./certificates-keys.md)
- [Managing timeouts](./timeout-management.md)
- [Benchmarking performance and scalability](./benchmarking.md)

### Multi-tenancy & Application Customization
- [Multi-tenancy architecture](./multi-tenancy.md)
- [Customization and localization](./customization.md)
- [Building an application portal](./application-portal.md)

