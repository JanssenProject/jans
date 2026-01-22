#!/bin/bash

# =============================================================================
# Test script for checking REST ↔ gRPC bridge + Audit API authorization
# Tests: log, health, telemetry (single + bulk) with write & read scopes
# Requirements: curl, jq, grpcurl
# =============================================================================

set -euo pipefail

HOST="https://server.jans.info"
GRPC_ADDR="server.jans.info:443"
CLIENT_ID="2200...."
CLIENT_SECRET="..."
INVALID_TOKEN="this-is-definitely-not-a-valid-token"

# ─────────────────────────────────────────────────────────────────────────────
# Test data — REST format
# ─────────────────────────────────────────────────────────────────────────────

# Log
SINGLE_LOG_JSON_REST='{
    "creation_date": "2024-04-21T18:25:43-05:00",
    "event_time":   "2024-04-21T18:25:43-05:00",
    "service":      "jans-auth",
    "node_name":    "node-1",
    "event_type":   "registration",
    "severity_level": "warning",
    "action":       "LOGIN_ATTEMPT",
    "decision_result": "allow",
    "requested_resource": "{\"res\":\"/api/user\",\"method\":\"POST\"}",
    "principal_id": "ACC-000123",
    "client_id":    "CLI-001",
    "jti":          "test-jti-123456",
    "context_information": {
      "ip": "192.168.1.77",
      "user_agent": "Mozilla/5.0 (test)"
    }
}'

BULK_LOG_JSON_REST='[
   {
      "creation_date": "2024-04-21T18:25:43-05:00",
      "event_time":   "2024-04-21T18:25:43-05:00",
      "service":      "jans-auth",
      "node_name":    "node-1",
      "event_type":   "registration",
      "severity_level": "warning",
      "action":       "LOGIN_ATTEMPT",
      "decision_result": "deny",
      "requested_resource": "{\"res\":\"/api/admin\"}",
      "principal_id": "ACC-000123",
      "client_id":    "CLI-001",
      "context_information": {"ip": "10.0.0.5"}
    },
    {
      "creation_date": "2024-06-15T14:10:22Z",
      "event_time":   "2024-06-15T14:10:22Z",
      "service":      "jans-auth",
      "node_name":    "node-2",
      "event_type":   "transaction",
      "severity_level": "info",
      "action":       "PAYMENT",
      "decision_result": "allow",
      "requested_resource": "{\"amount\":99.99}",
      "principal_id": "ACC-000123",
      "client_id":    "CLI-001"
    }
]'

# Health
SINGLE_HEALTH_JSON_REST='{
  "creationDate": "2024-04-21T17:25:43-05:00",
  "eventTime":    "2024-04-21T18:25:43-05:00",
  "service":      "jans-auth",
  "nodeName":     "node-1",
  "status":       "ok"
}'

BULK_HEALTH_JSON_REST='[
  {
    "creationDate": "2024-04-21T17:25:43-05:00",
    "eventTime":    "2024-04-21T18:25:43-05:00",
    "service":      "jans-auth",
    "nodeName":     "node-1",
    "status":       "ok"
  },
  {
    "creationDate": "2024-04-21T17:30:00-05:00",
    "eventTime":    "2024-04-21T18:30:00-05:00",
    "service":      "jans-lock",
    "nodeName":     "node-2",
    "status":       "degraded"
  }
]'

# Telemetry
SINGLE_TELEMETRY_JSON_REST='{
  "creationDate":               "2024-04-21T18:00:00-05:00",
  "eventTime":                  "2024-04-21T18:25:43-05:00",
  "service":                    "jans-auth",
  "nodeName":                   "node-1",
  "status":                     "ok",
  "lastPolicyLoadSize":         1024,
  "policySuccessLoadCounter":   100,
  "policyFailedLoadCounter":    3,
  "lastPolicyEvaluationTimeNs": 100000,
  "avgPolicyEvaluationTimeNs":  75000,
  "memoryUsage":                2097152,
  "evaluationRequestsCount":    100,
  "policyStats":                {"stat_1":100,"stat_2":3}
}'

