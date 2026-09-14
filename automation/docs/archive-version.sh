#!/usr/bin/env bash
# Sign and publish the documentation archives produced by `publish-docs.py prune`.
# Each docs-<version>.tar.gz is attached to its own release, which is the
# "Legacy Archive" that docs/EOL.md promises for end-of-life versions.
#
# Usage: archive-version.sh <archive-dir> [--dry-run]
set -euo pipefail

ARCHIVE_DIR="${1:?usage: archive-version.sh <archive-dir> [--dry-run]}"
DRY_RUN="${2:-}"
REPO="${REPO:-JanssenProject/jans}"

shopt -s nullglob
archives=("${ARCHIVE_DIR}"/docs-*.tar.gz)
if [ ${#archives[@]} -eq 0 ]; then
  echo "No archives in ${ARCHIVE_DIR}; nothing to publish."
  exit 0
fi

for archive in "${archives[@]}"; do
  base="$(basename "$archive")"
  version="${base#docs-}"
  version="${version%.tar.gz}"

  if ! gh release view "$version" --repo "$REPO" >/dev/null 2>&1; then
    echo "MISS  no release ${version} for ${base}" >&2
    exit 1
  fi

  if [ "$DRY_RUN" = "--dry-run" ]; then
    echo "would sign and upload ${base} -> ${version}"
    continue
  fi

  cosign sign-blob --yes --bundle "${archive}.bundle" "$archive"
  gh release upload "$version" "$archive" "${archive}.bundle" --clobber --repo "$REPO"
  echo "published ${base} -> ${version}"
done
