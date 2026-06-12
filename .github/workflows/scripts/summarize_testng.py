#!/usr/bin/env python3
"""De-duplicate TestNG retries and summarise distinct test outcomes.

The jans-auth-server/client (and some other) suites use a TestNG RetryAnalyzer. testng-results.xml
counts *every* retry attempt in its ``total``/``failed`` totals, so a flaky/slow test that fails
twice and then passes still inflates ``failed`` -- on the AIO that turned ~21 real failures into a
reported 486+. This collapses retries by (class, method, parameters), keeps each test's final
outcome (PASS > SKIP > FAIL), and reports the distinct counts, which are comparable to Jenkins.

Usage:
  summarize_testng.py [--dir test-reports]            # print a Markdown summary to stdout
  summarize_testng.py [--dir test-reports] --gate     # print a one-line tally; exit 1 if any
                                                       # distinct test failed or none were found
"""
import glob
import os
import sys
import xml.etree.ElementTree as ET
from collections import Counter

RANK = {"PASS": 3, "SKIP": 2, "FAIL": 1}


def _arg(flag, default):
    return sys.argv[sys.argv.index(flag) + 1] if flag in sys.argv else default


def collect(reports_dir):
    """Return {(class, method, params): final_status} and the raw (retry-inflated) total."""
    distinct, raw_total = {}, 0
    for f in sorted(glob.glob(os.path.join(reports_dir, "*.xml"))):
        try:
            root = ET.parse(f).getroot()
        except ET.ParseError:
            continue
        if root.tag != "testng-results":  # ignore the JUnit-format TEST-*.xml duplicates
            continue
        raw_total += int(root.get("total", 0))
        for cls in root.iter("class"):
            cname = cls.get("name", "")
            for m in cls.iter("test-method"):
                if m.get("is-config") == "true":
                    continue
                params = "|".join((p.findtext("value") or "") for p in m.findall("./params/param"))
                key = (cname, m.get("name", ""), params)
                st = m.get("status", "")
                if RANK.get(st, 0) > RANK.get(distinct.get(key, ""), 0):
                    distinct[key] = st
    return distinct, raw_total


def main():
    reports_dir = _arg("--dir", "test-reports")
    distinct, raw_total = collect(reports_dir)
    c = Counter(distinct.values())
    total = len(distinct)
    passed, failed, skipped = c.get("PASS", 0), c.get("FAIL", 0), c.get("SKIP", 0)
    retries = max(0, raw_total - total)

    if "--gate" in sys.argv:
        print(f"collected {total} distinct tests ({raw_total} raw incl ~{retries} retries), {failed} failed")
        if total == 0:
            sys.exit("::error::no test results were collected")
        if failed:
            sys.exit(f"::error::{failed} distinct test(s) failed")
        sys.exit(0)

    fails_by_class = Counter(cn for (cn, _, _), st in distinct.items() if st == "FAIL")
    print(f"## Integration tests — {os.environ.get('MATRIX', '')}\n")
    print(f"**{total} distinct tests** — {passed} passed, {failed} failed, {skipped} skipped  ")
    print(f"<sub>raw TestNG total {raw_total} includes ~{retries} RetryAnalyzer re-runs of flaky/slow "
          f"tests; the counts above de-duplicate retries by (class, method, parameters).</sub>\n")
    if fails_by_class:
        print("| Failing class | Distinct failures |")
        print("|---|---:|")
        for cn, n in fails_by_class.most_common(25):
            print(f"| {cn.rsplit('.', 1)[-1]} | {n} |")
    else:
        print("_No distinct failures._")


if __name__ == "__main__":
    main()
