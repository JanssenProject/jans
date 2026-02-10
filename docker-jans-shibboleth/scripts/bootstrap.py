import os
import logging
import shutil
from pathlib import Path
from string import Template

from jans.pycloudlib import get_manager
from jans.pycloudlib.persistence import CouchbaseClient
from jans.pycloudlib.persistence import LdapClient
from jans.pycloudlib.persistence import SpannerClient
from jans.pycloudlib.persistence import SqlClient
from jans.pycloudlib.persistence import PersistenceMapper
from jans.pycloudlib.utils import cert_to_truststore
from jans.pycloudlib.utils import get_random_chars

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger("shibboleth-bootstrap")

IDP_HOME = os.environ.get("IDP_HOME", "/opt/shibboleth-idp")
JETTY_BASE = os.environ.get("JETTY_BASE", "/opt/jans/jetty")
SEALER_PASSWORD_FILE = f"{IDP_HOME}/credentials/.sealer_password"


def get_persistence_client():
    persistence_type = os.environ.get("CN_PERSISTENCE_TYPE", "sql")

    if persistence_type == "couchbase":
        return CouchbaseClient()
    elif persistence_type == "ldap":
        return LdapClient()
    elif persistence_type == "spanner":
        return SpannerClient()
    else:
        return SqlClient()


class ShibbolethBootstrap:
    def __init__(self, manager):
        self.manager = manager
        self.persistence_client = get_persistence_client()

    def setup_idp_home(self):
        logger.info("Setting up IDP home directory")

        dirs = [
            f"{IDP_HOME}/conf",
            f"{IDP_HOME}/metadata",
            f"{IDP_HOME}/credentials",
            f"{IDP_HOME}/views",
            f"{IDP_HOME}/logs",
        ]

        for d in dirs:
            os.makedirs(d, exist_ok=True)

    def generate_sealer_key(self):
        logger.info("Generating sealer key")

        sealer_file = f"{IDP_HOME}/credentials/sealer.jks"
        sealer_password = os.environ.get("IDP_SEALER_PASSWORD")

        if os.path.exists(sealer_file):
            if sealer_password:
                return sealer_password
            elif os.path.exists(SEALER_PASSWORD_FILE):
                with open(SEALER_PASSWORD_FILE) as f:
                    return f.read().strip()
            else:
                raise RuntimeError(
                    f"Sealer keystore exists at {sealer_file} but no password found. "
                    "Set IDP_SEALER_PASSWORD environment variable."
                )

        if not sealer_password:
            sealer_password = get_random_chars(24)

        import subprocess
        subprocess.run([
            "keytool", "-genseckey",
            "-alias", "secret",
            "-keystore", sealer_file,
            "-storepass", sealer_password,
            "-keypass", sealer_password,
            "-keyalg", "AES",
            "-keysize", "128",
        ], check=True)

        with open(SEALER_PASSWORD_FILE, "w") as f:
            f.write(sealer_password)
        os.chmod(SEALER_PASSWORD_FILE, 0o600)
        os.chmod(sealer_file, 0o600)

        return sealer_password

    def configure_idp_properties(self, sealer_password):
        logger.info("Configuring IDP properties")

        entity_id = os.environ.get("IDP_ENTITY_ID", self.manager.config.get("hostname"))
        scope = os.environ.get("IDP_SCOPE", self.manager.config.get("hostname"))

        ctx = {
            "idp_entity_id": entity_id,
            "idp_scope": scope,
            "idp_sealer_password": sealer_password,
            "jans_auth_server_url": f"https://{self.manager.config.get('hostname')}",
            "jans_auth_client_id": self.manager.config.get("shibboleth_client_id", ""),
            "jans_auth_client_secret": self.manager.secret.get("shibboleth_client_pw", ""),
        }

        src = "/app/templates/idp.properties.tmpl"
        dst = f"{IDP_HOME}/conf/idp.properties"

        with open(src) as f:
            txt = Template(f.read()).safe_substitute(ctx)

        with open(dst, "w") as f:
            f.write(txt)

    def setup_credentials(self):
        logger.info("Setting up credentials")

        signing_cert = self.manager.secret.get("shibboleth_idp_signing_cert")
        signing_key = self.manager.secret.get("shibboleth_idp_signing_key")

        if signing_cert:
            cert_path = f"{IDP_HOME}/credentials/idp-signing.crt"
            with open(cert_path, "w") as f:
                f.write(signing_cert)

        if signing_key:
            key_path = f"{IDP_HOME}/credentials/idp-signing.key"
            with open(key_path, "w") as f:
                f.write(signing_key)
            os.chmod(key_path, 0o600)

        encryption_cert = self.manager.secret.get("shibboleth_idp_encryption_cert")
        encryption_key = self.manager.secret.get("shibboleth_idp_encryption_key")

        if encryption_cert:
            cert_path = f"{IDP_HOME}/credentials/idp-encryption.crt"
            with open(cert_path, "w") as f:
                f.write(encryption_cert)

        if encryption_key:
            key_path = f"{IDP_HOME}/credentials/idp-encryption.key"
            with open(key_path, "w") as f:
                f.write(encryption_key)
            os.chmod(key_path, 0o600)

    def setup_webapp(self):
        logger.info("Setting up webapp")

        war_src = os.environ.get(
            "JANS_SHIBBOLETH_WAR",
            "/tmp/shibboleth-idp-src/war/idp.war"
        )
        war_dst = f"{JETTY_BASE}/shibboleth-idp/webapps/idp.war"

        if os.path.exists(war_src):
            shutil.copy2(war_src, war_dst)

    def setup_jetty_base(self):
        logger.info("Setting up Jetty base")

        jetty_idp_base = f"{JETTY_BASE}/shibboleth-idp"

        dirs = [
            f"{jetty_idp_base}/webapps",
            f"{jetty_idp_base}/resources",
            f"{jetty_idp_base}/etc",
            f"{jetty_idp_base}/start.d",
        ]

        for d in dirs:
            os.makedirs(d, exist_ok=True)

        modules = [
            "server",
            "http",
            "deploy",
            "annotations",
            "jsp",
            "jstl",
            "plus",
        ]

        for mod in modules:
            mod_file = f"{jetty_idp_base}/start.d/{mod}.ini"
            with open(mod_file, "w") as f:
                f.write(f"--module={mod}\n")

    def bootstrap(self):
        logger.info("Starting Shibboleth IDP bootstrap")

        self.setup_idp_home()
        sealer_password = self.generate_sealer_key()
        self.configure_idp_properties(sealer_password)
        self.setup_credentials()
        self.setup_jetty_base()
        self.setup_webapp()

        logger.info("Shibboleth IDP bootstrap complete")


def main():
    manager = get_manager()
    bootstrap = ShibbolethBootstrap(manager)
    bootstrap.bootstrap()


if __name__ == "__main__":
    main()
