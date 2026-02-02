import os
import sys
import logging
import time

from jans.pycloudlib import get_manager
from jans.pycloudlib.wait import wait_for_config
from jans.pycloudlib.wait import wait_for_secret
from jans.pycloudlib.wait import wait_for_persistence

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger("shibboleth-wait")


def main():
    manager = get_manager()
    
    max_wait_time = int(os.environ.get("CN_WAIT_MAX_TIME", 300))
    sleep_duration = int(os.environ.get("CN_WAIT_SLEEP_DURATION", 10))
    
    deps = [
        "config",
        "secret",
        "persistence",
    ]
    
    start_time = time.time()
    
    for dep in deps:
        elapsed_time = time.time() - start_time
        remaining_time = max(0, max_wait_time - elapsed_time)
        
        if remaining_time <= 0:
            logger.error("Timeout waiting for dependencies")
            sys.exit(1)
        
        logger.info(f"Waiting for {dep} to be ready (max {remaining_time:.0f}s remaining)")
        
        if dep == "config":
            wait_for_config(manager, remaining_time)
        elif dep == "secret":
            wait_for_secret(manager, remaining_time)
        elif dep == "persistence":
            wait_for_persistence(manager, remaining_time)
    
    logger.info("All dependencies are ready")


if __name__ == "__main__":
    main()
