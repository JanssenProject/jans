import os
import sys

import requests


def run_healthcheck():
    host = os.environ.get("CN_SAML_HTTP_HOST", "0.0.0.0")  # nosec: B104
    port = os.environ.get("CN_SAML_HTTP_PORT", "8083")
    req = requests.get(f"http://{host}:{port}/kc/health", timeout=5)
    if not req.ok:
        return False

    data = req.json()
    if data["status"] == "UP":
        return True

    # any other value will be considered as unhealthy
    return False


def main():
    if run_healthcheck():
        sys.exit(0)
    sys.exit(1)


if __name__ == "__main__":
    main()
