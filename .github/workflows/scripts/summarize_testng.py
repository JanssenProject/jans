#!/usr/bin/env python3
"""De-duplicate TestNG RetryAnalyzer retries and report distinct test outcomes.

testng-results.xml counts every retry attempt, so a flaky test that fails then passes still inflates
``failed``. This collapses retries by (class, method, parameters), keeps each test's final outcome
(PASS > SKIP > FAIL), and reports distinct counts comparable to Jenkins. Failures in
KNOWN_FAILING_CLASSES are accepted as a baseline (reported, non-gating).

The Markdown summary is a table of contents: a per-module (total / failed) breakdown followed by a
table of every failing test with its **input parameters** (the TestNG data-provider values), so a
failure can be investigated at a glance without digging through the raw logs — the way Jenkins
listed "Parameter #1/#2/#3" on the test-result page.

Usage:
  summarize_testng.py [--dir DIR]           # per-leg Markdown summary to stdout
  summarize_testng.py [--dir DIR] --gate    # one-line tally; exit 1 on a regression or no results
  summarize_testng.py --combined PARENT     # global view across every PARENT/test-reports-* leg
"""
import glob
import os
import sys
import xml.etree.ElementTree as ET
from collections import Counter

RANK = {"PASS": 3, "SKIP": 2, "FAIL": 1}

# Top-level reactors, matched against the report filename prefix (run_aio_integration.sh names each
# collected report "<module-path-with-_>-<original>.xml"). Used to group results in the summary.
MODULES = ["jans-auth-server", "jans-config-api", "jans-fido2", "jans-scim", "jans-core", "jans-orm"]

# Pre-existing failures unrelated to the Jenkins offboarding (SCIM-client + config-api fido2-plugin),
# accepted as a baseline so the gate flags *regressions* in the offboarding-relevant suites rather
# than these known application-level bugs. Revisit as the underlying issues are fixed (e.g. #14249).
KNOWN_FAILING_CLASSES = {
    "io.jans.configapi.plugin.fido2.test.Fido2MetricsTest",             # config-api fido2-plugin (#14249)
    "io.jans.scim2.client.patch.PatchUserExtTest",                      # jans-scim-client
    "io.jans.scim2.client.patch.PatchReplaceUserTest",
    "io.jans.scim2.client.patch.PatchDeleteUserTest",
    "io.jans.scim2.client.singleresource.QueryParamCreateUpdateTest",
    "io.jans.scim2.client.singleresource.FullUserTest",
    "io.jans.scim2.client.singleresource.Fido2DeviceTest",
    "io.jans.scim2.client.tokens.UserTokensTest",
}


def _arg(flag, default):
    return sys.argv[sys.argv.index(flag) + 1] if flag in sys.argv else default


def _module_of(filename):
    """Map a collected report filename back to its top-level reactor."""
    for m in MODULES:
        if filename == m or filename.startswith(m + "_") or filename.startswith(m + "-"):
            return m
    return "other"


def collect(reports_dir):
    """Collapse retries and return (records, raw_total).

    ``records`` maps (class, method, params) -> {status, module, params_list} keeping each test's
    final outcome. ``raw_total`` is the retry-inflated TestNG count (for the retries estimate).
    """
    records, raw_total = {}, 0
    for f in sorted(glob.glob(os.path.join(reports_dir, "*.xml"))):
        try:
            root = ET.parse(f).getroot()
        except ET.ParseError:
            continue
        if root.tag != "testng-results":  # ignore the JUnit-format TEST-*.xml duplicates
            continue
        module = _module_of(os.path.basename(f))
        raw_total += int(root.get("total", 0))
        for cls in root.iter("class"):
            cname = cls.get("name", "")
            for m in cls.iter("test-method"):
                if m.get("is-config") == "true":
                    continue
                params_list = [(p.findtext("value") or "") for p in m.findall("./params/param")]
                key = (cname, m.get("name", ""), "|".join(params_list))
                st = m.get("status", "")
                prev = records.get(key)
                if prev is None or RANK.get(st, 0) > RANK.get(prev["status"], 0):
                    records[key] = {"status": st, "module": module, "params": params_list}
                elif prev.get("module") == "other" and module != "other":
                    prev["module"] = module
    return records, raw_total


def tally(records, raw_total):
    """Derive the headline counts (distinct, pass/fail/skip, regressions vs known baseline)."""
    c = Counter(r["status"] for r in records.values())
    total = len(records)
    failed = c.get("FAIL", 0)
    fails_by_class = Counter(cn for (cn, _, _), r in records.items() if r["status"] == "FAIL")
    known = sum(n for cls, n in fails_by_class.items() if cls in KNOWN_FAILING_CLASSES)
    return {
        "total": total,
        "passed": c.get("PASS", 0),
        "failed": failed,
        "skipped": c.get("SKIP", 0),
        "raw_total": raw_total,
        "retries": max(0, raw_total - total),
        "known": known,
        "regressions": failed - known,
        "fails_by_class": fails_by_class,
    }


def _md_cell(text):
    """Escape a value for a Markdown table cell."""
    return text.replace("\\", "\\\\").replace("|", "\\|").replace("\n", " ").strip()


