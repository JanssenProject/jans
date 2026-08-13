#!/usr/bin/env bash
# Assert one OPA/Cedarling authorize decision for the terraform-jwt demo.
# Builds the authorize payload, POSTs to the local OPA, and compares the
# decision against the expected value.
#
# Usage: authz_assert.sh <jwt> <action> <workspace-id> <expect:true|false> <label>
# Env:
#   OPA_URL           default http://localhost:8181/v1/data/infra/terraform_jwt
#   OPA_COMPOSE_DIR   dir to run `docker compose logs` from on failure (optional)
#   OPA_COMPOSE_FILES `-f` args used for that log dump (optional)
set -euo pipefail

JWT=$1; ACTION=$2; WORKSPACE=$3; EXPECT=$4; LABEL=$5
OPA_URL=${OPA_URL:-http://localhost:8181/v1/data/infra/terraform_jwt}

PAYLOAD=$(jq -n \
  --arg jwt "$JWT" --argjson t "$(date +%s)" \
  --arg action "$ACTION" --arg ws "$WORKSPACE" \
  '{ input: {
       tokens: [{ mapping: "CI::GitHubWorkflow", payload: $jwt }],
       action: ("CI::Action::\"" + $action + "\""),
       resource: { cedar_entity_mapping: { entity_type: "CI::TerraformWorkspace", id: $ws } },
       context: { current_time: $t }
     } }')

echo "Authorize: ${LABEL} — ${ACTION}/${WORKSPACE}, expect ${EXPECT}"
RESPONSE=$(curl -sf --connect-timeout 5 --max-time 10 -X POST "$OPA_URL" \
  -H "Content-Type: application/json" -d "$PAYLOAD") || {
  echo "ERROR: OPA authorize endpoint did not respond." >&2; exit 1; }

echo "$RESPONSE" | jq -e '.result' > /dev/null || {
  echo "ERROR: OPA response not valid JSON / missing .result: ${RESPONSE}" >&2; exit 1; }

DECISION=$(echo "$RESPONSE" | jq -r '.result.allow // false')
echo "  decision=${DECISION}"

if [ "$DECISION" != "$EXPECT" ]; then
  echo "ERROR: ${LABEL}: expected ${EXPECT}, got ${DECISION}." >&2
  echo "$RESPONSE" | jq . >&2
  if [ -n "${OPA_COMPOSE_DIR:-}" ]; then
    ( cd "$OPA_COMPOSE_DIR" && docker compose ${OPA_COMPOSE_FILES:-} logs >&2 ) || true
  fi
  exit 1
fi
echo "  OK: ${LABEL}"
