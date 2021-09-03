"""
jans.pycloudlib.persistence.couchbase
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains various helpers related to Couchbase persistence.
"""

import json
import logging
import os
from functools import partial
from typing import NoReturn

import requests
from requests_toolbelt.adapters.host_header_ssl import HostHeaderSSLAdapter

from jans.pycloudlib.utils import encode_text
from jans.pycloudlib.utils import cert_to_truststore
from jans.pycloudlib.utils import as_boolean

CN_COUCHBASE_TRUSTSTORE_PASSWORD = "newsecret"

logger = logging.getLogger(__name__)


def get_couchbase_user(manager=None) -> str:
    """Get Couchbase username from ``CN_COUCHBASE_USER``
    environment variable (default to ``admin``).

    :params manager: A no-op argument, preserved for backward compatibility.
    :returns: Couchbase username.
    """
    return os.environ.get("CN_COUCHBASE_USER", "admin")


def get_couchbase_password(manager, plaintext: bool = True) -> str:
    """Get Couchbase user's password from file
    (default to ``/etc/jans/conf/couchbase_password``).

    To change the location, simply pass ``CN_COUCHBASE_PASSWORD_FILE`` environment variable.

    :params manager: An instance of :class:`~jans.pycloudlib.manager._Manager`.
    :params plaintext: Whether to return plaintext or encoded password.
    :returns: Plaintext or encoded password.
    """
    password_file = os.environ.get(
        "CN_COUCHBASE_PASSWORD_FILE", "/etc/jans/conf/couchbase_password"
    )

    with open(password_file) as f:
        password = f.read().strip()
        if not plaintext:
            password = encode_text(password, manager.secret.get("encoded_salt")).decode()
        return password


#: Get Couchbase user's encoded password from file.
#:
#: This is a shortcut of :func:`get_couchbase_password` with ``plaintext``
#: argument set as ``False``.
get_encoded_couchbase_password = partial(get_couchbase_password, plaintext=False)


def get_couchbase_superuser(manager=None) -> str:
    """Get Couchbase username from ``CN_COUCHBASE_SUPERUSER``
    environment variable (default to empty-string).

    :params manager: A no-op argument, preserved for backward compatibility.
    :returns: Couchbase username.
    """
    return os.environ.get("CN_COUCHBASE_SUPERUSER", "")


def get_couchbase_superuser_password(manager, plaintext: bool = True) -> str:
    """Get Couchbase superuser's password from file (default to
    ``/etc/jans/conf/couchbase_superuser_password``).

    To change the location, simply pass ``CN_COUCHBASE_SUPERUSER_PASSWORD_FILE`` environment variable.

    :params manager: An instance of :class:`~jans.pycloudlib.manager._Manager`.
    :params plaintext: Whether to return plaintext or encoded password.
    :returns: Plaintext or encoded password.
    """
    password_file = os.environ.get(
        "CN_COUCHBASE_SUPERUSER_PASSWORD_FILE", "/etc/jans/conf/couchbase_superuser_password"
    )

    with open(password_file) as f:
        password = f.read().strip()
        if not plaintext:
            password = encode_text(password, manager.secret.get("encoded_salt")).decode()
        return password


#: Get Couchbase superuser's encoded password from file.
#:
#: This is a shortcut of :func:`get_couchbase_superuser_password` with ``plaintext``
#: argument set as ``False``.
get_encoded_couchbase_superuser_password = partial(get_couchbase_superuser_password, plaintext=False)


def prefixed_couchbase_mappings():
    prefix = os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")
    mappings = {
        "default": {"bucket": prefix, "mapping": ""},
        "user": {"bucket": f"{prefix}_user", "mapping": "people, groups, authorizations"},
        "cache": {"bucket": f"{prefix}_cache", "mapping": "cache"},
        "site": {"bucket": f"{prefix}_site", "mapping": "cache-refresh"},
        "token": {"bucket": f"{prefix}_token", "mapping": "tokens"},
        "session": {"bucket": f"{prefix}_session", "mapping": "sessions"},
    }
    return mappings


