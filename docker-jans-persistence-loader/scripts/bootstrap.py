import os

from jans.pycloudlib import get_manager
from jans.pycloudlib import wait_for_persistence_conn

from hybrid_setup import HybridBackend
from sql_setup import SQLBackend
from upgrade import Upgrade


def main():
    manager = get_manager()

    backend_classes = {
        "hybrid": HybridBackend,
        "sql": SQLBackend,
    }

    # initialize the backend
    persistence_type = os.environ.get("CN_PERSISTENCE_TYPE", "sql")
    backend_cls = backend_classes.get(persistence_type)
    if not backend_cls:
        raise ValueError("Unsupported persistence backend")

    wait_for_persistence_conn(manager)

    with manager.create_lock("persistence-loader-init"):
        backend = backend_cls(manager)
        backend.initialize()

    # run upgrade if needed
    with manager.create_lock("persistence-loader-upgrade"):
        upgrade = Upgrade(manager)
        upgrade.invoke()


if __name__ == "__main__":
    main()
