#!/usr/bin/env python3
# This software is available under the Apache-2.0 license.
# See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
#
# Copyright (c) 2025, Gluu, Inc.

"""Python binding micro-benchmarks for Cedarling authorization paths.

Outputs one line per benchmark:
name=<op> mean_us=<f> stddev_us=<f> min_us=<f> max_us=<f>
"""

from __future__ import annotations

import statistics
import sys
import time
from pathlib import Path

from cedarling_python import (
    AuthorizeMultiIssuerRequest,
    BootstrapConfig,
    Cedarling,
    EntityData,
    RequestUnsigned,
    TokenInput,
    authorize_errors,
)

WARMUP_ITERS = 100
MEASURE_ITERS = 1000

TEST_FILES = Path(__file__).resolve().parents[3] / "test_files"

RESOURCE = EntityData.from_dict(
    {
        "cedar_entity_mapping": {"entity_type": "Jans::Issue", "id": "random_id"},
        "org_id": "some_long_id",
        "country": "US",
    }
)

UNSIGNED_PRINCIPAL = EntityData.from_dict(
    {
        "cedar_entity_mapping": {"entity_type": "Jans::TestPrincipal1", "id": "test_principal_1"},
        "is_ok": True,
    }
)

JWT_HEADER = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
ACCESS_TOKEN = (
    JWT_HEADER
    + "."
    "eyJzdWIiOiJib0c4ZGZjNU1LVG4zN283Z3NkQ2V5cUw4THBXUXRnb080MW0xS1p3ZHEwIiwiY29kZSI6ImJmMTkzNGY2LTM5MDUtNDIwYS04Mjk5LTZiMmUzZmZkZGQ2ZSIsImlzcyI6Imh0dHBzOi8vdGVzdC5qYW5zLm9yZyIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJjbGllbnRfaWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhdWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhY3IiOiJiYXNpYyIsIng1dCNTMjU2IjoiIiwic2NvcGUiOlsib3BlbmlkIiwicHJvZmlsZSJdLCJvcmdfaWQiOiJzb21lX2xvbmdfaWQiLCJhdXRoX3RpbWUiOjE3MjQ4MzA3NDYsImV4cCI6MTcyNDk0NTk3OCwiaWF0IjoxNzI0ODMyMjU5LCJqdGkiOiJseFRtQ1ZSRlR4T2pKZ3ZFRXBvek1RIiwibmFtZSI6IkRlZmF1bHQgQWRtaW4gVXNlciIsInN0YXR1cyI6eyJzdGF0dXNfbGlzdCI6eyJpZHgiOjIwMSwidXJpIjoiaHR0cHM6Ly90ZXN0LmphbnMub3JnL2phbnMtYXV0aC9yZXN0djEvc3RhdHVzX2xpc3QifX19"
    + ".7n4vE60lisFLnEFhVwYMOPh5loyLLtPc07sCvaFI-Ik"
)
ID_TOKEN = (
    JWT_HEADER
    + "."
    "eyJhY3IiOiJiYXNpYyIsImFtciI6IjEwIiwiYXVkIjpbIjViNDQ4N2M0LThkYjEtNDA5ZC1hNjUzLWY5MDdiODA5NDAzOSJdLCJleHAiOjE3MjQ4MzU4NTksImlhdCI6MTcyNDgzMjI1OSwic3ViIjoiYm9HOGRmYzVNS1RuMzdvN2dzZENleXFMOExwV1F0Z29PNDFtMUtad2RxMCIsImlzcyI6Imh0dHBzOi8vdGVzdC5qYW5zLm9yZyIsImp0aSI6InNrM1Q0ME5ZU1l1azVzYUhaTnBrWnciLCJub25jZSI6ImMzODcyYWY5LWEwZjUtNGMzZi1hMWFmLWY5ZDBlODg0NmU4MSIsInNpZCI6IjZhN2ZlNTBhLWQ4MTAtNDU0ZC1iZTVkLTU0OWQyOTU5NWEwOSIsImphbnNPcGVuSURDb25uZWN0VmVyc2lvbiI6Im9wZW5pZGNvbm5lY3QtMS4wIiwiY19oYXNoIjoicEdvSzZZX1JLY1dIa1VlY005dXc2USIsImF1dGhfdGltZSI6MTcyNDgzMDc0NiwiZ3JhbnQiOiJhdXRob3JpemF0aW9uX2NvZGUiLCJzdGF0dXMiOnsic3RhdHVzX2xpc3QiOnsiaWR4IjoyMDIsInVyaSI6Imh0dHBzOi8vdGVzdC5qYW5zLm9yZy9qYW5zLWF1dGgvcmVzdHYxL3N0YXR1c19saXN0In19LCJyb2xlIjoiQWRtaW4ifQ"
    + ".RgCuWFUUjPVXmbW3ExQavJZH8Lw4q3kGhMFBRR0hSjA"
)
USERINFO_TOKEN = (
    JWT_HEADER
    + "."
    "eyJjb3VudHJ5IjoiVVMiLCJlbWFpbCI6InVzZXJAZXhhbXBsZS5jb20iLCJ1c2VybmFtZSI6IlVzZXJOYW1lRXhhbXBsZSIsInN1YiI6ImJvRzhkZmM1TUtUbjM3bzdnc2RDZXlxTDhMcFdRdGdvTzQxbTFLWndkcTAiLCJpc3MiOiJodHRwczovL3Rlc3QuamFucy5vcmciLCJnaXZlbl9uYW1lIjoiQWRtaW4iLCJtaWRkbGVfbmFtZSI6IkFkbWluIiwiaW51bSI6IjhkMWNkZTZhLTE0NDctNDc2Ni1iM2M4LTE2NjYzZTEzYjQ1OCIsImF1ZCI6IjViNDQ4N2M0LThkYjEtNDA5ZC1hNjUzLWY5MDdiODA5NDAzOSIsInVwZGF0ZWRfYXQiOjE3MjQ3Nzg1OTEsIm5hbWUiOiJEZWZhdWx0IEFkbWluIFVzZXIiLCJuaWNrbmFtZSI6IkFkbWluIiwiZmFtaWx5X25hbWUiOiJVc2VyIiwianRpIjoiZmFpWXZhWUlUMGNEQVQ3Rm93MHBRdyIsImphbnNBZG1pblVJUm9sZSI6WyJhcGktYWRtaW4iXSwiZXhwIjoxNzI0OTQ1OTc4fQ"
    + ".t6p8fYAe1NkUt9mn9n9MYJlNCni8JYfhk-82hb_C1O4"
)


