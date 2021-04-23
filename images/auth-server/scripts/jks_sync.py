import base64
import logging
import logging.config
import os
import time

from jans.pycloudlib import get_manager
from jans.pycloudlib.utils import as_boolean

from settings import LOGGING_CONFIG

manager = get_manager()

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("jks_sync")


def jks_created():
    manager.secret.to_file("auth_jks_base64", "/etc/certs/auth-keys.jks", decode=True, binary_mode=True)
    return True


def jwks_created():
    with open("/etc/certs/auth-server-keys.json", "w") as f:
        f.write(base64.b64decode(
            manager.secret.get("auth_openid_key_base64")
        ).decode())
    return True


def should_sync_jks():
    last_rotation = manager.config.get("auth_key_rotated_at")

    # keys are not rotated yet
    if not last_rotation:
        return False

    # check modification time of local JKS; we dont need to check JSON
    try:
        mtime = int(os.path.getmtime(manager.config.get("auth_openid_jks_fn")))
    except OSError:
        mtime = 0
    return mtime < int(last_rotation)


def sync_jks():
    if jks_created():
        logger.info("auth-server-keys.jks has been synchronized")
        return True
    return False


def sync_jwks():
    if jwks_created():
        logger.info("auth-server-keys.json has been synchronized")
        return True
    return False


def main():
    sync_enabled = as_boolean(
        os.environ.get("CN_SYNC_JKS_ENABLED", False)
    )
    if not sync_enabled:
        logger.warning("JKS sync is disabled")
        return

    # delay between JKS sync (in seconds)
    sync_interval = os.environ.get("CN_SYNC_JKS_INTERVAL", 30)

    try:
        sync_interval = int(sync_interval)
        # if value is lower than 1, use default
        if sync_interval < 1:
            sync_interval = 30
    except ValueError:
        sync_interval = 30

    try:
        while True:
            try:
                if should_sync_jks():
                    sync_jks()
                    sync_jwks()
            except Exception as exc:
                logger.warning(f"Got unhandled error; reason={exc}")

            # sane interval
            time.sleep(sync_interval)
    except KeyboardInterrupt:
        logger.warning("Canceled by user; exiting ...")


if __name__ == "__main__":
    main()