def get_couchbase_mappings(persistence_type: str, ldap_mapping: str) -> dict:
    """Get mappings of Couchbase buckets.

    Supported persistence types:

    - ``ldap``
    - ``couchbase``
    - ``hybrid``

    Supported LDAP mappings:

    - ``default``
    - ``user``
    - ``token``
    - ``site``
    - ``cache``
    - ``session``

    :params persistence_type: Type of persistence.
    :params ldap_mapping: Mapping that stored in LDAP persistence.
    :returns: A map of Couchbase buckets.
    """
    mappings = prefixed_couchbase_mappings()

    if persistence_type == "hybrid":
        return {
            name: mapping
            for name, mapping in mappings.items()
            if name != ldap_mapping
        }
    return mappings


def get_couchbase_conn_timeout() -> int:
    """Get connection timeout to Couchbase server.

    Default connection timeout is 10000  milliseconds. To change the value, pass
    `CN_COUCHBASE_CONN_TIMEOUT` environment variable.

    :returns: Connection timeout (in milliseconds).
    """
    default = 10000

    try:
        val = int(os.environ.get("CN_COUCHBASE_CONN_TIMEOUT", default))
    except ValueError:
        val = default
    return val


def get_couchbase_conn_max_wait() -> int:
    """Get connection maximum wait time to Couchbase server.

    Default time is 20000  milliseconds. To change the value, pass
    `CN_COUCHBASE_CONN_MAX_WAIT` environment variable.

    :returns: Connection wait time (in milliseconds).
    """
    default = 20000

    try:
        val = int(os.environ.get("CN_COUCHBASE_CONN_MAX_WAIT", default))
    except ValueError:
        val = default
    return val


def get_couchbase_scan_consistency() -> str:
    """Get scan consistency of Couchbase connection.

    Supported types:

    - ``not_bounded`` (default)
    - ``request_plus``
    - ``statement_plus``

    :returns: Scan consistency type.
    """
    opts = ("not_bounded", "request_plus", "statement_plus")
    default = "not_bounded"
    opt = os.environ.get("CN_COUCHBASE_SCAN_CONSISTENCY", default)
    if opt not in opts:
        opt = default
    return opt


def render_couchbase_properties(manager, src: str, dest: str) -> None:
    """Render file contains properties to connect to Couchbase server,
    i.e. ``/etc/jans/conf/jans-couchbase.properties``.

    :params manager: An instance of :class:`~jans.pycloudlib.manager._Manager`.
    :params src: Absolute path to the template.
    :params dest: Absolute path where generated file is located.
    """
    persistence_type = os.environ.get("CN_PERSISTENCE_TYPE", "couchbase")
    ldap_mapping = os.environ.get("CN_PERSISTENCE_LDAP_MAPPING", "default")
    hostname = os.environ.get("CN_COUCHBASE_URL", "localhost")
    bucket_prefix = os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")

    _couchbase_mappings = get_couchbase_mappings(persistence_type, ldap_mapping)
    couchbase_buckets = []
    couchbase_mappings = []

    for _, mapping in _couchbase_mappings.items():
        couchbase_buckets.append(mapping["bucket"])

        if not mapping["mapping"]:
            continue

        couchbase_mappings.append(
            f"bucket.{mapping['bucket']}.mapping: {mapping['mapping']}"
        )

    # always have  _default_ bucket
    if bucket_prefix not in couchbase_buckets:
        couchbase_buckets.insert(0, bucket_prefix)

    with open(src) as fr:
        txt = fr.read()

        with open(dest, "w") as fw:
            rendered_txt = txt % {
                "hostname": hostname,
                "couchbase_server_user": get_couchbase_user(manager),
                "encoded_couchbase_server_pw": get_encoded_couchbase_password(manager),
                "couchbase_buckets": ", ".join(couchbase_buckets),
                "default_bucket": bucket_prefix,
                "couchbase_mappings": "\n".join(couchbase_mappings),
                "encryption_method": "SSHA-256",
                "ssl_enabled": str(as_boolean(
                    os.environ.get("CN_COUCHBASE_TRUSTSTORE_ENABLE", True)
                )).lower(),
                "couchbaseTrustStoreFn": manager.config.get("couchbaseTrustStoreFn") or "/etc/certs/couchbase.pkcs12",
                "encoded_couchbaseTrustStorePass": encode_text(
                    CN_COUCHBASE_TRUSTSTORE_PASSWORD,
                    manager.secret.get("encoded_salt"),
                ).decode(),
                "couchbase_conn_timeout": get_couchbase_conn_timeout(),
                "couchbase_conn_max_wait": get_couchbase_conn_max_wait(),
                "couchbase_scan_consistency": get_couchbase_scan_consistency(),
                "couchbase_keepalive_interval": get_couchbase_keepalive_interval(),
                "couchbase_keepalive_timeout": get_couchbase_keepalive_timeout(),
            }
            fw.write(rendered_txt)


