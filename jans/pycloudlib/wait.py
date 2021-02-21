"""
jans.pycloudlib.wait
~~~~~~~~~~~~~~~~~~~~

This module consists of startup order utilities.
"""

import json
import logging
import os
import sys

import backoff
import ldap3
import requests

from jans.pycloudlib.persistence.couchbase import get_couchbase_user
from jans.pycloudlib.persistence.couchbase import get_couchbase_password
from jans.pycloudlib.persistence.couchbase import CouchbaseClient
from jans.pycloudlib.persistence.sql import SQLClient
from jans.pycloudlib.utils import as_boolean
from jans.pycloudlib.utils import decode_text


logger = logging.getLogger(__name__)


class WaitError(Exception):
    """Class to mark error while running ``wait_for_*`` functions.
    """
    pass


def get_wait_max_time() -> int:
    """Get maximum time accepted by ``wait_for`` function.

    Default maximum time is 300 seconds. To change the value, pass
    `CN_WAIT_MAX_TIME` environment variable.

    .. code-block:: python

        import os

        from jans.pycloudlib import get_manager
        from jans.pycloudlib.wait import wait_for_config

        os.environ["CN_WAIT_MAX_TIME"] = "1200"

        manager = get_manager()
        wait_for_config(manager)

    :returns: Wait maximum time (in seconds).
    """
    default = 60 * 5
    try:
        max_time = int(os.environ.get("CN_WAIT_MAX_TIME", default))
    except ValueError:
        max_time = default
    return max(1, max_time)


def get_wait_interval() -> int:
    """Get interval time between each execution of ``wait_for`` function.

    Default interval time is 10 seconds. To change the value, pass
    `CN_WAIT_SLEEP_DURATION` environment variable.

    .. code-block:: python

        import os

        from jans.pycloudlib import get_manager
        from jans.pycloudlib.wait import wait_for_config

        os.environ["CN_WAIT_SLEEP_DURATION"] = "10"

        manager = get_manager()
        wait_for_config(manager)

    :returns: Wait interval (in seconds).
    """
    default = 10
    try:
        interval = int(os.environ.get("CN_WAIT_SLEEP_DURATION", default))
    except ValueError:
        interval = default
    return max(1, interval)


def on_backoff(details: dict):
    details["error"] = sys.exc_info()[1]
    details["kwargs"]["label"] = details["kwargs"].pop("label", "Service")
    logger.warning(
        "{kwargs[label]} is not ready; reason={error}; "
        "retrying in {wait:0.1f} seconds".format(**details)
    )


def on_success(details: dict):
    details["kwargs"]["label"] = details["kwargs"].pop("label", "Service")
    logger.info("{kwargs[label]} is ready".format(**details))


def on_giveup(details: dict):
    details["kwargs"]["label"] = details["kwargs"].pop("label", "Service")
    logger.error(
        "{kwargs[label]} is not ready after " "{elapsed:0.1f} seconds".format(**details)
    )


#: A pre-configured alias of ``backoff.on_exception`` decorator.
#:
#: This decorator implies following setup:
#:
#: - each retry is executed with constant time
#: - catch all ``Exception``
retry_on_exception = backoff.on_exception(
    backoff.constant,
    Exception,
    max_time=get_wait_max_time,
    on_backoff=on_backoff,
    on_success=on_success,
    on_giveup=on_giveup,
    jitter=None,
    interval=get_wait_interval,
)


@retry_on_exception
def wait_for_config(manager, **kwargs):
    """Wait for readiness/availability of config backend.

    If ``conn_only`` keyword argument is set to ``True``,
    this function only checks its connection status; if set
    to ``False`` or omitted, this function will check config entry.

    :param manager: An instance of :class:`~jans.pycloudlib.manager._Manager`.
    """
    conn_only = as_boolean(kwargs.get("conn_only", False))
    hostname = manager.config.get("hostname")

    if not conn_only and not hostname:
        raise WaitError("Config 'hostname' is not available")


@retry_on_exception
def wait_for_secret(manager, **kwargs):
    """Wait for readiness/availability of secret backend.

    If ``conn_only`` keyword argument is set to ``True``,
    this function only checks its connection status; if set
    to ``False`` or omitted, this function will check config entry.

    :param manager: An instance of :class:`~jans.pycloudlib.manager._Manager`.
    """
    conn_only = as_boolean(kwargs.get("conn_only", False))
    ssl_cert = manager.secret.get("ssl_cert")

    if not conn_only and not ssl_cert:
        raise WaitError("Secret 'ssl_cert' is not available")


