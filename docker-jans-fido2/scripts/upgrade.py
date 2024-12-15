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
logger = logging.getLogger("jans-fido2")

Entry = namedtuple("Entry", ["id", "attrs"])


def _transform_fido2_dynamic_config(conf):
    should_update = False

    # add missing config (if not exist)
    for k, v in [
        ("superGluuEnabled", False),
        ("metadataUrlsProvider", ""),
        ("errorReasonEnabled", False),
        ("skipDownloadMdsEnabled", False),
        ("attestationMode", "monitor"),
        ("sessionIdPersistInCache", False),
        ("assertionOptionsGenerateEndpointEnabled", True),
    ]:
        # dont update if key exists
        if k in conf:
            continue

        conf[k] = v
        should_update = True

    # return modified config (if any) and update flag
    return conf, should_update


def _transform_fido2_static_config(conf):
    should_update = False

    # add missing config (if not exist)
    for k, v in [
        ("attributes", "ou=attributes,o=jans"),
        ("fido2Attestation", "ou=fido2_register,ou=fido2,o=jans"),
        ("fido2Assertion", "ou=fido2_auth,ou=fido2,o=jans"),
    ]:
        # dont update if key exists
        if k in conf["baseDn"]:
            continue

        conf["baseDn"][k] = v
        should_update = True

    # return modified config (if any) and update flag
    return conf, should_update


def _transform_fido2_error_config(conf):
    should_update = False

    if not conf:
        with open("/app/templates/jans-fido2/jans-fido2-errors.json") as f:
            conf = json.loads(f.read())
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
        self.update_fido2_dynamic_config()
        self.update_fido2_static_config()
        self.update_fido2_error_config()

    def update_fido2_dynamic_config(self):
        kwargs = {"table_name": "jansAppConf"}
        id_ = doc_id_from_dn("ou=jans-fido2,ou=configuration,o=jans")

        entry = self.backend.get_entry(id_, **kwargs)

        if not entry:
            return

        with contextlib.suppress(json.decoder.JSONDecodeError):
            entry.attrs["jansConfDyn"] = json.loads(entry.attrs["jansConfDyn"])

        conf, should_update = _transform_fido2_dynamic_config(entry.attrs["jansConfDyn"])

        if should_update:
            entry.attrs["jansConfDyn"] = json.dumps(conf)
            entry.attrs["jansRevision"] += 1
            self.backend.modify_entry(entry.id, entry.attrs, **kwargs)

    def update_fido2_static_config(self):
        kwargs = {"table_name": "jansAppConf"}
        id_ = doc_id_from_dn("ou=jans-fido2,ou=configuration,o=jans")

        entry = self.backend.get_entry(id_, **kwargs)

        if not entry:
            return

        with contextlib.suppress(json.decoder.JSONDecodeError):
            entry.attrs["jansConfStatic"] = json.loads(entry.attrs["jansConfStatic"])

        conf, should_update = _transform_fido2_static_config(entry.attrs["jansConfStatic"])

        if should_update:
            entry.attrs["jansConfStatic"] = json.dumps(conf)
            entry.attrs["jansRevision"] += 1
            self.backend.modify_entry(entry.id, entry.attrs, **kwargs)

    def update_fido2_error_config(self):
        kwargs = {"table_name": "jansAppConf"}
        id_ = doc_id_from_dn("ou=jans-fido2,ou=configuration,o=jans")

        entry = self.backend.get_entry(id_, **kwargs)

        if not entry:
            return

        # config maybe null
        with contextlib.suppress(json.decoder.JSONDecodeError):
            entry.attrs["jansConfErrors"] = json.loads(entry.attrs.get("jansConfErrors") or "{}")

        conf, should_update = _transform_fido2_error_config(entry.attrs["jansConfErrors"])

        if should_update:
            entry.attrs["jansConfErrors"] = json.dumps(conf)
            entry.attrs["jansRevision"] += 1
            self.backend.modify_entry(entry.id, entry.attrs, **kwargs)


def main():  # noqa: D103
    manager = get_manager()

    with manager.create_lock("fido2-upgrade"):
        upgrade = Upgrade(manager)
        upgrade.invoke()


if __name__ == "__main__":
    main()
