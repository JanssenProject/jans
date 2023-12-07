import os

from jans.pycloudlib import get_manager

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

    with manager.lock.create_lock("persistence-loader-init"):
        backend = backend_cls(manager)
        backend.initialize()

    # run upgrade if needed
    with manager.lock.create_lock("persistence-loader-upgrade"):
        upgrade = Upgrade(manager)
        upgrade.invoke()


if __name__ == "__main__":
    main()
