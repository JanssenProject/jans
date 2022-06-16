"""This module consists of startup order utilities."""

from __future__ import annotations
from typing import TYPE_CHECKING

if TYPE_CHECKING:  # pragma: no cover
    # imported objects for function type hint, completion, etc.
    # these won't be executed in runtime
    from jans.pycloudlib.manager import _Manager

import logging
import os
import sys

import backoff

from jans.pycloudlib.persistence.couchbase import CouchbaseClient
from jans.pycloudlib.persistence.couchbase import id_from_dn
from jans.pycloudlib.persistence.sql import SqlClient
from jans.pycloudlib.persistence.sql import doc_id_from_dn
from jans.pycloudlib.persistence.spanner import SpannerClient
from jans.pycloudlib.utils import as_boolean
from jans.pycloudlib.persistence.ldap import LdapClient
from jans.pycloudlib.persistence.utils import PersistenceMapper


logger = logging.getLogger(__name__)


class WaitError(Exception):  # noqa: D204
    """Class to mark error while running ``wait_for_*`` functions."""
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
    """Emit logs automatically when error is thrown while running a backoff-decorated function."""
    details["error"] = sys.exc_info()[1]
    details["kwargs"]["label"] = details["kwargs"].pop("label", "Service")
    logger.warning(
        "{kwargs[label]} is not ready; reason={error}; "
        "retrying in {wait:0.1f} seconds".format(**details)
    )


def on_success(details: dict):
    """Emit logs automatically when there's no error while running a backoff-decorated function."""
    details["kwargs"]["label"] = details["kwargs"].pop("label", "Service")
    logger.info("{kwargs[label]} is ready".format(**details))


def on_giveup(details: dict):
    """Emit logs automatically when a backoff-decorated function exceeds allowed retries."""
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


#: DN of admin group
_ADMIN_GROUP_DN = "inum=60B7,ou=groups,o=jans"


@retry_on_exception
def wait_for_ldap(manager: _Manager, **kwargs) -> None:
    """Wait for readiness/availability of LDAP server based on existing entry.

    :param manager: An instance of :class:`~jans.pycloudlib.manager._Manager`.
    """
    jca_client_id = manager.config.get("jca_client_id")
    search_mapping = {
        "default": (f"inum={jca_client_id},ou=clients,o=jans", "(objectClass=jansClnt)"),
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
        init = client.search(search_base, search_filter, attributes=["objectClass"], limit=1)
    except (IndexError, KeyError):
        init = client.is_connected()

    if not init:
        raise WaitError("LDAP is not fully initialized")


@retry_on_exception
def wait_for_ldap_conn(manager, **kwargs):
    """Wait for readiness/availability of LDAP server based on connection status.

    :param manager: An instance of :class:`~jans.pycloudlib.manager._Manager`.
    """
    connected = LdapClient(manager).is_connected()
    if not connected:
        raise WaitError("LDAP is unreachable")


@retry_on_exception
def wait_for_couchbase(manager, **kwargs):
    """Wait for readiness/availability of Couchbase server based on existing entry.

    :param manager: An instance of :class:`~jans.pycloudlib.manager._Manager`.
    """
    bucket_prefix = os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")
    jca_client_id = manager.config.get("jca_client_id")
    search_mapping = {
        "default": (id_from_dn(f"inum={jca_client_id},ou=clients,o=jans"), f"{bucket_prefix}"),
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
def wait_for_couchbase_conn(manager, **kwargs):
    """Wait for readiness/availability of Couchbase server based on connection status.

    :param manager: An instance of :class:`~jans.pycloudlib.manager._Manager`.
    """
    cb_client = CouchbaseClient(manager)
    req = cb_client.get_buckets()

    if not req.ok:
        raise WaitError(f"Unable to connect to host in {cb_client.host} list")


@retry_on_exception
def wait_for_sql_conn(manager, **kwargs):
    """Wait for readiness/liveness of an SQL database connection."""
    # checking connection
    init = SqlClient(manager).connected()
    if not init:
        raise WaitError("SQL backend is unreachable")


@retry_on_exception
def wait_for_sql(manager: _Manager, **kwargs) -> None:
    """Wait for readiness/liveness of an SQL database."""
    jca_client_id = manager.config.get("jca_client_id")
    search_mapping = {
        "default": (doc_id_from_dn(f"inum={jca_client_id},ou=clients,o=jans"), "jansClnt"),
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
def wait_for_spanner_conn(manager, **kwargs):
    """Wait for readiness/liveness of an Spanner database connection."""
    # checking connection
    init = SpannerClient(manager).connected()
    if not init:
        raise WaitError("Spanner backend is unreachable")


@retry_on_exception
def wait_for_spanner(manager: _Manager, **kwargs) -> None:
    """Wait for readiness/liveness of an Spanner database."""
    jca_client_id = manager.config.get("jca_client_id")
    search_mapping = {
        "default": (doc_id_from_dn(f"inum={jca_client_id},ou=clients,o=jans"), "jansClnt"),
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


def wait_for(manager, deps=None):
    """Dispatch appropriate one or more ``wait_for_*`` function(s).

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
        "sql_conn": {"func": wait_for_sql_conn, "kwargs": {"label": "SQL"}},
        "sql": {"func": wait_for_sql, "kwargs": {"label": "SQL"}},
        "spanner_conn": {"func": wait_for_spanner_conn, "kwargs": {"label": "Spanner"}},
        "spanner": {"func": wait_for_spanner, "kwargs": {"label": "Spanner"}},
    }

    for dep in deps:
        callback = callbacks.get(dep)
        if not callback:
            logger.warning(f"Unsupported callback for {dep} dependency")
            continue
        callback["func"](manager, **callback["kwargs"])


def wait_for_persistence(manager: _Manager) -> None:
    """Wait for defined persistence(s).

    :param manager: An instance of :class:`~jans.pycloudlib.manager._Manager`.
    """
    mapper = PersistenceMapper()
    # cast ``dict_keys`` to ``list``
    deps = list(mapper.groups().keys())
    wait_for(manager, deps)


def wait_for_persistence_conn(manager: _Manager) -> None:
    """Wait for defined persistence(s) connection.

    :param manager: An instance of :class:`~jans.pycloudlib.manager._Manager`.
    """
    mapper = PersistenceMapper()
    deps = [f"{type_}_conn" for type_ in mapper.groups().keys()]
    wait_for(manager, deps)
