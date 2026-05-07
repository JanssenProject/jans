#!/usr/bin/env bash
#
# tf_authz.sh — Terraform authorization wrapper using Cedarling-OPA
#
# Before delegating to the real `terraform` binary, this script sends an
# authorization request to the Cedarling-OPA server. The OPA server evaluates
# Cedar policies that govern who may run plan / apply / destroy and on which
# workspace environment.
#
# Required environment variables:
#   TF_WORKSPACE   Target workspace name (dev | staging | production)
#   TF_USER_ID     Identity of the operator (e.g. "alice", a CI service account)
#   TF_USER_ROLES  Comma-separated Cedar role names (e.g. "Developer,Ops")
#
# Optional environment variables:
#   OPA_URL        OPA server base URL (default: http://localhost:8181)
#   TERRAFORM_BIN  Path to the terraform binary (default: terraform)
#
# Usage:
#   ./tf_authz.sh plan
#   ./tf_authz.sh apply -auto-approve
#   ./tf_authz.sh -chdir=environments/prod apply
#   ./tf_authz.sh destroy -auto-approve
#
# For commands other than plan / apply / destroy, the auth check is skipped and
# terraform is called directly (e.g. terraform init, terraform fmt, etc.).
#
# OPA endpoint:
#   The script queries /v1/data/infra/terraform (the full package) rather than
#   /v1/data/infra/terraform/allow so that reasons and Cedarling errors are
#   all available in a single round-trip.
#
set -euo pipefail

OPA_URL="${OPA_URL:-http://localhost:8181}"
TF_WORKSPACE="${TF_WORKSPACE:-dev}"
TF_USER_ID="${TF_USER_ID:-${USER:-unknown}}"
TF_USER_ROLES="${TF_USER_ROLES:-Developer}"
TERRAFORM_BIN="${TERRAFORM_BIN:-terraform}"

# ─── Find the first non-flag Terraform sub-command ───────────────────────────
# Terraform global flags (e.g. -chdir=, -help, -version) may appear before the
# sub-command. Iterate through all arguments and pick the first non-flag token.
# Also capture -chdir= so we know where to look for the .terraform/ cache.
TF_SUBCOMMAND=""
CHDIR_ARG=""
for arg in "$@"; do
    case "${arg}" in
        -chdir=*)
            CHDIR_ARG="${arg}"
            ;;
        -*)
            ;;
        *)
            if [[ -z "${TF_SUBCOMMAND}" ]]; then
                TF_SUBCOMMAND="${arg}"
            fi
            ;;
    esac
done
# Default to plan when no sub-command is present (mirrors terraform's behaviour).
TF_SUBCOMMAND="${TF_SUBCOMMAND:-plan}"

# ─── Map terraform sub-command → Cedar action ────────────────────────────────
case "${TF_SUBCOMMAND}" in
    plan)    CEDAR_ACTION="Plan" ;;
    apply)   CEDAR_ACTION="Apply" ;;
    destroy) CEDAR_ACTION="Destroy" ;;
    *)
        # No authorization check needed for init, fmt, validate, output, etc.
        exec "${TERRAFORM_BIN}" "$@"
        ;;
esac

# ─── Verify dependencies ─────────────────────────────────────────────────────
for cmd in curl jq; do
    if ! command -v "${cmd}" &>/dev/null; then
        echo "tf_authz: ERROR — '${cmd}' is required but not installed." >&2
        exit 1
    fi
done

# ─── Build the OPA query payload ─────────────────────────────────────────────

# Convert comma-separated roles to a JSON array
ROLES_JSON=$(echo "${TF_USER_ROLES}" | tr ',' '\n' | sed 's/^[[:space:]]*//;s/[[:space:]]*$//' | jq -R . | jq -s .)

PAYLOAD=$(jq -n \
    --arg user_id   "${TF_USER_ID}" \
    --arg workspace "${TF_WORKSPACE}" \
    --arg action    "Infra::Action::\"${CEDAR_ACTION}\"" \
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
            }
        }
    }')

