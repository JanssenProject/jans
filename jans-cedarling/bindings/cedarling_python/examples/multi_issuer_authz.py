# This software is available under the Apache-2.0 license.
# See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
#
# Copyright (c) 2024, Gluu, Inc.

"""Multi-issuer authorization -- authorize with JWT tokens from one or more issuers.

Use ``authorize_multi_issuer`` when the caller presents JWT tokens.
Each token is wrapped in a ``TokenInput`` that maps it to the Cedar
entity type declared in the policy store's ``token_metadata``.

Policy store
    ``example_files/policy-store-multi-issuer/``

    Declares a single trusted issuer **JansTestIssuer** with three token
    types -- ``access_token``, ``id_token``, and ``userinfo_token``.
    Two Cedar policies check token tags:

    * One permits ``Update`` when the access token's ``org_id`` tag
      contains the resource's ``org_id``.
    * The other permits ``Update`` when the userinfo token's ``country``
      tag contains the resource's ``country``.

Run from the cedarling_python directory::

    python examples/multi_issuer_authz.py
"""

from __future__ import annotations

import sys
from pathlib import Path

try:
    from cedarling_python import (
        AuthorizeMultiIssuerRequest,
        BootstrapConfig,
        Cedarling,
        EntityData,
        TokenInput,
        authorize_errors,
    )
except ImportError:
    sys.exit(
        "cedarling_python is not installed. "
        "Run `maturin develop` in the cedarling_python directory first."
    )

EXAMPLE_FILES = Path(__file__).resolve().parent.parent / "example_files"

# ---------------------------------------------------------------------------
# Sample JWTs (HS256-signed, signature validation disabled for this example)
# ---------------------------------------------------------------------------

# Access token payload:
#   sub: "boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0"
#   iss: "https://test.jans.org"
#   client_id / aud: "5b4487c4-8db1-409d-a653-f907b8094039"
#   org_id: "some_long_id"
ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJib0c4ZGZjNU1LVG4zN283Z3NkQ2V5cUw4THBXUXRnb080MW0xS1p3ZHEwIiwiY29kZSI6ImJmMTkzNGY2LTM5MDUtNDIwYS04Mjk5LTZiMmUzZmZkZGQ2ZSIsImlzcyI6Imh0dHBzOi8vdGVzdC5qYW5zLm9yZyIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJjbGllbnRfaWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhdWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhY3IiOiJiYXNpYyIsIng1dCNTMjU2IjoiIiwic2NvcGUiOlsib3BlbmlkIiwicHJvZmlsZSJdLCJvcmdfaWQiOiJzb21lX2xvbmdfaWQiLCJhdXRoX3RpbWUiOjE3MjQ4MzA3NDYsImV4cCI6MTcyNDk0NTk3OCwiaWF0IjoxNzI0ODMyMjU5LCJqdGkiOiJseFRtQ1ZSRlR4T2pKZ3ZFRXBvek1RIiwibmFtZSI6IkRlZmF1bHQgQWRtaW4gVXNlciIsInN0YXR1cyI6eyJzdGF0dXNfbGlzdCI6eyJpZHgiOjIwMSwidXJpIjoiaHR0cHM6Ly90ZXN0LmphbnMub3JnL2phbnMtYXV0aC9yZXN0djEvc3RhdHVzX2xpc3QifX19.7n4vE60lisFLnEFhVwYMOPh5loyLLtPc07sCvaFI-Ik"  # noqa: E501 S105  # gitleaks:allow

# ID token payload:
#   sub: "boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0"
#   iss: "https://test.jans.org"
#   role: "Admin"
ID_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhY3IiOiJiYXNpYyIsImFtciI6IjEwIiwiYXVkIjpbIjViNDQ4N2M0LThkYjEtNDA5ZC1hNjUzLWY5MDdiODA5NDAzOSJdLCJleHAiOjE3MjQ4MzU4NTksImlhdCI6MTcyNDgzMjI1OSwic3ViIjoiYm9HOGRmYzVNS1RuMzdvN2dzZENleXFMOExwV1F0Z29PNDFtMUtad2RxMCIsImlzcyI6Imh0dHBzOi8vdGVzdC5qYW5zLm9yZyIsImp0aSI6InNrM1Q0ME5ZU1l1azVzYUhaTnBrWnciLCJub25jZSI6ImMzODcyYWY5LWEwZjUtNGMzZi1hMWFmLWY5ZDBlODg0NmU4MSIsInNpZCI6IjZhN2ZlNTBhLWQ4MTAtNDU0ZC1iZTVkLTU0OWQyOTU5NWEwOSIsImphbnNPcGVuSURDb25uZWN0VmVyc2lvbiI6Im9wZW5pZGNvbm5lY3QtMS4wIiwiY19oYXNoIjoicEdvSzZZX1JLY1dIa1VlY005dXc2USIsImF1dGhfdGltZSI6MTcyNDgzMDc0NiwiZ3JhbnQiOiJhdXRob3JpemF0aW9uX2NvZGUiLCJzdGF0dXMiOnsic3RhdHVzX2xpc3QiOnsiaWR4IjoyMDIsInVyaSI6Imh0dHBzOi8vdGVzdC5qYW5zLm9yZy9qYW5zLWF1dGgvcmVzdHYxL3N0YXR1c19saXN0In19LCJyb2xlIjoiQWRtaW4ifQ.RgCuWFUUjPVXmbW3ExQavJZH8Lw4q3kGhMFBRR0hSjA"  # noqa: E501 S105  # gitleaks:allow

