import logging
import logging.config
import os

from jans.pycloudlib import get_manager
from jans.pycloudlib import wait_for
from jans.pycloudlib.validators import validate_persistence_type
from jans.pycloudlib.validators import validate_persistence_ldap_mapping

from settings import LOGGING_CONFIG

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("wait")


def main():
    persistence_type = os.environ.get("CN_PERSISTENCE_TYPE", "ldap")
    validate_persistence_type(persistence_type)

    ldap_mapping = os.environ.get("CN_PERSISTENCE_LDAP_MAPPING", "default")
    validate_persistence_ldap_mapping(persistence_type, ldap_mapping)

    manager = get_manager()
    deps = ["config", "secret"]

    if persistence_type == "hybrid":
        deps += ["ldap_conn", "couchbase_conn"]
    else:
        deps.append("{}_conn".format(persistence_type))

    wait_for(manager, deps)


if __name__ == "__main__":
    main()
