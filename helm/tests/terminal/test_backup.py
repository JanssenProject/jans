import pytest


@pytest.mark.parametrize("given, expected", [
    ("", "*/30 * * * *"),  # default
    ("*/10 * * * *", "*/10 * * * *"),
])
def test_backup_ldap(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.backup import PromptBackup

    monkeypatch.setattr("click.prompt", lambda x, default: given or expected)

    settings.set("global.cnPersistenceType", "ldap")
    settings.set("installer-settings.ldap.backup.fullSchedule", "")

    PromptBackup(settings).prompt_backup()
    assert settings.get("installer-settings.ldap.backup.fullSchedule") == expected


@pytest.mark.parametrize("given, expected, type_", [
    ("", "*/30 * * * *", "couchbase"),  # default
    ("*/10 * * * *", "*/10 * * * *", "couchbase"),
    ("", "*/30 * * * *", "hybrid"),  # default
    ("*/10 * * * *", "*/10 * * * *", "hybrid"),
])
def test_backup_not_ldap_incr(monkeypatch, settings, given, expected, type_):
    from pygluu.kubernetes.terminal.backup import PromptBackup

    monkeypatch.setattr("click.prompt", lambda x, default: given or expected)

    settings.set("global.cnPersistenceType", type_)
    settings.set("installer-settings.couchbase.backup.fullSchedule", "0 2 * * 6")
    settings.set("installer-settings.couchbase.backup.retentionTime", "168h")
    settings.set("installer-settings.couchbase.backup.storageSize", "20Gi")
    settings.set("installer-settings.couchbase.backup.incrementalSchedule", "")

    PromptBackup(settings).prompt_backup()
    assert settings.get("installer-settings.couchbase.backup.incrementalSchedule") == expected


@pytest.mark.parametrize("given, expected, type_", [
    ("", "0 2 * * 6", "couchbase"),  # default
    ("0 1 * * 6", "0 1 * * 6", "couchbase"),
    ("", "0 2 * * 6", "hybrid"),  # default
    ("0 1 * * 6", "0 1 * * 6", "hybrid"),
])
def test_backup_not_ldap_full(monkeypatch, settings, given, expected, type_):
    from pygluu.kubernetes.terminal.backup import PromptBackup

    monkeypatch.setattr("click.prompt", lambda x, default: given or expected)

    settings.set("global.cnPersistenceType", type_)
    settings.set("installer-settings.couchbase.backup.incrementalSchedule", "*/30 * * * *")
    settings.set("installer-settings.couchbase.backup.retentionTime", "168h")
    settings.set("installer-settings.couchbase.backup.storageSize", "20Gi")
    settings.set("installer-settings.couchbase.backup.incrementalSchedule", "")

    PromptBackup(settings).prompt_backup()
    assert settings.get("installer-settings.couchbase.backup.incrementalSchedule") == expected


@pytest.mark.parametrize("given, expected, type_", [
    ("", "168h", "couchbase"),  # default
    ("160h", "160h", "couchbase"),
    ("", "168h", "hybrid"),  # default
    ("160h", "160h", "hybrid"),
])
def test_backup_not_ldap_retention(monkeypatch, settings, given, expected, type_):
    from pygluu.kubernetes.terminal.backup import PromptBackup

    monkeypatch.setattr("click.prompt", lambda x, default: given or expected)

    settings.set("global.cnPersistenceType", type_)
    settings.set("installer-settings.couchbase.backup.incrementalSchedule", "*/30 * * * *")
    settings.set("installer-settings.couchbase.backup.fullSchedule", "0 2 * * 6")
    settings.set("installer-settings.couchbase.backup.storageSize", "20Gi")
    settings.set("installer-settings.couchbase.backup.retentionTime", "")

    PromptBackup(settings).prompt_backup()
    assert settings.get("installer-settings.couchbase.backup.retentionTime") == expected


@pytest.mark.parametrize("given, expected, type_", [
    ("", "20Gi", "couchbase"),  # default
    ("10Gi", "10Gi", "couchbase"),
    ("", "20Gi", "hybrid"),  # default
    ("10Gi", "10Gi", "hybrid"),
])
def test_backup_not_ldap_storage(monkeypatch, settings, given, expected, type_):
    from pygluu.kubernetes.terminal.backup import PromptBackup

    monkeypatch.setattr("click.prompt", lambda x, default: given or expected)

    settings.set("global.cnPersistenceType", type_)
    settings.set("installer-settings.couchbase.backup.incrementalSchedule", "*/30 * * * *")
    settings.set("installer-settings.couchbase.backup.fullSchedule", "0 2 * * 6")
    settings.set("installer-settings.couchbase.backup.retentionTime", "168h")
    settings.set("installer-settings.couchbase.backup.storageSize", "")

    PromptBackup(settings).prompt_backup()
    assert settings.get("installer-settings.couchbase.backup.storageSize") == expected


def test_backup_fullschedule(monkeypatch, settings):
    from pygluu.kubernetes.terminal.backup import PromptBackup


    monkeypatch.setattr("click.prompt", lambda x, default: "0 2 * * 6")

    settings.set("global.cnPersistenceType", "couchbase")
    settings.set("installer-settings.couchbase.backup.fullSchedule", "")

    PromptBackup(settings).prompt_backup()

    assert settings.get("installer-settings.couchbase.backup.fullSchedule") == "0 2 * * 6"