#!/bin/bash

# =============================================================================
# Manual end-to-end test for the Lock Server TRACE endpoints
#
#   POST /api/v1/audit/trace                                   (trace.write)
#   GET  /api/v1/audit/trace/records/{record_id}               (trace.readonly)
#   GET  /api/v1/audit/trace/executions/{trace_execution_id}   (trace.readonly)
#   POST/GET /api/v1/audit/trace/admin/producer-keys[/revoke]  (trace.admin)
#   POST/GET /api/v1/audit/trace/admin/producer-chains         (trace.admin)
#
# Requirements: bash, curl, jq, and python3 with the `cryptography` package
# (or `uv`, which is used to fetch `cryptography` on the fly).
#
# Wire shape: this script produces records in the shape the server currently
# implements (trace.event.capability_ids[], producer ids of the form name/semver).
# Update the templates below when the server moves to the Oct 13 design shape.
#
# Prerequisites on the Lock Server under test
#   * traceConfiguration.enabled = true
#   * CLIENT_ID is bound to an evidence domain (clientDomainBindings, or
#     defaultEvidenceDomainId) and its allowedProducerIds covers the producers
#     used here (e2e-pdp-<run>/1.0.0, e2e-pep-<run>/1.0.0) or contains "*".
#   * CLIENT_ID has been granted the three TRACE scopes manually. The client
#     created by jans_lock.py gets its scopes from lock-plugin-swagger.yaml
#     (config-api plugin), not from Lock's own OpenAPI file, so it does NOT have
#     them by default:
#       https://jans.io/oauth/lock/trace.admin
#       https://jans.io/oauth/lock/trace.write
#       https://jans.io/oauth/lock/trace.readonly
#   * For the wrong-scope check the client should also hold
#     https://jans.io/oauth/lock/log.write (the section is skipped otherwise).
#
# Configuration (environment variables; placeholders below are NOT real secrets)
#   HOST            https://server.example.org
#   CLIENT_ID       "2200...."
#   CLIENT_SECRET   "..."
#   INSECURE        set to 1 to add curl -k (self-signed test hosts only)
#   OTHER_CLIENT_ID / OTHER_CLIENT_SECRET
#                   optional: a client bound to a DIFFERENT evidence domain and
#                   holding trace.readonly; enables the cross-domain 404 check.
#
# Producer keys are generated per run, kept in a mode-700 temp directory and
# removed on exit. Keys cannot be deleted from Lock, so every run registers new
# uniquely named keys and chains.
# =============================================================================

set -euo pipefail

HOST="${HOST:-https://server.example.org}"
CLIENT_ID="${CLIENT_ID:-2200....}"
CLIENT_SECRET="${CLIENT_SECRET:-...}"
OTHER_CLIENT_ID="${OTHER_CLIENT_ID:-}"
OTHER_CLIENT_SECRET="${OTHER_CLIENT_SECRET:-}"
INSECURE="${INSECURE:-0}"

API="${HOST}/jans-lock/api/v1/audit/trace"
TOKEN_URL="${HOST}/jans-auth/restv1/token"

SCOPE_ADMIN="https://jans.io/oauth/lock/trace.admin"
SCOPE_WRITE="https://jans.io/oauth/lock/trace.write"
SCOPE_READ="https://jans.io/oauth/lock/trace.readonly"
SCOPE_WRONG="https://jans.io/oauth/lock/log.write"

ZERO_HASH="sha256:0000000000000000000000000000000000000000000000000000000000000000"

CURL_BASE=(-sS)
[[ "$INSECURE" == "1" ]] && CURL_BASE+=(-k)

PASS=0
FAIL=0
SKIP=0

# ─────────────────────────────────────────────────────────────────────────────
# Tooling checks and signing helper
# ─────────────────────────────────────────────────────────────────────────────

for tool in curl jq; do
  command -v "$tool" >/dev/null 2>&1 || { echo "❌ required tool not found: $tool"; exit 1; }
done

if command -v python3 >/dev/null 2>&1 && python3 -c 'import cryptography' >/dev/null 2>&1; then
  PY=(python3)
elif command -v uv >/dev/null 2>&1; then
  PY=(uv run --quiet --with cryptography python3)
else
  echo "❌ need python3 with the 'cryptography' package, or uv (to fetch it)."
  exit 1
fi

WORK="$(mktemp -d)"
chmod 700 "$WORK"
trap 'rm -rf "$WORK"' EXIT

