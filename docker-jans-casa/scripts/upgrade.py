import json
import logging.config
import os
from collections import namedtuple
from contextlib import suppress
from urllib.parse import urlparse
from urllib.parse import urlunparse

from jans.pycloudlib import get_manager
from jans.pycloudlib.persistence.couchbase import CouchbaseClient
from jans.pycloudlib.persistence.couchbase import id_from_dn
from jans.pycloudlib.persistence.ldap import LdapClient
from jans.pycloudlib.persistence.spanner import SpannerClient
from jans.pycloudlib.persistence.sql import SqlClient
from jans.pycloudlib.persistence.sql import doc_id_from_dn
from jans.pycloudlib.persistence.utils import PersistenceMapper
from jans.pycloudlib.utils import as_boolean

from settings import LOGGING_CONFIG
from utils import get_ads_project_base64
from utils import get_ads_project_md5sum
from utils import generalized_time_utc
from utils import utcnow
from utils import CASA_AGAMA_DEPLOYMENT_ID
from utils import CASA_AGAMA_ARCHIVE

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("jans-casa")

Entry = namedtuple("Entry", ["id", "attrs"])


class LDAPBackend:
    def __init__(self, manager):
        self.manager = manager
        self.client = LdapClient(manager)
        self.type = "ldap"

    def format_attrs(self, entry, raw_values=None):
        raw_values = raw_values or []
        attrs = {}

        for attr in entry.entry_attributes:
            if attr in raw_values:
                values = entry[attr].raw_values
            else:
                values = entry[attr].values

            if len(values) < 2:
                v = values[0]
            else:
                v = values
            attrs[attr] = v
        return attrs

    def get_entry(self, key, filter_="", attrs=None, **kwargs):
        filter_ = filter_ or "(objectClass=*)"
        raw_values = kwargs.get("raw_values")

        entry = self.client.get(key, filter_=filter_, attributes=attrs)
        if not entry:
            return None
        return Entry(entry.entry_dn, self.format_attrs(entry, raw_values))

    def modify_entry(self, key, attrs=None, **kwargs):
        attrs = attrs or {}
        del_attrs = kwargs.get("delete_attrs") or []

        for k, v in attrs.items():
            if not isinstance(v, list):
                v = [v]

            if k in del_attrs:
                mod = self.client.MODIFY_DELETE
            else:
                mod = self.client.MODIFY_REPLACE
            attrs[k] = [(mod, v)]

        modified, _ = self.client.modify(key, attrs)
        return modified

    def delete_entry(self, key, **kwargs):
        return self.client.delete(key)


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

    def delete_entry(self, key, **kwargs):
        bucket = kwargs.get("bucket")
        return self.client.delete(bucket, key)


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

    def delete_entry(self, key, **kwargs):
        table_name = kwargs.get("table_name")
        return self.client.delete(table_name, key)


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
        self.update_client_scopes()
        self.update_client_uris()
        self.update_conf_app()
        self.update_agama_script()
        self.update_agama_deployment()

    def update_client_scopes(self):
        kwargs = {}
        client_id = self.manager.config.get("casa_client_id")
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
            client_scopes = entry.attrs["jansScope"]

        if not isinstance(client_scopes, list):
            client_scopes = [client_scopes]

        # all potential new scopes for client
        with open("/app/templates/jans-casa/scopes.json") as f:
            new_client_scopes = [
                f"inum={scope['inum']},ou=scopes,o=jans"
                for scope in json.loads(f.read())
            ]

        # find missing scopes from the client
        diff = list(set(new_client_scopes).difference(client_scopes))

        if diff:
            if self.backend.type == "sql" and self.backend.client.dialect == "mysql":
                entry.attrs["jansScope"]["v"] = client_scopes + diff
            else:
                entry.attrs["jansScope"] = client_scopes + diff
            self.backend.modify_entry(entry.id, entry.attrs, **kwargs)

    def update_conf_app(self):
        kwargs = {}
        id_ = "ou=casa,ou=configuration,o=jans"

        if self.backend.type in ("sql", "spanner"):
            kwargs = {"table_name": "jansAppConf"}
            id_ = doc_id_from_dn(id_)
        elif self.backend.type == "couchbase":
            kwargs = {"bucket": os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")}
            id_ = id_from_dn(id_)

        entry = self.backend.get_entry(id_, **kwargs)

        if not entry:
            return

        should_update = False

        if self.backend.type != "couchbase":
            with suppress(json.decoder.JSONDecodeError):
                entry.attrs["jansConfApp"] = json.loads(entry.attrs["jansConfApp"])

        for key in ["authz_redirect_uri", "post_logout_uri", "frontchannel_logout_uri"]:
            parsed_url = urlparse(entry.attrs["jansConfApp"]["oidc_config"][key])

            url_paths = [
                pth for pth in parsed_url.path.rsplit("/")
                if pth
            ]

            if url_paths[0] != "jans-casa":
                url_paths[0] = "jans-casa"
                parsed_url = parsed_url._replace(path="/".join(url_paths))
                entry.attrs["jansConfApp"]["oidc_config"][key] = urlunparse(parsed_url)
                should_update = True

        if should_update:
            if self.backend.type != "couchbase":
                entry.attrs["jansConfApp"] = json.dumps(entry.attrs["jansConfApp"])
            self.backend.modify_entry(entry.id, entry.attrs, **kwargs)

    def update_client_uris(self):
        kwargs = {}
        client_id = self.manager.config.get("casa_client_id")
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

        should_update = False
        hostname = self.manager.config.get("hostname")
        uri_mapping = {
            "jansLogoutURI": f"https://{hostname}/jans-casa/autologout",
            "jansPostLogoutRedirectURI": f"https://{hostname}/jans-casa/bye.zul",
            "jansRedirectURI": f"https://{hostname}/jans-casa",
        }

        for key, uri in uri_mapping.items():
            if self.backend.type == "sql" and self.backend.client.dialect == "mysql":
                client_uris = entry.attrs[key]["v"]
            else:
                client_uris = entry.attrs[key]

            if not isinstance(client_uris, list):
                client_uris = [client_uris]

            if uri not in client_uris:
                client_uris.append(uri)

                if self.backend.type == "sql" and self.backend.client.dialect == "mysql":
                    entry.attrs[key]["v"] = client_uris
                else:
                    entry.attrs[key] = client_uris
                should_update = True

        if should_update:
            self.backend.modify_entry(entry.id, entry.attrs, **kwargs)

    def update_agama_script(self):
        kwargs = {}
        agama_id = "inum=BADA-BADA,ou=scripts,o=jans"

        if self.backend.type in ("sql", "spanner"):
            kwargs = {"table_name": "jansCustomScr"}
            agama_id = doc_id_from_dn(agama_id)
        elif self.backend.type == "couchbase":
            kwargs = {"bucket": os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")}
            agama_id = id_from_dn(agama_id)

        # enable agama script
        entry = self.backend.get_entry(agama_id, **kwargs)

        if entry and as_boolean(entry.attrs["jansEnabled"]) is False:
            entry.attrs["jansEnabled"] = True
            entry.attrs["jansRevision"] += 1
            self.backend.modify_entry(entry.id, entry.attrs, **kwargs)

    def update_agama_deployment(self):
        casa_agama_deployment_id = CASA_AGAMA_DEPLOYMENT_ID
        deploy_id = f"jansId={casa_agama_deployment_id},ou=deployments,ou=agama,o=jans"

        if self.backend.type in ("sql", "spanner"):
            kwargs = {"table_name": "adsPrjDeployment"}
            deploy_id = doc_id_from_dn(deploy_id)
        elif self.backend.type == "couchbase":
            kwargs = {"bucket": os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")}
            deploy_id = id_from_dn(deploy_id)
        else:
            # for ldap, get the raw value of the following attribute so we get a precise value
            kwargs = {"raw_values": ["jansEndDate"]}

        entry = self.backend.get_entry(deploy_id, **kwargs)
        proj_archive = CASA_AGAMA_ARCHIVE
        assets_md5 = get_ads_project_md5sum(proj_archive)

        # marker to determine whether we need to update persistence if asset is changed
        if entry and assets_md5 != self.manager.config.get("casa_agama_md5sum"):
            logger.info(f"Detected changes of casa-agama-project assets; synchronizing changes from {proj_archive} to persistence.")

            entry.attrs["adsPrjDeplDetails"] = json.dumps({"projectMetadata": {"projectName": "casa"}})
            entry.attrs["adsPrjAssets"] = get_ads_project_base64(proj_archive)
            entry.attrs["jansActive"] = False
            start_date = utcnow()

            if self.backend.type in ("sql", "spanner"):
                entry.attrs["jansStartDate"] = start_date
                entry.attrs["jansEndDate"] = None
            elif self.backend.type == "couchbase":
                entry.attrs["jansStartDate"] = start_date.strftime("%Y-%m-%dT%H:%M:%SZ")
                entry.attrs["jansEndDate"] = ""
                entry.attrs["adsPrjDeplDetails"] = {"projectMetadata": {"projectName": "casa"}}
                ...
            else:  # ldap
                # remove jansEndDate
                kwargs["delete_attrs"] = ["jansEndDate"]
                entry.attrs["jansStartDate"] = generalized_time_utc(start_date)

            if self.backend.modify_entry(entry.id, entry.attrs, **kwargs):
                self.manager.config.set("casa_agama_md5sum", assets_md5)


def main():
    manager = get_manager()

    with manager.lock.create_lock("casa-upgrade"):
        upgrade = Upgrade(manager)
        upgrade.invoke()


if __name__ == "__main__":
    main()
