import base64
import json
import logging.config
import sys

from base_handler import BaseHandler
from settings import LOGGING_CONFIG
from utils import generate_openid_keys
from utils import export_openid_keys

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("certmanager")


class PassportHandler(BaseHandler):
    def patch_passport_rs(self):
        jks_fn = self.manager.config.get("passport_rs_client_jks_fn")
        jwks_fn = self.manager.config.get("passport_rs_client_jwks_fn")

        logger.info(f"Generating new {jks_fn} and {jwks_fn}")

        out, err, retcode = generate_openid_keys(
            self.manager.secret.get("passport_rs_client_jks_pass"),
            jks_fn,
            jwks_fn,
            self.manager.config.get("default_openid_jks_dn_name"),
        )
        if retcode != 0:
            logger.error(f"Unable to generate Passport RS keys; reason={err.decode()}")
            sys.exit(1)

        cert_alg = self.manager.config.get("passport_rs_client_cert_alg")
        cert_alias = ""
        for key in json.loads(out)["keys"]:
            if key["alg"] == cert_alg:
                cert_alias = key["kid"]
                break

        if not self.dry_run:
            self.manager.secret.set("passport_rs_client_base64_jwks", base64.b64encode(out))
            self.manager.secret.from_file(
                "passport_rs_jks_base64", jks_fn, encode=True, binary_mode=True,
            )
            self.manager.config.set("passport_rs_client_cert_alias", cert_alias)

    def patch_passport_rp(self):
        jks_pass = self.manager.secret.get("passport_rp_client_jks_pass")
        jks_fn = self.manager.config.get("passport_rp_client_jks_fn")
        jwks_fn = self.manager.config.get("passport_rp_client_jwks_fn")
        client_cert_fn = self.manager.config.get("passport_rp_client_cert_fn")

        logger.info(f"Generating new {jks_fn} and {jwks_fn}")

        out, err, code = generate_openid_keys(
            jks_pass,
            jks_fn,
            jwks_fn,
            self.manager.config.get("default_openid_jks_dn_name"),
        )
        if code != 0:
            logger.error(f"Unable to generate Passport RP keys; reason={err.decode()}")
            sys.exit(1)

        cert_alg = self.manager.config.get("passport_rp_client_cert_alg")
        cert_alias = ""
        for key in json.loads(out)["keys"]:
            if key["alg"] == cert_alg:
                cert_alias = key["kid"]
                break

        _, err, retcode = export_openid_keys(
            jks_fn, jks_pass, cert_alias, client_cert_fn,
        )
        if retcode != 0:
            logger.error(f"Unable to generate Passport RP client cert; reason={err.decode()}")
            sys.exit(1)

        if not self.dry_run:
            self.manager.secret.set("passport_rp_client_base64_jwks", base64.b64encode(out))
            self.manager.secret.from_file(
                "passport_rp_jks_base64", jks_fn, encode=True, binary_mode=True,
            )
            self.manager.config.set("passport_rp_client_cert_alias", cert_alias)
            self.manager.secret.from_file("passport_rp_client_cert_base64", client_cert_fn, encode=True)

    def patch_passport_sp(self):
        cert_fn, key_fn = self._patch_cert_key("passport-sp", self.manager.secret.get("passportSpJksPass"))

        if not self.dry_run:
            self.manager.secret.from_file("passport_sp_cert_base64", cert_fn, encode=True)
            self.manager.secret.from_file("passport_sp_key_base64", key_fn, encode=True)

    def patch(self):
        self.patch_passport_rs()
        self.patch_passport_rp()
        self.patch_passport_sp()
