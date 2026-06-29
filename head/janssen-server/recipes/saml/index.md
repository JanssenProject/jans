# SAML Recipes

This section contains recipes and guides for SAML-based Single Sign-On (SSO) with the Janssen Project.

## Shibboleth Identity Provider

The Janssen Project includes an integrated Shibboleth IDP for SAML 2.0 Identity Provider functionality. The Shibboleth IDP delegates authentication to the Janssen Auth Server, enabling all Janssen authentication methods for SAML-based SSO.

### Quick Links

- [Shibboleth IDP Overview](https://docs.jans.io/head/janssen-server/shibboleth-idp/index.md)
- [Installation Guide](https://docs.jans.io/head/janssen-server/shibboleth-idp/installation/index.md)
- [Configuration](https://docs.jans.io/head/janssen-server/shibboleth-idp/configuration/index.md)
- [Helm Deployment](https://docs.jans.io/head/janssen-server/shibboleth-idp/helm-deployment/index.md)
- [Config API](https://docs.jans.io/head/janssen-server/shibboleth-idp/config-api/index.md)
- [Terraform Provider](https://docs.jans.io/head/janssen-server/shibboleth-idp/terraform/index.md)

## SAML Topics

| Topic                                                                                              | Description                                          |
| -------------------------------------------------------------------------------------------------- | ---------------------------------------------------- |
| [Shibboleth IDP](https://docs.jans.io/head/janssen-server/shibboleth-idp/index.md)                 | SAML 2.0 Identity Provider based on Shibboleth 5.1.6 |
| [Federation](https://docs.jans.io/head/janssen-server/recipes/saml/federation/index.md)            | Joining identity federations                         |
| [IDP-Initiated SSO](https://docs.jans.io/head/janssen-server/recipes/saml/idp-init-authn/index.md) | IDP-initiated authentication flows                   |
| [SP Configuration](https://docs.jans.io/head/janssen-server/recipes/saml/sso-sp/index.md)          | Configuring Service Providers                        |

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

- [Auth Server](https://docs.jans.io/head/janssen-server/auth-server/index.md) - OAuth 2.0 and OpenID Connect
- [Agama](https://docs.jans.io/head/agama/introduction/index.md) - Custom authentication flows

Contribute

If you'd like to contribute to this document, get started with the [Contribution Guide](https://docs.jans.io/head/CONTRIBUTING/#contributing-to-the-documentation)
