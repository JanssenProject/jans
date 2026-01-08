import json
import os
import sys

THRESHOLD_NS = 1_000_000  # 1 millisecond in nanoseconds
BASE_PATH = "target/criterion"

EXCLUDE_BENCHMARKS = {"cedarling_startup"}

# The following benchmarks have a high threshold just to make sure that the benchmark tests are not failing in CI.
# This should be removed once issue https://github.com/JanssenProject/jans/issues/12947 is fixed.
EXCLUSION_THRESHOLD = 1_500_000  # 1.5 milliseconds in nanoseconds
PROBLEMATIC_BENCHMARKS = {
    "authz_authorize_without_jwt_validation",
    "authz_authorize_with_jwt_validation_hs256",
}


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

            if benchmark_name in PROBLEMATIC_BENCHMARKS:
                threshold = EXCLUSION_THRESHOLD
            else:
                threshold = THRESHOLD_NS

            if estimate > threshold:
                print(
                    f"❌ {benchmark_full_name}: {estimate:.0f} ns > {threshold} ns"
                )
                failed = True
            else:
                print(
                    f"✅ {benchmark_full_name}: {estimate:.0f} ns <= {threshold} ns"
                )

    if failed:
        sys.exit(1)


if __name__ == "__main__":
    check_benchmarks()
