"""This module consists of startup order utilities."""

from __future__ import annotations

import logging
import os
import sys
import typing as _t

import backoff

from jans.pycloudlib.persistence.couchbase import CouchbaseClient
from jans.pycloudlib.persistence.couchbase import id_from_dn
from jans.pycloudlib.persistence.sql import SqlClient
from jans.pycloudlib.persistence.sql import doc_id_from_dn
from jans.pycloudlib.persistence.spanner import SpannerClient
from jans.pycloudlib.utils import as_boolean
from jans.pycloudlib.persistence.ldap import LdapClient
from jans.pycloudlib.persistence.utils import PersistenceMapper

if _t.TYPE_CHECKING:  # pragma: no cover
    # imported objects for function type hint, completion, etc.
    # these won't be executed in runtime
    from backoff.types import Details
    from jans.pycloudlib.manager import Manager


logger = logging.getLogger(__name__)


class WaitError(Exception):  # noqa: D204
    """Class to mark error while running `wait_for_*` functions."""
    pass


def get_wait_max_time() -> int:
    """Get maximum time accepted by `wait_for` function.

    Default maximum time is 300 seconds. To change the value, pass
    `CN_WAIT_MAX_TIME` environment variable.

    Returns:
        Wait maximum time (in seconds).

    Examples:
        ```py
        import os

        from jans.pycloudlib import get_manager
        from jans.pycloudlib.wait import wait_for_config

        os.environ["CN_WAIT_MAX_TIME"] = "1200"

        manager = get_manager()
        wait_for_config(manager)
        ```
    """
    default = 60 * 5
    try:
        max_time = int(os.environ.get("CN_WAIT_MAX_TIME", default))
    except ValueError:
        max_time = default
    return max(1, max_time)


def get_wait_interval() -> int:
    """Get interval time between each execution of `wait_for` function.

    Default interval time is 10 seconds. To change the value, pass
    `CN_WAIT_SLEEP_DURATION` environment variable.

    Returns:
        Wait interval (in seconds).

    Examples:
        ```py
        import os

        from jans.pycloudlib import get_manager
        from jans.pycloudlib.wait import wait_for_config

        os.environ["CN_WAIT_SLEEP_DURATION"] = "10"

        manager = get_manager()
        wait_for_config(manager)
        ```
    """
    default = 10
    try:
        interval = int(os.environ.get("CN_WAIT_SLEEP_DURATION", default))
    except ValueError:
        interval = default
    return max(1, interval)


def on_backoff(details: Details) -> None:
    """Emit logs automatically when error is thrown while running a backoff-decorated function."""
    error = sys.exc_info()[1]
    label = details["kwargs"].pop("label", "Service")
    logger.warning(f"{label} is not ready; reason={error}; retrying in {details['wait']:0.1f} seconds")


def on_success(details: Details) -> None:
    """Emit logs automatically when there's no error while running a backoff-decorated function."""
    label = details["kwargs"].pop("label", "Service")
    logger.info(f"{label} is ready")


def on_giveup(details: Details) -> None:
    """Emit logs automatically when a backoff-decorated function exceeds allowed retries."""
    label = details["kwargs"].pop("label", "Service")
    logger.error(f"{label} is not ready after {details['elapsed']:0.1f}")


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
"""Pre-configured alias of `backoff.on_exception` decorator.

This decorator implies following setup:

- each retry is executed with constant time
- catch all `Exception`
"""


@retry_on_exception
def wait_for_config(manager: Manager, **kwargs: _t.Any) -> None:
    """Wait for readiness/availability of config backend.

    Args:
        manager: An instance of manager class.
        **kwargs: Arbitrary keyword arguments (see Other Parameters section, if any).

    Keyword Arguments:
        conn_only (bool): Determine whether to check for connection only.
            If set to `True`, this function only checks its connection status.
            If set to `False` or omitted, this function will check config entry.
    """
    conn_only = as_boolean(kwargs.get("conn_only", False))
    hostname = manager.config.get("hostname")

    if not conn_only and not hostname:
        raise WaitError("Config 'hostname' is not available")


