#!/usr/bin/env bash
#
# demo-runner.sh — Exercises the Terraform authorization scenarios end-to-end.
#
# Runs a series of representative allow/deny cases against a live
# Cedarling-OPA server, printing a pass/fail summary for each.
# The TERRAFORM_BIN environment variable is set to "echo" by the demo
# container so no real Terraform binary is needed.
#
set -euo pipefail

OPA_URL="${OPA_URL:-http://localhost:8181}"
PASS=0
FAIL=0

run_case() {
    local description="$1"
    local workspace="$2"
    local user="$3"
    local roles="$4"
    local subcommand="$5"
    local expect_allow="$6"

    export TF_WORKSPACE="$workspace"
    export TF_USER_ID="$user"
    export TF_USER_ROLES="$roles"
    export OPA_URL="$OPA_URL"
    export TERRAFORM_BIN="echo"

    printf "  %-55s " "$description"

    local output exit_code
    set +e
    output=$(bash /demo/tf_authz.sh "$subcommand" 2>&1)
    exit_code=$?
    set -e

    if [ "$expect_allow" = "true" ] && [ $exit_code -eq 0 ]; then
        echo "PASS (allowed as expected)"
        PASS=$((PASS + 1))
    elif [ "$expect_allow" = "false" ] && [ $exit_code -ne 0 ]; then
        echo "PASS (denied as expected)"
        PASS=$((PASS + 1))
    else
        echo "FAIL"
        echo "    Expected allow=$expect_allow but got exit_code=$exit_code"
        echo "    Output: $output"
        FAIL=$((FAIL + 1))
    fi
}

echo ""
echo "======================================================"
echo " Cedarling-OPA Terraform Authorization Demo"
echo "======================================================"
echo ""
echo "OPA server: $OPA_URL"
echo ""

echo "Waiting for OPA to be ready..."
until curl -sf "$OPA_URL/health" >/dev/null 2>&1; do
    sleep 1
done
echo "OPA is ready."
echo ""

echo "--- Developer scenarios ---"
run_case "Developer: plan on production (allowed)"    production alice Developer plan    true
run_case "Developer: apply on production (denied)"   production alice Developer apply   false
run_case "Developer: destroy on production (denied)" production alice Developer destroy false

echo ""
echo "--- Ops scenarios ---"
run_case "Ops: plan on staging (allowed)"            staging    bob   Ops       plan    true
run_case "Ops: apply on staging (allowed)"           staging    bob   Ops       apply   true
run_case "Ops: apply on production (denied)"         production bob   Ops       apply   false
run_case "Ops: destroy on staging (denied)"          staging    bob   Ops       destroy false

echo ""
echo "--- Admin scenarios ---"
run_case "Admin: plan on production (allowed)"       production carol Admin     plan    true
run_case "Admin: apply on production (allowed)"      production carol Admin     apply   true
run_case "Admin: destroy on production (allowed)"    production carol Admin     destroy true

echo ""
echo "======================================================"
printf " Results: %d passed, %d failed\n" "$PASS" "$FAIL"
echo "======================================================"
echo ""

if [ $FAIL -gt 0 ]; then
    exit 1
fi
