import pytest


@pytest.mark.parametrize("value, expected", [
    (10, 10),
    (0, 1),
    ("not_integer", 60 * 5),
])
def test_get_wait_max_time(monkeypatch, value, expected):
    from jans.pycloudlib.wait import get_wait_max_time

    monkeypatch.setenv("CN_WAIT_MAX_TIME", str(value))
    assert get_wait_max_time() == expected


@pytest.mark.parametrize("value, expected", [
    (5, 5),
    (0, 1),
    ("not_integer", 10),
])
def test_get_wait_interval(monkeypatch, value, expected):
    from jans.pycloudlib.wait import get_wait_interval

    monkeypatch.setenv("CN_WAIT_SLEEP_DURATION", str(value))
    assert get_wait_interval() == expected


def test_on_backoff(caplog):
    from jans.pycloudlib.wait import on_backoff

    details = {"kwargs": {"label": "Service"}, "wait": 10.0}
    on_backoff(details)
    assert "is not ready" in caplog.records[0].message


def test_on_succes(caplog):
    import logging
    from jans.pycloudlib.wait import on_success

    with caplog.at_level(logging.INFO):
        details = {"kwargs": {"label": "Service"}}
        on_success(details)
        assert "is ready" in caplog.records[0].message


def test_on_giveup(caplog):
    from jans.pycloudlib.wait import on_giveup

    details = {"kwargs": {"label": "Service"}, "elapsed": 10.0}
    on_giveup(details)
    assert "is not ready after" in caplog.records[0].message


def test_wait_for_config(gmanager, monkeypatch):
    from jans.pycloudlib.wait import wait_for_config

    monkeypatch.setenv("CN_WAIT_MAX_TIME", "0")

    with pytest.raises(Exception):
        wait_for_config(gmanager)


def test_wait_for_secret(gmanager, monkeypatch):
    from jans.pycloudlib.wait import wait_for_secret

    monkeypatch.setenv("CN_WAIT_MAX_TIME", "0")

    with pytest.raises(Exception):
        wait_for_secret(gmanager)


@pytest.mark.parametrize("persistence_type", ["ldap", "hybrid"])
def test_wait_for_ldap(gmanager, monkeypatch, persistence_type):
    from jans.pycloudlib.wait import wait_for_ldap

    monkeypatch.setenv("CN_WAIT_MAX_TIME", "0")
    monkeypatch.setenv("CN_PERSISTENCE_TYPE", persistence_type)

    with pytest.raises(Exception):
        wait_for_ldap(gmanager)


@pytest.mark.parametrize("persistence_type", ["couchbase", "hybrid"])
def test_wait_for_couchbase(gmanager, monkeypatch, persistence_type):
    from jans.pycloudlib.wait import wait_for_couchbase

    monkeypatch.setenv("CN_WAIT_MAX_TIME", "0")
    monkeypatch.setenv("CN_PERSISTENCE_TYPE", persistence_type)

    with pytest.raises(Exception):
        wait_for_couchbase(gmanager)
