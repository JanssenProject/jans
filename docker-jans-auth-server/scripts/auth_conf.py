import os

from jans.pycloudlib import get_manager

import logging.config
from settings import LOGGING_CONFIG

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("entrypoint")


manager = get_manager()


def push_auth_conf():
    conf_files = (
        "otp_configuration.json",
        "super_gluu_creds.json",
    )
    for conf_file in conf_files:
        file_ = f"/etc/certs/{conf_file}"
        secret_name = os.path.splitext(conf_file)[0]
        logger.info(f"Pushing {file_} to secrets")
        manager.secret.from_file(secret_name, file_)


if __name__ == "__main__":
    push_auth_conf()
