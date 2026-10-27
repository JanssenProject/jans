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
  put DEST FILE [...]          copy files into ``<DEST>/`` on the branch
  prune                        retire versions past the retention window,
                               leaving a redirect stub behind
"""

from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import tarfile
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

ARCHIVED_TEMPLATE = """<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <title>{version} documentation has been archived</title>
  <meta http-equiv="refresh" content="5; url=/{target}/" />
</head>
<body>
  <h1>{version} documentation has been archived</h1>
  <p>
    This version has reached end of life. Its documentation is available as a
    downloadable archive:
    <a href="{archive_url}">{archive_name}</a>.
  </p>
  <p>
    Redirecting to the <a href="/{target}/">current documentation</a> in five
    seconds.
  </p>
</body>
</html>
"""

ARCHIVE_URL = "https://github.com/{repo}/releases/download/{version}/{name}"

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


def put(root: Path, dest: str, files: list[str]) -> None:
    target = root / dest
    target.mkdir(parents=True, exist_ok=True)
    for name in files:
        source = Path(name)
        shutil.copy2(source, target / source.name)


def aliased_version(versions: list[dict], alias: str) -> str | None:
    for entry in versions:
        if alias in entry["aliases"]:
            return entry["version"]
    return None


def retire(
    root: Path, keep_releases: int, keep: list[str], archive_dir: Path | None,
    repo: str, default_alias: str,
) -> list[str]:
    versions = load_versions(root)
    protected = set(keep) | {"head", "nightly"}
    stable = aliased_version(versions, default_alias)
    if stable:
        protected.add(stable)

    releases = [v["version"] for v in versions if VERSION_RE.match(v["version"])]
    releases.sort(key=lambda v: sort_key({"version": v}))
    protected.update(releases[: keep_releases + 1])

    retired = []
    for entry in list(versions):
        version = entry["version"]
        if version in protected:
            continue
        directory = root / version
        if not directory.is_dir():
            versions.remove(entry)
            continue

        archive_name = f"docs-{version}.tar.gz"
        if archive_dir is not None:
            archive_dir.mkdir(parents=True, exist_ok=True)
            with tarfile.open(archive_dir / archive_name, "w:gz") as tar:
                tar.add(directory, arcname=version)

        shutil.rmtree(directory)
        directory.mkdir()
        (directory / "index.html").write_text(
            ARCHIVED_TEMPLATE.format(
                version=version,
                target=default_alias,
                archive_name=archive_name,
                archive_url=ARCHIVE_URL.format(
                    repo=repo, version=version, name=archive_name
                ),
            ),
            encoding="utf-8",
        )
        versions.remove(entry)
        retired.append(version)

    save_versions(root, versions)
    return retired


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", default=".", help="repository to operate on")
    parser.add_argument("--repo-slug", default="JanssenProject/jans",
                        help="owner/name used to build archive download URLs")
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

    put_cmd = sub.add_parser("put")
    put_cmd.add_argument("dest")
    put_cmd.add_argument("files", nargs="+")

    prune_cmd = sub.add_parser("prune")
    prune_cmd.add_argument("--keep-releases", type=int, default=4,
                           help="releases to keep in addition to the default one")
    prune_cmd.add_argument("--keep", action="append", default=[],
                           help="version to retain regardless of age, repeatable")
    prune_cmd.add_argument("--default-alias", default="stable")
    prune_cmd.add_argument("--archive-dir", type=Path,
                           help="write docs-<version>.tar.gz here before removing")

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
        elif args.command == "put":
            put(worktree, args.dest, [str(Path(f).resolve()) for f in args.files])
            default_message = f"docs: update {args.dest}"
        elif args.command == "prune":
            archive_dir = args.archive_dir.resolve() if args.archive_dir else None
            retired = retire(worktree, args.keep_releases, args.keep, archive_dir,
                             args.repo_slug, args.default_alias)
            if not retired:
                print("Nothing to retire.")
            else:
                print(f"Retired: {', '.join(retired)}")
            default_message = f"docs: retire {', '.join(retired)}"
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
