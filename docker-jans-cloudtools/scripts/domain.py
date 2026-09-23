import json
import logging.config

import click

from jans.pycloudlib import get_manager
from jans.pycloudlib.persistence import doc_id_from_dn

from settings import LOGGING_CONFIG
from persistence import SqlPersistence

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("cloudtools")


class Domain:
    def __init__(self, manager):
        self.manager = manager
        self.persistence = SqlPersistence(self.manager)

    def modify_auth_config(self, old_fqdn, new_fqdn):
        logger.info("Updating jans-auth configuration in persistence")
        entry = self.persistence.get_config(doc_id_from_dn("ou=jans-auth,ou=configuration,o=jans"))
        if not entry:
            return {}

        conf = json.loads(entry["jansConfDyn"])
        for k, v in conf.items():
            if all([k.endswith("Endpoint"), isinstance(v, (str, bytes)) and old_fqdn in v]):
                new_value = v.replace(old_fqdn, new_fqdn)
                logger.info("Changing %s: %s => %s", k, v, new_value)

            elif k == "ssaConfiguration" and "ssaEndpoint" in v:
                new_value = v["ssaEndpoint"].replace(old_fqdn, new_fqdn)
                logger.info("Changing %s.ssaEndpoint: %s => %s", k, v["ssaEndpoint"], new_value)

            elif k in (
                "issuer",
                "jwksUri",
                "archivedJwksUri",
                "opPolicyUri",
                "opTosUri",
                "jansId",
                "backchannelRedirectUri",
            ):
                new_value = v.replace(old_fqdn, new_fqdn)
                logger.info("Changing %s: %s => %s", k, v, new_value)
        return False

    def modify_casa_config(self, old_fqdn, new_fqdn):
        logger.info("Updating casa configuration in persistence")
        entry = self.persistence.get_config(doc_id_from_dn("ou=casa,ou=configuration,o=jans"))
        if not entry:
            return {}

        conf = json.loads(entry["jansConfApp"])
        for k, v in conf.items():
            if k == "oidc_config":
                for oidc_k in (
                    "op_host",
                    "authz_redirect_uri",
                    "post_logout_uri",
                    "frontchannel_logout_uri",
                ):
                    new_value = v[oidc_k].replace(old_fqdn, new_fqdn)
                    logger.info("Changing %s.%s: %s => %s", k, oidc_k, v[oidc_k], new_value)
        return False

    def modify_config_api_config(self, old_fqdn, new_fqdn):
        logger.info("Updating jans-config-api configuration in persistence")
        entry = self.persistence.get_config(doc_id_from_dn("ou=jans-config-api,ou=configuration,o=jans"))
        if not entry:
            return {}

        conf = json.loads(entry["jansConfDyn"])
        for k, v in conf.items():
            if all([k.endswith("Url"), isinstance(v, (str, bytes)) and old_fqdn in v]):
                new_value = v.replace(old_fqdn, new_fqdn)
                logger.info("Changing %s: %s => %s", k, v, new_value)
            elif k == "apiApprovedIssuer":
                for idx, issuer in enumerate(v):
                    if old_fqdn in issuer:
                        new_value = issuer.replace(old_fqdn, new_fqdn)
                        logger.info("Changing %s.[%s]: %s => %s", k, idx, v[idx], new_value)
        return False

    def modify_fido2_config(self, old_fqdn, new_fqdn):
        logger.info("Updating jans-fido2 configuration in persistence")
        entry = self.persistence.get_config(doc_id_from_dn("ou=jans-fido2,ou=configuration,o=jans"))
        if not entry:
            return {}

        conf = json.loads(entry["jansConfDyn"])
        for k, v in conf.items():
            if k in ("issuer", "baseEndpoint"):
                new_value = v.replace(old_fqdn, new_fqdn)
                logger.info("Changing %s: %s => %s", k, v, new_value)
            elif k == "fido2Configuration" and "rp" in v:
                for rp in v["rp"]:
                    logger.info("id => %s, origin => %s", rp["id"], rp["origins"])
        return False

    def change_fqdn(self, new_fqdn):
        old_fqdn = self.manager.config.get("hostname")

        logger.info("Detected old FQDN from existing config: %s", old_fqdn)
        logger.info("Changing FQDN from %s to %s", old_fqdn, new_fqdn)

        # self.modify_auth_config(old_fqdn, new_fqdn)
        # self.modify_casa_config(old_fqdn, new_fqdn)
        # self.modify_config_api_config(old_fqdn, new_fqdn)
        self.modify_fido2_config(old_fqdn, new_fqdn)


@click.command
@click.argument("fqdn")
def change_fqdn(fqdn):
    """Change FQDN."""
    manager = get_manager()
    domain = Domain(manager)
    domain.change_fqdn(fqdn)


if __name__ == "__main__":
    change_fqdn(prog_name="change-fqdn")
