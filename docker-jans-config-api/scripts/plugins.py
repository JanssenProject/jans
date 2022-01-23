import logging.config
import os
from uuid import uuid4

from jans.pycloudlib.utils import get_random_chars
from jans.pycloudlib.utils import encode_text
from jans.pycloudlib.utils import cert_to_truststore

from settings import LOGGING_CONFIG

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("plugins")


def discover_plugins():
    loaded_plugins = []

    plugins = [
        plugin.strip()
        for plugin in os.environ.get("CN_CONFIG_API_PLUGINS", "").strip().split(",")
        if plugin.strip()
    ]

    for plugin in plugins:
        plugin_jar = f"/opt/jans/jetty/jans-config-api/custom/libs/{plugin}-plugin.jar"

        if not os.path.isfile(plugin_jar):
            continue
        loaded_plugins.append(plugin)
    return loaded_plugins


class AdminUiPlugin:
    def __init__(self, manager):
        self.manager = manager

    def resolve_config_secret(self):
        # @TODO: move to configurator?
        def auth_server_client():
            if not self.manager.config.get("admin_ui_client_id"):
                self.manager.config.set("admin_ui_client_id", f"1901.{uuid4()}")

            client_pw = self.manager.secret.get("admin_ui_client_pw")
            if not client_pw:
                client_pw = get_random_chars()
                self.manager.secret.set("admin_ui_client_pw", client_pw)

            if not self.manager.secret.get("admin_ui_client_encoded_pw"):
                self.manager.secret.set(
                    "admin_ui_client_encoded_pw",
                    encode_text(client_pw, self.manager.secret.get("encoded_salt")),
                )

        def token_server_client():
            if not self.manager.config.get("token_server_admin_ui_client_id"):
                self.manager.config.set("token_server_admin_ui_client_id", f"1901.{uuid4()}")

            client_pw = self.manager.secret.get("token_server_admin_ui_client_pw")
            if not client_pw:
                client_pw = get_random_chars()
                self.manager.secret.set("token_server_admin_ui_client_pw", client_pw)

            if not self.manager.secret.get("token_server_admin_ui_client_encoded_pw"):
                self.manager.secret.set(
                    "token_server_admin_ui_client_encoded_pw",
                    encode_text(client_pw, self.manager.secret.get("encoded_salt")),
                )

        auth_server_client()
        token_server_client()

    def render_config(self):
        ctx = {
            "admin_ui_client_id": self.manager.config.get("admin_ui_client_id"),
            "admin_ui_client_pw": self.manager.secret.get("admin_ui_client_pw"),
            "hostname": self.manager.config.get("hostname"),

            "admin_ui_license_api_key": self.read_from_file("/etc/jans/conf/admin_ui_api_key"),
            "admin_ui_license_product_code": self.read_from_file("/etc/jans/conf/admin_ui_product_code"),
            "admin_ui_license_shared_key": self.read_from_file("/etc/jans/conf/admin_ui_shared_key"),
            "admin_ui_license_management_key": self.read_from_file("/etc/jans/conf/admin_ui_management_key"),
        }
        ctx.update(self.get_token_server_ctx())

        with open("/app/plugins/admin-ui/auiConfiguration.properties.tmpl") as f:
            txt = f.read() % ctx

        with open("/opt/jans/jetty/jans-config-api/custom/config/auiConfiguration.properties", "w") as f:
            f.write(txt)

    def read_from_file(self, path):
        txt = ""
        try:
            with open(path) as f:
                txt = f.read()
        except FileNotFoundError:
            logger.warning(f"Unable to read {path} file; fallback to empty string")
        return txt.strip()

    def get_token_server_ctx(self):
        hostname = os.environ.get("CN_TOKEN_SERVER_BASE_HOSTNAME") or self.manager.config.get("hostname")
        authz_endpoint = os.environ.get("CN_TOKEN_SERVER_AUTHZ_ENDPOINT") or "/jans-auth/authorize.htm"
        token_endpoint = os.environ.get("CN_TOKEN_SERVER_TOKEN_ENDPOINT") or "/jans-auth/restv1/token"
        introspection_endpoint = os.environ.get("CN_TOKEN_SERVER_INTROSPECTION_ENDPOINT") or "/jans-auth/restv1/introspection"
        userinfo_endpoint = os.environ.get("CN_TOKEN_SERVER_USERINFO_ENDPOINT") or "/jans-auth/restv1/userinfo"

        pw_file = "/etc/jans/conf/token_server_client_secret"
        if not os.path.isfile(pw_file):
            self.manager.secret.to_file("token_server_admin_ui_client_pw", pw_file)

        ctx = {
            "token_server_admin_ui_client_id": os.environ.get("CN_TOKEN_SERVER_CLIENT_ID") or self.manager.config.get("token_server_admin_ui_client_id"),
            "token_server_admin_ui_client_pw": self.read_from_file(pw_file),
            "token_server_authz_url": f"https://{hostname}{authz_endpoint}",
            "token_server_token_url": f"https://{hostname}{token_endpoint}",
            "token_server_introspection_url": f"https://{hostname}{introspection_endpoint}",
            "token_server_userinfo_url": f"https://{hostname}{userinfo_endpoint}",
        }
        return ctx

    def setup(self):
        self.resolve_config_secret()
        self.render_config()
        self.import_token_server_cert()

    def import_token_server_cert(self):
        cert_file = os.environ.get("CN_TOKEN_SERVER_CERT_FILE", "/etc/certs/token_server.crt")
        if not os.path.isfile(cert_file):
            self.manager.secret.to_file("ssl_cert", cert_file)

        cert_to_truststore(
            "token_server",
            cert_file,
            "/usr/java/latest/jre/lib/security/cacerts",
            "changeit",
        )
