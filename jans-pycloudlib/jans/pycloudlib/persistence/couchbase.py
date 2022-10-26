"""This module contains various helpers related to Couchbase persistence."""

from __future__ import annotations

import contextlib
import json
import logging
import os
import typing as _t
from abc import ABC
from abc import abstractmethod
from abc import abstractproperty
from datetime import datetime
from functools import cached_property
from functools import partial
from tempfile import NamedTemporaryFile

import requests
from requests_toolbelt.adapters.host_header_ssl import HostHeaderSSLAdapter
from ldif import LDIFParser

from jans.pycloudlib.persistence.utils import PersistenceMapper
from jans.pycloudlib.persistence.utils import RDN_MAPPING
from jans.pycloudlib.utils import encode_text
from jans.pycloudlib.utils import get_random_chars
from jans.pycloudlib.utils import cert_to_truststore
from jans.pycloudlib.utils import as_boolean
from jans.pycloudlib.utils import safe_render

if _t.TYPE_CHECKING:  # pragma: no cover
    # imported objects for function type hint, completion, etc.
    # these won't be executed in runtime
    from jans.pycloudlib.manager import Manager
    from requests import Response

logger = logging.getLogger(__name__)


def _get_cb_password(manager: Manager, password_file: str, secret_name: str) -> str:
    """Get Couchbase user's password.

    Priority:

    1. get from password file (for backward-compat)
    2. get from secrets

    Args:
        manager: An instance of manager class.
        password_file: Path to file contains password.
        secret_name: Name of the secrets to pull/push the password.

    Returns:
        Plaintext password.
    """
    if os.path.isfile(password_file):
        with open(password_file) as f:
            password = f.read().strip()
            manager.secret.set(secret_name, password)
            logger.warning(
                f"Loading password from {password_file} file is deprecated and will be removed in future releases. "
                f"Note, the password has been saved to secrets with key {secret_name} for later usage."
            )
    else:
        # get from secrets (if any)
        password = manager.secret.get(secret_name)
    return password


def get_couchbase_user(manager: _t.Union[Manager, None] = None) -> str:
    """Get Couchbase username from `CN_COUCHBASE_USER` environment variable (default to `admin`).

    Args:
        manager: A no-op argument, preserved for backward compatibility.

    Returns:
        Couchbase username.
    """
    return os.environ.get("CN_COUCHBASE_USER", "admin")


def get_couchbase_password(manager: Manager) -> str:
    """Get Couchbase user's password from file.

    Default file is set to `/etc/jans/conf/couchbase_password`.
    To change the location, simply pass `CN_COUCHBASE_PASSWORD_FILE` environment variable.

    Args:
        manager: An instance of manager class.

    Returns:
        Plaintext password.
    """
    secret_name = "couchbase_password"  # nosec: B105
    password_file = os.environ.get("CN_COUCHBASE_PASSWORD_FILE", "/etc/jans/conf/couchbase_password")
    return _get_cb_password(manager, password_file, secret_name)


def get_couchbase_superuser(manager: _t.Union[Manager, None] = None) -> str:
    """Get Couchbase username from `CN_COUCHBASE_SUPERUSER` environment variable (default to empty-string).

    Args:
        manager: A no-op argument, preserved for backward compatibility.

    Returns:
        Couchbase username.
    """
    return os.environ.get("CN_COUCHBASE_SUPERUSER", "")


def get_couchbase_superuser_password(manager: Manager) -> str:
    """Get Couchbase superuser's password from file.

    Default file is set to `/etc/jans/conf/couchbase_superuser_password`.
    To change the location, simply pass `CN_COUCHBASE_SUPERUSER_PASSWORD_FILE` environment variable.

    Args:
        manager: An instance of manager class.

    Returns:
        Plaintext password.
    """
    secret_name = "couchbase_superuser_password"  # nosec: B105
    password_file = os.environ.get("CN_COUCHBASE_SUPERUSER_PASSWORD_FILE", "/etc/jans/conf/couchbase_superuser_password")
    return _get_cb_password(manager, password_file, secret_name)


def get_couchbase_conn_timeout() -> int:
    """Get connection timeout to Couchbase server.

    Default connection timeout is 10000  milliseconds. To change the value, pass
    `CN_COUCHBASE_CONN_TIMEOUT` environment variable.

    Returns:
        Connection timeout (in milliseconds).
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

    Returns:
        Connection wait time (in milliseconds).
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

    - `not_bounded` (default)
    - `request_plus`
    - `statement_plus`

    Returns:
        Scan consistency type.
    """
    opts = ("not_bounded", "request_plus", "statement_plus")
    default = "not_bounded"
    opt = os.environ.get("CN_COUCHBASE_SCAN_CONSISTENCY", default)
    if opt not in opts:
        opt = default
    return opt


