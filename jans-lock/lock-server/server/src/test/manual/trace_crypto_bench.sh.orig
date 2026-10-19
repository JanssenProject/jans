#!/bin/bash

# =============================================================================
# Runs the TRACE crypto throughput benchmarks and prints a comparison table.
#
#   JAVA   : trace-crypto-java    JavaTraceCryptoBenchmarkTest
#   NATIVE : trace-crypto-native  NativeTraceCryptoBenchmarkTest
#
# Both tests extend AbstractTraceCryptoBenchmarkTest (java test-jar), so the
# workload and the measurement are identical. The tests are opt-in and run only
# with -Dtrace.bench=true, which this script sets.
#
# Usage: trace_crypto_bench.sh [-p java|native|both] [-t threads] [-w warmupMs]
#                              [-d durationMs] [-o] [-l logfile]
#   -p  provider to run (default: both)
#   -t  worker threads (default: 4)
#   -w  warm-up per operation in ms (default: 2000)
#   -d  measured time per operation in ms (default: 5000)
#   -o  run Maven offline
#   -l  keep the full Maven output in this file (default: temporary file)
#
# Requirements: bash, mvn, awk
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../../../.." && pwd)"

PROVIDER="both"
THREADS=4
WARMUP_MS=2000
DURATION_MS=5000
OFFLINE=""
LOG_FILE=""

usage() {
    local status="${1:-1}"
    local name
    name="$(basename "${BASH_SOURCE[0]}")"
    cat <<EOF
Usage: ${name} [options]

Runs the TRACE crypto throughput benchmarks (JAVA and NATIVE) and prints a
comparison table of throughput and latency.

Options:
  -p <provider>  Provider to run: java, native or both (default: both)
  -t <threads>   Worker threads (default: 4)
  -w <ms>        Warm-up time per operation in ms (default: 2000)
  -d <ms>        Measured time per operation in ms (default: 5000)
  -o             Run Maven offline
  -l <file>      Keep the full Maven output in <file> (default: temporary file)
  -h             Show this help and exit

Examples:
  ${name}                     # both providers, defaults
  ${name} -p native -t 8      # native only, 8 threads
  ${name} -o -w 500 -d 2000   # quick offline run

Requirements: bash, mvn, awk
EOF
    exit "${status}"
}

while getopts "p:t:w:d:ol:h" opt; do
    case "${opt}" in
        p) PROVIDER="${OPTARG}" ;;
        t) THREADS="${OPTARG}" ;;
        w) WARMUP_MS="${OPTARG}" ;;
        d) DURATION_MS="${OPTARG}" ;;
        o) OFFLINE="-o" ;;
        l) LOG_FILE="${OPTARG}" ;;
        h) usage 0 ;;
        *) usage 1 ;;
    esac
done

case "${PROVIDER}" in
    java|native|both) ;;
    *) echo "Invalid provider: ${PROVIDER}" >&2; usage 1 ;;
esac

for number in "${THREADS}" "${WARMUP_MS}" "${DURATION_MS}"; do
    [[ "${number}" =~ ^[0-9]+$ ]] || { echo "Not a number: ${number}" >&2; usage 1; }
done

if [[ -z "${LOG_FILE}" ]]; then
    LOG_FILE="$(mktemp -t trace-crypto-bench.XXXXXX.log)"
    TEMP_LOG=1
else
    TEMP_LOG=0
fi
: > "${LOG_FILE}"

BENCH_PROPS=(-Dtrace.bench=true "-Dtrace.bench.threads=${THREADS}"
    "-Dtrace.bench.warmupMs=${WARMUP_MS}" "-Dtrace.bench.durationMs=${DURATION_MS}"
    -Dsurefire.failIfNoSpecifiedTests=false)

run_mvn() {
    echo ">> mvn $*" >> "${LOG_FILE}"
    if ! (cd "${ROOT}" && mvn ${OFFLINE} -B "$@" "${BENCH_PROPS[@]}") >> "${LOG_FILE}" 2>&1; then
        echo "Maven failed. Last lines of ${LOG_FILE}:" >&2
        tail -n 40 "${LOG_FILE}" >&2
        exit 1
    fi
}

