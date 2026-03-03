---
tags:
  - administration
  - recipes
  - saml
---

# SAML Recipes

This section contains recipes and guides for SAML-based Single Sign-On (SSO) with the Janssen Project.

## Shibboleth Identity Provider

The Janssen Project includes an integrated Shibboleth IDP for SAML 2.0 Identity Provider functionality. The Shibboleth IDP delegates authentication to the Janssen Auth Server, enabling all Janssen authentication methods for SAML-based SSO.

### Quick Links

- [Shibboleth IDP Overview](../../shibboleth-idp/README.md)
- [Installation Guide](../../shibboleth-idp/installation.md)
- [Configuration](../../shibboleth-idp/configuration.md)
- [Helm Deployment](../../shibboleth-idp/helm-deployment.md)
- [Config API](../../shibboleth-idp/config-api.md)
- [Terraform Provider](../../shibboleth-idp/terraform.md)

## SAML Topics

| Topic | Description |
|-------|-------------|
| [Shibboleth IDP](../../shibboleth-idp/README.md) | SAML 2.0 Identity Provider based on Shibboleth 5.1.6 |
| [Federation](federation.md) | Joining identity federations |
| [IDP-Initiated SSO](idp-init-authn.md) | IDP-initiated authentication flows |
| [SP Configuration](sso-sp.md) | Configuring Service Providers |

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   SAML Service  │     │   Shibboleth    │     │   Janssen Auth  │
│   Provider      │────▶│   IDP           │────▶│   Server        │
└─────────────────┘     └─────────────────┘     └─────────────────┘
        │                       │                       │
        │   SAML AuthnRequest   │   OAuth/OIDC          │
        │──────────────────────▶│──────────────────────▶│
        │                       │                       │
        │                       │◀──────────────────────│
        │   SAML Response       │   User Identity       │
        │◀──────────────────────│                       │
```

## Use Cases

### Enterprise SSO
Use the Shibboleth IDP to provide SAML SSO for enterprise applications that require SAML authentication, such as:
- Salesforce
- Box
- ServiceNow
- Microsoft 365 (via SAML)
- Custom enterprise applications

### Federation
Join identity federations like InCommon or eduGAIN to enable cross-organizational authentication.

### Legacy Application Support
Support legacy applications that only understand SAML while maintaining modern authentication at the identity provider.

## Related Documentation

- [Auth Server](../../auth-server/README.md) - OAuth 2.0 and OpenID Connect
- [Agama](../../../agama/introduction.md) - Custom authentication flows

!!! Contribute
    If you'd like to contribute to this document, get started with the [Contribution Guide](https://docs.jans.io/head/CONTRIBUTING/#contributing-to-the-documentation)
