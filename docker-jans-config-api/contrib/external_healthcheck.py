#!/usr/bin/env python3
import json

import requests

from jans.pycloudlib import get_manager


def poll_healthchecks(manager):
    import urllib3

    hostname = manager.config.get("hostname")
    statuses = {}

    for comp, endpoint, status_code_only in [
        ("jans-auth", "/jans-auth/sys/health-check", False),
        ("jans-lock", "/jans-auth/sys/health-check", False),
        ("jans-config-api", "/jans-config-api/api/v1/health/live", False),
        ("jans-casa", "/jans-casa/health-check", False),
        ("jans-fido2", "/jans-fido2/sys/health-check", False),
        ("jans-scim", "/jans-scim/sys/health-check", False),
        ("jans-link", "/jans-link/sys/health-check", False),
        # healthcheck endpoint for KC is bind to internal port 9000,
        # hence we poll the public endpoint instead
        ("keycloak", "/kc", True),
    ]:
        # default component status
        status = "Down"

        scheme = "https"
        verify = False

        if scheme == "https" and verify is False:
            urllib3.disable_warnings()

        resp = requests.get(f"{scheme}://{hostname}{endpoint}", timeout=5, verify=verify)

        if resp.ok:
            if status_code_only:
                status = "Running"
            else:
                try:
                    if comp == "jans-lock":
                        healthcheck_data = resp.json().get("jans-lock", {"status": status})
                    else:
                        healthcheck_data = resp.json()

                    if healthcheck_data["status"].lower() in ("running", "up"):
                        status = "Running"

                # response from server is not JSON
                except requests.exceptions.JSONDecodeError:
                    if resp.text.lower() == "ok":
                        status = "Running"

        # finalized statuses
        statuses[comp] = status

    return json.dumps(statuses)


if __name__ == "__main__":
    manager = get_manager()
    print(poll_healthchecks(manager))
