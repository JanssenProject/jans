import contextlib
import json
import logging.config
import os
from collections import namedtuple

from ldif import LDIFParser

from jans.pycloudlib import get_manager
from jans.pycloudlib.persistence.sql import SqlClient
from jans.pycloudlib.persistence.sql import doc_id_from_dn
from jans.pycloudlib.persistence.utils import PersistenceMapper
from jans.pycloudlib.persistence.sql import get_sql_password
from jans.pycloudlib.utils import as_boolean
from jans.pycloudlib.utils import encode_text

from settings import LOGGING_CONFIG
from utils import get_role_scope_mappings
from hooks import transform_auth_dynamic_config_hook

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("persistence-loader")

Entry = namedtuple("Entry", ["id", "attrs"])

manager = get_manager()

#: ID of jans-auth config
JANS_AUTH_CONFIG_DN = "ou=jans-auth,ou=configuration,o=jans"

#: View profile scope
JANS_PROFILE_SCOPE_DN = "inum=43F1,ou=scopes,o=jans"

#: SCIM script DN
JANS_SCIM_SCRIPT_DN = "inum=2DAF-F9A5,ou=scripts,o=jans"

#: Basic script DN
JANS_BASIC_SCRIPT_DN = "inum=A51E-76DA,ou=scripts,o=jans"

#: SCIM users.read scope
JANS_SCIM_USERS_READ_SCOPE_DN = "inum=1200.2B7428,ou=scopes,o=jans"

#: SCIM users.write scope
JANS_SCIM_USERS_WRITE_SCOPE_DN = "inum=1200.0A0198,ou=scopes,o=jans"

DEFAULT_JANS_ATTRS = '{"spontaneousClientId":null,"spontaneousClientScopes":null,"showInConfigurationEndpoint":true}'

#: jans_stat scope
JANS_STAT_SCOPE_DN = "inum=C4F7,ou=scopes,o=jans"


def _transform_token_server_client(attrs):
    should_update = False

    with contextlib.suppress(ValueError):
        attrs["introspectionScripts"].remove("inum=BFD5-C87D,ou=scripts,o=jans")
        attrs["introspectionScripts"].append("inum=A44E-4F3D,ou=scripts,o=jans")
        should_update = True

    # returns the modified attributes and flag to determine whether it needs update or not
    return attrs, should_update


def _transform_profile_scope(attrs):
    def modify_claims(claims):
        should_update = False

        # remove deprecated claim
        old_attr = "inum=0A01,ou=attributes,o=jans"
        if old_attr in claims:
            claims.remove(old_attr)
            should_update = True

        # new AdminUI role attribute
        new_attr = "inum=4CF1,ou=attributes,o=jans"
        if new_attr not in claims:
            claims.append(new_attr)
            should_update = True

        # returns modified claims and the flag
        return claims, should_update

    # special case for SQL-based attrs
    if isinstance(attrs["jansClaim"], dict):
        attrs["jansClaim"]["v"], should_update = modify_claims(attrs["jansClaim"]["v"])
    else:
        attrs["jansClaim"], should_update = modify_claims(attrs["jansClaim"])

    # returns the modified attributes and flag to determine whether it needs update or not
    return attrs, should_update


def collect_claim_names(ldif_file="/app/templates/attributes.ldif"):
    rows = {}
    with open("/app/templates/attributes.ldif", "rb") as fd:
        parser = LDIFParser(fd)

        for dn, entry in parser.parse():
            if "jansClaimName" not in entry:
                continue
            rows[dn] = entry["jansClaimName"][0]
    return rows


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

    def delete_entry(self, key, **kwargs):
        table_name = kwargs.get("table_name")
        return self.client.delete(table_name, key)


BACKEND_CLASSES = {
    "sql": SQLBackend,
}


