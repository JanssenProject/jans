#!/bin/bash

# =============================================================================
# Runs the TRACE ingest benchmark (TraceIngestBenchmark, lock-server/service) and
# prints a result table.
#
# Signed TRACE records go through the real ingest pipeline (TraceIngestionService)
# over OrmTraceStore and a live SQL database, so every ORM round trip is paid.
# The benchmark is opt-in: the Maven profile "benchmark" (service pom) includes
# *Benchmark.java, the default build does not run it. The benchmark writes into a
# throw-away domain "bench-xxxxxxxx" and removes it afterwards.
#
# Usage: trace_ingest_bench.sh [-c confdir] [-n records] [-k chains] [-p preload]
#                              [-m minRate] [-s maxSlowdown] [-o] [-l logfile]
#   -c  database profile folder with jans-sql.properties and salt (default:
#       $LOCK_BENCH_PROFILE, else jans-orm/integration-test/profiles/jans-dev.jans.info/conf)
#   -n  records per chain (default: 1000)
#   -k  comma separated parallel chain counts (default: 1,4)
#   -p  committed receipts inserted into the domain before the run (default: 0)
#   -m  fail when one-chain records/s is lower than this (default: off)
#   -s  fail when the last quarter of a one-chain run is more than this many times
#       slower than the first quarter (default: off)
#   -o  run Maven offline
#   -l  keep the full Maven output in this file (default: temporary file)
#
# Requirements: bash, mvn, awk, a reachable SQL database (PostgreSQL or MySQL)
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../../../.." && pwd)"
REPO_ROOT="$(cd "${ROOT}/../.." && pwd)"

PROFILE_DIR="${LOCK_BENCH_PROFILE:-${REPO_ROOT}/jans-orm/integration-test/profiles/jans-dev.jans.info/conf}"
RECORDS=1000
CHAINS="1,4"
PRELOAD=0
MIN_RATE=""
MAX_SLOWDOWN=""
OFFLINE=""
LOG_FILE=""

usage() {
    local status="${1:-1}"
    local name
    name="$(basename "${BASH_SOURCE[0]}")"
    cat <<EOF
Usage: ${name} [options]

Runs the TRACE ingest benchmark against a live SQL database and prints records/s
and latency per scenario (one row per number of parallel chains).

Options:
  -c <dir>   Database profile folder with jans-sql.properties and salt
             (default: \$LOCK_BENCH_PROFILE, else
             jans-orm/integration-test/profiles/jans-dev.jans.info/conf)
  -n <num>   Records per chain (default: 1000)
  -k <list>  Comma separated parallel chain counts (default: 1,4)
  -p <num>   Committed receipts inserted into the domain before the run, to expose
             work that grows with the receipts of a domain (default: 0). Run ANALYZE
             on the receipt table after a big preload, or the first run sees stale
             planner statistics.
  -m <rate>  Fail when one-chain records/s is lower than <rate>
  -s <ratio> Fail when the last quarter of a one-chain run is more than <ratio> times
             slower than the first quarter
  -o         Run Maven offline
  -l <file>  Keep the full Maven output in <file> (default: temporary file)
  -h         Show this help and exit

Examples:
  ${name}                           # defaults
  ${name} -o -n 300 -k 1            # quick offline run, one chain
  ${name} -o -n 500 -k 1 -p 30000   # one chain over a domain with 30000 receipts
  ${name} -c /path/to/conf -s 2     # another database, fail on 2x slowdown

Requirements: bash, mvn, awk, a reachable SQL database
EOF
    exit "${status}"
}

while getopts "c:n:k:p:m:s:ol:h" opt; do
    case "${opt}" in
        c) PROFILE_DIR="${OPTARG}" ;;
        n) RECORDS="${OPTARG}" ;;
        k) CHAINS="${OPTARG}" ;;
        p) PRELOAD="${OPTARG}" ;;
        m) MIN_RATE="${OPTARG}" ;;
        s) MAX_SLOWDOWN="${OPTARG}" ;;
        o) OFFLINE="-o" ;;
        l) LOG_FILE="${OPTARG}" ;;
        h) usage 0 ;;
        *) usage 1 ;;
    esac
done

