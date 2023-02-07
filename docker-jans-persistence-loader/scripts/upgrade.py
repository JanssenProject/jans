import contextlib
import itertools
import json
import logging.config
import os
from collections import namedtuple

from ldif import LDIFParser

from jans.pycloudlib import get_manager
from jans.pycloudlib.persistence import CouchbaseClient
from jans.pycloudlib.persistence import LdapClient
from jans.pycloudlib.persistence import SpannerClient
from jans.pycloudlib.persistence import SqlClient
from jans.pycloudlib.persistence import doc_id_from_dn
from jans.pycloudlib.persistence import id_from_dn
from jans.pycloudlib.persistence import PersistenceMapper
from jans.pycloudlib.utils import as_boolean

from settings import LOGGING_CONFIG
from utils import get_role_scope_mappings

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("entrypoint")

Entry = namedtuple("Entry", ["id", "attrs"])

manager = get_manager()


#: ID of base entry
JANS_BASE_DN = "o=jans"

#: ID of manager group
JANS_MANAGER_GROUP_DN = "inum=60B7,ou=groups,o=jans"

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


def _transform_auth_dynamic_config(conf):
    should_update = False
    distribution = os.environ.get("CN_DISTRIBUTION", "default")

    if "redirectUrisRegexEnabled" not in conf:
        # enable only if not using openbanking distro
        conf["redirectUrisRegexEnabled"] = bool(distribution != "openbanking")
        should_update = True

    # common config in all distributions
    if "accessTokenSigningAlgValuesSupported" not in conf:
        conf["accessTokenSigningAlgValuesSupported"] = [
            "none",
            "HS256",
            "HS384",
            "HS512",
            "RS256",
            "RS384",
            "RS512",
            "ES256",
            "ES384",
            "ES512",
            "ES512",
            "PS256",
            "PS384",
            "PS512"
        ]
        should_update = True

    if "forceSignedRequestObject" not in conf:
        conf["forceSignedRequestObject"] = False
        should_update = True

    if "grantTypesAndResponseTypesAutofixEnabled" not in conf:
        conf["grantTypesAndResponseTypesAutofixEnabled"] = False
        should_update = True

    if "sessionIdEnabled" in conf:
        conf.pop("sessionIdEnabled")
        should_update = True

    # assert the authorizationRequestCustomAllowedParameters contains dict values instead of string
    params_with_dict = list(itertools.takewhile(
        lambda x: isinstance(x, dict), conf["authorizationRequestCustomAllowedParameters"]
    ))
    if not params_with_dict:
        conf["authorizationRequestCustomAllowedParameters"] = [
            {"paramName": p[0], "returnInResponse": p[1]}
            for p in [
                ("customParam1", False),
                ("customParam2", False),
                ("customParam3", False),
                ("customParam4", True),
                ("customParam5", True),
            ]
        ]
        should_update = True

    if "useHighestLevelScriptIfAcrScriptNotFound" not in conf:
        conf["useHighestLevelScriptIfAcrScriptNotFound"] = True
        should_update = True

    if "httpLoggingExcludePaths" not in conf:
        conf["httpLoggingExcludePaths"] = conf.pop("httpLoggingExludePaths", [])
        should_update = True

    if "requestUriBlockList" not in conf:
        conf["requestUriBlockList"] = [
            "localhost",
            "127.0.0.1",
        ]
        should_update = True

    if "ssaConfiguration" not in conf:
        hostname = manager.config.get("hostname")
        conf["ssaConfiguration"] = {
            "ssaEndpoint": f"https://{hostname}/jans-auth/restv1/ssa",
            "ssaSigningAlg": "RS256",
            "ssaExpirationInDays": 30
        }
        should_update = True

    if "ssaCustomAttributes" not in conf["ssaConfiguration"]:
        conf["ssaConfiguration"]["ssaCustomAttributes"] = []
        should_update = True

    for grant_type in [
        "urn:ietf:params:oauth:grant-type:device_code",
        "urn:ietf:params:oauth:grant-type:token-exchange",
    ]:
        if grant_type not in conf["grantTypesSupported"]:
            conf["grantTypesSupported"].append(grant_type)
            should_update = True

    # change ox to jans
    for old_attr, new_attr in [
        ("oxElevenGenerateKeyEndpoint", "jansElevenGenerateKeyEndpoint"),
        ("oxElevenSignEndpoint", "jansElevenSignEndpoint"),
        ("oxElevenVerifySignatureEndpoint", "jansElevenVerifySignatureEndpoint"),
        ("oxElevenDeleteKeyEndpoint", "jansElevenDeleteKeyEndpoint"),
        ("oxElevenJwksEndpoint", "jansElevenJwksEndpoint"),
        ("oxOpenIdConnectVersion", "jansOpenIdConnectVersion"),
        ("oxId", "jansId"),
    ]:
        if new_attr not in conf:
            conf[new_attr] = conf.pop(old_attr, None)
            should_update = True

    if "blockWebviewAuthorizationEnabled" not in conf:
        conf["blockWebviewAuthorizationEnabled"] = False
        should_update = True

    if "dateFormatterPatterns" not in conf:
        # remove old config
        conf.pop("userInfoConfiguration", None)
        conf["dateFormatterPatterns"] = {
            "birthdate": "yyyy-MM-dd",
        }
        should_update = True

    # specific config per distribution
    if distribution == "openbanking":
        if "dcrAuthorizationWithMTLS" not in conf:
            conf["dcrAuthorizationWithMTLS"] = False
            should_update = True

        if "scopesSupported" not in conf:
            conf["scopesSupported"] = [
                "openid",
                "consents",
                "accounts",
                "resources",
            ]
            should_update = True

        if "jwt" not in conf["responseModesSupported"]:
            conf["responseModesSupported"].append("jwt")
            should_update = True

        if "private_key_jwt" not in conf["tokenEndpointAuthMethodsSupported"]:
            conf["tokenEndpointAuthMethodsSupported"].append("private_key_jwt")
            should_update = True

        # if conf["redirectUrisRegexEnabled"]:
        #     conf["redirectUrisRegexEnabled"] = False
        #     should_update = True
    else:
        if all([
            os.environ.get("CN_PERSISTENCE_TYPE") in ("sql", "spanner"),
            conf["personCustomObjectClassList"]
        ]):
            conf["personCustomObjectClassList"] = []
            should_update = True

        if "subjectIdentifiersPerClientSupported" not in conf:
            conf["subjectIdentifiersPerClientSupported"] = ["mail", "uid"]
            should_update = True

        if "agamaConfiguration" not in conf:
            conf["agamaConfiguration"] = {
                "enabled": False,
                "templatesPath": "/ftl",
                "scriptsPath": "/scripts",
                "serializerType": "KRYO",
                "maxItemsLoggedInCollections": 3,
                "pageMismatchErrorPage": "mismatch.ftl",
                "interruptionErrorPage": "timeout.ftl",
                "crashErrorPage": "crash.ftl",
                "finishedFlowPage": "finished.ftl",
                "bridgeScriptPage": "agama.xhtml",
                "defaultResponseHeaders": {
                    "Cache-Control": "max-age=0, no-store",
                },
            }
            should_update = True

        if "interruptionTime" in conf["agamaConfiguration"]:
            conf["agamaConfiguration"].pop("interruptionTime", None)
            should_update = True

        # add Cache-Control and remove Expires, Content-Type
        if "Cache-Control" not in conf["agamaConfiguration"]["defaultResponseHeaders"]:
            conf["agamaConfiguration"]["defaultResponseHeaders"]["Cache-Control"] = "max-age=0, no-store"
            conf["agamaConfiguration"]["defaultResponseHeaders"].pop("Expires", None)
            conf["agamaConfiguration"]["defaultResponseHeaders"].pop("Content-Type", None)
            should_update = True

        for grant_type in [
            "urn:ietf:params:oauth:grant-type:device_code",
            "urn:ietf:params:oauth:grant-type:token-exchange",
        ]:
            if grant_type not in conf["dynamicGrantTypeDefault"]:
                conf["dynamicGrantTypeDefault"].append(grant_type)
                should_update = True

        # ensure agama_flow listed in authorizationRequestCustomAllowedParameters
        if "agama_flow" not in [
            p["paramName"] for p in conf["authorizationRequestCustomAllowedParameters"]
        ]:
            conf["authorizationRequestCustomAllowedParameters"].append({
                "paramName": "agama_flow", "returnInResponse": False,
            })
            should_update = True

    # return the conf and flag to determine whether it needs update or not
    return conf, should_update


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
            rows[dn] = entry["jansClaimName"][0]
    return rows


