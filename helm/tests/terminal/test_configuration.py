import pytest


@pytest.mark.parametrize("given, expected", [
    ("", "US"),  # default
    ("random", "random"),
])
def test_config_country(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.configuration import PromptConfiguration

    monkeypatch.setattr("click.prompt", lambda x, default: given or expected)

    settings.set("config.countryCode", "")
    settings.set("config.state", "TX")
    settings.set("config.city", "Austin")
    settings.set("config.email", "support@gluu.org")
    settings.set("config.orgName", "Gluu")
    settings.set("config.config.adminPassword", "Admin GUI")
    settings.set("global.fqdn", "demoexample.gluu.org")
    settings.set("config.migration.enabled", False)

    prompt = PromptConfiguration(settings)
    prompt.prompt_config()

    assert settings.get("config.countryCode") == expected


@pytest.mark.parametrize("given, expected", [
    ("", "TX"),  # default
    ("random", "random"),
])
def test_config_state(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.configuration import PromptConfiguration

    monkeypatch.setattr("click.prompt", lambda x, default: given or expected)

    settings.set("config.countryCode", "US")
    settings.set("config.state", "")
    settings.set("config.city", "Austin")
    settings.set("config.email", "support@gluu.org")
    settings.set("config.orgName", "Gluu")
    settings.set("config.config.adminPassword", "Admin GUI")
    settings.set("global.fqdn", "demoexample.gluu.org")
    settings.set("config.migration.enabled", False)

    prompt = PromptConfiguration(settings)
    prompt.prompt_config()

    assert settings.get("config.state") == expected


@pytest.mark.parametrize("given, expected", [
    ("", "Austin"),  # default
    ("random", "random"),
])
def test_config_city(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.configuration import PromptConfiguration

    monkeypatch.setattr("click.prompt", lambda x, default: given or expected)

    settings.set("config.countryCode", "US")
    settings.set("config.state", "TX")
    settings.set("config.city", "")
    settings.set("config.email", "support@gluu.org")
    settings.set("config.orgName", "Gluu")
    settings.set("config.config.adminPassword", "Admin GUI")
    settings.set("global.fqdn", "demoexample.gluu.org")
    settings.set("config.migration.enabled", False)

    prompt = PromptConfiguration(settings)
    prompt.prompt_config()

    assert settings.get("config.city") == expected


@pytest.mark.parametrize("given, expected", [
    ("", "support@gluu.org"),  # default
    ("random", "random"),
])
def test_config_email(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.configuration import PromptConfiguration

    monkeypatch.setattr("click.prompt", lambda x, default: given or expected)

    settings.set("config.countryCode", "US")
    settings.set("config.state", "TX")
    settings.set("config.city", "Austin")
    settings.set("config.email", "")
    settings.set("config.orgName", "Gluu")
    settings.set("config.config.adminPassword", "Admin GUI")
    settings.set("global.fqdn", "demoexample.gluu.org")
    settings.set("config.migration.enabled", False)

    prompt = PromptConfiguration(settings)
    prompt.prompt_config()

    assert settings.get("config.email") == expected


@pytest.mark.parametrize("given, expected", [
    ("", "Gluu"),  # default
    ("random", "random"),
])
def test_config_org(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.configuration import PromptConfiguration

    monkeypatch.setattr("click.prompt", lambda x, default: given or expected)

    settings.set("config.countryCode", "US")
    settings.set("config.state", "TX")
    settings.set("config.city", "Austin")
    settings.set("config.email", "support@gluu.org")
    settings.set("config.orgName", "")
    settings.set("config.config.adminPassword", "Admin GUI")
    settings.set("global.fqdn", "demoexample.gluu.org")
    settings.set("config.migration.enabled", False)

    prompt = PromptConfiguration(settings)
    prompt.prompt_config()

    assert settings.get("config.orgName") == expected


@pytest.mark.parametrize("given, expected", [
    ("", "demoexample.gluu.org"),  # default
])
def test_config_hostname(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.configuration import PromptConfiguration

    monkeypatch.setattr("click.prompt", lambda x, default: given or expected)

    settings.set("config.countryCode", "US")
    settings.set("config.state", "TX")
    settings.set("config.city", "Austin")
    settings.set("config.email", "support@gluu.org")
    settings.set("config.orgName", "Gluu")
    settings.set("config.adminPassword", "Admin GUI")
    settings.set("global.fqdn", "")
    settings.set("config.migration.enabled", False)

    prompt = PromptConfiguration(settings)
    prompt.prompt_config()

    assert settings.get("global.fqdn") == expected


@pytest.mark.parametrize("given, expected", [
    ("", "demoexample.gluu.org"),  # default
])
def test_config_hostname_2(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.configuration import PromptConfiguration

    monkeypatch.setattr("click.prompt", lambda x, default: given or expected)

    settings.set("config.countryCode", "US")
    settings.set("config.state", "TX")
    settings.set("config.city", "Austin")
    settings.set("config.email", "support@gluu.org")
    settings.set("config.orgName", "Gluu")
    settings.set("config.adminPassword", "Admin GUI")
    settings.set("global.fqdn", "")
    settings.set("config.migration.enabled", False)

    prompt = PromptConfiguration(settings)
    prompt.prompt_config()

    assert settings.get("global.fqdn") == expected


@pytest.mark.parametrize("given, expected", [
    ("", "./ce-migration"),
    ("migration", "migration")

])
def test_config_migration_dir(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.configuration import PromptConfiguration

    monkeypatch.setattr("click.prompt", lambda x, default: given or expected)

    settings.set("config.countryCode", "US")
    settings.set("config.state", "TX")
    settings.set("config.city", "Austin")
    settings.set("config.email", "support@gluu.org")
    settings.set("config.orgName", "Gluu")
    settings.set("config.adminPassword", "Admin GUI")
    settings.set("global.fqdn", "demoexample.gluu.org")
    settings.set("config.migration.enabled", True)
    settings.set("config.migration.migrationDir", "")

    prompt = PromptConfiguration(settings)
    prompt.prompt_config()

    assert settings.get("config.migration.migrationDir") == expected


@pytest.mark.parametrize("given, expected", [
    ("", "ldif"),
    ("ldif", "ldif"),
    ("couchbase+json", "couchbase+json"),
    ("spanner+avro", "spanner+avro"),
    ("postgresql+json", "postgresql+json"),
    ("mysql+json", "mysql+json")
])
def test_config_migration_dir(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.configuration import PromptConfiguration

    monkeypatch.setattr("click.prompt", lambda x, default: given or expected)

    settings.set("config.countryCode", "US")
    settings.set("config.state", "TX")
    settings.set("config.city", "Austin")
    settings.set("config.email", "support@gluu.org")
    settings.set("config.orgName", "Gluu")
    settings.set("config.adminPassword", "Admin GUI")
    settings.set("global.fqdn", "demoexample.gluu.org")
    settings.set("config.migration.enabled", True)
    settings.set("config.migration.migrationDir", "./ce-migration")
    settings.set("config.migration.migrationDataFormat", "")

    prompt = PromptConfiguration(settings)
    prompt.prompt_config()

    assert settings.get("config.migration.migrationDataFormat") == expected