echo "Threads=${THREADS} warmup=${WARMUP_MS}ms duration=${DURATION_MS}ms per operation; log: ${LOG_FILE}"

# The java module is installed because the native module reads its test-jar (base
# benchmark class) from the local repository.
if [[ "${PROVIDER}" != "native" ]]; then
    echo "Running JAVA benchmark ..."
    run_mvn -pl trace-crypto-java install -Dtest=JavaTraceCryptoBenchmarkTest \
        -Dmaven.javadoc.skip=true -Dmaven.source.skip=true -Dgpg.skip
elif ! ls "${HOME}"/.m2/repository/io/jans/jans-lock-trace-crypto-java/*/*-tests.jar > /dev/null 2>&1; then
    echo "Running JAVA module install (native benchmark needs its test-jar) ..."
    run_mvn -pl trace-crypto-java install -DskipTests \
        -Dmaven.javadoc.skip=true -Dmaven.source.skip=true -Dgpg.skip
fi

if [[ "${PROVIDER}" != "java" ]]; then
    echo "Running NATIVE benchmark ..."
    run_mvn -pl trace-crypto-native test -Dtest=NativeTraceCryptoBenchmarkTest
fi

if ! grep -q '^TRACE-BENCH ' "${LOG_FILE}"; then
    echo "No TRACE-BENCH lines found; the benchmarks may have been skipped. See ${LOG_FILE}" >&2
    exit 1
fi

echo
grep '^TRACE-BENCH ' "${LOG_FILE}" | awk '
function field(line, name,    pattern, rest) {
    pattern = " " name "=[^ ]+"
    if (match(line, pattern)) {
        rest = substr(line, RSTART + length(name) + 2, RLENGTH - length(name) - 2)
        return rest
    }
    return ""
}
{
    provider = field($0, "provider")
    op = field($0, "op")
    if (!(op in seenOp)) { seenOp[op] = 1; ops[++opCount] = op }
    if (!(provider in seenProvider)) { seenProvider[provider] = 1; providers[++providerCount] = provider }
    opsPerSecond[provider, op] = field($0, "ops/s")
    mean[provider, op] = field($0, "meanUs")
    p50[provider, op] = field($0, "p50Us")
    p99[provider, op] = field($0, "p99Us")
    maxLatency[provider, op] = field($0, "maxUs")
    threads = field($0, "threads")
    duration = field($0, "durationMs")
}
function cell(table, provider, op) {
    return ((provider, op) in table) ? table[provider, op] : "-"
}
function metric(title, table, withRatio,    i, j, line, ratio) {
    printf "%s\n", title
    printf "%-16s", "operation"
    for (j = 1; j <= providerCount; j++) printf " %12s", providers[j]
    if (withRatio && providerCount == 2) printf " %12s", providers[1] "/" providers[2]
    printf "\n"
    for (i = 1; i <= opCount; i++) {
        printf "%-16s", ops[i]
        for (j = 1; j <= providerCount; j++) printf " %12s", cell(table, providers[j], ops[i])
        if (withRatio && providerCount == 2) {
            a = table[providers[1], ops[i]] + 0
            b = table[providers[2], ops[i]] + 0
            ratio = (a > 0 && b > 0) ? sprintf("%.1fx", a / b) : "-"
            printf " %12s", ratio
        }
        printf "\n"
    }
    printf "\n"
}
END {
    printf "Threads: %s, measured: %s ms per operation\n\n", threads, duration
    metric("Throughput (ops/s, higher is better)", opsPerSecond, 1)
    metric("Mean latency (us, lower is better)", mean, 0)
    metric("p50 latency (us)", p50, 0)
    metric("p99 latency (us)", p99, 0)
    metric("Max latency (us)", maxLatency, 0)
}'

if [[ "${TEMP_LOG}" == "1" ]]; then
    echo "Full Maven output: ${LOG_FILE}"
fi
echo "CSV rows: ${ROOT}/trace-crypto-java/target/trace-crypto-bench.csv and ${ROOT}/trace-crypto-native/target/trace-crypto-bench.csv"
