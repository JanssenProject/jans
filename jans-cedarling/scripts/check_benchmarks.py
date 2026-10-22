import json
import os
import sys

THRESHOLD_NS = 1_000_000  # 1 millisecond in nanoseconds
BASE_PATH = "target/criterion"

# Startup benchmarks, heavy batch tests (which involve evaluating multiple
# tokens/issuers across N items) and the TRACE emitter throughput (which times
# 10 000 decisions per iteration) exceed the 1ms threshold by design: they are
# shown, but never fail the check.
EXCLUDE_BENCHMARKS = {
    "cedarling_startup",
    "multi_issuer_batch_vs_sequence_n10",
    "multi_issuer_batch_vs_sequence_n25",
    "unsigned_batch_vs_sequence_n25",
    "trace_emitter_throughput",
}


def check_benchmarks(base_path):
    """Prints every benchmark mean and returns whether a checked one exceeds the threshold."""
    failed = False

    for root, _, files in os.walk(base_path):
        if "estimates.json" in files:
            path = os.path.join(root, "estimates.json")
            benchmark_full_name = os.path.relpath(path, base_path)

            with open(path) as f:
                data = json.load(f)

            estimate = data.get("mean", {}).get("point_estimate")
            if estimate is None:
                continue

            benchmark_name = benchmark_full_name.split("/")[0]
            if benchmark_name in EXCLUDE_BENCHMARKS:
                print(
                    f"⚪ {benchmark_full_name}: {estimate:.0f} ns "
                    f"(not checked against {THRESHOLD_NS} ns)"
                )
            elif estimate > THRESHOLD_NS:
                print(
                    f"❌ {benchmark_full_name}: {estimate:.0f} ns > {THRESHOLD_NS} ns"
                )
                failed = True
            else:
                print(
                    f"✅ {benchmark_full_name}: {estimate:.0f} ns <= {THRESHOLD_NS} ns"
                )

    return failed


def main():
    if check_benchmarks(BASE_PATH):
        sys.exit(1)


if __name__ == "__main__":
    main()
