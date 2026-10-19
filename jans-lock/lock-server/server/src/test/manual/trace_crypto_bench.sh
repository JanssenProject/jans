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
#                              [-d durationMs] [-r] [-o] [-l logfile]
#   -p  provider to run (default: both)
#   -t  worker threads (default: 4)
#   -w  warm-up per operation in ms (default: 2000)
#   -d  measured time per operation in ms (default: 5000)
#   -r  with -p both, run NATIVE first instead of JAVA (also: REVERSE=1)
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
REVERSE="${REVERSE:-0}"

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
  -r             With -p both, run NATIVE first instead of JAVA. The first run sees
                 a colder cache and a different CPU frequency state, so repeat
                 with and without -r and compare. REVERSE=1 does the same.
  -o             Run Maven offline
  -l <file>      Keep the full Maven output in <file> (default: temporary file)
  -h             Show this help and exit

Examples:
  ${name}                     # both providers, defaults
  ${name} -p native -t 8      # native only, 8 threads
  ${name} -o -w 500 -d 2000   # quick offline run
  ${name} -r                  # both providers, NATIVE first

Requirements: bash, mvn, awk
EOF
    exit "${status}"
}

while getopts "p:t:w:d:rol:h" opt; do
    case "${opt}" in
        p) PROVIDER="${OPTARG}" ;;
        t) THREADS="${OPTARG}" ;;
        w) WARMUP_MS="${OPTARG}" ;;
        d) DURATION_MS="${OPTARG}" ;;
        r) REVERSE=1 ;;
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

run_java() {
    echo "Running JAVA benchmark ..."
    run_mvn -pl trace-crypto-java -am install -Dtest=JavaTraceCryptoBenchmarkTest \
        -Dmaven.javadoc.skip=true -Dmaven.source.skip=true -Dgpg.skip
}
run_native() {
    echo "Running NATIVE benchmark ..."
    run_mvn -pl trace-crypto-native -am install -Dtest=NativeTraceCryptoBenchmarkTest \
        -Dmaven.javadoc.skip=true -Dmaven.source.skip=true -Dgpg.skip
}
case "${PROVIDER}" in
    java) ORDER=(java) ;;
    native) ORDER=(native) ;;
    both)
        if [[ "${REVERSE}" == "1" ]]; then ORDER=(native java); else ORDER=(java native); fi ;;
esac
EXECUTION_ORDER="${ORDER[*]}"
EXECUTION_ORDER="${EXECUTION_ORDER// / then }"
echo "Execution order: ${EXECUTION_ORDER}"
for step in "${ORDER[@]}"; do "run_${step}"; done

if ! grep -q '^TRACE-BENCH ' "${LOG_FILE}"; then
    echo "No TRACE-BENCH lines found; the benchmarks may have been skipped. See ${LOG_FILE}" >&2
    exit 1
fi

echo
grep '^TRACE-BENCH ' "${LOG_FILE}" | awk -v order="${EXECUTION_ORDER}" '
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
    printf "Threads: %s, measured: %s ms per operation\n", threads, duration
    printf "Execution order: %s (the first run has no preceding warm cache)\n\n", order
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
