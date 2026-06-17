#!/usr/bin/env python3
"""Janssen release version bump + verify.

Rewrites every Janssen-owned version from the development sentinels
(``0.0.0-nightly`` / ``0.0.0``) to a release version, in the WORKING TREE only.
``release-trigger.yml`` runs this on a detached HEAD, so ``main`` is never
modified -- the bumped tree exists solely under the release tag.

Why two strategies instead of one regex:

* ``0.0.0-nightly`` is globally unique to our artifacts, so it is safe to
  replace as plain text across whole files. Only a tiny EXCLUDE set is skipped:
  workflow files where the literal is control-flow ("if tag == 0.0.0-nightly"),
  and CHANGELOG.md (history).
* plain ``0.0.0`` also occurs inside unrelated version numbers (e.g. ``70.0.0``),
  IP addresses (``0.0.0.0``) and lockfiles. It is therefore touched ONLY through
  anchored, structured edits on known owned fields -- never a blind scan.

``--verify`` re-scans and exits non-zero if any owned version was missed, so the
release aborts before a bad tag is pushed.
"""

import argparse
import json
import re
import subprocess
import sys
from pathlib import Path

NIGHTLY = "0.0.0-nightly"
# shields.io encodes a literal '-' as '--', so chart README badges carry this form
NIGHTLY_BADGE = "0.0.0--nightly"

# Contain "0.0.0-nightly" as control-flow or history, NOT as an owned version.
EXCLUDE_NIGHTLY = {
    ".github/workflows/build-test.yml",
    ".github/workflows/build-packages.yml",
    ".github/workflows/build-docs.yml",
    "CHANGELOG.md",
}

VERSION_RE = re.compile(r"^\d+\.\d+\.\d+(-[0-9A-Za-z.]+)?$")
SHA_RE = re.compile(r"\b[0-9a-f]{40}\b")

ROOT = Path.cwd()
DRY = False


# --------------------------------------------------------------------------- #
# git helpers (tree is a clean checkout in CI, so tracked files == the world)
# --------------------------------------------------------------------------- #
def _git(*args):
    out = subprocess.run(["git", *args], cwd=ROOT, capture_output=True, text=True)
    # git grep exits 1 when there are no matches; that is not an error here
    if out.returncode not in (0, 1):
        raise RuntimeError(f"git {' '.join(args)} failed: {out.stderr.strip()}")
    return [line for line in out.stdout.splitlines() if line]


def grep_files(token):
    return _git("grep", "-lF", token)


def ls_files(*globs):
    return _git("ls-files", *globs)


def read(rel):
    return (ROOT / rel).read_text(encoding="utf-8")


def write(rel, text):
    if not DRY:
        (ROOT / rel).write_text(text, encoding="utf-8")


# --------------------------------------------------------------------------- #
# bump
# --------------------------------------------------------------------------- #
def replace_text(rel, replacements):
    """Apply (old, new) string replacements to a file; return True if changed."""
    text = read(rel)
    new = text
    for old, repl in replacements:
        new = new.replace(old, repl)
    if new != text:
        write(rel, new)
        return True
    return False


def sub(rel, pattern, repl, flags=0):
    """Apply a single regex substitution to a file; return number of edits."""
    text = read(rel)
    new, n = re.subn(pattern, repl, text, flags=flags)
    if n and new != text:
        write(rel, new)
    return n


