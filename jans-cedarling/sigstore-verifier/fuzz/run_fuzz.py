#!/usr/bin/env python3
"""Run every cargo-fuzz target for sigstore-verifier, stopping each one once
its corpus stops growing, then minimize the resulting corpus.

Targets are auto-discovered from fuzz/fuzz_targets/*.rs. Each target runs in
fixed-length chunks (default 5 min) across all CPU cores (`cargo fuzz run
--jobs N`); after each chunk the script counts files in fuzz/corpus/<target>/
and stops once growth has been at or below --growth-threshold for
--plateau-chunks consecutive chunks. By default there is no total-time cap —
a target just keeps running chunk after chunk until it plateaus, however
long that takes; pass --max-seconds for a hard cap instead (whichever of
plateau / cap is hit first stops the target). A crash (nonzero exit, or a
new file under fuzz/artifacts/<target>/) stops that target immediately and
skips its corpus minimization — the crash is left for you to inspect and
reproduce per fuzz/README.md.

Run from anywhere; paths are resolved relative to this script's location
(sigstore-verifier/fuzz/). Requires a nightly toolchain and cargo-fuzz —
see fuzz/README.md for setup. This script always invokes `cargo +nightly`
explicitly; it never changes your default toolchain.

Usage:
    python3 fuzz/run_fuzz.py                       # run to plateau, no time cap
    python3 fuzz/run_fuzz.py --targets cert_from_der sct_parse_list
    python3 fuzz/run_fuzz.py --max-seconds 3600     # cap each target at 1h
    python3 fuzz/run_fuzz.py --chunk-seconds 60 --plateau-chunks 3
    python3 fuzz/run_fuzz.py --jobs 4 --growth-threshold 0
    python3 fuzz/run_fuzz.py --skip-minimize
"""

from __future__ import annotations

import argparse
import os
import subprocess
import sys
import time
from dataclasses import dataclass, field
from pathlib import Path

FUZZ_DIR = Path(__file__).resolve().parent
CRATE_ROOT = FUZZ_DIR.parent
TARGETS_DIR = FUZZ_DIR / "fuzz_targets"
CORPUS_DIR = FUZZ_DIR / "corpus"
ARTIFACTS_DIR = FUZZ_DIR / "artifacts"


@dataclass
class TargetResult:
    name: str
    chunks_run: int = 0
    seconds_run: float = 0.0
    corpus_before: int = 0
    corpus_after: int = 0
    corpus_minimized: int | None = None
    crashed: bool = False
    crash_files: list[Path] = field(default_factory=list)
    stopped_reason: str = ""


def discover_targets() -> list[str]:
    return sorted(p.stem for p in TARGETS_DIR.glob("*.rs"))


def count_files(d: Path) -> int:
    return sum(1 for p in d.iterdir() if p.is_file()) if d.is_dir() else 0


def crash_files_for(target: str) -> set[Path]:
    d = ARTIFACTS_DIR / target
    return set(d.glob("crash-*")) | set(d.glob("timeout-*")) | set(d.glob("oom-*")) if d.is_dir() else set()


def run_chunk(target: str, jobs: int, chunk_seconds: int, chunk_num: int) -> int:
    """Run one libFuzzer chunk for `target`. Returns the process exit code."""
    cmd = [
        "cargo", "+nightly", "fuzz", "run", target,
        "--jobs", str(jobs),
        "--", f"-max_total_time={chunk_seconds}",
    ]
    print(
        f"\n>>> [{target}] chunk {chunk_num}: running "
        f"(jobs={jobs}, up to {chunk_seconds}s)"
    )
    print(f"$ {' '.join(cmd)}  (cwd={CRATE_ROOT})")
    proc = subprocess.run(cmd, cwd=CRATE_ROOT)
    return proc.returncode


def minimize_corpus(target: str) -> int | None:
    cmd = ["cargo", "+nightly", "fuzz", "cmin", target]
    print(f"\n$ {' '.join(cmd)}  (cwd={CRATE_ROOT})")
    proc = subprocess.run(cmd, cwd=CRATE_ROOT)
    if proc.returncode != 0:
        print(f"  cmin failed for {target} (exit {proc.returncode}) — corpus left as-is")
        return None
    return count_files(CORPUS_DIR / target)