cat >"$WORK/trace_sign.py" <<'PYEOF'
# Standard library + cryptography. Canonicalization is RFC 8785 for the subset used
# here: integers, booleans, null and BMP strings only (no floats, no astral code points).
import base64, hashlib, json, os, sys
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey
from cryptography.hazmat.primitives import serialization

def b64u(data):
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode()

def jcs(value):
    return json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode("utf-8")

def load_key(path):
    with open(path, "r") as fh:
        seed = base64.urlsafe_b64decode(fh.read().strip() + "==")
    return Ed25519PrivateKey.from_private_bytes(seed)

cmd = sys.argv[1]
if cmd == "gen":
    key = Ed25519PrivateKey.generate()
    seed = key.private_bytes(serialization.Encoding.Raw, serialization.PrivateFormat.Raw,
                             serialization.NoEncryption())
    fd = os.open(sys.argv[2], os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    with os.fdopen(fd, "w") as fh:
        fh.write(b64u(seed))
    pub = key.public_key().public_bytes(serialization.Encoding.Raw, serialization.PublicFormat.Raw)
    print(b64u(pub))
elif cmd == "sign":
    key = load_key(sys.argv[2])
    doc = json.load(sys.stdin)
    doc.pop("signature", None)
    doc["signature"] = b64u(key.sign(jcs(doc)))
    sys.stdout.write(json.dumps(doc, separators=(",", ":"), ensure_ascii=False))
elif cmd == "tamper-signature":
    doc = json.load(sys.stdin)
    sig = doc["signature"]
    doc["signature"] = ("B" if sig[0] == "A" else "A") + sig[1:]
    sys.stdout.write(json.dumps(doc, separators=(",", ":"), ensure_ascii=False))
elif cmd == "sha256":
    print("sha256:" + hashlib.sha256(sys.argv[2].encode("utf-8")).hexdigest())
elif cmd == "uuid":
    import uuid
    print(uuid.uuid4())
else:
    sys.exit("unknown command: " + cmd)
PYEOF

py() { "${PY[@]}" "$WORK/trace_sign.py" "$@"; }

# ─────────────────────────────────────────────────────────────────────────────
# Helper functions
# ─────────────────────────────────────────────────────────────────────────────

HTTP_STATUS=""
HTTP_BODY=""

pass() { PASS=$((PASS + 1)); echo "✅  $1"; }
fail() { FAIL=$((FAIL + 1)); echo "❌  $1"; [[ -n "${2:-}" ]] && echo "    $2"; return 0; }
skip() { SKIP=$((SKIP + 1)); echo "⚠️  skipped: $1"; }

# Credentials go to curl through a config on stdin, tokens through a config
# file inside the private temp directory: neither shows up in the process list.
get_token() {
  local id="$1" secret="$2" scope="$3"
  printf 'user = "%s:%s"\n' "$id" "$secret" \
    | curl "${CURL_BASE[@]}" -K - "$TOKEN_URL" \
        -d "grant_type=client_credentials" -d "scope=${scope}" \
    | jq -r '.access_token // empty'
}

# http METHOD URL TOKEN [extra curl args...]  -> sets HTTP_STATUS, HTTP_BODY
# An empty TOKEN sends no Authorization header.
http() {
  local method="$1" url="$2" token="$3"
  shift 3
  local out="$WORK/response.body" cfg="$WORK/request.curlrc" auth=()
  if [[ -n "$token" ]]; then
    printf 'header = "Authorization: Bearer %s"\n' "$token" >"$cfg"
    auth=(-K "$cfg")
  fi
  HTTP_STATUS="$(curl "${CURL_BASE[@]}" -o "$out" -w '%{http_code}' -X "$method" "${auth[@]}" "$@" "$url")" || HTTP_STATUS="000"
  rm -f "$cfg"
  HTTP_BODY="$(cat "$out" 2>/dev/null || true)"
}

post_json() { # URL TOKEN BODY
  http POST "$1" "$2" -H 'Content-Type: application/json' --data-binary "$3"
}

expect_status() { # DESC EXPECTED_STATUS [EXPECTED_ERROR_ID]
  local desc="$1" want="$2" err="${3:-}"
  if [[ "$HTTP_STATUS" != "$want" ]]; then
    fail "$desc (HTTP $HTTP_STATUS, expected $want)" "$HTTP_BODY"
    return 0
  fi
  if [[ -n "$err" ]]; then
    local got
    got="$(jq -r '.error // empty' <<<"$HTTP_BODY" 2>/dev/null || true)"
    if [[ "$got" != "$err" ]]; then
      fail "$desc (HTTP $HTTP_STATUS, error '$got', expected '$err')" "$HTTP_BODY"
      return 0
    fi
    pass "$desc (HTTP $HTTP_STATUS, $err)"
  else
    pass "$desc (HTTP $HTTP_STATUS)"
  fi
}

expect_auth_denied() { # DESC  (401 or 403)
  if [[ "$HTTP_STATUS" == "401" || "$HTTP_STATUS" == "403" ]]; then
    pass "(expected failure) $1 (HTTP $HTTP_STATUS)"
  else
    fail "UNEXPECTED $1 (HTTP $HTTP_STATUS, expected 401/403)" "$HTTP_BODY"
  fi
}

expect_jq() { # DESC JQ_FILTER (must evaluate to true against HTTP_BODY)
  if jq -e "$2" >/dev/null 2>&1 <<<"$HTTP_BODY"; then
    pass "$1"
  else
    fail "$1" "filter: $2 | body: $HTTP_BODY"
  fi
}

# ─────────────────────────────────────────────────────────────────────────────
# Record builders (current implemented wire shape)
# ─────────────────────────────────────────────────────────────────────────────

RUN_ID="$(date +%s)$RANDOM"
EXEC_ID="exec-e2e-${RUN_ID}"
EXEC_AUTHORITY="spiffe://example.org/agent/e2e-${RUN_ID}"

PRODUCER_PDP="e2e-pdp-${RUN_ID}/1.0.0"
PRODUCER_PEP="e2e-pep-${RUN_ID}/1.0.0"
KID_PDP="e2e-pdp-${RUN_ID}-key-1"
KID_PEP="e2e-pep-${RUN_ID}-key-1"
INSTANCE_PDP="e2e-pdp-instance-1"
INSTANCE_PEP="e2e-pep-instance-1"
CHAIN_PDP="e2e-chain-pdp-${RUN_ID}"
CHAIN_PEP="e2e-chain-pep-${RUN_ID}"

BUNDLE_HASH="$(py sha256 "e2e-policy-bundle-${RUN_ID}")"

build_authorization_decision() { # RECORD_ID SEQ PREV_HASH SIGNED_AT -> signed JSON
  jq -n --arg producer "$PRODUCER_PDP" --arg kid "$KID_PDP" --arg rid "$1" --argjson seq "$2" \
        --arg prev "$3" --argjson signed "$4" --arg exec "$EXEC_ID" --arg auth "$EXEC_AUTHORITY" \
        --arg instance "$INSTANCE_PDP" --arg chain "$CHAIN_PDP" --arg bundle "$BUNDLE_HASH" '{
    producer: $producer, kid: $kid, record_id: $rid,
    trace: {
      eat_profile: "tag:jans.io,2026:trace-v1",
      event_kind: "AUTHORIZATION_DECISION",
      signed_at: $signed,
      trace_execution_id: $exec,
      execution_authority: $auth,
      subject: { workload_id: $auth, workload_instance_id: "e2e-workload-instance-1" },
      event: {
        outcome: "ALLOW",
        capability_ids: [ { capability_id: "invoke:payment-authorization", outcome: "ALLOW" } ],
        tokens: [ { issuer: "https://accounts.example.org", token_type: "access_token", jti: ("e2e-jti-" + $rid) } ]
      },
      policy: {
        policy_store_id: "https://example.org/policy-stores/payments",
        policy_store_version: "1.2.3",
        policy_language: "cedar",
        policy_language_version: "4.4.0",
        bundle_hash: $bundle
      },
      runtime: { pdp_id: "e2e-pdp-001" }
    },
    producer_chain: {
      producer_id: $producer, producer_instance_id: $instance, producer_chain_id: $chain,
      sequence_number: $seq, prev_record_hash: $prev
    },
    parent_record_ids: []
  }' | py sign "$WORK/pdp.key"
}

