import base64
import json
import logging.config
import sys

from base_handler import BaseHandler
from settings import LOGGING_CONFIG
from utils import generate_openid_keys

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("certmanager")


class ScimHandler(BaseHandler):
    def patch_scim_rs(self):
        jks_fn = self.manager.config.get("scim_rs_client_jks_fn")
        jwks_fn = self.manager.config.get("scim_rs_client_jwks_fn")

        logger.info(f"Generating new {jks_fn} and {jwks_fn}")

        out, err, retcode = generate_openid_keys(
            self.manager.secret.get("scim_rs_client_jks_pass"),
            jks_fn,
            jwks_fn,
            self.manager.config.get("default_openid_jks_dn_name"),
        )
        if retcode != 0:
            logger.error(f"Unable to generate SCIM RS keys; reason={err.decode()}")
            sys.exit(1)

        cert_alg = self.manager.config.get("scim_rs_client_cert_alg")
        cert_alias = ""
        for key in json.loads(out)["keys"]:
            if key["alg"] == cert_alg:
                cert_alias = key["kid"]
                break

        if not self.dry_run:
            self.manager.secret.set("scim_rs_client_base64_jwks", base64.b64encode(out))
            self.manager.config.set("scim_rs_client_cert_alias", cert_alias)
            self.manager.secret.from_file(
                "scim_rs_jks_base64", jks_fn, encode=True, binary_mode=True,
            )

    def patch_scim_rp(self):
        jks_fn = self.manager.config.get("scim_rp_client_jks_fn")
        jwks_fn = self.manager.config.get("scim_rp_client_jwks_fn")

        logger.info(f"Generating new {jks_fn} and {jwks_fn}")

        out, err, retcode = generate_openid_keys(
            self.manager.secret.get("scim_rp_client_jks_pass"),
            jks_fn,
            jwks_fn,
            self.manager.config.get("default_openid_jks_dn_name"),
        )
        if retcode != 0:
            logger.error(f"Unable to generate SCIM RP keys; reason={err.decode()}")
            sys.exit(1)

        if not self.dry_run:
            self.manager.secret.set("scim_rp_client_base64_jwks", base64.b64encode(out))
            self.manager.secret.from_file(
                "scim_rp_jks_base64", jks_fn, encode=True, binary_mode=True,
            )

    def patch(self):
        self.patch_scim_rs()
        self.patch_scim_rp()