# ─── Display authorization context ───────────────────────────────────────────
echo "tf_authz: checking authorization..."
echo "  User:      ${TF_USER_ID} (roles: ${TF_USER_ROLES})"
echo "  Action:    terraform ${TF_SUBCOMMAND}  →  Infra::Action::\"${CEDAR_ACTION}\""
echo "  Workspace: ${TF_WORKSPACE}  →  Infra::TerraformWorkspace::\"${TF_WORKSPACE}\""
echo ""

# ─── Query the Cedarling-OPA server ─────────────────────────────────────────
RESPONSE=$(curl -sf --connect-timeout 5 --max-time 10 \
    -X POST "${OPA_URL}/v1/data/infra/terraform" \
    -H "Content-Type: application/json" \
    -d "${PAYLOAD}") || {
    echo "tf_authz: ERROR — could not reach OPA server at ${OPA_URL}" >&2
    echo "          Make sure opa-cedarling is running (see README.md)." >&2
    exit 1
}

DECISION=$(echo "${RESPONSE}" | jq -r '.result.allow // false')

# ─── Enforce the decision ────────────────────────────────────────────────────
if [ "${DECISION}" = "true" ]; then
    MATCHING_POLICIES=$(echo "${RESPONSE}" | jq -r '.result.result.reasons // [] | join(", ")')
    echo "tf_authz: ALLOWED"
    if [ -n "${MATCHING_POLICIES}" ]; then
        echo "  Granted by Cedar policy: ${MATCHING_POLICIES}"
    fi
    echo ""

    # ─── Auto-initialize provider plugins if not yet downloaded ──────────────
    # When a real Terraform binary is in use, check whether the provider cache
    # exists.  If it doesn't (first run, or after a fresh clone), run
    # `terraform init` automatically so the user never has to do it manually.
    # The TERRAFORM_BIN=echo path used by the demo container skips this step.
    if [[ "${TERRAFORM_BIN}" != "echo" ]]; then
        # Honour -chdir= when deciding where .terraform/providers lives.
        _TF_WORK_DIR="${CHDIR_ARG:+${CHDIR_ARG#-chdir=}}"
        _TF_WORK_DIR="${_TF_WORK_DIR:-.}"
        if [[ ! -d "${_TF_WORK_DIR}/.terraform/providers" ]]; then
            echo "tf_authz: provider cache not found — running 'terraform init'..."
            "${TERRAFORM_BIN}" ${CHDIR_ARG:+"${CHDIR_ARG}"} init
            echo ""
        fi
    fi

    # ─── Rebuild args with -chdir= first (it is a global flag) ──────────────
    # terraform requires global flags to precede the sub-command.  Reconstruct
    # the argument list so -chdir= always comes first, regardless of the order
    # the caller used (e.g. `tf_authz.sh destroy -chdir=...` is valid input).
    _tf_args=()
    for _arg in "$@"; do
        [[ "${_arg}" == -chdir=* ]] && continue   # injected below as first arg
        _tf_args+=("${_arg}")
    done
    exec "${TERRAFORM_BIN}" ${CHDIR_ARG:+"${CHDIR_ARG}"} "${_tf_args[@]}"
else
    echo "tf_authz: DENIED" >&2
    echo "" >&2
    echo "  ${TF_USER_ID} is not permitted to run 'terraform ${TF_SUBCOMMAND}'" >&2
    echo "  on workspace '${TF_WORKSPACE}'." >&2
    echo "" >&2
    echo "  No Cedar policy allows this operation." >&2

    # Surface any Cedarling evaluation errors that may explain the denial.
    CEDAR_ERRORS=$(echo "${RESPONSE}" | jq -r '.result.result.errors // [] | join(", ")')
    if [ -n "${CEDAR_ERRORS}" ]; then
        echo "" >&2
        echo "  Cedarling evaluation errors: ${CEDAR_ERRORS}" >&2
    fi

    exit 1
fi