for number in "${RECORDS}" "${PRELOAD}"; do
    [[ "${number}" =~ ^[0-9]{1,9}$ ]] || { echo "Not a number (digits only, at most 9): ${number}" >&2; usage 1; }
done
# Plain decimal for the benchmark: strip leading zeros so 010 means 10, not octal 8
RECORDS=$((10#${RECORDS}))
PRELOAD=$((10#${PRELOAD}))
[[ "${CHAINS}" =~ ^[0-9]+(,[0-9]+)*$ ]] || { echo "Invalid chain list: ${CHAINS}" >&2; usage 1; }
for number in "${MIN_RATE}" "${MAX_SLOWDOWN}"; do
    [[ -z "${number}" || "${number}" =~ ^[0-9]+([.][0-9]+)?$ ]] || { echo "Not a number: ${number}" >&2; usage 1; }
done

for file in jans-sql.properties salt; do
    [[ -f "${PROFILE_DIR}/${file}" ]] || { echo "Missing ${PROFILE_DIR}/${file}; use -c <dir>" >&2; exit 1; }
done

# Maven runs from ${ROOT} and surefire from the service module: pass an absolute path
PROFILE_DIR="$(cd "${PROFILE_DIR}" && pwd)"

if [[ -z "${LOG_FILE}" ]]; then
    LOG_FILE="$(mktemp -t trace-ingest-bench.XXXXXX.log)"
    TEMP_LOG=1
else
    TEMP_LOG=0
fi
: > "${LOG_FILE}"

BENCH_PROPS=("-Dlock.benchmark.profile=${PROFILE_DIR}" "-Dlock.benchmark.records=${RECORDS}"
    "-Dlock.benchmark.chains=${CHAINS}" "-Dlock.benchmark.preload=${PRELOAD}"
    -Dsurefire.failIfNoSpecifiedTests=false -Ddownload.plugin.skip=true)
[[ -n "${MIN_RATE}" ]] && BENCH_PROPS+=("-Dlock.benchmark.min-rate=${MIN_RATE}")
[[ -n "${MAX_SLOWDOWN}" ]] && BENCH_PROPS+=("-Dlock.benchmark.max-slowdown=${MAX_SLOWDOWN}")

echo "Profile=${PROFILE_DIR} records=${RECORDS}/chain chains=${CHAINS} preload=${PRELOAD}; log: ${LOG_FILE}"
echo "Running ingest benchmark ..."

echo ">> mvn -P benchmark -pl model,service -am test ${BENCH_PROPS[*]}" >> "${LOG_FILE}"
if ! (cd "${ROOT}" && mvn ${OFFLINE} -B -P benchmark -pl model,service -am test "${BENCH_PROPS[@]}") >> "${LOG_FILE}" 2>&1; then
    echo "Maven failed. Last lines of ${LOG_FILE}:" >&2
    tail -n 40 "${LOG_FILE}" >&2
    exit 1
fi

ROW_RE='^  [0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9:.]+Z,'
if ! grep -Eq "${ROW_RE}" "${LOG_FILE}"; then
    echo "No benchmark result rows found; the benchmark may have been skipped. See ${LOG_FILE}" >&2
    exit 1
fi

echo
grep -E "${ROW_RE}" "${LOG_FILE}" | awk -F, -v records="${RECORDS}" -v preload="${PRELOAD}" '
BEGIN {
    printf "Records per chain: %s, preloaded receipts: %s\n\n", records, preload
    printf "%7s %9s %9s %12s %12s %9s %9s\n", "chains", "records", "seconds", "records/s", "first 1/4", "last 1/4", "p50 ms"
}
{
    gsub(/^ +/, "", $1)
    printf "%7s %9s %9s %12s %12s %9s %9s  p95 %s ms\n", $2, $4, $5, $6, $7, $8, $9, $10
}
END {
    printf "\nrecords/s: whole run; first/last 1/4: records/s per chain over the first and last quarter of the run\n"
}'

if [[ "${TEMP_LOG}" == "1" ]]; then
    echo "Full Maven output: ${LOG_FILE}"
fi
echo "CSV rows (appended on every run): ${ROOT}/service/target/benchmark/trace-ingest.csv"
