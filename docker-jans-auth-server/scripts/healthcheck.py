import os
import sys

import requests


def main():
    host = os.environ.get("CN_AUTH_JETTY_HOST", "0.0.0.0")  # nosec: B104
    port = os.environ.get("CN_AUTH_JETTY_PORT", "8080")
    req = requests.get(f"http://{host}:{port}/jans-auth/sys/health-check", timeout=5)
    if not req.ok:
        sys.exit(1)

    data = req.json()
    if data["status"] == "running" and data["db_status"] == "online":
        sys.exit(0)

    # any other value will be considered as unhealthy
    sys.exit(1)


if __name__ == "__main__":
    main()
