import json
import os
import sys

THRESHOLD_NS = 1_000_000  # 1 millisecond in nanoseconds
BASE_PATH = "target/criterion"

EXCLUDE_BENCHMARKS = {"cedarling_startup"}


def check_benchmarks():
    failed = False

    for root, _, files in os.walk(BASE_PATH):
        if "estimates.json" in files:
            path = os.path.join(root, "estimates.json")
            benchmark_full_name = os.path.relpath(path, BASE_PATH)

            benchmark_name = benchmark_full_name.split("/")[0]
            if benchmark_name in EXCLUDE_BENCHMARKS:
                continue

            with open(path) as f:
                data = json.load(f)

            estimate = data.get("mean", {}).get("point_estimate")
            if estimate is None:
                continue

            if estimate > THRESHOLD_NS:
                print(
                    f"❌ {benchmark_full_name}: {estimate:.0f} ns > {THRESHOLD_NS} ns"
                )
                failed = True
            else:
                print(
                    f"✅ {benchmark_full_name}: {estimate:.0f} ns <= {THRESHOLD_NS} ns"
                )

    if failed:
        sys.exit(1)


if __name__ == "__main__":
    check_benchmarks()
