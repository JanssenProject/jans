import os

from ruamel.yaml import safe_load
from ruamel.yaml import safe_dump

from jans.pycloudlib import get_manager
from jans.pycloudlib.persistence import render_couchbase_properties
from jans.pycloudlib.persistence import render_base_properties
from jans.pycloudlib.persistence import render_hybrid_properties
from jans.pycloudlib.persistence import render_ldap_properties
from jans.pycloudlib.persistence import render_salt
from jans.pycloudlib.persistence import sync_couchbase_truststore
from jans.pycloudlib.persistence import sync_ldap_truststore
from jans.pycloudlib.persistence import render_sql_properties
from jans.pycloudlib.utils import cert_to_truststore
from jans.pycloudlib.utils import get_random_chars
from jans.pycloudlib.utils import exec_cmd
from jans.pycloudlib.utils import generate_ssl_certkey


manager = get_manager()


def get_web_cert():
    if not os.path.isfile("/etc/certs/web_https.crt"):
        manager.secret.to_file("ssl_cert", "/etc/certs/web_https.crt")

    cert_to_truststore(
        "web_https",
        "/etc/certs/web_https.crt",
        "/usr/lib/jvm/default-jvm/jre/lib/security/cacerts",
        "changeit",
    )


def generate_keystore(cert_file, key_file, keystore_file, keystore_password):
    out, err, code = exec_cmd(
        "openssl pkcs12 -export -name client-api "
        f"-out {keystore_file} "
        f"-inkey {key_file} "
        f"-in {cert_file} "
        f"-passout pass:{keystore_password}"
    )
    assert code == 0, "Failed to generate application keystore; reason={}".format(err.decode())


class Connector:
    def __init__(self, manager, type_):
        self.manager = manager
        self.type = type_
        assert self.type in ("application", "admin")

    @property
    def cert_file(self):
        return f"/etc/certs/client_api_{self.type}.crt"

    @property
    def key_file(self):
        return f"/etc/certs/client_api_{self.type}.key"

    @property
    def keystore_file(self):
        return f"/etc/certs/client_api_{self.type}.keystore"

    @property
    def cert_cn(self):
        conn_type = self.type.upper()

        # backward-compat with 4.1.x
        if f"{conn_type}_KEYSTORE_CN" in os.environ:
            return os.environ.get(f"{conn_type}_KEYSTORE_CN", "localhost")
        return os.environ.get(f"CN_CLIENT_API_{conn_type}_CERT_CN", "localhost")

    def sync_x509(self):
        try:
            self.manager.secret.to_file(f"client_api_{self.type}_cert", self.cert_file)
            self.manager.secret.to_file(f"client_api_{self.type}_key", self.key_file)
        except TypeError:
            generate_ssl_certkey(
                f"client_api_{self.type}",
                self.manager.config.get("admin_email"),
                self.manager.config.get("hostname"),
                self.manager.config.get("orgName"),
                self.manager.config.get("country_code"),
                self.manager.config.get("state"),
                self.manager.config.get("city"),
                extra_dns=[self.cert_cn],
            )
            # save cert and key to secrets for later use
            self.manager.secret.from_file(f"client_api_{self.type}_cert", self.cert_file)
            self.manager.secret.from_file(f"client_api_{self.type}_key", self.key_file)

    def get_keystore_password(self):
        password = manager.secret.get(f"client_api_{self.type}_keystore_password")

        if not password:
            password = get_random_chars()
            manager.secret.set(f"client_api_{self.type}_keystore_password", password)
        return password

    def sync_keystore(self):
        # if there are no secrets, ``TypeError`` will be thrown
        try:
            self.manager.secret.to_file(
                f"client_api_{self.type}_jks_base64", self.keystore_file, decode=True, binary_mode=True,
            )
        except TypeError:
            generate_keystore(self.cert_file, self.key_file, self.keystore_file, self.get_keystore_password())
            # save keystore to secrets for later use
            self.manager.secret.from_file(
                f"client_api_{self.type}_jks_base64", self.keystore_file, encode=True, binary_mode=True,
            )

    def sync(self):
        self.sync_x509()
        self.sync_keystore()


def render_client_api_config():
    with open("/app/templates/client-api-server.yml.tmpl") as f:
        data = safe_load(f.read())

    persistence_type = os.environ.get("CN_PERSISTENCE_TYPE", "ldap")
    conn = f"jans-{persistence_type}.properties"

    data["storage"] = "jans_server_configuration"
    data["storage_configuration"] = {
        "baseDn": "o=jans",
        "type": "/etc/jans/conf/jans.properties",
        "salt": "/etc/jans/conf/salt",
        "connection": f"/etc/jans/conf/{conn}",
    }

    app_connector = Connector(manager, "application")
    app_connector.sync()
    admin_connector = Connector(manager, "admin")
    admin_connector.sync()

    data["server"]["applicationConnectors"][0]["keyStorePassword"] = app_connector.get_keystore_password()
    data["server"]["applicationConnectors"][0]["keyStorePath"] = app_connector.keystore_file
    data["server"]["adminConnectors"][0]["keyStorePassword"] = admin_connector.get_keystore_password()
    data["server"]["adminConnectors"][0]["keyStorePath"] = admin_connector.keystore_file

    # ip_addresses = os.environ.get("CN_CLIENT_API_BIND_IP_ADDRESSES", "*")
    # data["bind_ip_addresses"] = [
    #     addr.strip()
    #     for addr in ip_addresses.split(",")
    #     if addr
    # ]

    # write config
    with open("/opt/client-api/conf/client-api-server.yml", "w") as f:
        f.write(safe_dump(data))


def main():
    persistence_type = os.environ.get("CN_PERSISTENCE_TYPE", "ldap")

    render_salt(manager, "/app/templates/salt.tmpl", "/etc/jans/conf/salt")
    render_base_properties("/app/templates/jans.properties.tmpl", "/etc/jans/conf/jans.properties")

    if persistence_type in ("ldap", "hybrid"):
        render_ldap_properties(
            manager,
            "/app/templates/jans-ldap.properties.tmpl",
            "/etc/jans/conf/jans-ldap.properties",
        )
        sync_ldap_truststore(manager)

    if persistence_type in ("couchbase", "hybrid"):
        render_couchbase_properties(
            manager,
            "/app/templates/jans-couchbase.properties.tmpl",
            "/etc/jans/conf/jans-couchbase.properties",
        )
        sync_couchbase_truststore(manager)

    if persistence_type == "hybrid":
        render_hybrid_properties("/etc/jans/conf/jans-hybrid.properties")

    if persistence_type == "sql":
        render_sql_properties(
            manager,
            "/app/templates/jans-sql.properties.tmpl",
            "/etc/jans/conf/jans-sql.properties",
        )

    get_web_cert()

    # if not os.path.isfile("/opt/client-api/client-api-server.yml"):
    render_client_api_config()


if __name__ == "__main__":
    main()