@retry_on_exception
def wait_for_ldap(manager, **kwargs):
    """Wait for readiness/availability of LDAP server based on existing entry.

    :param manager: An instance of :class:`~jans.pycloudlib.manager._Manager`.
    """
    host = os.environ.get("CN_LDAP_URL", "localhost:1636")
    user = manager.config.get("ldap_binddn")
    password = decode_text(
        manager.secret.get("encoded_ox_ldap_pw"), manager.secret.get("encoded_salt")
    )

    persistence_type = os.environ.get("CN_PERSISTENCE_TYPE", "ldap")
    ldap_mapping = os.environ.get("CN_PERSISTENCE_LDAP_MAPPING", "default")
    ldap_server = ldap3.Server(host, 1636, use_ssl=True)

    # a minimum service stack is having config-api client
    jca_client_id = manager.config.get("jca_client_id")
    default_search = (
        f"inum={jca_client_id},ou=clients,o=jans",
        "(objectClass=jansClnt)",
    )

    if persistence_type == "hybrid":
        # `cache` and `token` mapping only have base entries
        search_mapping = {
            "default": default_search,
            "user": ("inum=60B7,ou=groups,o=jans", "(objectClass=jansGrp)"),
            "site": ("ou=cache-refresh,o=site", "(ou=cache-refresh)"),
            "cache": ("ou=cache,o=jans", "(ou=cache)"),
            "token": ("ou=tokens,o=jans", "(ou=tokens)"),
            "session": ("ou=sessions,o=jans", "(ou=sessions)"),
        }
        search = search_mapping[ldap_mapping]
    else:
        search = default_search

    with ldap3.Connection(ldap_server, user, password) as conn:
        conn.search(
            search_base=search[0],
            search_filter=search[1],
            search_scope=ldap3.SUBTREE,
            attributes=["objectClass"],
            size_limit=1,
        )

        if not conn.entries:
            raise WaitError("LDAP is not fully initialized")


@retry_on_exception
def wait_for_ldap_conn(manager, **kwargs):
    """Wait for readiness/availability of LDAP server based on connection status.

    :param manager: An instance of :class:`~jans.pycloudlib.manager._Manager`.
    """
    host = os.environ.get("CN_LDAP_URL", "localhost:1636")
    user = manager.config.get("ldap_binddn")
    password = decode_text(
        manager.secret.get("encoded_ox_ldap_pw"), manager.secret.get("encoded_salt")
    )

    ldap_server = ldap3.Server(host, 1636, use_ssl=True)
    search = ("", "(objectClass=*)")

    with ldap3.Connection(ldap_server, user, password) as conn:
        conn.search(
            search_base=search[0],
            search_filter=search[1],
            search_scope=ldap3.BASE,
            attributes=["1.1"],
            size_limit=1,
        )
        if not conn.entries:
            raise WaitError("LDAP is unreachable")


@retry_on_exception
def wait_for_couchbase(manager, **kwargs):
    """Wait for readiness/availability of Couchbase server based on existing entry.

    :param manager: An instance of :class:`~jans.pycloudlib.manager._Manager`.
    """
    host = os.environ.get("CN_COUCHBASE_URL", "localhost")
    user = get_couchbase_user(manager)
    password = get_couchbase_password(manager)

    persistence_type = os.environ.get("CN_PERSISTENCE_TYPE", "couchbase")
    ldap_mapping = os.environ.get("CN_PERSISTENCE_LDAP_MAPPING", "default")
    bucket_prefix = os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")

    # only default and user buckets buckets that may have initial data;
    # these data also affected by LDAP mapping selection;
    jca_client_id = manager.config.get("jca_client_id")
    bucket, key = bucket_prefix, f"clients_{jca_client_id}"

    # if `hybrid` is selected and default mapping is stored in LDAP,
    # the default bucket won't have data, hence we check the user bucket instead
    if persistence_type == "hybrid" and ldap_mapping == "default":
        bucket, key = f"{bucket_prefix}_user", "groups_60B7"

    cb_client = CouchbaseClient(host, user, password)

    req = cb_client.exec_query(
        f"SELECT objectClass FROM {bucket} USE KEYS $key",
        key=key,
    )

    if not req.ok:
        try:
            data = json.loads(req.text)
            err = data["errors"][0]["msg"]
        except (ValueError, KeyError, IndexError):
            err = req.reason
        raise WaitError(err)

    # request is OK, but result is not found
    data = req.json()
    if not data["results"]:
        raise WaitError(f"Missing document {key} in bucket {bucket}")


@retry_on_exception
def wait_for_couchbase_conn(manager, **kwargs):
    """Wait for readiness/availability of Couchbase server based on connection status.

    :param manager: An instance of :class:`~jans.pycloudlib.manager._Manager`.
    """
    host = os.environ.get("CN_COUCHBASE_URL", "localhost")
    user = get_couchbase_user(manager)
    password = get_couchbase_password(manager)

    cb_client = CouchbaseClient(host, user, password)
    req = cb_client.get_buckets()

    if not req.ok:
        raise WaitError(f"Unable to connect to host in {host} list")


