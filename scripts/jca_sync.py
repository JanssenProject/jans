import contextlib
import logging.config
import os
import time

from settings import LOGGING_CONFIG

from jans.pycloudlib.document import RClone

ROOT_DIR = "/repository/default"
SYNC_DIR = "/opt/jans/jetty/jans-auth/custom"

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("webdav")


def sync_from_webdav(url, username, password):
    rclone = RClone(url, username, password)
    rclone.configure()

    logger.info(f"Sync files with remote directory {url}{ROOT_DIR}{SYNC_DIR}")
    rclone.copy_from(SYNC_DIR, SYNC_DIR)


def sync_to_webdav(url, username, password):
    rclone = RClone(url, username, password)
    rclone.configure()

    files = (
        "/etc/certs/otp_configuration.json",
        "/etc/certs/super_gluu_creds.json",
    )

    for local in files:
        remote = os.path.dirname(local)
        logger.info(f"Sync file {local} to {url}{ROOT_DIR}{remote}")
        rclone.copy_to(remote, local)


def get_sync_interval():
    default = 5 * 60  # 5 minutes

    if "CN_JCA_SYNC_INTERVAL" in os.environ:
        env_name = "CN_JCA_SYNC_INTERVAL"
    else:
        env_name = "CN_JACKRABBIT_SYNC_INTERVAL"

    try:
        interval = int(os.environ.get(env_name, default))
    except ValueError:
        interval = default
    return interval


def get_jackrabbit_url():
    if "CN_JCA_URL" in os.environ:
        return os.environ["CN_JCA_URL"]
    return os.environ.get("CN_JACKRABBIT_URL", "http://localhost:8080")


def main():
    store_type = os.environ.get("CN_DOCUMENT_STORE_TYPE", "LOCAL")
    if store_type != "JCA":
        logger.warning(f"Using {store_type} document store; sync is disabled ...")
        return

    url = get_jackrabbit_url()

    username = os.environ.get("CN_JACKRABBIT_ADMIN_ID", "admin")
    password = ""

    password_file = os.environ.get(
        "CN_JACKRABBIT_ADMIN_PASSWORD_FILE",
        "/etc/jans/conf/jackrabbit_admin_password",
    )
    with contextlib.suppress(FileNotFoundError):
        with open(password_file) as f:
            password = f.read().strip()
    password = password or username

    sync_interval = get_sync_interval()
    try:
        while True:
            sync_from_webdav(url, username, password)
            sync_to_webdav(url, username, password)
            time.sleep(sync_interval)
    except KeyboardInterrupt:
        logger.warning("Canceled by user; exiting ...")


if __name__ == "__main__":
    main()
