import json
import os

from jans.pycloudlib.persistence.couchbase import CouchbaseClient
from jans.pycloudlib.persistence.ldap import LdapClient
from jans.pycloudlib.persistence.sql import SqlClient
from jans.pycloudlib.persistence.spanner import SpannerClient
from jans.pycloudlib.persistence.utils import PersistenceMapper


class BasePersistence:
    def get_auth_config(self):
        raise NotImplementedError

    def modify_auth_config(self, id_, rev, conf_dynamic):
        raise NotImplementedError


class LdapPersistence(BasePersistence):
    def __init__(self, manager):
        self.client = LdapClient(manager)

    def get_auth_config(self):
        entry = self.client.get(
            "ou=jans-auth,ou=configuration,o=jans",
            attributes=["jansRevision", "jansConfDyn"],
        )

        if not entry:
            return {}

        config = {
            "id": entry.entry_dn,
            "jansRevision": entry["jansRevision"][0],
            "jansConfDyn": entry["jansConfDyn"][0],
        }
        return config

    def modify_auth_config(self, id_, rev, conf_dynamic):
        modified, _ = self.client.modify(
            id_,
            {
                'jansRevision': [(self.client.MODIFY_REPLACE, [str(rev)])],
                'jansConfDyn': [(self.client.MODIFY_REPLACE, [json.dumps(conf_dynamic)])],
            }
        )
        return modified


class CouchbasePersistence(BasePersistence):
    def __init__(self, manager):
        self.client = CouchbaseClient(manager)

    def get_auth_config(self):
        bucket = os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")
        req = self.client.exec_query(
            "SELECT jansRevision, jansConfDyn "
            f"FROM `{bucket}` "
            "USE KEYS 'configuration_jans-auth'",
        )
        if not req.ok:
            return {}

        config = req.json()["results"][0]

        if not config:
            return {}

        config.update({"id": "configuration_jans-auth"})
        return config

    def modify_auth_config(self, id_, rev, conf_dynamic):
        conf_dynamic = json.dumps(conf_dynamic)
        bucket = os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")

        req = self.client.exec_query(
            f"UPDATE `{bucket}` USE KEYS '{id_}' "
            f"SET jansRevision={rev}, jansConfDyn={conf_dynamic}, "
            "RETURNING jansRevision"
        )

        if not req.ok:
            return False
        return True


class SqlPersistence(BasePersistence):
    def __init__(self, manager):
        self.client = SqlClient(manager)

    def get_auth_config(self):
        config = self.client.get(
            "jansAppConf",
            "jans-auth",
            ["jansRevision", "jansConfDyn"],
        )
        if not config:
            return {}

        config["id"] = "jans-auth"
        return config

    def modify_auth_config(self, id_, rev, conf_dynamic):
        modified = self.client.update(
            "jansAppConf",
            id_,
            {"jansRevision": rev, "jansConfDyn": json.dumps(conf_dynamic)}
        )
        return modified


class SpannerPersistence(SqlPersistence):
    def __init__(self, manager):
        self.client = SpannerClient(manager)


_backend_classes = {
    "ldap": LdapPersistence,
    "couchbase": CouchbasePersistence,
    "sql": SqlPersistence,
    "spanner": SpannerPersistence,
}


def modify_keystore_path(manager, path, jwks_uri):
    mapper = PersistenceMapper()
    backend_type = mapper.mapping["default"]

    # resolve backend
    backend = _backend_classes[backend_type](manager)

    config = backend.get_auth_config()
    if not config:
        # search failed due to missing entry
        return

    try:
        conf_dynamic = json.loads(config["jansConfDyn"])
    except TypeError:  # not string/buffer
        conf_dynamic = config["jansConfDyn"]

    # no changes, skip the process
    if path == conf_dynamic["keyStoreFile"]:
        return

    conf_dynamic.update({
        "keyStoreFile": path,
        "jwksUri": jwks_uri,
    })

    rev = int(config["jansRevision"]) + 1

    backend.modify_auth_config(
        config["id"],
        rev,
        conf_dynamic,
    )
