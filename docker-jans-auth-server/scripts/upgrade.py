# noqa: D100
import contextlib
import json
import logging.config
import os
from collections import namedtuple

from jans.pycloudlib import get_manager
from jans.pycloudlib.persistence.sql import SqlClient
from jans.pycloudlib.persistence.sql import doc_id_from_dn
from jans.pycloudlib.persistence.utils import PersistenceMapper
from jans.pycloudlib.utils import as_boolean

from settings import LOGGING_CONFIG
from utils import parse_lock_swagger_file

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("jans-auth")

Entry = namedtuple("Entry", ["id", "attrs"])


def _transform_lock_dynamic_config(conf, manager):
    should_update = False

    hostname = manager.config.get("hostname")

    # add missing top-level keys
    hostname = manager.config.get("hostname")
    for missing_key, value in [
        ("policiesJsonUrisAuthorizationToken", conf.pop("policiesJsonUrisAccessToken", "")),
        ("policiesZipUris", []),
        ("policiesZipUrisAuthorizationToken", conf.pop("policiesZipUrisAccessToken", "")),
        ("baseEndpoint", f"https://{hostname}/jans-auth/v1"),
        ("clientId", manager.config.get("lock_client_id")),
        ("clientPassword", manager.secret.get("lock_client_encoded_pw")),
        ("tokenUrl", f"https://{hostname}/jans-auth/restv1/token"),
        ("endpointDetails", {
            "jans-config-api/lock/audit/telemetry": [
                "https://jans.io/oauth/lock/telemetry.readonly",
                "https://jans.io/oauth/lock/telemetry.write"
            ],
            "jans-config-api/lock/audit/log": [
                "https://jans.io/oauth/lock/log.write"
            ],
            "jans-config-api/lock/audit/health": [
                "https://jans.io/oauth/lock/health.readonly",
                "https://jans.io/oauth/lock/health.write"
            ],
        }),
        ("endpointGroups", {
            "audit": [
                "telemetry",
                "health",
                "log"
            ],
        }),
        ("statEnabled", True),
        ("messageConsumerType", "DISABLED"),
        ("policyConsumerType", "DISABLED"),
    ]:
        if missing_key not in conf:
            conf[missing_key] = value
            should_update = True

    # channel rename
    if "jans_token" not in conf["tokenChannels"]:
        conf["tokenChannels"].append("jans_token")

        # remove old channel
        with contextlib.suppress(ValueError):
            conf["tokenChannels"].remove("id_token")
        should_update = True

    # base endpoint is changed from jans-lock to jans-auth
    if conf["baseEndpoint"] != f"https://{hostname}/jans-auth/v1":
        conf["baseEndpoint"] = f"https://{hostname}/jans-auth/v1"
        should_update = True

    # new audit endpoint groups
    for audit_endpoint in ["telemetry/bulk", "health/bulk", "log/bulk"]:
        if audit_endpoint in conf["endpointGroups"]["audit"]:
            continue
        conf["endpointGroups"]["audit"].append(audit_endpoint)
        should_update = True

    # new endpoint details
    for k, v in {
        "jans-config-api/lock/audit/telemetry/bulk": ["https://jans.io/oauth/lock/telemetry.readonly", "https://jans.io/oauth/lock/telemetry.write"],
        "jans-config-api/lock/audit/log/bulk": ["https://jans.io/oauth/lock/log.write"],
        "jans-config-api/lock/audit/health/bulk": ["https://jans.io/oauth/lock/health.readonly", "https://jans.io/oauth/lock/health.write"],
    }.items():
        if k in conf["endpointDetails"]:
            continue
        conf["endpointDetails"][k] = v
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

        if as_boolean(os.environ.get("CN_LOCK_ENABLED", "false")):
            self.update_lock_dynamic_config()
            self.update_lock_client_scopes()
            self.update_lock_error_config()
            self.update_lock_static_config()

    def update_lock_dynamic_config(self):
        kwargs = {"table_name": "jansAppConf"}
        id_ = doc_id_from_dn("ou=jans-lock,ou=configuration,o=jans")

        entry = self.backend.get_entry(id_, **kwargs)

        if not entry:
            return

        with contextlib.suppress(json.decoder.JSONDecodeError):
            entry.attrs["jansConfDyn"] = json.loads(entry.attrs["jansConfDyn"])

        conf, should_update = _transform_lock_dynamic_config(entry.attrs["jansConfDyn"], self.manager)

        if should_update:
            entry.attrs["jansConfDyn"] = json.dumps(conf)
            entry.attrs["jansRevision"] += 1
            self.backend.modify_entry(entry.id, entry.attrs, **kwargs)

    def get_all_scopes(self):
        kwargs = {"table_name": "jansScope"}
        entries = self.backend.search_entries(None, **kwargs)

        return {
            entry.attrs["jansId"]: entry.attrs.get("dn") or entry.id
            for entry in entries
        }

    def update_lock_client_scopes(self):
        kwargs = {"table_name": "jansClnt"}
        client_id = self.manager.config.get("lock_client_id")
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
        swagger = parse_lock_swagger_file()
        lock_jans_ids = list(swagger["components"]["securitySchemes"]["oauth2"]["flows"]["clientCredentials"]["scopes"].keys())
        lock_scopes = list({
            dn for jid, dn in all_scopes.items()
            if jid in lock_jans_ids
        })
        new_client_scopes += lock_scopes

        # find missing scopes from the client
        if diff := list(set(new_client_scopes).difference(client_scopes)):
            if not self.backend.client.use_simple_json:
                entry.attrs["jansScope"]["v"] = client_scopes + diff
            else:
                entry.attrs["jansScope"] = client_scopes + diff
            self.backend.modify_entry(entry.id, entry.attrs, **kwargs)

    def update_lock_error_config(self):
        kwargs = {"table_name": "jansAppConf"}
        id_ = doc_id_from_dn("ou=jans-lock,ou=configuration,o=jans")

        entry = self.backend.get_entry(id_, **kwargs)

        if not entry:
            return

        with contextlib.suppress(json.decoder.JSONDecodeError):
            entry.attrs["jansConfErrors"] = json.loads(entry.attrs["jansConfErrors"])

        with open("/app/templates/jans-lock/errors.json") as f:
            conf = json.loads(f.read())

        if conf != entry.attrs["jansConfErrors"]:
            entry.attrs["jansConfErrors"] = json.dumps(conf)
            entry.attrs["jansRevision"] += 1
            self.backend.modify_entry(entry.id, entry.attrs, **kwargs)

    def update_lock_static_config(self):
        kwargs = {"table_name": "jansAppConf"}
        id_ = doc_id_from_dn("ou=jans-lock,ou=configuration,o=jans")

        entry = self.backend.get_entry(id_, **kwargs)

        if not entry:
            return

        with contextlib.suppress(json.decoder.JSONDecodeError):
            entry.attrs["jansConfStatic"] = json.loads(entry.attrs["jansConfStatic"])

        with open("/app/templates/jans-lock/static-conf.json") as f:
            conf = json.loads(f.read())

        if conf != entry.attrs["jansConfStatic"]:
            entry.attrs["jansConfStatic"] = json.dumps(conf)
            entry.attrs["jansRevision"] += 1
            self.backend.modify_entry(entry.id, entry.attrs, **kwargs)


def main():  # noqa: D103
    manager = get_manager()

    with manager.create_lock("auth-upgrade"):
        upgrade = Upgrade(manager)
        upgrade.invoke()


if __name__ == "__main__":
    main()