class Upgrade:
    def __init__(self, manager):
        self.manager = manager

        mapper = PersistenceMapper()

        backend_cls = BACKEND_CLASSES[mapper.mapping["default"]]
        self.backend = backend_cls(manager)

        user_backend_cls = BACKEND_CLASSES[mapper.mapping["user"]]
        self.user_backend = user_backend_cls(manager)

    def invoke(self):
        logger.info("Running upgrade process (if required)")

        self.update_people_entries()
        self.update_scopes_entries()
        self.update_clients_entries()
        self.update_scim_scopes_entries()

        if hasattr(self.backend, "update_misc"):
            self.backend.update_misc()

        if as_boolean(os.environ.get("CN_PERSISTENCE_UPDATE_AUTH_DYNAMIC_CONFIG", "true")):
            self.update_auth_dynamic_config()

        self.update_auth_errors_config()
        self.update_auth_static_config()
        self.update_attributes_entries()
        self.update_scripts_entries()
        self.update_admin_ui_config()
        self.update_tui_client()
        self.update_config()

    def update_scripts_entries(self):
        kwargs = {}
        scim_id = doc_id_from_dn(JANS_SCIM_SCRIPT_DN)
        basic_id = doc_id_from_dn(JANS_BASIC_SCRIPT_DN)
        duo_id = doc_id_from_dn("inum=5018-F9CF,ou=scripts,o=jans")
        agama_id = doc_id_from_dn("inum=BADA-BADA,ou=scripts,o=jans")
        kwargs = {"table_name": "jansCustomScr"}

        # toggle scim script
        scim_entry = self.backend.get_entry(scim_id, **kwargs)
        scim_enabled = as_boolean(os.environ.get("CN_SCIM_ENABLED", False))

        if scim_entry and scim_entry.attrs["jansEnabled"] != scim_enabled:
            scim_entry.attrs["jansEnabled"] = scim_enabled
            scim_entry.attrs["jansRevision"] += 1
            self.backend.modify_entry(scim_entry.id, scim_entry.attrs, **kwargs)

        # always enable basic script
        basic_entry = self.backend.get_entry(basic_id, **kwargs)

        if basic_entry and not as_boolean(basic_entry.attrs["jansEnabled"]):
            basic_entry.attrs["jansEnabled"] = True
            basic_entry.attrs["jansRevision"] += 1
            self.backend.modify_entry(basic_entry.id, basic_entry.attrs, **kwargs)

        # delete DUO entry
        duo_entry = self.backend.get_entry(duo_id, **kwargs)
        if duo_entry and not as_boolean(os.environ.get("CN_DUO_ENABLED")):
            self.backend.delete_entry(duo_entry.id, **kwargs)

        agama_entry = self.backend.get_entry(agama_id, **kwargs)
        if agama_entry:
            if not self.backend.client.use_simple_json:
                props = agama_entry.attrs["jansConfProperty"]["v"]
            else:
                props = agama_entry.attrs["jansConfProperty"]

            if not isinstance(props, list):
                props = [props]

            props = [json.loads(prop) for prop in props]

            # filter out unwanted properties
            new_props = [
                prop for prop in props
                if prop["value1"] != "default_flow_name" and prop["value2"] != "agama_flow"
            ]

            if new_props != props:
                new_props = [json.dumps(prop) for prop in new_props]

                if not self.backend.client.use_simple_json:
                    agama_entry.attrs["jansConfProperty"]["v"] = new_props
                else:
                    agama_entry.attrs["jansConfProperty"] = new_props

                agama_entry.attrs["jansRevision"] += 1
                self.backend.modify_entry(agama_entry.id, agama_entry.attrs, **kwargs)

    def update_auth_dynamic_config(self):
        kwargs = {"table_name": "jansAppConf"}
        id_ = doc_id_from_dn(JANS_AUTH_CONFIG_DN)

        entry = self.backend.get_entry(id_, **kwargs)

        if not entry:
            return

        entry.attrs["jansConfDyn"] = json.loads(entry.attrs["jansConfDyn"])
        conf, should_update = transform_auth_dynamic_config_hook(entry.attrs["jansConfDyn"], self.manager)

        if should_update:
            entry.attrs["jansConfDyn"] = json.dumps(conf)
            entry.attrs["jansRevision"] += 1
            self.backend.modify_entry(entry.id, entry.attrs, **kwargs)

    def update_attributes_entries(self):
        def _update_claim_names():
            rows = collect_claim_names()

            for id_, claim_name in rows.items():
                id_ = doc_id_from_dn(id_)
                kwargs = {"table_name": "jansAttr"}

                entry = self.backend.get_entry(id_, **kwargs)

                if not entry:
                    return

                # jansClaimName already set
                if "jansClaimName" in entry.attrs and entry.attrs["jansClaimName"]:
                    continue

                entry.attrs["jansClaimName"] = claim_name
                self.backend.modify_entry(entry.id, entry.attrs, **kwargs)

        def _update_mobile_attr():
            kwargs = {"table_name": "jansAttr"}
            id_ = doc_id_from_dn("inum=6DA6,ou=attributes,o=jans")

            entry = self.backend.get_entry(id_, **kwargs)

            if not entry:
                return

            if not entry.attrs.get("jansMultivaluedAttr"):
                entry.attrs["jansMultivaluedAttr"] = True
                self.backend.modify_entry(entry.id, entry.attrs, **kwargs)

        _update_claim_names()
        _update_mobile_attr()

    def update_scim_scopes_entries(self):
        # add jansAttrs to SCIM users.read and users.write scopes
        for id_ in [JANS_SCIM_USERS_READ_SCOPE_DN, JANS_SCIM_USERS_WRITE_SCOPE_DN]:
            kwargs = {"table_name": "jansScope"}
            id_ = doc_id_from_dn(id_)

            entry = self.backend.get_entry(id_, **kwargs)

            if not entry:
                continue

            if "jansAttrs" not in entry.attrs:
                entry.attrs["jansAttrs"] = DEFAULT_JANS_ATTRS
                self.backend.modify_entry(entry.id, entry.attrs, **kwargs)

    def update_scopes_entries(self):
        kwargs = {"table_name": "jansScope"}
        id_ = doc_id_from_dn(JANS_PROFILE_SCOPE_DN)

        entry = self.backend.get_entry(id_, **kwargs)

        if not entry:
            return

        attrs, should_update = _transform_profile_scope(entry.attrs)
        if should_update:
            self.backend.modify_entry(entry.id, attrs, **kwargs)

    def update_people_entries(self):
        admin_inum = self.manager.config.get("admin_inum")

        id_ = doc_id_from_dn(f"inum={admin_inum},ou=people,o=jans")
        kwargs = {"table_name": "jansPerson"}

        entry = self.user_backend.get_entry(id_, **kwargs)

        if not entry:
            return

        should_update = False

        # add jansAdminUIRole and role to default admin user
        for attr_name, role_name in [
            ("jansAdminUIRole", "api-admin"),
            ("role", "CasaAdmin"),
        ]:
            if not self.user_backend.client.use_simple_json:
                old_roles = entry.attrs.get(attr_name, {}).get("v", [])
                new_roles = {"v": [role_name]}
            else:
                old_roles = entry.attrs.get(attr_name, [])
                new_roles = [role_name]

            if not old_roles:
                entry.attrs[attr_name] = roles
                should_update = True

        # set lowercased jansStatus
        if entry.attrs["jansStatus"] == "ACTIVE":
            entry.attrs["jansStatus"] = "active"
            should_update = True

        if should_update:
            self.user_backend.modify_entry(entry.id, entry.attrs, **kwargs)

    def update_clients_entries(self):
        # modify introspection script for token server client
        def _update_token_server_client():
            kwargs = {"table_name": "jansClnt"}
            token_server_admin_ui_client_id = self.manager.config.get("token_server_admin_ui_client_id")

            # admin-ui is not available
            if not token_server_admin_ui_client_id:
                return

            id_ = doc_id_from_dn(f"inum={token_server_admin_ui_client_id},ou=clients,o=jans")

            entry = self.backend.get_entry(id_, **kwargs)

            if not entry:
                return

            attrs, should_update = _transform_token_server_client(json.loads(entry.attrs["jansAttrs"]))
            if should_update:
                entry.attrs["jansAttrs"] = json.dumps(attrs)
                self.backend.modify_entry(entry.id, entry.attrs, **kwargs)

        _update_token_server_client()

    def update_admin_ui_config(self):
        kwargs = {"table_name": "jansAppConf"}
        id_ = doc_id_from_dn("ou=admin-ui,ou=configuration,o=jans")

        entry = self.backend.get_entry(id_, **kwargs)

        if not entry:
            return

        role_mapping = get_role_scope_mappings()

        try:
            conf = json.loads(entry.attrs["jansConfDyn"])
        except TypeError:
            conf = entry.attrs["jansConfDyn"]

        should_update = False

        if conf != role_mapping:
            conf = role_mapping
            should_update = True

        # licenseSpringCredentials must be removed in favor of SCAN license credentials
        if "licenseSpringCredentials" in conf:
            conf.pop("licenseSpringCredentials", None)
            should_update = True

        if should_update:
            entry.attrs["jansConfDyn"] = json.dumps(conf)
            entry.attrs["jansRevision"] += 1
            self.backend.modify_entry(entry.id, entry.attrs, **kwargs)

    def update_auth_errors_config(self):
        kwargs = {"table_name": "jansAppConf"}
        id_ = doc_id_from_dn(JANS_AUTH_CONFIG_DN)

        entry = self.backend.get_entry(id_, **kwargs)

        if not entry:
            return

        entry.attrs["jansConfErrors"] = json.loads(entry.attrs["jansConfErrors"])

        should_update = False

        # compare config from persistence with the ones from assets
        with open("/app/templates/jans-auth/jans-auth-errors.json") as f:
            new_conf = json.loads(f.read())

            if entry.attrs["jansConfErrors"] != new_conf:
                entry.attrs["jansConfErrors"] = new_conf
                should_update = True

        if should_update:
            entry.attrs["jansConfErrors"] = json.dumps(entry.attrs["jansConfErrors"])
            entry.attrs["jansRevision"] += 1
            self.backend.modify_entry(entry.id, entry.attrs, **kwargs)

    def update_auth_static_config(self):
        kwargs = {"table_name": "jansAppConf"}
        id_ = doc_id_from_dn(JANS_AUTH_CONFIG_DN)

        entry = self.backend.get_entry(id_, **kwargs)

        if not entry:
            return

        entry.attrs["jansConfStatic"] = json.loads(entry.attrs["jansConfStatic"])

        should_update = False

        # compare config from persistence with the ones from assets
        with open("/app/templates/jans-auth/jans-auth-static-conf.json") as f:
            new_conf = json.loads(f.read())

            if entry.attrs["jansConfStatic"] != new_conf:
                entry.attrs["jansConfStatic"] = new_conf
                should_update = True

        if should_update:
            entry.attrs["jansConfStatic"] = json.dumps(entry.attrs["jansConfStatic"])
            entry.attrs["jansRevision"] += 1
            self.backend.modify_entry(entry.id, entry.attrs, **kwargs)

    def update_tui_client(self):
        kwargs = {"table_name": "jansClnt"}
        tui_client_id = self.manager.config.get("tui_client_id")
        id_ = doc_id_from_dn(f"inum={tui_client_id},ou=clients,o=jans")

        entry = self.backend.get_entry(id_, **kwargs)

        if not entry:
            return

        should_update = False

        # add SSA scope inum=B9D2-D6E5,ou=scopes,o=jans to tui client
        ssa_scope = "inum=B9D2-D6E5,ou=scopes,o=jans"

        if not self.backend.client.use_simple_json:
            if ssa_scope not in entry.attrs["jansScope"]["v"]:
                entry.attrs["jansScope"]["v"].append(ssa_scope)
                should_update = True
        else:
            if ssa_scope not in entry.attrs["jansScope"]:
                entry.attrs["jansScope"].append(ssa_scope)
                should_update = True

        # use token reference
        try:
            attrs = json.loads(entry.attrs["jansAttrs"])
        except TypeError:
            attrs = entry.attrs["jansAttrs"]
        finally:
            if attrs["runIntrospectionScriptBeforeJwtCreation"] is True:
                attrs["runIntrospectionScriptBeforeJwtCreation"] = False
                should_update = True
            if "inum=2D3E.5A04,ou=scripts,o=jans" not in attrs["updateTokenScriptDns"]:
                attrs["updateTokenScriptDns"].append("inum=2D3E.5A04,ou=scripts,o=jans")
                should_update = True
            if "inum=A44E-4F3D,ou=scripts,o=jans" in attrs["introspectionScripts"]:
                attrs["introspectionScripts"].remove("inum=A44E-4F3D,ou=scripts,o=jans")
                should_update = True
            entry.attrs["jansAttrs"] = json.dumps(attrs)

        if should_update:
            self.backend.modify_entry(entry.id, entry.attrs, **kwargs)

    def update_config(self):
        kwargs = {"table_name": "jansAppConf"}
        id_ = doc_id_from_dn("ou=configuration,o=jans")

        entry = self.backend.get_entry(id_, **kwargs)

        if not entry:
            return

        should_update = False

        # set jansAuthMode if still empty
        if not entry.attrs.get("jansAuthMode"):
            entry.attrs["jansAuthMode"] = "simple_password_auth"
            should_update = True

        # scim support
        scim_enabled = as_boolean(os.environ.get("CN_SCIM_ENABLED", False))
        if as_boolean(entry.attrs["jansScimEnabled"]) != scim_enabled:
            entry.attrs["jansScimEnabled"] = scim_enabled
            should_update = True

        # message configuration
        message_conf = entry.attrs.get("jansMessageConf") or {}

        with contextlib.suppress(TypeError):
            message_conf = json.loads(message_conf)

        entry.attrs["jansMessageConf"], should_update = _transform_message_config(message_conf)

        # set document store
        doc_store_type = os.environ.get("CN_DOCUMENT_STORE_TYPE", "DB")

        with contextlib.suppress(TypeError):
            entry.attrs["jansDocStoreConf"] = json.loads(entry.attrs["jansDocStoreConf"])

        if entry.attrs["jansDocStoreConf"]["documentStoreType"] != doc_store_type:
            entry.attrs["jansDocStoreConf"]["documentStoreType"] = doc_store_type
            should_update = True

        if should_update:
            entry.attrs["jansMessageConf"] = json.dumps(entry.attrs["jansMessageConf"])
            entry.attrs["jansDocStoreConf"] = json.dumps(entry.attrs["jansDocStoreConf"])
            revision = entry.attrs.get("jansRevision") or 1
            entry.attrs["jansRevision"] = revision + 1
            self.backend.modify_entry(entry.id, entry.attrs, **kwargs)


