import os
import logging.config
import time

import requests
from jans.pycloudlib.utils import exec_cmd

from jans_aio.settings import LOGGING_CONFIG


logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("jans_aio")


def watch_jans_auth():
    logger.info("Checking jans-auth status")

    try:
        host = os.environ.get("CN_AUTH_JETTY_HOST", "127.0.0.1")  # nosec: B104
        port = os.environ.get("CN_AUTH_JETTY_PORT", "8081")
        req = requests.get(f"http://{host}:{port}/jans-auth/sys/health-check")

        if req.ok:
            logger.info(f"Got response from jans-auth: status={req.status_code}, body={req.json()}")
        else:
            logger.warning(f"Got unexpected response from jans-auth: status={req.status_code}, body={req.text}")
            logger.info("Checking network")
            out, err, retcode = exec_cmd("netstat -peanut")

            if retcode != 0:
                err = err or out
                logger.warning(f"Unable to run netstat command; reason={err.decode()}")
            else:
                logger.info(f"Got network status: {out.decode()}")
    except requests.exceptions.ConnectionError as exc:
        logger.warning(f"Got unexpected response from jans-auth; reason={exc}")


def main():
    while True:
        watch_jans_auth()
        time.sleep(30)


if __name__ == "__main__":
    main()
