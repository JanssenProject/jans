#!/usr/bin/env python3
"""Print a one-line set of ``#summary-<job-id>`` deep links for this run's test summaries.

The GitHub step-summary of a job is addressable at ``<run>/attempts/<n>#summary-<job-id>``, so with
the run's job list we can link straight to the MySQL leg, the PostgreSQL leg, and the combined
(aggregate) summary. Reads the jobs JSON (GitHub API ``.../jobs``) on stdin.

Usage: summary_links.py BASE_URL [md|zulip]   # BASE_URL = <server>/<repo>/actions/runs/<id>/attempts/<n>
"""
import json
import sys

base = sys.argv[1]
fmt = sys.argv[2] if len(sys.argv) > 2 else "md"

ids = {j.get("name"): j.get("id") for j in json.load(sys.stdin).get("jobs", [])}
labels = [("integration (MYSQL)", "MySQL"), ("integration (PGSQL)", "PostgreSQL"),
          ("aggregate", "Combined summary")]
parts = [f"[{label}]({base}#summary-{ids[name]})" for name, label in labels if ids.get(name)]

if parts:
    print(("**Sections:** " if fmt == "zulip" else "**Jump to:** ") + " · ".join(parts))
