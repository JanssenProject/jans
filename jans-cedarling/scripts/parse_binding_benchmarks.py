"""Parse binding benchmark output and write a Markdown summary section.

Usage:
    python3 parse_binding_benchmarks.py --format go  bench_output.txt
    python3 parse_binding_benchmarks.py --format go  bench_output.txt >> $GITHUB_STEP_SUMMARY
    python3 parse_binding_benchmarks.py --format c   bench_output.txt >> $GITHUB_STEP_SUMMARY

Supported formats:
    go   -- output of `go test -bench=. -benchmem`
    c    -- line-oriented `name=<op> mean_us=<f> stddev_us=<f> min_us=<f> max_us=<f>`
    rust -- output of scripts/check_benchmarks.py (`✅ <bench>: <ns> ns ...`)

Each invocation writes one self-contained Markdown section so multiple
bindings can append independently to $GITHUB_STEP_SUMMARY without
coordinating with each other.
"""

import argparse
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
# Markdown output
# ---------------------------------------------------------------------------

def _fmt_us(v: float) -> str:
    return f"{v:,.1f}"


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


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--format", required=True, choices=PARSERS, help="Benchmark output format")
    parser.add_argument("file", nargs="?", help="Input file (default: stdin)")
    args = parser.parse_args()

    text = open(args.file).read() if args.file else sys.stdin.read()
    binding_name, parse_fn = PARSERS[args.format]
    rows = parse_fn(text)
    print(render_markdown(binding_name, rows))
    return 0


if __name__ == "__main__":
    sys.exit(main())
