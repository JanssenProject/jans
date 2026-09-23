import json
from datetime import timedelta

from jans.pycloudlib.persistence.sql import SqlClient
from jans.pycloudlib.utils import utcnow


class SqlPersistence:
    def __init__(self, manager):
        self.client = SqlClient(manager)

    def get_config(self, doc_id):
        config = self.client.get(
            "jansAppConf",
            doc_id,
            ["jansRevision", "jansConfDyn", "jansConfWebKeys", "jansConfApp"],
        )
        if not config:
            return {}

        config["id"] = doc_id
        return config

    def modify_config(self, doc_id, rev, conf_dynamic=None, conf_webkeys=None, conf_app=None):
        conf_dynamic = conf_dynamic or {}
        conf_webkeys = conf_webkeys or {}
        conf_app = conf_app or {}
        mod_attrs = {
            "jansRevision": rev,
            "jansConfDyn": json.dumps(conf_dynamic or {}),
            "jansConfWebKeys": json.dumps(conf_webkeys or {}),
            "jansConfApp": json.dumps(conf_app or {}),
        }

        return self.client.update("jansAppConf", doc_id, mod_attrs)

    def create_archived_jwk(self, jwk, lifetime):
        table_name = "jansArchJwk"
        utc_now = utcnow()
        column_mapping = {
            "doc_id": jwk["kid"],
            "objectClass": table_name,
            "dn": f"jansId={jwk['kid']},ou=archived_jwks,o=jans",
            "jansId": jwk["kid"],
            "creationDate": utc_now,
            "exp": utc_now + timedelta(seconds=lifetime),
            "del": True,
            "jansData": json.dumps(jwk),
            "attr": json.dumps({"attributes": {}}),
        }
        self.client.insert_into(table_name, column_mapping)
