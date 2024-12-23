import logging.config
import os
from hashlib import sha256
from pathlib import Path

from jans.pycloudlib import get_manager
from jans.pycloudlib.utils import as_boolean

from settings import LOGGING_CONFIG

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("jans-auth")


def push_auth_conf(manager) -> None:
    conf_files = (
        "otp_configuration.json",
        "super_gluu_creds.json",
    )
    for conf_file in conf_files:
        file_ = Path(f"/etc/certs/{conf_file}")

        # compare digest; if they are different, push the contents of file to secrets
        val1 = manager.secret.get(file_.stem) or ""
        val2 = file_.read_text()

        if not digest_equals(val1, val2):
            logger.info(f"Detected changes in {file_}; pushing changes to secrets.")
            manager.secret.from_file(file_.stem, str(file_))


def digest_equals(val1: str, val2: str) -> bool:
    val1_digest = sha256(val1.encode()).hexdigest()
    val2_digest = sha256(val2.encode()).hexdigest()
    return val1_digest == val2_digest


if __name__ == "__main__":
    if as_boolean(os.environ.get("CN_SHARE_AUTH_CONF", "false")):
        manager = get_manager()

        with manager.create_lock("auth-share-conf"):
            push_auth_conf(manager)
