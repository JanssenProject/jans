#!/usr/bin/env python3
"""De-duplicate TestNG RetryAnalyzer retries and report distinct test outcomes.

testng-results.xml counts every retry attempt, so a flaky test that fails then passes still inflates
``failed``. This collapses retries by (class, method, parameters), keeps each test's final outcome
(PASS > SKIP > FAIL), and reports distinct counts comparable to Jenkins. Failures in
KNOWN_FAILING_CLASSES are accepted as a baseline (reported, non-gating).

Usage:
  summarize_testng.py [--dir DIR]           # Markdown summary to stdout
  summarize_testng.py [--dir DIR] --gate    # one-line tally; exit 1 on a regression or no results
"""
import glob
import os
import sys
import xml.etree.ElementTree as ET
from collections import Counter

RANK = {"PASS": 3, "SKIP": 2, "FAIL": 1}

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

    fails_by_class = Counter(cn for (cn, _, _), st in distinct.items() if st == "FAIL")
    known = sum(n for cls, n in fails_by_class.items() if cls in KNOWN_FAILING_CLASSES)
    regressions = failed - known

    if "--gate" in sys.argv:
        print(f"{total} distinct tests ({raw_total} raw incl ~{retries} retries) — "
              f"{failed} failed: {regressions} regression(s), {known} known-baseline")
        if total == 0:
            sys.exit("::error::no test results were collected")
        if regressions:
            offenders = ", ".join(f"{cls}({n})" for cls, n in fails_by_class.most_common()
                                  if cls not in KNOWN_FAILING_CLASSES)
            sys.exit(f"::error::{regressions} distinct test failure(s) outside the known baseline: {offenders}")
        sys.exit(0)

    print(f"## Integration tests — {os.environ.get('MATRIX', '')}\n")
    print(f"**{total} distinct tests** — {passed} passed, {failed} failed "
          f"({regressions} regression(s), {known} known-baseline), {skipped} skipped  ")
    print(f"<sub>raw TestNG total {raw_total} includes ~{retries} RetryAnalyzer re-runs of flaky/slow "
          f"tests; counts de-duplicate retries by (class, method, parameters).</sub>\n")
    if fails_by_class:
        print("| Failing class | Distinct failures | Status |")
        print("|---|---:|---|")
        for cls, n in fails_by_class.most_common(25):
            tag = "known baseline" if cls in KNOWN_FAILING_CLASSES else "**REGRESSION**"
            print(f"| {cls} | {n} | {tag} |")
    else:
        print("_No distinct failures._")


if __name__ == "__main__":
    main()
