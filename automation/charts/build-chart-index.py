#!/usr/bin/env python3
"""Point the Helm repo index at GitHub release assets.

The chart binaries no longer live on the gh-pages branch, only this index does.
Each entry keeps the metadata and digest Helm already recorded for it and gets
its ``urls`` rewritten to the release asset holding that version, so
``helm repo add janssen https://docs.jans.io/charts`` keeps resolving.

``helm repo index --url`` cannot do this: it stamps one base URL on every entry,
while each chart version is an asset of its own release tag.
"""

from __future__ import annotations

import argparse
import subprocess
import sys
from pathlib import Path

import yaml

ASSET_URL = "https://github.com/{repo}/releases/download/{tag}/{filename}"


def release_tag(version: str) -> str:
    return "nightly" if version.endswith("-nightly") else f"v{version}"


def merge_new_charts(index: Path, chart_dir: Path) -> Path:
    subprocess.run(
        ["helm", "repo", "index", str(chart_dir), "--merge", str(index)],
        check=True,
    )
    return chart_dir / "index.yaml"


def rewrite_urls(index: dict, repo: str) -> list[str]:
    rewritten = []
    for name, entries in (index.get("entries") or {}).items():
        for entry in entries:
            version = entry["version"]
            filename = f"{name}-{version}.tgz"
            url = ASSET_URL.format(
                repo=repo, tag=release_tag(version), filename=filename
            )
            entry["urls"] = [url]
            rewritten.append(url)
    return rewritten


def missing_assets(urls: list[str]) -> list[str]:
    missing = []
    for url in urls:
        result = subprocess.run(
            ["curl", "-sS", "-o", "/dev/null", "-w", "%{http_code}",
             "-L", "--head", url],
            check=True, text=True, capture_output=True,
        )
        if result.stdout.strip() != "200":
            missing.append(f"{result.stdout.strip()} {url}")
    return missing


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--index", required=True,
                        help="existing charts/index.yaml to rewrite")
    parser.add_argument("--add", type=Path,
                        help="directory of freshly packaged .tgz to merge in first")
    parser.add_argument("--repo", default="JanssenProject/jans")
    parser.add_argument("--output", help="defaults to --index, rewritten in place")
    parser.add_argument("--verify", action="store_true",
                        help="check every rewritten URL resolves before writing")
    args = parser.parse_args()

    index_path = Path(args.index)
    if args.add:
        index_path = merge_new_charts(index_path, args.add)

    index = yaml.safe_load(index_path.read_text(encoding="utf-8"))
    urls = rewrite_urls(index, args.repo)

    if args.verify:
        missing = missing_assets(urls)
        if missing:
            print("Assets not reachable:", file=sys.stderr)
            for line in missing:
                print(f"  {line}", file=sys.stderr)
            return 1

    output = Path(args.output or args.index)
    output.write_text(
        yaml.safe_dump(index, default_flow_style=False, sort_keys=True,
                       width=10000),
        encoding="utf-8",
    )
    print(f"Rewrote {len(urls)} chart URLs into {output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
