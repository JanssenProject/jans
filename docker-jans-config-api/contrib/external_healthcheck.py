#!/usr/bin/env python3
import json

# suppress post-quantum cryptography warnings emitted by google libs;
# this ensure the output of this entrypoint is purely JSON string
# @TODO: remove the filter after https://github.com/JanssenProject/jans/issues/15059 is resolved
import warnings
warnings.filterwarnings("ignore", category=FutureWarning, module="google")

import os  # noqa: E402
import ssl  # noqa: E402

import requests  # noqa: E402
from requests.adapters import HTTPAdapter  # noqa: E402

from jans.pycloudlib import get_manager  # noqa: E402

WEB_CERT_FILE = "/etc/certs/web_https.crt"


class _TLSAdapter(HTTPAdapter):
    def __init__(self, ssl_context, **kwargs):
        self._ssl_context = ssl_context
        super().__init__(**kwargs)

    def init_poolmanager(self, *args, **kwargs):
        kwargs["ssl_context"] = self._ssl_context
        return super().init_poolmanager(*args, **kwargs)


def _build_session():
    ctx = ssl.create_default_context()
    if os.path.isfile(WEB_CERT_FILE):
        # trust the deployment's (possibly self-signed) web certificate itself as an anchor
        ctx.load_verify_locations(cafile=WEB_CERT_FILE)
        ctx.verify_flags |= getattr(ssl, "VERIFY_X509_PARTIAL_CHAIN", 0)
    session = requests.Session()
    session.mount("https://", _TLSAdapter(ctx))
    return session


def poll_healthchecks(manager):
    hostname = manager.config.get("hostname")
    statuses = {}
    session = _build_session()

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

        try:
            resp = session.get(f"https://{hostname}{endpoint}", timeout=5)
        except requests.exceptions.RequestException:
            statuses[comp] = status
            continue

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