@retry_on_exception
def wait_for_secret(manager: Manager, **kwargs: _t.Any) -> None:
    """Wait for readiness/availability of secret backend.

    Args:
        manager: An instance of manager class.
        **kwargs: Arbitrary keyword arguments (see Other Parameters section, if any).

    Other Parameters:
        conn_only (bool): Determine whether to check for connection only.
            If set to `True`, this function only checks its connection status.
            If set to `False` or omitted, this function will check secret entry.
    """
    conn_only = as_boolean(kwargs.get("conn_only", False))
    ssl_cert = manager.secret.get("ssl_cert")

    if not conn_only and not ssl_cert:
        raise WaitError("Secret 'ssl_cert' is not available")


#: DN of admin group
_ADMIN_GROUP_DN = "inum=60B7,ou=groups,o=jans"


@retry_on_exception
def wait_for_ldap(manager: Manager, **kwargs: _t.Any) -> None:
    """Wait for readiness/availability of LDAP server based on existing entry.

    Args:
        manager: An instance of manager class.
        **kwargs: Arbitrary keyword arguments (see Other Parameters section, if any).
    """
    client_id = manager.config.get("role_based_client_id")
    search_mapping = {
        "default": (f"inum={client_id},ou=clients,o=jans", "(objectClass=jansClnt)"),
        "user": (_ADMIN_GROUP_DN, "(objectClass=jansGrp)"),
        "site": ("ou=cache-refresh,o=site", "(ou=cache-refresh)"),
        "cache": ("ou=cache,o=jans", "(ou=cache)"),
        "token": ("ou=tokens,o=jans", "(ou=tokens)"),
        "session": ("ou=sessions,o=jans", "(ou=sessions)"),
    }

    client = LdapClient(manager)
    try:
        # get the first data key
        key = PersistenceMapper().groups().get("ldap", [])[0]
        search_base, search_filter = search_mapping[key]
        init = bool(client.search(search_base, search_filter, attributes=["objectClass"], limit=1))
    except (IndexError, KeyError):
        init = client.is_connected()

    if not init:
        raise WaitError("LDAP is not fully initialized")


@retry_on_exception
def wait_for_ldap_conn(manager: Manager, **kwargs: _t.Any) -> None:
    """Wait for readiness/availability of LDAP server based on connection status.

    Args:
        manager: An instance of manager class.
        **kwargs: Arbitrary keyword arguments (see Other Parameters section, if any).
    """
    connected = LdapClient(manager).is_connected()
    if not connected:
        raise WaitError("LDAP is unreachable")


@retry_on_exception
def wait_for_couchbase(manager: Manager, **kwargs: _t.Any) -> None:
    """Wait for readiness/availability of Couchbase server based on existing entry.

    Args:
        manager: An instance of manager class.
        **kwargs: Arbitrary keyword arguments (see Other Parameters section, if any).
    """
    bucket_prefix = os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")
    client_id = manager.config.get("role_based_client_id")
    search_mapping = {
        "default": (id_from_dn(f"inum={client_id},ou=clients,o=jans"), f"{bucket_prefix}"),
        "user": (id_from_dn(_ADMIN_GROUP_DN), f"{bucket_prefix}_user"),
    }

    client = CouchbaseClient(manager)
    try:
        # get the first data key
        key = PersistenceMapper().groups().get("couchbase", [])[0]
        id_, bucket = search_mapping[key]
        init = client.doc_exists(bucket, id_)
    except (IndexError, KeyError):
        init = client.get_buckets().ok

    if not init:
        raise WaitError("Couchbase backend is not fully initialized")


@retry_on_exception
def wait_for_couchbase_conn(manager: Manager, **kwargs: _t.Any) -> None:
    """Wait for readiness/availability of Couchbase server based on connection status.

    Args:
        manager: An instance of manager class.
        **kwargs: Arbitrary keyword arguments (see Other Parameters section, if any).
    """
    cb_client = CouchbaseClient(manager)
    req = cb_client.get_buckets()

    if not req.ok:
        raise WaitError(f"Unable to connect to host in {cb_client.hosts} list")


@retry_on_exception
def wait_for_sql_conn(manager: Manager, **kwargs: _t.Any) -> None:
    """Wait for readiness/liveness of an SQL database connection.

    Args:
        manager: An instance of manager class.
        **kwargs: Arbitrary keyword arguments (see Other Parameters section, if any).
    """
    # checking connection
    init = SqlClient(manager).connected()
    if not init:
        raise WaitError("SQL backend is unreachable")


