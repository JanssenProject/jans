#!/usr/bin/env bash
# Craft a demo-mode JWT from a claims JSON object. OPA runs with signature
# validation disabled in demo mode, so the signature segment is a placeholder
# accepted as-is — this lets deny-path tests submit synthetic foreign claims.
#
# Usage: authz_make_jwt.sh '<claims-json>'  ->  header.payload.sig
set -euo pipefail

CLAIMS=$1
b64() { base64 | tr '+/' '-_' | tr -d '=\n'; }

H=$(printf '%s' '{"alg":"HS256","typ":"JWT"}' | b64)
P=$(printf '%s' "$CLAIMS" | b64)
S=$(printf '%s' 'fakesignature' | b64)
printf '%s.%s.%s' "$H" "$P" "$S"
