import os

from jans.pycloudlib import get_manager
from jans.pycloudlib import wait_for_persistence_conn
from jans.pycloudlib.persistence.couchbase import sync_couchbase_password
from jans.pycloudlib.persistence.couchbase import sync_couchbase_superuser_password
from jans.pycloudlib.persistence.ldap import sync_ldap_password
from jans.pycloudlib.persistence.sql import sync_sql_password
from jans.pycloudlib.persistence.utils import PersistenceMapper

from hybrid_setup import HybridBackend
from ldap_setup import LDAPBackend
from couchbase_setup import CouchbaseBackend
from sql_setup import SQLBackend
from spanner_setup import SpannerBackend
from upgrade import Upgrade


def main():
    manager = get_manager()

    backend_classes = {
        "ldap": LDAPBackend,
        "couchbase": CouchbaseBackend,
        "hybrid": HybridBackend,
        "sql": SQLBackend,
        "spanner": SpannerBackend,
    }

    # initialize the backend
    persistence_type = os.environ.get("CN_PERSISTENCE_TYPE", "ldap")
    backend_cls = backend_classes.get(persistence_type)
    if not backend_cls:
        raise ValueError("Unsupported persistence backend")

    persistence_groups = PersistenceMapper().groups().keys()

    if "ldap" in persistence_groups:
        sync_ldap_password(manager)

    if "sql" in persistence_groups:
        sync_sql_password(manager)

    if "couchbase" in persistence_groups:
        # superuser is required to create buckets, etc.
        sync_couchbase_superuser_password(manager)
        sync_couchbase_password(manager)

    wait_for_persistence_conn(manager)

    with manager.lock.create_lock("persistence-loader-init"):
        backend = backend_cls(manager)
        backend.initialize()

    # run upgrade if needed
    with manager.lock.create_lock("persistence-loader-upgrade"):
        upgrade = Upgrade(manager)
        upgrade.invoke()


if __name__ == "__main__":
    main()