class LDAPBackend:
    def __init__(self, manager):
        self.manager = manager
        self.client = LdapClient(manager)
        self.type = "ldap"

    def get_entry(self, key, filter_="", attrs=None, **kwargs):
        def format_attrs(attrs):
            _attrs = {}
            for k, v in attrs.items():
                if len(v) < 2:
                    v = v[0]
                _attrs[k] = v
            return _attrs

        filter_ = filter_ or "(objectClass=*)"

        entry = self.client.get(key, filter_=filter_, attributes=attrs)
        if not entry:
            return None
        return Entry(entry.entry_dn, format_attrs(entry.entry_attributes_as_dict))

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

    def update_misc(self):
        # 1 - fix objectclass for scim and config-api where it has lowecased objectclass instead of objectClass
        bucket = os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")

        # create the index for query
        self.client.exec_query(f'CREATE INDEX `def_jans_fix_oc` ON `{bucket}`(`objectclass`)')

        # get all scopes that has objectclass instead of objectClass
        req = self.client.exec_query(f"SELECT META().id, {bucket}.* FROM {bucket} WHERE `objectclass` IS NOT MISSING")
        if req.ok:
            resp = req.json()
            for doc in resp["results"]:
                id_ = doc.pop("id")
                doc["objectClass"] = doc["objectclass"][-1]
                self.modify_entry(id_, doc, **{"bucket": bucket})
                # remove the objectclass attribute so the query above wont return results
                self.modify_entry(id_, {"objectclass": []}, **{"bucket": bucket, "delete_attr": True})

        # drop the index
        self.client.exec_query(f'DROP INDEX `{bucket}`.`def_jans_fix_oc`')


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

        user_backend_cls = BACKEND_CLASSES[mapper.mapping["user"]]
        self.user_backend = user_backend_cls(manager)

    def invoke(self):
        logger.info("Running upgrade process (if required)")

        self.update_people_entries()
        self.update_scopes_entries()
        self.update_clients_entries()
        self.update_scim_scopes_entries()
        self.update_base_entries()

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

    def update_scripts_entries(self):
        # default to ldap persistence
        kwargs = {}
        scim_id = JANS_SCIM_SCRIPT_DN
        basic_id = JANS_BASIC_SCRIPT_DN

        if self.backend.type in ("sql", "spanner"):
            kwargs = {"table_name": "jansCustomScr"}
            scim_id = doc_id_from_dn(scim_id)
            basic_id = doc_id_from_dn(basic_id)
        elif self.backend.type == "couchbase":
            kwargs = {"bucket": os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")}
            scim_id = id_from_dn(scim_id)
            basic_id = id_from_dn(basic_id)

        # toggle scim script
        scim_entry = self.backend.get_entry(scim_id, **kwargs)
        scim_enabled = as_boolean(os.environ.get("CN_SCIM_ENABLED", False))

        if scim_entry and scim_entry.attrs["jansEnabled"] != scim_enabled:
            scim_entry.attrs["jansEnabled"] = scim_enabled
            self.backend.modify_entry(scim_entry.id, scim_entry.attrs, **kwargs)

        # always enable basic script
        basic_entry = self.backend.get_entry(basic_id, **kwargs)

        if basic_entry and not as_boolean(basic_entry.attrs["jansEnabled"]):
            basic_entry.attrs["jansEnabled"] = True
            self.backend.modify_entry(basic_entry.id, basic_entry.attrs, **kwargs)

    def update_auth_dynamic_config(self):
        # default to ldap persistence
        kwargs = {}
        id_ = JANS_AUTH_CONFIG_DN

        if self.backend.type in ("sql", "spanner"):
            kwargs = {"table_name": "jansAppConf"}
            id_ = doc_id_from_dn(id_)
        elif self.backend.type == "couchbase":
            kwargs = {"bucket": os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")}
            id_ = id_from_dn(id_)

        entry = self.backend.get_entry(id_, **kwargs)

        if not entry:
            return

        if self.backend.type != "couchbase":
            entry.attrs["jansConfDyn"] = json.loads(entry.attrs["jansConfDyn"])

        conf, should_update = _transform_auth_dynamic_config(entry.attrs["jansConfDyn"])

        if should_update:
            if self.backend.type != "couchbase":
                entry.attrs["jansConfDyn"] = json.dumps(conf)

            entry.attrs["jansRevision"] += 1
            self.backend.modify_entry(entry.id, entry.attrs, **kwargs)

    def update_attributes_entries(self):
        def _update_claim_names():
            # default to ldap persistence
            kwargs = {}
            rows = collect_claim_names()

            for id_, claim_name in rows.items():
                if self.backend.type in ("sql", "spanner"):
                    id_ = doc_id_from_dn(id_)
                    kwargs = {"table_name": "jansAttr"}
                elif self.backend.type == "couchbase":
                    id_ = id_from_dn(id_)
                    kwargs = {"bucket": os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")}

                entry = self.backend.get_entry(id_, **kwargs)

                if not entry:
                    return

                # jansClaimName already set
                if "jansClaimName" in entry.attrs and entry.attrs["jansClaimName"]:
                    continue

                entry.attrs["jansClaimName"] = claim_name
                self.backend.modify_entry(entry.id, entry.attrs, **kwargs)

        def _update_mobile_attr():
            kwargs = {}
            id_ = "inum=6DA6,ou=attributes,o=jans"

            if self.backend.type in ("sql", "spanner"):
                id_ = doc_id_from_dn(id_)
                kwargs = {"table_name": "jansAttr"}
            elif self.backend.type == "couchbase":
                id_ = id_from_dn(id_)
                kwargs = {"bucket": os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")}

            entry = self.backend.get_entry(id_, **kwargs)

            if not entry:
                return

            if not entry.attrs.get("jansMultivaluedAttr"):
                entry.attrs["jansMultivaluedAttr"] = True
                self.backend.modify_entry(entry.id, entry.attrs, **kwargs)

        _update_claim_names()
        _update_mobile_attr()

    def update_base_entries(self):
        # default to ldap persistence
        kwargs = {}
        id_ = JANS_BASE_DN

        if self.backend.type in ("sql", "spanner"):
            kwargs = {"table_name": "jansOrganization"}
            id_ = doc_id_from_dn(id_)
        elif self.backend.type == "couchbase":
            kwargs = {"bucket": os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")}
            id_ = id_from_dn(id_)

        # add jansManagerGrp to base entry
        entry = self.backend.get_entry(id_, **kwargs)

        if not entry:
            return

        if not entry.attrs.get("jansManagerGrp"):
            entry.attrs["jansManagerGrp"] = JANS_MANAGER_GROUP_DN
            self.backend.modify_entry(entry.id, entry.attrs, **kwargs)

    def update_scim_scopes_entries(self):
        # default to ldap persistence
        kwargs = {}

        # add jansAttrs to SCIM users.read and users.write scopes
        for id_ in [JANS_SCIM_USERS_READ_SCOPE_DN, JANS_SCIM_USERS_WRITE_SCOPE_DN]:
            if self.backend.type in ("sql", "spanner"):
                kwargs = {"table_name": "jansScope"}
                id_ = doc_id_from_dn(id_)
            elif self.backend.type == "couchbase":
                kwargs = {"bucket": os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")}
                id_ = id_from_dn(id_)

            entry = self.backend.get_entry(id_, **kwargs)

            if not entry:
                continue

            if "jansAttrs" not in entry.attrs:
                entry.attrs["jansAttrs"] = DEFAULT_JANS_ATTRS
                self.backend.modify_entry(entry.id, entry.attrs, **kwargs)

    def update_scopes_entries(self):
        # default to ldap persistence
        kwargs = {}
        id_ = JANS_PROFILE_SCOPE_DN

        if self.backend.type in ("sql", "spanner"):
            kwargs = {"table_name": "jansScope"}
            id_ = doc_id_from_dn(id_)
        elif self.backend.type == "couchbase":
            kwargs = {"bucket": os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")}
            id_ = id_from_dn(id_)

        entry = self.backend.get_entry(id_, **kwargs)

        if not entry:
            return

        attrs, should_update = _transform_profile_scope(entry.attrs)
        if should_update:
            self.backend.modify_entry(entry.id, attrs, **kwargs)

    def update_people_entries(self):
        # default to ldap persistence
        admin_inum = self.manager.config.get("admin_inum")

        id_ = f"inum={admin_inum},ou=people,o=jans"
        kwargs = {}

        if self.user_backend.type in ("sql", "spanner"):
            id_ = doc_id_from_dn(id_)
            kwargs = {"table_name": "jansPerson"}
        elif self.user_backend.type == "couchbase":
            id_ = id_from_dn(id_)
            bucket = os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")
            kwargs = {"bucket": f"{bucket}_user"}

        entry = self.user_backend.get_entry(id_, **kwargs)

        if not entry:
            return

        should_update = False

        # add jansAdminUIRole to default admin user
        if self.user_backend.type == "sql" and self.user_backend.client.dialect == "mysql" and not entry.attrs["jansAdminUIRole"]["v"]:
            entry.attrs["jansAdminUIRole"] = {"v": ["api-admin"]}
            should_update = True
        if self.user_backend.type == "sql" and self.user_backend.client.dialect == "pgsql" and not entry.attrs["jansAdminUIRole"]:
            entry.attrs["jansAdminUIRole"] = ["api-admin"]
            should_update = True
        elif self.user_backend.type == "spanner" and not entry.attrs["jansAdminUIRole"]:
            entry.attrs["jansAdminUIRole"] = ["api-admin"]
            should_update = True
        else:  # ldap and couchbase
            if "jansAdminUIRole" not in entry.attrs:
                entry.attrs["jansAdminUIRole"] = ["api-admin"]
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
            kwargs = {}
            token_server_admin_ui_client_id = self.manager.config.get("token_server_admin_ui_client_id")

            # admin-ui is not available
            if not token_server_admin_ui_client_id:
                return

            id_ = f"inum={token_server_admin_ui_client_id},ou=clients,o=jans"

            if self.backend.type in ("sql", "spanner"):
                kwargs = {"table_name": "jansClnt"}
                id_ = doc_id_from_dn(id_)
            elif self.backend.type == "couchbase":
                kwargs = {"bucket": os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")}
                id_ = id_from_dn(id_)

            entry = self.backend.get_entry(id_, **kwargs)

            if not entry:
                return

            attrs, should_update = _transform_token_server_client(json.loads(entry.attrs["jansAttrs"]))
            if should_update:
                entry.attrs["jansAttrs"] = json.dumps(attrs)
                self.backend.modify_entry(entry.id, entry.attrs, **kwargs)

        _update_token_server_client()

    def update_admin_ui_config(self):
        kwargs = {}
        id_ = "ou=admin-ui,ou=configuration,o=jans"

        if self.backend.type in ("sql", "spanner"):
            kwargs = {"table_name": "jansAppConf"}
            id_ = doc_id_from_dn(id_)
        elif self.backend.type == "couchbase":
            kwargs = {"bucket": os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")}
            id_ = id_from_dn(id_)

        entry = self.backend.get_entry(id_, **kwargs)

        if not entry:
            return

        # calculate new permissions for api-admin
        role_mapping = get_role_scope_mappings()
        api_admin_perms = []

        for api_role in role_mapping["rolePermissionMapping"]:
            if api_role["role"] == "api-admin":
                api_admin_perms = api_role["permissions"]
                break

        try:
            current_role_mapping = json.loads(entry.attrs["jansConfDyn"])
        except TypeError:
            current_role_mapping = entry.attrs["jansConfDyn"]

        should_update = False

        # check for rolePermissionMapping
        #
        # - compare role permissions for api-admin
        for i, api_role in enumerate(current_role_mapping["rolePermissionMapping"]):
            if api_role["role"] == "api-admin":
                # compare permissions between the ones from persistence (current) and newer permissions
                if sorted(api_role["permissions"]) != sorted(api_admin_perms):
                    current_role_mapping["rolePermissionMapping"][i]["permissions"] = api_admin_perms
                    should_update = True
                break

        # check for permissions
        #
        # - add new permission if not exist
        # - add defaultPermissionInToken (if not exist) in each permission

        # determine current permission with index/position
        current_perms = {
            permission["permission"]: {"index": i}
            for i, permission in enumerate(current_role_mapping["permissions"])
        }

        for perm in role_mapping["permissions"]:
            if perm["permission"] not in current_perms:
                # add missing permission
                current_role_mapping["permissions"].append(perm)
                should_update = True
            else:
                # add missing defaultPermissionInToken
                index = current_perms[perm["permission"]]["index"]
                if "defaultPermissionInToken" in current_role_mapping["permissions"][index]:
                    continue
                current_role_mapping["permissions"][index]["defaultPermissionInToken"] = perm["defaultPermissionInToken"]
                should_update = True

        if should_update:
            entry.attrs["jansConfDyn"] = json.dumps(current_role_mapping)
            entry.attrs["jansRevision"] += 1
            self.backend.modify_entry(entry.id, entry.attrs, **kwargs)

    def update_auth_errors_config(self):
        # default to ldap persistence
        kwargs = {}
        id_ = JANS_AUTH_CONFIG_DN

        if self.backend.type in ("sql", "spanner"):
            kwargs = {"table_name": "jansAppConf"}
            id_ = doc_id_from_dn(id_)
        elif self.backend.type == "couchbase":
            kwargs = {"bucket": os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")}
            id_ = id_from_dn(id_)

        entry = self.backend.get_entry(id_, **kwargs)

        if not entry:
            return

        if self.backend.type != "couchbase":
            entry.attrs["jansConfErrors"] = json.loads(entry.attrs["jansConfErrors"])

        conf, should_update = _transform_auth_errors_config(entry.attrs["jansConfErrors"])

        if should_update:
            if self.backend.type != "couchbase":
                entry.attrs["jansConfErrors"] = json.dumps(conf)

            entry.attrs["jansRevision"] += 1
            self.backend.modify_entry(entry.id, entry.attrs, **kwargs)

    def update_auth_static_config(self):
        # default to ldap persistence
        kwargs = {}
        id_ = JANS_AUTH_CONFIG_DN

        if self.backend.type in ("sql", "spanner"):
            kwargs = {"table_name": "jansAppConf"}
            id_ = doc_id_from_dn(id_)
        elif self.backend.type == "couchbase":
            kwargs = {"bucket": os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")}
            id_ = id_from_dn(id_)

        entry = self.backend.get_entry(id_, **kwargs)

        if not entry:
            return

        if self.backend.type != "couchbase":
            entry.attrs["jansConfStatic"] = json.loads(entry.attrs["jansConfStatic"])

        conf, should_update = _transform_auth_static_config(entry.attrs["jansConfStatic"])

        if should_update:
            if self.backend.type != "couchbase":
                entry.attrs["jansConfStatic"] = json.dumps(conf)

            entry.attrs["jansRevision"] += 1
            self.backend.modify_entry(entry.id, entry.attrs, **kwargs)

    def update_tui_client(self):
        kwargs = {}
        tui_client_id = self.manager.config.get("tui_client_id")
        id_ = f"inum={tui_client_id},ou=clients,o=jans"

        if self.backend.type in ("sql", "spanner"):
            kwargs = {"table_name": "jansClnt"}
            id_ = doc_id_from_dn(id_)
        elif self.backend.type == "couchbase":
            kwargs = {"bucket": os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")}
            id_ = id_from_dn(id_)

        entry = self.backend.get_entry(id_, **kwargs)

        if not entry:
            return

        should_update = False

        # add SSA scope inum=B9D2-D6E5,ou=scopes,o=jans to tui client
        ssa_scope = "inum=B9D2-D6E5,ou=scopes,o=jans"

        if isinstance(entry.attrs["jansScope"], dict):  # likely mysql
            if ssa_scope not in entry.attrs["jansScope"]["v"]:
                entry.attrs["jansScope"]["v"].append(ssa_scope)
                should_update = True
        else:
            if ssa_scope not in entry.attrs["jansScope"]:
                entry.attrs["jansScope"].append(ssa_scope)
                should_update = True

        if should_update:
            self.backend.modify_entry(entry.id, entry.attrs, **kwargs)


def _transform_auth_errors_config(conf):
    should_update = False

    if "ssa" not in conf:
        conf["ssa"] = [
            {
                "id": "invalid_request",
                "description": "The request is missing a required parameter, includes an invalid parameter value, includes a parameter more than once, or is otherwise malformed.",
                "uri": None,
            },
            {
                "id": "unauthorized_client",
                "description": "The Client is not authorized to use this authentication flow.",
                "uri": None,
            },
            {
                "id": "invalid_client",
                "description": "The Client is not authorized to use this authentication flow.",
                "uri": None,
            },
            {
                "id": "unknown_error",
                "description": "Unknown or not found error.",
                "uri": None,
            },
        ]
        should_update = True
    return conf, should_update


def _transform_auth_static_config(conf):
    should_update = False

    if "ssa" not in conf["baseDn"]:
        conf["baseDn"]["ssa"] = "ou=ssa,o=jans"
        should_update = True
    return conf, should_update
