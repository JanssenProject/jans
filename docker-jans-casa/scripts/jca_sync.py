import contextlib
import logging.config
import os
import shutil
import time
import filecmp

from webdav3.client import Client
from webdav3.exceptions import RemoteResourceNotFound
from webdav3.exceptions import NoConnection

from settings import LOGGING_CONFIG

ROOT_DIR = "/repository/default"
SYNC_DIR = "/opt/jans/jetty/jans-casa"
TMP_DIR = "/tmp/webdav"

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("jans-casa")


def sync_from_webdav(url, username, password):
    client = Client({
        "webdav_hostname": url,
        "webdav_login": username,
        "webdav_password": password,
        "webdav_root": ROOT_DIR,
    })
    client.verify = False

    try:
        logger.info(f"Sync files from {url}{ROOT_DIR}{SYNC_DIR}")
        # download files to temporary directory to avoid `/opt/gluu/jetty/casa`
        # getting deleted
        client.download(SYNC_DIR, TMP_DIR)

        # copy all downloaded files to /opt/gluu/jetty/casa
        for subdir, _, files in os.walk(TMP_DIR):
            for file_ in files:
                src = os.path.join(subdir, file_)
                dest = src.replace(TMP_DIR, SYNC_DIR)

                if not os.path.exists(os.path.dirname(dest)):
                    os.makedirs(os.path.dirname(dest))

                # if destination path exists, compare the contents with the source;
                # if both have same contents, do not copy the file
                if os.path.exists(dest) and filecmp.cmp(src, dest, shallow=False):
                    continue
                # logger.info(f"Copying {src} to {dest}")
                shutil.copyfile(src, dest)
    except (RemoteResourceNotFound, NoConnection) as exc:
        logger.warning(f"Unable to sync files from {url}{ROOT_DIR}{SYNC_DIR}; reason={exc}")

    files = (
        "/etc/certs/otp_configuration.json",
        "/etc/certs/super_gluu_creds.json",
    )

    for file_ in files:
        try:
            logger.info(f"Sync {file_} from {url}{ROOT_DIR}{file_}")
            client.download_file(file_, file_)
        except (RemoteResourceNotFound, NoConnection) as exc:
            logger.warning(f"Unable to sync {file_} from {url}{ROOT_DIR}{file_}; reason={exc}")


def get_sync_interval():
    default = 5 * 60  # 5 minutes

    env_name = "CN_JACKRABBIT_SYNC_INTERVAL"

    try:
        interval = int(os.environ.get(env_name, default))
    except ValueError:
        interval = default
    return interval


def get_jackrabbit_url():
    return os.environ.get("CN_JACKRABBIT_URL", "http://localhost:8080")


def main():
    store_type = os.environ.get("CN_DOCUMENT_STORE_TYPE", "DB")
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
            time.sleep(sync_interval)
    except KeyboardInterrupt:
        logger.warning("Canceled by user; exiting ...")


if __name__ == "__main__":
    main()
