#!/bin/bash

# =============================================================================
# Automatic endpoint test for the Lock Server: REST ↔ gRPC bridge + API authorization
#
# Audit (log, health, telemetry; single + bulk; write & read scopes), over REST and gRPC.
# TRACE (producer keys and chains, single + bulk submit, record/execution/search retrieval,
# scope checks), over REST and gRPC, including records written through one transport and
# read through the other.
#
# Requirements: bash 4+, curl, jq, grpcurl. The TRACE section also needs python3 with the
# `cryptography` package (or `uv`) to sign records.
#
# Configuration: automatic_enpoints_tests.properties (this folder) holds the defaults.
# Values can be overridden, highest priority first, by
#   * environment variables LOCK_<KEY>, for example LOCK_TRACE_CLIENT_SECRET
#   * automatic_enpoints_tests.local.properties in this folder (git-ignored)
#   * the client test profile
#     ../../../../client/profiles/<test.profile>/config-jans-lock-test-data.properties
# See the comments in the properties file. A section whose client is not configured is skipped.
#
# TRACE keys and chains cannot be deleted from Lock: every run registers new, uniquely named
# keys and chains and revokes the keys at the end.
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOCK_SERVER_DIR="$(cd "${SCRIPT_DIR}/../../../.." && pwd)"
PROFILES_DIR="${LOCK_SERVER_DIR}/client/profiles"
PROTO_DIR="${LOCK_SERVER_DIR}/service/src/main/proto/grpc"
DEFAULTS_FILE="${SCRIPT_DIR}/automatic_enpoints_tests.properties"
LOCAL_FILE="${SCRIPT_DIR}/automatic_enpoints_tests.local.properties"

# ─────────────────────────────────────────────────────────────────────────────
# Configuration
# ─────────────────────────────────────────────────────────────────────────────

declare -A DEFAULT_PROPS LOCAL_PROPS PROFILE_PROPS

trim() {
  local value="$1"
  value="${value#"${value%%[![:space:]]*}"}"
  printf '%s' "${value%"${value##*[![:space:]]}"}"
}

# load_properties FILE ARRAY_NAME: plain key=value lines; the file is read, never executed
load_properties() {
  local file="$1" line key
  local -n target="$2"
  [[ -f "$file" ]] || return 0
  while IFS= read -r line || [[ -n "$line" ]]; do
    line="${line%$'\r'}"
    [[ "$line" =~ ^[[:space:]]*(#|$) ]] && continue
    [[ "$line" == *=* ]] || continue
    key="$(trim "${line%%=*}")"
    target["$key"]="$(trim "${line#*=}")"
  done <"$file"
}

# prop KEY [DEFAULT]: environment > local file > profile > defaults file > DEFAULT
prop() {
  local key="$1" env_name
  env_name="LOCK_${key^^}"
  env_name="${env_name//[.-]/_}"
  if [[ -n "${!env_name:-}" ]]; then
    printf '%s' "${!env_name}"
  elif [[ -n "${LOCAL_PROPS[$key]:-}" ]]; then
    printf '%s' "${LOCAL_PROPS[$key]}"
  elif [[ -n "${PROFILE_PROPS[$key]:-}" ]]; then
    printf '%s' "${PROFILE_PROPS[$key]}"
  elif [[ -n "${DEFAULT_PROPS[$key]:-}" ]]; then
    printf '%s' "${DEFAULT_PROPS[$key]}"
  else
    printf '%s' "${2:-}"
  fi
}

is_configured() { [[ -n "$1" && "$1" != CHANGE_ME* ]]; }
is_true() { [[ "${1,,}" == "true" || "$1" == "1" ]]; }

load_properties "$DEFAULTS_FILE" DEFAULT_PROPS
load_properties "$LOCAL_FILE" LOCAL_PROPS

TEST_PROFILE="$(prop test.profile)"
PROFILE_FILE="${PROFILES_DIR}/${TEST_PROFILE}/config-jans-lock-test-data.properties"
if [[ -n "$TEST_PROFILE" && -f "$PROFILE_FILE" ]]; then
  load_properties "$PROFILE_FILE" PROFILE_PROPS
  echo "Profile '${TEST_PROFILE}' loaded from ${PROFILE_FILE}"
else
  echo "Profile '${TEST_PROFILE}' not found, using the properties files and the environment only"
fi

SERVER_NAME="$(prop test.server.name)"
HOST="https://${SERVER_NAME}"
GRPC_ADDR="$(prop grpc.address "${SERVER_NAME}:443")"
AUDIT_CLIENT_ID="$(prop audit.client.id)"
AUDIT_CLIENT_SECRET="$(prop audit.client.secret)"
TRACE_CLIENT_ID="$(prop trace.client.id)"
TRACE_CLIENT_SECRET="$(prop trace.client.secret)"
INVALID_TOKEN="$(prop invalid.token this-is-definitely-not-a-valid-token)"

CURL_TLS=""
GRPC_TLS=()
if is_true "$(prop tls.insecure true)"; then
  CURL_TLS="-k"
  GRPC_TLS=(-insecure)
fi
GRPCURL_CMD=(grpcurl ${GRPC_TLS[@]+"${GRPC_TLS[@]}"} -import-path "$PROTO_DIR")
GRPCURL="${GRPCURL_CMD[*]}"

for tool in curl jq grpcurl; do
  command -v "$tool" >/dev/null 2>&1 || { echo "❌ required tool not found: $tool"; exit 1; }
done

if ! is_configured "$SERVER_NAME" || [[ "$SERVER_NAME" == "server.example.org" ]]; then
  echo "❌ test.server.name is not configured (properties file, profile or LOCK_TEST_SERVER_NAME)"
  exit 1
fi

WORK="$(mktemp -d)"
chmod 700 "$WORK"
trap 'rm -rf "$WORK"' EXIT
RES_FILE="$WORK/res.txt"

# ─────────────────────────────────────────────────────────────────────────────
# Test data — REST format
# ─────────────────────────────────────────────────────────────────────────────

# Log
SINGLE_LOG_JSON_REST='{
    "creationDate": "2024-04-21T18:25:43-05:00",
    "eventTime":   "2024-04-21T18:25:43-05:00",
    "service":      "jans-auth",
    "nodeName":    "node-1",
    "eventType":   "registration",
    "severityLevel": "warning",
    "action":       "LOGIN_ATTEMPT",
    "decisionResult": "allow",
    "requestedResource": "{\"res\":\"/api/user\",\"method\":\"POST\"}",
    "principalId": "ACC-000123",
    "clientId":    "CLI-001",
    "jti":          "test-jti-123456",
    "contextInformation": {
      "ip": "192.168.1.77",
      "user_agent": "Mozilla/5.0 (test)"
    }
}'

BULK_LOG_JSON_REST='[
   {
      "creationDate": "2024-04-21T18:25:43-05:00",
      "eventTime":   "2024-04-21T18:25:43-05:00",
      "service":      "jans-auth",
      "nodeName":    "node-1",
      "eventType":   "registration",
      "severityLevel": "warning",
      "action":       "LOGIN_ATTEMPT",
      "decisionResult": "deny",
      "requestedResource": "{\"res\":\"/api/admin\"}",
      "principalId": "ACC-000123",
      "clientId":    "CLI-001",
      "contextInformation": {"ip": "10.0.0.5"}
    },
    {
      "creationDate": "2024-06-15T14:10:22Z",
      "eventTime":   "2024-06-15T14:10:22Z",
      "service":      "jans-auth",
      "nodeName":    "node-2",
      "eventType":   "transaction",
      "severityLevel": "info",
      "action":       "PAYMENT",
      "decisionResult": "allow",
      "requestedResource": "{\"amount\":99.99}",
      "principalId": "ACC-000123",
      "clientId":    "CLI-001"
    }
]'

# Health
SINGLE_HEALTH_JSON_REST='{
  "creationDate": "2024-04-21T17:25:43-05:00",
  "eventTime":    "2024-04-21T18:25:43-05:00",
  "service":      "jans-auth",
  "nodeName":     "node-1",
  "status":       "ok",
  "engineStatus": {
    "cedarling": "up",
    "policy_store": "loaded"
  }
}'

BULK_HEALTH_JSON_REST='[
  {
    "creationDate": "2024-04-21T17:25:43-05:00",
    "eventTime":    "2024-04-21T18:25:43-05:00",
    "service":      "jans-auth",
    "nodeName":     "node-1",
    "status":       "ok",
    "engineStatus": {
      "cedarling": "up",
      "policy_store": "loaded"
    }
  },
  {
    "creationDate": "2024-04-21T17:30:00-05:00",
    "eventTime":    "2024-04-21T18:30:00-05:00",
    "service":      "jans-lock",
    "nodeName":     "node-2",
    "status":       "degraded",
    "engineStatus": {
      "cedarling": "down"
    }
  }
]'

# Telemetry
SINGLE_TELEMETRY_JSON_REST='{
  "creationDate":  "2024-04-21T18:00:00-05:00",
  "service":       "jans-auth",
  "nodeName":      "node-1",
  "status":        "ok",
  "policyStats": {
    "policy_success_load_counter": 100,
    "policy_failed_load_counter":  3,
    "last_policy_load_size":       1024
  },
  "errorCounters": {
    "policy_load_errors":  3,
    "evaluation_errors":   0
  },
  "operationalStats": {
    "evaluation_requests_count":      100,
    "last_policy_evaluation_time_ns": 100000,
    "avg_policy_evaluation_time_ns":  75000,
    "memory_usage":                   2097152
  },
  "intervalSecs":  60
}'

