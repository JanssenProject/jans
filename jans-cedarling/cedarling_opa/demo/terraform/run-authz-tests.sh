#!/usr/bin/env bash
#
# run-authz-tests.sh — Data-driven integration test runner for tf-authz.
#
# Reads test cases from test-cases.yml (located in the same directory as this
# script), queries the Cedarling-OPA server for each case, and validates that
# the actual authorization decision matches the expected outcome.
#
# To add new test scenarios, edit test-cases.yml only.  No changes to this
# script or to the workflow YAML are required.
#
# Usage:
#   ./run-authz-tests.sh [--opa-url <url>]
#
# Environment variables (all optional):
#   OPA_URL   Base URL of the Cedarling-OPA server (default: http://localhost:8181)
#
# Exit codes:
#   0  All test cases passed.
#   1  One or more test cases failed, or a required tool is missing.
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CASES_FILE="${SCRIPT_DIR}/test-cases.yml"
OPA_URL="${OPA_URL:-http://localhost:8181}"

# ── Parse --opa-url flag ──────────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
    case "$1" in
        --opa-url)
            OPA_URL="$2"
            shift 2
            ;;
        *)
            echo "Usage: $0 [--opa-url <url>]" >&2
            exit 1
            ;;
    esac
done

# ── Verify dependencies ───────────────────────────────────────────────────────
for cmd in curl jq python3; do
    if ! command -v "${cmd}" &>/dev/null; then
        echo "run-authz-tests: ERROR — '${cmd}' is required but not installed." >&2
        exit 1
    fi
done

if [[ ! -f "${CASES_FILE}" ]]; then
    echo "run-authz-tests: ERROR — test cases file not found: ${CASES_FILE}" >&2
    exit 1
fi

# ── Convert YAML test cases to newline-delimited JSON records ─────────────────
# python3 is available on all GitHub-hosted ubuntu-latest runners and is the
# most portable way to parse YAML without an additional install step.
CASES_JSON=$(python3 - "${CASES_FILE}" <<'EOF'
import sys, json

# Minimal YAML-subset parser sufficient for test-cases.yml.
# Full PyYAML is not always available; this avoids the dependency.
def parse_cases(path):
    cases = []
    current = None
    with open(path) as f:
        for raw in f:
            line = raw.rstrip()
            stripped = line.lstrip()

            # Skip comments and the top-level key
            if not stripped or stripped.startswith('#') or stripped.startswith('test_cases:'):
                continue

            # New list item
            if stripped.startswith('- '):
                if current is not None:
                    cases.append(current)
                current = {}
                stripped = stripped[2:]

            # Key-value within an item (handles both "- key: val" and "  key: val")
            if current is not None and ':' in stripped:
                key, _, val = stripped.partition(':')
                key = key.strip()
                val = val.strip().strip('"')
                current[key] = val

    if current is not None:
        cases.append(current)
    return cases

cases = parse_cases(sys.argv[1])
for c in cases:
    print(json.dumps(c))
EOF
)

# ── Run each test case ────────────────────────────────────────────────────────
PASS=0
FAIL=0
TOTAL=0