# DEPRECATED
def sync_couchbase_cert(manager=None) -> str:
    cert_file = os.environ.get("CN_COUCHBASE_CERT_FILE", "/etc/certs/couchbase.crt")
    with open(cert_file) as f:
        return f.read()


def sync_couchbase_truststore(manager, dest: str = "/etc/certs/couchbase.pkcs12") -> None:
    """Pull secret contains base64-string contents of Couchbase truststore,
    and save it as a JKS file, i.e. ``/etc/certs/couchbase.pkcs12``.

    :params manager: An instance of :class:`~jans.pycloudlib.manager._Manager`.
    :params dest: Absolute path where generated file is located.
    """
    cert_file = os.environ.get("CN_COUCHBASE_CERT_FILE", "/etc/certs/couchbase.crt")
    dest = dest or manager.config.get("couchbaseTrustStoreFn")
    cert_to_truststore(
        "couchbase", cert_file, dest, CN_COUCHBASE_TRUSTSTORE_PASSWORD,
    )


class BaseClient:
    """A base class for API client.
    """

    def __init__(self, hosts, user, password):
        self._hosts = hosts
        self.host = ""
        self.user = user
        self.password = password
        self._session = None

    @property
    def scheme(self):
        """Scheme used when connecting to Couchbase server.
        """
        if as_boolean(os.environ.get("CN_COUCHBASE_TRUSTSTORE_ENABLE", True)):
            return "https"
        return "http"

    @property
    def session(self):
        """Get an instance of ``requests.Session``.

        By default, the session will not use certificate verification.

        To enable certificate verification:

        - set ``CN_COUCHBASE_VERIFY`` environment variable to ``true`` (default to ``false``)
        - ensure ``CN_COUCHBASE_CERT_FILE`` pointed to valid Couchbase cluster
          certificate (default to ``/etc/certs/couchbase.crt``)
        - optionally, set ``CN_COUCHBASE_HOST_HEADER`` to match Common Name
          or any of SubjectAltName defined in certificate (default to ``localhost``)
        """
        suppress_verification_warning()

        if not self._session:
            self._session = requests.Session()
            self._session.verify = False

            verify = as_boolean(os.environ.get("CN_COUCHBASE_VERIFY", False))
            if verify:
                self._session.mount("https://", HostHeaderSSLAdapter())
                self._session.verify = os.environ.get("CN_COUCHBASE_CERT_FILE") or "/etc/certs/couchbase.crt"
                self._session.headers["Host"] = os.environ.get("CN_COUCHBASE_HOST_HEADER") or "localhost"
        return self._session

    def resolve_host(self) -> str:
        """Get active/ready host from a list of servers.

        :returns: Hostname or IP address.
        """
        hosts = filter(None, map(lambda host: host.strip(), self._hosts.split(",")))

        for _host in hosts:
            try:
                resp = self.healthcheck(_host)
                if resp.ok:
                    self.host = _host
                    break
                logger.warning(f"Unable to connect to {_host}:{self.port}; reason={resp.reason}")
            except Exception as exc:  # noqa: B902
                logger.warning(f"Unable to connect to {_host}:{self.port}; reason={exc}")
        return self.host

    def healthcheck(self, host) -> NoReturn:
        """Run healthcheck to a host.

        Subclass **MUST** implement this method.
        """
        raise NotImplementedError

    def exec_api(self, path, **kwargs) -> NoReturn:
        """Execute a request to an API server.

        Subclass **MUST** implement this method.
        """
        raise NotImplementedError


