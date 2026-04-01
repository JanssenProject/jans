import logging.config
import os
import shutil
from tempfile import TemporaryDirectory
from urllib.parse import urlparse
from zipfile import ZipFile

from jans.pycloudlib.utils import cert_to_truststore
from jans.pycloudlib.utils import get_server_certificate

from settings import LOGGING_CONFIG

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("jans-config-api")

SUPPORTED_PLUGINS = (
    "admin-ui",
    "scim",
    "fido2",
    "user-mgt",
    "lock",
    "jans-link",
    # shibboleth
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

        src = f"/opt/jans/jetty/jans-config-api/_plugins/{plugin}-plugin.jar"
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
        self.configure_policy_store()

    def import_token_server_cert(self):
        cert_file = os.environ.get("CN_TOKEN_SERVER_CERT_FILE", "/etc/certs/token_server.crt")

        if not os.path.isfile(cert_file):
            # check if token server is not the fqdn
            base_url = os.environ.get("CN_TOKEN_SERVER_BASE_URL")

            if base_url:
                # download from given URL
                self.pull_token_server_cert(base_url, cert_file)
            else:
                self.manager.secret.to_file("ssl_cert", cert_file)

        cert_to_truststore(
            "token_server",
            cert_file,
            "/opt/java/lib/security/cacerts",
            "changeit",
        )

    def pull_token_server_cert(self, base_url, cert_file):
        logger.info("Downloading certificate from %s", base_url)

        parsed_url = urlparse(base_url)
        host = parsed_url.hostname
        port = parsed_url.port

        # port might not be defined in the given URL
        if not port:
            # resolve port as last segment of netloc
            port = parsed_url.netloc.split(":")[-1]

            # possible edge-cases while parsing netloc:
            #
            # - empty port, e.g. `localhost:`
            # - missing port, e.g. `localhost`
            if (not port or port == host):
                if parsed_url.scheme == "https":
                    port = 443
                else:
                    port = 80

        # download the cert (if possible)
        get_server_certificate(host, port, cert_file)

    def configure_policy_store(self):
        logger.info("Configuring admin-ui policy store")

        hostname = self.manager.config.get("hostname")
        policy_file = "trusted-issuers/GluuFlexAdminUI.json"
        policy_file_found = False

        with TemporaryDirectory() as tmp_dir:
            src_archive_path = "/opt/jans/jetty/jans-config-api/custom/config/adminUI/policy-store.cjar"
            tmp_archive_path = os.path.join(tmp_dir, "policy-store.cjar")

            with ZipFile(src_archive_path, "r") as src_archive, ZipFile(tmp_archive_path, "w", compression=src_archive.compression) as tmp_archive:
                for item in src_archive.infolist():
                    if item.filename == policy_file:
                        policy_file_found = True
                        policy = src_archive.read(item.filename).decode()
                        data = policy.replace("your-openid-provider.server", hostname).encode()
                    else:
                        data = src_archive.read(item.filename)
                    # copy item and preserve the original compression
                    tmp_archive.writestr(item, data, compress_type=item.compress_type)

            if not policy_file_found:
                logger.warning("The policy file %s is not found in %s. Policy for Flex admin-ui may not be applied properly.", policy_file, src_archive_path)

            # replace the original archive
            shutil.move(tmp_archive_path, src_archive_path)