while IFS= read -r case_json; do
    TOTAL=$((TOTAL + 1))

    NAME=$(echo "${case_json}"       | jq -r '.name        // ""')
    USER_ID=$(echo "${case_json}"    | jq -r '.user_id     // ""')
    ROLES=$(echo "${case_json}"      | jq -r '.roles       // ""')
    WORKSPACE=$(echo "${case_json}"  | jq -r '.workspace   // ""')
    SUBCOMMAND=$(echo "${case_json}" | jq -r '.subcommand  // ""')
    EXPECTED=$(echo "${case_json}"   | jq -r '.expected    // ""')

    # ── Validate required fields ──────────────────────────────────────────────
    MISSING=""
    [[ -z "${NAME}"       ]] && MISSING="${MISSING} name"
    [[ -z "${USER_ID}"    ]] && MISSING="${MISSING} user_id"
    [[ -z "${ROLES}"      ]] && MISSING="${MISSING} roles"
    [[ -z "${WORKSPACE}"  ]] && MISSING="${MISSING} workspace"
    [[ -z "${SUBCOMMAND}" ]] && MISSING="${MISSING} subcommand"
    [[ -z "${EXPECTED}"   ]] && MISSING="${MISSING} expected"
    if [[ -n "${MISSING}" ]]; then
        echo "  [FAIL] case #${TOTAL} — missing required field(s):${MISSING}" >&2
        echo "         entry: ${case_json}" >&2
        FAIL=$((FAIL + 1))
        continue
    fi
    if [[ "${EXPECTED}" != "ALLOWED" && "${EXPECTED}" != "DENIED" ]]; then
        echo "  [FAIL] ${NAME} — 'expected' must be ALLOWED or DENIED, got '${EXPECTED}'" >&2
        FAIL=$((FAIL + 1))
        continue
    fi

    # Map terraform subcommand → Cedar action
    case "${SUBCOMMAND}" in
        plan)    CEDAR_ACTION="Plan"    ;;
        apply)   CEDAR_ACTION="Apply"   ;;
        destroy) CEDAR_ACTION="Destroy" ;;
        *)
            echo "  [SKIP] ${NAME}"
            echo "         subcommand '${SUBCOMMAND}' does not require authorization."
            TOTAL=$((TOTAL - 1))
            continue
            ;;
    esac

    # Build ROLES_JSON array from comma-separated input
    ROLES_JSON=$(echo "${ROLES}" | tr ',' '\n' \
        | sed 's/^[[:space:]]*//;s/[[:space:]]*$//' \
        | jq -R . | jq -s .)

    CURRENT_TIME=$(date +%s)

    PAYLOAD=$(jq -n \
        --arg user_id   "${USER_ID}" \
        --arg workspace "${WORKSPACE}" \
        --arg action    "Infra::Action::\"${CEDAR_ACTION}\"" \
        --argjson cur_time "${CURRENT_TIME}" \
        --argjson roles "${ROLES_JSON}" \
        '{
            input: {
                principal: {
                    cedar_entity_mapping: {
                        entity_type: "Infra::User",
                        id: $user_id
                    },
                    sub:  $user_id,
                    role: $roles
                },
                action: $action,
                resource: {
                    cedar_entity_mapping: {
                        entity_type: "Infra::TerraformWorkspace",
                        id: $workspace
                    }
                },
                context: {
                    current_time: $cur_time
                }
            }
        }')

    # Query OPA
    RESPONSE=$(curl -sf --connect-timeout 5 --max-time 10 \
        -X POST "${OPA_URL}/v1/data/infra/terraform" \
        -H "Content-Type: application/json" \
        -d "${PAYLOAD}" 2>&1) || {
        echo "  [FAIL] ${NAME}"
        echo "         ERROR: could not reach OPA server at ${OPA_URL}" >&2
        FAIL=$((FAIL + 1))
        continue
    }

    DECISION=$(echo "${RESPONSE}" | jq -r '.result.allow // false')
    if [ "${DECISION}" = "true" ]; then
        ACTUAL="ALLOWED"
    else
        ACTUAL="DENIED"
    fi

    if [ "${ACTUAL}" = "${EXPECTED}" ]; then
        echo "  [PASS] ${NAME}"
        echo "         ${USER_ID} (${ROLES}) terraform ${SUBCOMMAND} on ${WORKSPACE} → ${ACTUAL}"
        PASS=$((PASS + 1))
    else
        echo "  [FAIL] ${NAME}" >&2
        echo "         ${USER_ID} (${ROLES}) terraform ${SUBCOMMAND} on ${WORKSPACE}" >&2
        echo "         expected=${EXPECTED}  actual=${ACTUAL}" >&2
        FAIL=$((FAIL + 1))
    fi

done <<< "${CASES_JSON}"

# ── Summary ───────────────────────────────────────────────────────────────────
echo ""
echo "────────────────────────────────────────────────────"
echo "Results: ${PASS} passed, ${FAIL} failed, ${TOTAL} total"
echo "────────────────────────────────────────────────────"

if [ "${FAIL}" -gt 0 ]; then
    echo "ERROR: ${FAIL} test case(s) failed." >&2
    exit 1
fi

echo "All Cedar policy test cases passed."