build_capability_invoked() { # RECORD_ID SEQ PREV_HASH SIGNED_AT PARENT_RECORD_ID -> signed JSON
  jq -n --arg producer "$PRODUCER_PEP" --arg kid "$KID_PEP" --arg rid "$1" --argjson seq "$2" \
        --arg prev "$3" --argjson signed "$4" --arg parent "$5" --arg exec "$EXEC_ID" \
        --arg auth "$EXEC_AUTHORITY" --arg instance "$INSTANCE_PEP" --arg chain "$CHAIN_PEP" \
        --arg pdp "$PRODUCER_PDP" '{
    producer: $producer, kid: $kid, record_id: $rid,
    trace: {
      eat_profile: "tag:jans.io,2026:trace-v1",
      event_kind: "CAPABILITY_INVOKED",
      signed_at: $signed,
      trace_execution_id: $exec,
      execution_authority: $auth,
      event: {
        outcome: "INVOKED",
        enforcement_point_id: "e2e-gateway-001",
        capability_ids: [ { capability_id: "invoke:payment-authorization", outcome: "ALLOW" } ],
        tokens: [ { issuer: "https://accounts.example.org", token_type: "access_token", jti: ("e2e-jti-" + $parent) } ]
      }
    },
    producer_chain: {
      producer_id: $producer, producer_instance_id: $instance, producer_chain_id: $chain,
      sequence_number: $seq, prev_record_hash: $prev
    },
    parent_record_ids: [ { producer_id: $pdp, record_id: $parent, relationship_type: "authorized_by" } ]
  }' | py sign "$WORK/pep.key"
}

