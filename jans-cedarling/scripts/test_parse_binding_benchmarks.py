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


def _provenance_block(text: str) -> tuple[str, str]:
    """Split the workflow into (Provenance block, everything else).

    The block runs from the `## Provenance` heading up to the generated
    pivot (emitted by the parse script), which is where the doc's next
    section begins.
    """
    start = text.index('echo "## Provenance"')
    end = text.index("parse_binding_benchmarks.py --format jsonl $BENCH_FILES", start)
    return text[start:end], text[:start] + text[end:]


def test_workflow_provenance_states_runner_variance_caveat():
    # The runner-variance caveat belongs only in the workflow-specific
    # Provenance block, where the GitHub-hosted-runner context is guaranteed.
    block, rest = _provenance_block(WORKFLOW.read_text())
    for marker in _RUNNER_CLAIM_MARKERS:
        assert marker in block, f"Provenance block missing caveat marker: {marker!r}"
        assert marker not in rest, f"caveat marker leaked outside Provenance: {marker!r}"
