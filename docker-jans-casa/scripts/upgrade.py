import json
import logging.config
import os
from collections import namedtuple
from contextlib import suppress
from urllib.parse import urlparse
from urllib.parse import urlunparse

from jans.pycloudlib import get_manager
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

    def invoke(self):
        logger.info("Running upgrade process (if required)")
        self.update_client_scopes()
        self.update_client_uris()
        self.update_conf_app()
        self.update_agama_script()
        self.update_agama_deployment()

    def update_client_scopes(self):
        kwargs = {"table_name": "jansClnt"}
        client_id = self.manager.config.get("casa_client_id")
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
        with open("/app/templates/jans-casa/scopes.json") as f:
            new_client_scopes = [
                f"inum={scope['inum']},ou=scopes,o=jans"
                for scope in json.loads(f.read())
            ]

        # find missing scopes from the client
        if diff := list(set(new_client_scopes).difference(client_scopes)):
            if not self.backend.client.use_simple_json:
                entry.attrs["jansScope"]["v"] = client_scopes + diff
            else:
                entry.attrs["jansScope"] = client_scopes + diff
            self.backend.modify_entry(entry.id, entry.attrs, **kwargs)

    def update_conf_app(self):
        kwargs = {"table_name": "jansAppConf"}
        id_ = doc_id_from_dn("ou=casa,ou=configuration,o=jans")

        entry = self.backend.get_entry(id_, **kwargs)

        if not entry:
            return

        should_update = False

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
            entry.attrs["jansConfApp"] = json.dumps(entry.attrs["jansConfApp"])
            self.backend.modify_entry(entry.id, entry.attrs, **kwargs)

    def update_client_uris(self):
        kwargs = {"table_name": "jansClnt"}
        client_id = self.manager.config.get("casa_client_id")
        id_ = doc_id_from_dn(f"inum={client_id},ou=clients,o=jans")

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
            if not self.backend.client.use_simple_json:
                client_uris = entry.attrs[key]["v"]
            else:
                client_uris = entry.attrs[key]

            if not isinstance(client_uris, list):
                client_uris = [client_uris]

            if uri not in client_uris:
                client_uris.append(uri)

                if not self.backend.client.use_simple_json:
                    entry.attrs[key]["v"] = client_uris
                else:
                    entry.attrs[key] = client_uris
                should_update = True

        if should_update:
            self.backend.modify_entry(entry.id, entry.attrs, **kwargs)

    def update_agama_script(self):
        kwargs = {"table_name": "jansCustomScr"}
        agama_id = doc_id_from_dn("inum=BADA-BADA,ou=scripts,o=jans")

        # enable agama script
        entry = self.backend.get_entry(agama_id, **kwargs)

        if entry and as_boolean(entry.attrs["jansEnabled"]) is False:
            entry.attrs["jansEnabled"] = True
            entry.attrs["jansRevision"] += 1
            self.backend.modify_entry(entry.id, entry.attrs, **kwargs)

    def update_agama_deployment(self):
        casa_agama_deployment_id = CASA_AGAMA_DEPLOYMENT_ID
        deploy_id = doc_id_from_dn(f"jansId={casa_agama_deployment_id},ou=deployments,ou=agama,o=jans")
        kwargs = {"table_name": "adsPrjDeployment"}

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

            entry.attrs["jansStartDate"] = start_date
            entry.attrs["jansEndDate"] = None

            if self.backend.modify_entry(entry.id, entry.attrs, **kwargs):
                self.manager.config.set("casa_agama_md5sum", assets_md5)


def main():
    manager = get_manager()

    with manager.create_lock("casa-upgrade"):
        upgrade = Upgrade(manager)
        upgrade.invoke()


if __name__ == "__main__":
    main()