@retry_on_exception
def wait_for_sql(manager: Manager, **kwargs: _t.Any) -> None:
    """Wait for readiness/liveness of an SQL database.

    Args:
        manager: An instance of manager class.
        **kwargs: Arbitrary keyword arguments (see Other Parameters section, if any).
    """
    client_id = manager.config.get("role_based_client_id")
    search_mapping = {
        "default": (doc_id_from_dn(f"inum={client_id},ou=clients,o=jans"), "jansClnt"),
        "user": (doc_id_from_dn(_ADMIN_GROUP_DN), "jansGrp"),
    }

    client = SqlClient(manager)
    try:
        # get the first data key
        key = PersistenceMapper().groups().get("sql", [])[0]
        doc_id, table_name = search_mapping[key]
        init = client.row_exists(table_name, doc_id)
    except (IndexError, KeyError):
        init = client.connected()

    if not init:
        raise WaitError("SQL backend is not fully initialized")


@retry_on_exception
def wait_for_spanner_conn(manager: Manager, **kwargs: _t.Any) -> None:
    """Wait for readiness/liveness of an Spanner database connection.

    Args:
        manager: An instance of manager class.
        **kwargs: Arbitrary keyword arguments (see Other Parameters section, if any).
    """
    # checking connection
    init = SpannerClient(manager).connected()
    if not init:
        raise WaitError("Spanner backend is unreachable")


@retry_on_exception
def wait_for_spanner(manager: Manager, **kwargs: _t.Any) -> None:
    """Wait for readiness/liveness of an Spanner database.

    Args:
        manager: An instance of manager class.
        **kwargs: Arbitrary keyword arguments (see Other Parameters section, if any).
    """
    client_id = manager.config.get("role_based_client_id")
    search_mapping = {
        "default": (doc_id_from_dn(f"inum={client_id},ou=clients,o=jans"), "jansClnt"),
        "user": (doc_id_from_dn(_ADMIN_GROUP_DN), "jansGrp"),
    }

    client = SpannerClient(manager)
    try:
        # get the first data key
        key = PersistenceMapper().groups().get("spanner", [])[0]
        doc_id, table_name = search_mapping[key]
        init = client.row_exists(table_name, doc_id)
    except (IndexError, KeyError):
        init = client.connected()

    if not init:
        raise WaitError("Spanner backend is not fully initialized")


WaitCallback = _t.TypedDict("WaitCallback", {
    "func": _t.Callable[..., None],
    "kwargs": dict[str, _t.Any],
})


def wait_for(manager: Manager, deps: _t.Union[list[str], None] = None) -> None:
    """Dispatch appropriate `wait_for_*` functions (if any).

    The following dependencies are supported:

    - `config`
    - `config_conn`
    - `ldap`
    - `ldap_conn`
    - `couchbase`
    - `couchbase_conn`
    - `secret`
    - `secret_conn`
    - `sql`
    - `sql_conn`
    - `spanner`
    - `spanner_conn`

    Args:
        manager: An instance of manager class.
        deps: An iterable of dependencies to check.

    Examples:
        ```py
        from jans.pycloudlib import get_manager
        from jans.pycloudlib.wait import wait_for

        manager = get_manager()
        deps = ["config", "secret", "ldap"]
        wait_for(manager, deps)
        ```
    """
    callbacks: dict[str, WaitCallback] = {
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
        "sql_conn": {"func": wait_for_sql_conn, "kwargs": {"label": "SQL"}},
        "sql": {"func": wait_for_sql, "kwargs": {"label": "SQL"}},
        "spanner_conn": {"func": wait_for_spanner_conn, "kwargs": {"label": "Spanner"}},
        "spanner": {"func": wait_for_spanner, "kwargs": {"label": "Spanner"}},
    }

    dependencies = deps or []
    for dep in dependencies:
        callback = callbacks.get(dep)
        if not callback:
            logger.warning(f"Unsupported callback for {dep} dependency")
            continue
        callback["func"](manager, **callback["kwargs"])


def wait_for_persistence(manager: Manager) -> None:
    """Wait for defined persistence(s).

    Args:
        manager: An instance of manager class.
    """
    mapper = PersistenceMapper()
    # cast `dict_keys` to `list`
    deps = list(mapper.groups().keys())
    wait_for(manager, deps)


def wait_for_persistence_conn(manager: Manager) -> None:
    """Wait for defined persistence(s) connection.

    Args:
        manager: An instance of manager class.
    """
    mapper = PersistenceMapper()
    deps = [f"{type_}_conn" for type_ in mapper.groups().keys()]
    wait_for(manager, deps)
