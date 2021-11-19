import logging.config

from jans.pycloudlib.utils import exec_cmd

from base_handler import BaseHandler
from settings import LOGGING_CONFIG

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("certmanager")


class OxshibbolethHandler(BaseHandler):
    @classmethod
    def gen_idp3_key(cls, storepass):
        cmd = (
            "java -classpath '/app/javalibs/*' "
            "net.shibboleth.utilities.java.support.security.BasicKeystoreKeyStrategyTool "
            "--storefile /etc/certs/sealer.jks "
            "--versionfile /etc/certs/sealer.kver "
            "--alias secret "
            f"--storepass {storepass}"
        )
        return exec_cmd(cmd)

    def _patch_shib_sealer(self, passwd):
        sealer_jks = "/etc/certs/sealer.jks"
        sealer_kver = "/etc/certs/sealer.kver"
        logger.info(f"Generating new {sealer_jks} and {sealer_kver} files")
        self.gen_idp3_key(passwd)
        return sealer_jks, sealer_kver

    def patch(self):
        passwd = self.manager.secret.get("shibJksPass")

        # shibIDP
        cert_fn, key_fn = self._patch_cert_key("shibIDP", passwd)
        if not self.dry_run:
            if cert_fn:
                self.manager.secret.from_file(
                    "shibIDP_cert", cert_fn, encode=True,
                )
            if key_fn:
                self.manager.secret.from_file(
                    "shibIDP_cert", key_fn, encode=True,
                )

        keystore_fn = self._patch_keystore(
            "shibIDP", self.manager.config.get("hostname"), passwd,
        )
        if not self.dry_run:
            if keystore_fn:
                self.manager.secret.from_file(
                    "shibIDP_jks_base64",
                    keystore_fn,
                    encode=True,
                    binary_mode=True,
                )

        sealer_jks_fn, sealer_kver_fn = self._patch_shib_sealer(passwd)
        if not self.dry_run:
            if sealer_jks_fn:
                self.manager.secret.from_file(
                    "sealer_jks_base64",
                    sealer_jks_fn,
                    encode=True,
                    binary_mode=True,
                )
            if sealer_kver_fn:
                self.manager.secret.from_file(
                    "sealer_kver_base64", sealer_kver_fn, encode=True,
                )

        # IDP signing
        cert_fn, key_fn = self._patch_cert_key("idp-signing", passwd)
        if not self.dry_run:
            if cert_fn:
                self.manager.secret.from_file(
                    "idp3SigningCertificateText", cert_fn,
                )
            if key_fn:
                self.manager.secret.from_file("idp3SigningKeyText", key_fn)

        # IDP encryption
        cert_fn, key_fn = self._patch_cert_key("idp-encryption", passwd)
        if not self.dry_run:
            if cert_fn:
                self.manager.secret.from_file(
                    "idp3EncryptionCertificateText", cert_fn,
                )
            if key_fn:
                self.manager.secret.from_file("idp3EncryptionKeyText", key_fn)
