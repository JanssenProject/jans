# Janssen Shibboleth Identity Provider

This Docker image contains the Shibboleth Identity Provider 5.1.6 integrated with Janssen Auth Server for SAML SSO functionality.

## Overview

The Janssen Shibboleth IDP container provides:
- SAML 2.0 Single Sign-On (SSO) support
- Integration with Janssen Auth Server for authentication
- Configurable attribute release policies
- Service Provider metadata management

## Environment Variables

| Name | Description | Default | Source |
|------|-------------|---------|--------|
| `CN_CONFIG_ADAPTER` | Configuration adapter type | `kubernetes` | pycloudlib |
| `CN_SECRET_ADAPTER` | Secret adapter type | `kubernetes` | pycloudlib |
| `CN_PERSISTENCE_TYPE` | Persistence backend | `sql` | entrypoint.py |
| `CN_WAIT_MAX_TIME` | Maximum wait time for dependencies | `300` | entrypoint.py |
| `CN_WAIT_SLEEP_DURATION` | Sleep duration between checks | `10` | entrypoint.py |
| `CN_SHIBBOLETH_LOG_LEVEL` | Log level for Shibboleth | `INFO` | settings.py |
| `CN_PYCLOUDLIB_LOG_LEVEL` | Log level for pycloudlib | `INFO` | settings.py |
| `CN_JAVA_OPTIONS` | JVM options for Jetty | `-Xms256m -Xmx512m` | entrypoint.py |
| `CN_MAX_RAM_PERCENTAGE` | Maximum RAM percentage for JVM | `75` | entrypoint.sh |
| `CN_HEALTH_CHECK_INTERVAL` | Interval in seconds for health checks | `30` | healthcheck.py |
| `CN_SHIBBOLETH_PORT` | HTTP port for IDP health check | `8080` | healthcheck.py |
| `CN_DEV_MODE` | Enable development mode (allows default sealer password) | `false` | shib_setup.py |
| `IDP_SEALER_PASSWORD` | Sealer keystore password (required in production) | - | shib_setup.py |

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

# Pin pycloudlib to a specific tag/commit for reproducible builds:
docker build --build-arg JANS_PYCLOUDLIB_REF=v1.0.0 -t janssenproject/shibboleth:5.1.6_dev .
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

## Security

This image runs as a non-root user (`shibboleth`, UID 1000) for improved security. All writable directories are owned by this user.

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