BULK_TELEMETRY_JSON_REST='[
  {
    "creationDate":  "2024-04-21T18:00:00-05:00",
    "service":       "jans-auth",
    "nodeName":      "node-1",
    "status":        "ok",
    "policyStats": {
      "policy_success_load_counter": 100,
      "policy_failed_load_counter":  3,
      "last_policy_load_size":       1024
    },
    "errorCounters": {
      "policy_load_errors":  3,
      "evaluation_errors":   0
    },
    "operationalStats": {
      "evaluation_requests_count":      100,
      "last_policy_evaluation_time_ns": 100000,
      "avg_policy_evaluation_time_ns":  75000,
      "memory_usage":                   2097152
    },
    "intervalSecs":  60
  },
  {
    "creationDate":  "2024-04-21T19:00:00Z",
    "service":       "jans-lock",
    "nodeName":      "node-2",
    "status":        "ok",
    "policyStats": {
      "policy_success_load_counter": 250,
      "policy_failed_load_counter":  1,
      "last_policy_load_size":       2048
    },
    "errorCounters": {
      "policy_load_errors":  1,
      "evaluation_errors":   2
    },
    "operationalStats": {
      "evaluation_requests_count":      420,
      "last_policy_evaluation_time_ns": 85000,
      "avg_policy_evaluation_time_ns":  92000,
      "memory_usage":                   4194304
    },
    "intervalSecs":  60
  }
]'

# gRPC wrappers
SINGLE_LOG_GRPC='{ "entry": '"${SINGLE_LOG_JSON_REST}"' }'
BULK_LOG_GRPC='{ "entries": '"${BULK_LOG_JSON_REST}"' }'

SINGLE_HEALTH_GRPC='{ "entry": '"${SINGLE_HEALTH_JSON_REST}"' }'
BULK_HEALTH_GRPC='{ "entries": '"${BULK_HEALTH_JSON_REST}"' }'

SINGLE_TELEMETRY_GRPC='{ "entry": '"${SINGLE_TELEMETRY_JSON_REST}"' }'
BULK_TELEMETRY_GRPC='{ "entries": '"${BULK_TELEMETRY_JSON_REST}"' }'

# ─────────────────────────────────────────────────────────────────────────────
# Helper functions
# ─────────────────────────────────────────────────────────────────────────────

check_http_success() {
  local cmd="$1"
  local desc="$2"
  local status
  status=$(eval "$cmd" -s -o /dev/null -w "%{http_code}")
  if [[ $status -ge 200 && $status -lt 300 ]]; then
    echo "✅  $desc (HTTP $status)"
  else
    echo "❌  $desc → HTTP $status"
    echo "Command: $cmd"
    return 1
  fi
}

check_http_should_fail() {
  local cmd="$1"
  local desc="$2"
  local status
  status=$(eval "$cmd" -s -o /dev/null -w "%{http_code}")
  if [[ $status -ge 401 && $status -le 403 ]]; then
    echo "✅ (expected failure) $desc (HTTP $status)"
  else
    echo "❌ UNEXPECTED success → $desc (HTTP $status)"
    echo "Command: $cmd"
    return 1
  fi
}

check_grpc_success() {
  local cmd="$1"
  local desc="$2"
  > "$RES_FILE"
  if eval "$cmd" >"$RES_FILE" 2>&1; then
    echo "✅  $desc"
  else
    echo "❌  $desc"
    echo "Command: $cmd"
    echo "Output:"
    cat "$RES_FILE"
    return 1
  fi
}

check_grpc_should_fail() {
  local cmd="$1"
  local desc="$2"
  > "$RES_FILE"
  if eval "$cmd" >"$RES_FILE" 2>&1; then
    echo "❌ UNEXPECTED success → $desc"
    echo "Command: $cmd"
    echo "Output:"
    cat "$RES_FILE"
    return 1
  else
    local output
    output=$(cat "$RES_FILE")
    if echo "$output" | grep -q -i "PermissionDenied" || echo "$output" | grep -q -i "Invalid token" || echo "$output" | grep -q -i "unauthenticated"; then
      local code
      code=$(echo "$output" | grep -i "Code:" | head -n 1 | sed -E 's/.*Code:[[:space:]]*([^[:space:],().]+).*/\1/i')
      echo "✅ (expected failure) $desc with status: $code"
    else
      echo "❌ UNEXPECTED gRPC error → $desc"
      echo "Command: $cmd"
      echo "Output:"
      echo "$output"
      return 1
    fi
  fi
}

# ─────────────────────────────────────────────────────────────────────────────
# TRACE tests
# ─────────────────────────────────────────────────────────────────────────────

SCOPE_TRACE_ADMIN="https://jans.io/oauth/lock/trace.admin"
SCOPE_TRACE_WRITE="https://jans.io/oauth/lock/trace.write"
SCOPE_TRACE_READ="https://jans.io/oauth/lock/trace.readonly"
ZERO_HASH="sha256:0000000000000000000000000000000000000000000000000000000000000000"