class N1qlClient(BaseClient):
    """This class interacts with N1QL server (part of Couchbase).
    """

    @property
    def port(self):
        """Port where N1QL server is bind to.
        """
        if as_boolean(os.environ.get("CN_COUCHBASE_TRUSTSTORE_ENABLE", True)):
            return 18093
        return 8093

    def healthcheck(self, host):
        """Run healthcheck to a host.

        :params host: Hostname or IP address.
        :returns: An instance of ``requests.models.Response``.
        """
        return self.session.post(
            f"{self.scheme}://{host}:{self.port}/query/service",
            data={"statement": "SELECT status FROM system:indexes LIMIT 1"},
            auth=(self.user, self.password),
            timeout=10,
        )

    def exec_api(self, path, **kwargs):
        """Execute a request to REST server.

        :params path: Path (or sub-URL) of API server.
        :params kwargs: Keyword-argument passed to ``requests.api.*`` function.
        :returns: An instance of ``requests.models.Response``.
        """
        data = kwargs.get("data", {})

        resp = self.session.post(
            f"{self.scheme}://{self.host}:{self.port}/{path}",
            data=data,
            auth=(self.user, self.password),
        )
        return resp


def build_n1ql_request_body(query: str, *args, **kwargs) -> dict:
    """Build request body for N1QL REST API.

    Request body consists of ``statement`` key, ``args`` key
    (if using positional parameters), and any key prefixed with ``$``
    (if using named parameters).

    See https://docs.couchbase.com/server/current/n1ql/n1ql-rest-api/index.html for reference.

    :params query: N1QL query string.
    :params *args: Positional parameters passed as ``args`` in request body.
    :params **kwargs: Named parameters passed as ``$``-prefixed
                      parameter in request body.
    """
    body = {"statement": query}

    if args:
        body["args"] = json.dumps(args or [])

    if kwargs:
        for k, v in kwargs.items():
            body[f"${k}"] = json.dumps(v)
    return body


class RestClient(BaseClient):
    """This class interacts with REST server (part of Couchbase).
    """

    @property
    def port(self):
        """Port where REST server is bind to.
        """
        if as_boolean(os.environ.get("CN_COUCHBASE_TRUSTSTORE_ENABLE", True)):
            return 18091
        return 8091

    def healthcheck(self, host):
        """Run healthcheck to a host.

        :params host: Hostname or IP address.
        :returns: An instance of ``requests.models.Response``.
        """
        return self.session.get(
            f"{self.scheme}://{host}:{self.port}/pools/",
            auth=(self.user, self.password),
            timeout=10,
        )

    def exec_api(self, path, **kwargs):
        """Execute a request to REST server.

        :params path: Path (or sub-URL) of API server.
        :params kwargs: Keyword-argument passed to ``requests.api.*`` function.
        :returns: An instance of ``requests.models.Response``.
        """
        data = kwargs.get("data", {})
        method = kwargs.get("method")

        callbacks = {
            "GET": self.session.get,
            "POST": partial(self.session.post, data=data),
            "PUT": partial(self.session.put, data=data),
        }

        req = callbacks.get(method)
        if not callable(req):
            raise ValueError(f"Unsupported method {method}")

        resp = req(
            f"{self.scheme}://{self.host}:{self.port}/{path}",
            auth=(self.user, self.password),
        )
        return resp


