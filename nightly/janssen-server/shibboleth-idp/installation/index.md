# Shibboleth IDP Installation

This guide covers installation of the Janssen Shibboleth IDP on various platforms.

## Prerequisites

Before installing the Shibboleth IDP, ensure you have:

1. **Janssen Auth Server** - A running Janssen Auth Server instance
1. **OAuth Client** - An OAuth client configured in Janssen for the IDP
1. **SSL Certificate** - Valid SSL certificate for the IDP hostname

### OAuth Client Configuration

Create an OAuth client in Janssen Auth Server with the following settings:

| Setting        | Value                                             |
| -------------- | ------------------------------------------------- |
| Client Type    | Confidential                                      |
| Grant Types    | authorization_code                                |
| Response Types | code                                              |
| Scopes         | openid, profile, email                            |
| Redirect URI   | https://your-idp-hostname/idp/Authn/Jans/callback |

## Docker Installation

### Quick Start

```
docker run -d \
  --name jans-shibboleth \
  -p 8080:8080 \
  -e CN_HOSTNAME=idp.example.com \
  -e CN_AUTH_SERVER_URL=https://auth.example.com \
  -e CN_CONFIG_ADAPTER=consul \
  -e CN_CONSUL_HOST=consul:8500 \
  janssenproject/shibboleth:<version>
```

### Environment Variables

| Variable               | Description                                                 | Required        |
| ---------------------- | ----------------------------------------------------------- | --------------- |
| `CN_HOSTNAME`          | IDP hostname                                                | Yes             |
| `CN_AUTH_SERVER_URL`   | Janssen Auth Server URL                                     | Yes             |
| `CN_CONFIG_ADAPTER`    | Configuration adapter (consul/kubernetes)                   | Yes             |
| `CN_CONSUL_HOST`       | Consul server address                                       | If using Consul |
| `CN_SECRET_ADAPTER`    | Secret adapter (vault/kubernetes)                           | Yes             |
| `SHIBBOLETH_ENTITY_ID` | IDP Entity ID (defaults to https://hostname/idp/shibboleth) | No              |
| `SHIBBOLETH_SCOPE`     | IDP scope for attributes                                    | No              |

### Docker Compose Example

```
services:
  shibboleth:
    image: janssenproject/shibboleth:<version>
    container_name: jans-shibboleth
    ports:
      - "8080:8080"
    environment:
      CN_HOSTNAME: idp.example.com
      CN_AUTH_SERVER_URL: https://auth.example.com
      CN_CONFIG_ADAPTER: consul
      CN_CONSUL_HOST: consul:8500
      CN_SECRET_ADAPTER: vault
      CN_VAULT_URL: http://vault:8200
    volumes:
      - ./certs:/etc/certs
    depends_on:
      - consul
      - vault
```

## Kubernetes Installation

See the [Helm Deployment Guide](https://docs.jans.io/nightly/janssen-server/shibboleth-idp/helm-deployment/index.md) for detailed Kubernetes installation instructions.

## Linux VM Installation

### System Requirements

- Ubuntu 22.04 LTS or RHEL 8/9
- 4 GB RAM minimum (8 GB recommended)
- 20 GB disk space
- Java 17 (installed automatically)

### Installation Steps

1. **Download the Janssen Installer**

```
wget https://github.com/JanssenProject/jans/releases/download/v5.1.6/jans-installer.pyz
chmod +x jans-installer.pyz
```

1. **Run the Installer**

```
sudo python3 jans-installer.pyz --install-shibboleth
```

1. **Configure During Installation**

The installer will prompt for:

- Janssen Auth Server URL
- OAuth client credentials
- IDP hostname and scope

### Manual Installation

For manual installation on Linux:

1. **Install Java 17**

```
# Ubuntu
apt-get install openjdk-17-jdk

# RHEL
dnf install java-17-openjdk
```

1. **Download Shibboleth IDP**

```
cd /opt
wget https://shibboleth.net/downloads/identity-provider/5.2.0/shibboleth-identity-provider-5.2.0.tar.gz
tar xzf shibboleth-identity-provider-5.2.0.tar.gz
```

1. **Install Jetty 12**

```
wget https://repo1.maven.org/maven2/org/eclipse/jetty/jetty-home/12.0.31/jetty-home-12.0.31.tar.gz
tar xzf jetty-home-12.0.31.tar.gz -C /opt
```

1. **Deploy Janssen Integration**

Copy the Janssen authentication plugin and configuration files from the `jans-shibboleth-idp` module.

1. **Configure and Start**

```
# Configure IDP
/opt/shibboleth-idp/bin/install.sh

# Start Jetty
/opt/jetty/bin/jetty.sh start
```

## Post-Installation Configuration

After installation, configure:

1. **IDP Metadata** - Download from https://your-idp/idp/shibboleth
1. **Attribute Release** - Configure attribute-filter.xml
1. **Trusted SPs** - Add Service Provider metadata

See the [Configuration Guide](https://docs.jans.io/nightly/janssen-server/shibboleth-idp/configuration/index.md) for detailed configuration instructions.

## Verification

Verify the installation:

```
# Check IDP status
curl -k https://your-idp-hostname/idp/status

# View IDP metadata
curl -k https://your-idp-hostname/idp/shibboleth
```

Expected status response:

```
{"status": "ok", "version": "5.1.6"}
```