HTTP_STATUS=""
HTTP_BODY=""
GRPC_RC=0
GRPC_OUT=""

t_pass() { echo "✅  $1"; }
t_fail() { echo "❌  $1"; [[ -n "${2:-}" ]] && echo "    $2"; exit 1; }

get_token() { # CLIENT_ID CLIENT_SECRET SCOPE: credentials go to curl through stdin, not the process list
  printf 'user = "%s:%s"\n' "$1" "$2" \
    | curl -sS $CURL_TLS -K - "${HOST}/jans-auth/restv1/token" -d "grant_type=client_credentials" -d "scope=$3" \
    | jq -r '.access_token // empty'
}

get_audit_token() { get_token "$AUDIT_CLIENT_ID" "$AUDIT_CLIENT_SECRET" "$1"; }

rest() { # METHOD URL TOKEN [curl args...] -> HTTP_STATUS, HTTP_BODY; an empty TOKEN sends no Authorization header
  local method="$1" url="$2" token="$3" out="$WORK/response.body" cfg="$WORK/request.curlrc" auth=()
  shift 3
  if [[ -n "$token" ]]; then
    printf 'header = "Authorization: Bearer %s"\n' "$token" >"$cfg"
    auth=(-K "$cfg")
  fi
  HTTP_STATUS="$(curl -sS $CURL_TLS -o "$out" -w '%{http_code}' -X "$method" ${auth[@]+"${auth[@]}"} "$@" "$url")" || HTTP_STATUS="000"
  rm -f "$cfg"
  HTTP_BODY="$(cat "$out" 2>/dev/null || true)"
}

rest_post() { rest POST "$1" "$2" -H 'Content-Type: application/json' --data-binary "$3"; } # URL TOKEN BODY

grpc() { # SERVICE/METHOD TOKEN JSON -> GRPC_RC, GRPC_OUT (stdout and stderr of grpcurl)
  local method="$1" token="$2" json="$3" args=()
  [[ -n "$token" ]] && args=(-H "authorization: Bearer ${token}")
  GRPC_RC=0
  GRPC_OUT="$("${GRPCURL_CMD[@]}" -proto trace.proto -emit-defaults ${args[@]+"${args[@]}"} -d @ "$GRPC_ADDR" \
    "io.jans.lock.trace.${method}" <<<"$json" 2>&1)" || GRPC_RC=$?
}

expect_http() { # DESC STATUS [ERROR_ID]
  local desc="$1" want="$2" err="${3:-}" got
  [[ "$HTTP_STATUS" == "$want" ]] || t_fail "REST  $desc (HTTP $HTTP_STATUS, expected $want)" "$HTTP_BODY"
  if [[ -n "$err" ]]; then
    got="$(jq -r '.error // empty' <<<"$HTTP_BODY" 2>/dev/null || true)"
    [[ "$got" == "$err" ]] || t_fail "REST  $desc (HTTP $HTTP_STATUS, error '$got', expected '$err')" "$HTTP_BODY"
  fi
  t_pass "REST  $desc (HTTP $HTTP_STATUS${err:+, $err})"
}

expect_http_denied() { # DESC: 401 or 403
  [[ "$HTTP_STATUS" == "401" || "$HTTP_STATUS" == "403" ]] \
    || t_fail "REST  UNEXPECTED $1 (HTTP $HTTP_STATUS, expected 401/403)" "$HTTP_BODY"
  t_pass "REST  (expected failure) $1 (HTTP $HTTP_STATUS)"
}

grpc_error_code() { sed -n 's/^[[:space:]]*Code:[[:space:]]*//p' <<<"$GRPC_OUT" | head -n 1; }

grpc_error_id() { # TRACE error id carried in the status message
  sed -n '/Message:/,$p' <<<"$GRPC_OUT" | sed '1s/.*Message:[[:space:]]*//' | jq -r '.error // empty' 2>/dev/null || true
}

expect_grpc() { # DESC
  [[ "$GRPC_RC" == "0" ]] || t_fail "gRPC  $1" "$GRPC_OUT"
  t_pass "gRPC  $1"
}

expect_grpc_error() { # DESC CODE [ERROR_ID]
  local desc="$1" want="$2" err="${3:-}" code
  [[ "$GRPC_RC" != "0" ]] || t_fail "gRPC  UNEXPECTED success → $desc" "$GRPC_OUT"
  code="$(grpc_error_code)"
  [[ "$code" == "$want" ]] || t_fail "gRPC  $desc (status '$code', expected '$want')" "$GRPC_OUT"
  if [[ -n "$err" ]]; then
    [[ "$(grpc_error_id)" == "$err" ]] || t_fail "gRPC  $desc (error '$(grpc_error_id)', expected '$err')" "$GRPC_OUT"
  fi
  t_pass "gRPC  (expected failure) $desc (${code}${err:+, $err})"
}

expect_grpc_denied() { # DESC: Unauthenticated or PermissionDenied
  local code
  [[ "$GRPC_RC" != "0" ]] || t_fail "gRPC  UNEXPECTED success → $1" "$GRPC_OUT"
  code="$(grpc_error_code)"
  [[ "$code" == "Unauthenticated" || "$code" == "PermissionDenied" ]] \
    || t_fail "gRPC  UNEXPECTED $1 (status '$code', expected Unauthenticated/PermissionDenied)" "$GRPC_OUT"
  t_pass "gRPC  (expected failure) $1 (${code})"
}

expect_rest_jq() { # DESC FILTER
  jq -e "$2" >/dev/null 2>&1 <<<"$HTTP_BODY" || t_fail "REST  $1" "filter: $2 | body: $HTTP_BODY"
  t_pass "REST  $1"
}

expect_grpc_jq() { # DESC FILTER
  jq -e "$2" >/dev/null 2>&1 <<<"$GRPC_OUT" || t_fail "gRPC  $1" "filter: $2 | output: $GRPC_OUT"
  t_pass "gRPC  $1"
}

# ─────────────────────────────────────────────────────────────────────────────
# TRACE record builder and signing
# ─────────────────────────────────────────────────────────────────────────────

if command -v python3 >/dev/null 2>&1 && python3 -c 'import cryptography' >/dev/null 2>&1; then
  PY=(python3)
elif command -v uv >/dev/null 2>&1; then
  PY=(uv run --quiet --with cryptography python3)
else
  PY=()
fi

py() { "${PY[@]}" "${SCRIPT_DIR}/trace_sign.py" "$@"; }