def _fmt_params(params_list):
    """Render data-provider parameters like Jenkins' numbered "Parameter #N" list."""
    cells = [_md_cell(p) for p in params_list]
    if not any(cells):
        return "—"
    return "<br>".join(f"{i}: {c}" for i, c in enumerate(cells, 1) if c)


def _module_rows(records):
    """[(module, total, failed)] sorted with the catch-all 'other' bucket last."""
    by_total = Counter(r["module"] for r in records.values())
    by_fail = Counter(r["module"] for r in records.values() if r["status"] == "FAIL")
    return [(m, by_total[m], by_fail.get(m, 0))
            for m in sorted(by_total, key=lambda m: (m == "other", m))]


def _print_toc(backend, records, stats):
    """One backend bullet + its per-module (total / failed) sub-bullets."""
    print(f"- **{backend}** — {stats['total']} distinct tests, {stats['failed']} failed "
          f"({stats['regressions']} regression(s), {stats['known']} known-baseline)")
    for mod, mtotal, mfail in _module_rows(records):
        suffix = f", failed: {mfail}" if mfail else ""
        print(f"  - {mod} (total: {mtotal}{suffix})")


def _failing_rows(records, backend=None):
    """[(backend, module, class, method, params)] for every distinct failing test."""
    rows = []
    for (cname, mname, _), r in records.items():
        if r["status"] == "FAIL":
            rows.append((backend, r["module"], cname, mname, r["params"]))
    return rows


def _print_failing_table(rows, with_backend):
    """Collapsible table of failing tests, one row per (class, method, params)."""
    rows.sort(key=lambda x: (x[0] or "", x[1], x[2], x[3]))
    cols = (["Backend"] if with_backend else []) + ["Module", "Class", "Method", "Input parameters", "Status"]
    scope = "backend, " if with_backend else ""
    print(f"<details><summary>Failing tests ({len(rows)}) — {scope}class, method, input parameters</summary>\n")
    print("| " + " | ".join(cols) + " |")
    print("|" + "---|" * len(cols))
    for backend, module, cname, mname, params in rows:
        short_cls = cname.rsplit(".", 1)[-1]
        tag = "known baseline" if cname in KNOWN_FAILING_CLASSES else "**REGRESSION**"
        lead = f"{backend} | " if with_backend else ""
        print(f"| {lead}{module} | `{short_cls}` | `{_md_cell(mname)}` | {_fmt_params(params)} | {tag} |")
    print("\n</details>")


def render_leg(backend, records, raw_total):
    """Per-leg step summary: headline, ToC, failing-tests table for a single backend."""
    stats = tally(records, raw_total)
    print(f"## Integration tests — {backend}\n")
    print(f"**{stats['total']} distinct tests** — {stats['passed']} passed, {stats['failed']} failed "
          f"({stats['regressions']} regression(s), {stats['known']} known-baseline), {stats['skipped']} skipped  ")
    print(f"<sub>raw TestNG total {stats['raw_total']} includes ~{stats['retries']} RetryAnalyzer re-runs "
          f"of flaky/slow tests; counts de-duplicate retries by (class, method, parameters).</sub>\n")
    _print_toc(backend, records, stats)
    print()
    rows = _failing_rows(records)
    if rows:
        _print_failing_table(rows, with_backend=False)
    else:
        print("_No distinct failures._")


def render_combined(parent):
    """Global view: one section stacking every PARENT/test-reports-<backend> leg."""
    legs = []
    for d in sorted(glob.glob(os.path.join(parent, "test-reports-*"))):
        if os.path.isdir(d):
            backend = os.path.basename(d)[len("test-reports-"):]
            records, raw_total = collect(d)
            legs.append((backend, records, raw_total))

    print("## Integration tests — all backends\n")
    if not legs:
        print("_No results collected._")
        return

    all_rows = []
    for backend, records, raw_total in legs:
        _print_toc(backend, records, tally(records, raw_total))
        all_rows += _failing_rows(records, backend=backend)
    print()
    if all_rows:
        _print_failing_table(all_rows, with_backend=True)
    else:
        print("_No distinct failures across any backend._")


def main():
    if "--combined" in sys.argv:
        render_combined(_arg("--combined", "."))
        return

    reports_dir = _arg("--dir", "test-reports")
    records, raw_total = collect(reports_dir)
    stats = tally(records, raw_total)

    if "--gate" in sys.argv:
        print(f"{stats['total']} distinct tests ({stats['raw_total']} raw incl ~{stats['retries']} retries) — "
              f"{stats['failed']} failed: {stats['regressions']} regression(s), {stats['known']} known-baseline")
        if stats["total"] == 0:
            sys.exit("::error::no test results were collected")
        if stats["regressions"]:
            offenders = ", ".join(f"{cls}({n})" for cls, n in stats["fails_by_class"].most_common()
                                  if cls not in KNOWN_FAILING_CLASSES)
            sys.exit(f"::error::{stats['regressions']} distinct test failure(s) outside the known baseline: {offenders}")
        sys.exit(0)

    render_leg(os.environ.get("MATRIX", "") or "results", records, raw_total)


if __name__ == "__main__":
    main()
