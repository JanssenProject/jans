import contextlib
import json
import logging.config
from collections import namedtuple

from jans.pycloudlib import get_manager
from jans.pycloudlib.persistence.sql import SqlClient
from jans.pycloudlib.persistence.utils import PersistenceMapper
from jans.pycloudlib.persistence.sql import doc_id_from_dn

from settings import LOGGING_CONFIG
from utils import get_config_api_scope_mapping

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("jans-config-api")

Entry = namedtuple("Entry", ["id", "attrs"])


def _transform_api_dynamic_config(conf):
    should_update = False

    # top-level config that need to be added (if missing)
    for missing_key, value in [
        ("userExclusionAttributes", ["userPassword"]),
        ("userMandatoryAttributes", [
            "mail",
            "displayName",
            "status",
            "userPassword",
            "givenName",
        ]),
        ("agamaConfiguration", {
            "mandatoryAttributes": [
                "qname",
                "source",
            ],
            "optionalAttributes": [
                "serialVersionUID",
                "enabled",
            ],
        }),
        ("auditLogConf", {
            "enabled": True,
            "headerAttributes": ["User-inum"],
        }),
        ("dataFormatConversionConf", {
            "enabled": True,
            "ignoreHttpMethod": [
                "@jakarta.ws.rs.GET()",
            ],
        }),
        ("customAttributeValidationEnabled", True),
        ("disableLoggerTimer", False),
        ("disableAuditLogger", False),
        ("assetMgtConfiguration", {}),
        ("maxCount", 200),
        ("acrValidationEnabled", True),
        ("serviceName", "jans-config-api"),
        ("acrExclusionList", ["simple_password_auth"]),
    ]:
        if missing_key not in conf:
            conf[missing_key] = value
            should_update = True

    if "plugins" not in conf:
        conf["plugins"] = []

    # current plugin names to lookup to
    plugins_names = tuple(plugin["name"] for plugin in conf["plugins"])

    with open("/tmp/config-api.dynamic-conf.json") as f:  # nosec: B108
        fmt_dynamic_conf = json.loads(f.read())

    for supported_plugin in fmt_dynamic_conf["plugins"]:
        if supported_plugin["name"] not in plugins_names:
            conf["plugins"].append(supported_plugin)
            should_update = True

    # userMandatoryAttributes.jansStatus is changed to userMandatoryAttributes.status
    if "jansStatus" in conf["userMandatoryAttributes"]:
        conf["userMandatoryAttributes"].remove("jansStatus")
        should_update = True

    if "status" not in conf["userMandatoryAttributes"]:
        conf["userMandatoryAttributes"].append("status")
        should_update = True

    if "smallryeHealthRootPath" in conf:
        conf.pop("smallryeHealthRootPath", None)
        should_update = True

    # asset management top-level keys
    asset_attrs = {
        "assetMgtEnabled": conf.pop("assetMgtEnabled", True),
        "assetServerUploadEnabled": True,
        "fileExtensionValidationEnabled": True,
        "moduleNameValidationEnabled": True,
        "assetDirMapping": [],
    }
    for k, v in asset_attrs.items():
        if k not in conf["assetMgtConfiguration"]:
            conf["assetMgtConfiguration"][k] = v
            should_update = True

    # add missing dir mapping
    dir_mapping_names = tuple(dir_["directory"] for dir_ in conf["assetMgtConfiguration"]["assetDirMapping"])

    for supported_dir_mapping in fmt_dynamic_conf["assetMgtConfiguration"]["assetDirMapping"]:
        if supported_dir_mapping["directory"] not in dir_mapping_names:
            conf["assetMgtConfiguration"]["assetDirMapping"].append(supported_dir_mapping)
            should_update = True

    for idx, dir_mapping in enumerate(conf["assetMgtConfiguration"]["assetDirMapping"]):
        match dir_mapping["directory"]:
            # add missing service module for `/opt/jans/jetty/%s/custom/libs` dir mapping
            case "/opt/jans/jetty/%s/custom/libs":
                for svc_module in ["jans-lock", "jans-link"]:
                    if svc_module in dir_mapping["jansServiceModule"]:
                        continue

                    conf["assetMgtConfiguration"]["assetDirMapping"][idx]["jansServiceModule"].append(svc_module)
                    should_update = True

            # remove `/opt/jans/jetty/%s/custom-libs` dir mapping
            case "/opt/jans/jetty/%s/custom-libs":
                conf["assetMgtConfiguration"]["assetDirMapping"].pop(idx)
                should_update = True

    # finalized conf and flag to determine update process
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
        self.update_client_redirect_uri()
        self.update_api_dynamic_config()

        # add missing scopes into internal config-api client (if enabled)
        self.update_client_scopes()
        self.update_test_client_scopes()

        # creatorAttrs data type has been changed
        self.update_scope_creator_attrs()

    def update_client_redirect_uri(self):
        kwargs = {"table_name": "jansClnt"}
        jca_client_id = self.manager.config.get("jca_client_id")
        id_ = doc_id_from_dn(f"inum={jca_client_id},ou=clients,o=jans")

        entry = self.backend.get_entry(id_, **kwargs)

        if not entry:
            return

        should_update = False
        hostname = self.manager.config.get("hostname")

        if not self.backend.client.use_simple_json:
            if f"https://{hostname}/admin" not in entry.attrs["jansRedirectURI"]["v"]:
                entry.attrs["jansRedirectURI"]["v"].append(f"https://{hostname}/admin")
                should_update = True
        else:
            if f"https://{hostname}/admin" not in entry.attrs["jansRedirectURI"]:
                entry.attrs["jansRedirectURI"].append(f"https://{hostname}/admin")
                should_update = True

        if should_update:
            self.backend.modify_entry(entry.id, entry.attrs, **kwargs)

    def update_api_dynamic_config(self):
        kwargs = {"table_name": "jansAppConf"}
        id_ = doc_id_from_dn("ou=jans-config-api,ou=configuration,o=jans")

        entry = self.backend.get_entry(id_, **kwargs)

        if not entry:
            return

        with contextlib.suppress(json.decoder.JSONDecodeError):
            entry.attrs["jansConfDyn"] = json.loads(entry.attrs["jansConfDyn"])

        conf, should_update = _transform_api_dynamic_config(entry.attrs["jansConfDyn"])

        if should_update:
            entry.attrs["jansConfDyn"] = json.dumps(conf)
            entry.attrs["jansRevision"] += 1
            self.backend.modify_entry(entry.id, entry.attrs, **kwargs)

    def update_client_scopes(self):
        kwargs = {"table_name": "jansClnt"}
        client_id = self.manager.config.get("jca_client_id")
        id_ = doc_id_from_dn(f"inum={client_id},ou=clients,o=jans")

        entry = self.backend.get_entry(id_, **kwargs)

        if not entry:
            return

        if not self.backend.client.use_simple_json:
            client_scopes = entry.attrs["jansScope"]["v"]
        else:
            client_scopes = entry.attrs["jansScope"]

        if not isinstance(client_scopes, list):
            client_scopes = [client_scopes]

        # all potential new scopes for client
        scope_mapping = get_config_api_scope_mapping()
        new_client_scopes = [f"inum={inum},ou=scopes,o=jans" for inum in scope_mapping.keys()]

        # find missing scopes from the client
        if diff := list(set(new_client_scopes).difference(client_scopes)):
            if not self.backend.client.use_simple_json:
                entry.attrs["jansScope"]["v"] = client_scopes + diff
            else:
                entry.attrs["jansScope"] = client_scopes + diff
            self.backend.modify_entry(entry.id, entry.attrs, **kwargs)

    def update_test_client_scopes(self):
        test_client_id = self.manager.config.get("test_client_id")
        id_ = doc_id_from_dn(f"inum={test_client_id},ou=clients,o=jans")
        kwargs = {"table_name": "jansClnt"}

        entry = self.backend.get_entry(id_, **kwargs)

        if not entry:
            return

        if not self.backend.client.use_simple_json:
            client_scopes = entry.attrs["jansScope"]["v"]
        else:
            client_scopes = entry.attrs["jansScope"]

        if not isinstance(client_scopes, list):
            client_scopes = [client_scopes]

        if self.backend.type == "sql":
            scopes = [
                scope_entry.attrs["dn"]
                for scope_entry in self.backend.search_entries("", **{"table_name": "jansScope"})
            ]
        else:
            scopes = [
                scope_entry.id
                for scope_entry in self.backend.search_entries("ou=scopes,o=jans")
            ]

        # find missing scopes from the client
        if diff := list(set(scopes).difference(client_scopes)):
            if not self.backend.client.use_simple_json:
                entry.attrs["jansScope"]["v"] = client_scopes + diff
            else:
                entry.attrs["jansScope"] = client_scopes + diff
            self.backend.modify_entry(entry.id, entry.attrs, **kwargs)

    def update_scope_creator_attrs(self):
        kwargs = {}

        if self.backend.type != "sql":
            return

        kwargs.update({"table_name": "jansScope"})
        entries = self.backend.search_entries("", **kwargs)

        for entry in entries:
            if not self.backend.client.use_simple_json:
                creator_attrs = (entry.attrs.get("creatorAttrs") or {}).get("v") or []
            else:
                creator_attrs = entry.attrs.get("creatorAttrs") or []

            if not isinstance(creator_attrs, list):
                creator_attrs = [creator_attrs]

            new_creator_attrs = []

            # check the type of attr
            for _, attr in enumerate(creator_attrs):
                with contextlib.suppress(TypeError, json.decoder.JSONDecodeError):
                    # migrating from old data, i.e. `{"v": ["{}"]}`
                    attr = json.loads(attr)

                if isinstance(attr, str):
                    # migrating from old data, i.e. `{"v": ["\"{}\""]}`
                    attr = json.loads(attr.strip('"'))
                new_creator_attrs.append(attr)

            if new_creator_attrs != creator_attrs:
                if not self.backend.client.use_simple_json:
                    entry.attrs["creatorAttrs"]["v"] = new_creator_attrs
                else:
                    entry.attrs["creatorAttrs"] = new_creator_attrs
                self.backend.modify_entry(entry.id, entry.attrs, **kwargs)


def main():
    manager = get_manager()

    with manager.create_lock("config-api-upgrade"):
        upgrade = Upgrade(manager)
        upgrade.invoke()


if __name__ == "__main__":
    main()