# ─────────────────────────────────────────────────────────────────────────────
# Main test flow
# ─────────────────────────────────────────────────────────────────────────────

echo "┌────────────────────────────────────────────────────────────────────┐"
echo "│                 TRACE API END-TO-END MANUAL TEST                   │"
echo "└────────────────────────────────────────────────────────────────────┘"
echo "Host: ${HOST}   Run id: ${RUN_ID}"

echo -e "\n1. Obtaining tokens\n"

ADMIN_TOKEN="$(get_token "$CLIENT_ID" "$CLIENT_SECRET" "$SCOPE_ADMIN")"
[[ -z "$ADMIN_TOKEN" ]] && { echo "❌ failed to obtain trace.admin token (is the scope granted to the client?)"; exit 1; }
echo "trace.admin token obtained"

WRITE_TOKEN="$(get_token "$CLIENT_ID" "$CLIENT_SECRET" "$SCOPE_WRITE")"
[[ -z "$WRITE_TOKEN" ]] && { echo "❌ failed to obtain trace.write token"; exit 1; }
echo "trace.write token obtained"

READ_TOKEN="$(get_token "$CLIENT_ID" "$CLIENT_SECRET" "$SCOPE_READ")"
[[ -z "$READ_TOKEN" ]] && { echo "❌ failed to obtain trace.readonly token"; exit 1; }
echo "trace.readonly token obtained"

WRONG_TOKEN="$(get_token "$CLIENT_ID" "$CLIENT_SECRET" "$SCOPE_WRONG")"
if [[ -z "$WRONG_TOKEN" ]]; then
  echo "wrong-scope (log.write) token not available: wrong-scope checks will be skipped"
else
  echo "wrong-scope (log.write) token obtained"
fi

echo -e "\n2. Admin: register producer keys and chains\n"

PUB_PDP="$(py gen "$WORK/pdp.key")"
PUB_PEP="$(py gen "$WORK/pep.key")"
VALID_FROM="$(date -u -d '1 day ago' +%Y-%m-%dT%H:%M:%SZ)"

key_body() { # PRODUCER KID PUBLIC_X
  jq -n --arg p "$1" --arg k "$2" --arg x "$3" --arg vf "$VALID_FROM" \
    '{producer_id: $p, kid: $k, public_key_jwk: {kty: "OKP", crv: "Ed25519", x: $x}, valid_from: $vf, valid_until: null}'
}
chain_body() { # PRODUCER INSTANCE CHAIN
  jq -n --arg p "$1" --arg i "$2" --arg c "$3" \
    '{producer_id: $p, producer_instance_id: $i, producer_chain_id: $c}'
}

KEY_PDP_BODY="$(key_body "$PRODUCER_PDP" "$KID_PDP" "$PUB_PDP")"
KEY_PEP_BODY="$(key_body "$PRODUCER_PEP" "$KID_PEP" "$PUB_PEP")"

post_json "$API/admin/producer-keys" "$ADMIN_TOKEN" "$KEY_PDP_BODY"
expect_status "admin → register PDP producer key" 201
post_json "$API/admin/producer-keys" "$ADMIN_TOKEN" "$KEY_PEP_BODY"
expect_status "admin → register PEP producer key" 201
post_json "$API/admin/producer-keys" "$ADMIN_TOKEN" "$KEY_PDP_BODY"
expect_status "admin → register same PDP key again" 409 "key_already_exists"

