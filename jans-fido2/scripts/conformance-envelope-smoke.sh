#!/usr/bin/env bash
#
# Layer 3 runtime smoke-test for the FIDO2 conformance response envelope (PR-1: CONF-01..05).
# Exercises the real fido2-server.war over HTTP and checks that every response carries the
# {status, errorMessage} envelope and the HTTP status you expect.
#
# Usage:
#   BASE=https://your-fido2-host scripts/conformance-envelope-smoke.sh
#
# Requires: curl, and (optionally) jq for pretty output.

set -u
BASE="${BASE:-https://localhost}"
ATT="$BASE/jans-fido2/restv1/attestation/options"
ASS="$BASE/jans-fido2/restv1/assertion/options"

pp() { if command -v jq >/dev/null 2>&1; then jq -c .; else cat; fi; }

call() {
  local label="$1"; local url="$2"; local body="${3-}"; shift 3 || true
  echo "──────────────────────────────────────────────────────────────"
  echo "▶ $label"
  if [ -z "${body+x}" ] || [ "$body" = "__NOBODY__" ]; then
    code=$(curl -sk -o /tmp/_fido_body -w '%{http_code}' -X POST "$url" -H 'Content-Type: application/json')
  else
    code=$(curl -sk -o /tmp/_fido_body -w '%{http_code}' -X POST "$url" -H 'Content-Type: application/json' -d "$body")
  fi
  echo "  HTTP $code"
  printf '  body: '; cat /tmp/_fido_body | pp
  echo
}

echo "Target: $BASE"
echo "Expected: success -> {\"status\":\"ok\",\"errorMessage\":\"\"} ; failure -> {\"status\":\"failed\",\"errorMessage\":\"<non-empty>\"}"
echo

# CONF-01 : valid attestation options -> status:ok
call "CONF-01 valid attestation/options (expect status:ok)" "$ATT" '{"username":"smoke","displayName":"Smoke Test"}'

# CONF-02 : missing username (Fido2RuntimeException) -> status:failed
call "CONF-02 missing username (expect status:failed)" "$ATT" '{}'

# CONF-03 : malformed JSON -> JsonProcessingExceptionMapper -> status:failed
call "CONF-03 malformed JSON body (expect status:failed)" "$ATT" '{bad json'

# CONF-03 : invalid enum value for attestation -> status:failed
call "CONF-03 invalid attestation enum (expect status:failed)" "$ATT" '{"username":"smoke","displayName":"Smoke","attestation":"bogus"}'

# CONF-04 : empty body -> ConstraintViolationExceptionMapper (@NotNull) -> status:failed
call "CONF-04 empty body (expect status:failed)" "$ATT" "__NOBODY__"

# CONF-01 : valid assertion options -> status:ok
call "CONF-01 valid assertion/options (expect status:ok)" "$ASS" '{"username":"smoke"}'

echo "Done. Verify: every body is the {status,errorMessage} envelope, and decide whether the"
echo "FIDO Conformance Tool needs HTTP 200 (vs 400) on the failed cases (see CONFORMANCE_AUDIT.md, Phase 1 note #1)."
