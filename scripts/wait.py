import logging
import logging.config
import os

from pygluu.containerlib import get_manager
from pygluu.containerlib import wait_for
from pygluu.containerlib.validators import validate_persistence_type
from pygluu.containerlib.validators import validate_persistence_ldap_mapping

from settings import LOGGING_CONFIG

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("wait")


def main():
    persistence_type = os.environ.get("JANS_PERSISTENCE_TYPE", "ldap")
    validate_persistence_type(persistence_type)

    ldap_mapping = os.environ.get("JANS_PERSISTENCE_LDAP_MAPPING", "default")
    validate_persistence_ldap_mapping(persistence_type, ldap_mapping)

    manager = get_manager()
    deps = ["config", "secret"]

    if persistence_type == "hybrid":
        deps += ["ldap", "couchbase"]
    else:
        deps.append(persistence_type)

    wait_for(manager, deps)


if __name__ == "__main__":
    main()