def _transform_message_config(conf):
    should_update = False
    provider_type = os.environ.get("CN_MESSAGE_TYPE", "DISABLED")

    if os.environ.get("CN_PERSISTENCE_TYPE", "sql") == "sql" and os.environ.get("CN_SQL_DB_DIALECT", "mysql") in ("pgsql", "postgresql"):
        pg_pw_encoded = encode_text(
            get_sql_password(manager),
            manager.secret.get("encoded_salt")
        ).decode()
        pg_host = os.environ.get("CN_SQL_DB_HOST", "localhost")
        pg_port = os.environ.get("CN_SQL_DB_PORT", "5432")
        pg_db = os.environ.get("CN_SQL_DB_NAME", "jans")
        pg_schema = os.environ.get("CN_SQL_DB_SCHEMA") or "public"
    else:
        pg_pw_encoded = ""
        pg_host = "localhost"
        pg_port = "5432"
        pg_db = "jans"
        pg_schema = "public"

    # backward-compat values
    pg_conf = conf.get("postgresConfiguration") or {}
    msg_wait_millis = pg_conf.get("messageWaitMillis") or pg_conf.get("message-wait-millis") or 100
    msg_sleep_thread = pg_conf.get("messageSleepThreadTime") or pg_conf.get("message-sleep-thread-millis") or 200
    new_conf = {
        "messageProviderType": provider_type,
        "postgresConfiguration": {
            "connectionUri": f"jdbc:postgresql://{pg_host}:{pg_port}/{pg_db}",
            "dbSchemaName": pg_schema,
            "authUserName": os.environ.get("CN_SQL_DB_USER", "jans"),
            "authUserPassword": pg_pw_encoded,
            "messageWaitMillis": msg_wait_millis,
            "messageSleepThreadTime": msg_sleep_thread,
        },
        "redisConfiguration": {
            "servers": os.environ.get("CN_REDIS_URL", "localhost:6379"),
        },
    }

    if new_conf != conf:
        conf = new_conf
        should_update = True
    return conf, should_update
