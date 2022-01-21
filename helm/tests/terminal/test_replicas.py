import pytest


def test_prompt_replicas_auth_server(monkeypatch, settings):
    from pygluu.kubernetes.terminal.replicas import PromptReplicas

    monkeypatch.setattr("click.prompt", lambda x, default: 1)

    settings.set("auth-server.replicas", "")
    PromptReplicas(settings).prompt_replicas()
    assert settings.get("auth-server.replicas") == 1


def test_prompt_replicas_fido2(monkeypatch, settings):
    from pygluu.kubernetes.terminal.replicas import PromptReplicas

    monkeypatch.setattr("click.prompt", lambda x, default: 1)

    # bypass
    settings.set("fido2.replicas", "")

    settings.set("global.fido2.enabled", "Y")
    PromptReplicas(settings).prompt_replicas()
    assert settings.get("fido2.replicas") == 1


def test_prompt_replicas_scim(monkeypatch, settings):
    from pygluu.kubernetes.terminal.replicas import PromptReplicas

    monkeypatch.setattr("click.prompt", lambda x, default: 1)

    # bypass
    settings.set("scim.replicas", "")

    settings.set("global.scim.enabled", "Y")
    PromptReplicas(settings).prompt_replicas()
    assert settings.get("scim.replicas") == 1


@pytest.mark.parametrize("type_", ["ldap", "hybrid"])
def test_prompt_replicas_persistence(monkeypatch, settings, type_):
    from pygluu.kubernetes.terminal.replicas import PromptReplicas

    monkeypatch.setattr("click.prompt", lambda x, default: 1)

    # bypass
    settings.set("opendj.replicas", "")

    settings.set("global.cnPersistenceType", type_)
    PromptReplicas(settings).prompt_replicas()
    assert settings.get("opendj.replicas") == 1


def test_prompt_replicas_oxshibboleth(monkeypatch, settings):
    from pygluu.kubernetes.terminal.replicas import PromptReplicas

    monkeypatch.setattr("click.prompt", lambda x, default: 1)

    # bypass
    settings.set("oxshibboleth.replicas", "")

    settings.set("global.oxshibboleth.enabled", "Y")
    PromptReplicas(settings).prompt_replicas()
    assert settings.get("oxshibboleth.replicas") == 1


def test_prompt_replicas_oxpassport(monkeypatch, settings):
    from pygluu.kubernetes.terminal.replicas import PromptReplicas

    monkeypatch.setattr("click.prompt", lambda x, default: 1)

    # bypass
    settings.set("oxpassport.replicas", "")

    settings.set("config.configmap.cnPassportEnabled", "Y")
    PromptReplicas(settings).prompt_replicas()
    assert settings.get("oxpassport.replicas") == 1


def test_prompt_replicas_client_api(monkeypatch, settings):
    from pygluu.kubernetes.terminal.replicas import PromptReplicas

    monkeypatch.setattr("click.prompt", lambda x, default: 1)

    # bypass
    settings.set("client-api.replicas", "")

    settings.set("global.client-api.enabled", "Y")
    PromptReplicas(settings).prompt_replicas()
    assert settings.get("client-api.replicas") == 1


def test_prompt_replicas_casa(monkeypatch, settings):
    from pygluu.kubernetes.terminal.replicas import PromptReplicas

    monkeypatch.setattr("click.prompt", lambda x, default: 1)

    # bypass
    settings.set("casa.replicas", "")

    settings.set("config.configmap.cnCasaEnabled", "Y")
    PromptReplicas(settings).prompt_replicas()
    assert settings.get("casa.replicas") == 1
