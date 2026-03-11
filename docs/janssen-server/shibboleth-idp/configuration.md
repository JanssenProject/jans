---
tags:
  - administration
  - shibboleth
  - configuration
---

# Shibboleth IDP Configuration

This guide covers configuration of the Janssen Shibboleth IDP.

## Configuration Files

The Shibboleth IDP uses several configuration files located in `/opt/shibboleth-idp/conf/`:

| File | Purpose |
|------|---------|
| `idp.properties` | Main IDP properties |
| `ldap.properties` | LDAP connection settings |
| `authn/jans-authn.properties` | Janssen authentication settings |
| `attribute-resolver.xml` | Attribute definitions |
| `attribute-filter.xml` | Attribute release policies |
| `metadata-providers.xml` | SP metadata configuration |
| `relying-party.xml` | Relying party configuration |

## Janssen Authentication Configuration

### Basic Configuration

Edit `/opt/shibboleth-idp/conf/authn/jans-authn.properties`:

```properties
# Janssen Auth Server URL
jans.auth.server.url=https://auth.example.com

# OAuth Client Credentials
jans.auth.client.id=your-client-id
jans.auth.client.secret=your-client-secret

# OAuth Scopes
jans.auth.scopes=openid,profile,email

# Redirect URI
jans.auth.redirect.uri=https://idp.example.com/idp/Authn/Jans/callback
```

### Advanced Options

```properties
# Authentication timeout (seconds)
jans.auth.timeout=30

# Token validation
jans.auth.validate.tokens=true

# PKCE enabled
jans.auth.pkce.enabled=true

# Additional authentication parameters
jans.auth.acr.values=simple_password_auth

# Session binding
jans.auth.session.binding=true
```

## IDP Properties

### Entity ID and Scope

Edit `/opt/shibboleth-idp/conf/idp.properties`:

```properties
# IDP Entity ID
idp.entityID=https://idp.example.com/idp/shibboleth

# Scope for attributes
idp.scope=example.com

# Signing credential
idp.signing.key=/opt/shibboleth-idp/credentials/idp-signing.key
idp.signing.cert=/opt/shibboleth-idp/credentials/idp-signing.crt

# Encryption credential
idp.encryption.key=/opt/shibboleth-idp/credentials/idp-encryption.key
idp.encryption.cert=/opt/shibboleth-idp/credentials/idp-encryption.crt
```

### Session Configuration

```properties
# Session timeout (minutes)
idp.session.timeout=PT60M

# Secondary session timeout
idp.session.secondaryServiceIndex=true

# Cookie settings
idp.cookie.secure=true
idp.cookie.httpOnly=true
idp.cookie.sameSite=Lax
```

## Attribute Resolver

Configure attribute resolution in `/opt/shibboleth-idp/conf/attribute-resolver.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<AttributeResolver xmlns="urn:mace:shibboleth:2.0:resolver">

    <!-- Attributes from Janssen OAuth token -->
    <AttributeDefinition id="uid" xsi:type="Simple">
        <InputDataConnector ref="jansToken" attributeNames="sub"/>
    </AttributeDefinition>

    <AttributeDefinition id="mail" xsi:type="Simple">
        <InputDataConnector ref="jansToken" attributeNames="email"/>
    </AttributeDefinition>

    <AttributeDefinition id="displayName" xsi:type="Simple">
        <InputDataConnector ref="jansToken" attributeNames="name"/>
    </AttributeDefinition>

    <AttributeDefinition id="givenName" xsi:type="Simple">
        <InputDataConnector ref="jansToken" attributeNames="given_name"/>
    </AttributeDefinition>

    <AttributeDefinition id="sn" xsi:type="Simple">
        <InputDataConnector ref="jansToken" attributeNames="family_name"/>
    </AttributeDefinition>

    <!-- eduPerson attributes -->
    <AttributeDefinition id="eduPersonPrincipalName" xsi:type="Scoped" scope="%{idp.scope}">
        <InputDataConnector ref="jansToken" attributeNames="sub"/>
    </AttributeDefinition>

    <!-- Janssen Token Data Connector -->
    <DataConnector id="jansToken" xsi:type="JansToken"/>

</AttributeResolver>
```

