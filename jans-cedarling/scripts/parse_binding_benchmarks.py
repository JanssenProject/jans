"""Parse binding benchmark output and write a Markdown summary section.

Usage:
    python3 parse_binding_benchmarks.py --format go    bench_output.txt
    python3 parse_binding_benchmarks.py --format jsonl java.jsonl go.jsonl ...
    python3 parse_binding_benchmarks.py --format c     bench_output.txt >> $GITHUB_STEP_SUMMARY

Supported formats:
    jsonl -- canonical cross-platform schema (one JSON object per line per
             scenario per binding). See bindings/benchmarks/CONTRACT.md.
             Multiple input files are pivoted into one cross-binding
             comparison table.
    go    -- output of `go test -bench=. -benchmem`     (legacy single-binding)
    c     -- line-oriented `name=<op> mean_us=<f> ...`  (legacy single-binding)
    rust  -- output of scripts/check_benchmarks.py      (legacy single-binding)

Each invocation writes one self-contained Markdown section so multiple
bindings can append independently to $GITHUB_STEP_SUMMARY without
coordinating with each other.
"""

import argparse
import json
import re
import sys


# ---------------------------------------------------------------------------
# Go format
# ---------------------------------------------------------------------------
# Example lines:
#   BenchmarkAuthorizeUnsigned-8        1000    1234567 ns/op    2048 B/op    18 allocs/op
# ---------------------------------------------------------------------------
_GO_RE = re.compile(
    r"^(Benchmark\S+?)-\d+\s+"   # name + GOMAXPROCS suffix
    r"\d+\s+"                    # iterations (b.N)
    r"([\d.]+)\s+ns/op"          # ns/op  ← the metric we want
    r"(?:\s+([\d.]+)\s+B/op)?"   # optional B/op
    r"(?:\s+([\d.]+)\s+allocs/op)?",  # optional allocs/op
    re.MULTILINE,
)


def _strip_benchmark_prefix(name: str) -> str:
    """'BenchmarkAuthorizeUnsigned' → 'AuthorizeUnsigned'"""
    return name.removeprefix("Benchmark")


def parse_go(text: str) -> list[dict]:
    rows = []
    for m in _GO_RE.finditer(text):
        ns = float(m.group(2))
        allocs = m.group(4)
        rows.append(
            {
                "operation": _strip_benchmark_prefix(m.group(1)),
                "mean_us": ns / 1_000,
                "allocs_per_op": int(float(allocs)) if allocs else None,
            }
        )
    return rows


# ---------------------------------------------------------------------------
# C / Python / WASM / Java format (shared line-oriented format)
# ---------------------------------------------------------------------------
# Example lines:
#   name=authorize_unsigned mean_us=295.2 stddev_us=12.0 min_us=281.0 max_us=345.0
# ---------------------------------------------------------------------------
_KV_RE = re.compile(
    r"name=(\S+)"
    r"\s+mean_us=([\d.]+)"
    r"\s+stddev_us=([\d.]+)"
    r"(?:\s+min_us=([\d.]+))?"
    r"(?:\s+max_us=([\d.]+))?",
    re.MULTILINE,
)


def parse_kv(text: str) -> list[dict]:
    rows = []
    for m in _KV_RE.finditer(text):
        rows.append(
            {
                "operation": m.group(1),
                "mean_us": float(m.group(2)),
                "stddev_us": float(m.group(3)),
            }
        )
    return rows


# ---------------------------------------------------------------------------
# Rust benchmark check script output
# ---------------------------------------------------------------------------
# Example lines:
#   ✅ authz_authorize_unsigned/new/estimates.json: 371266 ns <= 1000000 ns
# ---------------------------------------------------------------------------
_RUST_RE = re.compile(
    r"^[✅❌]\s+(.+?):\s+(\d+(?:\.\d+)?)\s+ns\b",
    re.MULTILINE,
)


def parse_rust(text: str) -> list[dict]:
    rows = []
    for m in _RUST_RE.finditer(text):
        rows.append(
            {
                "operation": m.group(1),
                "mean_us": float(m.group(2)) / 1000.0,
            }
        )
    return rows


# ---------------------------------------------------------------------------
# JSON Lines — the canonical cross-platform schema (see bindings/benchmarks/CONTRACT.md).
# Bad lines are skipped with a stderr warning so one crashing line doesn't poison the file.
# ---------------------------------------------------------------------------
_JSONL_REQUIRED_FIELDS = ("binding", "scenario", "status")


def parse_jsonl(text: str) -> list[dict]:
    rows = []
    for lineno, raw in enumerate(text.splitlines(), 1):
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        try:
            obj = json.loads(line)
        except json.JSONDecodeError as e:
            print(
                f"parse_binding_benchmarks: skip line {lineno}: {e}",
                file=sys.stderr,
            )
            continue
        missing = [f for f in _JSONL_REQUIRED_FIELDS if f not in obj]
        if missing:
            print(
                f"parse_binding_benchmarks: skip line {lineno}: missing fields {missing}",
                file=sys.stderr,
            )
            continue
        rows.append(obj)
    return rows


# ---------------------------------------------------------------------------
# Markdown output
# ---------------------------------------------------------------------------

def _fmt_us(v: float) -> str:
    return f"{v:,.1f}"


def _ns_to_us(ns: float | int | None) -> str:
    if ns is None:
        return "—"
    return _fmt_us(float(ns) / 1_000.0)


