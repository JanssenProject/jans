import logging.config
import os
import sys

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

    def render_config(self):
        prop_key = "plugins_admin_ui_properties"

        if not self.manager.secret.get(prop_key):
            logger.error(f"Unable to find {prop_key} from secret")
            sys.exit(1)

        self.manager.secret.to_file(
            prop_key,
            "/opt/jans/jetty/jans-config-api/custom/config/auiConfiguration.properties",
        )

    def setup(self):
        logger.info("Configuring admin-ui plugin")
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
