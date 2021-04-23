import os

from jans.pycloudlib import get_manager

from hybrid_setup import HybridBackend
from ldap_setup import LDAPBackend
from couchbase_setup import CouchbaseBackend
from sql_setup import SQLBackend


def main():
    manager = get_manager()

    backend_classes = {
        "ldap": LDAPBackend,
        "couchbase": CouchbaseBackend,
        "hybrid": HybridBackend,
        "sql": SQLBackend,
    }

    # initialize the backend
    persistence_type = os.environ.get("CN_PERSISTENCE_TYPE", "ldap")
    backend_cls = backend_classes.get(persistence_type)
    if not backend_cls:
        raise ValueError("unsupported backend")

    backend = backend_cls(manager)
    backend.initialize()


if __name__ == "__main__":
    main()
