#!/usr/bin/env python3
"""Publish a Zensical build to the versioned ``gh-pages`` layout.

Replaces ``mike``, which builds through MkDocs and therefore cannot publish a
Zensical site. The on-disk layout is kept byte-compatible with what mike
produced, because the Material version selector shipped by Zensical reads
``versions.json`` from the deploy root and resolves aliases as directories.

Subcommands:

  deploy VERSION [ALIAS ...]   copy a built site into ``<VERSION>/`` and point
                               the given aliases at it
  alias VERSION ALIAS [...]    (re)point aliases without rebuilding
  set-default ALIAS            write the root redirect
"""

from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

REDIRECT_TEMPLATE = """<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <title>Redirecting</title>
  <noscript>
    <meta http-equiv="refresh" content="1; url={target}/" />
  </noscript>
  <script>
    window.location.replace(
      "{target}/" + window.location.search + window.location.hash
    );
  </script>
</head>
<body>
  Redirecting to <a href="{target}/">{target}/</a>...
</body>
</html>
"""

VERSION_RE = re.compile(r"^v?(\d+)\.(\d+)\.(\d+)")


def run(*args: str, cwd: Path | None = None, check: bool = True) -> str:
    result = subprocess.run(
        args, cwd=cwd, check=check, text=True, capture_output=True
    )
    if result.stdout:
        print(result.stdout, end="")
    if result.stderr:
        print(result.stderr, end="", file=sys.stderr)
    return result.stdout


def sort_key(entry: dict) -> tuple:
    match = VERSION_RE.match(entry["version"])
    if not match:
        return (0, (0, 0, 0))
    return (1, tuple(-int(part) for part in match.groups()))


def load_versions(root: Path) -> list[dict]:
    path = root / "versions.json"
    if not path.exists():
        return []
    return json.loads(path.read_text(encoding="utf-8"))


def save_versions(root: Path, versions: list[dict]) -> None:
    versions.sort(key=sort_key)
    (root / "versions.json").write_text(
        json.dumps(versions, indent=2) + "\n", encoding="utf-8"
    )


def set_aliases(root: Path, version: str, aliases: list[str]) -> None:
    versions = load_versions(root)
    entry = next((v for v in versions if v["version"] == version), None)
    if entry is None:
        entry = {"version": version, "title": version, "aliases": []}
        versions.append(entry)

    for alias in aliases:
        for other in versions:
            if other is not entry and alias in other["aliases"]:
                other["aliases"].remove(alias)
        if alias not in entry["aliases"]:
            entry["aliases"].append(alias)

        link = root / alias
        if link.is_symlink() or link.exists():
            if link.is_dir() and not link.is_symlink():
                shutil.rmtree(link)
            else:
                link.unlink()
        link.symlink_to(version)

    save_versions(root, versions)


def deploy(root: Path, site: Path, version: str, aliases: list[str]) -> None:
    target = root / version
    if target.exists():
        shutil.rmtree(target)
    shutil.copytree(site, target, symlinks=True)
    set_aliases(root, version, aliases)


def set_default(root: Path, alias: str) -> None:
    (root / "index.html").write_text(
        REDIRECT_TEMPLATE.format(target=alias), encoding="utf-8"
    )


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", default=".", help="repository to operate on")
    parser.add_argument("--branch", default="gh-pages")
    parser.add_argument("--remote", default="origin")
    parser.add_argument("--site-dir", default="site", help="Zensical build output")
    parser.add_argument("--message", help="commit message")
    parser.add_argument("--push", action="store_true")
    sub = parser.add_subparsers(dest="command", required=True)

    deploy_cmd = sub.add_parser("deploy")
    deploy_cmd.add_argument("version")
    deploy_cmd.add_argument("aliases", nargs="*")

    alias_cmd = sub.add_parser("alias")
    alias_cmd.add_argument("version")
    alias_cmd.add_argument("aliases", nargs="+")

    default_cmd = sub.add_parser("set-default")
    default_cmd.add_argument("alias")

    args = parser.parse_args()
    repo = Path(args.repo).resolve()

    tracking = f"refs/remotes/{args.remote}/{args.branch}"
    run("git", "fetch", args.remote,
        f"+refs/heads/{args.branch}:{tracking}", cwd=repo)
    worktree = Path(tempfile.mkdtemp(prefix="gh-pages-"))
    run("git", "worktree", "add", "--detach", str(worktree), tracking, cwd=repo)
    try:
        if args.command == "deploy":
            deploy(worktree, Path(args.site_dir).resolve(), args.version, args.aliases)
            default_message = f"docs: deploy {args.version}"
        elif args.command == "alias":
            set_aliases(worktree, args.version, args.aliases)
            default_message = f"docs: alias {', '.join(args.aliases)} to {args.version}"
        else:
            set_default(worktree, args.alias)
            default_message = f"docs: set default to {args.alias}"

        run("git", "add", "--all", ".", cwd=worktree)
        status = run("git", "status", "--porcelain", cwd=worktree)
        if not status.strip():
            print("Nothing to commit.")
            return 0

        commit = ["git", "commit", "-m", args.message or default_message]
        if os.environ.get("DOCS_SIGN_COMMITS", "1") == "1":
            commit.insert(2, "-S")
        run(*commit, cwd=worktree)
        if args.push:
            run("git", "push", args.remote, f"HEAD:{args.branch}", cwd=worktree)
    finally:
        run("git", "worktree", "remove", "--force", str(worktree), cwd=repo,
            check=False)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
