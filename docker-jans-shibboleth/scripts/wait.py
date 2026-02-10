import os
import sys
import logging
import threading
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


class TimeoutError(Exception):
    """Exception raised when a wait operation times out."""
    pass


def wait_with_hard_timeout(func, manager, timeout, dep_name):
    """Execute wait function with a hard timeout using threading.

    Uses a separate thread to run the potentially blocking wait_for_* function,
    allowing the main thread to enforce a strict timeout.
    """
    result = {"success": False, "error": None}

    def target():
        try:
            func(manager)
            result["success"] = True
        except Exception as e:
            result["error"] = e

    thread = threading.Thread(target=target, daemon=True)
    thread.start()
    thread.join(timeout=timeout)

    if thread.is_alive():
        logger.error("Hard timeout waiting for %s after %.0fs", dep_name, timeout)
        return False

    if result["error"]:
        logger.error("Error waiting for %s: %s", dep_name, result["error"])
        return False

    return result["success"]


def main():
    manager = get_manager()

    max_wait_time = int(os.environ.get("CN_WAIT_MAX_TIME", 300))

    deps = [
        ("config", wait_for_config),
        ("secret", wait_for_secret),
        ("persistence", wait_for_persistence),
    ]

    start_time = time.time()

    for dep_name, wait_func in deps:
        elapsed_time = time.time() - start_time
        remaining_time = max(0, max_wait_time - elapsed_time)

        if remaining_time <= 0:
            logger.error("Timeout waiting for dependencies (total elapsed: %.0fs)", elapsed_time)
            sys.exit(1)

        logger.info("Waiting for %s to be ready (max %.0fs remaining)", dep_name, remaining_time)

        success = wait_with_hard_timeout(wait_func, manager, remaining_time, dep_name)
        if not success:
            logger.error("Failed to wait for %s within timeout", dep_name)
            sys.exit(1)

        logger.info("Dependency %s is ready", dep_name)

    total_elapsed = time.time() - start_time
    logger.info("All dependencies are ready (total time: %.0fs)", total_elapsed)


if __name__ == "__main__":
    main()