## Attribute Filter

Configure attribute release in `/opt/shibboleth-idp/conf/attribute-filter.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<AttributeFilterPolicyGroup xmlns="urn:mace:shibboleth:2.0:afp">

    <!-- Release basic attributes to all SPs -->
    <AttributeFilterPolicy id="releaseToAllSPs">
        <PolicyRequirementRule xsi:type="ANY"/>
        <AttributeRule attributeID="uid">
            <PermitValueRule xsi:type="ANY"/>
        </AttributeRule>
        <AttributeRule attributeID="mail">
            <PermitValueRule xsi:type="ANY"/>
        </AttributeRule>
        <AttributeRule attributeID="displayName">
            <PermitValueRule xsi:type="ANY"/>
        </AttributeRule>
    </AttributeFilterPolicy>

    <!-- Additional attributes for specific SPs -->
    <AttributeFilterPolicy id="releaseToSpecificSP">
        <PolicyRequirementRule xsi:type="Requester" value="https://sp.example.org"/>
        <AttributeRule attributeID="givenName">
            <PermitValueRule xsi:type="ANY"/>
        </AttributeRule>
        <AttributeRule attributeID="sn">
            <PermitValueRule xsi:type="ANY"/>
        </AttributeRule>
        <AttributeRule attributeID="eduPersonPrincipalName">
            <PermitValueRule xsi:type="ANY"/>
        </AttributeRule>
    </AttributeFilterPolicy>

</AttributeFilterPolicyGroup>
```

## Metadata Providers

Configure SP metadata sources in `/opt/shibboleth-idp/conf/metadata-providers.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<MetadataProvider xmlns="urn:mace:shibboleth:2.0:metadata">

    <!-- Local metadata file -->
    <MetadataProvider id="LocalMetadata" xsi:type="FilesystemMetadataProvider"
                      metadataFile="/opt/shibboleth-idp/metadata/sp-metadata.xml"/>

    <!-- Remote metadata URL -->
    <MetadataProvider id="RemoteMetadata" xsi:type="FileBackedHTTPMetadataProvider"
                      metadataURL="https://sp.example.org/metadata"
                      backingFile="/opt/shibboleth-idp/metadata/sp-example-metadata.xml"/>

    <!-- InCommon Federation -->
    <MetadataProvider id="InCommon" xsi:type="FileBackedHTTPMetadataProvider"
                      metadataURL="https://mdq.incommon.org/entities"
                      backingFile="/opt/shibboleth-idp/metadata/incommon-metadata.xml">
        <MetadataFilter xsi:type="SignatureValidation"
                        certificateFile="/opt/shibboleth-idp/credentials/inc-md-cert.pem"/>
    </MetadataProvider>

</MetadataProvider>
```

## Logging Configuration

Configure logging in `/opt/shibboleth-idp/conf/logback.xml`:

```xml
<configuration>
    <appender name="IDP_PROCESS" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <File>/opt/shibboleth-idp/logs/idp-process.log</File>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>/opt/shibboleth-idp/logs/idp-process-%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%date{ISO8601} %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- Janssen authentication logging -->
    <logger name="io.jans.idp.authn" level="DEBUG"/>

    <root level="INFO">
        <appender-ref ref="IDP_PROCESS"/>
    </root>
</configuration>
```

## Restart After Configuration Changes

After making configuration changes:

```bash
# Docker
docker restart jans-shibboleth

# Kubernetes
kubectl rollout restart deployment/shibboleth-idp

# Linux VM
systemctl restart shibboleth-idp
# or
/opt/jetty/bin/jetty.sh restart
```
