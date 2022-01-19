import pytest


def test_jackrabbit_enable(monkeypatch, settings):
    from pygluu.kubernetes.terminal.jackrabbit import PromptJackrabbit

    monkeypatch.setattr("click.confirm", lambda x, default: True)

    settings.set("jackrabbit.secrets.cnJackrabbitAdminPassword", "Test1234#")
    settings.set("config.configmap.cnJackrabbitAdminId", "admin")
    settings.set("jackrabbit.storage.size", "4Gi")
    settings.set("global.jackrabbit.enabled", "")

    prompt = PromptJackrabbit(settings)
    prompt.prompt_jackrabbit()

    assert settings.get("global.jackrabbit.enabled")
    assert settings.get("jackrabbit.storage.size") == "4Gi"
    assert settings.get("config.configmap.cnJackrabbitUrl") == "http://jackrabbit:8080"
    assert settings.get("config.configmap.cnJackrabbitAdminId") == "admin"
    assert settings.get("jackrabbit.secrets.cnJackrabbitAdminPassword") == "Test1234#"


def test_jackrabbit_disable_no_url(monkeypatch, settings):
    from pygluu.kubernetes.terminal.jackrabbit import PromptJackrabbit

    monkeypatch.setattr("click.confirm", lambda x, default: False)
    monkeypatch.setattr("click.prompt", lambda x, default: "http://jackrabbit:8080")

    settings.set("config.configmap.cnJackrabbitAdminId", "admin")
    settings.set("jackrabbit.secrets.cnJackrabbitAdminPassword", "Test1234#")
    settings.set("installer-settings.jackrabbit.clusterMode", "N")
    settings.set("config.configmap.cnJackrabbitUrl", "")

    prompt = PromptJackrabbit(settings)
    prompt.prompt_jackrabbit()

    assert settings.get("config.configmap.cnJackrabbitUrl") == "http://jackrabbit:8080"


def test_jackrabit_adminid(monkeypatch, settings):
    from pygluu.kubernetes.terminal.jackrabbit import PromptJackrabbit

    monkeypatch.setattr("click.prompt", lambda x, default: "admin")

    settings.set("config.configmap.cnJackrabbitAdminId", "")
    prompt = PromptJackrabbit(settings)
    prompt.prompt_jackrabbit()
    assert settings.get("config.configmap.cnJackrabbitAdminId") == "admin"


@pytest.mark.parametrize("given, expected", [
    (False, False),
    (True, True),
])
def test_testenv_prompt_test_environment(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.jackrabbit import PromptJackrabbit

    monkeypatch.setattr("click.confirm", lambda x, default: given)
    settings.set("installer-settings.postgres.namespace", "test")
    settings.set("installer-settings.jackrabbit.clusterMode", "")
    prompt = PromptJackrabbit(settings)
    prompt.prompt_jackrabbit()
    assert settings.get("installer-settings.jackrabbit.clusterMode") == expected
    

def test_jackrabit_postgresdb(monkeypatch, settings):
    from pygluu.kubernetes.terminal.jackrabbit import PromptJackrabbit

    monkeypatch.setattr("click.prompt", lambda x, default: "jackrabbit")
    settings.set("installer-settings.postgres.install", True)
    settings.set("installer-settings.jackrabbit.clusterMode", True)
    settings.set("config.configmap.cnJackrabbitPostgresDatabaseName", "")
    prompt = PromptJackrabbit(settings)
    prompt.prompt_jackrabbit()
    assert settings.get("config.configmap.cnJackrabbitPostgresDatabaseName") == "jackrabbit"


def test_jackrabit_postgresuser(monkeypatch, settings):
    from pygluu.kubernetes.terminal.jackrabbit import PromptJackrabbit

    monkeypatch.setattr("click.prompt", lambda x, default: "jackrabbit")
    settings.set("installer-settings.postgres.install", True)
    settings.set("installer-settings.jackrabbit.clusterMode", True)
    settings.set("config.configmap.cnJackrabbitPostgresUser", "")
    prompt = PromptJackrabbit(settings)
    prompt.prompt_jackrabbit()
    assert settings.get("config.configmap.cnJackrabbitPostgresUser") == "jackrabbit"
    

def test_jackrabit_postgressize(monkeypatch, settings):
    from pygluu.kubernetes.terminal.jackrabbit import PromptJackrabbit

    monkeypatch.setattr("click.prompt", lambda x, default: "4Gi")

    settings.set("global.jackrabbit.enabled", True)
    settings.set("jackrabbit.storage.size", "")
    prompt = PromptJackrabbit(settings)
    prompt.prompt_jackrabbit()
    assert settings.get("jackrabbit.storage.size") == "4Gi"
