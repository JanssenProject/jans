import json
import os
from urllib.parse import urlparse

from ldap3 import Connection
from ldap3 import Server
from ldap3 import BASE

from jans.pycloudlib import get_manager
from jans.pycloudlib.utils import decode_text
from jans.pycloudlib.persistence.couchbase import get_couchbase_user
from jans.pycloudlib.persistence.couchbase import get_couchbase_password
from jans.pycloudlib.persistence.couchbase import CouchbaseClient


class LdapPersistence:
    def __init__(self, host, user, password):
        ldap_server = Server(host, port=1636, use_ssl=True)
        self.backend = Connection(ldap_server, user, password)

    def get_auth_config(self):
        # base DN for auth config
        auth_base = ",".join([
            "ou=jans-auth",
            "ou=configuration",
            "o=jans",
        ])

        with self.backend as conn:
            conn.search(
                search_base=auth_base,
                search_filter="(objectClass=*)",
                search_scope=BASE,
                attributes=[
                    "jansConfDyn",
                ]
            )

            if not conn.entries:
                return {}

            entry = conn.entries[0]
            config = entry["jansConfDyn"][0]
            return config


class CouchbasePersistence:
    def __init__(self, host, user, password):
        self.backend = CouchbaseClient(host, user, password)

    def get_auth_config(self):
        bucket = os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")
        req = self.backend.exec_query(
            f"SELECT jansConfDyn FROM `{bucket}` USE KEYS 'configuration_jans-auth'"
        )
        if not req.ok:
            return {}

        config = req.json()["results"][0]
        if not config:
            return {}
        return config["jansConfDyn"]


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


def get_injected_urls():
    manager = get_manager()

    persistence_type = os.environ.get("CN_PERSISTENCE_TYPE", "ldap")
    ldap_mapping = os.environ.get("CN_PERSISTENCE_LDAP_MAPPING", "default")

    if persistence_type in ("ldap", "couchbase"):
        backend_type = persistence_type
    else:
        # maybe hybrid
        if ldap_mapping == "default":
            backend_type = "ldap"
        else:
            backend_type = "couchbase"

    # resolve backend
    if backend_type == "ldap":
        host = os.environ.get("CN_LDAP_URL", "localhost:1636")
        user = manager.config.get("ldap_binddn")
        password = decode_text(
            manager.secret.get("encoded_ox_ldap_pw"),
            manager.secret.get("encoded_salt"),
        )
        backend_cls = LdapPersistence
    else:
        host = os.environ.get("CN_COUCHBASE_URL", "localhost")
        user = get_couchbase_user(manager)
        password = get_couchbase_password(manager)
        backend_cls = CouchbasePersistence

    backend = backend_cls(host, user, password)

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
