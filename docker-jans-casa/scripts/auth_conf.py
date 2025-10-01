import os

from jans.pycloudlib import get_manager
from jans.pycloudlib.utils import as_boolean

import logging.config
from settings import LOGGING_CONFIG

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("jans-casa")


manager = get_manager()


def pull_auth_conf():
    conf_files = (
        "otp_configuration.json",
        "super_gluu_creds.json",
    )
    for conf_file in conf_files:
        file_ = f"/etc/certs/{conf_file}"
        secret_name = os.path.splitext(conf_file)[0]
        logger.info(f"Pulling {file_} from secrets")
        manager.secret.to_file(secret_name, file_)


if __name__ == "__main__":
    if as_boolean(os.environ.get("CN_SHARE_AUTH_CONF", "false")):
        pull_auth_conf()
