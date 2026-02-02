# Janssen Shibboleth Identity Provider

This Docker image contains the Shibboleth Identity Provider 5.1.6 integrated with Janssen Auth Server for SAML SSO functionality.

## Overview

The Janssen Shibboleth IDP container provides:
- SAML 2.0 Single Sign-On (SSO) support
- Integration with Janssen Auth Server for authentication
- Configurable attribute release policies
- Service Provider metadata management

## Environment Variables

| Name | Description | Default |
|------|-------------|---------|
| `CN_CONFIG_ADAPTER` | Configuration adapter type | `kubernetes` |
| `CN_SECRET_ADAPTER` | Secret adapter type | `kubernetes` |
| `CN_PERSISTENCE_TYPE` | Persistence backend | `sql` |
| `CN_WAIT_MAX_TIME` | Maximum wait time for dependencies | `300` |
| `CN_WAIT_SLEEP_DURATION` | Sleep duration between checks | `10` |
| `CN_SHIBBOLETH_LOG_LEVEL` | Log level for Shibboleth | `INFO` |

## Volumes

| Path | Description |
|------|-------------|
| `/opt/shibboleth-idp/conf` | Configuration files |
| `/opt/shibboleth-idp/credentials` | Signing and encryption keys |
| `/opt/shibboleth-idp/metadata` | SAML metadata files |
| `/opt/shibboleth-idp/logs` | Log files |

## Ports

| Port | Description |
|------|-------------|
| `8080` | HTTP port for IDP |

## Building

```bash
docker build -t janssenproject/shibboleth:5.1.6_dev .
```

## Running

```bash
docker run -d \
  --name jans-shibboleth \
  -p 8080:8080 \
  -e CN_CONFIG_ADAPTER=consul \
  -e CN_SECRET_ADAPTER=vault \
  janssenproject/shibboleth:5.1.6_dev
```

## Integration with Janssen

The Shibboleth IDP delegates authentication to Janssen Auth Server using OAuth 2.0/OpenID Connect. When a user attempts SAML SSO:

1. User initiates SAML SSO to the IDP
2. IDP redirects user to Janssen Auth Server for authentication
3. User authenticates via Janssen Auth
4. Janssen Auth redirects back with authorization code
5. IDP exchanges code for tokens and extracts user identity
6. IDP issues SAML assertion to the Service Provider

## Configuration

### Janssen Auth Client

Create an OIDC client in Janssen with:
- Response types: `code`
- Grant types: `authorization_code`
- Redirect URI: `https://<hostname>/idp/Authn/Jans/callback`
- Scopes: `openid`, `profile`, `email`

### Service Provider Metadata

Upload SP metadata to `/opt/shibboleth-idp/metadata/` directory.

## License

Apache License 2.0
