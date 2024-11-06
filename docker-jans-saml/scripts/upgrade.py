# noqa: D100
import contextlib
import json
import logging.config
import os
from collections import namedtuple

from jans.pycloudlib import get_manager
from jans.pycloudlib.persistence.couchbase import CouchbaseClient
from jans.pycloudlib.persistence.couchbase import id_from_dn
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


class CouchbaseBackend:
    def __init__(self, manager):
        self.manager = manager
        self.client = CouchbaseClient(manager)
        self.type = "couchbase"

    def get_entry(self, key, filter_="", attrs=None, **kwargs):
        bucket = kwargs.get("bucket")
        req = self.client.exec_query(
            f"SELECT META().id, {bucket}.* FROM {bucket} USE KEYS '{key}'"  # nosec: B608
        )
        if not req.ok:
            return None

        try:
            _attrs = req.json()["results"][0]
            id_ = _attrs.pop("id")
            entry = Entry(id_, _attrs)
        except IndexError:
            entry = None
        return entry

    def modify_entry(self, key, attrs=None, **kwargs):
        bucket = kwargs.get("bucket")
        del_flag = kwargs.get("delete_attr", False)
        attrs = attrs or {}

        if del_flag:
            kv = ",".join(attrs.keys())
            mod_kv = f"UNSET {kv}"
        else:
            kv = ",".join([
                "{}={}".format(k, json.dumps(v))
                for k, v in attrs.items()
            ])
            mod_kv = f"SET {kv}"

        query = f"UPDATE {bucket} USE KEYS '{key}' {mod_kv}"
        req = self.client.exec_query(query)

        if req.ok:
            resp = req.json()
            status = bool(resp["status"] == "success")
            message = resp["status"]
        else:
            status = False
            message = req.text or req.reason
        return status, message


BACKEND_CLASSES = {
    "sql": SQLBackend,
    "couchbase": CouchbaseBackend,
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
        kwargs = {}
        id_ = "ou=jans-saml,ou=configuration,o=jans"

        if self.backend.type == "sql":
            kwargs = {"table_name": "jansAppConf"}
            id_ = doc_id_from_dn(id_)
        else: # likely
            kwargs = {"bucket": os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")}
            id_ = id_from_dn(id_)

        entry = self.backend.get_entry(id_, **kwargs)

        if not entry:
            return

        if self.backend.type != "couchbase":
            with contextlib.suppress(json.decoder.JSONDecodeError):
                entry.attrs["jansConfDyn"] = json.loads(entry.attrs["jansConfDyn"])

        conf, should_update = _transform_saml_dynamic_config(entry.attrs["jansConfDyn"])

        if should_update:
            if self.backend.type != "couchbase":
                entry.attrs["jansConfDyn"] = json.dumps(conf)

            entry.attrs["jansRevision"] += 1
            self.backend.modify_entry(entry.id, entry.attrs, **kwargs)


def main():  # noqa: D103
    manager = get_manager()

    with manager.lock.create_lock("saml-upgrade"):
        upgrade = Upgrade(manager)
        upgrade.invoke()


if __name__ == "__main__":
    main()