def fuzz_one_target(
    target: str,
    jobs: int,
    chunk_seconds: int,
    max_seconds: int | None,
    plateau_chunks: int,
    growth_threshold: int,
) -> TargetResult:
    result = TargetResult(name=target)
    result.corpus_before = count_files(CORPUS_DIR / target)
    crashes_before = crash_files_for(target)

    print(f"\n{'=' * 70}\n{target}: starting (corpus: {result.corpus_before} files)\n{'=' * 70}")

    flat_streak = 0
    start = time.monotonic()
    while True:
        elapsed = time.monotonic() - start
        if max_seconds is not None and elapsed >= max_seconds:
            result.stopped_reason = f"hit --max-seconds ({max_seconds}s)"
            break

        this_chunk = chunk_seconds
        if max_seconds is not None:
            this_chunk = min(chunk_seconds, int(max_seconds - elapsed)) or 1
        before = count_files(CORPUS_DIR / target)
        rc = run_chunk(target, jobs, this_chunk, result.chunks_run + 1)
        result.chunks_run += 1
        result.seconds_run += this_chunk

        crashes_now = crash_files_for(target)
        new_crashes = crashes_now - crashes_before
        if rc != 0 or new_crashes:
            result.crashed = True
            result.crash_files = sorted(new_crashes) or sorted(crashes_now)
            result.stopped_reason = f"crash detected (exit {rc})"
            break

        after = count_files(CORPUS_DIR / target)
        grew_by = after - before
        total_grew = after - result.corpus_before
        print(
            f"<<< [{target}] chunk {result.chunks_run}: corpus {before} -> {after} "
            f"(+{grew_by} this chunk, +{total_grew} total, "
            f"flat streak {flat_streak + (1 if grew_by <= growth_threshold else 0)}/{plateau_chunks})"
        )

        if grew_by <= growth_threshold:
            flat_streak += 1
        else:
            flat_streak = 0

        if flat_streak >= plateau_chunks:
            result.stopped_reason = (
                f"plateau: growth <= {growth_threshold} for {plateau_chunks} consecutive chunks"
            )
            break

    result.corpus_after = count_files(CORPUS_DIR / target)
    return result


def print_summary(results: list[TargetResult]) -> None:
    print(f"\n{'=' * 70}\nSUMMARY\n{'=' * 70}")
    for r in results:
        status = "CRASH" if r.crashed else "ok"
        line = (
            f"{r.name:20} [{status:5}] {r.chunks_run} chunk(s), "
            f"~{int(r.seconds_run)}s, corpus {r.corpus_before} -> {r.corpus_after}"
        )
        if r.corpus_minimized is not None:
            line += f" -> {r.corpus_minimized} (minimized)"
        line += f"  ({r.stopped_reason})"
        print(line)
        for f in r.crash_files:
            print(f"    crash artifact: {f}")


def main() -> int:
    parser = argparse.ArgumentParser(
        description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter
    )
    parser.add_argument(
        "--targets", nargs="+", default=None,
        help="Targets to run (default: all discovered under fuzz_targets/)",
    )
    parser.add_argument(
        "--jobs", type=int, default=os.cpu_count() or 1,
        help="Parallel libFuzzer workers per target (default: all CPU cores)",
    )
    parser.add_argument(
        "--chunk-seconds", type=int, default=300,
        help="libFuzzer run length per chunk, in seconds (default: 300 = 5 min)",
    )
    parser.add_argument(
        "--max-seconds", type=int, default=None,
        help="Hard cap on total time per target, in seconds "
        "(default: none — run until plateau, however long that takes)",
    )
    parser.add_argument(
        "--plateau-chunks", type=int, default=2,
        help="Consecutive flat chunks before stopping early (default: 2)",
    )
    parser.add_argument(
        "--growth-threshold", type=int, default=1,
        help="Corpus growth (new files) at/below this counts as 'flat' (default: 1)",
    )
    parser.add_argument(
        "--skip-minimize", action="store_true",
        help="Don't run `cargo fuzz cmin` after fuzzing",
    )
    args = parser.parse_args()

    targets = args.targets or discover_targets()
    if not targets:
        print(f"no fuzz targets found under {TARGETS_DIR}", file=sys.stderr)
        return 1

    results: list[TargetResult] = []
    try:
        for target in targets:
            result = fuzz_one_target(
                target,
                jobs=args.jobs,
                chunk_seconds=args.chunk_seconds,
                max_seconds=args.max_seconds,
                plateau_chunks=args.plateau_chunks,
                growth_threshold=args.growth_threshold,
            )
            results.append(result)

            if result.crashed:
                print(f"\n{target}: CRASH — skipping corpus minimization, see artifacts above")
                continue
            if args.skip_minimize:
                continue
            result.corpus_minimized = minimize_corpus(target)
    except KeyboardInterrupt:
        print("\ninterrupted — stopping (targets not yet reached are skipped)")

    print_summary(results)
    return 1 if any(r.crashed for r in results) else 0


if __name__ == "__main__":
    sys.exit(main())
