#!/usr/bin/env python3

import base64
import logging
import os
from pathlib import Path

logger = logging.getLogger("shibboleth")

SHIBBOLETH_HOME = os.environ.get("SHIBBOLETH_HOME", "/opt/shibboleth-idp")
SEALER_PASSWORD_FILE = f"{SHIBBOLETH_HOME}/credentials/.sealer_password"


class ShibbolethSetup:
    def __init__(self, manager) -> None:
        self.manager = manager
        self.hostname = manager.config.get("hostname")
        self.jans_auth_url = f"https://{self.hostname}"

    def configure(self) -> None:
        self.setup_directories()
        self.configure_jetty()
        self.configure_credentials()
        self.configure_idp_properties()
        self.configure_relying_party()
        self.configure_attribute_resolver()
        self.configure_metadata()
        self.configure_jans_authentication()

    def setup_directories(self) -> None:
        logger.info("Setting up Shibboleth directories")

        dirs = [
            f"{SHIBBOLETH_HOME}/conf",
            f"{SHIBBOLETH_HOME}/conf/authn",
            f"{SHIBBOLETH_HOME}/credentials",
            f"{SHIBBOLETH_HOME}/metadata",
            f"{SHIBBOLETH_HOME}/logs",
            f"{SHIBBOLETH_HOME}/jetty",
            f"{SHIBBOLETH_HOME}/jetty/webapps",
        ]

        for d in dirs:
            Path(d).mkdir(parents=True, exist_ok=True)

    def configure_jetty(self) -> None:
        logger.info("Configuring Jetty for Shibboleth IDP")

        jetty_base = os.environ.get("JETTY_BASE", f"{SHIBBOLETH_HOME}/jetty")

        start_ini = """
--module=server
--module=http
--module=deploy
--module=webapp
--module=resources
jetty.http.port=8080
jetty.deploy.scanInterval=0
"""

        Path(f"{jetty_base}/start.ini").write_text(start_ini.strip())

        resources_dir = Path(f"{jetty_base}/resources")
        resources_dir.mkdir(parents=True, exist_ok=True)

    def configure_credentials(self) -> None:
        logger.info("Configuring Shibboleth credentials")

        credentials_dir = f"{SHIBBOLETH_HOME}/credentials"

        idp_signing_key = self.manager.secret.get("shibboleth_idp_signing_key")
        idp_signing_cert = self.manager.secret.get("shibboleth_idp_signing_cert")

        if idp_signing_key:
            key_path = Path(f"{credentials_dir}/idp-signing.key")
            key_path.write_text(idp_signing_key)
            os.chmod(key_path, 0o600)
        if idp_signing_cert:
            Path(f"{credentials_dir}/idp-signing.crt").write_text(idp_signing_cert)

        idp_encryption_key = self.manager.secret.get("shibboleth_idp_encryption_key")
        idp_encryption_cert = self.manager.secret.get("shibboleth_idp_encryption_cert")

        if idp_encryption_key:
            key_path = Path(f"{credentials_dir}/idp-encryption.key")
            key_path.write_text(idp_encryption_key)
            os.chmod(key_path, 0o600)
        if idp_encryption_cert:
            Path(f"{credentials_dir}/idp-encryption.crt").write_text(idp_encryption_cert)

        sealer_key = self.manager.secret.get("shibboleth_sealer_key")
        if sealer_key:
            sealer_path = Path(f"{credentials_dir}/sealer.jks")
            try:
                sealer_bytes = base64.b64decode(sealer_key)
                sealer_path.write_bytes(sealer_bytes)
            except Exception:
                sealer_path.write_bytes(sealer_key.encode() if isinstance(sealer_key, str) else sealer_key)
            os.chmod(sealer_path, 0o600)

    def _get_sealer_password(self) -> str:
        """Get sealer password from environment, file, or secret.

        In production (CN_DEV_MODE != 'true'), fails fast if no password is configured.
        """
        sealer_password = os.environ.get("IDP_SEALER_PASSWORD")
        if sealer_password:
            return sealer_password

        if os.path.exists(SEALER_PASSWORD_FILE):
            with open(SEALER_PASSWORD_FILE) as f:
                password = f.read().strip()
                if password:
                    return password

        sealer_password = self.manager.secret.get("shibboleth_sealer_password")
        if sealer_password:
            return sealer_password

        dev_mode = os.environ.get("CN_DEV_MODE", "false").lower() == "true"
        if dev_mode:
            logger.warning(
                "Using default sealer password in dev mode. "
                "Set IDP_SEALER_PASSWORD or shibboleth_sealer_password secret for production."
            )
            return "changeit"

        raise RuntimeError(
            "Sealer password not configured. Set IDP_SEALER_PASSWORD environment variable "
            "or shibboleth_sealer_password secret. For development, set CN_DEV_MODE=true "
            "to use the default password."
        )

    def configure_idp_properties(self) -> None:
        logger.info("Configuring idp.properties")

        idp_entity_id = f"https://{self.hostname}/idp/shibboleth"
        idp_scope = self.hostname.split(".", 1)[-1] if "." in self.hostname else self.hostname
        sealer_password = self._get_sealer_password()

        props = f"""
idp.entityID={idp_entity_id}
idp.scope={idp_scope}
idp.home={SHIBBOLETH_HOME}
idp.sealer.storePassword={sealer_password}
idp.sealer.keyPassword={sealer_password}
idp.signing.key=%{{idp.home}}/credentials/idp-signing.key
idp.signing.cert=%{{idp.home}}/credentials/idp-signing.crt
idp.encryption.key=%{{idp.home}}/credentials/idp-encryption.key
idp.encryption.cert=%{{idp.home}}/credentials/idp-encryption.crt
idp.consent.StorageService=shibboleth.JPAStorageService
idp.session.StorageService=shibboleth.StorageService
idp.replayCache.StorageService=shibboleth.StorageService
idp.artifact.StorageService=shibboleth.StorageService
idp.logout.elaboration=true
idp.logout.authenticated=true
"""

        props_path = Path(f"{SHIBBOLETH_HOME}/conf/idp.properties")
        props_path.write_text(props.strip())
        os.chmod(props_path, 0o600)

    def configure_relying_party(self) -> None:
        logger.info("Configuring relying-party.xml")

        relying_party_xml = """<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:util="http://www.springframework.org/schema/util"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
                           http://www.springframework.org/schema/util http://www.springframework.org/schema/util/spring-util.xsd">

    <bean id="shibboleth.UnverifiedRelyingParty" parent="RelyingParty" />

    <bean id="shibboleth.DefaultRelyingParty" parent="RelyingParty">
        <property name="profileConfigurations">
            <list>
                <bean parent="Shibboleth.SSO" />
                <bean parent="SAML1.AttributeQuery" />
                <bean parent="SAML1.ArtifactResolution" />
                <bean parent="SAML2.SSO" />
                <bean parent="SAML2.ECP" />
                <bean parent="SAML2.Logout" />
                <bean parent="SAML2.AttributeQuery" />
                <bean parent="SAML2.ArtifactResolution" />
            </list>
        </property>
    </bean>

</beans>
"""

        Path(f"{SHIBBOLETH_HOME}/conf/relying-party.xml").write_text(relying_party_xml)

    def configure_attribute_resolver(self) -> None:
        logger.info("Configuring attribute-resolver.xml")

        attr_resolver_xml = """<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:resolver="urn:mace:shibboleth:2.0:resolver"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
                           urn:mace:shibboleth:2.0:resolver http://shibboleth.net/schema/idp/shibboleth-attribute-resolver.xsd">

    <resolver:AttributeDefinition id="uid" xsi:type="resolver:Simple">
        <resolver:Dependency ref="jansSubject" />
        <resolver:AttributeEncoder xsi:type="resolver:SAML2String" name="urn:oid:0.9.2342.19200300.100.1.1" friendlyName="uid" />
    </resolver:AttributeDefinition>

    <resolver:AttributeDefinition id="mail" xsi:type="resolver:Simple">
        <resolver:Dependency ref="jansSubject" />
        <resolver:AttributeEncoder xsi:type="resolver:SAML2String" name="urn:oid:0.9.2342.19200300.100.1.3" friendlyName="mail" />
    </resolver:AttributeDefinition>

    <resolver:AttributeDefinition id="displayName" xsi:type="resolver:Simple">
        <resolver:Dependency ref="jansSubject" />
        <resolver:AttributeEncoder xsi:type="resolver:SAML2String" name="urn:oid:2.16.840.1.113730.3.1.241" friendlyName="displayName" />
    </resolver:AttributeDefinition>

    <resolver:DataConnector id="jansSubject" xsi:type="resolver:Subject">
        <resolver:ExportAttributes>
            <resolver:Attribute>uid</resolver:Attribute>
            <resolver:Attribute>mail</resolver:Attribute>
            <resolver:Attribute>displayName</resolver:Attribute>
        </resolver:ExportAttributes>
    </resolver:DataConnector>

</beans>
"""

        Path(f"{SHIBBOLETH_HOME}/conf/attribute-resolver.xml").write_text(attr_resolver_xml)

    def configure_metadata(self) -> None:
        logger.info("Configuring metadata providers")

        metadata_providers_xml = """<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">

    <bean id="shibboleth.MetadataProviderChain" class="net.shibboleth.idp.saml.metadata.impl.ChainingMetadataProvider">
        <property name="providers">
            <list>
                <bean class="org.opensaml.saml.metadata.resolver.impl.FilesystemMetadataResolver">
                    <property name="parserPool" ref="shibboleth.ParserPool" />
                    <property name="metadataFile" value="%{idp.home}/metadata/sp-metadata.xml" />
                </bean>
            </list>
        </property>
    </bean>

</beans>
"""

        Path(f"{SHIBBOLETH_HOME}/conf/metadata-providers.xml").write_text(metadata_providers_xml)

        sp_metadata = """<?xml version="1.0" encoding="UTF-8"?>
<EntityDescriptor xmlns="urn:oasis:names:tc:SAML:2.0:metadata"
                  entityID="https://example.sp.com/shibboleth">
    <!-- SP metadata will be populated by configuration -->
</EntityDescriptor>
"""

        Path(f"{SHIBBOLETH_HOME}/metadata/sp-metadata.xml").write_text(sp_metadata)

    def configure_jans_authentication(self) -> None:
        logger.info("Configuring Janssen authentication")

        client_id = self.manager.config.get("shibboleth_idp_client_id", "")
        client_secret = self.manager.secret.get("shibboleth_idp_client_secret", "")

        jans_props = f"""
jans.auth.server.url={self.jans_auth_url}
jans.auth.client.id={client_id}
jans.auth.client.secret={client_secret}
jans.auth.redirect.uri=https://{self.hostname}/idp/Authn/Jans/callback
jans.auth.scopes=openid,profile,email
jans.auth.ssl.validation=true
"""

        props_path = Path(f"{SHIBBOLETH_HOME}/conf/jans.properties")
        props_path.write_text(jans_props.strip())
        os.chmod(props_path, 0o600)