def render_couchbase_properties(manager: Manager, src: str, dest: str) -> None:
    """Render file contains properties to connect to Couchbase server.

    Args:
        manager: An instance of manager class.
        src: Absolute path to the template.
        dest: Absolute path where generated file is located.
    """
    hostname = os.environ.get("CN_COUCHBASE_URL", "localhost")
    bucket_prefix = os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")

    mapper = PersistenceMapper()
    groups = mapper.groups()["couchbase"]

    mappings = {}
    for rmapping, rdn in RDN_MAPPING.items():
        if rmapping not in groups:
            continue

        if rmapping == "default":
            bucket = ""
        else:
            bucket = f"{bucket_prefix}_{rmapping}"

        mappings[rmapping] = {
            "bucket": bucket,
            "rdn": rdn,
        }

    couchbase_buckets = []
    couchbase_mappings = []

    for mapping in mappings.values():
        if mapping["bucket"]:
            couchbase_buckets.append(mapping["bucket"])

        if mapping["rdn"]:
            couchbase_mappings.append(
                f"bucket.{mapping['bucket']}.mapping: {mapping['rdn']}"
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
            "encoded_couchbase_server_pw": encode_text(
                get_couchbase_password(manager),
                manager.secret.get("encoded_salt"),
            ).decode(),
            "couchbase_buckets": ", ".join(couchbase_buckets),
            "default_bucket": bucket_prefix,
            "couchbase_mappings": "\n".join(couchbase_mappings),
            "encryption_method": "SSHA-256",
            "ssl_enabled": str(as_boolean(
                os.environ.get("CN_COUCHBASE_TRUSTSTORE_ENABLE", True)
            )).lower(),
            "couchbaseTrustStoreFn": manager.config.get("couchbaseTrustStoreFn") or "/etc/certs/couchbase.pkcs12",
            "encoded_couchbaseTrustStorePass": encode_text(
                resolve_couchbase_truststore_pw(manager),
                manager.secret.get("encoded_salt"),
            ).decode(),
            "couchbase_conn_timeout": get_couchbase_conn_timeout(),
            "couchbase_conn_max_wait": get_couchbase_conn_max_wait(),
            "couchbase_scan_consistency": get_couchbase_scan_consistency(),
        }
        fw.write(rendered_txt)


# DEPRECATED
def sync_couchbase_cert(manager: _t.Union[Manager, None] = None) -> str:
    """Synchronize Couchbase certificate.

    This function is deprecated and will be removed in future versions.
    """
    cert_file = os.environ.get("CN_COUCHBASE_CERT_FILE", "/etc/certs/couchbase.crt")
    with open(cert_file) as f:
        return f.read()


def sync_couchbase_truststore(manager: Manager, dest: str = "/etc/certs/couchbase.pkcs12") -> None:
    """Pull secret contains base64-string contents of Couchbase truststore and save it as a JKS file.

    Args:
        manager: An instance of manager class.
        dest: Absolute path where generated file is located.
    """
    cert_file = os.environ.get("CN_COUCHBASE_CERT_FILE", "/etc/certs/couchbase.crt")
    dest = dest or manager.config.get("couchbaseTrustStoreFn")
    cert_to_truststore(
        "couchbase", cert_file, dest, resolve_couchbase_truststore_pw(manager),
    )


