#!/usr/bin/env bash
#
# tf_authz_jwt.sh — JWT-based Terraform authorization wrapper using Cedarling-OPA
#
# This script is a CI-first variant of tf_authz.sh that authenticates the caller
# via a signed OIDC JWT rather than self-asserted environment variables. In
# GitHub Actions, the pipeline receives a GitHub-issued OIDC token automatically
# (when `permissions: id-token: write` is set). That token contains claims such
# as `repository`, `ref`, and `environment` that Cedarling verifies cryptographically
# before evaluating the Cedar policies.
#
# The script sends a `cedarling.opa.authorize_multi_issuer` request to OPA.
# Cedarling validates the JWT signature against GitHub's JWKS endpoint, maps the
# verified claims to a CI::GitHubWorkflow Cedar entity, and evaluates the policies.
#
# Required environment variables:
#   TF_JWT         Signed OIDC JWT (e.g. the GitHub Actions ID token).
#                  In GitHub Actions, fetch this with tf_authz_jwt.sh --fetch-token
#                  or set it manually from $ACTIONS_ID_TOKEN_REQUEST_URL.
#
# Optional environment variables:
#   TF_WORKSPACE   Target workspace name (dev | staging | production)
#                  Default: dev
#   OPA_URL        OPA server base URL. Default: http://localhost:8181
#   TERRAFORM_BIN  Path to the terraform binary. Default: terraform
#
# Usage:
#   export TF_JWT="$(./tf_authz_jwt.sh --fetch-token)"
#   ./tf_authz_jwt.sh plan
#   ./tf_authz_jwt.sh apply -auto-approve
#   ./tf_authz_jwt.sh -chdir=environments/prod apply
#   ./tf_authz_jwt.sh destroy -auto-approve
#
# GitHub Actions usage (see github-actions-example.yml for a full workflow):
#   - name: Authorize and run Terraform
#     env:
#       TF_JWT: ${{ steps.get-token.outputs.token }}
#       TF_WORKSPACE: production
#     run: ./demo/terraform-jwt/tf_authz_jwt.sh apply -auto-approve
#
# For commands other than plan / apply / destroy, the auth check is skipped and
# terraform is called directly (e.g. terraform init, terraform fmt, etc.).
#
set -euo pipefail

OPA_URL="${OPA_URL:-http://localhost:8181}"
TF_WORKSPACE="${TF_WORKSPACE:-dev}"
TERRAFORM_BIN="${TERRAFORM_BIN:-terraform}"

# ─── --fetch-token helper ─────────────────────────────────────────────────────
# When called as `./tf_authz_jwt.sh --fetch-token`, request a GitHub OIDC token
# and print it to stdout. The caller should capture it into TF_JWT:
#   export TF_JWT="$(./tf_authz_jwt.sh --fetch-token)"
#
if [[ "${1:-}" == "--fetch-token" ]]; then
    if [[ -z "${ACTIONS_ID_TOKEN_REQUEST_URL:-}" || -z "${ACTIONS_ID_TOKEN_REQUEST_TOKEN:-}" ]]; then
        echo "tf_authz_jwt: ERROR — ACTIONS_ID_TOKEN_REQUEST_URL and ACTIONS_ID_TOKEN_REQUEST_TOKEN" >&2
        echo "              must be set. These are only available inside GitHub Actions jobs" >&2
        echo "              that have 'permissions: id-token: write'." >&2
        exit 1
    fi
    # GitHub's token endpoint returns { "count": N, "value": "<jwt>" }
    TOKEN=$(curl -sf \
        -H "Authorization: bearer ${ACTIONS_ID_TOKEN_REQUEST_TOKEN}" \
        "${ACTIONS_ID_TOKEN_REQUEST_URL}&audience=cedarling-terraform" \
        | jq -r '.value')
    if [[ -z "${TOKEN}" || "${TOKEN}" == "null" ]]; then
        echo "tf_authz_jwt: ERROR — failed to obtain GitHub OIDC token." >&2
        exit 1
    fi
    echo "${TOKEN}"
    exit 0
fi

# ─── Find the first non-flag Terraform sub-command ───────────────────────────
# This must happen before the JWT check so that non-auth commands (init, fmt,
# validate, output, etc.) can pass through without requiring TF_JWT.
# Also capture -chdir= so we can honour it for auto-init and arg reordering.
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
TF_SUBCOMMAND="${TF_SUBCOMMAND:-plan}"

# ─── Map terraform sub-command → Cedar action ────────────────────────────────
case "${TF_SUBCOMMAND}" in
    plan)    CEDAR_ACTION="Plan" ;;
    apply)   CEDAR_ACTION="Apply" ;;
    destroy) CEDAR_ACTION="Destroy" ;;
    *)
        # No authorization check for init, fmt, validate, output, etc.
        exec "${TERRAFORM_BIN}" "$@"
        ;;
esac

# ─── Require the JWT (only for plan / apply / destroy) ───────────────────────
if [[ -z "${TF_JWT:-}" ]]; then
    echo "tf_authz_jwt: ERROR — TF_JWT is not set." >&2
    echo "" >&2
    echo "  In GitHub Actions, fetch the token first:" >&2
    echo "    export TF_JWT=\"\$(./tf_authz_jwt.sh --fetch-token)\"" >&2
    echo "" >&2
    echo "  For local testing, set TF_JWT to a pre-crafted JWT (see README)." >&2
    exit 1
fi