@retry_on_exception
def wait_for_oxauth(manager, **kwargs):
    """Wait for readiness/availability of oxAuth server.

    This function makes a request to specific URL in oxAuth.

    :param manager: An instance of :class:`~jans.pycloudlib.manager._Manager`.
    """
    addr = os.environ.get("CN_OXAUTH_BACKEND", "localhost:8081")
    url = f"http://{addr}/oxauth/.well-known/openid-configuration"
    req = requests.get(url)

    if not req.ok:
        raise WaitError(req.reason)


@retry_on_exception
def wait_for_oxtrust(manager, **kwargs):
    """Wait for readiness/availability of oxTrust server.

    This function makes a request to specific URL in oxTrust.

    :param manager: An instance of :class:`~jans.pycloudlib.manager._Manager`.
    """
    addr = os.environ.get("CN_OXTRUST_BACKEND", "localhost:8082")
    url = f"http://{addr}/identity/finishlogout.htm"
    req = requests.get(url)

    if not req.ok:
        raise WaitError(req.reason)


@retry_on_exception
def wait_for_oxd(manager, **kwargs):
    """Wait for readiness/availability of oxd server.

    This function makes a request to specific URL in oxd.

    :param manager: An instance of :class:`~jans.pycloudlib.manager._Manager`.
    """
    import urllib3

    urllib3.disable_warnings()

    addr = os.environ.get("CN_OXD_SERVER_URL", "localhost:8443")
    verify = as_boolean(os.environ.get("CN_OXD_SERVER_VERIFY", False))
    url = f"https://{addr}/health-check"
    req = requests.get(url, verify=verify)

    if not req.ok:
        raise WaitError(req.reason)


@retry_on_exception
def wait_for_sql_conn(manager, **kwargs):
    """Wait for readiness/liveness of an SQL database connection.
    """
    # checking connection
    SQLClient().is_alive()


@retry_on_exception
def wait_for_sql(manager, **kwargs):
    """Wait for readiness/liveness of an SQL database.
    """
    from sqlalchemy.sql import text

    client = SQLClient()

    with client.engine.connect() as conn:
        result = conn.execute(
            text("SELECT COUNT(doc_id) FROM jansClnt WHERE doc_id = :doc_id"),
            **{"doc_id": manager.config.get("jca_client_id")}
        )

        if not result.fetchone()[0]:
            raise WaitError("SQL is not fully initialized")


def wait_for(manager, deps=None):
    """A high-level function to run one or more ``wait_for_*`` function(s).

    The following dependencies are supported:

    - `config`
    - `config_conn`
    - `ldap`
    - `ldap_conn`
    - `couchbase`
    - `couchbase_conn`
    - `secret`
    - `secret_conn`
    - `oxauth`
    - `oxtrust`
    - `oxd`
    - `sql`
    - `sql_conn`

    .. code-block:: python

        from jans.pycloudlib import get_manager
        from jans.pycloudlib.wait import wait_for

        manager = get_manager()
        deps = ["config", "secret", "ldap"]
        wait_for(manager, deps)

    :param manager: An instance of :class:`~jans.pycloudlib.manager._Manager`.
    :param deps: An iterable of dependencies to check.
    """
    deps = deps or []
    callbacks = {
        "config": {"func": wait_for_config, "kwargs": {"label": "Config"}},
        "config_conn": {
            "func": wait_for_config,
            "kwargs": {"label": "Config", "conn_only": True},
        },
        "ldap": {"func": wait_for_ldap, "kwargs": {"label": "LDAP"}},
        "ldap_conn": {"func": wait_for_ldap_conn, "kwargs": {"label": "LDAP"}},
        "couchbase": {"func": wait_for_couchbase, "kwargs": {"label": "Couchbase"}},
        "couchbase_conn": {
            "func": wait_for_couchbase_conn,
            "kwargs": {"label": "Couchbase"},
        },
        "secret": {"func": wait_for_secret, "kwargs": {"label": "Secret"}},
        "secret_conn": {
            "func": wait_for_secret,
            "kwargs": {"label": "Secret", "conn_only": True},
        },
        "oxauth": {"func": wait_for_oxauth, "kwargs": {"label": "oxAuth"}},
        "oxtrust": {"func": wait_for_oxtrust, "kwargs": {"label": "oxTrust"}},
        "oxd": {"func": wait_for_oxd, "kwargs": {"label": "oxd"}},
        "sql_conn": {"func": wait_for_sql_conn, "kwargs": {"label": "SQL"}},
        "sql": {"func": wait_for_sql, "kwargs": {"label": "SQL"}},
    }

    for dep in deps:
        callback = callbacks.get(dep)
        if not callback:
            logger.warning(f"Unsupported callback for {dep} dependency")
            continue
        callback["func"](manager, **callback["kwargs"])
