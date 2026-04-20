# This software is available under the Apache-2.0 license.
# See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
#
# Copyright (c) 2024, Gluu, Inc.

"""Unsigned authorization -- authorize requests without JWT tokens.

Use ``authorize_unsigned`` when callers are *not* identified by JWTs
(e.g. internal services, background jobs, or test harnesses).  Instead
of tokens you supply an optional **principal** directly as an EntityData
object together with a Cedar action and resource.

Policy store
    ``example_files/policy-store-unsigned/``

    Defines three principal types -- TestPrincipal1, TestPrincipal2 and
    TestPrincipal3 -- each carrying an ``is_ok`` boolean attribute.
    A single Cedar policy permits the action ``UpdateForTestPrincipals``
    whenever ``principal.is_ok`` is true.

Run from the cedarling_python directory::

    python examples/unsigned_authz.py
"""

from __future__ import annotations

import sys
from pathlib import Path

try:
    from cedarling_python import (
        BootstrapConfig,
        Cedarling,
        EntityData,
        RequestUnsigned,
        authorize_errors,
    )
except ImportError:
    sys.exit(
        "cedarling_python is not installed. "
        "Run `maturin develop` in the cedarling_python directory first."
    )

EXAMPLE_FILES = Path(__file__).resolve().parent.parent / "example_files"


def main():
    # -- 1. Initialize Cedarling with the unsigned policy store ---------------
    config = BootstrapConfig({
        "CEDARLING_APPLICATION_NAME": "ExampleApp",
        "CEDARLING_POLICY_STORE_LOCAL_FN": str(
            EXAMPLE_FILES / "policy-store-unsigned"
        ),
        "CEDARLING_USER_AUTHZ": "enabled",
        "CEDARLING_WORKLOAD_AUTHZ": "enabled",
        "CEDARLING_JWT_SIG_VALIDATION": "disabled",
        "CEDARLING_JWT_STATUS_VALIDATION": "disabled",
        "CEDARLING_LOG_TYPE": "std_out",
        "CEDARLING_LOG_LEVEL": "INFO",
        "CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED": ["HS256"],
    })
    instance = Cedarling(config)

    # -- 2. Define the resource being accessed --------------------------------
    resource = EntityData.from_dict({
        "cedar_entity_mapping": {
            "entity_type": "Jans::Issue",
            "id": "issue-123",
        },
        "org_id": "some_long_id",
        "country": "US",
    })

    # -- 3. Define the principal (who is requesting access) -------------------
    # is_ok=True means the policy will ALLOW this principal.
    principal = EntityData.from_dict({
        "cedar_entity_mapping": {
            "entity_type": "Jans::TestPrincipal1",
            "id": "p1",
        },
        "is_ok": True,
    })

    # -- 4. Build and execute the unsigned authorization request ---------------
    request = RequestUnsigned(
        action='Jans::Action::"UpdateForTestPrincipals"',
        resource=resource,
        context={},
        principal=principal,
    )

    try:
        result = instance.authorize_unsigned(request)
    except authorize_errors.AuthorizeError as e:
        print(f"Authorization error: {e}")
        return

    # -- 5. Inspect the result ------------------------------------------------
    print(f"Allowed:    {result.is_allowed()}")
    print(f"Request ID: {result.request_id()}")

    resp = result.response
    print(f"Decision:   {resp.decision.value}")

    # Diagnostics: which Cedar policies contributed to this decision
    print("\nMatched policies:")
    for policy_id in resp.diagnostics.reason:
        print(f"  {policy_id}")

    if resp.diagnostics.errors:
        print("\nEvaluation errors:")
        for err in resp.diagnostics.errors:
            print(f"  policy {err.id}: {err.error}")


if __name__ == "__main__":
    main()
