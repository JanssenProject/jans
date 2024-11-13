import json
from dataclasses import dataclass
from unittest.mock import patch

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


@pytest.mark.parametrize("value, expected", [
    ("random", "random"),
    ("", "Service"),
])
def test_on_backoff(caplog, value, expected):
    from jans.pycloudlib.wait import on_backoff

    details = {"kwargs": {"label": value}, "wait": 10.0}
    on_backoff(details)
    assert "is not ready" in caplog.records[0].message
    assert expected in caplog.records[0].message


@pytest.mark.parametrize("value, expected", [
    ("random", "random"),
    ("", "Service"),
])
def test_on_succes(caplog, value, expected):
    import logging
    from jans.pycloudlib.wait import on_success

    with caplog.at_level(logging.INFO):
        details = {"kwargs": {"label": value}}
        on_success(details)
        assert "is ready" in caplog.records[0].message
        assert expected in caplog.records[0].message


@pytest.mark.parametrize("value, expected", [
    ("random", "random"),
    ("", "Service"),
])
def test_on_giveup(caplog, value, expected):
    from jans.pycloudlib.wait import on_giveup

    details = {"kwargs": {"label": value}, "elapsed": 10.0}
    on_giveup(details)
    assert "is not ready after" in caplog.records[0].message
    assert expected in caplog.records[0].message


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


_PERSISTENCE_MAPPER_GROUP_FUNC = "jans.pycloudlib.persistence.utils.PersistenceMapper.groups"


def test_wait_for_sql(monkeypatch, gmanager):
    from jans.pycloudlib.wait import wait_for_sql

    monkeypatch.setenv("CN_WAIT_MAX_TIME", "0")
    monkeypatch.setenv("CN_PERSISTENCE_TYPE", "sql")

    monkeypatch.setattr(
        "jans.pycloudlib.persistence.sql.SqlClient.row_exists",
        lambda cls, t, i: False
    )

    with pytest.raises(Exception):
        wait_for_sql(gmanager)


def test_wait_for_sql_no_search_mapping(monkeypatch, gmanager):
    from jans.pycloudlib.wait import wait_for_sql

    monkeypatch.setenv("CN_WAIT_MAX_TIME", "0")
    monkeypatch.setenv("CN_PERSISTENCE_TYPE", "sql")

    monkeypatch.setattr(
        _PERSISTENCE_MAPPER_GROUP_FUNC,
        lambda cls: {"sql": ["random"]}
    )

    monkeypatch.setattr(
        "jans.pycloudlib.persistence.sql.SqlClient.connected",
        lambda cls: False
    )

    with pytest.raises(Exception):
        wait_for_sql(gmanager)


def test_wait_for_sql_conn(monkeypatch, gmanager):
    from jans.pycloudlib.wait import wait_for_sql_conn

    monkeypatch.setenv("CN_WAIT_MAX_TIME", "0")
    monkeypatch.setenv("CN_PERSISTENCE_TYPE", "sql")

    monkeypatch.setattr(
        "jans.pycloudlib.persistence.sql.SqlClient.connected",
        lambda cls: False
    )

    with pytest.raises(Exception):
        wait_for_sql_conn(gmanager)


_WAIT_FOR_FUNC = "jans.pycloudlib.wait.wait_for"


@pytest.mark.parametrize("persistence_type, deps", [
    ("sql", ["sql"]),
])
def test_wait_for_persistence(monkeypatch, gmanager, persistence_type, deps):
    from jans.pycloudlib.wait import wait_for_persistence

    monkeypatch.setenv("CN_PERSISTENCE_TYPE", persistence_type)

    with patch(_WAIT_FOR_FUNC, autospec=True) as patched:
        wait_for_persistence(gmanager)
        patched.assert_called_with(gmanager, deps)


def test_wait_for_persistence_hybrid(monkeypatch, gmanager):
    from jans.pycloudlib.wait import wait_for_persistence

    monkeypatch.setenv("CN_PERSISTENCE_TYPE", "hybrid")
    monkeypatch.setenv(
        "CN_HYBRID_MAPPING",
        json.dumps({
            "default": "sql",
            "user": "sql",
            "site": "sql",
            "cache": "sql",
            "token": "sql",
            "session": "sql",
        }),
    )

    with patch(_WAIT_FOR_FUNC, autospec=True) as patched:
        wait_for_persistence(gmanager)
        patched.assert_called_with(gmanager, ["sql"])


@pytest.mark.parametrize("persistence_type, deps", [
    ("sql", ["sql_conn"]),
])
def test_wait_for_persistence_conn(monkeypatch, gmanager, persistence_type, deps):
    from jans.pycloudlib.wait import wait_for_persistence_conn

    monkeypatch.setenv("CN_PERSISTENCE_TYPE", persistence_type)

    with patch(_WAIT_FOR_FUNC, autospec=True) as patched:
        wait_for_persistence_conn(gmanager)
        patched.assert_called_with(gmanager, deps)


def test_wait_for_persistence_conn_hybrid(monkeypatch, gmanager):
    from jans.pycloudlib.wait import wait_for_persistence_conn

    monkeypatch.setenv("CN_PERSISTENCE_TYPE", "hybrid")
    monkeypatch.setenv(
        "CN_HYBRID_MAPPING",
        json.dumps({
            "default": "sql",
            "user": "sql",
            "site": "sql",
            "cache": "sql",
            "token": "sql",
            "session": "sql",
        }),
    )

    with patch(_WAIT_FOR_FUNC, autospec=True) as patched:
        wait_for_persistence_conn(gmanager)
        patched.assert_called_with(gmanager, ["sql_conn"])


def test_wait_for(gmanager):
    from jans.pycloudlib.wait import wait_for

    with patch("jans.pycloudlib.wait.wait_for_config") as patched:
        wait_for(gmanager, ["config"])
        patched.assert_called()


def test_wait_for_invalid_deps(gmanager, caplog):
    from jans.pycloudlib.wait import wait_for

    wait_for(gmanager, ["random"])
    assert "Unsupported callback for random dependency" in caplog.records[0].message
