#!/usr/bin/env python3
"""Summarise gotestsum JUnit XML for the Terraform-provider workflow.

Mirrors the role summarize_testng.py plays for the integration workflow, but for
Go test output (the integration script is TestNG-specific).

  default : print a Markdown table to stdout (append to $GITHUB_STEP_SUMMARY)
  --gate  : exit 1 if any test failed/errored, or if no results were produced

Reports dir: $REPORTS_DIR (default ./test-reports). Matrix label: $MATRIX.
"""
import glob
import os
import sys
import xml.etree.ElementTree as ET


def collect(reports_dir):
    tests = failures = errors = skipped = 0
    failed = []
    for path in sorted(glob.glob(os.path.join(reports_dir, "*.xml"))):
        try:
            root = ET.parse(path).getroot()
        except ET.ParseError:
            continue
        for ts in root.iter("testsuite"):
            for tc in ts.findall("testcase"):
                tests += 1
                if tc.find("failure") is not None:
                    failures += 1
                    failed.append(f"{tc.get('classname', '')}.{tc.get('name', '')}")
                elif tc.find("error") is not None:
                    errors += 1
                    failed.append(f"{tc.get('classname', '')}.{tc.get('name', '')}")
                elif tc.find("skipped") is not None:
                    skipped += 1
    return tests, failures, errors, skipped, failed


def main():
    gate = "--gate" in sys.argv
    reports_dir = os.environ.get("REPORTS_DIR", "test-reports")
    matrix = os.environ.get("MATRIX", "")
    tests, failures, errors, skipped, failed = collect(reports_dir)
    bad = failures + errors

    if gate:
        if tests == 0:
            print(f"::error::no Terraform-provider test results found in {reports_dir}")
            sys.exit(1)
        sys.exit(1 if bad else 0)

    passed = tests - failures - errors - skipped
    label = f" ({matrix})" if matrix else ""
    status = "❌ failures" if bad else ("✅ all passed" if tests else "⚠️ no results")
    print(f"## Terraform provider tests{label}\n")
    print(f"{status}\n")
    print("| Metric | Count |")
    print("|--------|-------|")
    print(f"| Total | {tests} |")
    print(f"| Passed | {passed} |")
    print(f"| Failed | {failures} |")
    print(f"| Errored | {errors} |")
    print(f"| Skipped | {skipped} |")
    if failed:
        print("\n### Failed tests\n```")
        for name in sorted(set(failed)):
            print(name)
        print("```")


if __name__ == "__main__":
    main()
