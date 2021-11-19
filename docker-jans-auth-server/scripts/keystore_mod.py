import json
import os

from jans.pycloudlib.persistence.couchbase import CouchbaseClient
from jans.pycloudlib.persistence.couchbase import get_couchbase_user
from jans.pycloudlib.persistence.couchbase import get_couchbase_password
from jans.pycloudlib.persistence.ldap import LdapClient
from jans.pycloudlib.persistence.sql import SQLClient
from jans.pycloudlib.persistence.spanner import SpannerClient


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
        host = os.environ.get("CN_COUCHBASE_URL", "localhost")
        user = get_couchbase_user(manager)
        password = get_couchbase_password(manager)
        self.client = CouchbaseClient(host, user, password)

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
        self.client = SQLClient()

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
        self.client = SpannerClient()


_backend_classes = {
    "ldap": LdapPersistence,
    "couchbase": CouchbasePersistence,
    "sql": SqlPersistence,
    "spanner": SpannerPersistence,
}


def modify_keystore_path(manager, path, jwks_uri):
    persistence_type = os.environ.get("CN_PERSISTENCE_TYPE", "ldap")
    ldap_mapping = os.environ.get("CN_PERSISTENCE_LDAP_MAPPING", "default")

    if persistence_type in ("ldap", "couchbase", "sql", "spanner"):
        backend_type = persistence_type
    else:
        # persistence_type is hybrid
        if ldap_mapping == "default":
            backend_type = "ldap"
        else:
            backend_type = "couchbase"

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