def bump(version):
    changed = []

    # 1. The unambiguous "0.0.0-nightly" sentinel -- whole-file text replace.
    #    Covers ~120 poms, all charts (Chart.yaml/values.yaml/README.md), every
    #    service Dockerfile (CN_VERSION, OCI label, BASE_VERSION), Agama
    #    project.json, demo manifests, docs, and the shell/automation scripts.
    for rel in sorted(set(grep_files(NIGHTLY) + grep_files(NIGHTLY_BADGE))):
        if rel in EXCLUDE_NIGHTLY:
            continue
        if replace_text(rel, [(NIGHTLY, version), (NIGHTLY_BADGE, version)]):
            changed.append(rel)

    # 2. Bare "nightly" release-tag pointers (download URLs resolve against these).
    #    CN_RELEASE_TAG must track CN_VERSION or the WAR download 404s.
    for rel in grep_files("CN_RELEASE_TAG=nightly"):
        if sub(rel, r"(CN_RELEASE_TAG=)nightly\b", rf"\g<1>v{version}"):
            changed.append(rel)
    cedarling_pom = "jans-cedarling/bindings/cedarling-java/pom.xml"
    if sub(cedarling_pom,
           r"(<cedarling\.release\.tag>)nightly(</cedarling\.release\.tag>)",
           rf"\g<1>v{version}\g<2>"):
        changed.append(cedarling_pom)

    # 3. Plain "0.0.0" -- anchored, structured edits on known owned fields only.
    # Rust crates: the package version sits on a line starting `version = `;
    # dependency pins are inline tables, so the anchor cannot hit them.
    for rel in ls_files("*Cargo.toml"):
        if sub(rel, r'(?m)^version\s*=\s*"0\.0\.0"', f'version = "{version}"'):
            changed.append(rel)
    # Python __version__ (jans-pycloudlib, jans-cli-tui, jans-linux-setup)
    for rel in ls_files("*version.py"):
        if sub(rel, r'(__version__\s*=\s*")0\.0\.0(")', rf"\g<1>{version}\g<2>"):
            changed.append(rel)
    # pyproject literal version (docs, flask-sidecar; pycloudlib is dynamic)
    for rel in ls_files("*pyproject.toml"):
        if sub(rel, r'(?m)^version\s*=\s*"0\.0\.0"', f'version = "{version}"'):
            changed.append(rel)
    # cedarling native artifact version (paired with the release.tag above)
    if sub(cedarling_pom,
           r"(<cedarling\.native\.version>)0\.0\.0(</cedarling\.native\.version>)",
           rf"\g<1>{version}\g<2>"):
        changed.append(cedarling_pom)
    # browser-extension manifests (separate from their already-1.x package.json)
    for rel in ls_files("demos/janssen-tarp/*manifest.json"):
        if sub(rel, r'("version"\s*:\s*")0\.0\.0(")', rf"\g<1>{version}\g<2>"):
            changed.append(rel)

    # 4. jans-linux-setup app_info.json: version + build-suffix live in two keys.
    app_info = "jans-linux-setup/jans_setup/app_info.json"
    if (ROOT / app_info).exists():
        data = json.loads(read(app_info))
        if data.get("JANS_APP_VERSION") == "0.0.0" or data.get("JANS_BUILD") == "-nightly":
            data["JANS_APP_VERSION"] = version
            data["JANS_BUILD"] = ""
            write(app_info, json.dumps(data, indent=2) + "\n")
            changed.append(app_info)

    # 5. Helm prerelease flag -- a tagged release is not a prerelease.
    for rel in ("charts/janssen/Chart.yaml", "charts/janssen-all-in-one/Chart.yaml"):
        if (ROOT / rel).exists():
            if sub(rel, r"(artifacthub\.io/prerelease:\s*['\"])true(['\"])", r"\g<1>false\g<2>"):
                changed.append(rel)

    return sorted(set(changed))


def pin_source_sha(sha):
    """Optionally pin Dockerfile JANS_SOURCE_VERSION to the release commit."""
    if not SHA_RE.fullmatch(sha):
        raise SystemExit(f"--pin-source-sha must be a 40-char commit sha, got: {sha}")
    changed = []
    for rel in grep_files("JANS_SOURCE_VERSION="):
        if sub(rel, r"(JANS_SOURCE_VERSION=)" + SHA_RE.pattern, rf"\g<1>{sha}"):
            changed.append(rel)
    return changed


