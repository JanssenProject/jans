import logging.config

from jans.pycloudlib.utils import exec_cmd
from jans.pycloudlib.utils import generate_ssl_certkey

from base_handler import BaseHandler
from settings import LOGGING_CONFIG

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("certmanager")


class ClientApiHandler(BaseHandler):
    def generate_x509(self, suffix, cert_cn):
        cert_file, key_file = generate_ssl_certkey(
            suffix,
            self.manager.config.get("admin_email"),
            self.manager.config.get("hostname"),
            self.manager.config.get("orgName"),
            self.manager.config.get("country_code"),
            self.manager.config.get("state"),
            self.manager.config.get("city"),
            extra_dn=cert_cn,
        )
        return cert_file, key_file

    @staticmethod
    def generate_keystore(cert_file, key_file, keystore_file, keystore_password):
        out, err, code = exec_cmd(
            "openssl pkcs12 -export -name client-api "
            f"-out {keystore_file} "
            f"-inkey {key_file} "
            f"-in {cert_file} "
            f"-passout pass:{keystore_password}"
        )
        if code != 0:
            logger.warning(f"Failed to generate keystore; reason={err.decode()}")
            return False
        return True

    def _patch_connector(self, conn_type):
        suffix = f"client_api_{conn_type}"
        cert_file, key_file = f"{suffix}.crt", f"{suffix}.key"
        cert_cn = self.opts.get(f"{conn_type}-cn", "localhost")

        cert_file, key_file = self.generate_x509(suffix, cert_cn)
        logger.info(f"Generating new {cert_file} and {key_file} file(s)")
        if not self.dry_run:
            self.manager.secret.from_file(
                f"client_api_{conn_type}_cert", cert_file,
            )
            self.manager.secret.from_file(
                f"client_api_{conn_type}_key", key_file,
            )

        keystore_file = f"/etc/certs/client_api_{conn_type}.keystore"
        keystore_password = self.manager.secret.get(f"client_api_{conn_type}_keystore_password")

        logger.info(f"Generating new {keystore_file} file")
        generated = self.generate_keystore(
            cert_file, key_file, keystore_file, keystore_password,
        )
        if not self.dry_run and generated:
            self.manager.secret.from_file(
                f"client_api_{conn_type}_jks_base64",
                keystore_file,
                encode=True,
                binary_mode=True,
            )

    def patch(self):
        for conn_type in ("application", "admin"):
            self._patch_connector(conn_type)
