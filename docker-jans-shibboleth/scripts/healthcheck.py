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


def check_health():
    port = int(os.environ.get("CN_SHIBBOLETH_PORT", 8080))
    health_url = f"http://localhost:{port}/idp/status"
    
    try:
        response = requests.get(health_url, timeout=10)
        if response.status_code == 200:
            return True
    except requests.RequestException as e:
        logger.debug(f"Health check failed: {e}")
    
    return False


def main():
    interval = int(os.environ.get("CN_HEALTH_CHECK_INTERVAL", 30))
    
    logger.info(f"Starting health check loop with {interval}s interval")
    
    while True:
        time.sleep(interval)
        
        if check_health():
            logger.debug("Health check passed")
        else:
            logger.warning("Health check failed")


if __name__ == "__main__":
    main()
