import os

from ruamel.yaml import safe_load
from ruamel.yaml import safe_dump

from jans.pycloudlib import get_manager
from jans.pycloudlib.persistence import render_couchbase_properties
from jans.pycloudlib.persistence import render_gluu_properties
from jans.pycloudlib.persistence import render_hybrid_properties
from jans.pycloudlib.persistence import render_ldap_properties
from jans.pycloudlib.persistence import render_salt
from jans.pycloudlib.persistence import sync_couchbase_truststore
from jans.pycloudlib.persistence import sync_ldap_truststore
from jans.pycloudlib.utils import cert_to_truststore
from jans.pycloudlib.utils import get_server_certificate
from jans.pycloudlib.utils import get_random_chars
from jans.pycloudlib.utils import exec_cmd
from jans.pycloudlib.utils import as_boolean


manager = get_manager()


def get_gluu_cert():
    if not os.path.isfile("/etc/certs/gluu_https.crt"):
        if as_boolean(os.environ.get("JANS_SSL_CERT_FROM_SECRETS", False)):
            manager.secret.to_file("ssl_cert", "/etc/certs/gluu_https.crt")
        else:
            get_server_certificate(manager.config.get("hostname"), 443, "/etc/certs/gluu_https.crt")

    cert_to_truststore(
        "gluu_https",
        "/etc/certs/gluu_https.crt",
        "/usr/lib/jvm/default-jvm/jre/lib/security/cacerts",
        "changeit",
    )


def generate_x509(cert_file, key_file, cert_cn):
    out, err, code = exec_cmd(
        "openssl req -x509 -newkey rsa:2048 "
        f"-keyout {key_file} "
        f"-out {cert_file} "
        f"-subj '/CN={cert_cn}' "
        "-days 365 "
        "-nodes"
    )
    assert code == 0, "Failed to generate application cert and key; reason={}".format(err.decode())


def generate_keystore(cert_file, key_file, keystore_file, keystore_password):
    out, err, code = exec_cmd(
        "openssl pkcs12 -export -name oxd-server "
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
        return f"/etc/certs/oxd_{self.type}.crt"

    @property
    def key_file(self):
        return f"/etc/certs/oxd_{self.type}.key"

    @property
    def keystore_file(self):
        return f"/etc/certs/oxd_{self.type}.keystore"

    @property
    def cert_cn(self):
        conn_type = self.type.upper()

        # backward-compat with 4.1.x
        if f"{conn_type}_KEYSTORE_CN" in os.environ:
            return os.environ.get(f"{conn_type}_KEYSTORE_CN", "localhost")
        return os.environ.get(f"JANS_CLIENT_API_{conn_type}_CERT_CN", "localhost")

    def sync_x509(self):
        try:
            self.manager.secret.to_file(f"oxd_{self.type}_cert", self.cert_file)
            self.manager.secret.to_file(f"oxd_{self.type}_key", self.key_file)
        except TypeError:
            generate_x509(self.cert_file, self.key_file, self.cert_cn)
            # save cert and key to secrets for later use
            self.manager.secret.from_file(f"oxd_{self.type}_cert", self.cert_file)
            self.manager.secret.from_file(f"oxd_{self.type}_key", self.key_file)

    def get_keystore_password(self):
        password = manager.secret.get(f"oxd_{self.type}_keystore_password")

        if not password:
            password = get_random_chars()
            manager.secret.set(f"oxd_{self.type}_keystore_password", password)
        return password

    def sync_keystore(self):
        # if there are no secrets, ``TypeError`` will be thrown
        try:
            self.manager.secret.to_file(
                f"oxd_{self.type}_jks_base64", self.keystore_file, decode=True, binary_mode=True,
            )
        except TypeError:
            generate_keystore(self.cert_file, self.key_file, self.keystore_file, self.get_keystore_password())
            # save keystore to secrets for later use
            self.manager.secret.from_file(
                f"oxd_{self.type}_jks_base64", self.keystore_file, encode=True, binary_mode=True,
            )

    def sync(self):
        self.sync_x509()
        self.sync_keystore()


def render_oxd_config():
    app_connector = Connector(manager, "application")
    app_connector.sync()
    admin_connector = Connector(manager, "admin")
    admin_connector.sync()

    with open("/app/templates/oxd-server.yml.tmpl") as f:
        data = safe_load(f.read())

    data["server"]["applicationConnectors"][0]["keyStorePassword"] = app_connector.get_keystore_password()
    data["server"]["applicationConnectors"][0]["keyStorePath"] = app_connector.keystore_file
    data["server"]["adminConnectors"][0]["keyStorePassword"] = admin_connector.get_keystore_password()
    data["server"]["adminConnectors"][0]["keyStorePath"] = admin_connector.keystore_file

    persistence_type = os.environ.get("JANS_PERSISTENCE_TYPE", "ldap")

    if persistence_type in ("ldap", "hybrid"):
        conn = "gluu-ldap.properties"
    else:
        # likely "couchbase"
        conn = "gluu-couchbase.properties"

    data["storage_configuration"]["connection"] = f"/etc/gluu/conf/{conn}"

    ip_addresses = os.environ.get("JANS_CLIENT_API_BIND_IP_ADDRESSES", "*")
    data["bind_ip_addresses"] = [
        addr.strip()
        for addr in ip_addresses.split(",")
        if addr
    ]

    with open("/opt/oxd-server/conf/oxd-server.yml", "w") as f:
        f.write(safe_dump(data))


def main():
    persistence_type = os.environ.get("JANS_PERSISTENCE_TYPE", "ldap")

    render_salt(manager, "/app/templates/salt.tmpl", "/etc/gluu/conf/salt")
    render_gluu_properties("/app/templates/gluu.properties.tmpl", "/etc/gluu/conf/gluu.properties")

    if persistence_type in ("ldap", "hybrid"):
        render_ldap_properties(
            manager,
            "/app/templates/gluu-ldap.properties.tmpl",
            "/etc/gluu/conf/gluu-ldap.properties",
        )
        sync_ldap_truststore(manager)

    if persistence_type in ("couchbase", "hybrid"):
        render_couchbase_properties(
            manager,
            "/app/templates/gluu-couchbase.properties.tmpl",
            "/etc/gluu/conf/gluu-couchbase.properties",
        )
        sync_couchbase_truststore(manager)

    if persistence_type == "hybrid":
        render_hybrid_properties("/etc/gluu/conf/gluu-hybrid.properties")

    get_gluu_cert()

    # if not os.path.isfile("/opt/oxd-server/oxd-server.yml"):
    render_oxd_config()


if __name__ == "__main__":
    main()