# Userinfo token payload:
#   sub: "boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0"
#   iss: "https://test.jans.org"
#   country: "US", email: "user@example.com"
USERINFO_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb3VudHJ5IjoiVVMiLCJlbWFpbCI6InVzZXJAZXhhbXBsZS5jb20iLCJ1c2VybmFtZSI6IlVzZXJOYW1lRXhhbXBsZSIsInN1YiI6ImJvRzhkZmM1TUtUbjM3bzdnc2RDZXlxTDhMcFdRdGdvTzQxbTFLWndkcTAiLCJpc3MiOiJodHRwczovL3Rlc3QuamFucy5vcmciLCJnaXZlbl9uYW1lIjoiQWRtaW4iLCJtaWRkbGVfbmFtZSI6IkFkbWluIiwiaW51bSI6IjhkMWNkZTZhLTE0NDctNDc2Ni1iM2M4LTE2NjYzZTEzYjQ1OCIsImF1ZCI6IjViNDQ4N2M0LThkYjEtNDA5ZC1hNjUzLWY5MDdiODA5NDAzOSIsInVwZGF0ZWRfYXQiOjE3MjQ3Nzg1OTEsIm5hbWUiOiJEZWZhdWx0IEFkbWluIFVzZXIiLCJuaWNrbmFtZSI6IkFkbWluIiwiZmFtaWx5X25hbWUiOiJVc2VyIiwianRpIjoiZmFpWXZhWUlUMGNEQVQ3Rm93MHBRdyIsImphbnNBZG1pblVJUm9sZSI6WyJhcGktYWRtaW4iXSwiZXhwIjoxNzI0OTQ1OTc4fQ.t6p8fYAe1NkUt9mn9n9MYJlNCni8JYfhk-82hb_C1O4"  # noqa: E501 S105  # gitleaks:allow


def main():
    # -- 1. Initialize Cedarling with the multi-issuer policy store -----------
    config = BootstrapConfig({
        "CEDARLING_APPLICATION_NAME": "ExampleApp",
        "CEDARLING_POLICY_STORE_LOCAL_FN": str(
            EXAMPLE_FILES / "policy-store-multi-issuer"
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
    # The Cedar policies match on resource.org_id and resource.country,
    # which must correspond to values carried in the JWT tokens above.
    resource = EntityData.from_dict({
        "cedar_entity_mapping": {
            "entity_type": "Jans::Issue",
            "id": "issue-456",
        },
        "org_id": "some_long_id",
        "country": "US",
    })

    # -- 3. Wrap each JWT in a TokenInput -------------------------------------
    # The ``mapping`` string must match an ``entity_type_name`` declared
    # in the policy store's trusted_issuers -> token_metadata section.
    tokens = [
        TokenInput(mapping="Jans::Access_Token", payload=ACCESS_TOKEN),
        TokenInput(mapping="Jans::Id_Token", payload=ID_TOKEN),
        TokenInput(mapping="Jans::Userinfo_Token", payload=USERINFO_TOKEN),
    ]

    # -- 4. Build and execute the multi-issuer request ------------------------
    request = AuthorizeMultiIssuerRequest(
        tokens=tokens,
        action='Jans::Action::"Update"',
        resource=resource,
        context={},
    )

    try:
        result = instance.authorize_multi_issuer(request)
    except authorize_errors.AuthorizeError as e:
        print(f"Authorization error: {e}")
        return

    # -- 5. Inspect the result ------------------------------------------------
    print(f"Allowed:    {result.is_allowed()}")
    print(f"Request ID: {result.request_id()}")

    resp = result.response()
    print(f"Decision:   {resp.decision.value}")

    # Diagnostics: which policies matched
    print("\nMatched policies:")
    for policy_id in resp.diagnostics.reason:
        print(f"  {policy_id}")

    if resp.diagnostics.errors:
        print("\nEvaluation errors:")
        for err in resp.diagnostics.errors:
            print(f"  policy {err.id}: {err.error}")


if __name__ == "__main__":
    main()
