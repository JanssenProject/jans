import json
import logging.config
import os
from collections import namedtuple

from jans.pycloudlib import get_manager
from jans.pycloudlib.persistence import CouchbaseClient
from jans.pycloudlib.persistence import LdapClient
from jans.pycloudlib.persistence import SpannerClient
from jans.pycloudlib.persistence import SqlClient
from jans.pycloudlib.persistence import PersistenceMapper
from jans.pycloudlib.persistence import doc_id_from_dn
from jans.pycloudlib.persistence import id_from_dn

from settings import LOGGING_CONFIG
from utils import parse_swagger_file

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("entrypoint")

Entry = namedtuple("Entry", ["id", "attrs"])


class LDAPBackend:
    def __init__(self, manager):
        self.manager = manager
        self.client = LdapClient(manager)
        self.type = "ldap"

    def format_attrs(self, attrs):
        _attrs = {}
        for k, v in attrs.items():
            if len(v) < 2:
                v = v[0]
            _attrs[k] = v
        return _attrs

    def get_entry(self, key, filter_="", attrs=None, **kwargs):
        filter_ = filter_ or "(objectClass=*)"

        entry = self.client.get(key, filter_=filter_, attributes=attrs)
        if not entry:
            return None
        return Entry(entry.entry_dn, self.format_attrs(entry.entry_attributes_as_dict))

    def modify_entry(self, key, attrs=None, **kwargs):
        attrs = attrs or {}
        del_flag = kwargs.get("delete_attr", False)

        if del_flag:
            mod = self.client.MODIFY_DELETE
        else:
            mod = self.client.MODIFY_REPLACE

        for k, v in attrs.items():
            if not isinstance(v, list):
                v = [v]
            attrs[k] = [(mod, v)]
        return self.client.modify(key, attrs)

    def search_entries(self, key, filter_="", attrs=None, **kwargs):
        filter_ = filter_ or "(objectClass=*)"
        entries = self.client.search(key, filter_, attrs)

        return [
            Entry(entry.entry_dn, self.format_attrs(entry.entry_attributes_as_dict))
            for entry in entries
        ]


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


class CouchbaseBackend:
    def __init__(self, manager):
        self.manager = manager
        self.client = CouchbaseClient(manager)
        self.type = "couchbase"

    def get_entry(self, key, filter_="", attrs=None, **kwargs):
        bucket = kwargs.get("bucket")
        req = self.client.exec_query(
            f"SELECT META().id, {bucket}.* FROM {bucket} USE KEYS '{key}'"
        )
        if not req.ok:
            return

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

    def search_entries(self, key, filter_="", attrs=None, **kwargs):
        bucket = kwargs.get("bucket")
        req = self.client.exec_query(
            f"SELECT META().id, {bucket}.* FROM {bucket} {filter_}"
        )
        if not req.ok:
            return []

        entries = []
        for item in req.json()["results"]:
            id_ = item.pop("id")
            entries.append(Entry(id_, item))
        return entries


class SpannerBackend:
    def __init__(self, manager):
        self.manager = manager
        self.client = SpannerClient(manager)
        self.type = "spanner"

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
    "couchbase": CouchbaseBackend,
    "spanner": SpannerBackend,
    "ldap": LDAPBackend,
}


class Upgrade:
    def __init__(self, manager):
        self.manager = manager

        mapper = PersistenceMapper()

        backend_cls = BACKEND_CLASSES[mapper.mapping["default"]]
        self.backend = backend_cls(manager)

    def invoke(self):
        logger.info("Running upgrade process (if required)")
        # temporarily disable dynamic scopes creation
        # see https://github.com/JanssenProject/jans/issues/2869
        # self.update_client_scopes()

    def get_all_scopes(self):
        if self.backend.type in ("sql", "spanner"):
            kwargs = {"table_name": "jansScope"}
            entries = self.backend.search_entries(None, **kwargs)
        elif self.backend.type == "couchbase":
            kwargs = {"bucket": os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")}
            entries = self.backend.search_entries(
                None, filter_="WHERE objectClass = 'jansScope'", **kwargs
            )
        else:
            # likely ldap
            entries = self.backend.search_entries(
                "ou=scopes,o=jans", filter_="(objectClass=jansScope)"
            )

        return {
            entry.attrs["jansId"]: entry.attrs.get("dn") or entry.id
            for entry in entries
        }

    def update_client_scopes(self):
        kwargs = {}
        client_id = self.manager.config.get("scim_client_id")
        id_ = f"inum={client_id},ou=clients,o=jans"

        if self.backend.type in ("sql", "spanner"):
            kwargs = {"table_name": "jansClnt"}
            id_ = doc_id_from_dn(id_)
        elif self.backend.type == "couchbase":
            kwargs = {"bucket": os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")}
            id_ = id_from_dn(id_)

        entry = self.backend.get_entry(id_, **kwargs)

        if not entry:
            return

        if self.backend.type == "sql" and self.backend.client.dialect == "mysql":
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
        diff = list(set(new_client_scopes).difference(client_scopes))

        if diff:
            if self.backend.type == "sql" and self.backend.client.dialect == "mysql":
                entry.attrs["jansScope"]["v"] = client_scopes + diff
            else:
                entry.attrs["jansScope"] = client_scopes + diff
            self.backend.modify_entry(entry.id, entry.attrs, **kwargs)


def main():
    manager = get_manager()
    upgrade = Upgrade(manager)
    upgrade.invoke()


if __name__ == "__main__":
    main()
