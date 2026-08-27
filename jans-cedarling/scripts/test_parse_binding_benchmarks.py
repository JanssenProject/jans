"""Tests for parse_binding_benchmarks.py.

Run: python3 -m pytest jans-cedarling/scripts/test_parse_binding_benchmarks.py
"""

import pathlib
import subprocess
import sys

import parse_binding_benchmarks as pbb

SCRIPT = pathlib.Path(__file__).with_name("parse_binding_benchmarks.py")
WORKFLOW = (
    pathlib.Path(__file__).resolve().parents[2]
    / ".github"
    / "workflows"
    / "test-cedarling.yml"
)

# Wording that only holds when benchmarks run on GitHub-hosted runners. The
# generic renderer serves arbitrary local-file / stdin input too, so it must
# NOT bake this provenance claim into its output.
_RUNNER_CLAIM_MARKERS = ("GitHub-hosted", "per-runner variance", "unpaired")

_SAMPLE_ROWS = [
    {"binding": "c", "scenario": "unsigned_simple", "status": "ok", "mean_ns": 25800},
    {"binding": "rust", "scenario": "unsigned_simple", "status": "ok", "mean_ns": 24200},
]


def test_generic_renderer_omits_runner_provenance_claim():
    out = pbb.render_jsonl_pivot(_SAMPLE_ROWS)
    for marker in _RUNNER_CLAIM_MARKERS:
        assert marker not in out, f"generic renderer leaked provenance claim: {marker!r}"


def test_stdin_output_omits_runner_provenance_claim():
    jsonl = "".join(
        f'{{"binding":"{r["binding"]}","scenario":"{r["scenario"]}",'
        f'"status":"ok","mean_ns":{r["mean_ns"]}}}\n'
        for r in _SAMPLE_ROWS
    )
    proc = subprocess.run(
        [sys.executable, str(SCRIPT), "--format", "jsonl"],
        input=jsonl,
        capture_output=True,
        text=True,
        check=True,
    )
    for marker in _RUNNER_CLAIM_MARKERS:
        assert marker not in proc.stdout, f"stdin path leaked provenance claim: {marker!r}"


def test_workflow_provenance_states_runner_variance_caveat():
    # The runner-variance caveat belongs only in the workflow-specific
    # Provenance block, where the GitHub-hosted-runner context is guaranteed.
    text = WORKFLOW.read_text()
    assert "GitHub-hosted" in text
    assert "per-runner variance" in text
    assert "unpaired" in text