# --------------------------------------------------------------------------- #
# verify -- the safety net; any leftover owned version fails the release
# --------------------------------------------------------------------------- #
def verify():
    problems = []

    for token in (NIGHTLY, NIGHTLY_BADGE):
        for rel in grep_files(token):
            if rel not in EXCLUDE_NIGHTLY:
                problems.append(f"{rel}: still contains {token}")

    for rel in grep_files("CN_RELEASE_TAG=nightly"):
        problems.append(f"{rel}: CN_RELEASE_TAG still 'nightly'")

    cedarling_pom = "jans-cedarling/bindings/cedarling-java/pom.xml"
    if (ROOT / cedarling_pom).exists():
        txt = read(cedarling_pom)
        if re.search(r"<cedarling\.release\.tag>nightly<", txt):
            problems.append(f"{cedarling_pom}: cedarling.release.tag still 'nightly'")
        if re.search(r"<cedarling\.native\.version>0\.0\.0<", txt):
            problems.append(f"{cedarling_pom}: cedarling.native.version still '0.0.0'")

    for rel in ls_files("*Cargo.toml") + ls_files("*pyproject.toml"):
        if re.search(r'(?m)^version\s*=\s*"0\.0\.0"', read(rel)):
            problems.append(f"{rel}: package version still '0.0.0'")
    for rel in ls_files("*version.py"):
        if re.search(r'__version__\s*=\s*"0\.0\.0"', read(rel)):
            problems.append(f"{rel}: __version__ still '0.0.0'")
    for rel in ls_files("demos/janssen-tarp/*manifest.json"):
        if re.search(r'"version"\s*:\s*"0\.0\.0"', read(rel)):
            problems.append(f"{rel}: manifest version still '0.0.0'")

    app_info = "jans-linux-setup/jans_setup/app_info.json"
    if (ROOT / app_info).exists():
        data = json.loads(read(app_info))
        if data.get("JANS_APP_VERSION") == "0.0.0":
            problems.append(f"{app_info}: JANS_APP_VERSION still '0.0.0'")
        if data.get("JANS_BUILD") == "-nightly":
            problems.append(f"{app_info}: JANS_BUILD still '-nightly'")

    if problems:
        print("VERSION VERIFY FAILED -- owned versions left at the dev sentinel:\n")
        for p in problems:
            print(f"  - {p}")
        print(f"\n{len(problems)} location(s) not bumped. Refusing to release.")
        return False
    print("Version verify passed: no owned dev-version sentinels remain.")
    return True


# --------------------------------------------------------------------------- #
def main():
    global ROOT, DRY
    ap = argparse.ArgumentParser(description="Bump/verify Janssen release versions in the working tree.")
    ap.add_argument("version", help="release version, e.g. 1.15.0")
    ap.add_argument("--verify", action="store_true", help="verify the tree is fully bumped (no writes)")
    ap.add_argument("--dry-run", action="store_true", help="report changes without writing")
    ap.add_argument("--root", type=Path, default=Path.cwd(), help="repo root (default: cwd)")
    ap.add_argument("--pin-source-sha", metavar="SHA",
                    help="also pin Dockerfile JANS_SOURCE_VERSION to this commit")
    args = ap.parse_args()

    if not VERSION_RE.match(args.version):
        ap.error(f"invalid version '{args.version}' (expected X.Y.Z or X.Y.Z-suffix)")

    ROOT = args.root.resolve()
    DRY = args.dry_run

    if args.verify:
        sys.exit(0 if verify() else 1)

    changed = bump(args.version)
    if args.pin_source_sha:
        changed += pin_source_sha(args.pin_source_sha)
    changed = sorted(set(changed))

    mode = "DRY RUN -- would change" if DRY else "changed"
    print(f"{mode} {len(changed)} file(s) -> {args.version}")
    for rel in changed:
        print(f"  {rel}")


if __name__ == "__main__":
    main()