# ─── Verify dependencies ─────────────────────────────────────────────────────
for cmd in curl jq; do
    if ! command -v "${cmd}" &>/dev/null; then
        echo "tf_authz_jwt: ERROR — '${cmd}' is required but not installed." >&2
        exit 1
    fi
done

# ─── Decode JWT claims for display (no signature check here — Cedarling does it) ──
JWT_PAYLOAD_B64=$(echo "${TF_JWT}" | cut -d'.' -f2)
# Base64url → base64 padding fix
JWT_PAYLOAD=$(echo "${JWT_PAYLOAD_B64}" | tr -- '-_' '+/' | awk '{
    n = length($0) % 4;
    if (n == 2) print $0 "==";
    else if (n == 3) print $0 "=";
    else print $0;
}' | base64 -d 2>/dev/null || echo '{}')

JWT_REPO=$(echo "${JWT_PAYLOAD}" | jq -r '.repository // "unknown"')
JWT_REF=$(echo "${JWT_PAYLOAD}" | jq -r '.ref // "unknown"')
JWT_ENV=$(echo "${JWT_PAYLOAD}" | jq -r '.environment // "(none)"')
JWT_SUB=$(echo "${JWT_PAYLOAD}" | jq -r '.sub // "unknown"')

# ─── Build the authorize_multi_issuer payload ─────────────────────────────────
CURRENT_TIME=$(date +%s)

PAYLOAD=$(jq -n \
    --arg jwt       "${TF_JWT}" \
    --arg workspace "${TF_WORKSPACE}" \
    --arg action    "CI::Action::\"${CEDAR_ACTION}\"" \
    --argjson cur   "${CURRENT_TIME}" \
    '{
        input: {
            tokens: [
                {
                    mapping: "CI::GitHubWorkflow",
                    payload: $jwt
                }
            ],
            action: $action,
            resource: {
                cedar_entity_mapping: {
                    entity_type: "CI::TerraformWorkspace",
                    id: $workspace
                }
            },
            context: {
                current_time: $cur
            }
        }
    }')

# ─── Display authorization context ───────────────────────────────────────────
echo "tf_authz_jwt: checking authorization..."
echo "  Repository: ${JWT_REPO}"
echo "  Ref:        ${JWT_REF}"
echo "  Environment:${JWT_ENV}"
echo "  Sub:        ${JWT_SUB}"
echo "  Action:     terraform ${TF_SUBCOMMAND}  →  CI::Action::\"${CEDAR_ACTION}\""
echo "  Workspace:  ${TF_WORKSPACE}  →  CI::TerraformWorkspace::\"${TF_WORKSPACE}\""
echo ""

# ─── Query the Cedarling-OPA server ──────────────────────────────────────────
RESPONSE=$(curl -sf -X POST "${OPA_URL}/v1/data/infra/terraform_jwt" \
    -H "Content-Type: application/json" \
    -d "${PAYLOAD}") || {
    echo "tf_authz_jwt: ERROR — could not reach OPA server at ${OPA_URL}" >&2
    echo "              Make sure opa-cedarling is running (see README.md)." >&2
    exit 1
}

DECISION=$(echo "${RESPONSE}" | jq -r '.result.allow // false')

# ─── Enforce the decision ────────────────────────────────────────────────────
if [ "${DECISION}" = "true" ]; then
    MATCHING_POLICIES=$(echo "${RESPONSE}" | jq -r '.result.result.reasons // [] | join(", ")')
    echo "tf_authz_jwt: ALLOWED"
    if [ -n "${MATCHING_POLICIES}" ]; then
        echo "  Granted by Cedar policy: ${MATCHING_POLICIES}"
    fi
    echo ""

    # ─── Auto-initialize provider plugins if not yet downloaded ──────────────
    if [[ "${TERRAFORM_BIN}" != "echo" ]]; then
        _TF_WORK_DIR="${CHDIR_ARG:+${CHDIR_ARG#-chdir=}}"
        _TF_WORK_DIR="${_TF_WORK_DIR:-.}"
        if [[ ! -d "${_TF_WORK_DIR}/.terraform/providers" ]]; then
            echo "tf_authz_jwt: provider cache not found — running 'terraform init'..."
            "${TERRAFORM_BIN}" ${CHDIR_ARG:+"${CHDIR_ARG}"} init
            echo ""
        fi
    fi

    # ─── Rebuild args with -chdir= first (global flag must precede sub-command)
    _tf_args=()
    for _arg in "$@"; do
        [[ "${_arg}" == -chdir=* ]] && continue
        _tf_args+=("${_arg}")
    done
    exec "${TERRAFORM_BIN}" ${CHDIR_ARG:+"${CHDIR_ARG}"} "${_tf_args[@]}"
else
    echo "tf_authz_jwt: DENIED" >&2
    echo "" >&2
    echo "  The pipeline is not permitted to run 'terraform ${TF_SUBCOMMAND}'" >&2
    echo "  on workspace '${TF_WORKSPACE}'." >&2
    echo "" >&2
    echo "  JWT claims presented:" >&2
    echo "    repository:  ${JWT_REPO}" >&2
    echo "    ref:         ${JWT_REF}" >&2
    echo "    environment: ${JWT_ENV}" >&2
    echo "" >&2
    echo "  No Cedar policy allows this combination of claims, action, and workspace." >&2

    CEDAR_ERRORS=$(echo "${RESPONSE}" | jq -r '.result.result.errors // [] | join(", ")')
    if [ -n "${CEDAR_ERRORS}" ]; then
        echo "" >&2
        echo "  Cedarling evaluation errors: ${CEDAR_ERRORS}" >&2
    fi

    exit 1
fi
