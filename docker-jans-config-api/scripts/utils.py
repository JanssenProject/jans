import json
import os
from urllib.parse import urlparse

from jans.pycloudlib import get_manager
from jans.pycloudlib.persistence.couchbase import get_couchbase_user
from jans.pycloudlib.persistence.couchbase import get_couchbase_password
from jans.pycloudlib.persistence.couchbase import CouchbaseClient
from jans.pycloudlib.persistence.sql import SQLClient
from jans.pycloudlib.persistence.ldap import LdapClient
from jans.pycloudlib.persistence.spanner import SpannerClient


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
        host = os.environ.get("CN_COUCHBASE_URL", "localhost")
        user = get_couchbase_user(manager)
        password = get_couchbase_password(manager)
        self.client = CouchbaseClient(host, user, password)

    def get_auth_config(self):
        bucket = os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")
        req = self.client.exec_query(
            f"SELECT jansConfDyn FROM `{bucket}` USE KEYS 'configuration_jans-auth'"
        )
        if not req.ok:
            return {}

        config = req.json()["results"][0]
        if not config:
            return {}
        return config["jansConfDyn"]


class SqlPersistence:
    def __init__(self, manager):
        self.client = SQLClient()

    def get_auth_config(self):
        config = self.client.get(
            "jansAppConf",
            "jans-auth",
            ["jansConfDyn"],
        )
        return config.get("jansConfDyn", "")


class SpannerPersistence(SqlPersistence):
    def __init__(self, manager):
        self.client = SpannerClient()


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

    persistence_type = os.environ.get("CN_PERSISTENCE_TYPE", "ldap")
    ldap_mapping = os.environ.get("CN_PERSISTENCE_LDAP_MAPPING", "default")

    if persistence_type in ("ldap", "couchbase", "sql", "spanner"):
        backend_type = persistence_type
    else:
        # maybe hybrid
        if ldap_mapping == "default":
            backend_type = "ldap"
        else:
            backend_type = "couchbase"

    # resolve backend
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
