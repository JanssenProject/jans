import json
import os
from urllib.parse import urlparse

from jans.pycloudlib import get_manager
from jans.pycloudlib.persistence.couchbase import CouchbaseClient
from jans.pycloudlib.persistence.sql import SqlClient
from jans.pycloudlib.persistence.ldap import LdapClient
from jans.pycloudlib.persistence.spanner import SpannerClient
from jans.pycloudlib.persistence.utils import PersistenceMapper


class LdapPersistence:
    def __init__(self, manager):
        self.client = LdapClient(manager)

    def get_auth_config(self):
        # base DN for auth config
        dn = "ou=jans-auth,ou=configuration,o=jans"
        entry = self.client.get(dn)

        if not entry:
            return {}
        return entry["jansConfDyn"][0]


class CouchbasePersistence:
    def __init__(self, manager):
        self.client = CouchbaseClient(manager)

    def get_auth_config(self):
        bucket = os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")
        req = self.client.exec_query(
            f"SELECT jansConfDyn FROM `{bucket}` USE KEYS 'configuration_jans-auth'"  # nosec: B608
        )
        if not req.ok:
            return {}

        config = req.json()["results"][0]
        if not config:
            return {}
        return config["jansConfDyn"]


class SqlPersistence:
    def __init__(self, manager):
        self.client = SqlClient(manager)

    def get_auth_config(self):
        config = self.client.get(
            "jansAppConf",
            "jans-auth",
            ["jansConfDyn"],
        )
        return config.get("jansConfDyn", "")


class SpannerPersistence(SqlPersistence):
    def __init__(self, manager):
        self.client = SpannerClient(manager)


def transform_url(url):
    auth_server_url = os.environ.get("CN_AUTH_SERVER_URL", "")

    if not auth_server_url:
        return url

    parse_result = urlparse(url)
    if parse_result.path.startswith("/.well-known"):
        path = f"/jans-auth{parse_result.path}"
    else:
        path = parse_result.path
    url = f"http://{auth_server_url}{path}"
    return url


_backend_classes = {
    "ldap": LdapPersistence,
    "couchbase": CouchbasePersistence,
    "sql": SqlPersistence,
    "spanner": SpannerPersistence,
}


def get_injected_urls():
    manager = get_manager()

    # resolve backend
    mapping = PersistenceMapper().mapping
    backend_type = mapping["default"]
    backend = _backend_classes[backend_type](manager)

    auth_config = backend.get_auth_config()
    try:
        auth_config = json.loads(auth_config)
    except TypeError:
        pass

    endpoints = [
        "issuer",
        "openIdConfigurationEndpoint",
        "introspectionEndpoint",
        "tokenEndpoint",
        "tokenRevocationEndpoint",
    ]
    transformed_urls = {
        attr: transform_url(auth_config[attr])
        for attr in endpoints
    }
    return transformed_urls
