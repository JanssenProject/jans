#!/usr/bin/env python3
import json

# suppress post-quantum cryptography warnings emitted by google libs;
# this ensure the output of this entrypoint is purely JSON string
# @TODO: remove the filter after https://github.com/JanssenProject/jans/issues/15059 is resolved
import warnings
warnings.filterwarnings("ignore", category=FutureWarning, module="google")

import requests  # noqa: E402

from jans.pycloudlib import get_manager  # noqa: E402


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
