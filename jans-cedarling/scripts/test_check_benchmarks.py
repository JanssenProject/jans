"""Tests for check_benchmarks.py.

Run: python3 -m pytest jans-cedarling/scripts/test_check_benchmarks.py
"""

import json

import pytest

import check_benchmarks as cb


def write_estimate(base, name, mean_ns):
    path = base / name / "new"
    path.mkdir(parents=True)
    (path / "estimates.json").write_text(
        json.dumps({"mean": {"point_estimate": mean_ns}})
    )


def test_excluded_benchmark_over_threshold_is_shown_and_passes(tmp_path, capsys):
    write_estimate(tmp_path, "cedarling_startup", 5 * cb.THRESHOLD_NS)

    failed = cb.check_benchmarks(str(tmp_path))

    out = capsys.readouterr().out
    assert not failed, "an excluded benchmark never fails the check"
    assert (
        f"⚪ cedarling_startup/new/estimates.json: {5 * cb.THRESHOLD_NS} ns "
        f"(not checked against {cb.THRESHOLD_NS} ns)" in out
    ), f"an excluded benchmark is still shown, got {out!r}"


def test_checked_benchmark_over_threshold_fails(tmp_path, capsys):
    write_estimate(tmp_path, "authz_authorize_unsigned", cb.THRESHOLD_NS + 1)

    failed = cb.check_benchmarks(str(tmp_path))

    out = capsys.readouterr().out
    assert failed, "a checked benchmark over the threshold fails the check"
    assert "❌ authz_authorize_unsigned/new/estimates.json" in out, out


def test_main_exits_non_zero_only_on_a_checked_failure(tmp_path, monkeypatch):
    write_estimate(tmp_path, "cedarling_startup", 5 * cb.THRESHOLD_NS)
    monkeypatch.setattr(cb, "BASE_PATH", str(tmp_path))
    cb.main()

    write_estimate(tmp_path, "authz_authorize_unsigned", cb.THRESHOLD_NS + 1)
    with pytest.raises(SystemExit) as exit_info:
        cb.main()
    assert exit_info.value.code == 1, "a checked failure exits with 1"