def _unsigned_cedarling() -> Cedarling:
    config = BootstrapConfig(
        {
            "CEDARLING_APPLICATION_NAME": "BenchmarkApp",
            "CEDARLING_POLICY_STORE_LOCAL_FN": str(TEST_FILES / "policy-store_ok_2.yaml"),
            "CEDARLING_JWT_SIG_VALIDATION": "disabled",
            "CEDARLING_JWT_STATUS_VALIDATION": "disabled",
            "CEDARLING_LOG_TYPE": "off",
        }
    )
    return Cedarling(config)


def _multi_issuer_cedarling() -> Cedarling:
    config = BootstrapConfig(
        {
            "CEDARLING_APPLICATION_NAME": "TestApp",
            # Match tests/config.py + get_multi_issuer_config fixture.
            "CEDARLING_POLICY_STORE_LOCAL_FN": str(TEST_FILES / "policy-store-multi-issuer-test.yaml"),
            "CEDARLING_JWT_SIG_VALIDATION": "disabled",
            "CEDARLING_JWT_STATUS_VALIDATION": "disabled",
            "CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED": ["HS256"],
            "CEDARLING_LOG_TYPE": "off",
            "CEDARLING_LOG_LEVEL": "DEBUG",
        }
    )
    return Cedarling(config)


def _run_bench(name: str, fn) -> None:
    for _ in range(WARMUP_ITERS):
        fn()

    samples_us: list[float] = []
    for _ in range(MEASURE_ITERS):
        t0 = time.perf_counter_ns()
        fn()
        t1 = time.perf_counter_ns()
        samples_us.append((t1 - t0) / 1000.0)

    mean_us = statistics.fmean(samples_us)
    stddev_us = statistics.pstdev(samples_us, mu=mean_us)
    min_us = min(samples_us)
    max_us = max(samples_us)
    print(
        f"name={name} mean_us={mean_us:.1f} stddev_us={stddev_us:.1f} "
        f"min_us={min_us:.1f} max_us={max_us:.1f}"
    )


def bench_authorize_unsigned() -> None:
    cedarling = _unsigned_cedarling()
    request = RequestUnsigned(
        principal=UNSIGNED_PRINCIPAL,
        action='Jans::Action::"UpdateForTestPrincipals"',
        context={},
        resource=RESOURCE,
    )

    try:
        validation = cedarling.authorize_unsigned(request)
    except authorize_errors.AuthorizeError as exc:
        raise RuntimeError(f"Validation call for authorize_unsigned failed: {exc}") from exc
    if not validation.is_allowed():
        raise RuntimeError("Validation call for authorize_unsigned returned deny")

    _run_bench("authorize_unsigned", lambda: cedarling.authorize_unsigned(request))


def bench_authorize_multi_issuer() -> None:
    cedarling = _multi_issuer_cedarling()
    request = AuthorizeMultiIssuerRequest(
        tokens=[
            TokenInput(mapping="Jans::Access_Token", payload=ACCESS_TOKEN),
            TokenInput(mapping="Jans::Id_Token", payload=ID_TOKEN),
            TokenInput(mapping="Jans::Userinfo_Token", payload=USERINFO_TOKEN),
        ],
        action='Jans::Action::"Update"',
        context={},
        resource=RESOURCE,
    )

    try:
        validation = cedarling.authorize_multi_issuer(request)
    except authorize_errors.AuthorizeError as exc:
        raise RuntimeError(f"Validation call for authorize_multi_issuer failed: {exc}") from exc
    if not validation.is_allowed():
        raise RuntimeError("Validation call for authorize_multi_issuer returned deny")

    _run_bench("authorize_multi_issuer", lambda: cedarling.authorize_multi_issuer(request))


def main() -> int:
    try:
        bench_authorize_unsigned()
        bench_authorize_multi_issuer()
        return 0
    except Exception as exc:  # noqa: BLE001
        print(f"benchmark failed: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
