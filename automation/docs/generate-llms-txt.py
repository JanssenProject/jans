#!/usr/bin/env python3
"""Generate ``llms.txt``, ``llms-full.txt`` and per-page Markdown for the site.

Zensical has no equivalent of ``mkdocs-llmstxt``, so this reproduces that
plugin's output from the same ``plugins.llmstxt`` block in ``mkdocs.yml``:
an index at ``/llms.txt``, the full corpus at ``/llms-full.txt``, and an
``index.md`` beside every rendered page so the links in them resolve.

Unlike the plugin, pages are emitted as their Markdown source rather than as
HTML converted back to Markdown, which keeps tables and code fences intact.
"""

from __future__ import annotations

import argparse
import fnmatch
import os
from pathlib import Path
from urllib.parse import urljoin

import yaml


class _IgnoreUnknownTags(yaml.SafeLoader):
    """mkdocs.yml carries ``!!python/name:`` tags we neither need nor trust."""


_IgnoreUnknownTags.add_multi_constructor(
    "", lambda loader, suffix, node: None
)
_IgnoreUnknownTags.add_multi_constructor(
    "tag:yaml.org,2002:python/name:", lambda loader, suffix, node: None
)


def load_config(path: Path) -> dict:
    with path.open(encoding="utf-8") as handle:
        return yaml.load(handle, Loader=_IgnoreUnknownTags)


def llmstxt_config(config: dict) -> dict:
    for plugin in config.get("plugins") or []:
        if isinstance(plugin, dict) and "llmstxt" in plugin:
            return plugin["llmstxt"] or {}
    return {}


def nav_titles(nav, titles: dict[str, str]) -> dict[str, str]:
    if isinstance(nav, list):
        for item in nav:
            nav_titles(item, titles)
    elif isinstance(nav, dict):
        for title, value in nav.items():
            if isinstance(value, str):
                titles.setdefault(value, title)
            else:
                nav_titles(value, titles)
    elif isinstance(nav, str):
        titles.setdefault(nav, None)
    return titles


def strip_front_matter(text: str) -> str:
    if not text.startswith("---"):
        return text
    end = text.find("\n---", 3)
    if end == -1:
        return text
    return text[text.find("\n", end + 1) + 1:]


def first_heading(text: str) -> str | None:
    for line in strip_front_matter(text).splitlines():
        if line.startswith("# "):
            return line[2:].strip()
    return None


def page_url_path(src: str) -> str:
    """Mirror MkDocs/Zensical directory URLs: ``a/b.md`` -> ``a/b/index.md``."""
    stem = src[: -len(".md")]
    name = stem.rsplit("/", 1)[-1]
    if name in ("index", "README"):
        parent = stem.rsplit("/", 1)[0] if "/" in stem else ""
        return f"{parent}/index.md" if parent else "index.md"
    return f"{stem}/index.md"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--config", default="mkdocs.yml")
    parser.add_argument("--site-dir", default="site")
    args = parser.parse_args()

    config = load_config(Path(args.config))
    options = llmstxt_config(config)
    sections = options.get("sections") or {}
    if not sections:
        print("No plugins.llmstxt.sections configured; nothing to do.")
        return 0

    docs_dir = Path(config.get("docs_dir", "docs"))
    site_dir = Path(args.site_dir)
    site_url = config.get("site_url", "").rstrip("/") + "/"

    version = os.environ.get("MIKE_DOCS_VERSION")
    if version and site_url:
        site_url = urljoin(site_url, version).rstrip("/") + "/"

    titles = nav_titles(config.get("nav") or [], {})
    sources = sorted(
        str(path.relative_to(docs_dir)).replace(os.sep, "/")
        for path in docs_dir.rglob("*.md")
    )

    header = [
        f"# {config.get('site_name', '')}",
        "",
        f"> {config.get('site_description', '')}",
        "",
    ]
    description = options.get("markdown_description")
    if description:
        header += [description, ""]

    index_lines = list(header)
    full_lines = list(header)

    for section, patterns in sections.items():
        index_lines += [f"## {section}", ""]
        full_lines += [f"# {section}", ""]
        for src in sources:
            if not any(fnmatch.fnmatch(src, pattern) for pattern in patterns):
                continue
            text = (docs_dir / src).read_text(encoding="utf-8", errors="replace")
            url_path = page_url_path(src)
            title = titles.get(src) or first_heading(text) or Path(src).stem
            index_lines.append(f"- [{title}]({site_url}{url_path})")

            body = strip_front_matter(text).strip()
            full_lines += [body, ""]

            page_md = site_dir / url_path
            page_md.parent.mkdir(parents=True, exist_ok=True)
            page_md.write_text(body + "\n", encoding="utf-8")
        index_lines.append("")

    (site_dir / "llms.txt").write_text("\n".join(index_lines), encoding="utf-8")
    full_output = options.get("full_output")
    if full_output:
        (site_dir / full_output).write_text(
            "\n".join(full_lines), encoding="utf-8"
        )
    print(f"Wrote llms.txt and {full_output} to {site_dir}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
