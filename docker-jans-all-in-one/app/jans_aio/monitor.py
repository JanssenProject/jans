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
        req = requests.get(f"http://{host}:{port}/sys/health-check", timeout=15)

        if req.ok:
            logger.info("Got response from jans-auth: status=%s, body=%s", req.status_code, req.json())
        else:
            logger.warning("Got unexpected response from jans-auth: status=%s, body=%s", req.status_code, req.text)
            logger.info("Checking network")
            out, err, retcode = exec_cmd("netstat -peanut")

            if retcode != 0:
                err = err or out
                logger.warning("Unable to run netstat command; reason=%s", err.decode())
            else:
                logger.info("Got network status: %s", out.decode())
    except (requests.exceptions.ConnectionError, requests.exceptions.Timeout) as exc:
        logger.warning("Got unexpected response from jans-auth; reason=%s", exc)


def main():
    while True:
        watch_jans_auth()
        time.sleep(60)


if __name__ == "__main__":
    main()
