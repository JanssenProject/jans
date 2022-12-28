import logging.config
import os
import shutil

from jans.pycloudlib.utils import cert_to_truststore

from settings import LOGGING_CONFIG

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("plugins")

SUPPORTED_PLUGINS = (
    "admin-ui",
    "scim",
    "fido2",
    "user-mgt",
)


def discover_plugins() -> list[str]:
    """Discover enabled plugins.

    The plugin JAR file will be copied to ``/opt/jans/jetty/jans-config-api/custom/libs`` directory.
    """
    loaded_plugins = []

    user_plugins = [
        plugin.strip()
        for plugin in os.environ.get("CN_CONFIG_API_PLUGINS", "").strip().split(",")
        if plugin.strip()
    ]

    for plugin in set(user_plugins):
        if plugin not in SUPPORTED_PLUGINS:
            continue

        src = f"/usr/share/java/{plugin}-plugin.jar"
        dst = f"/opt/jans/jetty/jans-config-api/custom/libs/{plugin}-plugin.jar"

        if not os.path.isfile(src):
            continue

        shutil.copyfile(src, dst)
        loaded_plugins.append(plugin)

    # a list of loaded plugins
    return loaded_plugins


class AdminUiPlugin:
    def __init__(self, manager):
        self.manager = manager

    def setup(self):
        logger.info("Configuring admin-ui plugin")
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
