#!/usr/bin/env bash
# check-policy-checklist.sh
#
# Validates that every .cedar policy file being introduced or modified follows
# the role-addition checklist documented in PERMISSIONS.md:
#
#   1. The role name extracted from the policy's principal clause is present in
#      the Roles table (## Roles section) of PERMISSIONS.md.
#   2. At least one test case in test-cases.yml references that role name in its
#      `roles:` field.
#
# Usage:
#   bash check-policy-checklist.sh [file.cedar ...]
#
#   When called with no arguments the script checks every .cedar file found in
#   policy-store/policies/ relative to this script's directory.
#
#   Pass one or more paths explicitly to check only those files.  Paths may be
#   absolute, CWD-relative (e.g. as returned by `git diff --name-only`), or
#   relative to this script's directory.  The script resolves them in that
#   order of preference.
#
# Exit codes:
#   0 — all checks passed
#   1 — one or more checks failed (details printed to stderr)
#   2 — usage or unrecoverable setup error

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
POLICY_DIR="$SCRIPT_DIR/policy-store/policies"
PERMISSIONS_FILE="$SCRIPT_DIR/PERMISSIONS.md"
TEST_CASES_FILE="$SCRIPT_DIR/test-cases.yml"

# ── Helpers ──────────────────────────────────────────────────────────────────

# Resolve a path to an absolute path using three strategies, in order:
#   1. Already absolute — use as-is.
#   2. Relative to CWD — check if the file exists there first.
#   3. Relative to SCRIPT_DIR — fall back for paths like "policy-store/...".
# Returns the resolved path on stdout, or an empty string if none found.
resolve_path() {
  local p="$1"
  if [[ "$p" == /* ]]; then
    echo "$p"
    return
  fi
  if [ -f "$p" ]; then
    echo "$(pwd)/$p"
    return
  fi
  if [ -f "$SCRIPT_DIR/$p" ]; then
    echo "$SCRIPT_DIR/$p"
    return
  fi
  echo ""
}

# Extract role names from a Cedar policy file.
# Handles two forms:
#   principal in Infra::Role::"RoleName"  (entity-hierarchy check)
#   principal == Infra::Role::"RoleName"  (entity-hierarchy check)
#   principal.role.contains("RoleName")   (attribute containment check — used
#       by the Cedarling unsigned-authorize path which stores roles as a
#       Set<String> attribute rather than entity-hierarchy parents)
# Comment lines (// ...) are excluded so that role strings inside commented-out
# example blocks do not generate false positive role requirements.
extract_roles() {
  local file="$1"
  {
    # Entity-hierarchy form: Infra::Role::"RoleName"
    grep -v '^[[:space:]]*//' "$file" \
      | grep -oE 'Infra::Role::"[^"]+"' \
      | sed 's/Infra::Role::"\([^"]*\)"/\1/' \
      || true

    # Attribute containment form: principal.role.contains("RoleName")
    grep -v '^[[:space:]]*//' "$file" \
      | grep -oE '\.role\.contains\("[^"]+"\)' \
      | sed 's/\.role\.contains("\([^"]*\)")/\1/' \
      || true
  } | sort -u
}

# Return 0 if the given role name appears in the ## Roles table of PERMISSIONS.md.
# The Roles section starts at "## Roles" and ends at the next "---" separator.
role_in_permissions_table() {
  local role="$1"
  awk '/^## Roles/{found=1; next} /^---/ && found{found=0; next} found' \
    "$PERMISSIONS_FILE" \
    | grep -qF "\"$role\""
}

# Return 0 if the given role name appears in at least one test-cases.yml entry.
# The roles: field can hold a single name or a comma-separated list, possibly
# wrapped in quotes: roles: Admin  or  roles: "Developer,Ops"
# The role name is escaped for regex so metacharacters in future role names do
# not cause incorrect matches or unexpected failures.
role_in_test_cases() {
  local role="$1"
  local escaped_role
  escaped_role="$(printf '%s' "$role" | sed 's/[][\\^$.*+?{}|()]/\\&/g')"
  grep -E '^[[:space:]]+roles:[[:space:]]' "$TEST_CASES_FILE" \
    | grep -qE "(^|[[:space:],\"'])${escaped_role}([[:space:],\"']|$)"
}

# ── Resolve the list of .cedar files to validate ──────────────────────────────

explicit_mode=0
cedar_files=()

if [ $# -gt 0 ]; then
  explicit_mode=1
  for arg in "$@"; do
    cedar_files+=("$arg")
  done
else
  mapfile -t cedar_files < <(find "$POLICY_DIR" -maxdepth 1 -name '*.cedar' | sort)
fi

if [ ${#cedar_files[@]} -eq 0 ]; then
  echo "No .cedar files to check." >&2
  exit 0
fi

# ── Validate each file ────────────────────────────────────────────────────────

errors=0
files_checked=0

for cedar_file in "${cedar_files[@]}"; do
  # Skip non-.cedar files that might have been passed by mistake (e.g. from a
  # broad git diff that included other file types).
  if [[ "$cedar_file" != *.cedar ]]; then
    continue
  fi

  resolved="$(resolve_path "$cedar_file")"
  if [ -z "$resolved" ] || [ ! -f "$resolved" ]; then
    echo "WARNING: could not find '$cedar_file' — skipping." >&2
    continue
  fi

  cedar_file="$resolved"
  basename_file="$(basename "$cedar_file")"
  files_checked=$((files_checked + 1))

  # Extract all role names from this policy file.
  mapfile -t roles < <(extract_roles "$cedar_file")

  if [ ${#roles[@]} -eq 0 ]; then
    echo "WARNING: $basename_file — no Infra::Role principal found; skipping role checks." >&2
    continue
  fi

  for role in "${roles[@]}"; do
    echo "Checking role \"$role\" from $basename_file ..."

    # Check 1: Role must appear in the Roles table of PERMISSIONS.md.
    if ! role_in_permissions_table "$role"; then
      echo "  ERROR: Role \"$role\" is not listed in the Roles table (## Roles section) of PERMISSIONS.md." >&2
      echo "         Add a row for \"$role\" to the Roles table before merging." >&2
      errors=$((errors + 1))
    else
      echo "  OK: \"$role\" found in PERMISSIONS.md Roles table."
    fi

    # Check 2: At least one test case must reference this role.
    if ! role_in_test_cases "$role"; then
      echo "  ERROR: No test cases found for role \"$role\" in test-cases.yml." >&2
      echo "         Add ALLOWED and DENIED test cases for \"$role\" before merging." >&2
      errors=$((errors + 1))
    else
      echo "  OK: \"$role\" has at least one test case in test-cases.yml."
    fi
  done
done

# When the caller passed explicit paths, fail hard if none of them could be
# resolved — this prevents silent success when all inputs were unreachable.
if [ "$explicit_mode" -eq 1 ] && [ "$files_checked" -eq 0 ]; then
  echo "" >&2
  echo "ERROR: none of the provided .cedar file path(s) could be resolved." >&2
  echo "Paths provided:" >&2
  for arg in "$@"; do echo "  $arg" >&2; done
  exit 1
fi

# ── Summary ───────────────────────────────────────────────────────────────────

if [ "$errors" -gt 0 ]; then
  echo "" >&2
  echo "check-policy-checklist: $errors error(s) found." >&2
  echo "See PERMISSIONS.md → 'How to extend the model' for the required steps." >&2
  exit 1
fi

echo ""
echo "check-policy-checklist: all checks passed ($files_checked file(s) examined)."
