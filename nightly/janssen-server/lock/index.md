# Janssen Lock Guide

Janssen Lock provides a centralized control plane for domains to use [Cedar](https://www.cedarpolicy.com/en) to secure a network of distributed applications and audit the activity of both people and software. The Lock Server acts as the Policy Retrieval Point (PRP) in a distributed authorization architecture, connecting ephemeral Cedarling instances to enterprise infrastructure.

## Overview

A Lock topology consists of three main software components:

1. **[Lock Server](https://docs.jans.io/nightly/janssen-server/lock/lock-server/index.md)**: A Java Weld application that serves as the centralized Policy Retrieval Point (PRP)
1. **[Cedarling](https://docs.jans.io/nightly/cedarling/index.md)**: An embedded Java PDP that runs the [Amazon Rust Cedar Engine](https://github.com/cedar-policy/cedar) and validates JWTs
1. **Jans Auth Server**: Provides OAuth and OpenID Connect services for secure communication

## Key Features

### Centralized Policy Distribution

- **Policy Store Management**: Centralized repository for Cedar policies and schemas
- **Version Control**: Integration with external policy sources (GitHub, file systems)
- **Cache Management**: Efficient policy caching with TTL expiration

### OAuth-Protected Communication

- **Secure Endpoints**: All Lock Server endpoints protected by OAuth 2.0

### Comprehensive Audit Capabilities

- **Decision Logging**: Centralized collection of authorization decisions
- **Health Monitoring**: Real-time health status from Cedarling clients
- **Telemetry Data**: Performance metrics and usage statistics
- **Compliance Support**: Structured audit logs for regulatory requirements

### Flexible Deployment Options

- **Standalone Deployment**: Independent web server for dedicated Lock Server instances
- **Integrated Deployment**: Embedded within Jans Auth Server for unified management
- **High Availability**: Support for load-balanced, multi-instance deployments
- **Container Ready**: Docker containerization with Kubernetes support

## Architecture Overview

Lock is designed for organizations that deploy a **network of Cedarlings**. Communication in this topology is bi-directional:

- **Cedarling → Lock Server**: HTTP requests to OAuth-protected endpoints to retrieve policies and send audit data

### Authorization Model Alignment

Janssen Lock aligns with established authorization frameworks including [RFC 2904](https://datatracker.ietf.org/doc/html/rfc2904#section-4.4) and [XACML](https://docs.oasis-open.org/xacml/3.0/xacml-3.0-core-spec-cos01-en.html):

| Role                        | Acronym | Lock Component  | Description                                                 |
| --------------------------- | ------- | --------------- | ----------------------------------------------------------- |
| Policy Decision Point       | PDP     | Cedarling       | Evaluates authorization policies and makes access decisions |
| Policy Information Point    | PIP     | JWT Tokens      | Provides entity data for policy evaluation                  |
| Policy Enforcement Point    | PEP     | Application     | Enforces authorization decisions from Cedarling             |
| Policy Administration Point | PAP     | Jans Config API | Administrative interface for policy management              |
| Policy Retrieval Point      | PRP     | Lock Server     | Centralized repository and distribution point for policies  |

## Policy Store Structure

The Policy Store is a JSON document that contains all data required for Cedarling policy evaluation and JWT token verification. For detailed information about the Policy Store format, structure, and configuration options, see the [Cedarling Policy Store documentation](https://docs.jans.io/nightly/cedarling/reference/cedarling-policy-store/index.md).

## Security Model

### OAuth 2.0 Protection

All Lock Server endpoints are protected using OAuth 2.0 with specific scopes:

- `https://jans.io/oauth/lock/policy-store.read` - Policy store retrieval
- `https://jans.io/oauth/lock/log.write` - Audit log submission
- `https://jans.io/oauth/lock/health.write` - Health status reporting
- `https://jans.io/oauth/lock/telemetry.write` - Telemetry data submission

### Authorization Flow

1. **Client Registration**: Cedarling clients register using Software Statement Assertion (SSA)
1. **Token Acquisition**: Clients obtain access tokens with required scopes
1. **Request Authorization**: Lock Server validates tokens and uses embedded Cedarling for authorization
1. **Resource Access**: Authorized requests receive policy store data or accept audit submissions

### Embedded Cedarling Authorization

The Lock Server uses its own embedded Cedarling instance to make authorization decisions:

- **Policy Store Access**: Determines which clients can access specific policy stores
- **Audit Submission**: Controls which clients can submit audit data
- **Administrative Operations**: Protects configuration and management endpoints

## Getting Started

### Quick Start

1. **Deploy Lock Server**: Choose standalone or integrated deployment
1. **Configure Policy Store**: Set up policy sources and trusted issuers
1. **Register Cedarling Clients**: Use SSA for secure client registration
1. **Test Policy Retrieval**: Verify Cedarling can fetch policies
1. **Monitor Operations**: Use CLI/TUI for ongoing management

### Deployment Options

- **[Standalone Deployment](https://docs.jans.io/nightly/janssen-server/lock/lock-server/#standalone-deployment)**: Independent Lock Server instance
- **[Integrated Deployment](https://docs.jans.io/nightly/janssen-server/lock/lock-server/#integrated-deployment)**: Embedded in Jans Auth Server
- **[High Availability Setup](https://docs.jans.io/nightly/janssen-server/lock/lock-server/#high-availability-setup)**: Load-balanced multi-instance deployment

### Management Interfaces

- **[CLI Commands](https://docs.jans.io/nightly/janssen-server/lock/lock-server/#cli-management)**: Command-line policy and client management
- **[TUI Interface](https://docs.jans.io/nightly/janssen-server/lock/lock-server/#tui-interface)**: Text-based user interface for administration
- **[REST API](https://docs.jans.io/nightly/janssen-server/lock/lock-server/#rest-api-endpoints)**: Programmatic access to Lock Server functionality

## Use Cases

### Enterprise Application Security

- Centralized policy management for microservices
- Consistent authorization across distributed applications
- Real-time policy updates without service restarts

### Compliance and Auditing

- Centralized audit log collection
- Regulatory compliance reporting
- Security event monitoring and alerting

### Multi-tenant Environments

- Tenant-specific policy isolation
- Scalable policy distribution
- Per-tenant audit and monitoring

### Cloud-Native Deployments

- Kubernetes-native policy distribution
- Container-based Cedarling deployment
- Service mesh integration

## More Information

- **[Lock Server Configuration and Operation](https://docs.jans.io/nightly/janssen-server/lock/lock-server/index.md)**: Detailed setup and management guide
- **[Cedarling Documentation](https://docs.jans.io/nightly/cedarling/index.md)**: Client-side authorization engine
- **[Cedar Policy Language](https://docs.cedarpolicy.com/)**: Official Cedar documentation
- **[OAuth 2.0 Integration](https://docs.jans.io/nightly/janssen-server/auth-server/oauth-features/index.md)**: OAuth implementation details
- **[Jans Auth Server](https://docs.jans.io/nightly/janssen-server/index.md)**: Complete identity platform documentation