# build_record PRODUCER KID KEY_FILE INSTANCE CHAIN RECORD_ID SEQ PREV_HASH EXEC_ID BUNDLE_HASH -> signed AUTHORIZATION_DECISION
build_record() {
  jq -n --arg producer "$1" --arg kid "$2" --arg instance "$4" --arg chain "$5" --arg rid "$6" --argjson seq "$7" \
        --arg prev "$8" --arg exec "$9" --arg bundle "${10}" --argjson signed "$(date +%s)" \
        --arg auth "spiffe://example.org/agent/${9}" '{
    producer: $producer, producer_version: "1.4.2", kid: $kid, record_id: $rid,
    trace: {
      eat_profile: "tag:jans.io,2026:trace-v1",
      event_kind: "AUTHORIZATION_DECISION",
      signed_at: $signed,
      trace_execution_id: $exec,
      execution_authority: $auth,
      subject: { workload_id: $auth, workload_instance_id: "auto-workload-instance-1" },
      event: {
        decision: { action: "Acme::Action::\"Pay\"", resource_type: "Acme::Payment", outcome: "ALLOW" },
        tokens: [ { issuer: "https://accounts.example.org", token_type: "access_token", jti: ("auto-jti-" + $rid) } ]
      },
      policy: {
        policy_store_id: "https://example.org/policy-stores/payments",
        policy_store_version: "1.2.3",
        policy_language: "cedar",
        policy_language_version: "4.4.0",
        bundle_hash: $bundle
      },
      runtime: { pdp_id: "auto-pdp-001" }
    },
    producer_chain: {
      producer_id: $producer, producer_instance_id: $instance, producer_chain_id: $chain,
      sequence_number: $seq, prev_record_hash: $prev
    },
    parent_record_ids: []
  }' | py sign "$3"
}

b64() { printf '%s' "$1" | base64 -w0; }

# ─────────────────────────────────────────────────────────────────────────────
# TRACE test flow
# ─────────────────────────────────────────────────────────────────────────────