post_json "$API/admin/producer-chains" "$ADMIN_TOKEN" "$(chain_body "$PRODUCER_PDP" "$INSTANCE_PDP" "$CHAIN_PDP")"
expect_status "admin → register PDP chain" 201
post_json "$API/admin/producer-chains" "$ADMIN_TOKEN" "$(chain_body "$PRODUCER_PEP" "$INSTANCE_PEP" "$CHAIN_PEP")"
expect_status "admin → register PEP chain" 201
post_json "$API/admin/producer-chains" "$ADMIN_TOKEN" "$(chain_body "$PRODUCER_PDP" "$INSTANCE_PDP" "$CHAIN_PDP")"
expect_status "admin → register same PDP chain again" 409 "chain_already_exists"

http GET "$API/admin/producer-keys" "$ADMIN_TOKEN" -G --data-urlencode "producer_id=${PRODUCER_PDP}"
expect_status "admin → list keys" 200
expect_jq "admin → listed keys contain the registered kid" ".keys | map(.kid) | index(\"${KID_PDP}\") != null"

http GET "$API/admin/producer-chains" "$ADMIN_TOKEN" -G --data-urlencode "producer_id=${PRODUCER_PDP}"
expect_status "admin → list chains" 200
expect_jq "admin → listed chains contain the registered chain" ".chains | map(.producer_chain_id) | index(\"${CHAIN_PDP}\") != null"

echo -e "\n3. Write: submit records\n"

NOW="$(date +%s)"
RID_AUTH="$(py uuid)"
RID_INVOKE="$(py uuid)"

AUTH_JSON="$(build_authorization_decision "$RID_AUTH" 1 "$ZERO_HASH" "$NOW")"
post_json "$API" "$WRITE_TOKEN" "$AUTH_JSON"
expect_status "write → AUTHORIZATION_DECISION (producer seq 1)" 202
expect_jq "write → acceptance is not a replay" '.accepted == true and .idempotent_replay == false'
RECEIPT_AUTH="$(jq -r '.receipt_sequence // empty' <<<"$HTTP_BODY")"
DIGEST_AUTH="$(jq -r '.content_digest // empty' <<<"$HTTP_BODY")"
[[ -n "$RECEIPT_AUTH" ]] && echo "    receipt_sequence=${RECEIPT_AUTH}"

INVOKE_JSON="$(build_capability_invoked "$RID_INVOKE" 1 "$ZERO_HASH" "$NOW" "$RID_AUTH")"
post_json "$API" "$WRITE_TOKEN" "$INVOKE_JSON"
expect_status "write → CAPABILITY_INVOKED (second producer and chain, genesis)" 202
RECEIPT_INVOKE="$(jq -r '.receipt_sequence // empty' <<<"$HTTP_BODY")"
if [[ -n "$RECEIPT_AUTH" && -n "$RECEIPT_INVOKE" && "$RECEIPT_INVOKE" -gt "$RECEIPT_AUTH" ]]; then
  pass "write → second record received a later receipt_sequence (${RECEIPT_INVOKE} > ${RECEIPT_AUTH})"
else
  fail "write → second record receipt_sequence (${RECEIPT_INVOKE:-none}) not greater than first (${RECEIPT_AUTH:-none})"
fi

post_json "$API" "$WRITE_TOKEN" "$AUTH_JSON"
expect_status "write → replay of first record" 202
expect_jq "write → replay returns the original receipt with idempotent_replay=true" \
  ".idempotent_replay == true and .receipt_sequence == ${RECEIPT_AUTH:-0} and .content_digest == \"${DIGEST_AUTH}\""

RESIGNED_JSON="$(build_authorization_decision "$RID_AUTH" 1 "$ZERO_HASH" "$((NOW + 1))")"
post_json "$API" "$WRITE_TOKEN" "$RESIGNED_JSON"
expect_status "write → same identity re-signed with different content" 409 "record_conflict"

TAMPERED_JSON="$(build_authorization_decision "$(py uuid)" 2 "$DIGEST_AUTH" "$NOW" | py tamper-signature)"
post_json "$API" "$WRITE_TOKEN" "$TAMPERED_JSON"
expect_status "write → tampered signature" 400 "invalid_signature"

