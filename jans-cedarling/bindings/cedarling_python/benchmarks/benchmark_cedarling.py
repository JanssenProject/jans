#!/usr/bin/env python3
# This software is available under the Apache-2.0 license.
# See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
#
# Copyright (c) 2025, Gluu, Inc.

"""Python cross-platform bench harness. See bindings/benchmarks/CONTRACT.md."""

from __future__ import annotations

import json
import os
import sys
import time
from pathlib import Path
from typing import Any, Callable, Optional

from cedarling_python import (
    AuthorizeMultiIssuerRequest,
    BootstrapConfig,
    Cedarling,
    EntityData,
    RequestUnsigned,
    TokenInput,
)

BINDING_NAME = "python"


def resolve_repo_root() -> Path:
    env = os.environ.get("CEDARLING_REPO_ROOT")
    if env:
        return Path(env).resolve()
    # Default: bindings/cedarling_python/benchmarks/ → repo root is 3 levels up.
    return Path(__file__).resolve().parents[3]


def _emit(row: dict) -> None:
    print(json.dumps(row))


def _build_config(scenario: dict, repo_root: Path) -> BootstrapConfig:
    cfg = dict(scenario.get("config_overrides") or {})
    cfg["CEDARLING_POLICY_STORE_LOCAL_FN"] = str(
        repo_root / scenario["policy_store_fn"]
    )
    return BootstrapConfig(cfg)


def _build_bench_fn(cedarling: Cedarling, scenario: dict) -> Callable[[], Any]:
    kind = scenario["kind"]
    resource = EntityData.from_dict(scenario["resource"])
    # Context is JSON-as-string in the manifest; parse once outside the hot path.
    ctx_str = scenario.get("context") or "{}"
    context = json.loads(ctx_str) if ctx_str else {}
    action = scenario["action"]

    if kind == "unsigned":
        principal_dict = scenario.get("principal")
        principal = EntityData.from_dict(principal_dict) if principal_dict else None
        request = RequestUnsigned(
            principal=principal,
            action=action,
            context=context,
            resource=resource,
        )
        return lambda: cedarling.authorize_unsigned(request).is_allowed()

    if kind == "multi_issuer":
        tokens = [
            TokenInput(mapping=t["mapping"], payload=t["payload"])
            for t in scenario.get("tokens", [])
        ]
        request = AuthorizeMultiIssuerRequest(
            tokens=tokens,
            action=action,
            context=context,
            resource=resource,
        )
        return lambda: cedarling.authorize_multi_issuer(request).is_allowed()

    raise ValueError(f"unknown scenario kind: {kind}")


def _percentile(sorted_samples: list[int], frac: float) -> int:
    return sorted_samples[int(len(sorted_samples) * frac)]


def _run_scenario(
    scenario: dict, repo_root: Path, warmup_iters: int, measure_iters: int
) -> None:
    sid = scenario["id"]
    if scenario.get("mock_op_required"):
        _emit(
            {
                "binding": BINDING_NAME,
                "scenario": sid,
                "status": "skipped",
                "reason": "mock_op_unavailable",
            }
        )
        return

    try:
        config = _build_config(scenario, repo_root)
        cedarling = Cedarling(config)
        fn = _build_bench_fn(cedarling, scenario)

        if not fn():
            _emit(
                {
                    "binding": BINDING_NAME,
                    "scenario": sid,
                    "status": "skipped",
                    "reason": "validation_deny",
                }
            )
            return

        for _ in range(warmup_iters):
            fn()

        samples_ns: list[int] = []
        for _ in range(measure_iters):
            t0 = time.perf_counter_ns()
            fn()
            t1 = time.perf_counter_ns()
            samples_ns.append(t1 - t0)
    except Exception as exc:  # noqa: BLE001
        _emit(
            {
                "binding": BINDING_NAME,
                "scenario": sid,
                "status": "skipped",
                "reason": f"error:{type(exc).__name__}:{exc}",
            }
        )
        return

    sorted_samples = sorted(samples_ns)
    mean_ns = sum(samples_ns) // measure_iters
    _emit(
        {
            "binding": BINDING_NAME,
            "scenario": sid,
            "iter": measure_iters,
            "mean_ns": mean_ns,
            "p50_ns": _percentile(sorted_samples, 0.50),
            "p95_ns": _percentile(sorted_samples, 0.95),
            "p99_ns": _percentile(sorted_samples, 0.99),
            "min_ns": sorted_samples[0],
            "max_ns": sorted_samples[-1],
            "allocs_per_op": None,
            "status": "ok",
        }
    )


def main() -> int:
    repo_root = resolve_repo_root()
    manifest_path = (
        repo_root / "bindings" / "benchmarks" / "fixtures" / "scenarios.json"
    )
    with manifest_path.open() as f:
        manifest = json.load(f)
    policy = manifest.get("iteration_policy", {})
    warmup_iters = int(policy.get("warmup_iters", 100))
    measure_iters = int(policy.get("measure_iters", 1000))

    for scenario in manifest.get("scenarios", []):
        _run_scenario(scenario, repo_root, warmup_iters, measure_iters)
    return 0


if __name__ == "__main__":
    sys.exit(main())