run_trace_tests() {
  local api="${HOST}/jans-lock/api/v1/audit/trace"

  echo "┌────────────────────────────────────────────────────────────────────┐"
  echo "│           TRACE API AUTO-TEST (REST + gRPC, admin / write / read)  │"
  echo "└────────────────────────────────────────────────────────────────────┘"

  if [[ ${#PY[@]} -eq 0 ]]; then
    echo "❌ the TRACE tests need python3 with the 'cryptography' package, or uv (to fetch it)."
    exit 1
  fi

  local run_id="$(date +%s)$RANDOM"
  local bundle exec_rest exec_grpc
  bundle="$(py sha256 "auto-policy-bundle-${run_id}")"
  exec_rest="auto-exec-rest-${run_id}"
  exec_grpc="auto-exec-grpc-${run_id}"

  # One producer per transport: producer, key, instance and chain
  local p_rest="auto-rest-${run_id}" p_grpc="auto-grpc-${run_id}"
  local kid_rest="${p_rest}-key-1" kid_grpc="${p_grpc}-key-1"
  local inst_rest="${p_rest}-instance-1" inst_grpc="${p_grpc}-instance-1"
  local chain_rest="${p_rest}-chain-1" chain_grpc="${p_grpc}-chain-1"

  echo -e "\n6. TRACE: obtaining tokens\n"
  local admin_token write_token read_token
  admin_token="$(get_token "$TRACE_CLIENT_ID" "$TRACE_CLIENT_SECRET" "$SCOPE_TRACE_ADMIN")"
  [[ -z "$admin_token" ]] && { echo "Failed to obtain trace.admin token (is the scope granted to the client?)"; exit 1; }
  write_token="$(get_token "$TRACE_CLIENT_ID" "$TRACE_CLIENT_SECRET" "$SCOPE_TRACE_WRITE")"
  [[ -z "$write_token" ]] && { echo "Failed to obtain trace.write token"; exit 1; }
  read_token="$(get_token "$TRACE_CLIENT_ID" "$TRACE_CLIENT_SECRET" "$SCOPE_TRACE_READ")"
  [[ -z "$read_token" ]] && { echo "Failed to obtain trace.readonly token"; exit 1; }
  echo "trace.admin, trace.write and trace.readonly tokens obtained"

  echo -e "\n7. TRACE admin: register producer keys and chains\n"
  local key_file_rest="$WORK/rest.key" key_file_grpc="$WORK/grpc.key" pub_rest pub_grpc valid_from
  pub_rest="$(py gen "$key_file_rest")"
  pub_grpc="$(py gen "$key_file_grpc")"
  valid_from="$(py one-day-ago)"

  key_body() { # PRODUCER KID PUBLIC_X
    jq -n --arg p "$1" --arg k "$2" --arg x "$3" --arg vf "$valid_from" \
      '{producer_id: $p, kid: $k, public_key_jwk: {kty: "OKP", crv: "Ed25519", x: $x},
        authorized_event_kinds: ["AUTHORIZATION_DECISION"], valid_from: $vf}'
  }
  chain_body() { # PRODUCER INSTANCE CHAIN
    jq -n --arg p "$1" --arg i "$2" --arg c "$3" '{producer_id: $p, producer_instance_id: $i, producer_chain_id: $c}'
  }

  local key_rest_body key_grpc_body
  key_rest_body="$(key_body "$p_rest" "$kid_rest" "$pub_rest")"
  key_grpc_body="$(key_body "$p_grpc" "$kid_grpc" "$pub_grpc")"

  rest_post "$api/admin/producer-keys" "$admin_token" "$key_rest_body"
  expect_http "register producer key" 201
  expect_rest_jq "registered key carries its authorized_event_kinds" '.authorized_event_kinds == ["AUTHORIZATION_DECISION"]'
  rest_post "$api/admin/producer-keys" "$admin_token" "$key_rest_body"
  expect_http "register the same producer key again" 409 "key_already_exists"
  grpc TraceAdminService/RegisterProducerKey "$admin_token" "$key_grpc_body"
  expect_grpc "register producer key"
  expect_grpc_jq "registered key carries its authorized_event_kinds" '.authorizedEventKinds == ["AUTHORIZATION_DECISION"]'
  grpc TraceAdminService/RegisterProducerKey "$admin_token" "$key_grpc_body"
  expect_grpc_error "register the same producer key again" AlreadyExists "key_already_exists"

  rest_post "$api/admin/producer-chains" "$admin_token" "$(chain_body "$p_rest" "$inst_rest" "$chain_rest")"
  expect_http "register producer chain" 201
  rest_post "$api/admin/producer-chains" "$admin_token" "$(chain_body "$p_rest" "$inst_rest" "$chain_rest")"
  expect_http "register the same producer chain again" 409 "chain_already_exists"
  grpc TraceAdminService/RegisterProducerChain "$admin_token" "$(chain_body "$p_grpc" "$inst_grpc" "$chain_grpc")"
  expect_grpc "register producer chain"
  grpc TraceAdminService/RegisterProducerChain "$admin_token" "$(chain_body "$p_grpc" "$inst_grpc" "$chain_grpc")"
  expect_grpc_error "register the same producer chain again" AlreadyExists "chain_already_exists"

  rest GET "$api/admin/producer-keys" "$admin_token" -G --data-urlencode "producer_id=${p_grpc}"
  expect_http "list keys of the producer registered over gRPC"  200
  expect_rest_jq "key registered over gRPC is listed over REST" ".keys | map(.kid) | index(\"${kid_grpc}\") != null"
  grpc TraceAdminService/ListProducerKeys "$admin_token" "{\"producer_id\": \"${p_rest}\"}"
  expect_grpc "list keys of the producer registered over REST"
  expect_grpc_jq "key registered over REST is listed over gRPC" ".keys | map(.kid) | index(\"${kid_rest}\") != null"
  rest GET "$api/admin/producer-chains" "$admin_token" -G --data-urlencode "producer_id=${p_grpc}"
  expect_http "list chains of the producer registered over gRPC" 200
  expect_rest_jq "chain registered over gRPC is listed over REST" ".chains | map(.producer_chain_id) | index(\"${chain_grpc}\") != null"
  grpc TraceAdminService/ListProducerChains "$admin_token" "{\"producer_id\": \"${p_rest}\"}"
  expect_grpc "list chains of the producer registered over REST"
  expect_grpc_jq "chain registered over REST is listed over gRPC" ".chains | map(.producerChainId) | index(\"${chain_rest}\") != null"

  echo -e "\n8. TRACE write: single, replay and bulk submit\n"
  local rid_rest_1 rid_rest_2 rid_rest_3 rid_grpc_1 rid_grpc_2 rid_grpc_3
  rid_rest_1="$(py uuid)"; rid_rest_2="$(py uuid)"; rid_rest_3="$(py uuid)"
  rid_grpc_1="$(py uuid)"; rid_grpc_2="$(py uuid)"; rid_grpc_3="$(py uuid)"

  local rec_rest_1 rec_rest_2 rec_rest_3 rec_grpc_1 rec_grpc_2 rec_grpc_3 digest_rest_1 digest_grpc_1 receipt_rest_1 receipt_grpc_1
  rec_rest_1="$(build_record "$p_rest" "$kid_rest" "$key_file_rest" "$inst_rest" "$chain_rest" "$rid_rest_1" 1 "$ZERO_HASH" "$exec_rest" "$bundle")"
  rec_grpc_1="$(build_record "$p_grpc" "$kid_grpc" "$key_file_grpc" "$inst_grpc" "$chain_grpc" "$rid_grpc_1" 1 "$ZERO_HASH" "$exec_grpc" "$bundle")"

  rest_post "$api" "$write_token" "$rec_rest_1"
  expect_http "submit record (genesis of the REST chain)" 202
  expect_rest_jq "acceptance is not a replay" '.accepted == true and .idempotent_replay == false'
  digest_rest_1="$(jq -r '.content_digest' <<<"$HTTP_BODY")"
  receipt_rest_1="$(jq -r '.receipt_sequence' <<<"$HTTP_BODY")"

  grpc TraceService/SubmitRecord "$write_token" "{\"assertion\": \"$(b64 "$rec_grpc_1")\"}"
  expect_grpc "submit record (genesis of the gRPC chain)"
  expect_grpc_jq "acceptance is not a replay" '.accepted == true and .idempotentReplay == false'
  digest_grpc_1="$(jq -r '.contentDigest' <<<"$GRPC_OUT")"
  receipt_grpc_1="$(jq -r '.receiptSequence' <<<"$GRPC_OUT")"
  [[ "$digest_grpc_1" == "$(py digest <<<"$rec_grpc_1")" ]] || t_fail "gRPC  content_digest differs from the digest of the submitted bytes" "$digest_grpc_1"
  t_pass "gRPC  content_digest matches the digest of the submitted record"

  rest_post "$api" "$write_token" "$rec_grpc_1"
  expect_http "replay of the record submitted over gRPC" 202
  expect_rest_jq "replay returns the original receipt" ".idempotent_replay == true and .content_digest == \"${digest_grpc_1}\" and (.receipt_sequence | tostring) == \"${receipt_grpc_1}\""
  grpc TraceService/SubmitRecord "$write_token" "{\"assertion\": \"$(b64 "$rec_rest_1")\"}"
  expect_grpc "replay of the record submitted over REST"
  expect_grpc_jq "replay returns the original receipt" ".idempotentReplay == true and .contentDigest == \"${digest_rest_1}\" and (.receiptSequence | tostring) == \"${receipt_rest_1}\""

  rest_post "$api" "$write_token" "$(py tamper-signature <<<"$(build_record "$p_rest" "$kid_rest" "$key_file_rest" "$inst_rest" "$chain_rest" "$(py uuid)" 2 "$digest_rest_1" "$exec_rest" "$bundle")")"
  expect_http "tampered signature" 400 "invalid_signature"
  grpc TraceService/SubmitRecord "$write_token" "{\"assertion\": \"$(b64 "$(py tamper-signature <<<"$(build_record "$p_grpc" "$kid_grpc" "$key_file_grpc" "$inst_grpc" "$chain_grpc" "$(py uuid)" 2 "$digest_grpc_1" "$exec_grpc" "$bundle")")")\"}"
  expect_grpc_error "tampered signature" InvalidArgument "invalid_signature"
  grpc TraceService/SubmitRecord "$write_token" "{\"assertion\": \"$(b64 '{}')\"}"
  expect_grpc_error "assertion without required members" InvalidArgument "invalid_assertion"

  rec_rest_2="$(build_record "$p_rest" "$kid_rest" "$key_file_rest" "$inst_rest" "$chain_rest" "$rid_rest_2" 2 "$digest_rest_1" "$exec_rest" "$bundle")"
  rec_rest_3="$(build_record "$p_rest" "$kid_rest" "$key_file_rest" "$inst_rest" "$chain_rest" "$rid_rest_3" 3 "$(py digest <<<"$rec_rest_2")" "$exec_rest" "$bundle")"
  rec_grpc_2="$(build_record "$p_grpc" "$kid_grpc" "$key_file_grpc" "$inst_grpc" "$chain_grpc" "$rid_grpc_2" 2 "$digest_grpc_1" "$exec_grpc" "$bundle")"
  rec_grpc_3="$(build_record "$p_grpc" "$kid_grpc" "$key_file_grpc" "$inst_grpc" "$chain_grpc" "$rid_grpc_3" 3 "$(py digest <<<"$rec_grpc_2")" "$exec_grpc" "$bundle")"

  rest_post "$api/bulk" "$write_token" "[${rec_rest_2},${rec_rest_3}]"
  expect_http "bulk submit of two chained records" 207
  expect_rest_jq "bulk results keep request order and are all accepted" '[.results[].index] == [0, 1] and ([.results[].accepted] | all)'
  local bulk_grpc="[${rec_grpc_2},${rec_grpc_3}]"
  grpc TraceService/SubmitBulkRecords "$write_token" "{\"records\": \"$(b64 "$bulk_grpc")\"}"
  expect_grpc "bulk submit of two chained records"
  expect_grpc_jq "bulk results keep request order and are all accepted" '[.results[].index] == [0, 1] and ([.results[].accepted] | all)'
  grpc TraceService/SubmitBulkRecords "$write_token" "{\"records\": \"$(b64 '{}')\"}"
  expect_grpc_error "bulk body that is not an array" InvalidArgument "invalid_request"
  rest_post "$api/bulk" "$write_token" "{}"
  expect_http "bulk body that is not an array" 400 "invalid_request"

  echo -e "\n9. TRACE read: records and executions, across transports\n"
  rest GET "$api/records/${rid_grpc_1}" "$read_token" -G --data-urlencode "producer_id=${p_grpc}"
  expect_http "read over REST the record submitted over gRPC" 200
  [[ "$(jq -r '.assertion_raw' <<<"$HTTP_BODY")" == "$rec_grpc_1" ]] || t_fail "REST  assertion_raw differs from the submitted text" "$HTTP_BODY"
  t_pass "REST  assertion_raw is the exact submitted text"
  expect_rest_jq "verification is valid and the receipt matches" ".verification.signature_valid == true and (.ingestion.receipt_sequence | tostring) == \"${receipt_grpc_1}\""
  grpc TraceService/GetRecord "$read_token" "{\"record_id\": \"${rid_rest_1}\", \"producer_id\": \"${p_rest}\"}"
  expect_grpc "read over gRPC the record submitted over REST"
  expect_grpc_jq "envelope matches what was accepted" ".recordId == \"${rid_rest_1}\" and .contentDigest == \"${digest_rest_1}\" and .verification.signatureValid == true and (.ingestion.receiptSequence | tostring) == \"${receipt_rest_1}\""
  [[ "$(jq -r '.assertionRaw' <<<"$GRPC_OUT")" == "$rec_rest_1" ]] || t_fail "gRPC  assertion_raw differs from the submitted text" "$GRPC_OUT"
  t_pass "gRPC  assertion_raw is the exact submitted text"

  rest GET "$api/records/${rid_rest_3}" "$read_token"
  expect_http "read a record submitted through bulk, without producer_id" 200
  grpc TraceService/GetRecord "$read_token" "{\"record_id\": \"${rid_grpc_3}\"}"
  expect_grpc "read a record submitted through bulk, without producer_id"
  rest GET "$api/records/${rid_rest_1}-missing" "$read_token"
  expect_http "read an unknown record" 404 "record_not_found"
  grpc TraceService/GetRecord "$read_token" "{\"record_id\": \"${rid_rest_1}-missing\"}"
  expect_grpc_error "read an unknown record" NotFound "record_not_found"

  rest GET "$api/executions/${exec_rest}" "$read_token" -G --data-urlencode "execution_authority=spiffe://example.org/agent/${exec_rest}"
  expect_http "read execution of the REST producer" 200
  expect_rest_jq "execution holds three records ordered by receipt_sequence" '(.records | length) == 3 and ([.records[].ingestion.receipt_sequence | tonumber] == ([.records[].ingestion.receipt_sequence | tonumber] | sort))'
  grpc TraceService/GetExecution "$read_token" "{\"trace_execution_id\": \"${exec_grpc}\", \"execution_authority\": \"spiffe://example.org/agent/${exec_grpc}\"}"
  expect_grpc "read execution of the gRPC producer"
  expect_grpc_jq "execution holds three records ordered by receipt_sequence" '(.records | length) == 3 and ([.records[].ingestion.receiptSequence | tonumber] == ([.records[].ingestion.receiptSequence | tonumber] | sort))'
  grpc TraceService/GetExecution "$read_token" "{\"trace_execution_id\": \"${exec_rest}\", \"count\": 2}"
  expect_grpc "read execution of the REST producer over gRPC, two records per page"
  expect_grpc_jq "page size is honoured" '(.records | length) == 2'
  grpc TraceService/GetExecution "$read_token" "{\"trace_execution_id\": \"${exec_rest}\", \"count\": 0}"
  expect_grpc_error "execution read with count 0" InvalidArgument "invalid_request"

  rest GET "$api/records" "$read_token" -G --data-urlencode "bundle_hash=${bundle}"
  expect_http "search records by bundle_hash" 200
  expect_rest_jq "search returns the six records of both producers" '.records | length == 6'
  grpc TraceService/SearchRecords "$read_token" "{\"bundle_hash\": \"${bundle}\"}"
  expect_grpc "search records by bundle_hash"
  expect_grpc_jq "search returns the six records of both producers" '.records | length == 6'
  grpc TraceService/SearchRecords "$read_token" "{\"bundle_hash\": \"${bundle}\", \"start\": 4, \"count\": 5}"
  expect_grpc "search records, second page"
  expect_grpc_jq "second page holds the last two records" '.records | length == 2'
  rest GET "$api/records" "$read_token"
  expect_http "search without a policy filter" 400 "invalid_request"
  grpc TraceService/SearchRecords "$read_token" "{}"
  expect_grpc_error "search without a policy filter" InvalidArgument "invalid_request"

  echo -e "\n10. TRACE: wrong scope, read token and invalid token must fail\n"
  rest_post "$api" "$read_token" "$rec_rest_1"
  expect_http_denied "submit with a trace.readonly token"
  grpc TraceService/SubmitRecord "$read_token" "{\"assertion\": \"$(b64 "$rec_rest_1")\"}"
  expect_grpc_denied "submit with a trace.readonly token"
  rest_post "$api/bulk" "$read_token" "[${rec_rest_1}]"
  expect_http_denied "bulk submit with a trace.readonly token"
  grpc TraceService/SubmitBulkRecords "$read_token" "{\"records\": \"$(b64 "[${rec_rest_1}]")\"}"
  expect_grpc_denied "bulk submit with a trace.readonly token"
  rest GET "$api/records/${rid_rest_1}" "$write_token"
  expect_http_denied "read a record with a trace.write token"
  grpc TraceService/GetRecord "$write_token" "{\"record_id\": \"${rid_rest_1}\"}"
  expect_grpc_denied "read a record with a trace.write token"
  rest GET "$api/admin/producer-keys" "$write_token"
  expect_http_denied "list producer keys with a trace.write token"
  grpc TraceAdminService/ListProducerKeys "$write_token" "{}"
  expect_grpc_denied "list producer keys with a trace.write token"
  grpc TraceAdminService/RegisterProducerChain "$read_token" "$(chain_body "$p_grpc" "$inst_grpc" "${chain_grpc}-2")"
  expect_grpc_denied "register a producer chain with a trace.readonly token"

  rest_post "$api" "$INVALID_TOKEN" "$rec_rest_1"
  expect_http_denied "submit with an invalid token"
  grpc TraceService/SubmitRecord "$INVALID_TOKEN" "{\"assertion\": \"$(b64 "$rec_rest_1")\"}"
  expect_grpc_denied "submit with an invalid token"
  rest GET "$api/records/${rid_rest_1}" "$INVALID_TOKEN"
  expect_http_denied "read a record with an invalid token"
  grpc TraceService/GetRecord "$INVALID_TOKEN" "{\"record_id\": \"${rid_rest_1}\"}"
  expect_grpc_denied "read a record with an invalid token"
  rest GET "$api/admin/producer-keys" ""
  expect_http_denied "list producer keys without a token"
  grpc TraceAdminService/ListProducerKeys "" "{}"
  expect_grpc_denied "list producer keys without a token"

  echo -e "\n11. TRACE admin: revoke the producer keys\n"
  rest_post "$api/admin/producer-keys/revoke" "$admin_token" "{\"producer_id\": \"${p_rest}\", \"kid\": \"${kid_rest}\"}"
  expect_http "revoke the REST producer key" 200
  expect_rest_jq "revoked key carries revoked_at" '.revoked_at != null'
  grpc TraceAdminService/RevokeProducerKey "$admin_token" "{\"producer_id\": \"${p_grpc}\", \"kid\": \"${kid_grpc}\"}"
  expect_grpc "revoke the gRPC producer key"
  expect_grpc_jq "revoked key carries revoked_at" '.revokedAt != null'
  grpc TraceAdminService/RevokeProducerKey "$admin_token" "{\"producer_id\": \"${p_rest}\", \"kid\": \"${kid_rest}\"}"
  expect_grpc "revoke over gRPC the key revoked over REST (idempotent)"
  rest_post "$api/admin/producer-keys/revoke" "$admin_token" "{\"producer_id\": \"${p_rest}\", \"kid\": \"${kid_rest}-unknown\"}"
  expect_http "revoke an unknown key" 404 "record_not_found"
  grpc TraceAdminService/RevokeProducerKey "$admin_token" "{\"producer_id\": \"${p_rest}\", \"kid\": \"${kid_rest}-unknown\"}"
  expect_grpc_error "revoke an unknown key" NotFound "record_not_found"
}

run_audit_tests() {
  # Audit tests
  # ─────────────────────────────────────────────────────────────────────────────

  echo "┌────────────────────────────────────────────────────────────────────┐"
  echo "│             AUDIT API AUTO-TEST (log / health / telemetry)         │"
  echo "└────────────────────────────────────────────────────────────────────┘"

  echo -e "\n1. Obtaining tokens\n"

  WRITE_LOG_TOKEN=$(get_audit_token "https://jans.io/oauth/lock/log.write")
  [[ -z "$WRITE_LOG_TOKEN" ]] && { echo "Failed to obtain log.write token"; exit 1; }
  echo "Log write token obtained"

  WRITE_HEALTH_TOKEN=$(get_audit_token "https://jans.io/oauth/lock/health.write")
  [[ -z "$WRITE_HEALTH_TOKEN" ]] && { echo "Failed to obtain health.write token"; exit 1; }
  echo "Health write token obtained"

  WRITE_TELEMETRY_TOKEN=$(get_audit_token "https://jans.io/oauth/lock/telemetry.write")
  [[ -z "$WRITE_TELEMETRY_TOKEN" ]] && { echo "Failed to obtain telemetry.write token"; exit 1; }
  echo "Telemetry write token obtained"

  READ_LOG_TOKEN=$(get_audit_token "https://jans.io/oauth/lock/log.read")
  [[ -z "$READ_LOG_TOKEN" ]] && { echo "Failed to obtain log.read token"; exit 1; }
  echo "Log read token obtained"

  READ_HEALTH_TOKEN=$(get_audit_token "https://jans.io/oauth/lock/health.read")
  [[ -z "$READ_HEALTH_TOKEN" ]] && { echo "Failed to obtain health.read token"; exit 1; }
  echo "Health read token obtained"

  READ_TELEMETRY_TOKEN=$(get_audit_token "https://jans.io/oauth/lock/telemetry.read")
  [[ -z "$READ_TELEMETRY_TOKEN" ]] && { echo "Failed to obtain telemetry.read token"; exit 1; }
  echo "Telemetry read token obtained"

  echo -e "\n2. Tests with valid write tokens (should succeed)\n"

  echo -e "\n── Log ───────────────────────────────────────────────"
  check_http_success "curl $CURL_TLS -H 'Authorization: Bearer $WRITE_LOG_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/log' <<< '$SINGLE_LOG_JSON_REST'" "REST → single log entry"
  check_http_success "curl $CURL_TLS -H 'Authorization: Bearer $WRITE_LOG_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/log/bulk' <<< '$BULK_LOG_JSON_REST'" "REST → bulk log entries"
  check_grpc_success "$GRPCURL --proto audit.proto -H 'authorization: bearer $WRITE_LOG_TOKEN' -d '$SINGLE_LOG_GRPC' $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessLog" "gRPC → ProcessLog"
  check_grpc_success "$GRPCURL --proto audit.proto -H 'authorization: bearer $WRITE_LOG_TOKEN' -H 'GRPC_APP: jans-lock' -d '$BULK_LOG_GRPC'  $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessBulkLog" "gRPC → ProcessBulkLog"

  echo -e "\n── Health ───────────────────────────────────────────"
  check_http_success "curl $CURL_TLS -H 'Authorization: Bearer $WRITE_HEALTH_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/health' <<< '$SINGLE_HEALTH_JSON_REST'" "REST → single health entry"
  check_http_success "curl $CURL_TLS -H 'Authorization: Bearer $WRITE_HEALTH_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/health/bulk' <<< '$BULK_HEALTH_JSON_REST'" "REST → bulk health entries"
  check_grpc_success "$GRPCURL --proto audit.proto -H 'authorization: bearer $WRITE_HEALTH_TOKEN' -d '$SINGLE_HEALTH_GRPC' $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessHealth" "gRPC → ProcessHealth"
  check_grpc_success "$GRPCURL --proto audit.proto -H 'authorization: bearer $WRITE_HEALTH_TOKEN' -d '$BULK_HEALTH_GRPC'  $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessBulkHealth" "gRPC → ProcessBulkHealth"

  echo -e "\n── Telemetry ─────────────────────────────────────────"
  check_http_success "curl $CURL_TLS -H 'Authorization: Bearer $WRITE_TELEMETRY_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/telemetry' <<< '$SINGLE_TELEMETRY_JSON_REST'" "REST → single telemetry entry"
  check_http_success "curl $CURL_TLS -H 'Authorization: Bearer $WRITE_TELEMETRY_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/telemetry/bulk' <<< '$BULK_TELEMETRY_JSON_REST'" "REST → bulk telemetry entries"

  check_grpc_success "$GRPCURL --proto audit.proto -H 'authorization: bearer $WRITE_TELEMETRY_TOKEN' -d '$SINGLE_TELEMETRY_GRPC' $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessTelemetry" "gRPC → ProcessTelemetry"
  check_grpc_success "$GRPCURL --proto audit.proto -H 'authorization: bearer $WRITE_TELEMETRY_TOKEN' -d '$BULK_TELEMETRY_GRPC'  $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessBulkTelemetry" "gRPC → ProcessBulkTelemetry"

  echo -e "\n3. Tests with invalid write tokens → write operations should fail\n"

  echo -e "\n── Log with invalid scope ────────────────────────────"
  check_http_should_fail "curl $CURL_TLS -H 'Authorization: Bearer $WRITE_HEALTH_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/log' <<< '$SINGLE_LOG_JSON_REST'" "REST → single log entry"
  check_grpc_should_fail "$GRPCURL --proto audit.proto -H 'authorization: bearer $WRITE_HEALTH_TOKEN' -d '$SINGLE_LOG_GRPC' $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessLog" "gRPC → ProcessLog"

  echo -e "\n── Health with invalid scope ─────────────────────────"
  check_http_should_fail "curl $CURL_TLS -H 'Authorization: Bearer $WRITE_LOG_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/health' <<< '$SINGLE_HEALTH_JSON_REST'" "REST → single health entry"
  check_grpc_should_fail "$GRPCURL --proto audit.proto -H 'authorization: bearer $WRITE_LOG_TOKEN' -d '$SINGLE_HEALTH_GRPC' $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessHealth" "gRPC → ProcessHealth"

  echo -e "\n── Telemetry with invalid scope ──────────────────────"
  check_http_should_fail "curl $CURL_TLS -H 'Authorization: Bearer $WRITE_LOG_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/telemetry' <<< '$SINGLE_TELEMETRY_JSON_REST'" "REST → single telemetry entry"
  check_grpc_should_fail "$GRPCURL --proto audit.proto -H 'authorization: bearer $WRITE_LOG_TOKEN' -d '$SINGLE_TELEMETRY_GRPC' $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessTelemetry" "gRPC → ProcessTelemetry"

  echo -e "\n4. Tests with read-only tokens → write operations should fail\n"

  echo -e "\n── Log read token ────────────────────────────────────"
  check_http_should_fail "curl $CURL_TLS -H 'Authorization: Bearer $READ_LOG_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/log' <<< '$SINGLE_LOG_JSON_REST'" "REST → single log entry with read-only token"
  check_http_should_fail "curl $CURL_TLS -H 'Authorization: Bearer $READ_LOG_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/log/bulk' <<< '$BULK_LOG_JSON_REST'" "REST → bulk log entries with read-only token"
  check_grpc_should_fail "$GRPCURL --proto audit.proto -H 'authorization: bearer $READ_LOG_TOKEN' -d '$SINGLE_LOG_GRPC' $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessLog" "gRPC → ProcessLog with read-only token"
  check_grpc_should_fail "$GRPCURL --proto audit.proto -H 'authorization: bearer $READ_LOG_TOKEN' -d '$BULK_LOG_GRPC'  $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessBulkLog" "gRPC → ProcessBulkLog with read-only token"

  echo -e "\n── Health read token ─────────────────────────────────"
  check_http_should_fail "curl $CURL_TLS -H 'Authorization: Bearer $READ_HEALTH_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/health' <<< '$SINGLE_HEALTH_JSON_REST'" "REST → single health entry with read-only token"
  check_http_should_fail "curl $CURL_TLS -H 'Authorization: Bearer $READ_HEALTH_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/health/bulk' <<< '$BULK_HEALTH_JSON_REST'" "REST → bulk health entries with read-only token"
  check_grpc_should_fail "$GRPCURL --proto audit.proto -H 'authorization: bearer $READ_HEALTH_TOKEN' -d '$SINGLE_HEALTH_GRPC' $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessHealth" "gRPC → ProcessHealth with read-only token"
  check_grpc_should_fail "$GRPCURL --proto audit.proto -H 'authorization: bearer $READ_HEALTH_TOKEN' -d '$BULK_HEALTH_GRPC'  $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessBulkHealth" "gRPC → ProcessBulkHealth with read-only token"

  echo -e "\n── Telemetry read token ──────────────────────────────"
  check_http_should_fail "curl $CURL_TLS -H 'Authorization: Bearer $READ_TELEMETRY_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/telemetry' <<< '$SINGLE_TELEMETRY_JSON_REST'" "REST → single telemetry entry with read-only token"
  check_http_should_fail "curl $CURL_TLS -H 'Authorization: Bearer $READ_TELEMETRY_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/telemetry/bulk' <<< '$BULK_TELEMETRY_JSON_REST'" "REST → bulk telemetry entries with read-only token"
  check_grpc_should_fail "$GRPCURL --proto audit.proto -H 'authorization: bearer $READ_TELEMETRY_TOKEN' -d '$SINGLE_TELEMETRY_GRPC' $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessTelemetry" "gRPC → ProcessTelemetry with read-only token"
  check_grpc_should_fail "$GRPCURL --proto audit.proto -H 'authorization: bearer $READ_TELEMETRY_TOKEN' -d '$BULK_TELEMETRY_GRPC'  $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessBulkTelemetry" "gRPC → ProcessBulkTelemetry with read-only token"

  echo -e "\n5. Tests with invalid token (should fail)\n"

  echo -e "\n── Log invalid token ────────────────────────────────────"
  check_http_should_fail "curl $CURL_TLS -H 'Authorization: Bearer $INVALID_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/log' <<< '$SINGLE_LOG_JSON_REST'" "REST → single log entry with invalid token"
  check_http_should_fail "curl $CURL_TLS -H 'Authorization: Bearer $INVALID_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/log/bulk' <<< '$BULK_LOG_JSON_REST'" "REST → bulk log entries with invalid token"
  check_grpc_should_fail "$GRPCURL --proto audit.proto -H 'authorization: bearer $INVALID_TOKEN' -d '$SINGLE_LOG_GRPC' $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessLog" "gRPC → ProcessLog with invalid token"
  check_grpc_should_fail "$GRPCURL --proto audit.proto -H 'authorization: bearer $INVALID_TOKEN' -d '$BULK_LOG_GRPC'  $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessBulkLog" "gRPC → ProcessBulkLog with invalid token"

  echo -e "\n── Health invalid token ─────────────────────────────────"
  check_http_should_fail "curl $CURL_TLS -H 'Authorization: Bearer $INVALID_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/health' <<< '$SINGLE_HEALTH_JSON_REST'" "REST → single health entry with invalid token"
  check_http_should_fail "curl $CURL_TLS -H 'Authorization: Bearer $INVALID_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/health/bulk' <<< '$BULK_HEALTH_JSON_REST'" "REST → bulk health entries with invalid token"
  check_grpc_should_fail "$GRPCURL --proto audit.proto -H 'authorization: bearer $INVALID_TOKEN' -d '$SINGLE_HEALTH_GRPC' $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessHealth" "gRPC → ProcessHealth with invalid token"
  check_grpc_should_fail "$GRPCURL --proto audit.proto -H 'authorization: bearer $INVALID_TOKEN' -d '$BULK_HEALTH_GRPC'  $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessBulkHealth" "gRPC → ProcessBulkHealth with invalid token"

  echo -e "\n── Telemetry invalid token ──────────────────────────────"
  check_http_should_fail "curl $CURL_TLS -H 'Authorization: Bearer $INVALID_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/telemetry' <<< '$SINGLE_TELEMETRY_JSON_REST'" "REST → single telemetry entry with invalid token"
  check_http_should_fail "curl $CURL_TLS -H 'Authorization: Bearer $INVALID_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/telemetry/bulk' <<< '$BULK_TELEMETRY_JSON_REST'" "REST → bulk telemetry entries with invalid token"
  check_grpc_should_fail "$GRPCURL --proto audit.proto -H 'authorization: bearer $INVALID_TOKEN' -H 'GRPC_APP: jans-lock' -d '$SINGLE_TELEMETRY_GRPC' $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessTelemetry" "gRPC → ProcessTelemetry with invalid token"
  check_grpc_should_fail "$GRPCURL --proto audit.proto -H 'authorization: bearer $INVALID_TOKEN' -d '$BULK_TELEMETRY_GRPC'  $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessBulkTelemetry" "gRPC → ProcessBulkTelemetry with invalid token"
}

# ─────────────────────────────────────────────────────────────────────────────
# Main
# ─────────────────────────────────────────────────────────────────────────────

if is_true "$(prop test.audit true)"; then
  if is_configured "$AUDIT_CLIENT_ID" && is_configured "$AUDIT_CLIENT_SECRET"; then
    run_audit_tests
  else
    echo "⚠️  audit tests skipped: audit.client.id / audit.client.secret are not configured"
  fi
fi

if is_true "$(prop test.trace true)"; then
  if is_configured "$TRACE_CLIENT_ID" && is_configured "$TRACE_CLIENT_SECRET"; then
    run_trace_tests
  else
    echo "⚠️  TRACE tests skipped: trace.client.id / trace.client.secret are not configured"
  fi
fi

echo -e "\n┌──────────────────────────────┐"
echo   "│         TESTS PASSED         │"
echo   "└──────────────────────────────┘"