def render_jsonl_pivot(rows: list[dict]) -> str:
    """Render a Scenario × Binding pivot plus per-binding detail tables."""
    if not rows:
        return "### Cross-Platform Binding Benchmarks\n\n_No results found._\n"

    by_key: dict[tuple[str, str], dict] = {}
    bindings: list[str] = []
    scenarios: list[str] = []
    for r in rows:
        b, s = r["binding"], r["scenario"]
        if b not in bindings:
            bindings.append(b)
        if s not in scenarios:
            scenarios.append(s)
        by_key[(b, s)] = r

    out: list[str] = ["### Cross-Platform Binding Benchmarks\n\n"]
    out.append("#### Mean (µs) per scenario × binding\n\n")
    out.append("| Scenario | " + " | ".join(bindings) + " |\n")
    out.append("|----------|" + "|".join(["----------:"] * len(bindings)) + "|\n")
    for s in scenarios:
        cells: list[str] = []
        for b in bindings:
            r = by_key.get((b, s))
            if r is None:
                cells.append("—")
            elif r.get("status") == "skipped":
                cells.append("_skipped_")
            else:
                cells.append(_ns_to_us(r.get("mean_ns")))
        out.append(f"| {s} | " + " | ".join(cells) + " |\n")
    out.append("\n")
    for b in bindings:
        out.append(f"#### {b} detail\n\n")
        out.append("| Scenario | Mean | p50 | p95 | p99 | Min | Max | Allocs/op | Status |\n")
        out.append("|----------|----:|----:|----:|----:|----:|----:|----------:|--------|\n")
        for s in scenarios:
            r = by_key.get((b, s))
            if r is None:
                continue
            status = r.get("status", "ok")
            if status == "skipped":
                reason = r.get("reason", "")
                out.append(
                    f"| {s} | — | — | — | — | — | — | — | skipped ({reason}) |\n"
                )
                continue
            allocs = r.get("allocs_per_op")
            allocs_cell = str(allocs) if allocs is not None else "—"
            out.append(
                f"| {s} | "
                f"{_ns_to_us(r.get('mean_ns'))} | "
                f"{_ns_to_us(r.get('p50_ns'))} | "
                f"{_ns_to_us(r.get('p95_ns'))} | "
                f"{_ns_to_us(r.get('p99_ns'))} | "
                f"{_ns_to_us(r.get('min_ns'))} | "
                f"{_ns_to_us(r.get('max_ns'))} | "
                f"{allocs_cell} | "
                f"{status} |\n"
            )
        out.append("\n")

    return "".join(out)


def render_markdown(binding: str, rows: list[dict]) -> str:
    if not rows:
        return f"### {binding} Binding Benchmarks\n\n_No results found._\n"

    has_allocs = any(r.get("allocs_per_op") is not None for r in rows)
    has_stddev = any(r.get("stddev_us") is not None for r in rows)

    header = f"### {binding} Binding Benchmarks\n\n"
    if has_allocs:
        header += "| Operation | Mean (µs) | Allocs/op |\n"
        header += "|-----------|----------:|----------:|\n"
    elif has_stddev:
        header += "| Operation | Mean (µs) | Std Dev (µs) |\n"
        header += "|-----------|----------:|-------------:|\n"
    else:
        header += "| Operation | Mean (µs) |\n"
        header += "|-----------|----------:|\n"

    lines = [header]
    for r in rows:
        if has_allocs:
            allocs = str(r["allocs_per_op"]) if r.get("allocs_per_op") is not None else "—"
            lines.append(f"| {r['operation']} | {_fmt_us(r['mean_us'])} | {allocs} |\n")
        elif has_stddev:
            stddev = _fmt_us(r["stddev_us"]) if r.get("stddev_us") is not None else "—"
            lines.append(f"| {r['operation']} | {_fmt_us(r['mean_us'])} | {stddev} |\n")
        else:
            lines.append(f"| {r['operation']} | {_fmt_us(r['mean_us'])} |\n")

    return "".join(lines)


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

PARSERS = {
    "go": ("Go", parse_go),
    "rust": ("Rust", parse_rust),
    "c": ("C", parse_kv),
    "python": ("Python", parse_kv),
    "wasm": ("WASM", parse_kv),
    "java": ("Java", parse_kv),
}

CANONICAL_FORMAT = "jsonl"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--format",
        required=True,
        choices=[CANONICAL_FORMAT, *PARSERS],
        help="Benchmark output format ('jsonl' = canonical cross-platform schema)",
    )
    parser.add_argument(
        "files",
        nargs="*",
        help="Input file(s). Default: stdin. jsonl format accepts multiple files for cross-binding pivot.",
    )
    args = parser.parse_args()

    if args.format == CANONICAL_FORMAT:
        # Cross-binding: read every file (or stdin), merge rows, pivot.
        rows: list[dict] = []
        if args.files:
            for path in args.files:
                with open(path) as f:
                    rows.extend(parse_jsonl(f.read()))
        else:
            rows.extend(parse_jsonl(sys.stdin.read()))
        print(render_jsonl_pivot(rows))
        return 0

    # Legacy single-binding paths.
    if len(args.files) > 1:
        print(
            f"parse_binding_benchmarks: --format {args.format} accepts only one input file",
            file=sys.stderr,
        )
        return 2
    text = open(args.files[0]).read() if args.files else sys.stdin.read()
    binding_name, parse_fn = PARSERS[args.format]
    rows = parse_fn(text)
    print(render_markdown(binding_name, rows))
    return 0


if __name__ == "__main__":
    sys.exit(main())
