import sys

import requests


def main():
    req = requests.get("http://0.0.0.0:8080/jans-auth/sys/health-check")
    if not req.ok:
        sys.exit(1)

    data = req.json()
    if data["status"] == "running" and data["db_status"] == "online":
        sys.exit(0)

    # any other value will be considered as unhealthy
    sys.exit(1)


if __name__ == "__main__":
    main()
