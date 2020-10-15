import logging.config

from pygluu.containerlib.utils import exec_cmd

from base_handler import BaseHandler
from settings import LOGGING_CONFIG

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("certmanager")


class OxdHandler(BaseHandler):
    @staticmethod
    def generate_x509(cert_file, key_file, cert_cn):
        out, err, code = exec_cmd(
            "openssl req -x509 -newkey rsa:2048 "
            f"-keyout {key_file} "
            f"-out {cert_file} "
            f"-subj '/CN={cert_cn}' "
            "-days 365 "
            "-nodes"
        )
        if code != 0:
            logger.warning(f"Failed to generate cert and key; reason={err.decode()}")
            return False
        return True

    @staticmethod
    def generate_keystore(cert_file, key_file, keystore_file, keystore_password):
        out, err, code = exec_cmd(
            "openssl pkcs12 -export -name oxd-server "
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
        cert_file = f"/etc/certs/oxd_{conn_type}.crt"
        key_file = f"/etc/certs/oxd_{conn_type}.key"
        cert_cn = self.opts.get(f"{conn_type}-cn", "localhost")

        logger.info(f"Generating new {cert_file} and {key_file} file(s)")
        generated = self.generate_x509(cert_file, key_file, cert_cn)
        if not self.dry_run and generated:
            self.manager.secret.from_file(
                f"oxd_{conn_type}_cert", cert_file,
            )
            self.manager.secret.from_file(
                f"oxd_{conn_type}_key", key_file,
            )

        keystore_file = f"/etc/certs/oxd_{conn_type}.keystore"
        keystore_password = self.manager.secret.get(f"oxd_{conn_type}_keystore_password")

        logger.info(f"Generating new {keystore_file} file")
        generated = self.generate_keystore(
            cert_file, key_file, keystore_file, keystore_password,
        )
        if not self.dry_run and generated:
            self.manager.secret.from_file(
                f"oxd_{conn_type}_jks_base64",
                keystore_file,
                encode=True,
                binary_mode=True,
            )

    def patch(self):
        for conn_type in ("application", "admin"):
            self._patch_connector(conn_type)
