# Shibboleth Identity Provider

## Overview

The Janssen Shibboleth IDP module provides SAML 2.0 Identity Provider functionality integrated with the Janssen Auth Server. This integration enables organizations to provide SAML-based Single Sign-On (SSO) while leveraging Janssen's powerful authentication capabilities including multi-factor authentication, adaptive authentication, and customizable authentication flows.

## Key Features

- **SAML 2.0 Identity Provider**: Full SAML 2.0 SSO support based on Shibboleth IDP 5.1.6
- **Janssen Auth Integration**: Authentication delegates to Janssen Auth Server via OAuth 2.0/OIDC
- **Flexible Deployment**: Available as Docker container, Helm chart, or Linux VM installation
- **Centralized Management**: Configure via Config API, CLI-TUI, or Terraform
- **Multi-factor Authentication**: Leverage all Janssen authentication methods for SAML SSO

## Architecture

The Shibboleth IDP acts as a SAML Identity Provider while delegating actual user authentication to the Janssen Auth Server. This architecture provides:

1. **Separation of Concerns**: SAML protocol handling is separate from authentication logic
1. **Unified Authentication**: All authentication methods available in Janssen work with SAML SPs
1. **Centralized User Management**: User data remains in Janssen's LDAP/database

### Authentication Flow

```
┌─────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   SAML SP   │────▶│  Shibboleth IDP │────▶│ Janssen Auth    │
│             │     │                 │     │ Server          │
└─────────────┘     └─────────────────┘     └─────────────────┘
       │                    │                       │
       │  1. SAML Request   │                       │
       │───────────────────▶│                       │
       │                    │  2. OAuth Redirect    │
       │                    │──────────────────────▶│
       │                    │                       │
       │                    │  3. User Authenticates│
       │                    │                       │
       │                    │  4. OAuth Callback    │
       │                    │◀──────────────────────│
       │                    │                       │
       │  5. SAML Response  │                       │
       │◀───────────────────│                       │
```

1. Service Provider sends SAML AuthnRequest to Shibboleth IDP
1. IDP redirects user to Janssen Auth Server for authentication
1. User authenticates using configured authentication method
1. Auth Server returns OAuth tokens to IDP via callback
1. IDP issues SAML assertion to Service Provider

## Components

| Component                | Description                                                        |
| ------------------------ | ------------------------------------------------------------------ |
| `jans-shibboleth-idp`    | Maven module with IDP configuration and authentication integration |
| `docker-jans-shibboleth` | Docker container image for containerized deployment                |
| Helm Chart               | Kubernetes deployment via `charts/janssen/charts/shibboleth-idp`   |
| Config API Plugin        | REST API for managing IDP configuration                            |
| Terraform Resources      | Infrastructure as Code support                                     |
| Linux Installer          | VM-based installation support                                      |

## Version Information

- **Shibboleth IDP Version**: 5.1.6 (released August 26, 2024)
- **Servlet Container**: Jetty 12.0.25
- **Java Version**: OpenJDK 11

## Quick Start

### Docker Deployment

```
docker run -d \
  -p 8080:8080 \
  -e CN_HOSTNAME=your-hostname.example.com \
  -e CN_AUTH_SERVER_URL=https://your-hostname.example.com \
  janssenproject/shibboleth:5.1.6_dev
```

### Helm Deployment

```
helm install janssen janssen/janssen \
  --set shibboleth-idp.enabled=true \
  --set global.fqdn=your-hostname.example.com
```

## Documentation

- [Installation Guide](https://docs.jans.io/head/janssen-server/shibboleth-idp/installation/index.md) - Detailed installation instructions
- [Configuration](https://docs.jans.io/head/janssen-server/shibboleth-idp/configuration/index.md) - IDP and authentication configuration
- [Helm Deployment](https://docs.jans.io/head/janssen-server/shibboleth-idp/helm-deployment/index.md) - Kubernetes deployment guide
- [Config API](https://docs.jans.io/head/janssen-server/shibboleth-idp/config-api/index.md) - REST API for configuration management
- [Terraform](https://docs.jans.io/head/janssen-server/shibboleth-idp/terraform/index.md) - Infrastructure as Code deployment

## Related Documentation

- [SAML Recipes](https://docs.jans.io/head/janssen-server/recipes/saml/index.md) - SAML integration recipes
- [Auth Server](https://docs.jans.io/head/janssen-server/auth-server/index.md) - Janssen Auth Server documentation
- [Kubernetes Operations](https://docs.jans.io/head/janssen-server/kubernetes-ops/index.md) - Kubernetes deployment guide