class BaseApi(ABC):
    """A base class for API client."""

    @abstractproperty
    def port(self) -> int:
        """Port used by API server."""

    def __init__(self, hosts: str, user: str, password: str) -> None:
        self._hosts = hosts
        self.host = ""
        self.user = user
        self.password = password

    @property
    def scheme(self) -> str:
        """Scheme used when connecting to Couchbase server."""
        if as_boolean(os.environ.get("CN_COUCHBASE_TRUSTSTORE_ENABLE", True)):
            return "https"
        return "http"

    @cached_property
    def session(self) -> requests.Session:
        """Get an instance of `requests.Session`.

        By default, the session will not use certificate verification.

        To enable certificate verification:

        - set `CN_COUCHBASE_VERIFY` environment variable to `true` (default to `false`)
        - ensure `CN_COUCHBASE_CERT_FILE` pointed to valid Couchbase cluster
          certificate (default to `/etc/certs/couchbase.crt`)
        - optionally, set `CN_COUCHBASE_HOST_HEADER` to match Common Name
          or any of SubjectAltName defined in certificate (default to `localhost`)
        """
        suppress_verification_warning()

        session = requests.Session()
        session.verify = False

        verify = as_boolean(os.environ.get("CN_COUCHBASE_VERIFY", False))
        if verify:
            session.mount("https://", HostHeaderSSLAdapter())
            session.verify = os.environ.get("CN_COUCHBASE_CERT_FILE") or "/etc/certs/couchbase.crt"
            session.headers["Host"] = os.environ.get("CN_COUCHBASE_HOST_HEADER") or "localhost"
        return session

    def resolve_host(self) -> str:
        """Get active/ready host from a list of servers.

        Returns:
            Hostname or IP address.
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

    @abstractmethod
    def healthcheck(self, host: str) -> Response:  # pragma: no cover
        """Run healthcheck to a host.

        !!! important
            Subclass **MUST** implement this method.
        """

    @abstractmethod
    def exec_api(self, path: str, **kwargs: _t.Any) -> Response:  # pragma: no cover
        """Execute a request to an API server.

        !!! important
            Subclass **MUST** implement this method.
        """


class N1qlApi(BaseApi):
    """This class interacts with N1QL server (part of Couchbase)."""

    @property
    def port(self) -> int:
        """Port where N1QL server is bind to."""
        if as_boolean(os.environ.get("CN_COUCHBASE_TRUSTSTORE_ENABLE", True)):
            return 18093
        return 8093

    def healthcheck(self, host: str) -> Response:
        """Run healthcheck to a host.

        Args:
            host: Hostname or IP address.

        Returns:
            An instance of `requests.Response`.
        """
        return self.session.post(
            f"{self.scheme}://{host}:{self.port}/query/service",
            data={"statement": "SELECT status FROM system:indexes LIMIT 1"},
            auth=(self.user, self.password),
            timeout=10,
        )

    def exec_api(self, path: str, **kwargs: _t.Any) -> Response:
        """Execute a request to REST server.

        Args:
            path: Path (or sub-URL) of API server.
            **kwargs: Keyword-argument passed to `requests.api.*` function.

        Returns:
            An instance of `requests.Response`.
        """
        data = kwargs.get("data", {})

        resp = self.session.post(
            f"{self.scheme}://{self.host}:{self.port}/{path}",
            data=data,
            auth=(self.user, self.password),
        )
        return resp


def build_n1ql_request_body(query: str, *args: _t.Any, **kwargs: _t.Any) -> dict[str, _t.Any]:
    """Build request body for N1QL REST API.

    Request body consists of `statement` key, `args` key
    (if using positional parameters), and any key prefixed with `$`
    (if using named parameters).

    See https://docs.couchbase.com/server/current/n1ql/n1ql-rest-api/index.html for reference.

    Args:
        query: N1QL query string.
        *args: Positional parameters passed as `args` in request body.
        **kwargs: Named parameters passed as `$`-prefixed parameter in request body.
    """
    body = {"statement": query}

    if args:
        body["args"] = json.dumps(args or [])

    if kwargs:
        for k, v in kwargs.items():
            body[f"${k}"] = json.dumps(v)
    return body


class RestApi(BaseApi):
    """This class interacts with REST server (part of Couchbase)."""

    @property
    def port(self) -> int:
        """Port where REST server is bind to."""
        if as_boolean(os.environ.get("CN_COUCHBASE_TRUSTSTORE_ENABLE", True)):
            return 18091
        return 8091

    def healthcheck(self, host: str) -> Response:
        """Run healthcheck to a host.

        Args:
            host: Hostname or IP address.

        Returns:
            An instance of `requests.Response`.
        """
        return self.session.get(
            f"{self.scheme}://{host}:{self.port}/pools/",
            auth=(self.user, self.password),
            timeout=10,
        )

    def exec_api(self, path: str, **kwargs: _t.Any) -> Response:
        """Execute a request to REST server.

        Args:
            path: Path (or sub-URL) of API server.
            **kwargs: Keyword-argument passed to `requests.api.*` function.

        Returns:
            An instance of `requests.Response`.
        """
        data = kwargs.get("data", {})
        method = kwargs.get("method", "")

        callbacks = {
            "GET": self.session.get,
            "POST": partial(self.session.post, data=data),
            "PUT": partial(self.session.put, data=data),
        }

        req = callbacks.get(method)
        if not callable(req):
            raise ValueError(f"Unsupported method {method}")

        resp: Response = req(
            f"{self.scheme}://{self.host}:{self.port}/{path}",
            auth=(self.user, self.password),
        )
        return resp


class AttrProcessor:
    """Attribute processor."""

    @cached_property
    def attr_maps(self) -> dict[str, list[str]]:
        """Get a mapping of OpenDJ types."""
        with open("/app/schema/opendj_types.json") as f:
            return json.loads(f.read())  # type: ignore

    @cached_property
    def schemas(self) -> list[dict[str, _t.Any]]:
        """Get a mapping of schemas from pre-defined file."""
        with open("/app/schema/jans_schema.json") as f:
            return json.loads(f.read()).get("attributeTypes", [])  # type: ignore

    @property
    def syntax_types(self) -> dict[str, str]:
        """Get a mapping of syntax types."""
        return {
            '1.3.6.1.4.1.1466.115.121.1.7': 'boolean',
            '1.3.6.1.4.1.1466.115.121.1.27': 'integer',
            '1.3.6.1.4.1.1466.115.121.1.24': 'datetime',
        }

    def process(self) -> dict[str, _t.Any]:
        """Pre-populate attributes."""
        attrs = {}

        for type_, names in self.attr_maps.items():
            for name in names:
                attrs[name] = {"type": type_, "multivalued": False}

        for schema in self.schemas:
            if schema.get("json"):
                type_ = "json"
            elif schema["syntax"] in self.syntax_types:
                type_ = self.syntax_types[schema["syntax"]]
            else:
                type_ = "string"

            multivalued = schema.get("multivalued", False)
            for name in schema["names"]:
                attrs[name] = {
                    "type": type_,
                    "multivalued": multivalued,
                }

        # override `member`
        attrs["member"]["multivalued"] = True
        return attrs

    @cached_property
    def attrs(self) -> dict[str, _t.Any]:
        """Get a mapping of attributes."""
        return self.process()

    def is_multivalued(self, name: str) -> bool:
        """Check whether attribute is multivalued.

        Args:
            name: Attribute name.
        """
        return bool(self.attrs.get(name, {}).get("multivalued", False))

    def get_type(self, name: str) -> str:
        """Get attribute's data type.

        Args:
            name: Attribute name.
        """
        return str(self.attrs.get(name, {}).get("type", "string"))


class CouchbaseClient:
    """This class interacts with Couchbase server.

    Args:
        manager: An instance of manager class.
        *args: Positional arguments (if any).
        **kwargs: Keyword arguments (if any).
    """

    def __init__(self, manager: Manager, *args: _t.Any, **kwargs: _t.Any) -> None:
        self.manager = manager

        self.hosts = kwargs.get("hosts", "") or os.environ.get("CN_COUCHBASE_URL", "localhost")
        self.user = kwargs.get("user", "") or get_couchbase_superuser(manager) or get_couchbase_user(manager)

        password = kwargs.get("password", "")
        with contextlib.suppress(FileNotFoundError):
            password = get_couchbase_superuser_password(manager)
        self.password: str = password or get_couchbase_password(manager)

        self.attr_processor = AttrProcessor()

    @cached_property
    def rest_client(self) -> RestApi:
        """Get instance of [jans.pycloudlib.persistence.couchbase.RestApi][] class."""
        client = RestApi(self.hosts, self.user, self.password)
        client.resolve_host()
        if not client.host:
            raise ValueError(f"Unable to resolve host for data service from {self.hosts} list")
        return client

    @cached_property
    def n1ql_client(self) -> N1qlApi:
        """Get instance of [jans.pycloudlib.persistence.couchbase.N1qlApi][] class."""
        client = N1qlApi(self.hosts, self.user, self.password)
        client.resolve_host()
        if not client.host:
            raise ValueError(f"Unable to resolve host for query service from {self.hosts} list")
        return client

    def get_buckets(self) -> Response:
        """Get all buckets.

        Returns:
            An instance of `requests.Response`.
        """
        kwargs: dict[str, _t.Any] = {"method": "GET"}
        resp: Response = self.rest_client.exec_api("pools/default/buckets", **kwargs)
        return resp

    def add_bucket(self, name: str, memsize: int = 100, type_: str = "couchbase") -> Response:
        """Add new bucket.

        Args:
            name: Bucket's name.
            memsize: Desired memory size of the bucket.
            type_: Bucket's type.

        Returns:
            An instance of `requests.Response`.
        """
        kwargs: dict[str, _t.Any] = {
            "data": {
                "name": name,
                "bucketType": type_,
                "ramQuotaMB": memsize,
                "authType": "sasl",
            },
            "method": "POST",
        }
        resp: Response = self.rest_client.exec_api("pools/default/buckets", **kwargs)
        return resp

    def get_system_info(self) -> dict[str, _t.Any]:
        """Get system info of Couchbase server.

        Returns:
            A mapping of system information retrieved from Couchbase server.
        """
        sys_info = {}
        kwargs: dict[str, _t.Any] = {"method": "GET"}
        resp = self.rest_client.exec_api("pools/default", **kwargs)

        if resp.ok:
            sys_info = resp.json()
        return sys_info

    def exec_query(self, query: str, *args: _t.Any, **kwargs: _t.Any) -> Response:
        """Execute N1QL query.

        Args:
            query: N1QL query string.

        Returns:
            An instance of `requests.Response`.
        """
        data = build_n1ql_request_body(query, *args, **kwargs)
        resp: Response = self.n1ql_client.exec_api("query/service", data=data)
        return resp

    def create_user(self, username: str, password: str, fullname: str, roles: str) -> Response:
        """Create user by making request to REST API."""
        kwargs: dict[str, _t.Any] = {
            "data": {
                "name": fullname,
                "password": password,
                "roles": roles,
            },
            "method": "PUT",
        }
        resp: Response = self.rest_client.exec_api(f"settings/rbac/users/local/{username}", **kwargs)
        return resp

    def get_index_nodes(self) -> list[str]:
        """Get list of nodes that has index service."""
        kwargs: dict[str, _t.Any] = {"method": "GET"}
        resp = self.rest_client.exec_api("pools/default", **kwargs)
        if not resp.ok:
            return []
        return [node for node in resp.json()["nodes"] if "index" in node["services"]]

    def _transform_value(self, name: str, values: _t.Any) -> _t.Any:
        """Transform value from one to another based on its data type.

        Args:
            name: Attribute name.
            values: Pre-transformed values.
        """
        def as_dict(val: _t.Any) -> dict[str, _t.Any]:
            retval: dict[str, _t.Any] = json.loads(val)
            return retval

        def as_bool(val: _t.Any) -> bool:
            return val.lower() in ("true", "yes", "1", "on")

        def as_int(val: _t.Any) -> _t.Union[int, _t.Any]:
            retval = val
            try:
                retval = int(val)
            except (TypeError, ValueError):
                pass
            return retval

        def as_datetime(val: _t.Any) -> str:
            if '.' in val:
                date_format = '%Y%m%d%H%M%S.%fZ'
            else:
                date_format = '%Y%m%d%H%M%SZ'

            if not val.lower().endswith('z'):
                val += 'Z'

            dt = datetime.strptime(val, date_format)
            return dt.isoformat()

        callbacks = {
            "json": as_dict,
            "boolean": as_bool,
            "integer": as_int,
            "datetime": as_datetime,
        }

        type_ = self.attr_processor.get_type(name)
        callback: _t.Union[_t.Callable[..., _t.Any], None] = callbacks.get(type_)

        # maybe string
        if not callable(callback):
            return values
        return [callback(item) for item in values]

    def _transform_entry(self, entry: dict[str, _t.Any]) -> dict[str, _t.Any]:
        for k, v in entry.items():
            v = self._transform_value(k, v)

            if len(v) == 1 and self.attr_processor.is_multivalued(k) is False:
                entry[k] = v[0]

            if k != "objectClass":
                continue

            entry[k].remove("top")
            ocs = entry[k]

            for oc in ocs:
                remove_oc = any(["Custom" in oc, "jans" not in oc.lower()])
                if len(ocs) > 1 and remove_oc:
                    ocs.remove(oc)
            entry[k] = ocs[0]
        return entry

    def create_from_ldif(self, filepath: str, ctx: dict[str, _t.Any]) -> None:
        """Create entry with data loaded from an LDIF template file.

        Args:
            filepath: Path to LDIF template file.
            ctx: Key-value pairs of context that rendered into LDIF template file.
        """
        with open(filepath) as src, NamedTemporaryFile("w+") as dst:
            dst.write(safe_render(src.read(), ctx))
            # ensure rendered template is written
            dst.flush()

            with open(dst.name, "rb") as fd:
                parser = LDIFParser(fd)

                for dn, entry in parser.parse():
                    if len(entry) <= 2:
                        continue

                    key = id_from_dn(dn)
                    bucket = get_bucket_for_key(key)
                    entry["dn"] = [dn]
                    entry = self._transform_entry(entry)
                    data = json.dumps(entry)

                    # using INSERT will cause duplication error, but the data is left intact
                    query = 'INSERT INTO `%s` (KEY, VALUE) VALUES ("%s", %s)' % (bucket, key, data)  # nosec: B608
                    req = self.exec_query(query)

                    if not req.ok:
                        logger.warning("Failed to execute query, reason={}".format(req.json()))

    def doc_exists(self, bucket: str, id_: str) -> bool:
        """Check if certain document exists in a bucket.

        Args:
            bucket: Bucket name.
            id_: ID of document.
        """
        kwargs: dict[str, _t.Any] = {"key": id_}
        req = self.exec_query(
            f"SELECT objectClass FROM {bucket} USE KEYS $key",  # nosec: B608
            **kwargs,
        )

        if not req.ok:
            try:
                data = json.loads(req.text)
                err = data["errors"][0]["msg"]
            except (ValueError, KeyError, IndexError):
                err = req.reason
            logger.warning(f"Unable to find document {id_} in bucket {bucket}; reason={err}")
            return False

        if not req.json()["results"]:
            logger.warning(f"Missing document {id_} in bucket {bucket}")
            return False
        return True


# backward-compat
def suppress_verification_warning() -> None:
    """Hide verification warning when using non-secure connection."""
    import urllib3

    if as_boolean(os.environ.get("CN_COUCHBASE_SUPPRESS_VERIFICATION", True)):
        urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)


def get_couchbase_keepalive_interval() -> int:
    """Get keep-alive interval to Couchbase server.

    Default keep-alive interval is 30000  milliseconds. To change the value, pass
    `CN_COUCHBASE_KEEPALIVE_INTERVAL` environment variable.

    Returns:
        Keep-alive interval (in milliseconds).
    """
    default = 30000

    try:
        val = int(os.environ.get("CN_COUCHBASE_KEEPALIVE_INTERVAL", default))
    except ValueError:
        val = default
    return val


def get_couchbase_keepalive_timeout() -> int:
    """Get keep-alive timeout to Couchbase server.

    Default keepalive timeout is 2500  milliseconds. To change the value, pass
    `CN_COUCHBASE_KEEPALIVE_TIMEOUT` environment variable.

    Returns:
        Keep-alive timeout (in milliseconds).
    """
    default = 2500

    try:
        val = int(os.environ.get("CN_COUCHBASE_KEEPALIVE_TIMEOUT", default))
    except ValueError:
        val = default
    return val


def id_from_dn(dn: str) -> str:
    """Determine document ID based on LDAP's DN.

    Args:
        dn: LDAP's DN string.

    Returns:
        Document ID.
    """
    # for example: `"inum=29DA,ou=attributes,o=jans"`
    # becomes `["29DA", "attributes"]`
    dns = [i.split("=")[-1] for i in dn.split(",") if i != "o=jans"]
    dns.reverse()

    # the actual key
    return '_'.join(dns) or "_"


def get_bucket_for_key(key: str) -> str:
    """Determine which bucket goes for specific key.

    Returns:
        Bucket name with prefix.
    """
    bucket_prefix = os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")

    cursor = key.find("_")
    key_prefix = key[:cursor + 1]

    if key_prefix in ("groups_", "people_", "authorizations_"):
        bucket = f"{bucket_prefix}_user"
    elif key_prefix in ("site_", "cache-refresh_"):
        bucket = f"{bucket_prefix}_site"
    elif key_prefix in ("tokens_",):
        bucket = f"{bucket_prefix}_token"
    elif key_prefix in ("cache_",):
        bucket = f"{bucket_prefix}_cache"
    else:
        bucket = bucket_prefix
    return bucket


def resolve_couchbase_truststore_pw(manager: Manager) -> str:
    """Resolve Couchbase truststore password.

    Args:
        manager: An instance of manager class.

    Returns:
        Password for truststore used by Couchbase connection.
    """
    pw: str = manager.secret.get("couchbase_truststore_pw")

    if not pw:
        pw = get_random_chars()
        manager.secret.set("couchbase_truststore_pw", pw)
    return pw