BULK_TELEMETRY_JSON_REST='[
  {
    "creationDate":               "2024-04-21T18:00:00-05:00",
    "eventTime":                  "2024-04-21T18:25:43-05:00",
    "service":                    "jans-auth",
    "nodeName":                   "node-1",
    "status":                     "ok",
    "lastPolicyLoadSize":         1024,
    "policySuccessLoadCounter":   100,
    "policyFailedLoadCounter":    3,
    "lastPolicyEvaluationTimeNs": 100000,
    "avgPolicyEvaluationTimeNs":  75000,
    "memoryUsage":                2097152,
    "evaluationRequestsCount":    100,
    "policyStats":                {"stat_1":100,"stat_2":3}
  },
  {
    "creationDate":               "2024-04-21T19:00:00Z",
    "eventTime":                  "2024-04-21T19:10:22Z",
    "service":                    "jans-lock",
    "nodeName":                   "node-2",
    "status":                     "ok",
    "lastPolicyLoadSize":         2048,
    "policySuccessLoadCounter":   250,
    "policyFailedLoadCounter":    1,
    "lastPolicyEvaluationTimeNs": 85000,
    "avgPolicyEvaluationTimeNs":  92000,
    "memoryUsage":                4194304,
    "evaluationRequestsCount":    420,
    "policyStats":                {"p1":300,"p2":120}
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

get_token() {
  local scope="$1"
  curl -s -k \
    -u "${CLIENT_ID}:${CLIENT_SECRET}" \
    "${HOST}/jans-auth/restv1/token" \
    -d "grant_type=client_credentials" \
    -d "scope=${scope}" \
    | jq -r '.access_token // empty'
}

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
  > res.txt
  if eval "$cmd" >res.txt 2>&1; then
    echo "✅  $desc"
  else
    echo "❌  $desc"
    echo "Command: $cmd"
    echo "Output:"
    cat res.txt
    return 1
  fi
}