DOMAIN_JSON="$(build_authorization_decision "$(py uuid)" 2 "$DIGEST_AUTH" "$NOW" | jq -c '. + {evidence_domain_id: "other-domain"}')"
post_json "$API" "$WRITE_TOKEN" "$DOMAIN_JSON"
expect_status "write → body carrying evidence_domain_id" 400 "domain_field_forbidden"

if [[ -n "$WRONG_TOKEN" ]]; then
  post_json "$API" "$WRONG_TOKEN" "$AUTH_JSON"
  expect_auth_denied "write → token without trace.write scope"
else
  skip "write with wrong-scope token"
fi

post_json "$API" "" "$AUTH_JSON"
if [[ "$HTTP_STATUS" == "401" ]]; then
  pass "(expected failure) write → no token (HTTP 401)"
else
  fail "UNEXPECTED write → no token (HTTP $HTTP_STATUS, expected 401)" "$HTTP_BODY"
fi

echo -e "\n4. Read: records and executions\n"

http GET "$API/records/${RID_AUTH}" "$READ_TOKEN" -G --data-urlencode "producer_id=${PRODUCER_PDP}"
expect_status "read → record by id and producer_id" 200
expect_jq "read → envelope matches what was accepted" \
  ".record_id == \"${RID_AUTH}\" and .content_digest == \"${DIGEST_AUTH}\" and .verification.signature_valid == true and .ingestion.receipt_sequence == ${RECEIPT_AUTH:-0}"

http GET "$API/executions/${EXEC_ID}" "$READ_TOKEN" -G --data-urlencode "execution_authority=${EXEC_AUTHORITY}"
expect_status "read → execution" 200
expect_jq "read → execution holds both records ordered by receipt_sequence" \
  '(.records | length) == 2 and ([.records[].ingestion.receipt_sequence] == ([.records[].ingestion.receipt_sequence] | sort))'

http GET "$API/records/${RID_AUTH}" "$READ_TOKEN" -G --data-urlencode "producer_id=${PRODUCER_PDP}-unknown"
expect_status "read → record with unknown producer_id" 404 "record_not_found"

if [[ -n "$WRITE_TOKEN" ]]; then
  http GET "$API/records/${RID_AUTH}" "$WRITE_TOKEN" -G --data-urlencode "producer_id=${PRODUCER_PDP}"
  expect_auth_denied "read → record with trace.write token"
fi

if [[ -n "$OTHER_CLIENT_ID" && -n "$OTHER_CLIENT_SECRET" ]]; then
  OTHER_TOKEN="$(get_token "$OTHER_CLIENT_ID" "$OTHER_CLIENT_SECRET" "$SCOPE_READ")"
  if [[ -z "$OTHER_TOKEN" ]]; then
    skip "cross-domain read: no trace.readonly token for OTHER_CLIENT_ID"
  else
    http GET "$API/records/${RID_AUTH}" "$OTHER_TOKEN" -G --data-urlencode "producer_id=${PRODUCER_PDP}"
    expect_status "read → record through a client of another evidence domain" 404 "record_not_found"
  fi
else
  skip "cross-domain read (set OTHER_CLIENT_ID and OTHER_CLIENT_SECRET to enable)"
fi

echo -e "\n5. Admin: revoke key, then submit with it\n"

post_json "$API/admin/producer-keys/revoke" "$ADMIN_TOKEN" \
  "$(jq -n --arg p "$PRODUCER_PEP" --arg k "$KID_PEP" '{producer_id: $p, kid: $k}')"
expect_status "admin → revoke PEP key" 200
expect_jq "admin → revoked key carries revoked_at" '.revoked_at != null'

sleep 1
REVOKED_JSON="$(build_capability_invoked "$(py uuid)" 2 "$ZERO_HASH" "$(date +%s)" "$RID_AUTH")"
post_json "$API" "$WRITE_TOKEN" "$REVOKED_JSON"
expect_status "write → record signed with the revoked key" 400 "key_not_valid"

echo
echo "Passed: ${PASS}   Failed: ${FAIL}   Skipped: ${SKIP}"
if [[ "$FAIL" -gt 0 ]]; then
  echo -e "\n┌──────────────────────────────┐"
  echo      "│         TESTS FAILED         │"
  echo      "└──────────────────────────────┘"
  exit 1
fi
echo -e "\n┌──────────────────────────────┐"
echo      "│         TESTS PASSED         │"
echo      "└──────────────────────────────┘"
