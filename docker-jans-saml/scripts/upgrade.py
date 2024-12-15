# noqa: D100
import contextlib
import json
import logging.config
import os
from collections import namedtuple

from jans.pycloudlib import get_manager
from jans.pycloudlib.persistence.sql import doc_id_from_dn
from jans.pycloudlib.persistence.sql import SqlClient
from jans.pycloudlib.persistence.utils import PersistenceMapper

from settings import LOGGING_CONFIG

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("jans-saml")

Entry = namedtuple("Entry", ["id", "attrs"])


def _transform_saml_dynamic_config(conf):
    should_update = False

    # add missing config (if not exist)
    for k, v in [
        ("scope", "openid"),
        ("tokenUrl", "/realms/%s/protocol/openid-connect/token"),
        ("idpUrl", "/admin/realms/%s/identity-provider/instances"),
        ("idpMetadataImportUrl", "/admin/realms/%s/identity-provider/import-config"),
        ("kcAttributes", [
            "alias",
            "displayName",
            "internalId",
            "providerId",
            "enabled",
            "trustEmail",
            "storeToken",
            "addReadTokenRoleOnCreate",
            "authenticateByDefault",
            "linkOnly",
            "firstBrokerLoginFlowAlias",
            "postBrokerLoginFlowAlias",
            "config",
        ]),
        ("kcSamlConfig", [
            "signingCertificate",
            "validateSignature",
            "singleLogoutServiceUrl",
            "nameIDPolicyFormat",
            "principalAttribute",
            "principalType",
            "idpEntityId",
            "singleSignOnServiceUrl",
            "encryptionPublicKey",
        ]),
        ("extIDPTokenUrl", "/realms/%s/broker/%s/token"),
        ("extIDPRedirectUrl", "/kc/realms/%s/protocol/openid-connect/auth?client_id=%s&redirect_uri=%s&response_type=%s&kc_idp_hint=%s"),
        ("setConfigDefaultValue", True),
    ]:
        if k not in conf:
            conf[k] = v
            should_update = True

    # new attrs in kcSamlConfig
    for new_attr in ["principalAttribute", "principalType"]:
        if new_attr not in conf["kcSamlConfig"]:
            conf["kcSamlConfig"].append(new_attr)
            should_update = True

    # return modified config (if any) and update flag
    return conf, should_update


class SQLBackend:
    def __init__(self, manager):
        self.manager = manager
        self.client = SqlClient(manager)
        self.type = "sql"

    def get_entry(self, key, filter_="", attrs=None, **kwargs):
        table_name = kwargs.get("table_name")
        entry = self.client.get(table_name, key, attrs)

        if not entry:
            return None
        return Entry(key, entry)

    def modify_entry(self, key, attrs=None, **kwargs):
        attrs = attrs or {}
        table_name = kwargs.get("table_name")
        return self.client.update(table_name, key, attrs), ""


BACKEND_CLASSES = {
    "sql": SQLBackend,
}


class Upgrade:
    def __init__(self, manager):
        self.manager = manager

        mapper = PersistenceMapper()

        backend_cls = BACKEND_CLASSES[mapper.mapping["default"]]
        self.backend = backend_cls(manager)

    def invoke(self):
        logger.info("Running upgrade process (if required)")
        self.update_saml_dynamic_config()

    def update_saml_dynamic_config(self):
        kwargs = {"table_name": "jansAppConf"}
        id_ = doc_id_from_dn("ou=jans-saml,ou=configuration,o=jans")

        entry = self.backend.get_entry(id_, **kwargs)

        if not entry:
            return

        with contextlib.suppress(json.decoder.JSONDecodeError):
            entry.attrs["jansConfDyn"] = json.loads(entry.attrs["jansConfDyn"])

        conf, should_update = _transform_saml_dynamic_config(entry.attrs["jansConfDyn"])

        if should_update:
            entry.attrs["jansConfDyn"] = json.dumps(conf)
            entry.attrs["jansRevision"] += 1
            self.backend.modify_entry(entry.id, entry.attrs, **kwargs)


def main():  # noqa: D103
    manager = get_manager()

    with manager.create_lock("saml-upgrade"):
        upgrade = Upgrade(manager)
        upgrade.invoke()


if __name__ == "__main__":
    main()