check_grpc_should_fail() {
  local cmd="$1"
  local desc="$2"
  > res.txt
  if eval "$cmd" >res.txt 2>&1; then
    echo "❌ UNEXPECTED success → $desc"
    echo "Command: $cmd"
    echo "Output:"
    cat res.txt
    return 1
  else
    local output
    output=$(cat res.txt)
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
# Main test flow
# ─────────────────────────────────────────────────────────────────────────────

echo "┌────────────────────────────────────────────────────────────────────┐"
echo "│             AUDIT API AUTO-TEST (log / health / telemetry)         │"
echo "└────────────────────────────────────────────────────────────────────┘"

echo -e "\n1. Obtaining tokens\n"

WRITE_LOG_TOKEN=$(get_token "https://jans.io/oauth/lock/log.write")
[[ -z "$WRITE_LOG_TOKEN" ]] && { echo "Failed to obtain log.write token"; exit 1; }
echo "Log write token obtained"

WRITE_HEALTH_TOKEN=$(get_token "https://jans.io/oauth/lock/health.write")
[[ -z "$WRITE_HEALTH_TOKEN" ]] && { echo "Failed to obtain health.write token"; exit 1; }
echo "Health write token obtained"

WRITE_TELEMETRY_TOKEN=$(get_token "https://jans.io/oauth/lock/telemetry.write")
[[ -z "$WRITE_TELEMETRY_TOKEN" ]] && { echo "Failed to obtain telemetry.write token"; exit 1; }
echo "Telemetry write token obtained"

READ_LOG_TOKEN=$(get_token "https://jans.io/oauth/lock/log.read")
[[ -z "$READ_LOG_TOKEN" ]] && { echo "Failed to obtain log.read token"; exit 1; }
echo "Log read token obtained"

READ_HEALTH_TOKEN=$(get_token "https://jans.io/oauth/lock/health.read")
[[ -z "$READ_HEALTH_TOKEN" ]] && { echo "Failed to obtain health.read token"; exit 1; }
echo "Health read token obtained"

READ_TELEMETRY_TOKEN=$(get_token "https://jans.io/oauth/lock/telemetry.read")
[[ -z "$READ_TELEMETRY_TOKEN" ]] && { echo "Failed to obtain telemetry.read token"; exit 1; }
echo "Telemetry read token obtained"

echo -e "\n2. Tests with valid write tokens (should succeed)\n"

echo -e "\n── Log ───────────────────────────────────────────────"
check_http_success "curl -k -H 'Authorization: Bearer $WRITE_LOG_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/log' <<< '$SINGLE_LOG_JSON_REST'" "REST → single log entry"
check_http_success "curl -k -H 'Authorization: Bearer $WRITE_LOG_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/log/bulk' <<< '$BULK_LOG_JSON_REST'" "REST → bulk log entries"
check_grpc_success "grpcurl -insecure --proto audit.proto -H 'authorization: bearer $WRITE_LOG_TOKEN' -d '$SINGLE_LOG_GRPC' $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessLog" "gRPC → ProcessLog"
check_grpc_success "grpcurl -insecure --proto audit.proto -H 'authorization: bearer $WRITE_LOG_TOKEN' -H 'GRPC_APP: jans-lock' -d '$BULK_LOG_GRPC'  $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessBulkLog" "gRPC → ProcessBulkLog"

echo -e "\n── Health ───────────────────────────────────────────"
check_http_success "curl -k -H 'Authorization: Bearer $WRITE_HEALTH_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/health' <<< '$SINGLE_HEALTH_JSON_REST'" "REST → single health entry"
check_http_success "curl -k -H 'Authorization: Bearer $WRITE_HEALTH_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/health/bulk' <<< '$BULK_HEALTH_JSON_REST'" "REST → bulk health entries"
check_grpc_success "grpcurl -insecure --proto audit.proto -H 'authorization: bearer $WRITE_HEALTH_TOKEN' -d '$SINGLE_HEALTH_GRPC' $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessHealth" "gRPC → ProcessHealth"
check_grpc_success "grpcurl -insecure --proto audit.proto -H 'authorization: bearer $WRITE_HEALTH_TOKEN' -d '$BULK_HEALTH_GRPC'  $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessBulkHealth" "gRPC → ProcessBulkHealth"

echo -e "\n── Telemetry ─────────────────────────────────────────"
check_http_success "curl -k -H 'Authorization: Bearer $WRITE_TELEMETRY_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/telemetry' <<< '$SINGLE_TELEMETRY_JSON_REST'" "REST → single telemetry entry"
check_http_success "curl -k -H 'Authorization: Bearer $WRITE_TELEMETRY_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/telemetry/bulk' <<< '$BULK_TELEMETRY_JSON_REST'" "REST → bulk telemetry entries"
check_grpc_success "grpcurl -insecure --proto audit.proto -H 'authorization: bearer $WRITE_TELEMETRY_TOKEN' -d '$SINGLE_TELEMETRY_GRPC' $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessTelemetry" "gRPC → ProcessTelemetry"
check_grpc_success "grpcurl -insecure --proto audit.proto -H 'authorization: bearer $WRITE_TELEMETRY_TOKEN' -d '$BULK_TELEMETRY_GRPC'  $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessBulkTelemetry" "gRPC → ProcessBulkTelemetry"

echo -e "\n3. Tests with invalid write tokens → write operations should fail\n"

echo -e "\n── Log with invalid scope ────────────────────────────"
check_http_should_fail "curl -k -H 'Authorization: Bearer $WRITE_HEALTH_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/log' <<< '$SINGLE_LOG_JSON_REST'" "REST → single log entry"
check_grpc_should_fail "grpcurl -insecure --proto audit.proto -H 'authorization: bearer $WRITE_HEALTH_TOKEN' -d '$SINGLE_LOG_GRPC' $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessLog" "gRPC → ProcessLog"

echo -e "\n── Health with invalid scope ─────────────────────────"
check_http_should_fail "curl -k -H 'Authorization: Bearer $WRITE_LOG_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/health' <<< '$SINGLE_HEALTH_JSON_REST'" "REST → single health entry"
check_grpc_should_fail "grpcurl -insecure --proto audit.proto -H 'authorization: bearer $WRITE_LOG_TOKEN' -d '$SINGLE_HEALTH_GRPC' $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessHealth" "gRPC → ProcessHealth"

echo -e "\n── Telemetry with invalid scope ──────────────────────"
check_http_should_fail "curl -k -H 'Authorization: Bearer $WRITE_LOG_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/telemetry' <<< '$SINGLE_TELEMETRY_JSON_REST'" "REST → single telemetry entry"
check_grpc_should_fail "grpcurl -insecure --proto audit.proto -H 'authorization: bearer $WRITE_LOG_TOKEN' -d '$SINGLE_TELEMETRY_GRPC' $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessTelemetry" "gRPC → ProcessTelemetry"

echo -e "\n4. Tests with read-only tokens → write operations should fail\n"

echo -e "\n── Log read token ────────────────────────────────────"
check_http_should_fail "curl -k -H 'Authorization: Bearer $READ_LOG_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/log' <<< '$SINGLE_LOG_JSON_REST'" "REST → single log entry with read-only token"
check_http_should_fail "curl -k -H 'Authorization: Bearer $READ_LOG_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/log/bulk' <<< '$BULK_LOG_JSON_REST'" "REST → bulk log entries with read-only token"
check_grpc_should_fail "grpcurl -insecure --proto audit.proto -H 'authorization: bearer $READ_LOG_TOKEN' -d '$SINGLE_LOG_GRPC' $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessLog" "gRPC → ProcessLog with read-only token"
check_grpc_should_fail "grpcurl -insecure --proto audit.proto -H 'authorization: bearer $READ_LOG_TOKEN' -d '$BULK_LOG_GRPC'  $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessBulkLog" "gRPC → ProcessBulkLog with read-only token"

echo -e "\n── Health read token ─────────────────────────────────"
check_http_should_fail "curl -k -H 'Authorization: Bearer $READ_HEALTH_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/health' <<< '$SINGLE_HEALTH_JSON_REST'" "REST → single health entry with read-only token"
check_http_should_fail "curl -k -H 'Authorization: Bearer $READ_HEALTH_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/health/bulk' <<< '$BULK_HEALTH_JSON_REST'" "REST → bulk health entries with read-only token"
check_grpc_should_fail "grpcurl -insecure --proto audit.proto -H 'authorization: bearer $READ_HEALTH_TOKEN' -d '$SINGLE_HEALTH_GRPC' $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessHealth" "gRPC → ProcessHealth with read-only token"
check_grpc_should_fail "grpcurl -insecure --proto audit.proto -H 'authorization: bearer $READ_HEALTH_TOKEN' -d '$BULK_HEALTH_GRPC'  $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessBulkHealth" "gRPC → ProcessBulkHealth with read-only token"

echo -e "\n── Telemetry read token ──────────────────────────────"
check_http_should_fail "curl -k -H 'Authorization: Bearer $READ_TELEMETRY_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/telemetry' <<< '$SINGLE_TELEMETRY_JSON_REST'" "REST → single telemetry entry with read-only token"
check_http_should_fail "curl -k -H 'Authorization: Bearer $READ_TELEMETRY_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/telemetry/bulk' <<< '$BULK_TELEMETRY_JSON_REST'" "REST → bulk telemetry entries with read-only token"
check_grpc_should_fail "grpcurl -insecure --proto audit.proto -H 'authorization: bearer $READ_TELEMETRY_TOKEN' -d '$SINGLE_TELEMETRY_GRPC' $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessTelemetry" "gRPC → ProcessTelemetry with read-only token"
check_grpc_should_fail "grpcurl -insecure --proto audit.proto -H 'authorization: bearer $READ_TELEMETRY_TOKEN' -d '$BULK_TELEMETRY_GRPC'  $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessBulkTelemetry" "gRPC → ProcessBulkTelemetry with read-only token"

echo -e "\n5. Tests with invalid token (should fail)\n"

echo -e "\n── Log invalid token ────────────────────────────────────"
check_http_should_fail "curl -k -H 'Authorization: Bearer $INVALID_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/log' <<< '$SINGLE_LOG_JSON_REST'" "REST → single log entry with invalid token"
check_http_should_fail "curl -k -H 'Authorization: Bearer $INVALID_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/log/bulk' <<< '$BULK_LOG_JSON_REST'" "REST → bulk log entries with invalid token"
check_grpc_should_fail "grpcurl -insecure --proto audit.proto -H 'authorization: bearer $INVALID_TOKEN' -d '$SINGLE_LOG_GRPC' $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessLog" "gRPC → ProcessLog with invalid token"
check_grpc_should_fail "grpcurl -insecure --proto audit.proto -H 'authorization: bearer $INVALID_TOKEN' -d '$BULK_LOG_GRPC'  $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessBulkLog" "gRPC → ProcessBulkLog with invalid token"

echo -e "\n── Health invalid token ─────────────────────────────────"
check_http_should_fail "curl -k -H 'Authorization: Bearer $INVALID_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/health' <<< '$SINGLE_HEALTH_JSON_REST'" "REST → single health entry with invalid token"
check_http_should_fail "curl -k -H 'Authorization: Bearer $INVALID_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/health/bulk' <<< '$BULK_HEALTH_JSON_REST'" "REST → bulk health entries with invalid token"
check_grpc_should_fail "grpcurl -insecure --proto audit.proto -H 'authorization: bearer $INVALID_TOKEN' -d '$SINGLE_HEALTH_GRPC' $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessHealth" "gRPC → ProcessHealth with invalid token"
check_grpc_should_fail "grpcurl -insecure --proto audit.proto -H 'authorization: bearer $INVALID_TOKEN' -d '$BULK_HEALTH_GRPC'  $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessBulkHealth" "gRPC → ProcessBulkHealth with invalid token"

echo -e "\n── Telemetry invalid token ──────────────────────────────"
check_http_should_fail "curl -k -H 'Authorization: Bearer $INVALID_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/telemetry' <<< '$SINGLE_TELEMETRY_JSON_REST'" "REST → single telemetry entry with invalid token"
check_http_should_fail "curl -k -H 'Authorization: Bearer $INVALID_TOKEN' -H 'Content-Type: application/json' -d @- '${HOST}/jans-lock/api/v1/audit/telemetry/bulk' <<< '$BULK_TELEMETRY_JSON_REST'" "REST → bulk telemetry entries with invalid token"
check_grpc_should_fail "grpcurl -insecure --proto audit.proto -H 'authorization: bearer $INVALID_TOKEN' -H 'GRPC_APP: jans-lock' -d '$SINGLE_TELEMETRY_GRPC' $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessTelemetry" "gRPC → ProcessTelemetry with invalid token"
check_grpc_should_fail "grpcurl -insecure --proto audit.proto -H 'authorization: bearer $INVALID_TOKEN' -d '$BULK_TELEMETRY_GRPC'  $GRPC_ADDR io.jans.lock.audit.AuditService/ProcessBulkTelemetry" "gRPC → ProcessBulkTelemetry with invalid token"

echo -e "\n┌──────────────────────────────┐"
echo   "│         TESTS PASSED         │"
echo   "└──────────────────────────────┘"
