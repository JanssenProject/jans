#!/usr/bin/env python3
"""De-duplicate TestNG RetryAnalyzer retries and report distinct test outcomes.

testng-results.xml counts every retry attempt, so a flaky test that fails then passes still inflates
``failed``. This collapses retries by (class, method, parameters), keeps each test's final outcome
(PASS > SKIP > FAIL), and reports distinct counts comparable to Jenkins. Failures in
KNOWN_FAILING_CLASSES are accepted as a baseline (reported, non-gating).

Pure-JUnit modules (no testng-results.xml — e.g. cedarling-java, jans-lock, agama, the fido2-server
unit tests) emit only JUnit ``TEST-*.xml``. Those are parsed separately and gated with no baseline
(they should be clean). A module's JUnit ``TEST-*.xml`` is skipped when that module also produced a
testng-results.xml — the TestNG provider emits both and the JUnit copy is a duplicate.

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

# Pure-JUnit unit suites are expected to pass cleanly. Baseline only genuinely pre-existing,
# environmental or flaky failures (mirrors KNOWN_FAILING_CLASSES):
#  - jans-auth-server comp.db opens an LDAP connection pool, but the AIO persistence is SQL — it fails
#    with "connect error 91" regardless of our changes.
#  - the cedarling two-round telemetry test asserts flush-accumulation across a ~14s window; it is
#    timing-flaky (passes on MySQL, intermittently fails on the slower PGSQL leg).
KNOWN_FAILING_JUNIT = {
    "io.jans.as.server.comp.db.UserJansExtUidAttributeTest",
    "io.jans.lock.cedarling.telemetry.CedarlingTelemetryIntegrationTest$TwoRoundTelemetryLifecycle",
}


def _arg(flag, default):
    return sys.argv[sys.argv.index(flag) + 1] if flag in sys.argv else default


def collect(reports_dir):
    """Return {(class, method, params): final_status}, the raw (retry-inflated) total, and the set of
    report filename-prefixes carrying a testng-results.xml (so their JUnit duplicates are skipped)."""
    distinct, raw_total, testng_prefixes = {}, 0, set()
    for f in sorted(glob.glob(os.path.join(reports_dir, "*.xml"))):
        try:
            root = ET.parse(f).getroot()
        except ET.ParseError:
            continue
        if root.tag != "testng-results":  # JUnit-format TEST-*.xml handled by collect_junit()
            continue
        name = os.path.basename(f)
        if name.endswith("-testng-results.xml"):
            testng_prefixes.add(name[: -len("-testng-results.xml")])
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
    return distinct, raw_total, testng_prefixes


def collect_junit(reports_dir, testng_prefixes):
    """Parse JUnit ``TEST-*.xml`` from modules without a testng-results.xml.
    Return (total, failed, skipped, fails_by_class)."""
    total = failed = skipped = 0
    fails_by_class = Counter()
    for f in sorted(glob.glob(os.path.join(reports_dir, "*TEST-*.xml"))):
        if os.path.basename(f).split("-TEST-", 1)[0] in testng_prefixes:
            continue  # duplicate of a TestNG suite already counted by collect()
        try:
            root = ET.parse(f).getroot()
        except ET.ParseError:
            continue
        suites = [root] if root.tag == "testsuite" else root.iter("testsuite")
        for ts in suites:
            total += int(ts.get("tests", 0))
            skipped += int(ts.get("skipped", 0))
            bad = int(ts.get("failures", 0)) + int(ts.get("errors", 0))
            if bad:
                failed += bad
                fails_by_class[ts.get("name", os.path.basename(f))] += bad
    return total, failed, skipped, fails_by_class


def main():
    reports_dir = _arg("--dir", "test-reports")
    distinct, raw_total, testng_prefixes = collect(reports_dir)
    c = Counter(distinct.values())
    total = len(distinct)
    passed, failed, skipped = c.get("PASS", 0), c.get("FAIL", 0), c.get("SKIP", 0)
    retries = max(0, raw_total - total)

    fails_by_class = Counter(cn for (cn, _, _), st in distinct.items() if st == "FAIL")
    known = sum(n for cls, n in fails_by_class.items() if cls in KNOWN_FAILING_CLASSES)
    regressions = failed - known

    # pure-JUnit unit suites (cedarling-java, jans-lock, agama, fido2-server units)
    u_total, u_failed, u_skipped, u_fails = collect_junit(reports_dir, testng_prefixes)
    u_known = sum(n for cls, n in u_fails.items() if cls in KNOWN_FAILING_JUNIT)
    u_regressions = u_failed - u_known

    if "--gate" in sys.argv:
        print(f"{total} distinct TestNG ({raw_total} raw incl ~{retries} retries) — {failed} failed: "
              f"{regressions} regression(s), {known} known-baseline; "
              f"unit(JUnit) {u_total} — {u_failed} failed: {u_regressions} regression(s)")
        if total == 0:
            sys.exit("::error::no test results were collected")
        problems = []
        if regressions:
            problems.append(", ".join(f"{cls}({n})" for cls, n in fails_by_class.most_common()
                                      if cls not in KNOWN_FAILING_CLASSES))
        if u_regressions:
            problems.append(", ".join(f"{cls}({n})" for cls, n in u_fails.most_common()
                                      if cls not in KNOWN_FAILING_JUNIT))
        if problems:
            sys.exit(f"::error::test failure(s) outside the known baseline: {'; '.join(problems)}")
        sys.exit(0)

    print(f"## Integration tests — {os.environ.get('MATRIX', '')}\n")
    print(f"**{total} distinct tests** — {passed} passed, {failed} failed "
          f"({regressions} regression(s), {known} known-baseline), {skipped} skipped  ")
    print(f"<sub>raw TestNG total {raw_total} includes ~{retries} RetryAnalyzer re-runs of flaky/slow "
          f"tests; counts de-duplicate retries by (class, method, parameters).</sub>\n")
    if u_total:
        print(f"**Unit suites (JUnit): {u_total} tests** — {u_failed} failed "
              f"({u_regressions} regression(s)), {u_skipped} skipped  \n")
    rows = [(cls, n, cls in KNOWN_FAILING_CLASSES) for cls, n in fails_by_class.most_common(25)]
    rows += [(cls, n, cls in KNOWN_FAILING_JUNIT) for cls, n in u_fails.most_common(25)]
    if rows:
        print("| Failing class | Distinct failures | Status |")
        print("|---|---:|---|")
        for cls, n, is_known in rows:
            print(f"| {cls} | {n} | {'known baseline' if is_known else '**REGRESSION**'} |")
    else:
        print("_No distinct failures._")


if __name__ == "__main__":
    main()
