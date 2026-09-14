#!/usr/bin/env bash
# One-time migration: move the Helm chart binaries that accumulated on gh-pages
# to the GitHub release they belong to, so the branch only has to carry the
# index. Charts released before v2.0.0 predate Cosign signing and move unsigned.
#
# Usage: backfill-chart-releases.sh <charts-dir> [--dry-run|--cleanup]
#
# --cleanup additionally deletes each successfully uploaded file from
# <charts-dir>. Point it at a gh-pages checkout and commit and push the
# resulting deletions to take the binaries off the branch; only the index
# needs to stay.
set -euo pipefail

CHARTS_DIR="${1:?usage: backfill-chart-releases.sh <charts-dir> [--dry-run|--cleanup]}"
MODE="${2:-}"
REPO="${REPO:-JanssenProject/jans}"

uploaded=0
skipped=0
missing=0

for file in "${CHARTS_DIR}"/*.tgz "${CHARTS_DIR}"/*.tgz.sigstore.json; do
  [ -e "$file" ] || continue
  base="$(basename "$file")"
  stem="${base%.sigstore.json}"
  stem="${stem%.tgz}"

  if [[ "$stem" =~ -([0-9]+\.[0-9]+\.[0-9]+(-[A-Za-z0-9.]+)?)$ ]]; then
    version="${BASH_REMATCH[1]}"
  else
    echo "SKIP  cannot parse version from ${base}" >&2
    skipped=$((skipped + 1))
    continue
  fi

  if [[ "$version" == *-nightly ]]; then
    tag="nightly"
  else
    tag="v${version}"
  fi

  if ! gh release view "$tag" --repo "$REPO" >/dev/null 2>&1; then
    echo "MISS  no release ${tag} for ${base}" >&2
    missing=$((missing + 1))
    continue
  fi

  if [ "$MODE" = "--dry-run" ]; then
    echo "would upload ${base} -> ${tag}"
  else
    gh release upload "$tag" "$file" --clobber --repo "$REPO"
    echo "uploaded ${base} -> ${tag}"
    if [ "$MODE" = "--cleanup" ]; then
      rm -f "$file"
      echo "removed ${base} from ${CHARTS_DIR}"
    fi
  fi
  uploaded=$((uploaded + 1))
done

echo "charts: ${uploaded} to upload, ${skipped} unparsed, ${missing} without a release"
[ "$missing" -eq 0 ] && [ "$skipped" -eq 0 ]