class CouchbaseClient:
    """This class interacts with Couchbase server.
    """

    def __init__(self, hosts, user, password):
        self.hosts = hosts
        self.user = user
        self.password = password
        self._rest_client = None
        self._n1ql_client = None

    @property
    def rest_client(self):
        """An instance of :class:`~jans.pycloudlib.persistence.couchbase.RestClient`.
        """
        if not self._rest_client:
            self._rest_client = RestClient(
                self.hosts, self.user, self.password,
            )
            self._rest_client.resolve_host()
            if not self._rest_client.host:
                raise ValueError(f"Unable to resolve host for data service from {self.hosts} list")
        return self._rest_client

    @property
    def n1ql_client(self):
        """An instance of :class:`~jans.pycloudlib.persistence.couchbase.N1qlClient`.
        """
        if not self._n1ql_client:
            self._n1ql_client = N1qlClient(
                self.hosts, self.user, self.password,
            )
            self._n1ql_client.resolve_host()
            if not self._n1ql_client.host:
                raise ValueError(f"Unable to resolve host for query service from {self.hosts} list")
        return self._n1ql_client

    def get_buckets(self):
        """Get all buckets.

        :returns: An instance of ``requests.models.Response``.
        """
        return self.rest_client.exec_api("pools/default/buckets", method="GET",)

    def add_bucket(self, name: str, memsize: int = 100, type_: str = "couchbase"):
        """Add new bucket.

        :params name: Bucket's name.
        :params memsize: Desired memory size of the bucket.
        :params type\\_: Bucket's type.
        :returns: An instance of ``requests.models.Response``.
        """
        return self.rest_client.exec_api(
            "pools/default/buckets",
            data={
                "name": name,
                "bucketType": type_,
                "ramQuotaMB": memsize,
                "authType": "sasl",
            },
            method="POST",
        )

    def get_system_info(self) -> dict:
        """Get system info of Couchbase server.

        :returns: A ``dict`` of system information retrieved from Couchbase server.
        """
        sys_info = {}
        resp = self.rest_client.exec_api("pools/default", method="GET",)

        if resp.ok:
            sys_info = resp.json()
        return sys_info

    def exec_query(self, query: str, *args, **kwargs):
        """Execute N1QL query.

        :params query: N1QL query string.
        :returns: An instance of ``requests.models.Response``.
        """
        data = build_n1ql_request_body(query, *args, **kwargs)
        return self.n1ql_client.exec_api("query/service", data=data)

    def create_user(self, username, password, fullname, roles):
        data = {
            "name": fullname,
            "password": password,
            "roles": roles,
        }
        return self.rest_client.exec_api(
            f"settings/rbac/users/local/{username}", data=data, method="PUT",
        )

    def get_index_nodes(self):
        resp = self.rest_client.exec_api("pools/default", method="GET")
        if not resp.ok:
            return []
        return [node for node in resp.json()["nodes"] if "index" in node["services"]]


# backward-compat
def suppress_verification_warning():
    import urllib3

    if as_boolean(os.environ.get("CN_COUCHBASE_SUPPRESS_VERIFICATION", True)):
        urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)


def get_couchbase_keepalive_interval():
    """Get keep-alive interval to Couchbase server.

    Default keep-alive interval is 30000  milliseconds. To change the value, pass
    `CN_COUCHBASE_KEEPALIVE_INTERVAL` environment variable.

    :returns: Keep-alive interval (in milliseconds).
    """
    default = 30000

    try:
        val = int(os.environ.get("CN_COUCHBASE_KEEPALIVE_INTERVAL", default))
    except ValueError:
        val = default
    return val


def get_couchbase_keepalive_timeout():
    """Get keep-alive timeout to Couchbase server.

    Default keepalive timeout is 2500  milliseconds. To change the value, pass
    `CN_COUCHBASE_KEEPALIVE_TIMEOUT` environment variable.

    :returns: Keep-alive timeout (in milliseconds).
    """
    default = 2500

    try:
        val = int(os.environ.get("CN_COUCHBASE_KEEPALIVE_TIMEOUT", default))
    except ValueError:
        val = default
    return val
