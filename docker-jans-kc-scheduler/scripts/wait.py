import logging.config

from jans.pycloudlib import get_manager
from jans.pycloudlib import wait_for

from settings import LOGGING_CONFIG

logging.config.dictConfig(LOGGING_CONFIG)


def main():
    manager = get_manager()
    deps = ["config", "secret"]
    wait_for(manager, deps)


if __name__ == "__main__":
    main()
