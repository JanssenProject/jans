#!/usr/bin/env python3
"""Stamp git creation/revision dates into page front matter before a build.

Zensical renders ``page.meta.git_revision_date_localized`` and
``git_creation_date_localized`` when present but, unlike
``mkdocs-git-revision-date-localized-plugin``, does not compute them. One
``git log`` traversal fills them in, so the "last update" footer survives the
move off MkDocs.

Run against a throwaway checkout: it rewrites the Markdown sources in place.
"""

from __future__ import annotations

import argparse
import subprocess
from datetime import datetime, timezone
from pathlib import Path

import yaml

DATE_FORMAT = "%B %d, %Y"


def collect_dates(docs_dir: Path) -> dict[str, tuple[int, int]]:
    """Return ``{path: (created, updated)}`` from a single history walk."""
    log = subprocess.run(
        ["git", "log", "--format=C%at", "--name-only", "--", str(docs_dir)],
        check=True, text=True, capture_output=True,
    ).stdout

    dates: dict[str, tuple[int, int]] = {}
    timestamp = 0
    for line in log.splitlines():
        if line.startswith("C") and line[1:].isdigit():
            timestamp = int(line[1:])
        elif line.strip():
            created, updated = dates.get(line, (timestamp, timestamp))
            dates[line] = (timestamp, updated)
    return dates


def split_front_matter(text: str) -> tuple[dict, str]:
    if not text.startswith("---"):
        return {}, text
    end = text.find("\n---", 3)
    if end == -1:
        return {}, text
    block = text[3:end]
    rest = text[text.find("\n", end + 1) + 1:]
    try:
        meta = yaml.safe_load(block)
    except yaml.YAMLError:
        return {}, text
    if not isinstance(meta, dict):
        return {}, text
    return meta, rest


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--docs-dir", default="docs")
    args = parser.parse_args()

    docs_dir = Path(args.docs_dir)
    dates = collect_dates(docs_dir)

    stamped = 0
    for path in docs_dir.rglob("*.md"):
        entry = dates.get(str(path).replace("\\", "/"))
        if not entry:
            continue
        created, updated = entry
        text = path.read_text(encoding="utf-8", errors="replace")
        meta, body = split_front_matter(text)
        meta["git_creation_date_localized"] = datetime.fromtimestamp(
            created, timezone.utc
        ).strftime(DATE_FORMAT)
        meta["git_revision_date_localized"] = datetime.fromtimestamp(
            updated, timezone.utc
        ).strftime(DATE_FORMAT)
        front = yaml.safe_dump(meta, default_flow_style=False, sort_keys=False,
                               allow_unicode=True)
        body = body.lstrip("\n")
        path.write_text(f"---\n{front}---\n\n{body}", encoding="utf-8")
        stamped += 1

    print(f"Stamped git dates into {stamped} pages.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
