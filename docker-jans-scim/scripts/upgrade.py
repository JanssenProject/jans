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
from utils import parse_swagger_file

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("jans-scim")

Entry = namedtuple("Entry", ["id", "attrs"])


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

    def search_entries(self, key, filter_="", attrs=None, **kwargs):
        attrs = attrs or {}
        table_name = kwargs.get("table_name")
        return [
            Entry(entry["doc_id"], entry)
            for entry in self.client.search(table_name, attrs)
        ]


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
        self.update_client_scopes()
        self.update_scim_dynamic_config()

    def get_all_scopes(self):
        kwargs = {"table_name": "jansScope"}
        entries = self.backend.search_entries(None, **kwargs)

        return {
            entry.attrs["jansId"]: entry.attrs.get("dn") or entry.id
            for entry in entries
        }

    def update_client_scopes(self):
        kwargs = {"table_name": "jansClnt"}
        client_id = self.manager.config.get("scim_client_id")
        id_ = doc_id_from_dn(f"inum={client_id},ou=clients,o=jans")

        entry = self.backend.get_entry(id_, **kwargs)

        if not entry:
            return

        if not self.backend.client.use_simple_json:
            client_scopes = entry.attrs["jansScope"]["v"]
        else:
            client_scopes = entry.attrs.get("jansScope") or []

        if not isinstance(client_scopes, list):
            client_scopes = [client_scopes]

        # all scopes mapping from persistence
        all_scopes = self.get_all_scopes()

        # all potential scopes for client
        new_client_scopes = []

        # extract config_api scopes within range of jansId defined in swagger
        swagger = parse_swagger_file()
        scim_jans_ids = list(swagger["components"]["securitySchemes"]["scim_oauth"]["flows"]["clientCredentials"]["scopes"].keys())
        scim_scopes = list({
            dn for jid, dn in all_scopes.items()
            if jid in scim_jans_ids
        })
        new_client_scopes += scim_scopes

        # find missing scopes from the client
        if diff := list(set(new_client_scopes).difference(client_scopes)):
            if not self.backend.client.use_simple_json:
                entry.attrs["jansScope"]["v"] = client_scopes + diff
            else:
                entry.attrs["jansScope"] = client_scopes + diff
            self.backend.modify_entry(entry.id, entry.attrs, **kwargs)

    def update_scim_dynamic_config(self):
        kwargs = {"table_name": "jansAppConf"}
        id_ = doc_id_from_dn("ou=jans-scim,ou=configuration,o=jans")

        entry = self.backend.get_entry(id_, **kwargs)

        if not entry:
            return

        with contextlib.suppress(json.decoder.JSONDecodeError):
            entry.attrs["jansConfDyn"] = json.loads(entry.attrs["jansConfDyn"])

        conf, should_update = _transform_scim_dynamic_config(entry.attrs["jansConfDyn"])

        if should_update:
            entry.attrs["jansConfDyn"] = json.dumps(conf)
            entry.attrs["jansRevision"] += 1
            self.backend.modify_entry(entry.id, entry.attrs, **kwargs)


def main():
    manager = get_manager()

    with manager.create_lock("scim-upgrade"):
        upgrade = Upgrade(manager)
        upgrade.invoke()


def _transform_scim_dynamic_config(conf):
    should_update = False

    # top-level config that need to be added (if missing)
    for missing_key, value in [
        ("skipDefinedPasswordValidation", False),
    ]:
        if missing_key not in conf:
            conf[missing_key] = value
            should_update = True

    # finalized conf and flag to determine update process
    return conf, should_update


if __name__ == "__main__":
    main()
