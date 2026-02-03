import os
import sys
import time
import logging
import requests

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger("shibboleth-healthcheck")

DEFAULT_PORT = 8080
DEFAULT_INTERVAL = 30


def check_health():
    try:
        port = int(os.environ.get("CN_SHIBBOLETH_PORT", DEFAULT_PORT))
    except (ValueError, TypeError):
        logger.warning("Invalid CN_SHIBBOLETH_PORT, using default %d", DEFAULT_PORT)
        port = DEFAULT_PORT

    health_url = f"http://localhost:{port}/idp/status"

    try:
        response = requests.get(health_url, timeout=10)
        if response.status_code == 200:
            return True
    except requests.RequestException as e:
        logger.debug("Health check failed: %s", e)

    return False


def main():
    try:
        interval = int(os.environ.get("CN_HEALTH_CHECK_INTERVAL", DEFAULT_INTERVAL))
        if interval <= 0:
            raise ValueError("Interval must be positive")
    except (ValueError, TypeError):
        logger.warning("Invalid CN_HEALTH_CHECK_INTERVAL, using default %d", DEFAULT_INTERVAL)
        interval = DEFAULT_INTERVAL

    logger.info("Starting health check loop with %ds interval", interval)

    while True:
        time.sleep(interval)

        if check_health():
            logger.debug("Health check passed")
        else:
            logger.warning("Health check failed")


if __name__ == "__main__":
    main()
